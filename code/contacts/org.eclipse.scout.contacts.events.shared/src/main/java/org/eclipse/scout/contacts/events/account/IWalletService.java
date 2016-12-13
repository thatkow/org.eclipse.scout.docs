package org.eclipse.scout.contacts.events.account;

import org.eclipse.scout.rt.platform.ApplicationScoped;
import org.eclipse.scout.rt.shared.TunnelToServer;

@ApplicationScoped
@TunnelToServer
public interface IWalletService {

  public String create(String walletPath, String password);

  public String createAliceLenaTransaction(String personId);

  public void sendTransaction(String transactionId);

}
