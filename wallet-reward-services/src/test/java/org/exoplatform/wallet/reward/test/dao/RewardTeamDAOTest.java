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
