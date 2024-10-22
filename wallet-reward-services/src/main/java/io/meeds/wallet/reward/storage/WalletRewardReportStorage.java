/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.reward.storage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.wallet.model.RewardPeriod;
import io.meeds.wallet.model.RewardPeriodType;
import io.meeds.wallet.model.RewardReport;
import io.meeds.wallet.model.RewardStatus;
import io.meeds.wallet.model.TransactionDetail;
import io.meeds.wallet.model.Wallet;
import io.meeds.wallet.model.WalletReward;
import io.meeds.wallet.reward.dao.RewardDAO;
import io.meeds.wallet.reward.dao.RewardPeriodDAO;
import io.meeds.wallet.reward.entity.WalletRewardEntity;
import io.meeds.wallet.reward.entity.WalletRewardPeriodEntity;
import io.meeds.wallet.service.WalletAccountService;
import io.meeds.wallet.service.WalletTransactionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class WalletRewardReportStorage {

  private static final Log         LOG = ExoLogger.getLogger(WalletRewardReportStorage.class);

  @Autowired
  private RewardDAO                rewardDAO;

  @Autowired
  private RewardPeriodDAO          rewardPeriodDAO;

  @Autowired
  private WalletAccountService     walletAccountService;

  @Autowired
  private WalletTransactionService walletTransactionService;

  public Page<RewardPeriod> findRewardReportPeriods(Pageable pageable) {
    Page<WalletRewardPeriodEntity> rewardPeriodEntities = rewardPeriodDAO.findAll(pageable);
    return rewardPeriodEntities.map(this::toDTO);
  }

  public Page<RewardPeriod> findRewardPeriodsBetween(long from, long to, Pageable pageable) {
    Page<WalletRewardPeriodEntity> rewardPeriodEntities = rewardPeriodDAO.findRewardPeriodsBetween(from, to, pageable);
    return rewardPeriodEntities.map(this::toDTO);
  }

  public RewardReport getRewardReportByPeriodId(long id, ZoneId zoneId) {
    return getRewardReport(rewardPeriodDAO.findById(id).orElse(null), zoneId);
  }

  public RewardReport getRewardReport(RewardPeriodType periodType, LocalDate date, ZoneId zoneId) {
    RewardPeriod period = periodType.getPeriodOfTime(date, zoneId);
    WalletRewardPeriodEntity rewardPeriodEntity =
                                                rewardPeriodDAO.findRewardPeriodByTypeAndTime(periodType,
                                                                                              period.getPeriodMedianDateInSeconds());
    return getRewardReport(rewardPeriodEntity, zoneId);
  }

  public RewardPeriod getRewardPeriod(RewardPeriodType periodType, LocalDate date, ZoneId zoneId) {
    RewardPeriod period = periodType.getPeriodOfTime(date, zoneId);
    WalletRewardPeriodEntity rewardPeriodEntity =
                                                rewardPeriodDAO.findRewardPeriodByTypeAndTime(periodType,
                                                                                              period.getPeriodMedianDateInSeconds());
    return toDTO(rewardPeriodEntity);
  }

  public RewardPeriod getRewardPeriodById(long rewardPeriodId) {
    WalletRewardPeriodEntity rewardPeriodEntity = rewardPeriodDAO.findById(rewardPeriodId).orElse(null);
    return toDTO(rewardPeriodEntity);
  }

  private RewardReport getRewardReport(WalletRewardPeriodEntity rewardPeriodEntity, ZoneId zoneId) {
    if (rewardPeriodEntity == null) {
      return null;
    }
    RewardReport rewardReport = new RewardReport();
    RewardPeriod periodOfTime = toDTO(rewardPeriodEntity);
    periodOfTime = periodOfTime.getRewardPeriodType().getPeriodOfTime(periodOfTime.getPeriodMedianDate(), zoneId);
    periodOfTime.setId(rewardPeriodEntity.getId());
    rewardReport.setPeriod(periodOfTime);

    List<WalletRewardEntity> rewardEntities = rewardDAO.findRewardsByPeriodId(rewardPeriodEntity.getId());
    if (rewardEntities != null) {
      List<WalletRewardEntity> walletRewardEntities = new ArrayList<>();
      rewardEntities.forEach(reward -> {
        List<WalletRewardEntity> walletRewardList = rewardEntities.stream()
                                                                  .filter(wr -> wr.getIdentityId() == reward.getIdentityId())
                                                                  .toList();
        if (walletRewardList.size() == 1) {
          walletRewardEntities.add(reward);
        } else {
          WalletRewardEntity walletRewardEntity = walletRewardList.stream()
                                                                  .filter(r -> r.getTransactionHash() != null)
                                                                  .min((r2, r1) -> Double.compare(r1.getTokensSent(),
                                                                                                  r2.getTokensSent()))
                                                                  .orElseGet(walletRewardList::getFirst);
          if (walletRewardEntity.getId().longValue() == reward.getId().longValue()) {
            walletRewardEntities.add(reward);
          }
        }
      });

      Set<WalletReward> rewards = walletRewardEntities.stream()
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
    WalletRewardPeriodEntity rewardPeriodEntity =
                                                rewardPeriodDAO.findRewardPeriodByTypeAndTime(period.getRewardPeriodType(),
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

    if (rewardReport.isCompletelyProcessed()) {
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
    rewardPeriodEntity = rewardPeriodDAO.save(rewardPeriodEntity);

    // No null check, it has been already checked by
    // rewardReport.countValidRewards()
    Set<WalletReward> rewards = rewardReport.getRewards();
    for (WalletReward walletReward : rewards) { // NOSONAR
      if (walletReward.getWallet() == null || StringUtils.isBlank(walletReward.getWallet().getAddress())) {
        continue;
      }
      long identityId = walletReward.getIdentityId();

      WalletRewardEntity rewardEntity;
      List<WalletRewardEntity> rewardEntities =
                                              rewardDAO.findRewardByIdentityIdAndPeriodId(identityId, rewardPeriodEntity.getId());
      if (CollectionUtils.isEmpty(rewardEntities)) {
        rewardEntity = null;
      } else {
        rewardEntity = getFirstItem(rewardEntities);
      }
      if (rewardEntity == null) {
        rewardEntity = new WalletRewardEntity();
      }
      rewardEntity.setTokensSent(walletReward.getTokensSent());
      rewardEntity.setTokensToSend(walletReward.getAmount());
      rewardEntity.setPoints(walletReward.getPoints());
      rewardEntity.setEnabled(walletReward.isEnabled());
      rewardEntity.setIdentityId(identityId);
      rewardEntity.setPeriod(rewardPeriodEntity);

      TransactionDetail rewardTransaction = walletReward.getTransaction();
      rewardEntity.setTransactionHash(rewardTransaction == null ? null : rewardTransaction.getHash());

      rewardDAO.save(rewardEntity);
    }
  }

  public List<RewardPeriod> findRewardPeriodsByStatus(RewardStatus rewardStatus) {
    List<WalletRewardPeriodEntity> rewardPeriodEntities = rewardPeriodDAO.findWalletRewardPeriodEntitiesByStatus(rewardStatus);
    return rewardPeriodEntities.stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<WalletReward> listRewards(long identityId, ZoneId zoneId, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<WalletRewardEntity> rewardEntities = rewardDAO.findWalletRewardEntitiesByIdentityId(identityId, pageable);
    List<WalletReward> walletRewards = rewardEntities.stream()
                                                     .map(rewardEntity -> toDTO(rewardEntity, zoneId))
                                                     .collect(Collectors.toList());
    if (!walletRewards.isEmpty()) {
      WalletReward walletReward = walletRewards.getFirst();
      Wallet wallet = walletReward.getWallet();
      walletAccountService.retrieveWalletBlockchainState(wallet);
      walletRewards.forEach(wr -> wr.setWallet(wallet));
    }
    return walletRewards;
  }

  public double countRewards(long identityId) {
    double countRewardsByUser = rewardDAO.countWalletRewardEntitiesByIdentityId(identityId);
    return Double.isNaN(countRewardsByUser) ? 0 : countRewardsByUser;
  }

  public void replaceRewardTransactions(String oldHash, String newHash) {
    rewardDAO.replaceRewardTransactions(oldHash, newHash);
  }  
  
  public Page<WalletReward> findWalletRewardsByPeriodIdAndStatus(long periodId, boolean isValid, ZoneId zoneId, Pageable pageable) {
    Page<WalletRewardEntity> walletRewardEntities = rewardDAO.findWalletRewardsByPeriodIdAndStatus(periodId, isValid, pageable);
    return walletRewardEntities.map(walletRewardEntity -> toDTO(walletRewardEntity, zoneId));
  }

  public double countWalletRewardsPointsByPeriodIdAndStatus(long periodId, boolean isValid) {
    Double countWalletRewardsPoints =  rewardDAO.countWalletRewardsPointsByPeriodIdAndStatus(periodId, isValid);
    return countWalletRewardsPoints != null ? countWalletRewardsPoints : 0;
  }

  private RewardPeriod toDTO(WalletRewardPeriodEntity period) {
    if (period == null) {
      return null;
    }
    RewardPeriod rewardPeriod = new RewardPeriod(period.getPeriodType());
    rewardPeriod.setId(period.getId());
    rewardPeriod.setStartDateInSeconds(period.getStartTime());
    rewardPeriod.setEndDateInSeconds(period.getEndTime());
    if (StringUtils.isNotBlank(period.getTimeZone())) {
      rewardPeriod.setTimeZone(period.getTimeZone());
    }
    return rewardPeriod;
  }

  private WalletReward toDTO(WalletRewardEntity rewardEntity, ZoneId zoneId) {
    WalletReward walletReward = new WalletReward();
    walletReward.setAmount(rewardEntity.getTokensToSend());
    walletReward.setPoints(rewardEntity.getPoints() == null ? 0d : rewardEntity.getPoints());
    retrieveWallet(rewardEntity, walletReward);
    retrieveTransaction(rewardEntity, walletReward);
    WalletRewardPeriodEntity periodEntity = rewardEntity.getPeriod();
    if (periodEntity != null && periodEntity.getPeriodType() != null) {
      RewardPeriodType rewardPeriodType = periodEntity.getPeriodType();
      ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(periodEntity.getStartTime()), zoneId);
      walletReward.setPeriod(rewardPeriodType.getPeriodOfTime(zonedDateTime));
      Integer rank = rewardDAO.findRankById(rewardEntity.getId(), periodEntity.getId());
      walletReward.setRank(rank != null ? rank : 0);
    }
    return walletReward;
  }

  private void retrieveTransaction(WalletRewardEntity rewardEntity, WalletReward walletReward) {
    String transactionHash = rewardEntity.getTransactionHash();
    if (StringUtils.isNotBlank(transactionHash)) {
      TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(transactionHash);
      walletReward.setTransaction(transactionDetail);
    }
  }

  private void retrieveWallet(WalletRewardEntity rewardEntity, WalletReward walletReward) {
    Wallet wallet = walletAccountService.getWalletByIdentityId(rewardEntity.getIdentityId());
    walletReward.setWallet(wallet);
  }

  private static WalletRewardEntity getFirstItem(List<WalletRewardEntity> resultList) {
    if (CollectionUtils.isEmpty(resultList)) {
      return null;
    } else {
      return resultList.stream()
                       .filter(r -> StringUtils.isNotBlank(r.getTransactionHash()))
                       .min((r2, r1) -> Double.compare(r1.getTokensSent(), r2.getTokensSent()))
                       .orElse(resultList.getFirst());
    }
  }

}
