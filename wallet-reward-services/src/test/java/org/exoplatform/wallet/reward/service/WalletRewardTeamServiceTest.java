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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.reward.BaseWalletRewardTest;

public class WalletRewardTeamServiceTest extends BaseWalletRewardTest {

  /**
   * Check that service is instantiated
   */
  @Test
  public void testServiceInstantiated() {
    WalletRewardTeamService rewardTeamService = getService(WalletRewardTeamService.class);
    assertNotNull(rewardTeamService);
  }

  @Test
  public void testGetTeams() {
    WalletRewardTeamService rewardTeamService = getService(WalletRewardTeamService.class);

    List<RewardTeam> teams = rewardTeamService.getTeams();
    assertNotNull(teams);
    assertEquals(0, teams.size());
  }

  @Test
  public void testSaveTeam() {
    WalletRewardTeamService rewardTeamService = getService(WalletRewardTeamService.class);

    RewardTeam rewardTeam = newRewardTeam();

    rewardTeam = rewardTeamService.saveTeam(rewardTeam);

    entitiesToClean.add(rewardTeam);

    List<RewardTeam> teams = rewardTeamService.getTeams();
    assertEquals(1, teams.size());

    RewardTeam savedTeam = teams.iterator().next();
    assertNotNull(savedTeam.getId());
    assertTrue(savedTeam.getId() > 0);
    assertEquals(TEAM_NAME, savedTeam.getName());
    assertEquals(TEAM_DESCRIPTION, savedTeam.getDescription());
    assertNull(savedTeam.getSpaceId());
    assertEquals(TEAM_BUDGET, savedTeam.getBudget(), 0);
    assertEquals(TEAM_BUDGET_TYPE, savedTeam.getRewardType());
    assertNotNull(savedTeam.getManager());
    assertEquals(MANAGER_IDENTITY_ID, savedTeam.getManager().getIdentityId(), 0);

    assertNotNull(savedTeam.getMembers());
    assertEquals(1, savedTeam.getMembers().size());
    assertNotNull(savedTeam.getMembers().get(0));
    assertEquals(MEMBER_IDENTITY_ID, savedTeam.getMembers().get(0).getIdentityId(), 0);
  }

  @Test
  public void testRemoveTeam() {
    WalletRewardTeamService rewardTeamService = getService(WalletRewardTeamService.class);

    RewardTeam rewardTeam = newRewardTeam();

    rewardTeam = rewardTeamService.saveTeam(rewardTeam);
    assertFalse(rewardTeam.isDeleted());
    List<RewardTeam> teams = rewardTeamService.getTeams();
    assertEquals(1, teams.size());

    rewardTeamService.removeTeam(rewardTeam.getId());
    teams = rewardTeamService.getTeams();
    assertEquals(0, teams.size());

    rewardTeam = rewardTeamService.getTeamsById(rewardTeam.getId());
    assertTrue(rewardTeam.isDeleted());

    entitiesToClean.add(rewardTeam);
  }

}
