package org.eclipse.scout.contacts.events.account;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.scout.contacts.events.account.model.Transaction;
import org.eclipse.scout.contacts.events.account.model.Wallet;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class WalletService implements IWalletService {
  private static final Logger LOG = LoggerFactory.getLogger(WalletService.class);

  public static final String[] DEMO_ADDRESS = {
      "0x8d2ec831056c620fea2fabad8bf6548fc5810cc3",
      "0xcbc12f306da804bb681aceeb34f0bc58ba2f7ad7"
  };

  public static final int ALICE_ADDRESS = 0;
  public static final String ALICE_ID = "prs01";

  public static final int LENA_ADDRESS = 1;
  public static final String LENA_ID = "prs01a";

  @Override
  public String createAliceLenaTransaction(String personId) {

    if (!(ALICE_ID.contentEquals(personId) || LENA_ID.contentEquals(personId))) {
      return null;
    }

    String from, to;

    if (personId.equals(ALICE_ID)) {
      from = DEMO_ADDRESS[ALICE_ADDRESS];
      to = DEMO_ADDRESS[LENA_ADDRESS];
    }
    else {
      from = DEMO_ADDRESS[LENA_ADDRESS];
      to = DEMO_ADDRESS[ALICE_ADDRESS];
    }

    BigDecimal amountEther = new BigDecimal("0.01");
    BigInteger amountWei = Convert.toWei(amountEther, Unit.ETHER).toBigInteger();
    String transactionId = BEANS.get(EthereumService.class).createTransaction(from, to, amountWei);

    return transactionId;
  }

  @Override
  public String create(String walletPath, String password) {

    if (!StringUtility.hasText(walletPath)) {
      walletPath = createWalletPath();
    }

    // make sure there is a path to store the wallet file
    if (!StringUtility.hasText(walletPath)) {
      return null;
    }

    // TODO fixme/cleanup
    EthereumService service = BEANS.get(EthereumService.class);
    Wallet wallet = null; // service.createWallet("WALLET", password, walletPath);

    service.save(wallet);

    return wallet.getAddress();
  }

  private String createWalletPath() {
    try {
      File tmpFile = File.createTempFile("tmp", ".txt");
      return tmpFile.getParent();
    }
    catch (IOException e) {
      LOG.error("failed to create path to temp file", e);
    }

    return null;
  }

  @Override
  public void sendTransaction(String transactionId) {
    EthereumService service = BEANS.get(EthereumService.class);
    Transaction tx = service.getTransaction(transactionId);
    service.send(tx);
  }
}
