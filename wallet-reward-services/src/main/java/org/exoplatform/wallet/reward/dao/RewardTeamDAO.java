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

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.reward.entity.RewardTeamEntity;

public class RewardTeamDAO extends GenericDAOJPAImpl<RewardTeamEntity, Long> {

  public List<RewardTeamEntity> findTeamsByMemberId(long identityId) {
    TypedQuery<RewardTeamEntity> query = getEntityManager().createNamedQuery("RewardTeam.findTeamsByMemberId",
                                                                             RewardTeamEntity.class);
    query.setParameter("identityId", identityId);
    List<RewardTeamEntity> result = query.getResultList();
    return result == null ? Collections.emptyList() : result;
  }

  public List<RewardTeamEntity> findNotDeletedTeams() {
    TypedQuery<RewardTeamEntity> query = getEntityManager().createNamedQuery("RewardTeam.findNoDeletedTeams",
                                                                             RewardTeamEntity.class);
    List<RewardTeamEntity> result = query.getResultList();
    return result == null ? Collections.emptyList() : result;
  }

}
