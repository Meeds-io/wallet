package org.exoplatform.wallet.reward.storage;

import static org.exoplatform.wallet.utils.RewardUtils.timeFromSeconds;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.reward.*;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.dao.*;
import org.exoplatform.wallet.reward.entity.*;
import org.exoplatform.wallet.reward.storage.RewardReportStorage;
import org.exoplatform.wallet.reward.storage.RewardTeamStorage;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

public class WalletRewardReportStorage implements RewardReportStorage {

  private static final Log         LOG = ExoLogger.getLogger(WalletRewardReportStorage.class);

  private RewardPluginDAO          rewardPluginDAO;

  private RewardDAO                rewardDAO;

  private RewardPeriodDAO          rewardPeriodDAO;

  private RewardTeamDAO            rewardTeamDAO;

  private RewardTeamStorage        rewardTeamStorage;

  private WalletAccountService     walletAccountService;

  private WalletTransactionService walletTransactionService;

  public WalletRewardReportStorage(RewardPluginDAO rewardPluginDAO,
                                   RewardDAO rewardDAO,
                                   RewardPeriodDAO rewardPeriodDAO,
                                   RewardTeamDAO rewardTeamDAO,
                                   RewardTeamStorage rewardTeamStorage,
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

  @Override
  public RewardReport getRewardReport(RewardPeriodType periodType, long periodTimeInSeconds) {
    RewardPeriod period = periodType.getPeriodOfTime(timeFromSeconds(periodTimeInSeconds));
    WalletRewardPeriodEntity rewardPeriodEntity = rewardPeriodDAO.findRewardPeriodByTypeAndTime(periodType,
                                                                                                period.getStartDateInSeconds());
    if (rewardPeriodEntity == null) {
      return null;
    }
    RewardReport rewardReport = new RewardReport();
    RewardPeriod periodOfTime = rewardPeriodEntity.getPeriodType().getPeriodOfTime(timeFromSeconds(periodTimeInSeconds));
    rewardReport.setPeriod(periodOfTime);

    List<WalletRewardEntity> rewardEntities = rewardDAO.findRewardsByPeriodId(rewardPeriodEntity.getId());
    if (rewardEntities != null) {
      Set<WalletReward> rewards = rewardEntities.stream().map(rewardEntity -> toDTO(rewardEntity)).collect(Collectors.toSet());
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
    LOG.info("Saving reward report for period from {} to {}",
             period.getStartDateFormatted("en"),
             period.getEndDateFormatted("en"));

    WalletRewardPeriodEntity rewardPeriodEntity = rewardPeriodDAO.findRewardPeriodByTypeAndTime(period.getRewardPeriodType(),
                                                                                                period.getStartDateInSeconds());
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
    for (WalletReward walletReward : rewards) {
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

  @Override
  public List<RewardPeriod> findRewardPeriodsByStatus(RewardStatus rewardStatus) {
    List<WalletRewardPeriodEntity> rewardPeriodEntities = rewardPeriodDAO.findRewardPeriodsByStatus(rewardStatus);
    return rewardPeriodEntities.stream().map(period -> toDTO(period)).collect(Collectors.toList());
  }

  @Override
  public List<WalletReward> listRewards(long identityId, int limit) {
    List<WalletRewardEntity> rewardEntities = rewardDAO.findRewardsByIdentityId(identityId, limit);
    List<WalletReward> walletRewards = rewardEntities.stream()
                                                     .map(rewardEntity -> toDTO(rewardEntity))
                                                     .collect(Collectors.toList());
    if (!walletRewards.isEmpty()) {
      WalletReward walletReward = walletRewards.get(0);
      Wallet wallet = walletReward.getWallet();
      walletAccountService.retrieveWalletBlockchainState(wallet);
      walletRewards.forEach(wr -> wr.setWallet(wallet));
    }
    return walletRewards;
  }

  private RewardPeriod toDTO(WalletRewardPeriodEntity period) {
    RewardPeriod rewardPeriod = new RewardPeriod(period.getPeriodType());
    rewardPeriod.setStartDateInSeconds(period.getStartTime());
    rewardPeriod.setEndDateInSeconds(period.getEndTime());
    return rewardPeriod;
  }

  private void retrieveRewardPlugins(WalletRewardEntity rewardEntity, WalletReward walletReward) {
    Set<WalletPluginReward> rewardPlugins = getRewardPluginsByRewardId(rewardEntity.getId());
    walletReward.setRewards(rewardPlugins);
  }

  private Set<WalletPluginReward> getRewardPluginsByRewardId(Long rewardId) {
    List<WalletRewardPluginEntity> rewardsPluginEntities = rewardPluginDAO.getRewardPluginsByRewardId(rewardId);
    if (rewardsPluginEntities != null) {
      return rewardsPluginEntities.stream()
                                  .map(entity -> toDTO(entity))
                                  .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  private WalletReward toDTO(WalletRewardEntity rewardEntity) {
    WalletReward walletReward = new WalletReward();
    retrieveTeam(rewardEntity, walletReward);
    retrieveWallet(rewardEntity, walletReward);
    retrieveTransaction(rewardEntity, walletReward);
    retrieveRewardPlugins(rewardEntity, walletReward);
    WalletRewardPeriodEntity periodEntity = rewardEntity.getPeriod();
    if (periodEntity != null && periodEntity.getPeriodType() != null) {
      RewardPeriodType rewardPeriodType = periodEntity.getPeriodType();
      walletReward.setPeriod(rewardPeriodType.getPeriodOfTime(timeFromSeconds(periodEntity.getStartTime())));
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
      if (!StringUtils.equals(transactionDetail.getContractMethodName(), "reward")) {
        LOG.warn("Transaction with hash {} is not a reward transaction, data seems not coherent", transactionHash);
      }
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
