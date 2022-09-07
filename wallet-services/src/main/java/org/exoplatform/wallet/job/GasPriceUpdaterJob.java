/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.wallet.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletService;

@DisallowConcurrentExecution
public class GasPriceUpdaterJob implements Job {

  private static final Log             LOG = ExoLogger.getLogger(GasPriceUpdaterJob.class);

  private PortalContainer              container;

  private BlockchainTransactionService blockchainTransactionService;

  private WalletService                walletService;

  private WalletAccountService         walletAccountService;

  public GasPriceUpdaterJob() {
    this.container = PortalContainer.getInstance();
    this.blockchainTransactionService = this.container.getComponentInstanceOfType(BlockchainTransactionService.class);
    this.walletService = this.container.getComponentInstanceOfType(WalletService.class);
    this.walletAccountService = this.container.getComponentInstanceOfType(WalletAccountService.class);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    if (walletService.isUseDynamicGasPrice()) {
      ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(this.container);
      try {
        // Refresh gas price only when admin wallet has been initialized
        Wallet adminWallet = walletAccountService.getAdminWallet();
        if (adminWallet != null && Boolean.TRUE.equals(adminWallet.getIsInitialized())) {
          long blockchainGasPrice = blockchainTransactionService.refreshBlockchainGasPrice();
          walletService.setDynamicGasPrice(blockchainGasPrice);
        }
      } catch (Exception e) {
        LOG.error("Error while refreshing gas price", e);
      } finally {
        RequestLifeCycle.end();
        ExoContainerContext.setCurrentContainer(currentContainer);
      }
    }
  }

}
