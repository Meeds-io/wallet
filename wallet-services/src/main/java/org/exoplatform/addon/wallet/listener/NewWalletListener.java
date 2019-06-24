/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.wallet.listener;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.WalletInitializationState;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.service.WalletTokenAdminService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;

/**
 * This listener will be triggered when a new address is associated to a user or
 * a space.
 */
@Asynchronous
public class NewWalletListener extends Listener<Wallet, Wallet> {

  private WalletAccountService    walletAccountService;

  private WalletTokenAdminService tokenTransactionService;

  private ExoContainer            container;

  public NewWalletListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Wallet, Wallet> event) throws Exception {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      String contractAddress = getTokenTransactionService().getContractAddress();
      if (StringUtils.isBlank(contractAddress)) {
        return;
      }
      Wallet wallet = event.getData();
      String walletAddress = wallet.getAddress();
      boolean initializedWallet = getTokenTransactionService().isInitializedAccount(walletAddress)
          || getTokenTransactionService().isApprovedAccount(walletAddress);
      if (initializedWallet) {
        wallet.setInitializationState(WalletInitializationState.INITIALIZED.name());
      } else {
        wallet.setInitializationState(WalletInitializationState.NEW.name());
      }
      getWalletAccountService().saveWallet(wallet);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private WalletAccountService getWalletAccountService() {
    if (walletAccountService == null) {
      walletAccountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return walletAccountService;
  }

  private WalletTokenAdminService getTokenTransactionService() {
    if (tokenTransactionService == null) {
      tokenTransactionService = CommonsUtils.getService(WalletTokenAdminService.class);
    }
    return tokenTransactionService;
  }

}
