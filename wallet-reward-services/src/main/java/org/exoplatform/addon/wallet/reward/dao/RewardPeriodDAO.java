package org.exoplatform.addon.wallet.reward.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.addon.wallet.model.reward.RewardPeriodType;
import org.exoplatform.addon.wallet.reward.entity.WalletRewardPeriodEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class RewardPeriodDAO extends GenericDAOJPAImpl<WalletRewardPeriodEntity, Long> {

  public WalletRewardPeriodEntity findRewardPeriodByTypeAndTime(RewardPeriodType periodType, long periodTime) {
    TypedQuery<WalletRewardPeriodEntity> query = getEntityManager().createNamedQuery("RewardPeriod.getPeriodByTypeAndTime",
                                                                                     WalletRewardPeriodEntity.class);
    query.setParameter("periodType", periodType);
    query.setParameter("periodTime", periodTime);
    List<WalletRewardPeriodEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

}
