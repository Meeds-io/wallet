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

import javax.persistence.TypedQuery;

import org.apache.commons.collections.CollectionUtils;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardStatus;
import org.exoplatform.wallet.reward.entity.WalletRewardPeriodEntity;

public class RewardPeriodDAO extends GenericDAOJPAImpl<WalletRewardPeriodEntity, Long> {

  public List<WalletRewardPeriodEntity> findRewardPeriods(int offset, int limit) {
    TypedQuery<WalletRewardPeriodEntity> query = getEntityManager().createNamedQuery("RewardPeriod.findRewardPeriods",
                                                                                     WalletRewardPeriodEntity.class);
    if (offset > 0) {
      query.setFirstResult(offset);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    List<WalletRewardPeriodEntity> result = query.getResultList();
    return CollectionUtils.isEmpty(result) ? Collections.emptyList() : result;
  }

  public WalletRewardPeriodEntity findRewardPeriodByTypeAndTime(RewardPeriodType periodType, long periodTime) {
    TypedQuery<WalletRewardPeriodEntity> query = getEntityManager().createNamedQuery("RewardPeriod.findRewardPeriodByTypeAndTime",
                                                                                     WalletRewardPeriodEntity.class);
    query.setParameter("periodType", periodType);
    query.setParameter("periodTime", periodTime);
    List<WalletRewardPeriodEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

  public List<WalletRewardPeriodEntity> findRewardPeriodsByStatus(RewardStatus status) {
    TypedQuery<WalletRewardPeriodEntity> query = getEntityManager().createNamedQuery("RewardPlugin.findRewardPeriodsByStatus",
                                                                                     WalletRewardPeriodEntity.class);
    query.setParameter("status", status);
    List<WalletRewardPeriodEntity> resultList = query.getResultList();
    return resultList == null ? Collections.emptyList() : resultList;
  }

}
