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

import static org.exoplatform.addon.wallet.utils.RewardUtils.*;
import static org.exoplatform.addon.wallet.utils.WalletUtils.convertFromDecimals;
import static org.exoplatform.addon.wallet.utils.WalletUtils.getSettings;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.model.reward.*;
import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.reward.api.RewardPlugin;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A storage service to save/load reward settings
 */
public class WalletRewardService implements RewardService {
  private static final Log         LOG                             = ExoLogger.getLogger(WalletRewardService.class);

  private static final String      DEFAULT_REWARD_LABEL_TEMPLATE   =
                                                                 "{name} is rewarded {amount} {symbol} for period: {startDate} to {endDate}";

  private static final String      DEFAULT_REWARD_MESSAGE_TEMPLATE =
                                                                   "You have earned {amount} {symbol} in reward for your {rewardCount} {pluginName} {earned in pool_label} for period: {startDate} to {endDate}";

  private WalletAccountService     walletAccountService;

  private WalletTransactionService walletTransactionService;

  private WalletTokenAdminService  walletTokenAdminService;

  private RewardSettingsService    rewardSettingsService;

  private RewardTeamService        rewardTeamService;

  private RewardTransactionService rewardTransactionService;

  public WalletRewardService(WalletAccountService walletAccountService,
                             WalletTransactionService walletTransactionService,
                             RewardSettingsService rewardSettingsService,
                             RewardTransactionService rewardTransactionService,
                             RewardTeamService rewardTeamService) {
    this.walletAccountService = walletAccountService;
    this.walletTransactionService = walletTransactionService;
    this.rewardSettingsService = rewardSettingsService;
    this.rewardTeamService = rewardTeamService;
    this.rewardTransactionService = rewardTransactionService;
  }

  @Override
  public void sendRewards(long periodDateInSeconds, String username) throws Exception {
    Set<WalletReward> rewards = computeReward(periodDateInSeconds);
    if (rewards == null || rewards.isEmpty()) {
      return;
    }
    String adminWalletAddress = getTokenAdminService().getAdminWalletAddress();
    if (StringUtils.isBlank(adminWalletAddress)) {
      throw new IllegalStateException("No admin wallet is configured");
    }
    if (getTokenAdminService().getAdminLevel(adminWalletAddress) < 4) {
      throw new IllegalStateException("Configured admin wallet is not configured as admin on token. It must be a Token admin with level 4 at least.");
    }

    Iterator<WalletReward> rewardedWalletsIterator = rewards.iterator();
    while (rewardedWalletsIterator.hasNext()) {
      WalletReward walletReward = rewardedWalletsIterator.next();
      if (walletReward == null || !walletReward.isEnabled() || walletReward.getRewards() == null
          || walletReward.getTokensToSend() == 0) {
        rewardedWalletsIterator.remove();
        continue;
      }
      if (walletReward.getTokensToSend() < 0) {
        throw new IllegalStateException("Can't send reward transaction for wallet of " + walletReward.getWallet().getType() + " "
            + walletReward.getWallet().getId() + " with a negative amount" + walletReward.getTokensToSend());
      }
      RewardTransaction rewardTransaction = walletReward.getRewardTransaction();
      if (rewardTransaction != null && StringUtils.isNotBlank(rewardTransaction.getHash())) {
        String hash = rewardTransaction.getHash();
        String transactionStatus = rewardTransaction.getStatus();
        if (StringUtils.isBlank(transactionStatus)) {
          LOG.warn("Can't find transaction detail of hash {} as reward transaction for {} '{}' in period {}. The reward will be re-sent.",
                   hash,
                   walletReward.getWallet().getType(),
                   walletReward.getWallet().getId(),
                   new Date(periodDateInSeconds * 1000));
        } else if (StringUtils.equals(transactionStatus, TRANSACTION_STATUS_PENDING)) {
          throw new IllegalStateException("Reward transaction " + hash
              + " is pending, thus no reward sending is allowed until the transactions finishes");
        } else if (StringUtils.equals(transactionStatus, TRANSACTION_STATUS_SUCCESS)) {
          rewardedWalletsIterator.remove();
        }
      }
    }

    if (rewards.isEmpty()) {
      throw new IllegalStateException("No rewards to send for selected period");
    }
    GlobalSettings settings = getSettings();
    ContractDetail contractDetail = settings.getContractDetail();
    if (contractDetail == null) {
      throw new IllegalStateException("Token with address " + settings.getContractAddress() + "wasn't found");
    }
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    if (rewardSettings == null) {
      throw new IllegalStateException("No reward settings is found");
    }
    RewardPeriodType periodType = rewardSettings.getPeriodType();
    RewardPeriod periodOfTime = periodType.getPeriodOfTime(timeFromSeconds(periodDateInSeconds));
    BigInteger adminTokenBalance = getTokenAdminService().balanceOf(adminWalletAddress);
    double adminBalance = convertFromDecimals(adminTokenBalance, contractDetail.getDecimals());
    double rewardsAmount = rewards.stream().mapToDouble(WalletReward::getTokensToSend).sum();
    if (rewardsAmount > adminBalance) {
      throw new IllegalStateException("Admin doesn't have enough funds to send rewards");
    }
    for (WalletReward walletReward : rewards) {
      TransactionDetail transactionDetail = new TransactionDetail();
      transactionDetail.setTo(walletReward.getWallet().getAddress());
      transactionDetail.setContractAmount(walletReward.getTokensToSend());
      transactionDetail.setValue(walletReward.getTokensToSend());
      String transactionLabel = getTransactionLabel(walletReward, contractDetail, periodOfTime);
      transactionDetail.setLabel(transactionLabel);
      String transactionMessage = getTransactionMessage(walletReward, contractDetail, periodOfTime);
      transactionDetail.setMessage(transactionMessage);
      transactionDetail = getTokenAdminService().reward(transactionDetail, username);
      RewardTransaction rewardTransaction = walletReward.getRewardTransaction();
      if (rewardTransaction == null) {
        rewardTransaction = new RewardTransaction();
      }
      rewardTransaction.setHash(transactionDetail.getHash());
      rewardTransaction.setPeriodType(periodType.name());
      rewardTransaction.setReceiverId(walletReward.getWallet().getId());
      rewardTransaction.setReceiverType(walletReward.getWallet().getType());
      rewardTransaction.setReceiverIdentityId(walletReward.getWallet().getTechnicalId());
      rewardTransaction.setStartDateInSeconds(periodOfTime.getStartDateInSeconds());
      rewardTransaction.setStatus(TRANSACTION_STATUS_PENDING);
      rewardTransaction.setTokensSent(walletReward.getTokensToSend());
      rewardTransactionService.saveRewardTransaction(rewardTransaction);
    }
  }

  @Override
  public Set<WalletReward> computeReward(long periodDateInSeconds) {
    if (periodDateInSeconds == 0) {
      throw new IllegalArgumentException("periodDate is mandatory");
    }
    Set<Wallet> wallets = walletAccountService.listWallets();
    wallets = wallets.stream().filter(wallet -> WalletType.isUser(wallet.getType())).collect(Collectors.toSet());

    Set<Long> identityIds = wallets.stream()
                                   .map(Wallet::getTechnicalId)
                                   .collect(Collectors.toSet());
    if (identityIds == null || identityIds.isEmpty()) {
      return Collections.emptySet();
    }
    Collection<RewardPlugin> rewardPlugins = rewardSettingsService.getRewardPlugins();
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    if (rewardSettings == null) {
      throw new IllegalStateException("Error computing rewards using empty settings");
    }
    if (rewardSettings.getPeriodType() == null) {
      throw new IllegalStateException("Error computing rewards using empty period type");
    }
    Set<RewardPluginSettings> pluginSettings = rewardSettings.getPluginSettings();
    if (pluginSettings == null || pluginSettings.isEmpty()) {
      throw new IllegalStateException("Error computing rewards using empty rewards types");
    }

    RewardPeriodType periodType = rewardSettings.getPeriodType();
    RewardPeriod periodOfTime = periodType.getPeriodOfTime(timeFromSeconds(periodDateInSeconds));

    Set<Long> enabledIdentityIds = getEnabledWallets(identityIds);
    Set<Long> walletsWithEnabledTeam = getEnabledTeamMembers(enabledIdentityIds);

    Set<WalletPluginReward> walletRewardsByPlugin = new HashSet<>();
    for (RewardPlugin rewardPlugin : rewardPlugins) {
      if (rewardPlugin == null || !rewardPlugin.isEnabled()) {
        continue;
      }
      RewardPluginSettings rewardPluginSettings = getPluginSetting(pluginSettings, rewardPlugin.getPluginId());
      if (rewardPluginSettings != null) {
        Map<Long, Double> earnedPoints = rewardPlugin.getEarnedPoints(identityIds,
                                                                     periodOfTime.getStartDateInSeconds(),
                                                                     periodOfTime.getEndDateInSeconds());
        Set<Long> validIdentityIdsToUse = rewardPluginSettings.isUsePools() ? walletsWithEnabledTeam : enabledIdentityIds;
        computeReward(rewardPluginSettings, earnedPoints, validIdentityIdsToUse, walletRewardsByPlugin);
      }
    }

    List<RewardTransaction> rewardTransactions = rewardTransactionService.getRewardTransactions(periodType.name(),
                                                                                                periodOfTime.getStartDateInSeconds());

    Set<WalletReward> walletRewards = new HashSet<>();
    for (Wallet wallet : wallets) {
      WalletReward walletReward = new WalletReward();
      walletReward.setWallet(wallet);
      walletReward.setEnabled(enabledIdentityIds.contains(wallet.getTechnicalId()));

      Set<WalletPluginReward> rewardDetails = walletRewardsByPlugin.stream()
                                                                   .filter(reward -> reward.getIdentityId() == wallet.getTechnicalId())
                                                                   .collect(Collectors.toSet());
      walletReward.setRewards(rewardDetails);

      RewardTransaction rewardTransaction = rewardTransactions.stream()
                                                              .filter(transaction -> transaction.getReceiverIdentityId() == wallet.getTechnicalId())
                                                              .findFirst()
                                                              .orElse(null);
      walletReward.setRewardTransaction(rewardTransaction);
      if (rewardTransaction != null && StringUtils.isNotBlank(rewardTransaction.getHash())) {
        String hash = rewardTransaction.getHash();
        TransactionDetail transactionDetail = walletTransactionService.getTransactionByHash(hash);
        if (transactionDetail != null) {
          if (transactionDetail.isPending()) {
            rewardTransaction.setStatus(TRANSACTION_STATUS_PENDING);
          } else if (transactionDetail.isSucceeded()) {
            rewardTransaction.setStatus(TRANSACTION_STATUS_SUCCESS);
          } else {
            rewardTransaction.setStatus(TRANSACTION_STATUS_FAILED);
          }
        }
      }
      walletRewards.add(walletReward);
    }

    List<RewardTeam> teams = rewardTeamService.getTeams();
    for (RewardTeam rewardTeam : teams) {
      List<RewardTeamMember> members = rewardTeam.getMembers();
      for (RewardTeamMember teamMember : members) {
        WalletReward wallet = walletRewards.stream()
                                           .filter(walletReward -> walletReward.getWallet()
                                                                               .getTechnicalId() == teamMember.getIdentityId())
                                           .findFirst()
                                           .orElse(null);
        if (wallet != null) {
          wallet.setPoolName(rewardTeam.getName());
        }
      }
    }
    return walletRewards;
  }

  private Set<Long> getEnabledTeamMembers(Set<Long> identityIds) {
    Set<Long> walletsWithEnabledTeam = new HashSet<>(identityIds);
    List<RewardTeam> teams = rewardTeamService.getTeams();
    for (RewardTeam rewardTeam : teams) {
      if (!rewardTeam.isDisabled() || rewardTeam.getMembers() == null || rewardTeam.getMembers().isEmpty()) {
        continue;
      }
      rewardTeam.getMembers().forEach(member -> walletsWithEnabledTeam.remove(member.getIdentityId()));
    }
    return walletsWithEnabledTeam;
  }

  private Set<Long> getEnabledWallets(Set<Long> identityIds) {
    Set<Long> enabledIdentityIds = new HashSet<>(identityIds);
    Iterator<Long> identityIdsIterator = enabledIdentityIds.iterator();
    while (identityIdsIterator.hasNext()) {
      Long identityId = identityIdsIterator.next();
      if (identityId == null || identityId == 0) {
        identityIdsIterator.remove();
      }
      Wallet wallet = walletAccountService.getWalletByIdentityId(identityId);
      if (wallet == null) {
        identityIdsIterator.remove();
        continue;
      }
      if (!wallet.isEnabled() || wallet.isDeletedUser() || wallet.isDisabledUser()
          || !StringUtils.equals(wallet.getInitializationState(), WalletInitializationState.INITIALIZED.name())) {
        identityIdsIterator.remove();
      }
    }
    return enabledIdentityIds;
  }

  private RewardPluginSettings getPluginSetting(Set<RewardPluginSettings> pluginSettings, String pluginId) {
    for (RewardPluginSettings rewardPluginSettings : pluginSettings) {
      if (StringUtils.equals(pluginId, rewardPluginSettings.getPluginId())) {
        return rewardPluginSettings;
      }
    }
    return null;
  }

  private void computeReward(RewardPluginSettings rewardPluginSettings,
                             Map<Long, Double> earnedPoints,
                             Set<Long> validIdentityIdsToUse,
                             Set<WalletPluginReward> rewardMemberDetails) {
    RewardBudgetType budgetType = rewardPluginSettings.getBudgetType();
    if (budgetType == null) {
      LOG.warn("Budget type of reward plugin {} is empty, thus no computing is possible", rewardPluginSettings.getPluginId());
      return;
    }
    String pluginId = rewardPluginSettings.getPluginId();
    double configuredPluginAmount = rewardPluginSettings.getAmount();
    if (configuredPluginAmount < 0) {
      throw new IllegalStateException("Plugin " + pluginId + " has a configured negative reward amount ("
          + configuredPluginAmount + ")");
    }

    // Filter non elligible users switch threshold
    filterElligibleMembers(earnedPoints.entrySet(), validIdentityIdsToUse, rewardPluginSettings, rewardMemberDetails);

    double amountPerPoint = 0;
    double totalFixedBudget = 0;
    switch (budgetType) {
    case FIXED_PER_POINT:
      amountPerPoint = configuredPluginAmount;
      addRewardsSwitchPointAmount(rewardMemberDetails, earnedPoints.entrySet(), pluginId, amountPerPoint);
      break;
    case FIXED:
      totalFixedBudget = configuredPluginAmount;
      addTeamMembersReward(rewardPluginSettings, earnedPoints, totalFixedBudget, rewardMemberDetails);
      break;
    case FIXED_PER_MEMBER:
      double budgetPerMember = configuredPluginAmount;
      int totalElligibleMembersCount = earnedPoints.size();
      totalFixedBudget = budgetPerMember * totalElligibleMembersCount;
      addTeamMembersReward(rewardPluginSettings, earnedPoints, totalFixedBudget, rewardMemberDetails);
      break;
    default:
      throw new IllegalStateException("Budget type is not recognized in plugin settings: " + pluginId
          + ", budget type = " + budgetType);
    }
  }

  private void addTeamMembersReward(RewardPluginSettings rewardPluginSettings,
                                    Map<Long, Double> earnedPoints,
                                    double totalFixedBudget,
                                    Set<WalletPluginReward> rewardMemberDetails) {
    if (totalFixedBudget <= 0) {
      return;
    }
    double amountPerPoint;
    if (rewardPluginSettings.isUsePools()) {
      List<RewardTeam> teams = rewardTeamService.getTeams();
      Set<Long> identityIds = filterEligibleMembersAndTeams(teams, earnedPoints);
      buildNoPoolUsers(earnedPoints, teams, identityIds);
      computeTeamsMembersBudget(rewardPluginSettings.getPluginId(), teams, totalFixedBudget, rewardMemberDetails, earnedPoints);
    } else {
      double totalPoints = earnedPoints.entrySet().stream().collect(Collectors.summingDouble(entry -> entry.getValue()));
      if (totalPoints <= 0 || totalFixedBudget <= 0) {
        return;
      }
      amountPerPoint = totalFixedBudget / totalPoints;
      addRewardsSwitchPointAmount(rewardMemberDetails,
                                  earnedPoints.entrySet(),
                                  rewardPluginSettings.getPluginId(),
                                  amountPerPoint);
    }
  }

  private void addRewardsSwitchPointAmount(Set<WalletPluginReward> rewardMemberDetails,
                                           Set<Entry<Long, Double>> identitiesPointsEntries,
                                           String pluginId,
                                           double amountPerPoint) {
    for (Entry<Long, Double> identitiyPointsEntry : identitiesPointsEntries) {
      Long identityId = identitiyPointsEntry.getKey();
      Double points = identitiyPointsEntry.getValue();
      double amount = points * amountPerPoint;

      WalletPluginReward rewardMemberDetail = new WalletPluginReward();
      rewardMemberDetail.setIdentityId(identityId);
      rewardMemberDetail.setPluginId(pluginId);
      rewardMemberDetail.setPoints(points);
      rewardMemberDetail.setAmount(amount);
      rewardMemberDetail.setPoolsUsed(false);
      rewardMemberDetails.add(rewardMemberDetail);
    }
  }

  private void filterElligibleMembers(Set<Entry<Long, Double>> identitiesPointsEntries,
                                      Set<Long> validIdentityIdsToUse,
                                      RewardPluginSettings rewardPluginSettings,
                                      Set<WalletPluginReward> rewardMemberDetails) {
    String pluginId = rewardPluginSettings.getPluginId();
    double threshold = rewardPluginSettings.getThreshold();

    Iterator<Entry<Long, Double>> identitiesPointsIterator = identitiesPointsEntries.iterator();
    while (identitiesPointsIterator.hasNext()) {
      Map.Entry<java.lang.Long, java.lang.Double> entry = identitiesPointsIterator.next();
      Long identityId = entry.getKey();
      Double points = entry.getValue();
      points = points == null ? 0 : points;
      if (points < 0) {
        throw new IllegalStateException("Plugin with id " + pluginId + " has assigned a negative points (" + points
            + ") to user with id " + identityId);
      }

      if (points < threshold || points == 0 || !validIdentityIdsToUse.contains(identityId)) {
        // Member doesn't have enough points or his wallet is disabled => not
        // eligible
        identitiesPointsIterator.remove();

        if (points > 0) {
          // Add member with earned points for information on UI
          WalletPluginReward rewardMemberDetail = new WalletPluginReward();
          rewardMemberDetail.setIdentityId(identityId);
          rewardMemberDetail.setPluginId(pluginId);
          rewardMemberDetail.setPoints(points);
          rewardMemberDetail.setAmount(0);
          rewardMemberDetail.setPoolsUsed(rewardPluginSettings.isUsePools());
          rewardMemberDetails.add(rewardMemberDetail);
        }
      }
    }
  }

  private void computeTeamsMembersBudget(String pluginId,
                                         List<RewardTeam> teams,
                                         double totalTeamsBudget,
                                         Set<WalletPluginReward> rewardMemberDetails,
                                         Map<Long, Double> earnedPoints) {
    double totalFixedTeamsBudget = 0;
    double computedRecipientsCount = 0;
    List<RewardTeam> computedBudgetTeams = new ArrayList<>();
    Map<Long, Double> totalPointsPerTeam = new HashMap<>();

    // Compute teams budget with fixed amount
    for (RewardTeam rewardTeam : teams) {
      RewardBudgetType teamBudgetType = rewardTeam.getRewardType();
      double totalTeamPoints = rewardTeam.getMembers()
                                         .stream()
                                         .collect(Collectors.summingDouble(member -> earnedPoints.get(member.getIdentityId())));
      if (teamBudgetType == RewardBudgetType.COMPUTED) {
        computedRecipientsCount += rewardTeam.getMembers().size();
        computedBudgetTeams.add(rewardTeam);
        totalPointsPerTeam.put(rewardTeam.getId(), totalTeamPoints);
      } else if (teamBudgetType == RewardBudgetType.FIXED_PER_MEMBER) {
        double totalTeamBudget = rewardTeam.getBudget() * rewardTeam.getMembers().size();
        addTeamRewardRepartition(rewardTeam,
                                 totalTeamBudget,
                                 totalTeamPoints,
                                 pluginId,
                                 earnedPoints,
                                 rewardMemberDetails);
        totalFixedTeamsBudget += totalTeamBudget;
      } else if (teamBudgetType == RewardBudgetType.FIXED) {
        double totalTeamBudget = rewardTeam.getBudget();
        addTeamRewardRepartition(rewardTeam,
                                 totalTeamBudget,
                                 totalTeamPoints,
                                 pluginId,
                                 earnedPoints,
                                 rewardMemberDetails);
        totalFixedTeamsBudget += rewardTeam.getBudget();
      }
    }

    if (totalFixedTeamsBudget >= totalTeamsBudget) {
      throw new IllegalStateException("Total fixed teams budget is higher than fixed budget for all users");
    }

    // Compute teams budget with computed amount
    if (computedRecipientsCount > 0 && !computedBudgetTeams.isEmpty()) {
      double remaingBudgetForComputedTeams = totalTeamsBudget - totalFixedTeamsBudget;
      double budgetPerTeamMember = remaingBudgetForComputedTeams / computedRecipientsCount;
      computedBudgetTeams.forEach(rewardTeam -> {
        double totalTeamBudget = budgetPerTeamMember * rewardTeam.getMembers().size();
        Double totalTeamPoints = totalPointsPerTeam.get(rewardTeam.getId());
        addTeamRewardRepartition(rewardTeam,
                                 totalTeamBudget,
                                 totalTeamPoints,
                                 pluginId,
                                 earnedPoints,
                                 rewardMemberDetails);
      });
    }
  }

  private void buildNoPoolUsers(Map<Long, Double> earnedPoints, List<RewardTeam> teams, Set<Long> identityIds) {
    // Build "No pool" users
    ArrayList<Long> noPoolsIdentityIds = new ArrayList<>(earnedPoints.keySet());
    noPoolsIdentityIds.removeAll(identityIds);
    if (!noPoolsIdentityIds.isEmpty()) {
      RewardTeam noPoolRewardTeam = new RewardTeam();
      noPoolRewardTeam.setDisabled(false);
      List<RewardTeamMember> noPoolRewardTeamList = noPoolsIdentityIds.stream()
                                                                      .map(identityId -> {
                                                                        RewardTeamMember rewardTeamMember =
                                                                                                          new RewardTeamMember();
                                                                        rewardTeamMember.setIdentityId(identityId);
                                                                        return rewardTeamMember;
                                                                      })
                                                                      .collect(Collectors.toList());
      noPoolRewardTeam.setMembers(noPoolRewardTeamList);
      noPoolRewardTeam.setId(0L);
      noPoolRewardTeam.setRewardType(RewardBudgetType.COMPUTED);
      teams.add(noPoolRewardTeam);
    }
  }

  private Set<Long> filterEligibleMembersAndTeams(List<RewardTeam> teams, Map<Long, Double> earnedPoints) {
    Set<Long> identityIds = new HashSet<>();

    // Search for duplicated users and retain only elligible members in
    // Teams
    Iterator<RewardTeam> teamsIterator = teams.iterator();
    while (teamsIterator.hasNext()) {
      RewardTeam rewardTeam = teamsIterator.next();
      List<RewardTeamMember> members = rewardTeam.getMembers();
      if (members != null && !members.isEmpty()) {
        Iterator<RewardTeamMember> membersIterator = members.iterator();
        while (membersIterator.hasNext()) {
          RewardTeamMember member = membersIterator.next();
          Long identityId = member.getIdentityId();
          if (identityIds.contains(identityId)) {
            throw new IllegalStateException("Team " + rewardTeam.getName() + " has a duplicated member in another Team");
          }
          identityIds.add(identityId);
          // Retain in Teams collection only elligible members
          if (!earnedPoints.containsKey(identityId)) {
            membersIterator.remove();
          }
        }
      }
      if (members == null || members.isEmpty()) {
        teamsIterator.remove();
      }
    }
    return identityIds;
  }

  private String getTransactionLabel(WalletReward walletReward, ContractDetail contractDetail, RewardPeriod periodOfTime) {
    Wallet wallet = walletReward.getWallet();
    return DEFAULT_REWARD_LABEL_TEMPLATE.replace("{name}", wallet.getName())
                                        .replace("{amount}", String.valueOf(walletReward.getTokensToSend()))
                                        .replace("{startDate}",
                                                 formatTime(periodOfTime.getStartDateInSeconds()))
                                        .replace("{endDate}", formatTime(periodOfTime.getEndDateInSeconds()))
                                        .replace("{symbol}", contractDetail.getSymbol());
  }

  private String getTransactionMessage(WalletReward walletReward, ContractDetail contractDetail, RewardPeriod periodOfTime) {
    StringBuilder transactionMessage = new StringBuilder();
    Set<WalletPluginReward> walletRewardsByPlugin = walletReward.getRewards();
    for (WalletPluginReward walletPluginReward : walletRewardsByPlugin) {
      Wallet wallet = walletReward.getWallet();
      String transactionMessagePart = DEFAULT_REWARD_MESSAGE_TEMPLATE.replace("{name}", wallet.getName())
                                                                     .replace("{amount}",
                                                                              String.valueOf(walletPluginReward.getAmount()))
                                                                     .replace("{rewardCount}",
                                                                              String.valueOf(walletPluginReward.getPoints()))
                                                                     .replace("{pluginName}",
                                                                              walletPluginReward.getPluginId())
                                                                     .replace("{earned in pool_label}",
                                                                              walletPluginReward.isPoolsUsed()
                                                                                  && StringUtils.isNotBlank(walletReward.getPoolName()) ? ("earned in '"
                                                                                      + walletReward.getPoolName() + "' pool")
                                                                                                                                        : "")
                                                                     .replace("{startDate}",
                                                                              formatTime(periodOfTime.getStartDateInSeconds()))
                                                                     .replace("{endDate}",
                                                                              formatTime(periodOfTime.getEndDateInSeconds()))
                                                                     .replace("{symbol}", contractDetail.getSymbol());

      transactionMessage.append(transactionMessagePart);
      transactionMessage.append("\r\n");
    }
    return transactionMessage.toString();
  }

  private void addTeamRewardRepartition(RewardTeam rewardTeam,
                                        double totalTeamBudget,
                                        double totalTeamPoints,
                                        String pluginId,
                                        Map<Long, Double> earnedPoints,
                                        Set<WalletPluginReward> rewardMemberDetails) {
    if (rewardTeam.getMembers().isEmpty() || totalTeamBudget <= 0 || totalTeamPoints <= 0) {
      return;
    }

    double amountPerPoint = totalTeamBudget / totalTeamPoints;
    rewardTeam.getMembers().forEach(member -> {
      Long identityId = member.getIdentityId();
      Double points = earnedPoints.get(identityId);

      WalletPluginReward rewardMemberDetail = new WalletPluginReward();
      rewardMemberDetail.setIdentityId(identityId);
      rewardMemberDetail.setPluginId(pluginId);
      rewardMemberDetail.setPoints(points);
      rewardMemberDetail.setAmount(points * amountPerPoint);
      rewardMemberDetail.setPoolsUsed(true);
      rewardMemberDetails.add(rewardMemberDetail);
    });
  }

  /**
   * Workaround: WalletTokenAdminService retrieved here instead of dependency
   * injection using constructor because the service is added after
   * PortalContainer startup. (See PLF-8123)
   * 
   * @return wallet token service
   */
  private WalletTokenAdminService getTokenAdminService() {
    if (walletTokenAdminService == null) {
      walletTokenAdminService = CommonsUtils.getService(WalletTokenAdminService.class);
    }
    return walletTokenAdminService;
  }
}
