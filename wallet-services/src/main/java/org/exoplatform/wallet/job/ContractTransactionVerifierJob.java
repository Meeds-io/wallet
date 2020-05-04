package org.exoplatform.wallet.job;

import org.quartz.*;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletAccountService;

@DisallowConcurrentExecution
public class ContractTransactionVerifierJob implements Job {

  private static final Log             LOG = ExoLogger.getLogger(ContractTransactionVerifierJob.class);

  private ExoContainer                 container;

  private BlockchainTransactionService blockchainTransactionService;

  private WalletAccountService         walletAccountService;

  public ContractTransactionVerifierJob() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      // Refresh gas price only when admin wallet has been initialized
      Wallet adminWallet = getWalletAccountService().getAdminWallet();
      if (adminWallet != null && adminWallet.getIsInitialized() != null && adminWallet.getIsInitialized().booleanValue()) {
        getBlockchainTransactionService().scanNewerBlocks();
      }
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

  public WalletAccountService getWalletAccountService() {
    if (walletAccountService == null) {
      walletAccountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return walletAccountService;
  }
}
