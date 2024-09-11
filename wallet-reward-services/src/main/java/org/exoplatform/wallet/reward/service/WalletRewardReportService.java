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
package org.exoplatform.wallet.reward.service;

import static org.exoplatform.wallet.utils.RewardUtils.REWARD_TRANSACTION_LABEL_KEY;
import static org.exoplatform.wallet.utils.RewardUtils.REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY;
import static org.exoplatform.wallet.utils.RewardUtils.TRANSACTION_STATUS_PENDING;
import static org.exoplatform.wallet.utils.RewardUtils.TRANSACTION_STATUS_SUCCESS;
import static org.exoplatform.wallet.utils.RewardUtils.formatTime;
import static org.exoplatform.wallet.utils.WalletUtils.convertFromDecimals;
import static org.exoplatform.wallet.utils.WalletUtils.formatNumber;
import static org.exoplatform.wallet.utils.WalletUtils.getContractDetail;
import static org.exoplatform.wallet.utils.WalletUtils.getIdentityByTypeAndId;
import static org.exoplatform.wallet.utils.WalletUtils.getLocale;
import static org.exoplatform.wallet.utils.WalletUtils.getResourceBundleKey;
import static org.exoplatform.wallet.utils.WalletUtils.isUserRewardingAdmin;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.meeds.gamification.service.RealizationService;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.model.reward.RewardBudgetType;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.model.reward.RewardStatus;
import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.model.reward.RewardTeamMember;
import org.exoplatform.wallet.model.reward.WalletPluginReward;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.storage.WalletRewardReportStorage;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTokenAdminService;

import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

/**
 * A service to manage reward reports
 */
@Service
@Primary
public class WalletRewardReportService implements RewardReportService {

  private static final Log                LOG            = ExoLogger.getLogger(WalletRewardReportService.class);

  private static final String             EMPTY_SETTINGS = "Error computing rewards using empty settings";

  @Autowired
  private WalletAccountService      walletAccountService;

  @Autowired
  private WalletTokenAdminService         walletTokenAdminService;

  @Autowired
  private RewardSettingsService     rewardSettingsService;

  @Autowired
  private RewardTeamService         rewardTeamService;

  @Autowired
  private WalletRewardReportStorage rewardReportStorage;

  @Autowired
  private RealizationService        realizationService;

  @Setter
  private boolean                         rewardSendingInProgress;

  @Override
  public void sendRewards(LocalDate date, String username) throws Exception { // NOSONAR
    if (!isUserRewardingAdmin(username)) {
      throw new IllegalAccessException("User " + username + " is not allowed to send rewards");
    }
    RewardReport rewardReport = computeRewards(date);
    if (rewardReport.getPeriod().getEndDateInSeconds() > (System.currentTimeMillis() / 1000)) {
      throw new IllegalStateException("Can't send rewards for current period");
    }

    if (rewardReport.getRewards() == null || rewardReport.getRewards().isEmpty()) {
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
    BigInteger adminTokenBalance = getTokenAdminService().getTokenBalanceOf(adminWalletAddress);
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
        walletReward.setTransaction(transactionDetail);
        getTokenAdminService().reward(transactionDetail, username);
      } catch (Exception e) {
        LOG.warn("Error while sending reward transaction for user '{}'", walletReward.getWallet().getName(), e);
      }
    }
    this.rewardSendingInProgress = true;
    try {
      rewardReportStorage.saveRewardReport(rewardReport);
    } finally {
      this.rewardSendingInProgress = false;
    }
  }

  @Override
  public boolean isRewardSendingInProgress() {
    return rewardSendingInProgress;
  }

  @Override
  public RewardReport computeRewards(LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException("date is mandatory");
    }

    RewardReport rewardReport = getRewardReport(date);
    if (rewardReport == null) {
      rewardReport = new RewardReport();
      rewardReport.setPeriod(getRewardPeriod(date));
    }

    // Only user wallets benefits from rewards
    Set<Wallet> wallets = walletAccountService.listWallets()
                                              .stream()
                                              .filter(wallet -> WalletType.isUser(wallet.getType()))
                                              .collect(Collectors.toSet());
    computeRewardDetails(rewardReport, wallets);
    return rewardReport;
  }

  @Override
  public RewardReport computeRewardsByUser(LocalDate date, long userIdentityId) {
    RewardReport rewardReport = computeRewards(date);
    Set<WalletReward> rewards = rewardReport.getRewards()
                                            .stream()
                                            .filter(reward -> reward.getIdentityId() == userIdentityId)
                                            .collect(Collectors.toSet());
    rewardReport.setRewards(rewards);
    return rewardReport;
  }

  @Override
  public RewardReport getRewardReportByPeriodId(long periodId) {
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    return rewardReportStorage.getRewardReportByPeriodId(periodId, rewardSettings.zoneId());
  }

  @Override
  public RewardReport getRewardReport(LocalDate date) {
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    if (rewardSettings == null) {
      throw new IllegalStateException(EMPTY_SETTINGS);
    }
    if (rewardSettings.getPeriodType() == null) {
      throw new IllegalStateException("Error computing rewards using empty period type");
    }

    return rewardReportStorage.getRewardReport(rewardSettings.getPeriodType(), date, rewardSettings.zoneId());
  }

  @Override
  public RewardPeriod getRewardPeriod(RewardPeriodType periodType, LocalDate date) {
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    return rewardReportStorage.getRewardPeriod(periodType, date, rewardSettings.zoneId());
  }

  @Override
  public void saveRewardReport(RewardReport rewardReport) {
    if (rewardReport == null) {
      throw new IllegalArgumentException("Reward report to save is null");
    }
    rewardReportStorage.saveRewardReport(rewardReport);
  }

  @Override
  public List<RewardPeriod> findRewardReportPeriods(int offset, int limit) {
    return rewardReportStorage.findRewardReportPeriods(offset, limit);
  }

  @Override
  public List<RewardPeriod> findRewardPeriodsBetween(long from, long to, int offset, int limit) {
    return rewardReportStorage.findRewardPeriodsBetween(from, to, offset, limit);
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
      RewardSettings rewardSettings = rewardSettingsService.getSettings();
      return rewardReportStorage.listRewards(Long.parseLong(identity.getId()), rewardSettings.zoneId(), limit);
    }
  }

  @Override
  public double countRewards(String currentUser) {
    Identity identity = getIdentityByTypeAndId(WalletType.USER, currentUser);
    if (identity == null) {
      throw new IllegalArgumentException("can't get user");
    } else {
      return rewardReportStorage.countRewards(Long.parseLong(identity.getId()));
    }
  }

  @Override
  public void replaceRewardTransactions(String oldHash, String newHash) {
    rewardReportStorage.replaceRewardTransactions(oldHash, newHash);
  }

  private RewardPeriod getRewardPeriod(LocalDate date) {
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    RewardPeriodType periodType = rewardSettings.getPeriodType();
    return periodType.getPeriodOfTime(date, rewardSettings.zoneId());
  }

  private void computeRewardDetails(RewardReport rewardReport, Set<Wallet> wallets) {
    // Get te list of enabled reward plugins
    RewardPeriod period = rewardReport.getPeriod();
    Set<WalletReward> walletRewards = retrieveWalletRewards(rewardReport, wallets);
    Set<WalletReward> enabledRewards = walletRewards.stream().filter(WalletReward::isEnabled).collect(Collectors.toSet());

    // Compute rewards per plugin
    Set<WalletPluginReward> walletRewardsByPlugin = new HashSet<>();
    RewardSettings rewardSettings = rewardSettingsService.getSettings();

    Set<Long> identityIds = walletRewards.stream().map(WalletReward::getIdentityId).collect(Collectors.toSet());
    Map<Long, Double> earnedPoints = getEarnedPoints(identityIds, period.getStartDateInSeconds(), period.getEndDateInSeconds());

    computeReward(rewardSettings, earnedPoints, enabledRewards, walletRewardsByPlugin);

    // Assign rewards objects for each wallet,a wallet can have multiple rewards
    // one per plugin
    for (WalletReward walletReward : walletRewards) {
      Set<WalletPluginReward> rewardDetails =
                                            walletRewardsByPlugin.stream()
                                                                 .filter(rewardByPlugin -> rewardByPlugin.getIdentityId() == walletReward.getIdentityId())
                                                                 .collect(Collectors.toSet());
      walletReward.setRewards(rewardDetails);
    }
  }

  private Map<Long, Double> getEarnedPoints(Set<Long> identityIds, long startDateInSeconds, long endDateInSeconds) {
    HashMap<Long, Double> earnedPoints = new HashMap<>();
    if (identityIds == null || identityIds.isEmpty()) {
      return earnedPoints;
    }
    Date startDate = new Date(startDateInSeconds * 1000);
    Date endDate = new Date(endDateInSeconds * 1000);
    Map<Long, Long> points = new HashMap<>();
    try {
      points = realizationService.getScoresByIdentityIdsAndBetweenDates(identityIds.stream().map(Object::toString).toList(),
                                                                        startDate,
                                                                        endDate);
    } catch (Exception e) {
      LOG.warn("Error getting points for user with ids {}", identityIds, e);
    }
    return points.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue()));
  }

  private Set<WalletReward> retrieveWalletRewards(RewardReport rewardReport, Set<Wallet> wallets) {
    Set<WalletReward> walletRewards = rewardReport.getRewards();
    if (walletRewards == null) {
      walletRewards = new HashSet<>();
      rewardReport.setRewards(walletRewards);
    }

    boolean completelyProceeded = rewardReport.isCompletelyProceeded();
    for (Wallet wallet : wallets) {
      List<WalletReward> walletRewardList = walletRewards.stream()
                                                         .filter(wr -> wallet != null && wr.getWallet() != null
                                                             && wr.getIdentityId() == wallet.getTechnicalId())
                                                         .toList();
      WalletReward walletReward =
                                walletRewardList.stream()
                                                .filter(r -> r.getTransaction() != null)
                                                .min((r2, r1) -> Double.compare(r1.getTokensSent(), r2.getTokensSent()))
                                                .orElseGet(() -> walletRewardList.isEmpty() ? null : walletRewardList.getFirst());
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

  private void computeReward(RewardSettings rewardSettings,
                             Map<Long, Double> earnedPoints,
                             Set<WalletReward> enabledRewards,
                             Set<WalletPluginReward> rewardMemberDetails) {
    RewardBudgetType budgetType = rewardSettings.getBudgetType();
    if (budgetType == null) {
      LOG.warn("Budget type of reward is empty, thus no computing is possible");
      return;
    }
    double configuredPluginAmount = rewardSettings.getAmount();
    if (configuredPluginAmount < 0) {
      throw new IllegalStateException("reward amount has a configured negative (" + configuredPluginAmount + ")");
    }

    Set<Long> validIdentityIdsToUse = enabledRewards.stream().map(WalletReward::getIdentityId).collect(Collectors.toSet());
    // Filter non-eligible users switch threshold
    filterEligibleMembers(earnedPoints.entrySet(), validIdentityIdsToUse, rewardSettings, rewardMemberDetails);

    double amountPerPoint;
    double totalFixedBudget;
    switch (budgetType) {
    case FIXED_PER_POINT:
      amountPerPoint = configuredPluginAmount;
      addRewardsSwitchPointAmount(rewardMemberDetails, earnedPoints.entrySet(), amountPerPoint);
      break;
    case FIXED:
      totalFixedBudget = configuredPluginAmount;
      addTeamMembersReward(earnedPoints, totalFixedBudget, rewardMemberDetails);
      break;
    case FIXED_PER_MEMBER:
      int totalEligibleMembersCount = earnedPoints.size();
      totalFixedBudget = configuredPluginAmount * totalEligibleMembersCount;
      addTeamMembersReward(earnedPoints, totalFixedBudget, rewardMemberDetails);
      break;
    default:
      throw new IllegalStateException("Budget type is not recognized, budget type = " + budgetType);
    }
  }

  private void addTeamMembersReward(Map<Long, Double> earnedPoints,
                                    double totalFixedBudget,
                                    Set<WalletPluginReward> rewardMemberDetails) {
    if (totalFixedBudget <= 0) {
      return;
    }
    double amountPerPoint;
    double totalPoints = earnedPoints.values().stream().mapToDouble(v -> v).sum();
    if (totalPoints <= 0) {
      return;
    }
    amountPerPoint = totalFixedBudget / totalPoints;
    addRewardsSwitchPointAmount(rewardMemberDetails, earnedPoints.entrySet(), amountPerPoint);
  }

  private void addRewardsSwitchPointAmount(Set<WalletPluginReward> rewardMemberDetails,
                                           Set<Entry<Long, Double>> identitiesPointsEntries,
                                           double amountPerPoint) {
    for (Entry<Long, Double> identitiyPointsEntry : identitiesPointsEntries) {
      Long identityId = identitiyPointsEntry.getKey();
      Double points = identitiyPointsEntry.getValue();
      double amount = points * amountPerPoint;

      WalletPluginReward rewardMemberDetail = new WalletPluginReward();
      rewardMemberDetail.setIdentityId(identityId);
      rewardMemberDetail.setPoints(points);
      rewardMemberDetail.setAmount(amount);
      rewardMemberDetails.add(rewardMemberDetail);
    }
  }

  private void filterEligibleMembers(Set<Entry<Long, Double>> identitiesPointsEntries,
                                     Set<Long> validIdentityIdsToUse,
                                     RewardSettings rewardSettings,
                                     Set<WalletPluginReward> rewardMemberDetails) {
    double threshold = rewardSettings.getThreshold();

    Iterator<Entry<Long, Double>> identitiesPointsIterator = identitiesPointsEntries.iterator();
    while (identitiesPointsIterator.hasNext()) {
      Map.Entry<java.lang.Long, java.lang.Double> entry = identitiesPointsIterator.next();
      Long identityId = entry.getKey();
      Double points = entry.getValue();
      points = points == null ? 0 : points;
      if (points < 0) {
        throw new IllegalStateException("Negative points has assigned (" + points + ") to user with id " + identityId);
      }

      if (points < threshold || points == 0 || !validIdentityIdsToUse.contains(identityId)) {
        // Member doesn't have enough points or his wallet is disabled => not
        // eligible
        identitiesPointsIterator.remove();

        if (points > 0) {
          // Add member with earned points for information on UI
          WalletPluginReward rewardMemberDetail = new WalletPluginReward();
          rewardMemberDetail.setIdentityId(identityId);
          rewardMemberDetail.setPoints(points);
          rewardMemberDetail.setAmount(0);
          rewardMemberDetails.add(rewardMemberDetail);
        }
      }
    }
  }

  private void buildNoPoolUsers(Map<Long, Double> earnedPoints, List<RewardTeam> teams, Set<Long> identityIds) {
    // Build "No pool" users
    ArrayList<Long> noPoolsIdentityIds = new ArrayList<>(earnedPoints.keySet());
    noPoolsIdentityIds.removeAll(identityIds);
    if (!noPoolsIdentityIds.isEmpty()) {
      RewardTeam noPoolRewardTeam = new RewardTeam();
      noPoolRewardTeam.setDisabled(false);
      List<RewardTeamMember> noPoolRewardTeamList = noPoolsIdentityIds.stream().map(identityId -> {
        RewardTeamMember rewardTeamMember = new RewardTeamMember();
        rewardTeamMember.setIdentityId(identityId);
        return rewardTeamMember;
      }).toList();
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
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    return label.replace("{0}", wallet.getName())
                .replace("{1}", formatNumber(walletReward.getTokensToSend(), locale.getLanguage()))
                .replace("{2}", contractDetail.getSymbol())
                .replace("{3}", formatTime(periodOfTime.getStartDateInSeconds(), rewardSettings.zoneId(), locale.getLanguage()))
                .replace("{4}",
                         formatTime(periodOfTime.getEndDateInSeconds() - 1, rewardSettings.zoneId(), locale.getLanguage()));
  }

  private String getTransactionMessage(WalletReward walletReward, ContractDetail contractDetail, RewardPeriod periodOfTime) {
    StringBuilder transactionMessage = new StringBuilder();
    Set<WalletPluginReward> walletRewardsByPlugin = walletReward.getRewards();
    Locale locale = getLocale(walletReward.getWallet());
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    ZoneId zoneId = rewardSettings.zoneId();

    for (WalletPluginReward walletPluginReward : walletRewardsByPlugin) {// NOSONAR
      String transactionMessagePart;
      String label = getResourceBundleKey(locale, REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY);
      if (StringUtils.isBlank(label)) {
        continue;
      }
      transactionMessagePart =
                             label.replace("{0}", formatNumber(walletPluginReward.getAmount(), locale.getLanguage()))
                                  .replace("{1}", contractDetail.getSymbol())
                                  .replace("{2}", formatNumber(walletPluginReward.getPoints(), locale.getLanguage()))
                                  .replace("{4}", formatTime(periodOfTime.getStartDateInSeconds(), zoneId, locale.getLanguage()))
                                  .replace("{5}",
                                           formatTime(periodOfTime.getEndDateInSeconds() - 1, zoneId, locale.getLanguage()));

      transactionMessage.append(transactionMessagePart);
      transactionMessage.append("\r\n");
    }
    return transactionMessage.toString();
  }

  private void addTeamRewardRepartition(RewardTeam rewardTeam,
                                        double totalTeamBudget,
                                        double totalTeamPoints,
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
      rewardMemberDetail.setPoints(points);
      rewardMemberDetail.setAmount(points * amountPerPoint);
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
