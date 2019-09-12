/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.addon.wallet.reward.service;

import java.util.Set;

import org.exoplatform.addon.wallet.model.reward.RewardReport;
import org.exoplatform.addon.wallet.model.reward.WalletReward;

/**
 * A storage service to save/load reward settings
 */
public interface RewardService {

  /**
   * Compute rewards swicth configurations for the list of identities passed in
   * parameters
   * 
   * @param periodDateInSeconds a timestamp in seconds inside the period time
   *          that will be retrieved
   * @return a {@link Set} of {@link WalletReward} with the details of sent
   *         tokens and tokens to send
   */
  public RewardReport getRewardReport(long periodDateInSeconds);

  /**
   * Send rewards transactions
   * 
   * @param periodDateInSeconds a timestamp in seconds inside the period time
   *          that will be retrieved
   * @param username current username sending rewards
   * @throws Exception if an error occurs while sending the rewards transactions
   *           on blockchain
   */
  public void sendRewards(long periodDateInSeconds, String username) throws Exception; // NOSONAR

}
