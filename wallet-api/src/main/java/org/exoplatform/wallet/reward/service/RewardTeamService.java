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
package org.exoplatform.wallet.reward.service;

import java.util.List;

import org.exoplatform.wallet.model.reward.RewardTeam;

/**
 * A storage service to save/load reward teams
 */
public interface RewardTeamService {

  /**
   * @return {@link List} of reward teams of type {@link RewardTeam}
   */
  public List<RewardTeam> getTeams();

  /**
   * Update or create a reward team
   * 
   * @param rewardTeam reward to save
   * @return saved reward team
   */
  public RewardTeam saveTeam(RewardTeam rewardTeam);

  /**
   * Remove a reward Team/Pool by id
   * 
   * @param id Team technical ID
   * @return removed reward team
   */
  public RewardTeam removeTeam(Long id);

  /**
   * Find reward teams by identity id
   * 
   * @param identityId user social identity id
   * @return {@link List} of {@link RewardTeam}
   */
  public List<RewardTeam> findTeamsByMemberId(long identityId);
}
