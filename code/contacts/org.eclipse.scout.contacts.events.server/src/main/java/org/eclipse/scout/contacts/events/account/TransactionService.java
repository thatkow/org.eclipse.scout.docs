package org.eclipse.scout.contacts.events.account;

import java.math.BigDecimal;

import org.eclipse.scout.contacts.events.account.TransactionTablePageData.TransactionTableRowData;
import org.eclipse.scout.contacts.events.account.model.Transaction;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class TransactionService implements ITransactionService {

  @Override
  public TransactionTablePageData getTransactionTableData(SearchFilter filter) {
    TransactionTablePageData pageData = new TransactionTablePageData();

    BEANS.get(EthereumService.class).getTransactions()
        .stream()
        .forEach(txId -> {
          Transaction tx = BEANS.get(EthereumService.class).getTransaction(txId);
          addRow(txId, tx, pageData);
        });

    return pageData;
  }

  private void addRow(String txId, Transaction tx, TransactionTablePageData pageData) {
    TransactionTableRowData rowData = pageData.addRow();
    rowData.setId(txId);
    rowData.setFrom(tx.getFromAddress());
    rowData.setTo(tx.getToAddress());
    BigDecimal valueEther = Convert.fromWei(new BigDecimal(tx.getValue()), Unit.ETHER);
    rowData.setValue(valueEther);
    rowData.setStatus(tx.getStatus());
    rowData.setHash(tx.getHash());
    TransactionReceipt receipt = tx.getTransactionReceipt();
    if (receipt != null) {
      rowData.setBlock(receipt.getBlockNumber().longValue());
    }
  }

  @Override
  public void refresh(String transactionId) {
    BEANS.get(EthereumService.class).refreshStatus(transactionId);
  }
}
