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

import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.reward.entity.WalletRewardPluginEntity;

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
