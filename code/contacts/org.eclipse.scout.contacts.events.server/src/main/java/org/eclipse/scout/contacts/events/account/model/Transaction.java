package org.eclipse.scout.contacts.events.account.model;

import java.math.BigInteger;
import java.util.UUID;

import org.web3j.protocol.core.methods.request.RawTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class Transaction {

  // https://www.reddit.com/r/ethereum/comments/5g8ia6/attention_miners_we_recommend_raising_gas_limit/
  // check https://ethstats.net/ for current values
  public static final BigInteger GAS_PRICE_DEFAULT = BigInteger.valueOf(25_000_000_000L);
  public static final BigInteger GAS_LIMIT_DEFAULT = BigInteger.valueOf(30_000L);

  public static final int ERROR = -1;
  public static final int UNDEFINED = 0;
  public static final int OFFLINE = 1;
  public static final int PENDING = 2;
  public static final int CONFIRMED = 3;

  /**
   * tx id's are tricky. that's why we add an artificial id here. used for {@link equals} and {@link hashCode}.
   */
  private UUID id;
  private RawTransaction tx;
  private int status;
  private String fromAddress;
  private String signedContent;
  private TransactionReceipt receipt;
  private String hash;

  public Transaction(String toAddress, BigInteger nonce, BigInteger value) {
    id = UUID.randomUUID();
    tx = RawTransaction.createEtherTransaction(
        nonce,
        Transaction.GAS_PRICE_DEFAULT,
        Transaction.GAS_LIMIT_DEFAULT,
        toAddress,
        value);
    status = UNDEFINED;
  }

  public UUID getId() {
    return id;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public RawTransaction getRawTransaction() {
    return tx;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public String getToAddress() {
    return tx.getTo();
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getSignedContent() {
    return signedContent;
  }

  public void setSignedContent(String signedContent) {
    this.signedContent = signedContent;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public BigInteger getValue() {
    return tx.getValue();
  }

  public TransactionReceipt getTransactionReceipt() {
    return receipt;
  }

  public void setTransactionReceipt(TransactionReceipt receipt) {
    if (receipt != null && receipt.getBlockHash().length() > 0) {
      status = CONFIRMED;
    }

    this.receipt = receipt;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj instanceof Transaction) {
      UUID thatId = ((Transaction) obj).id;
      return id.equals(thatId);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

}
