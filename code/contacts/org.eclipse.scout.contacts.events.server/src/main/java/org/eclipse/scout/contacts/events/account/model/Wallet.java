package org.eclipse.scout.contacts.events.account.model;

import java.io.File;
import java.math.BigInteger;

import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

public class Wallet {
  private static final Logger LOG = LoggerFactory.getLogger(Wallet.class);

  private String personId;
  private String name;
  private Credentials credentials = null;
  private String fileName;
  private String pathToFile;
  private String password;

  public static Wallet create(String name, String password, String pathToFile) {
    LOG.info("Creating wallet '" + name + "' with password '" + password + "'");

    Wallet wallet = new Wallet();
    wallet.name = name;
    wallet.pathToFile = pathToFile;
    wallet.password = password;

    try {
      wallet.fileName = WalletUtils.generateNewWalletFile(password, new File(pathToFile));
      wallet.credentials = wallet.getCredentials();

      if (wallet.credentials == null) {
        throw new ProcessingException("failed to obtain account credentials");
      }
    }
    catch (Exception e) {
      LOG.error("failed to create account", e);
      return null;
    }

    LOG.info("Wallet successfully created. File at " + wallet.pathToFile + "/" + wallet.fileName);

    return wallet;
  }

  public static Wallet load(String name, String password, String pathToFile, String fileName) {
    Wallet wallet = new Wallet();
    wallet.name = name;
    wallet.pathToFile = pathToFile;
    wallet.fileName = fileName;
    wallet.password = password;
    wallet.getCredentials();

    return wallet;
  }

  public String getPersonId() {
    return personId;
  }

  public void setPersonId(String personId) {
    this.personId = personId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getPathToFile() {
    return pathToFile;
  }

  public String getAddress() {
    return getCredentials().getAddress();
  }

  public void setCredentials(Credentials credentials) {
    this.credentials = credentials;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public File getFile() {
    return new File(pathToFile, fileName);
  }

  public Credentials getCredentials() {
    if (credentials != null) {
      return credentials;
    }

    try {
      String fileWithPath = getFile().getAbsolutePath();
      credentials = WalletUtils.loadCredentials(password, fileWithPath);
// tried to figure out where the time is used up
//      File file = new File(fileWithPath);
//      ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper(); // 8sec
//      WalletFile walletFile = objectMapper.readValue(file, WalletFile.class);
//      ECKeyPair keyPair = org.web3j.crypto.Wallet.decrypt(password, walletFile); // > 30s
// -> "culprit" is Wallet.generateDerivedScryptKey() inside of Wallet.decrypt
//      Credentials credentials = Credentials.create(keyPair);

      return credentials;
    }
    catch (Exception e) {
      LOG.error("failed to access credentials in file '" + getFile().getAbsolutePath() + "'", e);
      return null;
    }
  }

  public Transaction createSignedTransaction(String toAddress, BigInteger nonce, BigInteger value) {
    Transaction tx = new Transaction(toAddress, nonce, value);
    tx.setFromAddress(getAddress());

    byte[] signedMessage = TransactionEncoder.signMessage(tx.getRawTransaction(), getCredentials());

    tx.setSignedContent(Numeric.toHexString(signedMessage));
    tx.setStatus(Transaction.OFFLINE);

    return tx;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj instanceof Wallet) {
      String address = getAddress();
      String thatAddress = ((Wallet) obj).getAddress();
      return address.equals(thatAddress);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return getAddress().hashCode();
  }
}
