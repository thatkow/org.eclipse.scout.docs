package org.eclipse.scout.contacts.events.account;

import java.math.BigDecimal;

import org.eclipse.scout.contacts.events.account.AccountTablePageData.AccountTableRowData;
import org.eclipse.scout.contacts.events.account.model.Wallet;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

public class AccountService implements IAccountService {

  @Override
  public AccountTablePageData getAccountTableData(SearchFilter filter, String personId) {
    AccountTablePageData pageData = new AccountTablePageData();

    BEANS.get(EthereumService.class).getWallets()
        .stream()
        .forEach(address -> {
          Wallet wallet = BEANS.get(EthereumService.class).getWallet(address);

          if (personId == null || personId.equals(wallet.getPersonId())) {
            addRow(wallet.getPersonId(), wallet.getName(), address, pageData);
          }
        });

    if (personId == null) {
      addRow(null, "Dummy 1", "0x0731F6b07eA5a2143E8EDd7C75E52a4f7d42E244", pageData);
      addRow(null, "Dummy 2", "0x61B2feE671a2f20E7ed04be9af076BeB356b0702", pageData);
    }

    return pageData;
  }

  private void addRow(String personId, String name, String address, AccountTablePageData pageData) {
    BigDecimal balance = BEANS.get(EthereumService.class).getBalance(address);
    AccountTableRowData rowData = pageData.addRow();
    rowData.setPerson(personId);
    rowData.setAccountName(name);
    rowData.setAddress(address);
    rowData.setBalance(balance);
  }
}
