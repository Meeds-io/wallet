package org.exoplatform.addon.wallet.scheduled;

import static org.exoplatform.addon.wallet.utils.WalletUtils.EMPTY_HASH;
import static org.exoplatform.addon.wallet.utils.WalletUtils.NEW_TRANSACTION_EVENT;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.web3j.protocol.core.methods.response.Transaction;

import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.service.EthereumClientConnector;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@DisallowConcurrentExecution
public class PendingTransactionVerifierJob implements Job {

  private static final Log         LOG = ExoLogger.getLogger(PendingTransactionVerifierJob.class);

  private EthereumClientConnector  ethereumClientConnector;

  private WalletTransactionService transactionService;

  private ListenerService          listenerService;

  private ExoContainer             container;

  public PendingTransactionVerifierJob() {
    this(PortalContainer.getInstance());
  }

  public PendingTransactionVerifierJob(ExoContainer container) {
    this.container = container;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      List<TransactionDetail> pendingTransactions = getTransactionService().getPendingTransactions();
      if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
        LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database",
                  pendingTransactions.size());
        long pendingTransactionMaxDays = getTransactionService().getPendingTransactionMaxDays();
        for (TransactionDetail pendingTransactionDetail : pendingTransactions) {
          verifyTransactionStatusOnBlockchain(pendingTransactionDetail, pendingTransactionMaxDays);
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking pending transactions", e);
    } finally {
      RequestLifeCycle.end();
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private void verifyTransactionStatusOnBlockchain(TransactionDetail pendingTransactionDetail, long pendingTransactionMaxDays) {
    String hash = pendingTransactionDetail.getHash();
    try {
      Transaction transaction = getEthereumClientConnector().getTransaction(hash);
      String blockHash = transaction == null ? null : transaction.getBlockHash();
      if (!StringUtils.isBlank(blockHash)
          && !StringUtils.equalsIgnoreCase(EMPTY_HASH, blockHash)
          && transaction.getBlockNumber() != null) {
        getListenerService().broadcast(NEW_TRANSACTION_EVENT, transaction, null);
      } else if (pendingTransactionMaxDays > 0) {
        long creationTimestamp = pendingTransactionDetail.getTimestamp();
        if (transaction == null && creationTimestamp > 0) {
          Duration duration = Duration.ofMillis(System.currentTimeMillis() - creationTimestamp);
          if (duration.toDays() >= pendingTransactionMaxDays) {
            LOG.info("Transaction '{}' was not found on blockchain for more than '{}' days, so mark it as failed",
                     hash,
                     pendingTransactionMaxDays);
            getListenerService().broadcast(NEW_TRANSACTION_EVENT, hash, null);
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Error treating pending transaction: {}", hash, e);
    }
  }

  private EthereumClientConnector getEthereumClientConnector() {
    if (ethereumClientConnector == null) {
      ethereumClientConnector = CommonsUtils.getService(EthereumClientConnector.class);
    }
    return ethereumClientConnector;
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }
}
