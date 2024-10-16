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
package io.meeds.wallet.reward.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import io.meeds.wallet.wallet.model.reward.RewardPeriod;
import io.meeds.wallet.wallet.model.reward.RewardPeriodType;
import io.meeds.wallet.wallet.model.reward.RewardReport;
import io.meeds.wallet.wallet.model.reward.WalletReward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * A storage service to save/load reward settings
 */
@Service
public interface RewardReportService {

  /**
   * Compute rewards swicth configurations for the list of identities passed in
   * parameters
   * 
   * @param localDate a {@link LocalDate} inside the period time
   *          that will be retrieved
   * @return a {@link Set} of {@link WalletReward} with the details of sent
   *         tokens and tokens to send
   */
  RewardReport computeRewards(LocalDate localDate);

  /**
   * Compute rewards By User
   *
   * @param localDate a {@link LocalDate} inside the period time
   *          that will be retrieved
   * @param userIdentityId User technical identifier ({@link Identity#getId()})
   * @return a {@link Set} of {@link WalletReward} with the details of sent
   *         tokens and tokens to send
   */
  RewardReport computeRewardsByUser(LocalDate localDate, long userIdentityId);

  /**
   * Send rewards transactions
   * 
   * @param localDate a {@link LocalDate} inside the period time
   *          that will be retrieved
   * @param username current username sending rewards
   * @throws Exception if an error occurs while sending the rewards transactions
   *           on blockchain
   */
  void sendRewards(LocalDate localDate, String username) throws Exception; // NOSONAR

  /**
   * Retrieve a {@link RewardReport} corresponding to a period of time
   * 
   * @param localDate a {@link LocalDate} inside the period time
   *          that will be retrieved
   * @return {@link RewardReport} if there is a saved one, else null
   */
  RewardReport getRewardReport(LocalDate localDate);

  /**
   * Return the stored reward period associated to select period type and date
   * 
   * @param periodType {@link RewardPeriodType}
   * @param date {@link LocalDate}
   * @return {@link RewardPeriod}
   */
  RewardPeriod getRewardPeriod(RewardPeriodType periodType, LocalDate date);

  /**
   * Retrieve a {@link RewardReport} corresponding to a period identified by its id
   * 
   * @param periodId technical identifier of {@link RewardPeriod}
   * @return {@link RewardReport}
   */
  RewardReport getRewardReportByPeriodId(long periodId);

  /**
   * @param rewardReport save generated reward report
   */
  void saveRewardReport(RewardReport rewardReport);

  /**
   * @return true if reward sending status storage is in progress, else return false
   */
  boolean isRewardSendingInProgress();

  /**
   * @return a {@link List} of {@link RewardPeriod} that are in progress
   */
  List<RewardPeriod> getRewardPeriodsInProgress();

  /*
   * Return list of reward reports not sent yet
   */
  List<RewardPeriod> getRewardPeriodsNotSent();

  /**
   * Retrieves the list of periods sorted descending by start date
   *
   * @param pageable {@link Pageable} the page to be returned.
   * @return {@link Page} of {@link RewardPeriod}
   */
  Page<RewardPeriod> findRewardReportPeriods(Pageable pageable);

  /**
   * Retrieves the list of periods by interval sorted descending by start date
   *
   * @param from from date
   * @param to to date
   * @param pageable {@link Pageable} the page to be returned.
   * @return {@link Page} of {@link RewardPeriod}
   */
  Page<RewardPeriod> findRewardPeriodsBetween(long from, long to, Pageable pageable);

  /**
   * @param currentUser current user listing his rewards
   * @param limit size limit of items to return
   * @return a {@link List} of {@link WalletReward} of current user
   */
  List<WalletReward> listRewards(String currentUser, int limit);

  /**
   * @param currentUser the current user listing his rewards
   * @return a total rewards sent  for current person
   */
  double countRewards(String currentUser);

  /**
   * Replaces an old reward transaction hash to a new one, that had boosted the
   * first one
   * 
   * @param oldHash old Transaction hash
   * @param newHash new Transaction hash
   */
  void replaceRewardTransactions(String oldHash, String newHash);

}
