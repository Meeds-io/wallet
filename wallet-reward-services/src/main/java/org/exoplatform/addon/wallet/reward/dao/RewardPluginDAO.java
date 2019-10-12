package org.exoplatform.addon.wallet.reward.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.addon.wallet.reward.entity.WalletRewardPluginEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class RewardPluginDAO extends GenericDAOJPAImpl<WalletRewardPluginEntity, Long> {
  private static final Log LOG = ExoLogger.getLogger(RewardPluginDAO.class);

  public List<WalletRewardPluginEntity> getRewardPluginsByRewardId(long rewardId) {
    TypedQuery<WalletRewardPluginEntity> query = getEntityManager().createNamedQuery("RewardPlugin.getRewardPluginsByRewardId",
                                                                                     WalletRewardPluginEntity.class);
    query.setParameter("rewardId", rewardId);
    return query.getResultList();
  }

  public WalletRewardPluginEntity getRewardPluginsByRewardIdAndPluginId(long rewardId, String pluginId) {
    TypedQuery<WalletRewardPluginEntity> query =
                                               getEntityManager().createNamedQuery("RewardPlugin.getRewardPluginsByRewardIdAndPluginId",
                                                                                   WalletRewardPluginEntity.class);
    query.setParameter("rewardId", rewardId);
    query.setParameter("pluginId", pluginId);
    List<WalletRewardPluginEntity> result = query.getResultList();
    if (result == null || result.isEmpty()) {
      return null;
    } else if (result.size() > 1) {
      LOG.warn("More than one reward plugin was found for rewardId {} and pluginId {}", rewardId, pluginId);
    }
    return result.get(0);
  }

}
