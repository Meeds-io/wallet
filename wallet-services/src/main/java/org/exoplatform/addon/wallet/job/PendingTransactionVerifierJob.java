package org.exoplatform.addon.wallet.job;

import org.quartz.*;

import org.exoplatform.addon.wallet.service.BlockchainTransactionService;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class PendingTransactionVerifierJob implements Job {

  private static final Log             LOG = ExoLogger.getLogger(PendingTransactionVerifierJob.class);

  private ExoContainer                 container;

  private WalletTransactionService     walletTransactionService;

  private BlockchainTransactionService blockchainTransactionService;

  public PendingTransactionVerifierJob() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      getBlockchainTransactionService().checkPendingTransactions(getWalletTransactionService().getPendingTransactionMaxDays());
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private BlockchainTransactionService getBlockchainTransactionService() {
    if (blockchainTransactionService == null) {
      blockchainTransactionService = CommonsUtils.getService(BlockchainTransactionService.class);
    }
    return blockchainTransactionService;
  }

  private WalletTransactionService getWalletTransactionService() {
    if (walletTransactionService == null) {
      walletTransactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return walletTransactionService;
  }
}
