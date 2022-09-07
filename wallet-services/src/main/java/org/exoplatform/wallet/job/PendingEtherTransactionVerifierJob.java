/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
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
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

/**
 * A job that will refresh periodically ether transactions sent by admin wallet
 * from blockchain
 */
@DisallowConcurrentExecution
public class PendingEtherTransactionVerifierJob implements Job {

  private static final Log             LOG = ExoLogger.getLogger(PendingEtherTransactionVerifierJob.class);

  private ExoContainer                 container;

  private BlockchainTransactionService blockchainTransactionService;

  private WalletTransactionService     walletTransactionService;

  private WalletAccountService         walletAccountService;

  public PendingEtherTransactionVerifierJob() {
    this.container = PortalContainer.getInstance();
    this.blockchainTransactionService = this.container.getComponentInstanceOfType(BlockchainTransactionService.class);
    this.walletTransactionService = this.container.getComponentInstanceOfType(WalletTransactionService.class);
    this.walletAccountService = this.container.getComponentInstanceOfType(WalletAccountService.class);
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      Wallet adminWallet = walletAccountService.getAdminWallet();
      if (adminWallet != null) {
        List<TransactionDetail> transactionDetails =
                                                   walletTransactionService.getPendingEtherTransactions(adminWallet.getAddress());
        if (CollectionUtils.isNotEmpty(transactionDetails)) {
          transactionDetails.forEach(transactionDetail -> blockchainTransactionService.refreshTransactionFromBlockchain(transactionDetail.getHash()));
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }
}
