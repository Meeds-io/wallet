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

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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

  private static WalletRewardEntity getFirstItem(List<WalletRewardEntity> resultList) {
    if (CollectionUtils.isEmpty(resultList)) {
      return null;
    } else {
      return resultList.stream().filter(r -> StringUtils.isNotBlank(r.getTransactionHash())).sorted((r2, r1) -> {
        if (r1.getTokensSent() > r2.getTokensSent()) {
          return 1;
        } else if (r2.getTokensSent() > r1.getTokensSent()) {
          return -1;
        } else {
          return 0;
        }
      }).findFirst().orElse(resultList.get(0));
    }
  }

}
