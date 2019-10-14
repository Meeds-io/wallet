package org.exoplatform.wallet.job;

import org.quartz.*;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.service.BlockchainTransactionService;

@DisallowConcurrentExecution
public class TransactionSenderJob implements Job {

  private static final Log             LOG = ExoLogger.getLogger(TransactionSenderJob.class);

  private ExoContainer                 container;

  private BlockchainTransactionService blockchainTransactionService;

  public TransactionSenderJob() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      getBlockchainTransactionService().sendRawTransactions();
    } catch (Exception e) {
      LOG.error("Error while sending raw transactions", e);
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
}
