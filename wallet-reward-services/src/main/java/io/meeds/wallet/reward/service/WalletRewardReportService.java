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
package io.meeds.wallet.reward.service;

import static io.meeds.wallet.model.RewardBudgetType.FIXED_PER_MEMBER;
import static io.meeds.wallet.utils.RewardUtils.REWARD_TRANSACTION_LABEL_KEY;
import static io.meeds.wallet.utils.RewardUtils.REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY;
import static io.meeds.wallet.utils.RewardUtils.TRANSACTION_STATUS_PENDING;
import static io.meeds.wallet.utils.RewardUtils.TRANSACTION_STATUS_SUCCESS;
import static io.meeds.wallet.utils.RewardUtils.formatTime;
import static io.meeds.wallet.utils.WalletUtils.convertFromDecimals;
import static io.meeds.wallet.utils.WalletUtils.formatNumber;
import static io.meeds.wallet.utils.WalletUtils.getContractDetail;
import static io.meeds.wallet.utils.WalletUtils.getIdentityByTypeAndId;
import static io.meeds.wallet.utils.WalletUtils.getLocale;
import static io.meeds.wallet.utils.WalletUtils.getResourceBundleKey;
import static io.meeds.wallet.utils.WalletUtils.isUserRewardingAdmin;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.meeds.gamification.constant.RealizationStatus;
import io.meeds.wallet.model.*;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

import io.meeds.gamification.constant.IdentityType;
import io.meeds.gamification.model.filter.RealizationFilter;
import io.meeds.gamification.service.RealizationService;
import io.meeds.wallet.reward.storage.WalletRewardReportStorage;
import io.meeds.wallet.service.WalletAccountService;
import io.meeds.wallet.service.WalletTokenAdminService;

import lombok.Setter;

/**
 * A service to manage reward reports
 */
@Service
public class WalletRewardReportService implements RewardReportService {

  private static final Log          LOG            = ExoLogger.getLogger(WalletRewardReportService.class);

  private static final String       EMPTY_SETTINGS = "Error computing rewards using empty settings";

  private WalletAccountService      walletAccountService;

  private WalletTokenAdminService   walletTokenAdminService;

  private RewardSettingsService     rewardSettingsService;

  private WalletRewardReportStorage rewardReportStorage;

  private RealizationService        realizationService;

  @Setter
  private boolean                   rewardSendingInProgress;

  @Getter
  public Map<Long, Boolean>         rewardSettingChanged = new ConcurrentHashMap<>();

  public WalletRewardReportService(WalletAccountService walletAccountService,
                                   WalletTokenAdminService walletTokenAdminService,
                                   RewardSettingsService rewardSettingsService,
                                   WalletRewardReportStorage rewardReportStorage,
                                   RealizationService realizationService) {
    this.walletAccountService = walletAccountService;
    this.walletTokenAdminService = walletTokenAdminService;
    this.rewardSettingsService = rewardSettingsService;
    this.rewardReportStorage = rewardReportStorage;
    this.realizationService = realizationService;
  }

  @Override
  public void sendRewards(LocalDate date, String username) throws IllegalAccessException { // NOSONAR
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
      if (walletReward == null || !walletReward.isEnabled() || walletReward.getAmount() == 0
          || (walletReward.getTransaction() != null
              && (walletReward.getTransaction().isPending() || walletReward.getTransaction().isSucceeded()))) {
        rewardedWalletsIterator.remove();
        continue;
      }

      if (walletReward.getAmount() < 0) {
        throw new IllegalStateException("Can't send reward transaction for wallet of " + walletReward.getWallet().getType() + " "
            + walletReward.getWallet().getId() + " with a negative amount" + walletReward.getAmount());
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
        transactionDetail.setContractAmount(walletReward.getAmount());
        transactionDetail.setValue(walletReward.getAmount());
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
  @ExoTransactional
  public RewardReportStatus getReport(RewardPeriod rewardPeriod) {
    RewardReport rewardReport = getRewardReport(rewardPeriod.getPeriodMedianDate());
    RewardPeriod storedRewardPeriod = getRewardPeriod(rewardPeriod.getRewardPeriodType(), rewardPeriod.getPeriodMedianDate());
    if (storedRewardPeriod != null && storedRewardPeriod.getId() > 0 && getRewardSettingChanged() != null
        && Boolean.TRUE.equals(getRewardSettingChanged().get(storedRewardPeriod.getId()))) {
      rewardReport = computeRewards(rewardPeriod.getPeriodMedianDate());
      saveRewardReport(rewardReport);
      Map<Long, Boolean> rewardSettingChangedMap = getRewardSettingChanged();
      rewardSettingChangedMap.put(storedRewardPeriod.getId(), false);
      setRewardSettingChanged(rewardSettingChangedMap);
    }
    return buildReportStatus(rewardReport, rewardPeriod);
  }

  @Override
  @ExoTransactional
  public RewardReport computeRewards(LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException("date is mandatory");
    }
    RewardPeriod rewardPeriod = getRewardPeriod(date);
    Date start = new Date(rewardPeriod.getStartDateInSeconds() * 1000L);
    Date end = new Date(rewardPeriod.getEndDateInSeconds() * 1000L);

    RealizationFilter realizationFilter = new RealizationFilter();
    realizationFilter.setEarnerType(IdentityType.USER);
    realizationFilter.setFromDate(start);
    realizationFilter.setToDate(end);

    int participationsCount = realizationService.countRealizationsByFilter(realizationFilter);

    if (participationsCount == 0) {
      RewardReport rewardReport = new RewardReport();
      rewardReport.setPeriod(getRewardPeriod(date));
      rewardReport.setParticipationsCount(0);
      return rewardReport;
    }

    RewardReport rewardReport = getRewardReport(date);
    if (rewardReport == null) {
      rewardReport = new RewardReport();
      rewardReport.setPeriod(getRewardPeriod(date));
    }
    rewardReport.setParticipationsCount(realizationService.countRealizationsByFilter(realizationFilter));

    List<Long> participants = realizationService.getParticipantsBetweenDates(start, end);
    // Only user wallets benefits from rewards
    if (CollectionUtils.isNotEmpty(participants)) {
      Set<Wallet> wallets = walletAccountService.listWalletsByIdentityIds(participants);
      computeRewardDetails(rewardReport, wallets);
    }
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
  public RewardPeriod getRewardPeriodById(long rewardPeriodId) {
    return rewardReportStorage.getRewardPeriodById(rewardPeriodId);
  }

  @Override
  public void saveRewardReport(RewardReport rewardReport) {
    if (rewardReport == null) {
      throw new IllegalArgumentException("Reward report to save is null");
    }
    rewardReportStorage.saveRewardReport(rewardReport);
  }

  @Override
  public DistributionForecast computeDistributionForecast(RewardSettings rewardSettings) {

    RewardPeriod rewardPeriod = RewardPeriod.getCurrentPeriod(rewardSettings);
    Date start = new Date(rewardPeriod.getStartDateInSeconds() * 1000L);
    Date end = new Date(rewardPeriod.getEndDateInSeconds() * 1000L);

    RealizationFilter realizationFilter = new RealizationFilter();
    realizationFilter.setEarnerType(IdentityType.USER);
    realizationFilter.setFromDate(start);
    realizationFilter.setToDate(end);

    List<Long> participants = realizationService.getParticipantsBetweenDates(start, end);

    Set<Wallet> wallets = walletAccountService.listWalletsByIdentityIds(participants)
                                              .stream()
                                              .filter(Wallet::isEnabled)
                                              .collect(Collectors.toSet());

    Map<Long, Double> earnedPoints = getEarnedPoints(wallets.stream().map(Wallet::getTechnicalId).collect(Collectors.toSet()),
                                                     rewardPeriod.getStartDateInSeconds(),
                                                     rewardPeriod.getEndDateInSeconds());
    double acceptedContributions = earnedPoints.values().stream().mapToDouble(Double::doubleValue).sum();

    earnedPoints.entrySet().removeIf(entry -> entry.getValue() < rewardSettings.getThreshold());

    double totalBudget = rewardSettings.getAmount();

    if (FIXED_PER_MEMBER.equals(rewardSettings.getBudgetType())) {
      double totalEligibleMembersCount = earnedPoints.size();
      totalBudget = rewardSettings.getAmount() * totalEligibleMembersCount;
    }

    DistributionForecast distributionForecast = new DistributionForecast();
    distributionForecast.setBudget(totalBudget);
    distributionForecast.setAcceptedContributions(acceptedContributions);
    distributionForecast.setParticipantsCount(participants.size());
    distributionForecast.setEligibleContributorsCount(earnedPoints.size());

    return distributionForecast;
  }

  @Override
  public Page<RewardPeriod> findRewardReportPeriods(Pageable pageable) {
    return rewardReportStorage.findRewardReportPeriods(pageable);
  }

  @Override
  public Page<RewardPeriod> findRewardPeriodsBetween(long from, long to, Pageable pageable) {
    return rewardReportStorage.findRewardPeriodsBetween(from, to, pageable);
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

  @Override
  public Page<WalletReward> findWalletRewardsByPeriodIdAndStatus(long periodId, String status, ZoneId zoneId, Pageable pageable) {
    boolean isValid = !status.equals("INVALID");
    return rewardReportStorage.findWalletRewardsByPeriodIdAndStatus(periodId, isValid, zoneId, pageable);
  }

  public void setRewardSettingChanged(Map<Long, Boolean> updatedSettings) {
    rewardSettingChanged.putAll(updatedSettings);
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

    RewardSettings rewardSettings = rewardSettingsService.getSettings();

    Set<Long> identityIds = walletRewards.stream().map(WalletReward::getIdentityId).collect(Collectors.toSet());
    Map<Long, Double> earnedPoints = getEarnedPoints(identityIds, period.getStartDateInSeconds(), period.getEndDateInSeconds());

    computeReward(rewardSettings, earnedPoints, enabledRewards);
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
    return points.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().doubleValue()));
  }

  private Set<WalletReward> retrieveWalletRewards(RewardReport rewardReport, Set<Wallet> wallets) {
    Set<WalletReward> walletRewards = rewardReport.getRewards();
    if (walletRewards == null) {
      walletRewards = new HashSet<>();
      rewardReport.setRewards(walletRewards);
    }

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
    }
    return walletRewards;
  }

  private void computeReward(RewardSettings rewardSettings, Map<Long, Double> earnedPoints, Set<WalletReward> enabledRewards) {
    RewardBudgetType budgetType = rewardSettings.getBudgetType();
    if (budgetType == null) {
      LOG.warn("Budget type of reward is empty, thus no computing is possible");
      return;
    }
    double configuredPluginAmount = rewardSettings.getAmount();
    if (configuredPluginAmount < 0) {
      throw new IllegalStateException("reward amount has a configured negative (" + configuredPluginAmount + ")");
    }

    // Filter non-eligible users switch threshold
    filterEligibleMembers(earnedPoints.entrySet(), enabledRewards, rewardSettings);

    double totalFixedBudget;
    switch (budgetType) {
    case FIXED:
      totalFixedBudget = configuredPluginAmount;
      addTeamMembersReward(earnedPoints, totalFixedBudget, enabledRewards);
      break;
    case FIXED_PER_MEMBER:
      int totalEligibleMembersCount = earnedPoints.size();
      totalFixedBudget = configuredPluginAmount * totalEligibleMembersCount;
      addTeamMembersReward(earnedPoints, totalFixedBudget, enabledRewards);
      break;
    default:
      throw new IllegalStateException("Budget type is not recognized, budget type = " + budgetType);
    }
  }

  private void addTeamMembersReward(Map<Long, Double> earnedPoints, double totalFixedBudget, Set<WalletReward> enabledRewards) {
    if (totalFixedBudget <= 0) {
      return;
    }
    double amountPerPoint;
    double totalPoints = earnedPoints.values().stream().mapToDouble(v -> v).sum();
    if (totalPoints <= 0) {
      return;
    }
    amountPerPoint = totalFixedBudget / totalPoints;
    addRewardsSwitchPointAmount(enabledRewards, earnedPoints.entrySet(), amountPerPoint);
  }

  private void addRewardsSwitchPointAmount(Set<WalletReward> enabledRewards,
                                           Set<Entry<Long, Double>> identitiesPointsEntries,
                                           double amountPerPoint) {
    for (Entry<Long, Double> identitiyPointsEntry : identitiesPointsEntries) {
      Long identityId = identitiyPointsEntry.getKey();
      Double points = identitiyPointsEntry.getValue();
      double amount = points * amountPerPoint;

      WalletReward walletReward = enabledRewards.stream()
                                                .filter(enabledReward -> enabledReward.getIdentityId() == identityId)
                                                .findFirst()
                                                .orElse(null);
      if (walletReward != null) {
        walletReward.setIdentityId(identityId);
        walletReward.setPoints(points);
        walletReward.setAmount(amount);
      }
    }
  }

  private void filterEligibleMembers(Set<Entry<Long, Double>> identitiesPointsEntries,
                                     Set<WalletReward> enabledRewards,
                                     RewardSettings rewardSettings) {

    Set<Long> validIdentityIdsToUse = enabledRewards.stream().map(WalletReward::getIdentityId).collect(Collectors.toSet());
    double threshold = rewardSettings.getThreshold();

    Iterator<Entry<Long, Double>> identitiesPointsIterator = identitiesPointsEntries.iterator();
    while (identitiesPointsIterator.hasNext()) {
      Entry<Long, Double> entry = identitiesPointsIterator.next();
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
          WalletReward walletReward = enabledRewards.stream()
                                                    .filter(enabledReward -> enabledReward.getIdentityId() == identityId)
                                                    .findFirst()
                                                    .orElse(null);
          if (walletReward != null) {
            walletReward.setPoints(points);
            walletReward.setAmount(0);
          }
        }
      }
    }
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
                .replace("{1}", formatNumber(walletReward.getAmount(), locale.getLanguage()))
                .replace("{2}", contractDetail.getSymbol())
                .replace("{3}", formatTime(periodOfTime.getStartDateInSeconds(), rewardSettings.zoneId(), locale.getLanguage()))
                .replace("{4}",
                         formatTime(periodOfTime.getEndDateInSeconds() - 1, rewardSettings.zoneId(), locale.getLanguage()));
  }

  private String getTransactionMessage(WalletReward walletReward, ContractDetail contractDetail, RewardPeriod periodOfTime) {
    StringBuilder transactionMessage = new StringBuilder();
    Locale locale = getLocale(walletReward.getWallet());
    RewardSettings rewardSettings = rewardSettingsService.getSettings();
    ZoneId zoneId = rewardSettings.zoneId();
    String transactionMessagePart;
    String label = getResourceBundleKey(locale, REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY);
    if (StringUtils.isBlank(label)) {
      return null;
    }
    transactionMessagePart =
                           label.replace("{0}", formatNumber(walletReward.getAmount(), locale.getLanguage()))
                                .replace("{1}", contractDetail.getSymbol())
                                .replace("{2}", formatNumber(walletReward.getPoints(), locale.getLanguage()))
                                .replace("{4}", formatTime(periodOfTime.getStartDateInSeconds(), zoneId, locale.getLanguage()))
                                .replace("{5}", formatTime(periodOfTime.getEndDateInSeconds() - 1, zoneId, locale.getLanguage()));

    transactionMessage.append(transactionMessagePart);
    transactionMessage.append("\r\n");

    return transactionMessage.toString();
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

  private RewardReportStatus buildReportStatus(RewardReport rewardReport, RewardPeriod rewardPeriod) {
    Date fromDate = new Date(rewardPeriod.getStartDateInSeconds() * 1000L);
    Date toDate = new Date(rewardPeriod.getEndDateInSeconds() * 1000L);

    long participantsCount = realizationService.countParticipantsBetweenDates(fromDate, toDate);

    RealizationFilter realizationFilter = new RealizationFilter();
    realizationFilter.setFromDate(fromDate);
    realizationFilter.setToDate(toDate);
    realizationFilter.setEarnerType(IdentityType.USER);
    realizationFilter.setStatus(RealizationStatus.ACCEPTED);
    int achievementsCount = realizationService.countRealizationsByFilter(realizationFilter);
    if (rewardReport == null) {
      if (participantsCount > 0) {
        rewardReport = computeRewards(rewardPeriod.getPeriodMedianDate());
        saveRewardReport(rewardReport);
      } else {
        rewardReport = new RewardReport();
        rewardReport.setPeriod(rewardPeriod);
      }
    }
    WalletReward succeededTransaction = rewardReport.getRewards()
                                                    .stream()
                                                    .filter(reward -> reward.getTransaction() != null
                                                        && reward.getTransaction().isSucceeded())
                                                    .findFirst()
                                                    .orElse(null);

    return new RewardReportStatus(succeededTransaction != null ? succeededTransaction.getTransaction().getSentTimestamp() : 0,
                                  rewardReport.getPeriod(),
                                  participantsCount,
                                  rewardReport.getValidRewardCount(),
                                  achievementsCount,
                                  rewardReport.getTokensSent(),
                                  rewardReport.getTokensToSend(),
                                  CollectionUtils.isNotEmpty(rewardReport.getRewards()) && rewardReport.isCompletelyProceeded());
  }

}
