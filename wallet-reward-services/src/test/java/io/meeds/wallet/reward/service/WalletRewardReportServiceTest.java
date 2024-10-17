/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Lab contact@meedslab.com
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

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

import io.meeds.gamification.model.filter.RealizationFilter;
import io.meeds.gamification.service.RealizationService;

import io.meeds.wallet.model.*;
import io.meeds.wallet.utils.WalletUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.meeds.wallet.reward.storage.WalletRewardReportStorage;
import io.meeds.wallet.service.WalletAccountService;
import io.meeds.wallet.service.WalletTokenAdminService;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { WalletRewardReportService.class })
public class WalletRewardReportServiceTest { // NOSONAR

  private static final String       ADMIN_USER = "root";

  private static final Pageable     PAGEABLE   = Pageable.ofSize(2);

  @MockBean
  private WalletAccountService      walletAccountService;

  @MockBean
  private WalletTokenAdminService   walletTokenAdminService;

  @MockBean
  private RewardSettingsService     rewardSettingsService;

  @MockBean
  private WalletRewardReportStorage rewardReportStorage;

  @MockBean
  private RealizationService        realizationService;

  @Autowired
  private RewardReportService       rewardReportService;

  @BeforeEach
  void setup() {
    IdentityRegistry identityRegistry = PortalContainer.getInstance().getComponentInstanceOfType(IdentityRegistry.class);

    Identity identity = buildUserIdentityAsAdmin();
    identityRegistry.register(identity);
  }

  @Test
  void testComputeRewards() {
    LocalDate date = YearMonth.of(2019, 3).atEndOfMonth();
    RewardSettings rewardSettings = new RewardSettings();
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);
    RewardReport rewardReport = rewardReportService.computeRewards(date);
    assertNotNull(rewardReport);
    assertNotNull(rewardReport.getRewards());
    assertEquals(0, rewardReport.getRewards().size());

    when(realizationService.getParticipantsBetweenDates(any(Date.class), any(Date.class))).thenReturn(List.of(1L, 4L, 5L));
    when(realizationService.countRealizationsByFilter(any(RealizationFilter.class))).thenReturn(10);
    Set<Wallet> participantsWallet = new HashSet<>();
    participantsWallet.add(newWallet(1L));
    participantsWallet.add(newWallet(4L));
    participantsWallet.add(newWallet(5L));
    when(walletAccountService.listWalletsByIdentityIds(List.of(1L, 4L, 5L))).thenReturn(participantsWallet);

    rewardReport = rewardReportService.computeRewards(date);
    assertNotNull(rewardReport);
    // Even if settings are null, the returned rewards shouldn't be empty
    assertEquals(participantsWallet.size(), rewardReport.getRewards().size());
  }

  @Test
  void testGetRewardReportByPeriodId() {

    RewardSettings rewardSettings = new RewardSettings();
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);

    // When
    rewardReportService.getRewardReportByPeriodId(1);
    // Then
    verify(rewardReportStorage, times(1)).getRewardReportByPeriodId(1, rewardSettings.zoneId());

    // When
    rewardReportService.getRewardPeriod(RewardPeriodType.MONTH, LocalDate.now());
    // Then
    verify(rewardReportStorage, times(1)).getRewardPeriod(RewardPeriodType.MONTH, LocalDate.now(), rewardSettings.zoneId());

  }

  @Test
  void testSaveRewardReport() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () -> rewardReportService.saveRewardReport(null));
    assertEquals("Reward report to save is null", exception.getMessage());

    RewardReport rewardReport = new RewardReport();
    rewardReportService.saveRewardReport(rewardReport);
    verify(rewardReportStorage, times(1)).saveRewardReport(rewardReport);
  }

  @Test
  void testGetRewardReport() {
    RewardSettings rewardSettings = new RewardSettings();
    rewardSettings.setPeriodType(null);
    when(rewardSettingsService.getSettings()).thenReturn(null);
    Throwable exception = assertThrows(IllegalStateException.class, () -> rewardReportService.getRewardReport(LocalDate.now()));
    assertEquals("Error computing rewards using empty settings", exception.getMessage());

    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);
    exception = assertThrows(IllegalStateException.class, () -> rewardReportService.getRewardReport(LocalDate.now()));
    assertEquals("Error computing rewards using empty period type", exception.getMessage());

    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());

    // When
    rewardReportService.getRewardReportByPeriodId(1);
    // Then
    verify(rewardReportStorage, times(1)).getRewardReportByPeriodId(1, rewardSettings.zoneId());

  }

  @Test
    void testGetReportStatus() {
        when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());

        RewardPeriod rewardPeriod = new RewardPeriod(RewardPeriodType.WEEK, ZoneId.systemDefault().getId(), 1725832800, 1726437600);
        // When
        rewardReportService.getReport(rewardPeriod);
        // Then
        verify(realizationService, times(1)).countParticipantsBetweenDates(any(Date.class), any(Date.class));
        verify(realizationService, times(1)).countRealizationsByFilter(any(RealizationFilter.class));
    }

  @Test
  void testFindRewardReportPeriods() {
        rewardReportService.findRewardReportPeriods(PAGEABLE);
        verify(rewardReportStorage, times(1)).findRewardReportPeriods(PAGEABLE);

    }

  @Test
  void testFindRewardPeriodsBetween() {
    rewardReportService.findRewardPeriodsBetween(1725832800, 1726437600, PAGEABLE);
    verify(rewardReportStorage, times(1)).findRewardPeriodsBetween(1725832800, 1726437600, PAGEABLE);

  }

  @Test
  void testGetRewardPeriodsInProgress() {
    rewardReportService.getRewardPeriodsInProgress();
    verify(rewardReportStorage, times(1)).findRewardPeriodsByStatus(RewardStatus.PENDING);

  }

  @Test
  void testFindWalletRewardsByPeriodIdAndStatus() {
    RewardSettings rewardSettings = new RewardSettings();

    rewardReportService.findWalletRewardsByPeriodIdAndStatus(1, "VALID", rewardSettings.zoneId(), PAGEABLE);
    verify(rewardReportStorage, times(1)).findWalletRewardsByPeriodIdAndStatus(1, true, rewardSettings.zoneId(), PAGEABLE);

    rewardReportService.findWalletRewardsByPeriodIdAndStatus(1, "INVALID", rewardSettings.zoneId(), PAGEABLE);
    verify(rewardReportStorage, times(1)).findWalletRewardsByPeriodIdAndStatus(1, false, rewardSettings.zoneId(), PAGEABLE);
  }

  @Test
  void testGetRewardPeriodsNotSent() {
    rewardReportService.getRewardPeriodsNotSent();
    verify(rewardReportStorage, times(1)).findRewardPeriodsByStatus(RewardStatus.ESTIMATION);
  }

  @Test
  void testComputeRewardsByUser() {
    LocalDate date = YearMonth.of(2022, 12).atEndOfMonth();
    RewardSettings rewardSettings = new RewardSettings();
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);
    when(realizationService.getParticipantsBetweenDates(any(Date.class), any(Date.class))).thenReturn(List.of(1L, 4L, 5L));
    when(realizationService.countRealizationsByFilter(any(RealizationFilter.class))).thenReturn(10);
    Set<Wallet> participantsWallet = new HashSet<>();
    participantsWallet.add(newWallet(1L));
    participantsWallet.add(newWallet(4L));
    participantsWallet.add(newWallet(5L));
    when(walletAccountService.listWalletsByIdentityIds(List.of(1L, 4L, 5L))).thenReturn(participantsWallet);

    RewardReport rewardReport = rewardReportService.computeRewardsByUser(date, 1L);
    assertNotNull(rewardReport);
    assertNotNull(rewardReport.getRewards());
    assertEquals(1, rewardReport.getRewards().size());
  }

  @Test
  void testSendRewards() throws Exception {
    try (MockedStatic<WalletUtils> walletUtilsMockedStatic = Mockito.mockStatic(WalletUtils.class)) {
      ContractDetail mockContractDetail = new ContractDetail();
      mockContractDetail.setDecimals(12);
      walletUtilsMockedStatic.when(WalletUtils::getContractDetail).thenReturn(mockContractDetail);
      walletUtilsMockedStatic.when(() -> WalletUtils.convertFromDecimals(any(BigInteger.class), anyInt())).thenReturn(10.0);

      int contractDecimals = WalletUtils.getContractDetail().getDecimals();

      LocalDate date = YearMonth.of(2019, 4).atEndOfMonth();

      RewardSettings newSettings = new RewardSettings();

      newSettings.setPeriodType(RewardPeriodType.MONTH);
      double sumOfTokensToSend = 5490d;
      newSettings.setBudgetType(RewardBudgetType.FIXED);
      newSettings.setAmount(sumOfTokensToSend);
      newSettings.setThreshold(0);
      when(rewardSettingsService.getSettings()).thenReturn(newSettings);

      Map<Long, Long> points = new HashMap<>();
      points.put(1L, 50L);
      points.put(4L, 100L);
      points.put(5L, 40L);
      when(realizationService.getScoresByIdentityIdsAndBetweenDates(anyList(),
                                                                    any(Date.class),
                                                                    any(Date.class))).thenReturn(points);
      when(realizationService.getParticipantsBetweenDates(any(Date.class), any(Date.class))).thenReturn(List.of(1L, 4L, 5L));
      when(realizationService.countRealizationsByFilter(any(RealizationFilter.class))).thenReturn(10);
      Set<Wallet> participantsWallet = new HashSet<>();
      Wallet wallet = newWallet(1L);
      Wallet wallet4 = newWallet(4L);
      Wallet wallet5 = newWallet(5L);
      participantsWallet.add(wallet);
      participantsWallet.add(wallet4);
      participantsWallet.add(wallet5);
      when(walletAccountService.listWalletsByIdentityIds(List.of(1L, 4L, 5L))).thenReturn(participantsWallet);

      // Admin having only 10 tokens
      when(walletTokenAdminService.getTokenBalanceOf("adminAddress")).thenReturn(BigInteger.valueOf(10L).pow(contractDecimals));

      Throwable exception = assertThrows(IllegalAccessException.class, () -> rewardReportService.sendRewards(date, ADMIN_USER));
      assertEquals("User " + ADMIN_USER + " is not allowed to send rewards", exception.getMessage());

      walletUtilsMockedStatic.when(() -> WalletUtils.isUserRewardingAdmin(ADMIN_USER)).thenReturn(true);

      // Sending when No admin wallet is configured
      exception = assertThrows(IllegalStateException.class, () -> rewardReportService.sendRewards(date, ADMIN_USER));
      assertEquals("No admin wallet is configured", exception.getMessage());

      when(walletTokenAdminService.getAdminWalletAddress()).thenReturn("adminAddress");

      exception = assertThrows(IllegalStateException.class, () -> rewardReportService.sendRewards(date, ADMIN_USER));
      assertEquals("Admin doesn't have enough funds to send rewards", exception.getMessage());

      // Admin having enough funds
      walletUtilsMockedStatic.when(() -> WalletUtils.convertFromDecimals(any(BigInteger.class), anyInt())).thenReturn(5491.0);

      when(walletTokenAdminService.getTokenBalanceOf("adminAddress")).thenReturn(BigInteger.valueOf((long) sumOfTokensToSend + 1)
                                                                                           .pow(contractDecimals));

      rewardReportService.sendRewards(date, ADMIN_USER);
      verify(walletTokenAdminService, times(3)).reward(any(), any());

      // Send reward for the second time for the same period
      when(walletTokenAdminService.getTokenBalanceOf("adminAddress")).thenReturn(BigInteger.valueOf((long) sumOfTokensToSend + 1)
                                                                                           .pow(contractDecimals));

      RewardReport rewardReport = new RewardReport();
      RewardPeriod rewardPeriod = new RewardPeriod();
      rewardReport.setPeriod(rewardPeriod);
      rewardReport.setParticipationsCount(10);
      Set<WalletReward> walletRewards = new HashSet<>();
      TransactionDetail transactionDetail = new TransactionDetail();
      transactionDetail.setSucceeded(true);
      walletRewards.add(new WalletReward(wallet, transactionDetail, 1L, 100, 10, rewardPeriod));
      walletRewards.add(new WalletReward(wallet4, transactionDetail, 4L, 200, 50, rewardPeriod));
      walletRewards.add(new WalletReward(wallet5, transactionDetail, 5L, 300, 40, rewardPeriod));
      rewardReport.setRewards(walletRewards);
      when(rewardReportStorage.getRewardReport(newSettings.getPeriodType(), date, newSettings.zoneId())).thenReturn(rewardReport);

      exception = assertThrows(IllegalStateException.class, () -> rewardReportService.sendRewards(date, ADMIN_USER));
      assertEquals("No rewards to send for selected period", exception.getMessage());

      // Sending reward for current period
      transactionDetail.setSucceeded(false);
      rewardPeriod.setEndDateInSeconds(System.currentTimeMillis() / 1000 + 1);
      exception = assertThrows(IllegalStateException.class, () -> rewardReportService.sendRewards(date, ADMIN_USER));
      assertEquals("Can't send rewards for current period", exception.getMessage());

      // Sending reward for current period
      transactionDetail.setPending(true);
      rewardPeriod.setEndDateInSeconds(System.currentTimeMillis() / 1000 - 5);
      exception = assertThrows(IllegalStateException.class, () -> rewardReportService.sendRewards(date, ADMIN_USER));
      String startDateFormatted = rewardReport.getPeriod().getStartDateFormatted(Locale.getDefault().getLanguage());
      String endDateFormatted = rewardReport.getPeriod().getEndDateFormatted(Locale.getDefault().getLanguage());
      assertEquals("There are some pending transactions for rewards of period between " + startDateFormatted + " and "
          + endDateFormatted + ", thus no reward sending is allowed until the transactions finishes", exception.getMessage());
    }
  }

  @Test
  void testComputeDistributionForecast() {
    RewardSettings rewardSettings = new RewardSettings();

    rewardSettings.setThreshold(49);
    when(realizationService.getParticipantsBetweenDates(any(Date.class), any(Date.class))).thenReturn(List.of(1L, 4L, 5L));
    when(realizationService.countRealizationsByFilter(any(RealizationFilter.class))).thenReturn(10);
    Set<Wallet> participantsWallet = new HashSet<>();
    participantsWallet.add(newWallet(1L));
    participantsWallet.add(newWallet(4L));
    participantsWallet.add(newWallet(5L));
    when(walletAccountService.listWalletsByIdentityIds(List.of(1L, 4L, 5L))).thenReturn(participantsWallet);
    Map<Long, Long> points = new HashMap<>();
    points.put(1L, 50L);
    points.put(4L, 100L);
    points.put(5L, 40L);
    when(realizationService.getScoresByIdentityIdsAndBetweenDates(anyList(),
                                                                  any(Date.class),
                                                                  any(Date.class))).thenReturn(points);
    DistributionForecast distributionForecast = rewardReportService.computeDistributionForecast(rewardSettings);

    assertNotNull(distributionForecast);
    assertEquals(3, distributionForecast.getParticipantsCount());
    assertEquals(2, distributionForecast.getEligibleContributorsCount());
    assertEquals(190.0, distributionForecast.getAcceptedContributions());
  }

  @Test
  void testListRewards() {
    // Given
    RewardSettings rewardSettings = new RewardSettings();
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);

    // When
    rewardReportService.listRewards("root", 10);
    rewardReportService.countRewards("root");

    // Then
    verify(rewardReportStorage, times(1)).listRewards(1, rewardSettings.zoneId(), 10);
    verify(rewardReportStorage, times(1)).countRewards(1);
  }

  protected Wallet newWallet(long identityId) {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(identityId);
    wallet.setAddress("walletAddress" + identityId);
    wallet.setPassPhrase("passphrase");
    wallet.setEnabled(true);
    wallet.setIsInitialized(true);
    wallet.setEtherBalance(0d);
    wallet.setTokenBalance(0d);
    wallet.setInitializationState(WalletState.INITIALIZED.name());
    return wallet;
  }

  protected org.exoplatform.services.security.Identity buildUserIdentityAsAdmin() {
    String group = "/platform/rewarding";
    MembershipEntry entry = new MembershipEntry(group, MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet<>();
    entryTest.add(entry);
    return new org.exoplatform.services.security.Identity(WalletRewardReportServiceTest.ADMIN_USER, entryTest);
  }
}
