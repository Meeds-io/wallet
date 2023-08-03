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
package org.exoplatform.wallet.reward.storage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardStatus;
import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.model.reward.WalletPluginReward;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.dao.RewardDAO;
import org.exoplatform.wallet.reward.dao.RewardPeriodDAO;
import org.exoplatform.wallet.reward.dao.RewardPluginDAO;
import org.exoplatform.wallet.reward.dao.RewardTeamDAO;
import org.exoplatform.wallet.reward.entity.RewardTeamEntity;
import org.exoplatform.wallet.reward.entity.WalletRewardEntity;
import org.exoplatform.wallet.reward.entity.WalletRewardPeriodEntity;
import org.exoplatform.wallet.reward.entity.WalletRewardPluginEntity;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

public class WalletRewardReportStorage {

  private static final Log         LOG = ExoLogger.getLogger(WalletRewardReportStorage.class);

  private RewardPluginDAO          rewardPluginDAO;

  private RewardDAO                rewardDAO;

  private RewardPeriodDAO          rewardPeriodDAO;

  private RewardTeamDAO            rewardTeamDAO;

  private WalletRewardTeamStorage  rewardTeamStorage;

  private WalletAccountService     walletAccountService;

  private WalletTransactionService walletTransactionService;

  public WalletRewardReportStorage(RewardPluginDAO rewardPluginDAO,
                                   RewardDAO rewardDAO,
                                   RewardPeriodDAO rewardPeriodDAO,
                                   RewardTeamDAO rewardTeamDAO,
                                   WalletRewardTeamStorage rewardTeamStorage,
                                   WalletAccountService walletAccountService,
                                   WalletTransactionService walletTransactionService) {
    this.rewardPluginDAO = rewardPluginDAO;
    this.rewardDAO = rewardDAO;
    this.rewardPeriodDAO = rewardPeriodDAO;
    this.rewardTeamDAO = rewardTeamDAO;
    this.rewardTeamStorage = rewardTeamStorage;
    this.walletAccountService = walletAccountService;
    this.walletTransactionService = walletTransactionService;
  }

  public List<RewardPeriod> findRewardReportPeriods(int offset, int limit) {
    List<WalletRewardPeriodEntity> rewardPeriodEntities = rewardPeriodDAO.findRewardPeriods(offset, limit);
    return rewardPeriodEntities.stream().map(this::toDTO).toList();
  }

  public RewardReport getRewardReport(RewardPeriodType periodType, LocalDate date, ZoneId zoneId) {
    RewardPeriod period = periodType.getPeriodOfTime(date, zoneId);
    WalletRewardPeriodEntity rewardPeriodEntity = rewardPeriodDAO.findRewardPeriodByTypeAndTime(periodType,
                                                                                                period.getPeriodMedianDateInSeconds());
    if (rewardPeriodEntity == null) {
      return null;
    }
    RewardReport rewardReport = new RewardReport();
    RewardPeriod periodOfTime = rewardPeriodEntity.getPeriodType().getPeriodOfTime(date, zoneId);
    rewardReport.setPeriod(periodOfTime);

    List<WalletRewardEntity> rewardEntities = rewardDAO.findRewardsByPeriodId(rewardPeriodEntity.getId());
    if (rewardEntities != null) {
      Set<WalletReward> rewards = rewardEntities.stream()
                                                .map(rewardEntity -> toDTO(rewardEntity, zoneId))
                                                .collect(Collectors.toSet());
      rewardReport.setRewards(rewards);
    }
    return rewardReport;
  }

  public void saveRewardReport(RewardReport rewardReport) { // NOSONAR
    if (rewardReport == null) {
      throw new IllegalArgumentException("reward report is null");
    }
    RewardPeriod period = rewardReport.getPeriod();
    LOG.info("Saving reward report for period from {} to {}",
             period.getStartDateFormatted("en"),
             period.getEndDateFormatted("en"));

    WalletRewardPeriodEntity rewardPeriodEntity = rewardPeriodDAO.findRewardPeriodByTypeAndTime(period.getRewardPeriodType(),
                                                                                                period.getPeriodMedianDateInSeconds());
    if (rewardPeriodEntity == null) {
      rewardPeriodEntity = new WalletRewardPeriodEntity();
    } else if (rewardPeriodEntity.getStatus() == RewardStatus.SUCCESS) {
      LOG.warn("Reward report  from {} to {} shouldn't be modified because it has been already marked as completed.",
               period.getStartDateFormatted("en"),
               period.getEndDateFormatted("en"));
    }

    rewardPeriodEntity.setPeriodType(period.getRewardPeriodType());
    rewardPeriodEntity.setStartTime(period.getStartDateInSeconds());
    rewardPeriodEntity.setEndTime(period.getEndDateInSeconds());
    rewardPeriodEntity.setTimeZone(period.getTimeZone());

    if (rewardReport.isCompletelyProceeded()) {
      rewardPeriodEntity.setStatus(RewardStatus.SUCCESS);
    } else if (rewardReport.getPendingTransactionCount() > 0) { // Always
                                                                // pending if
                                                                // some tx
      // failed
      rewardPeriodEntity.setStatus(RewardStatus.PENDING);
    } else if (rewardReport.getFailedTransactionCount() > 0) { // If no failed
                                                               // and there are
      // somme errors
      rewardPeriodEntity.setStatus(RewardStatus.ERROR);
    } else if (rewardReport.getTransactionsCount() > 0) { // If some
                                                          // transactions
      // was sent
      rewardPeriodEntity.setStatus(RewardStatus.PENDING);
    } else {
      rewardPeriodEntity.setStatus(RewardStatus.ESTIMATION);
    }

    if (rewardPeriodEntity.getId() == null) {
      rewardPeriodEntity = rewardPeriodDAO.create(rewardPeriodEntity);
    } else {
      rewardPeriodEntity = rewardPeriodDAO.update(rewardPeriodEntity);
    }

    // No null check, it has been already checked by
    // rewardReport.countValidRewards()
    Set<WalletReward> rewards = rewardReport.getRewards();
    for (WalletReward walletReward : rewards) { // NOSONAR
      if (walletReward.getWallet() == null || StringUtils.isBlank(walletReward.getWallet().getAddress())) {
        continue;
      }
      long identityId = walletReward.getIdentityId();
      WalletRewardEntity rewardEntity = rewardDAO.findRewardByIdentityIdAndPeriodId(identityId, rewardPeriodEntity.getId());
      if (rewardEntity == null) {
        rewardEntity = new WalletRewardEntity();
      }
      rewardEntity.setTokensSent(walletReward.getTokensSent());
      rewardEntity.setTokensToSend(walletReward.getTokensToSend());
      rewardEntity.setEnabled(walletReward.isEnabled());
      rewardEntity.setIdentityId(identityId);
      rewardEntity.setPeriod(rewardPeriodEntity);

      TransactionDetail rewardTransaction = walletReward.getTransaction();
      rewardEntity.setTransactionHash(rewardTransaction == null ? null : rewardTransaction.getHash());

      RewardTeam team = walletReward.getTeam();
      if (team == null) {
        rewardEntity.setTeam(null);
      } else {
        RewardTeamEntity teamEntity = rewardTeamDAO.find(team.getId());
        rewardEntity.setTeam(teamEntity);
      }

      if (rewardEntity.getId() == null) {
        rewardEntity = rewardDAO.create(rewardEntity);
      } else {
        rewardEntity = rewardDAO.update(rewardEntity);
      }

      Set<WalletPluginReward> rewardPlugins = walletReward.getRewards();
      if (rewardPlugins == null || rewardPlugins.isEmpty()) {
        continue;
      }

      for (WalletPluginReward rewardPlugin : rewardPlugins) {
        WalletRewardPluginEntity rewardPluginEntity =
                                                    rewardPluginDAO.getRewardPluginsByRewardIdAndPluginId(rewardEntity.getId(),
                                                                                                          rewardPlugin.getPluginId());
        if (rewardPluginEntity == null) {
          rewardPluginEntity = new WalletRewardPluginEntity();
        }
        rewardPluginEntity.setAmount(rewardPlugin.getAmount());
        rewardPluginEntity.setPoints(rewardPlugin.getPoints());
        rewardPluginEntity.setPluginId(rewardPlugin.getPluginId());
        rewardPluginEntity.setPoolUsed(rewardPlugin.isPoolsUsed());
        rewardPluginEntity.setReward(rewardEntity);

        if (rewardPluginEntity.getId() == null) {
          rewardPluginDAO.create(rewardPluginEntity);
        } else {
          rewardPluginDAO.update(rewardPluginEntity);
        }
      }
    }
  }

  public List<RewardPeriod> findRewardPeriodsByStatus(RewardStatus rewardStatus) {
    List<WalletRewardPeriodEntity> rewardPeriodEntities = rewardPeriodDAO.findRewardPeriodsByStatus(rewardStatus);
    return rewardPeriodEntities.stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<WalletReward> listRewards(long identityId, ZoneId zoneId, int limit) {
    List<WalletRewardEntity> rewardEntities = rewardDAO.findRewardsByIdentityId(identityId, limit);
    List<WalletReward> walletRewards = rewardEntities.stream()
                                                     .map(rewardEntity -> toDTO(rewardEntity, zoneId))
                                                     .collect(Collectors.toList());
    if (!walletRewards.isEmpty()) {
      WalletReward walletReward = walletRewards.get(0);
      Wallet wallet = walletReward.getWallet();
      walletAccountService.retrieveWalletBlockchainState(wallet);
      walletRewards.forEach(wr -> wr.setWallet(wallet));
    }
    return walletRewards;
  }

  public double countRewards(long identityId) {
    double countRewardsByUser = rewardDAO.countRewardsByIdentityId(identityId);
    return Double.isNaN(countRewardsByUser) ? 0 : countRewardsByUser;
  }

  public void replaceRewardTransactions(String oldHash, String newHash) {
    rewardDAO.replaceRewardTransactions(oldHash, newHash);
  }

  private RewardPeriod toDTO(WalletRewardPeriodEntity period) {
    RewardPeriod rewardPeriod = new RewardPeriod(period.getPeriodType());
    rewardPeriod.setStartDateInSeconds(period.getStartTime());
    rewardPeriod.setEndDateInSeconds(period.getEndTime());
    if (StringUtils.isNotBlank(period.getTimeZone())) {
      rewardPeriod.setTimeZone(period.getTimeZone());
    }
    return rewardPeriod;
  }

  private void retrieveRewardPlugins(WalletRewardEntity rewardEntity, WalletReward walletReward) {
    Set<WalletPluginReward> rewardPlugins = getRewardPluginsByRewardId(rewardEntity.getId());
    walletReward.setRewards(rewardPlugins);
  }

  private Set<WalletPluginReward> getRewardPluginsByRewardId(Long rewardId) {
    List<WalletRewardPluginEntity> rewardsPluginEntities = rewardPluginDAO.getRewardPluginsByRewardId(rewardId);
    if (rewardsPluginEntities != null) {
      return rewardsPluginEntities.stream().map(this::toDTO).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  private WalletReward toDTO(WalletRewardEntity rewardEntity, ZoneId zoneId) {
    WalletReward walletReward = new WalletReward();
    retrieveTeam(rewardEntity, walletReward);
    retrieveWallet(rewardEntity, walletReward);
    retrieveTransaction(rewardEntity, walletReward);
    retrieveRewardPlugins(rewardEntity, walletReward);
    WalletRewardPeriodEntity periodEntity = rewardEntity.getPeriod();
    if (periodEntity != null && periodEntity.getPeriodType() != null) {
      RewardPeriodType rewardPeriodType = periodEntity.getPeriodType();
      ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(periodEntity.getStartTime()), zoneId);
      walletReward.setPeriod(rewardPeriodType.getPeriodOfTime(zonedDateTime));
    }
    return walletReward;
  }

  private WalletPluginReward toDTO(WalletRewardPluginEntity rewardPluginEntity) {
    WalletPluginReward pluginReward = new WalletPluginReward();
    pluginReward.setIdentityId(rewardPluginEntity.getReward().getIdentityId());
    pluginReward.setPluginId(rewardPluginEntity.getPluginId());
    pluginReward.setPoolsUsed(rewardPluginEntity.isPoolUsed());
    pluginReward.setPoints(rewardPluginEntity.getPoints());
    pluginReward.setAmount(rewardPluginEntity.getAmount());
    return pluginReward;
  }

  private void retrieveTransaction(WalletRewardEntity rewardEntity, WalletReward walletReward) {
    String transactionHash = rewardEntity.getTransactionHash();
    if (StringUtils.isNotBlank(transactionHash)) {
      TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(transactionHash);
      walletReward.setTransaction(transactionDetail);
    }
  }

  private void retrieveTeam(WalletRewardEntity rewardEntity, WalletReward walletReward) {
    if (rewardEntity.getTeam() != null) {
      long teamId = rewardEntity.getTeam().getId();
      RewardTeam team = rewardTeamStorage.getTeamById(teamId);
      walletReward.setTeams(Collections.singletonList(team));
    }
  }

  private void retrieveWallet(WalletRewardEntity rewardEntity, WalletReward walletReward) {
    Wallet wallet = walletAccountService.getWalletByIdentityId(rewardEntity.getIdentityId());
    walletReward.setWallet(wallet);
  }

}
