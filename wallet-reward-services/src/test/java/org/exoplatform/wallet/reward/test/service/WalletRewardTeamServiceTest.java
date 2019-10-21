package org.exoplatform.wallet.reward.test.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.reward.service.WalletRewardTeamService;
import org.exoplatform.wallet.reward.test.BaseWalletRewardTest;

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
