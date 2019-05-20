package org.exoplatform.addon.wallet.listener;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.service.WalletTokenAdminService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class ModifiedAdminWalletListener extends Listener<Wallet, Wallet> {

  private WalletTokenAdminService tokenTransactionService;

  @Override
  public void onEvent(Event<Wallet, Wallet> event) throws Exception {
    getTokenTransactionService().reinit();
  }

  private WalletTokenAdminService getTokenTransactionService() {
    if (tokenTransactionService == null) {
      tokenTransactionService = CommonsUtils.getService(WalletTokenAdminService.class);
    }
    return tokenTransactionService;
  }

}
