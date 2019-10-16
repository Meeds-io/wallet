/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wallet.reward.service;

import java.util.List;

import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.reward.storage.RewardTeamStorage;

/**
 * A service to manage reward teams
 */
public class WalletRewardTeamService implements RewardTeamService {

  private RewardTeamStorage rewardTeamStorage;

  public WalletRewardTeamService(RewardTeamStorage rewardTeamStorage) {
    this.rewardTeamStorage = rewardTeamStorage;
  }

  @Override
  public List<RewardTeam> getTeams() {
    return this.rewardTeamStorage.getTeams();
  }

  @Override
  public RewardTeam saveTeam(RewardTeam rewardTeam) {
    if (rewardTeam == null) {
      throw new IllegalArgumentException("Empty team to save");
    }
    return this.rewardTeamStorage.saveTeam(rewardTeam);
  }

  @Override
  public RewardTeam removeTeam(Long id) {
    if (id == null || id == 0) {
      throw new IllegalArgumentException("Team id is required");
    }
    return this.rewardTeamStorage.removeTeam(id);
  }

  @Override
  public List<RewardTeam> findTeamsByMemberId(long identityId) {
    if (identityId == 0) {
      throw new IllegalArgumentException("User identity id is required");
    }
    return this.rewardTeamStorage.findTeamsByMemberId(identityId);
  }

}
