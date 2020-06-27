package org.exoplatform.wallet.reward.listener;

import java.util.Map;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.reward.service.WalletRewardReportService;

/**
 * A listener that is triggered when a transaction gets replaced/boosted
 */
public class TransactionReplacedListener extends Listener<Object, Map<String, String>> {
  private static final Log          LOG = ExoLogger.getLogger(TransactionReplacedListener.class);

  private WalletRewardReportService walletRewardReportService;

  @Override
  public void onEvent(Event<Object, Map<String, String>> event) throws Exception {
    String oldHash = null;
    String newHash = null;
    PortalContainer container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      Map<String, String> transaction = event.getData();
      oldHash = transaction.get("oldHash");
      newHash = transaction.get("hash");
      getWalletRewardReportService().replaceRewardTransactions(oldHash, newHash);
    } catch (Exception e) {
      LOG.error("Error while replacing Reward transaction from old hash {} to new hash {}", oldHash, newHash, e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  private WalletRewardReportService getWalletRewardReportService() {
    if (walletRewardReportService == null) {
      walletRewardReportService = ExoContainerContext.getService(WalletRewardReportService.class);
    }
    return walletRewardReportService;
  }
}
