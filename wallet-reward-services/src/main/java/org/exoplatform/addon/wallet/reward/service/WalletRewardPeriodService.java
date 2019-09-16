package org.exoplatform.addon.wallet.reward.service;

import static org.exoplatform.addon.wallet.utils.RewardUtils.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.reward.*;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.reward.dao.*;
import org.exoplatform.addon.wallet.reward.entity.*;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class WalletRewardPeriodService implements RewardPeriodService {

  private static final Log         LOG = ExoLogger.getLogger(WalletRewardService.class);

  private RewardPluginDAO          rewardPluginDAO;

  private RewardDAO                rewardDAO;

  private RewardPeriodDAO          rewardPeriodDAO;

  private RewardTeamDAO            rewardTeamDAO;

  private WalletAccountService     walletAccountService;

  private WalletTransactionService walletTransactionService;

  public WalletRewardPeriodService(RewardPluginDAO rewardPluginDAO,
                                   RewardDAO rewardDAO,
                                   RewardPeriodDAO rewardPeriodDAO,
                                   RewardTeamDAO rewardTeamDAO,
                                   WalletAccountService walletAccountService,
                                   WalletTransactionService walletTransactionService) {
    this.rewardPluginDAO = rewardPluginDAO;
    this.rewardDAO = rewardDAO;
    this.rewardPeriodDAO = rewardPeriodDAO;
    this.rewardTeamDAO = rewardTeamDAO;
    this.walletAccountService = walletAccountService;
    this.walletTransactionService = walletTransactionService;
  }

  @Override
  public RewardReport getRewardReport(RewardPeriodType periodType, long periodTimeInSeconds) {
    WalletRewardPeriodEntity rewardPeriodEntity = rewardPeriodDAO.findRewardPeriodByTypeAndTime(periodType, periodTimeInSeconds);
    if (rewardPeriodEntity == null) {
      return null;
    }
    RewardReport rewardReport = new RewardReport();
    RewardPeriod periodOfTime = rewardPeriodEntity.getPeriodType().getPeriodOfTime(timeFromSeconds(periodTimeInSeconds));
    rewardReport.setPeriod(periodOfTime);

    List<WalletRewardEntity> rewardEntities = rewardDAO.findRewardsByPeriodId(rewardPeriodEntity.getId());
    if (rewardEntities != null) {
      Set<WalletReward> rewards = rewardEntities.stream().map(rewardEntity -> {
        WalletReward walletReward = new WalletReward();
        walletReward.setEnabled(rewardEntity.isEnabled());
        if (rewardEntity.getTeam() != null) {
          walletReward.setPoolId(rewardEntity.getTeam().getId());
          walletReward.setPoolName(rewardEntity.getTeam().getName());
        }
        walletReward.setEnabled(rewardEntity.isEnabled());
        Wallet wallet = walletAccountService.getWalletByIdentityId(rewardEntity.getIdentityId());
        walletReward.setWallet(wallet);
        String transactionHash = rewardEntity.getTransactionHash();
        if (StringUtils.isBlank(transactionHash)) {
          TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(transactionHash);
          if (!StringUtils.equals(transactionDetail.getContractMethodName(), "reward")) {
            LOG.warn("Transaction with hash {} is not a reward transaction, data seems not coherent", transactionHash);
            return null;
          }
          RewardTransaction rewardTransaction = new RewardTransaction();
          rewardTransaction.setHash(transactionDetail.getHash());
          rewardTransaction.setPeriodType(periodType.name());
          rewardTransaction.setReceiverId(wallet.getId());
          rewardTransaction.setReceiverIdentityId(wallet.getTechnicalId());
          rewardTransaction.setReceiverType(wallet.getType());
          rewardTransaction.setStartDateInSeconds(periodOfTime.getStartDateInSeconds());
          if (transactionDetail.isPending()) {
            rewardTransaction.setStatus(TRANSACTION_STATUS_PENDING);
          } else if (transactionDetail.isSucceeded()) {
            rewardTransaction.setStatus(TRANSACTION_STATUS_SUCCESS);
          } else {
            rewardTransaction.setStatus(TRANSACTION_STATUS_FAILED);
          }
          rewardTransaction.setTokensSent(transactionDetail.getContractAmount());
          walletReward.setTransaction(rewardTransaction);

          List<WalletRewardPluginEntity> rewardsPluginEntities =
                                                               rewardPluginDAO.findRewardPluginsByPeriodId(rewardEntity.getId());
          if (rewardsPluginEntities != null) {
            Set<WalletPluginReward> rewardPlugins = rewardsPluginEntities.stream().map(rewardPluginEntity -> {
              WalletPluginReward pluginReward = new WalletPluginReward();
              pluginReward.setPluginId(rewardPluginEntity.getPluginId());
              pluginReward.setPoolsUsed(rewardPluginEntity.isPoolUsed());
              pluginReward.setPoints(rewardPluginEntity.getPoints());
              pluginReward.setAmount(rewardPluginEntity.getAmount());
              return pluginReward;
            }).collect(Collectors.toSet());
            walletReward.setRewards(rewardPlugins);
          }
        }
        return walletReward;
      }).collect(Collectors.toSet());
      rewardReport.setRewards(rewards);
    }
    return rewardReport;
  }

  @Override
  public void saveRewardReport(RewardReport rewardReport) {
    if (rewardReport == null) {
      throw new IllegalArgumentException("reward report is null");
    }
    RewardPeriod period = rewardReport.getPeriod();
    WalletRewardPeriodEntity rewardPeriodEntity = rewardPeriodDAO.findRewardPeriodByTypeAndTime(period.getRewardPeriodType(),
                                                                                                period.getStartDateInSeconds());
    if (rewardPeriodEntity == null) {
      rewardPeriodEntity = new WalletRewardPeriodEntity();
    } else if (rewardPeriodEntity.getStatus() == RewardStatus.SUCCESS) {
      LOG.debug("Reward report shouldn't be modified because it has been already marked as completed");
    }

    if (rewardReport.countValidRewards() == 0 || rewardReport.totalAmount() == 0) {
      LOG.debug("Reward report doesn't have valid rewards yet, thus it will not be saved");
      return;
    }

    rewardPeriodEntity.setPeriodType(period.getRewardPeriodType());
    rewardPeriodEntity.setStartTime(period.getStartDateInSeconds());
    rewardPeriodEntity.setEndTime(period.getEndDateInSeconds());

    if (rewardReport.isCompletelyProceeded()) {
      rewardPeriodEntity.setStatus(RewardStatus.SUCCESS);
    } else if (rewardReport.countPending() > 0) { // Always pending if some tx
                                                  // failed
      rewardPeriodEntity.setStatus(RewardStatus.PENDING);
    } else if (rewardReport.countFailed() > 0) { // If no failed and there are
                                                 // somme errors
      rewardPeriodEntity.setStatus(RewardStatus.ERROR);
    } else if (rewardReport.countTransactions() > 0) { // If some transactions
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
    for (WalletReward walletReward : rewards) {
      if (walletReward.getWallet() == null || StringUtils.isBlank(walletReward.getWallet().getAddress())) {
        continue;
      }
      long identityId = walletReward.getWallet().getTechnicalId();
      WalletRewardEntity rewardEntity = rewardDAO.findRewardByIdentityIdAndPeriod(identityId, rewardPeriodEntity.getId());
      if (rewardEntity == null) {
        rewardEntity = new WalletRewardEntity();
      }
      rewardEntity.setTokensSent(walletReward.getTokensSent());
      rewardEntity.setTokensToSend(walletReward.getTokensToSend());
      rewardEntity.setEnabled(walletReward.isEnabled());
      rewardEntity.setIdentityId(identityId);
      rewardEntity.setPeriod(rewardPeriodEntity);

      RewardTransaction rewardTransaction = walletReward.getTransaction();
      if (rewardTransaction != null) {
        rewardEntity.setTransactionHash(rewardTransaction.getHash());
      }

      long poolId = walletReward.getPoolId();
      if (poolId > 0) {
        RewardTeamEntity teamEntity = rewardTeamDAO.find(poolId);
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
                                                    rewardPluginDAO.findRewardPluginByRewardIdAndPlugin(rewardEntity.getId(),
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

}
