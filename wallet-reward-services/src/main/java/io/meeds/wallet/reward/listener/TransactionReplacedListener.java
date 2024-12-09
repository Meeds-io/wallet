package io.meeds.wallet.reward.listener;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.common.ContainerTransactional;
import io.meeds.wallet.reward.service.RewardReportService;

import jakarta.annotation.PostConstruct;

/**
 * A listener that is triggered when a transaction gets replaced/boosted
 */
@Component
public class TransactionReplacedListener extends Listener<Object, Map<String, String>> {

  private static final Log    LOG        = ExoLogger.getLogger(TransactionReplacedListener.class);

  private static final String EVENT_NAME = "exo.wallet.transaction.replaced";

  @Autowired
  private RewardReportService rewardReportService;

  @Autowired
  private ListenerService     listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener(EVENT_NAME, this);
  }

  @ContainerTransactional
  @Override
  public void onEvent(Event<Object, Map<String, String>> event) {
    String oldHash = null;
    String newHash = null;
    try {
      Map<String, String> transaction = event.getData();
      oldHash = transaction.get("oldHash");
      newHash = transaction.get("hash");
      rewardReportService.replaceRewardTransactions(oldHash, newHash);
    } catch (Exception e) {
      LOG.error("Error while replacing Reward transaction from old hash {} to new hash {}", oldHash, newHash, e);
    }
  }
}
