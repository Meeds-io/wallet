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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.meeds.wallet.model.*;
import io.meeds.wallet.reward.dao.RewardDAO;
import io.meeds.wallet.reward.dao.RewardPeriodDAO;
import io.meeds.wallet.reward.entity.WalletRewardEntity;
import io.meeds.wallet.reward.entity.WalletRewardPeriodEntity;
import io.meeds.wallet.service.WalletAccountService;
import io.meeds.wallet.service.WalletTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@SpringBootTest(classes = { WalletRewardReportStorage.class, })
@ExtendWith(MockitoExtension.class)
class WalletRewardReportStorageTest {

  private static final Long         REWARD_ID        = 2L;

  private static final Long         REWARD_PERIOD_ID = 2L;

  private static final Long         IDENTITY_ID      = 5L;

  private static final Pageable     PAGEABLE         = Pageable.ofSize(2);

  @MockBean
  private RewardDAO                 rewardDAO;

  @MockBean
  private RewardPeriodDAO           rewardPeriodDAO;

  @MockBean
  private WalletAccountService      walletAccountService;

  @MockBean
  private WalletTransactionService  walletTransactionService;

  @Autowired
  private WalletRewardReportStorage walletRewardReportStorage;

  @BeforeEach
    void setup() {
        when(rewardDAO.save(any())).thenAnswer(invocation -> {
            WalletRewardEntity rewardEntity = invocation.getArgument(0);
            if (rewardEntity.getId() == null) {
                rewardEntity.setId(REWARD_ID);
            }
            when(rewardDAO.findById(REWARD_ID)).thenReturn(Optional.of(rewardEntity));
            when(rewardDAO.findWalletRewardEntitiesByIdentityId(IDENTITY_ID, PAGEABLE)).thenReturn(List.of(rewardEntity));
            when(rewardDAO.findAll(PAGEABLE)).thenReturn(new PageImpl<>(List.of(rewardEntity)));
            when(rewardDAO.count()).thenReturn(1L);
            when(rewardDAO.countWalletRewardEntitiesByIdentityId(IDENTITY_ID)).thenReturn(1.0);
            when(rewardDAO.findRewardsByPeriodId(REWARD_ID)).thenReturn(List.of(rewardEntity));
            return rewardEntity;
        });
        doAnswer(invocation -> {
            WalletRewardEntity entity = invocation.getArgument(0);
            when(rewardDAO.findById(entity.getId())).thenReturn(Optional.empty());
            return null;
        }).when(rewardDAO).delete(any());

      when(rewardPeriodDAO.save(any())).thenAnswer(invocation -> {
          WalletRewardPeriodEntity entity = invocation.getArgument(0);
          if (entity.getId() == null) {
              entity.setId(REWARD_PERIOD_ID);
          }
          if (entity.getPeriodType() == null) {
              entity.setPeriodType(RewardPeriodType.WEEK);
          }
          when(rewardPeriodDAO.findById(REWARD_PERIOD_ID)).thenReturn(Optional.of(entity));
          when(rewardPeriodDAO.findRewardPeriodsBetween(12125, 222125 ,PAGEABLE)).thenReturn(new PageImpl<>(List.of(entity)));
          when(rewardPeriodDAO.findAll(PAGEABLE)).thenReturn(new PageImpl<>(List.of(entity)));
          when(rewardPeriodDAO.findRewardPeriodByTypeAndTime(any(RewardPeriodType.class), anyLong())).thenReturn(entity);
          when(rewardPeriodDAO.findWalletRewardPeriodEntitiesByStatus(any(RewardStatus.class))).thenReturn(List.of(entity));
          when(rewardPeriodDAO.count()).thenReturn(1L);
          return entity;
      });
      doAnswer(invocation -> {
          WalletRewardPeriodEntity entity = invocation.getArgument(0);
          when(rewardPeriodDAO.findById(entity.getId())).thenReturn(Optional.empty());
          return null;
      }).when(rewardPeriodDAO).delete(any());
    }

  @Test
  void countRewards() {
    // Given
    RewardReport rewardReport = createRewardReportInstance();

    // When
    walletRewardReportStorage.saveRewardReport(rewardReport);

    // Then
    assertEquals(1.0, walletRewardReportStorage.countRewards(IDENTITY_ID));
  }

  @Test
  void listRewards() {
    // Given
    RewardReport rewardReport = createRewardReportInstance();

    // When
    walletRewardReportStorage.saveRewardReport(rewardReport);

    // Then
    assertNotNull(walletRewardReportStorage.listRewards(IDENTITY_ID, ZoneId.systemDefault(), 2));
    assertNotNull(walletRewardReportStorage.findRewardReportPeriods(PAGEABLE));
    assertNotNull(walletRewardReportStorage.findRewardPeriodsBetween(12125, 222125, PAGEABLE));
    assertNotNull(walletRewardReportStorage.getRewardPeriod(RewardPeriodType.WEEK, LocalDate.now(), ZoneId.systemDefault()));
  }

    @Test
    void getRewardReport() {

        assertNull(walletRewardReportStorage.getRewardReport(RewardPeriodType.WEEK, LocalDate.now(), ZoneId.systemDefault()));

        // Given
        RewardReport rewardReport = createRewardReportInstance();

        // When
        walletRewardReportStorage.saveRewardReport(rewardReport);

        // Then
        assertNotNull(walletRewardReportStorage.getRewardReportByPeriodId(REWARD_PERIOD_ID, ZoneId.systemDefault()));
        assertNotNull(walletRewardReportStorage.getRewardPeriod(RewardPeriodType.WEEK, LocalDate.now(), ZoneId.systemDefault()));
    }

    @Test
    void findRewardPeriodsByStatus() {
        // Given
        RewardReport rewardReport = createRewardReportInstance();

        // When
        walletRewardReportStorage.saveRewardReport(rewardReport);

        // Then
        assertNotNull(walletRewardReportStorage.findRewardPeriodsByStatus(RewardStatus.SUCCESS));
    }

  protected RewardReport createRewardReportInstance() {
    Wallet wallet = newWallet(IDENTITY_ID);
    Wallet wallet4 = newWallet(4L);
    Wallet wallet5 = newWallet(5L);

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

    return rewardReport;
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
}
