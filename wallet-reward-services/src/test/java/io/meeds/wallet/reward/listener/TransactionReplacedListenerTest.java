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
package io.meeds.wallet.reward.listener;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.wallet.reward.service.WalletRewardReportService;

import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TransactionReplacedListener.class})
class TransactionReplacedListenerTest {

  private static final String EVENT_NAME = "exo.wallet.transaction.replaced";

  @MockBean
  private ListenerService listenerService;

  @MockBean
  private WalletRewardReportService walletRewardReportService;

  @MockBean
  private Event<Object, Map<String, String>> event;

  @Autowired
  private TransactionReplacedListener transactionReplacedListener;

  @BeforeEach
  void setUp() {
    transactionReplacedListener.init();
  }

  @Test
  void testInit() {
    verify(listenerService).addListener(EVENT_NAME, transactionReplacedListener);
  }

  @Test
  void testOnEvent() {
    Map<String, String> transactionData = new HashMap<>();
    transactionData.put("oldHash", "oldHashValue");
    transactionData.put("hash", "newHashValue");

    when(event.getData()).thenReturn(transactionData);

    // When
    transactionReplacedListener.onEvent(event);

    // Then
    verify(walletRewardReportService).replaceRewardTransactions("oldHashValue", "newHashValue");
  }
}


