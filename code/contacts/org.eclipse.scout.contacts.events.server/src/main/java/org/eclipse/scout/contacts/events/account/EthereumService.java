package org.eclipse.scout.contacts.events.account;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.scout.contacts.events.account.model.Account;
import org.eclipse.scout.contacts.events.account.model.Transaction;
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
  // private static final String ETHEREUM_TEST = "https://ropsten.infura.io/" + TOKEN;

  // the connection to the ethereum net
  private Web3j web3j = null;

  // TODO replace these with some real persistence
  private static Map<String, Account> wallets = new HashMap<>();
  // transactions need to be persisted as well as ethereum currently does not offer an api to list all tx for an account
  // also see https://github.com/ethereum/go-ethereum/issues/1897
  private static Map<UUID, Transaction> transactions = new HashMap<>();

  @PostConstruct
  private void init() {
    LOG.info("Poulating dummy/temp accounts ...");
    populateAccount("prs01", "UTC--2016-12-12T08-09-51.487000000Z--8d2ec831056c620fea2fabad8bf6548fc5810cc3.json", "123");
    populateAccount("prs01a", "UTC--2016-12-12T09-07-24.203000000Z--cbc12f306da804bb681aceeb34f0bc58ba2f7ad7.json", "456");
    LOG.info("local wallets successfully loaded ...");
  }

  private void populateAccount(String personId, String fileName, String password) {
    String walletName = "Primary Account";
    String walletPath = "C:\\Users\\mzi\\AppData\\Local\\Temp";
    Account wallet = Account.load(walletName, password, walletPath, fileName);
    wallet.setPersonId(personId);

    save(wallet);
  }

  public String createTransaction(String from, String to, BigInteger amountWei, String data, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit) {

    if (from == null || to == null || amountWei == null) {
      return null;
    }

    Account wallet = getWallet(from);
    if (wallet == null) {
      return null;
    }

    if (nonce == null) {
      nonce = getNonce(from);
    }

    if (gasPrice == null) {
      gasPrice = Transaction.GAS_PRICE_DEFAULT;
    }

    if (gasLimit == null) {
      gasLimit = Transaction.GAS_LIMIT_DEFAULT;
    }

    Transaction tx = wallet.createSignedTransaction(to, amountWei, data, nonce, gasPrice, gasLimit);
    save(tx);

    return tx.getId().toString();
  }

  public Set<String> getWallets() {
    return wallets.keySet();
  }

  public Set<String> getWallets(String personId) {
    if (personId == null) {
      return new HashSet<>();
    }

    return wallets.values()
        .stream()
        .filter(wallet -> personId.equals(wallet.getPersonId()))
        .map(wallet -> wallet.getAddress())
        .collect(Collectors.toSet());
  }

  public Account getWallet(String address) {
    return wallets.get(address);
  }

  public void save(Account wallet) {
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

  public void save(Transaction transaction) {
    LOG.info("Caching tx from: " + transaction.getFromAddress() + " to: " + transaction.getToAddress() + " with amount " + transaction.getValue() + " and hash: " + transaction.getHash());

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
      throw new ProcessingException("Failed to get balance for address '" + address + "'", e);
    }
  }

  public Transaction send(Transaction tx) {
    LOG.info("Sending TX ...");
    EthSendTransaction ethSendTransaction = null;

    try {
      ethSendTransaction = getWeb3j().ethSendRawTransaction(tx.getSignedContent()).sendAsync().get();
    }
    catch (Exception e) {
      throw new ProcessingException("Failed to send transaction " + tx.getSignedContent(), e);
    }

    checkResponseFromSending(ethSendTransaction);

    tx.setSent(new Date());
    tx.setError(ethSendTransaction.getError());
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
      String message = "Failed to send transaction: " + error.getMessage();
      LOG.error(message);
      throw new ProcessingException(message);
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
