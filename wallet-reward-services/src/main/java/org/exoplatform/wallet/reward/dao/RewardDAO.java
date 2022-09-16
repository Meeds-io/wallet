/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
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
package org.exoplatform.wallet.reward.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.reward.entity.WalletRewardEntity;

public class RewardDAO extends GenericDAOJPAImpl<WalletRewardEntity, Long> {
  private static final Log    LOG         = ExoLogger.getLogger(RewardDAO.class);

  private static final String IDENTITY_ID = "identityId";

  public List<WalletRewardEntity> findRewardsByPeriodId(long periodId) {
    TypedQuery<WalletRewardEntity> query = getEntityManager().createNamedQuery("Reward.findRewardsByPeriodId",
                                                                               WalletRewardEntity.class);
    query.setParameter("periodId", periodId);
    List<WalletRewardEntity> result = query.getResultList();
    return toNotNullList(result);
  }

  public List<WalletRewardEntity> findRewardsByIdentityId(long identityId, int limit) {
    TypedQuery<WalletRewardEntity> query = getEntityManager().createNamedQuery("Reward.findRewardsByIdentityId",
                                                                               WalletRewardEntity.class);
    query.setParameter(IDENTITY_ID, identityId);
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    List<WalletRewardEntity> result = query.getResultList();
    return toNotNullList(result);
  }

  public double countRewardsByIdentityId(long identityId) {
    TypedQuery<Double> query = getEntityManager().createNamedQuery("Reward.countRewardsByIdentityId", Double.class);
    query.setParameter(IDENTITY_ID, identityId);
    Double result = query.getSingleResult();
    return toNotNullDouble(result);
  }

  public WalletRewardEntity findRewardByIdentityIdAndPeriodId(long identityId, long periodId) {
    TypedQuery<WalletRewardEntity> query = getEntityManager().createNamedQuery("Reward.findRewardByIdentityIdAndPeriodId",
                                                                               WalletRewardEntity.class);
    query.setParameter("periodId", periodId);
    query.setParameter(IDENTITY_ID, identityId);
    List<WalletRewardEntity> result = query.getResultList();
    if (result == null || result.isEmpty()) {
      return null;
    } else if (result.size() > 1) {
      LOG.warn("More than one reward was found for identityId {} and periodId {}", identityId, periodId);
    }
    return getFirstItem(result);
  }

  @ExoTransactional
  public void replaceRewardTransactions(String oldHash, String newHash) {
    Query query = getEntityManager().createNamedQuery("Reward.updateTransactionHash");
    query.setParameter("oldHash", oldHash);
    query.setParameter("newHash", newHash);
    query.executeUpdate();
  }

  private List<WalletRewardEntity> toNotNullList(List<WalletRewardEntity> result) {
    return result == null ? Collections.emptyList() : result;
  }

  private double toNotNullDouble(Double result) {
    return result == null ? 0 : result;
  }

  private WalletRewardEntity getFirstItem(List<WalletRewardEntity> resultList) {
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

}
