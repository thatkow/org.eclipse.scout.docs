package org.eclipse.scout.contacts.events.account;

import org.eclipse.scout.rt.platform.service.IService;
import org.eclipse.scout.rt.shared.TunnelToServer;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

/**
 * <h3>{@link IAccountService}</h3>
 *
 * @author mzi
 */
@TunnelToServer
public interface IAccountService extends IService {

  /**
   * @param filter
   * @param personId
   * @return
   */
  AccountTablePageData getAccountTableData(SearchFilter filter, String personId);
}
