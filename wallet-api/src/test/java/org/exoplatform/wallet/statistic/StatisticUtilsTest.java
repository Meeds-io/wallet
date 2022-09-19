/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
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
package org.exoplatform.wallet.statistic;

import static org.exoplatform.wallet.statistic.StatisticUtils.DURATION;
import static org.exoplatform.wallet.statistic.StatisticUtils.ERROR_MSG;
import static org.exoplatform.wallet.statistic.StatisticUtils.LOCAL_SERVICE;
import static org.exoplatform.wallet.statistic.StatisticUtils.OPERATION;
import static org.exoplatform.wallet.statistic.StatisticUtils.REMOTE_SERVICE;
import static org.exoplatform.wallet.statistic.StatisticUtils.STATUS;
import static org.exoplatform.wallet.statistic.StatisticUtils.STATUS_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.analytics.model.StatisticData.StatisticStatus;
import org.exoplatform.analytics.utils.AnalyticsUtils;
import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;

@RunWith(MockitoJUnitRunner.class)
public class StatisticUtilsTest {

  @Mock
  ExoFeatureService featureService;

  @Test
  public void testParameterValidation() {
    assertThrows(IllegalArgumentException.class, () -> StatisticUtils.addStatisticEntry(null));
    assertThrows(IllegalArgumentException.class, () -> StatisticUtils.addStatisticEntry(Collections.emptyMap())); // NOSONAR
  }

  @Test
  public void testIsAnalyticsFeatureEnabled() {
    StatisticUtils.featureService = featureService;
    when(featureService.isActiveFeature(StatisticUtils.WALLET_ANALYTICS_FEATURE_NAME)).thenReturn(true);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(REMOTE_SERVICE, "REMOTE_SERVICE");
    parameters.put(OPERATION, "OPERATION");
    parameters.put(STATUS, "KO");
    parameters.put(STATUS_CODE, "500");
    parameters.put(ERROR_MSG, "ERROR_MSG");
    parameters.put(DURATION, 1220l);

    Wallet toWallet = addWallet(parameters, "to", 1l, "toWallet");
    Wallet fromWallet = addWallet(parameters, "fromWallet", 2l, "fromWallet");
    Wallet byWallet = addWallet(parameters, "byWallet", 1l, "byWallet");
    Wallet wallet = addWallet(parameters, "wallet", 1l, "wallet");

    AtomicInteger logConsumerInvocationCount = new AtomicInteger();
    AtomicReference<StatisticData> statisticDataReference = new AtomicReference<>();
    StatisticUtils.addStatisticEntry(new HashMap<>(parameters),
                                     logEntry -> logConsumerInvocationCount.incrementAndGet(),
                                     statisticData -> statisticDataReference.set(statisticData));
    assertEquals(0, logConsumerInvocationCount.get());

    StatisticData statisticData = statisticDataReference.get();
    assertNotNull(statisticData);
    assertEquals("wallet", statisticData.getModule());
    assertEquals(parameters.get(REMOTE_SERVICE), statisticData.getSubModule());
    assertEquals(parameters.get(OPERATION), statisticData.getOperation());
    assertEquals(parameters.get(ERROR_MSG), statisticData.getErrorMessage());
    assertEquals(Long.parseLong(parameters.get(DURATION).toString()), statisticData.getDuration());
    assertEquals(StatisticStatus.valueOf(parameters.get(STATUS).toString().toUpperCase()), statisticData.getStatus());
    assertEquals(Long.parseLong(parameters.get(STATUS_CODE).toString()), statisticData.getErrorCode());
    assertEquals(0, statisticData.getUserId());
    assertEquals(String.valueOf(toWallet.getTechnicalId()), statisticData.getParameters().get("toIdentityId"));
    assertEquals(toWallet.getAddress(), statisticData.getParameters().get("toWalletAddress"));
    assertEquals(String.valueOf(fromWallet.getTechnicalId()), statisticData.getParameters().get("fromIdentityId"));
    assertEquals(fromWallet.getAddress(), statisticData.getParameters().get("fromWalletAddress"));
    assertEquals(String.valueOf(byWallet.getTechnicalId()), statisticData.getParameters().get("byIdentityId"));
    assertEquals(byWallet.getAddress(), statisticData.getParameters().get("byWalletAddress"));
    assertEquals(String.valueOf(wallet.getTechnicalId()),
                 statisticData.getParameters().get(AnalyticsUtils.FIELD_SOCIAL_IDENTITY_ID));
    assertEquals(wallet.getAddress(), statisticData.getParameters().get("walletAddress"));
  }

  @Test
  public void testIsAnalyticsFeatureDisabled() {
    StatisticUtils.featureService = featureService;
    when(featureService.isActiveFeature(StatisticUtils.WALLET_ANALYTICS_FEATURE_NAME)).thenReturn(false);
    Log logger = mock(Log.class);
    StatisticUtils.log = logger;
    when(logger.isDebugEnabled()).thenReturn(true);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(LOCAL_SERVICE, "REMOTE_SERVICE");
    parameters.put(OPERATION, "OPERATION");
    parameters.put(STATUS, "KO");
    parameters.put(STATUS_CODE, "500");
    parameters.put(ERROR_MSG, "ERROR_MSG");
    parameters.put(DURATION, "1220");

    addWallet(parameters, "to", 1l, "toWallet");
    addWallet(parameters, "fromWallet", 2l, "fromWallet");
    addWallet(parameters, "byWallet", 1l, "byWallet");
    addWallet(parameters, "wallet", 1l, "wallet");

    AtomicInteger logConsumerInvocationCount = new AtomicInteger();
    AtomicInteger analyticsConsumerInvocationCount = new AtomicInteger();
    StatisticUtils.addStatisticEntry(new HashMap<>(parameters),
                                     logEntry -> logConsumerInvocationCount.incrementAndGet(),
                                     statisticData -> analyticsConsumerInvocationCount.incrementAndGet());
    assertEquals(1, logConsumerInvocationCount.get());
    assertEquals(0, analyticsConsumerInvocationCount.get());
  }

  private Wallet addWallet(Map<String, Object> parameters, String key, long technicalId, String address) {
    Wallet toWallet = new Wallet();
    toWallet.setTechnicalId(technicalId);
    toWallet.setAddress(address);
    parameters.put(key, toWallet);
    return toWallet;
  }

}
