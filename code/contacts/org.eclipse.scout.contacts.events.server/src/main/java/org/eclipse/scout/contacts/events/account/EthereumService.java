package org.eclipse.scout.contacts.events.account;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.scout.contacts.events.account.model.Transaction;
import org.eclipse.scout.contacts.events.account.model.Wallet;
import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

@ApplicationScoped
public class EthereumService {

  private static final Logger LOG = LoggerFactory.getLogger(EthereumService.class);

  private static final String TOKEN = "3UMFlH4jlpWx6IqttMeG";
  private static final String ETHEREUM_MAIN = "https://mainnet.infura.io/" + TOKEN;
  private static final String ETHEREUM_TEST = "https://ropsten.infura.io/" + TOKEN;

  // the connection to the ethereum net
  private Web3j web3j = null;

  // TODO replace these with some real persistence
  private static Map<String, Wallet> wallets = new HashMap<>();
  // transactions need to be persisted as well as ethereum currently does not offer an api to list all tx for an account
  // also see https://github.com/ethereum/go-ethereum/issues/1897
  private static Map<UUID, Transaction> transactions = new HashMap<>();

  @PostConstruct
  private void init() {
    LOG.info("Poulating dummy/temp wallets ...");
    populateWallet("prs01", "UTC--2016-12-12T08-09-51.487000000Z--8d2ec831056c620fea2fabad8bf6548fc5810cc3.json", "123");
    populateWallet("prs01a", "UTC--2016-12-12T09-07-24.203000000Z--cbc12f306da804bb681aceeb34f0bc58ba2f7ad7.json", "456");
    LOG.info("local wallets successfully loaded ...");
  }

  private void populateWallet(String personId, String fileName, String password) {
    String walletName = "WALLET";
    String walletPath = "C:\\Users\\mzi\\AppData\\Local\\Temp";
    Wallet wallet = Wallet.load(walletName, password, walletPath, fileName);
    wallet.setPersonId(personId);

    save(wallet);
  }

  public String createTransaction(String from, String to, BigInteger amountWei) {

    if (from == null || to == null || amountWei == null) {
      return null;
    }

    Wallet wallet = getWallet(from);
    if (wallet == null) {
      return null;
    }

    BigInteger nonce = getNonce(from);
    Transaction tx = wallet.createSignedTransaction(to, nonce, amountWei);
    save(tx);

    return tx.getId().toString();
  }

  public Set<String> getWallets() {
    return wallets.keySet();
  }

  public Wallet getWallet(String address) {
    return wallets.get(address);
  }

  public void save(Wallet wallet) {
    LOG.info("caching wallet '" + wallet.getFileName() + "' with address '" + wallet.getAddress() + "'");

    wallets.put(wallet.getAddress(), wallet);
  }

  public Set<String> getTransactions() {
    return transactions.keySet()
        .stream()
        .map(id -> id.toString())
        .collect(Collectors.toSet());
  }

  public Transaction getTransaction(String id) {
    return transactions.get(UUID.fromString(id));
  }

  private void save(Transaction transaction) {
    LOG.info("caching tx: " + transaction.getValue() + " to: " + transaction.getToAddress() + " with hash " + transaction.getHash());

    transactions.put(transaction.getId(), transaction);
  }

  private Web3j getWeb3j() {
    if (web3j == null) {
      LOG.info("Trying to connect to Ethereum net ...");
      web3j = Web3j.build(new InfuraHttpService(ETHEREUM_MAIN));
      LOG.info("Successfully connected");
    }

    return web3j;
  }

  public BigDecimal getBalance(String address) {
    return getBalance(address, Unit.ETHER);
  }

  public BigDecimal getBalance(String address, Unit unit) {
    if (address == null || unit == null) {
      return null;
    }

    BigInteger balance = getBalanceWei(address);

    if (balance == null) {
      return null;
    }

    return Convert.fromWei(new BigDecimal(balance), unit);
  }

  public BigInteger getBalanceWei(String address) {
    try {
      EthGetBalance balance = getWeb3j().ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
      return balance.getBalance();
    }
    catch (Exception e) {
      throw new ProcessingException("failed to get balance for address '" + address + "'", e);
    }
  }

  public Transaction send(Transaction tx) {
    LOG.info("Sending TX ...");
    EthSendTransaction ethSendTransaction = null;

    try {
      ethSendTransaction = getWeb3j().ethSendRawTransaction(tx.getSignedContent()).sendAsync().get();
    }
    catch (Exception e) {
      throw new ProcessingException("failed to send transaction " + tx.getSignedContent(), e);
    }

    checkResponseFromSending(ethSendTransaction);

    tx.setHash(ethSendTransaction.getTransactionHash());
    tx.setStatus(Transaction.PENDING);
    LOG.info("Successfully sent TX. Hash: " + tx.getHash());

    save(tx);

    return tx;
  }

  private void checkResponseFromSending(EthSendTransaction response) {
    Error error = response.getError();
    String result = response.getResult();

    if (error != null) {
      LOG.error("error (code, message, data): " + error.getCode() + ", '" + error.getMessage() + "', '" + error.getData() + "'");
    }
    else {
      LOG.info("result:" + result);
    }
  }

  public Transaction refreshStatus(String transactionId) {
    LOG.info("Polling TX status...");

    Transaction tx = getTransaction(transactionId);
    EthGetTransactionReceipt txReceipt = null;

    try {
      txReceipt = getWeb3j().ethGetTransactionReceipt(tx.getHash()).sendAsync().get();
    }
    catch (Exception e) {
      throw new ProcessingException("failed to poll status for transaction " + tx.getSignedContent(), e);
    }

    tx.setTransactionReceipt(txReceipt.getResult());
    LOG.info("Successfully polled status. Status: " + tx.getStatus());

    save(tx);

    return tx;
  }

  public BigInteger getNonce(String address) {
    LOG.info("Getting nonce for address " + address + " ...");
    EthGetTransactionCount txCount;

    try {
      txCount = getWeb3j().ethGetTransactionCount(
          address, DefaultBlockParameterName.LATEST).sendAsync().get();
    }
    catch (Exception e) {
      throw new ProcessingException("failed to get nonce for address '" + address + "'");
    }

    BigInteger nonce = txCount.getTransactionCount();
    LOG.info("Successfully got nonce: " + nonce);

    return txCount.getTransactionCount();
  }
}
