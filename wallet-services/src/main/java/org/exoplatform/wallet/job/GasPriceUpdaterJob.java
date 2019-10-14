package org.exoplatform.wallet.job;

import org.quartz.*;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletService;

@DisallowConcurrentExecution
public class GasPriceUpdaterJob implements Job {

  private static final Log             LOG = ExoLogger.getLogger(GasPriceUpdaterJob.class);

  private ExoContainer                 container;

  private BlockchainTransactionService blockchainTransactionService;

  private WalletService                walletService;

  public GasPriceUpdaterJob() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      long blockchainGasPrice = getBlockchainTransactionService().refreshBlockchainGasPrice();
      getWalletService().setDynamicGasPrice(blockchainGasPrice);
    } catch (Exception e) {
      LOG.error("Error while refrshing gas price", e);
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

  public WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }
}
