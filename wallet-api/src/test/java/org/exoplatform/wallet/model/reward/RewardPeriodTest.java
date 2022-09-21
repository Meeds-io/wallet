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
package org.exoplatform.wallet.model.reward;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RewardPeriodTest {

  @Test
  public void testGetCurrentPeriod() {
    ZoneId zoneId = ZoneId.of("America/Rio_Branco");

    RewardSettings rewardSettings = new RewardSettings();
    rewardSettings.setPeriodType(RewardPeriodType.MONTH);
    rewardSettings.setTimeZone(zoneId.getId());
    RewardPeriod currentPeriod = RewardPeriod.getCurrentPeriod(rewardSettings);
    assertEquals(RewardPeriodType.MONTH, currentPeriod.getRewardPeriodType());
    assertEquals(zoneId.getId(), currentPeriod.getTimeZone());
    assertEquals(LocalDate.ofInstant(Instant.ofEpochSecond((currentPeriod.getEndDateInSeconds()
        + currentPeriod.getStartDateInSeconds()) / 2), zoneId), currentPeriod.getPeriodMedianDate());
    assertEquals(YearMonth.now().atDay(1).atStartOfDay(zoneId).toEpochSecond(), currentPeriod.getStartDateInSeconds());
    assertEquals(YearMonth.now().plusMonths(1).atDay(1).atStartOfDay(zoneId).toEpochSecond(),
                 currentPeriod.getEndDateInSeconds());
  }

  @Test
  public void testGetPeriodQuarter() {
    ZoneId zoneId = ZoneId.of("America/Rio_Branco");

    RewardSettings rewardSettings = new RewardSettings();
    rewardSettings.setPeriodType(RewardPeriodType.QUARTER);
    rewardSettings.setTimeZone(zoneId.getId());

    LocalDate date = LocalDate.of(2022, Month.SEPTEMBER, 1);

    RewardPeriod period = RewardPeriod.getPeriodOfTime(rewardSettings, date);
    assertEquals(1656651600l, period.getStartDateInSeconds());
    assertEquals("1 Jul 2022", period.getStartDateFormatted("en"));
    assertEquals(1664600400l, period.getEndDateInSeconds());
    assertEquals("1 Oct 2022", period.getEndDateFormatted("en"));
    assertEquals(RewardPeriodType.QUARTER, period.getRewardPeriodType());
    assertEquals(zoneId.getId(), period.getTimeZone());
    assertEquals(LocalDate.of(2022, Month.AUGUST, 16), period.getPeriodMedianDate());
  }

  @Test
  public void testGetPeriodMonth() {
    ZoneId zoneId = ZoneId.of("America/Rio_Branco");

    RewardSettings rewardSettings = new RewardSettings();
    rewardSettings.setPeriodType(RewardPeriodType.MONTH);
    rewardSettings.setTimeZone(zoneId.getId());

    LocalDate date = LocalDate.of(2022, Month.SEPTEMBER, 1);

    RewardPeriod period = RewardPeriod.getPeriodOfTime(rewardSettings, date);
    assertEquals(1662008400l, period.getStartDateInSeconds());
    assertEquals("1 Sep 2022", period.getStartDateFormatted("en"));
    assertEquals(1664600400l, period.getEndDateInSeconds());
    assertEquals("1 Oct 2022", period.getEndDateFormatted("en"));
    assertEquals(RewardPeriodType.MONTH, period.getRewardPeriodType());
    assertEquals(zoneId.getId(), period.getTimeZone());
    assertEquals(LocalDate.of(2022, Month.SEPTEMBER, 16), period.getPeriodMedianDate());
  }

  @Test
  public void testGetPeriodYear() {
    ZoneId zoneId = ZoneId.of("America/Rio_Branco");

    RewardSettings rewardSettings = new RewardSettings();
    rewardSettings.setPeriodType(RewardPeriodType.YEAR);
    rewardSettings.setTimeZone(zoneId.getId());

    LocalDate date = LocalDate.of(2022, Month.SEPTEMBER, 1);

    RewardPeriod period = RewardPeriod.getPeriodOfTime(rewardSettings, date);
    assertEquals(1641013200l, period.getStartDateInSeconds());
    assertEquals("1 Jan 2022", period.getStartDateFormatted("en"));
    assertEquals(1672549200l, period.getEndDateInSeconds());
    assertEquals("1 Jan 2023", period.getEndDateFormatted("en"));
    assertEquals(RewardPeriodType.YEAR, period.getRewardPeriodType());
    assertEquals(zoneId.getId(), period.getTimeZone());
    assertEquals(LocalDate.of(2022, Month.JULY, 2), period.getPeriodMedianDate());
  }

}
