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
package org.exoplatform.wallet.reward.service;

import static org.exoplatform.wallet.utils.RewardUtils.*;
import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.model.*;
import org.exoplatform.wallet.model.reward.*;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.api.RewardPlugin;
import org.exoplatform.wallet.reward.storage.RewardReportStorage;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTokenAdminService;

/**
 * A service to manage reward reports
 */
public class WalletRewardReportService implements RewardReportService {

  private static final Log        LOG = ExoLogger.getLogger(WalletRewardReportService.class);

  private WalletAccountService    walletAccountService;

  private WalletTokenAdminService walletTokenAdminService;

  private RewardSettingsService   rewardSettingsService;

  private RewardTeamService       rewardTeamService;

  private RewardReportStorage     rewardReportStorage;

  public WalletRewardReportService(WalletAccountService walletAccountService,
                                   RewardSettingsService rewardSettingsService,
                                   RewardTeamService rewardTeamService,
                                   RewardReportStorage rewardReportStorage) {
    this.walletAccountService = walletAccountService;
    this.rewardSettingsService = rewardSettingsService;
    this.rewardTeamService = rewardTeamService;
    this.rewardReportStorage = rewardReportStorage;
  }

  @Override
  public void sendRewards(long periodDateInSeconds, String username) throws Exception {
    if (!isUserRewardingAdmin(username)) {
      throw new IllegalAccessException("User " + username + " is not allowed to send rewards");
    }
    RewardReport rewardReport = computeRewards(periodDateInSeconds);
    if (rewardReport.getPeriod().getEndDateInSeconds() > (System.currentTimeMillis() / 1000)) {
      throw new IllegalStateException("Can't send rewards for current period");
    }

    if (rewardReport == null || rewardReport.getRewards() == null || rewardReport.getRewards().isEmpty()) {
      return;
    }

    if (rewardReport.getPendingTransactionCount() > 0) {
      String startDateFormatted = rewardReport.getPeriod().getStartDateFormatted(Locale.getDefault().getLanguage());
      String endDateFormatted = rewardReport.getPeriod().getEndDateFormatted(Locale.getDefault().getLanguage());
      throw new IllegalStateException("There are some pending transactions for rewards of period between " + startDateFormatted
          + " and " + endDateFormatted + ", thus no reward sending is allowed until the transactions finishes");
    }

    String adminWalletAddress = getTokenAdminService().getAdminWalletAddress();
    if (StringUtils.isBlank(adminWalletAddress)) {
      throw new IllegalStateException("No admin wallet is configured");
    }
    if (getTokenAdminService().getAdminLevel(adminWalletAddress) < 2) {
      throw new IllegalStateException("Configured admin wallet is not configured as admin on token. It must be a Token admin with level 2 at least.");
    }

    Set<WalletReward> rewards = new HashSet<>(rewardReport.getRewards());
    Iterator<WalletReward> rewardedWalletsIterator = rewards.iterator();
    while (rewardedWalletsIterator.hasNext()) {
      WalletReward walletReward = rewardedWalletsIterator.next();
      if (walletReward == null || !walletReward.isEnabled() || walletReward.getRewards() == null
          || walletReward.getTokensToSend() == 0 || (walletReward.getTransaction() != null
              && (walletReward.getTransaction().isPending() || walletReward.getTransaction().isSucceeded()))) {
        rewardedWalletsIterator.remove();
        continue;
      }

      if (walletReward.getTokensToSend() < 0) {
        throw new IllegalStateException("Can't send reward transaction for wallet of " + walletReward.getWallet().getType() + " "
            + walletReward.getWallet().getId() + " with a negative amount" + walletReward.getTokensToSend());
      }
      // If the tokens are already sent, then ignore sending rewards to user
      // If the sent transaction is pending, an exception is thrown ate the
      // start of the method
      // If the transaction is failed, then re-send rewards
      // Else (no transaction already made) re-send rewards
      if (StringUtils.equals(walletReward.getStatus(), TRANSACTION_STATUS_SUCCESS)
          || StringUtils.equals(walletReward.getStatus(), TRANSACTION_STATUS_PENDING)) {
        rewardedWalletsIterator.remove();
      }
    }

    if (rewards.isEmpty()) {
      throw new IllegalStateException("No rewards to send for selected period");
    }
    ContractDetail contractDetail = getContractDetail();
    BigInteger adminTokenBalance = getTokenAdminService().balanceOf(adminWalletAddress);
    double adminBalance = convertFromDecimals(adminTokenBalance, contractDetail.getDecimals());
    double rewardsAmount = rewardReport.getRemainingTokensToSend();

    if (rewardsAmount > adminBalance) {
      throw new IllegalStateException("Admin doesn't have enough funds to send rewards");
    }

    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    for (WalletReward walletReward : rewards) {
      try {
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setFrom(adminWalletAddress);
        transactionDetail.setTo(walletReward.getWallet().getAddress());
        transactionDetail.setContractAmount(walletReward.getTokensToSend());
        transactionDetail.setValue(walletReward.getTokensToSend());
        String transactionLabel = getTransactionLabel(walletReward, contractDetail, rewardPeriod);
        transactionDetail.setLabel(transactionLabel);
        String transactionMessage = getTransactionMessage(walletReward, contractDetail, rewardPeriod);
        transactionDetail.setMessage(transactionMessage);
        transactionDetail = getTokenAdminService().reward(transactionDetail, username);
        walletReward.setTransaction(transactionDetail);
      } catch (Exception e) {
        LOG.warn("Error while sending reward transaction for user '{}'", walletReward.getWallet().getName(), e);
      }
    }
    rewardReportStorage.saveRewardReport(rewardReport);
  }

  @Override
  public RewardReport computeRewards(long periodDateInSeconds) {
    if (periodDateInSeconds == 0) {
      throw new IllegalArgumentException("periodDate is mandatory");
    }

    RewardReport rewardReport = getRewardReport(periodDateInSeconds);
    if (rewardReport == null) {
      rewardReport = new RewardReport();
      rewardReport.setPeriod(getRewardPeriod(periodDateInSeconds));
    }

    // Only user wallets benifits from rewards
    Set<Wallet> wallets = walletAccountService.listWallets()
                                              .stream()
                                              .filter(wallet -> WalletType.isUser(wallet.getType()))
                                              .collect(Collectors.toSet());
    computeRewardDetails(rewardReport, wallets);
    return rewardReport;
  }

  @Override
  public RewardReport getRewardReport(long periodTimeInSeconds) {
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    if (rewardSettings == null) {
      throw new IllegalStateException("Error computing rewards using empty settings");
    }
    if (rewardSettings.getPeriodType() == null) {
      throw new IllegalStateException("Error computing rewards using empty period type");
    }

    return rewardReportStorage.getRewardReport(rewardSettings.getPeriodType(), periodTimeInSeconds);
  }

  @Override
  public void saveRewardReport(RewardReport rewardReport) {
    if (rewardReport == null) {
      throw new IllegalArgumentException("Reward report to save is null");
    }
    rewardReportStorage.saveRewardReport(rewardReport);
  }

  @Override
  public List<RewardPeriod> getRewardPeriodsInProgress() {
    return rewardReportStorage.findRewardPeriodsByStatus(RewardStatus.PENDING);
  }

  @Override
  public List<RewardPeriod> getRewardPeriodsNotSent() {
    return rewardReportStorage.findRewardPeriodsByStatus(RewardStatus.ESTIMATION);
  }

  @Override
  public List<WalletReward> listRewards(String currentUser, int limit) {
    Identity identity = getIdentityByTypeAndId(WalletType.USER, currentUser);
    if (identity == null) {
      return Collections.emptyList();
    } else {
      return rewardReportStorage.listRewards(Long.parseLong(identity.getId()), limit);
    }
  }

  private RewardPeriod getRewardPeriod(long periodDateInSeconds) {
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    RewardPeriodType periodType = rewardSettings.getPeriodType();
    return periodType.getPeriodOfTime(timeFromSeconds(periodDateInSeconds));
  }

  private void computeRewardDetails(RewardReport rewardReport, Set<Wallet> wallets) {
    // Get te list of enabled reward plugins
    Map<RewardPlugin, RewardPluginSettings> rewardPlugins = getEnabledRewardPlugins();
    RewardPeriod period = rewardReport.getPeriod();
    Set<WalletReward> walletRewards = retrieveWalletRewards(rewardReport, wallets);
    Set<WalletReward> enabledRewards = walletRewards.stream()
                                                    .filter(wr -> wr.isEnabled())
                                                    .collect(Collectors.toSet());
    Set<WalletReward> enabledTeamRewards = enabledRewards.stream()
                                                         .filter(wr -> wr.getTeam() == null || !wr.getTeam().isDisabled())
                                                         .collect(Collectors.toSet());

    // Compute rewards per plugin
    Set<WalletPluginReward> walletRewardsByPlugin = new HashSet<>();
    for (Map.Entry<RewardPlugin, RewardPluginSettings> pluginEntry : rewardPlugins.entrySet()) {
      RewardPlugin plugin = pluginEntry.getKey();
      RewardPluginSettings pluginSettings = pluginEntry.getValue();

      Set<Long> identityIds = walletRewards.stream().map(wr -> wr.getIdentityId()).collect(Collectors.toSet());
      Map<Long, Double> earnedPoints = plugin.getEarnedPoints(identityIds,
                                                              period.getStartDateInSeconds(),
                                                              period.getEndDateInSeconds());

      Set<WalletReward> validWalletRewards = pluginSettings.isUsePools() ? enabledTeamRewards : enabledRewards;
      computeReward(pluginSettings, earnedPoints, validWalletRewards, walletRewardsByPlugin);
    }

    // Assign rewards objects for each wallet,a wallet can have multiple rewards
    // one per plugin
    for (WalletReward walletReward : walletRewards) {
      Set<WalletPluginReward> rewardDetails = walletRewardsByPlugin.stream()
                                                                   .filter(rewardByPlugin -> rewardByPlugin.getIdentityId() == walletReward.getIdentityId())
                                                                   .collect(Collectors.toSet());
      walletReward.setRewards(rewardDetails);
    }
  }

  private Set<WalletReward> retrieveWalletRewards(RewardReport rewardReport, Set<Wallet> wallets) {
    Set<WalletReward> walletRewards = rewardReport.getRewards();
    if (walletRewards == null) {
      walletRewards = new HashSet<>();
      rewardReport.setRewards(walletRewards);
    }

    boolean completelyProceeded = rewardReport.isCompletelyProceeded();
    for (Wallet wallet : wallets) {
      WalletReward walletReward = walletRewards.stream()
                                               .filter(wr -> wallet != null && wr.getWallet() != null
                                                   && wr.getIdentityId() == wallet.getTechnicalId())
                                               .findFirst()
                                               .orElse(null);
      if (walletReward == null) {
        walletReward = new WalletReward();
        walletRewards.add(walletReward);
      }
      walletReward.setWallet(wallet);

      if (!completelyProceeded) {
        List<RewardTeam> rewardTeams = rewardTeamService.findTeamsByMemberId(walletReward.getIdentityId());
        walletReward.setTeams(rewardTeams);
      }
    }
    return walletRewards;
  }

  private Map<RewardPlugin, RewardPluginSettings> getEnabledRewardPlugins() {
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    final Set<RewardPluginSettings> pluginSettings = rewardSettings.getPluginSettings()
                                                                   .stream()
                                                                   .filter(RewardPluginSettings::isEnabled)
                                                                   .collect(Collectors.toSet());
    Collection<RewardPlugin> rewardPlugins = rewardSettingsService.getRewardPlugins();
    return rewardPlugins.stream()
                        .filter(RewardPlugin::isEnabled)
                        .filter(rewardPlugin -> getPluginSetting(pluginSettings, rewardPlugin.getPluginId()) != null)
                        .collect(Collectors.toMap(Function.identity(),
                                                  rewardPlugin -> getPluginSetting(pluginSettings, rewardPlugin.getPluginId())));
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
                             Set<WalletReward> enabledRewards,
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

    Set<Long> validIdentityIdsToUse = enabledRewards.stream().map(WalletReward::getIdentityId).collect(Collectors.toSet());
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
      if (rewardTeam.getMembers() == null || rewardTeam.getMembers().isEmpty()) {
        continue;
      }
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
        if (rewardTeam.getMembers() != null && !rewardTeam.getMembers().isEmpty()) {
          double totalTeamBudget = budgetPerTeamMember * rewardTeam.getMembers().size();
          Double totalTeamPoints = totalPointsPerTeam.get(rewardTeam.getId());
          addTeamRewardRepartition(rewardTeam,
                                   totalTeamBudget,
                                   totalTeamPoints,
                                   pluginId,
                                   earnedPoints,
                                   rewardMemberDetails);
        }
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
      if (members == null || members.isEmpty()) {
        teamsIterator.remove();
      } else {
        Iterator<RewardTeamMember> membersIterator = members.iterator();
        while (membersIterator.hasNext()) {
          RewardTeamMember member = membersIterator.next();
          Long identityId = member.getIdentityId();
          identityIds.add(identityId);
          // Retain in Teams collection only elligible members
          if (!earnedPoints.containsKey(identityId)) {
            membersIterator.remove();
          }
        }
      }
    }
    return identityIds;
  }

  private String getTransactionLabel(WalletReward walletReward, ContractDetail contractDetail, RewardPeriod periodOfTime) {
    Wallet wallet = walletReward.getWallet();
    Locale locale = getLocale(wallet);
    String label = getResourceBundleKey(locale, REWARD_TRANSACTION_LABEL_KEY);
    if (StringUtils.isBlank(label)) {
      return "";
    }
    return label.replace("{0}", wallet.getName())
                .replace("{1}", formatNumber(walletReward.getTokensToSend(), locale.getLanguage()))
                .replace("{2}", contractDetail.getSymbol())
                .replace("{3}", formatTime(periodOfTime.getStartDateInSeconds(), locale.getLanguage()))
                .replace("{4}", formatTime(periodOfTime.getEndDateInSeconds() - 1, locale.getLanguage()));
  }

  private String getTransactionMessage(WalletReward walletReward, ContractDetail contractDetail, RewardPeriod periodOfTime) {
    StringBuilder transactionMessage = new StringBuilder();
    Set<WalletPluginReward> walletRewardsByPlugin = walletReward.getRewards();
    Locale locale = getLocale(walletReward.getWallet());

    for (WalletPluginReward walletPluginReward : walletRewardsByPlugin) {
      String transactionMessagePart = null;
      if (walletPluginReward.isPoolsUsed() && StringUtils.isNotBlank(walletReward.getPoolName())) {
        String label = getResourceBundleKey(locale, REWARD_TRANSACTION_WITH_POOL_MESSAGE_KEY);
        if (StringUtils.isBlank(label)) {
          continue;
        }
        transactionMessagePart = label.replace("{0}", formatNumber(walletPluginReward.getAmount(), locale.getLanguage()))
                                      .replace("{1}", contractDetail.getSymbol())
                                      .replace("{2}", formatNumber(walletPluginReward.getPoints(), locale.getLanguage()))
                                      .replace("{3}", walletPluginReward.getPluginId())
                                      .replace("{4}", walletReward.getPoolName())
                                      .replace("{5}", formatTime(periodOfTime.getStartDateInSeconds(), locale.getLanguage()))
                                      .replace("{6}", formatTime(periodOfTime.getEndDateInSeconds() - 1, locale.getLanguage()));

      } else {
        String label = getResourceBundleKey(locale, REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY);
        if (StringUtils.isBlank(label)) {
          continue;
        }
        transactionMessagePart = label.replace("{0}", formatNumber(walletPluginReward.getAmount(), locale.getLanguage()))
                                      .replace("{1}", contractDetail.getSymbol())
                                      .replace("{2}", formatNumber(walletPluginReward.getPoints(), locale.getLanguage()))
                                      .replace("{3}", walletPluginReward.getPluginId())
                                      .replace("{4}", formatTime(periodOfTime.getStartDateInSeconds(), locale.getLanguage()))
                                      .replace("{5}", formatTime(periodOfTime.getEndDateInSeconds() - 1, locale.getLanguage()));
      }

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
    if (rewardTeam.getMembers() == null || rewardTeam.getMembers().isEmpty() || totalTeamBudget <= 0 || totalTeamPoints <= 0) {
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
