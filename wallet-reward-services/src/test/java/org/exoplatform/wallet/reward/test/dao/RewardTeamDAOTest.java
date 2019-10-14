package org.exoplatform.wallet.reward.test.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import org.exoplatform.wallet.reward.dao.RewardTeamDAO;
import org.exoplatform.wallet.reward.entity.RewardTeamEntity;
import org.exoplatform.wallet.reward.test.BaseWalletRewardTest;

public class RewardTeamDAOTest extends BaseWalletRewardTest {

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    RewardTeamDAO rewardTeamDAO = getService(RewardTeamDAO.class);
    assertNotNull(rewardTeamDAO);

    List<RewardTeamEntity> teamEntities = rewardTeamDAO.findAll();
    assertNotNull("Returned teams list shouldn't be null", teamEntities);
    assertEquals("Returned teams should be empty", 0, teamEntities.size());
  }

}
