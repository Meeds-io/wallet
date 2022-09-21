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
package org.exoplatform.wallet.reward.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.rest.RequestHandler;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.ProvidersRegistry;
import org.exoplatform.services.rest.impl.RequestDispatcher;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.tools.ResourceLauncher;
import org.exoplatform.services.test.mock.MockHttpServletRequest;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardPeriodWithFullDate;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.service.RewardSettingsService;

@RunWith(MockitoJUnitRunner.class)
public class TestRewardSettingsREST {

  @Mock
  private RewardSettingsService rewardSettingsService;

  @Mock
  private ExoContainerContext   containerContext;

  @Mock
  private ExoContainer          container;

  private ResourceLauncher      launcher;

  private RewardSettingsREST    rewardSettingsREST;

  @Before
  public void setUp() {
    if (rewardSettingsREST == null) {
      rewardSettingsREST = new RewardSettingsREST(rewardSettingsService);
    }
    when(containerContext.getName()).thenReturn("portal");
    ExoContainerContext.setCurrentContainer(container);
  }

  @Test
  public void testGetNegativeTimeZone() throws Exception {
    String restPath = "/wallet/api/reward/settings/getDates?date=2022-09-20"; // NOSONAR
    String timeZone = "America/Rio_Branco";

    RewardSettings settings = new RewardSettings();
    settings.setTimeZone(timeZone);
    when(rewardSettingsService.getSettings()).thenReturn(settings);

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(restPath, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);
    ContainerResponse response = getLauncher().service("GET", restPath, "", null, null, envctx);
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    RewardPeriodWithFullDate periodWithFullDate = (RewardPeriodWithFullDate) response.getEntity();
    assertNotNull(periodWithFullDate);
    assertEquals("2022-09-01T00:00:00.000-05:00", periodWithFullDate.getStartDate());
    assertEquals(1662008400l, periodWithFullDate.getStartDateInSeconds());
    assertEquals("2022-09-30T23:59:59.000-05:00", periodWithFullDate.getEndDate());
    assertEquals(1664600400l, periodWithFullDate.getEndDateInSeconds());
    assertEquals(RewardPeriodType.DEFAULT, periodWithFullDate.getRewardPeriodType());
    assertEquals(timeZone, periodWithFullDate.getTimeZone());
  }

  @Test
  public void testGetPositiveTimeZone() throws Exception {
    String restPath = "/wallet/api/reward/settings/getDates?date=2022-09-20"; // NOSONAR
    String timeZone = "Asia/Tokyo";

    RewardSettings settings = new RewardSettings();
    settings.setTimeZone(timeZone);
    settings.setPeriodType(RewardPeriodType.WEEK);
    when(rewardSettingsService.getSettings()).thenReturn(settings);

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(restPath, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);
    ContainerResponse response = getLauncher().service("GET", restPath, "", null, null, envctx);
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    RewardPeriodWithFullDate periodWithFullDate = (RewardPeriodWithFullDate) response.getEntity();
    assertNotNull(periodWithFullDate);
    assertEquals("2022-09-19T00:00:00.000+09:00", periodWithFullDate.getStartDate());
    assertEquals(1663513200l, periodWithFullDate.getStartDateInSeconds());
    assertEquals("2022-09-25T23:59:59.000+09:00", periodWithFullDate.getEndDate());
    assertEquals(1664118000l, periodWithFullDate.getEndDateInSeconds());
    assertEquals(RewardPeriodType.WEEK, periodWithFullDate.getRewardPeriodType());
    assertEquals(timeZone, periodWithFullDate.getTimeZone());
  }

  @Test
  public void testGetDSTTimeZone() throws Exception {
    String restPath = "/wallet/api/reward/settings/getDates?date=2022-10-25"; // NOSONAR
    String timeZone = "Europe/Paris";

    RewardSettings settings = new RewardSettings();
    settings.setTimeZone(timeZone);
    settings.setPeriodType(RewardPeriodType.WEEK);
    when(rewardSettingsService.getSettings()).thenReturn(settings);

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(restPath, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);
    ContainerResponse response = getLauncher().service("GET", restPath, "", null, null, envctx);
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    RewardPeriodWithFullDate periodWithFullDate = (RewardPeriodWithFullDate) response.getEntity();
    assertNotNull(periodWithFullDate);
    assertEquals("2022-10-24T00:00:00.000+02:00", periodWithFullDate.getStartDate());
    assertEquals(1666562400l, periodWithFullDate.getStartDateInSeconds());
    assertEquals("2022-10-30T23:59:59.000+01:00", periodWithFullDate.getEndDate());
    assertEquals(1667170800l, periodWithFullDate.getEndDateInSeconds());
    assertEquals(RewardPeriodType.WEEK, periodWithFullDate.getRewardPeriodType());
    assertEquals(timeZone, periodWithFullDate.getTimeZone());
  }

  private ResourceLauncher getLauncher() throws Exception {
    if (launcher == null) {
      ResourceBinder resourceBinder = new ResourceBinder(containerContext);
      resourceBinder.addResource(rewardSettingsREST, null);
      ProvidersRegistry providers = new ProvidersRegistry();
      RequestDispatcher requestDispatcher = new RequestDispatcher(resourceBinder, providers);
      RequestHandler requestHandler = new RequestHandlerImpl(requestDispatcher, null);
      launcher = new ResourceLauncher(requestHandler);
    }
    return launcher;
  }

}
