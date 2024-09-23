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
package io.meeds.wallet.wallet.model.reward;

import static io.meeds.wallet.wallet.utils.RewardUtils.timeToSecondsAtDayStart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum RewardPeriodType {
  WEEK, MONTH, QUARTER;

  public static final RewardPeriodType DEFAULT = WEEK;

  public RewardPeriod getPeriodOfTime(LocalDate date, ZoneId zoneId) {
    ZonedDateTime zonedDateTime = date.atStartOfDay(zoneId);
    return getPeriodOfTime(zonedDateTime);
  }

  public RewardPeriod getPeriodOfTime(ZonedDateTime zonedDateTime) {
    ZoneId zoneId = zonedDateTime.getZone();

    RewardPeriod rewardPeriod = new RewardPeriod(this);
    rewardPeriod.setTimeZone(zoneId.getId());
    YearMonth yearMonth = YearMonth.from(zonedDateTime);
    switch (this) {
    case WEEK:
      ZonedDateTime firstDayOfThisWeek = zonedDateTime.with(DayOfWeek.MONDAY);
      ZonedDateTime firstDayOfNextWeek = firstDayOfThisWeek.plusWeeks(1);
      rewardPeriod.setStartDateInSeconds(timeToSecondsAtDayStart(firstDayOfThisWeek.toLocalDate(), zoneId));
      rewardPeriod.setEndDateInSeconds(timeToSecondsAtDayStart(firstDayOfNextWeek.toLocalDate(), zoneId));
      break;
    case MONTH:
      YearMonth nextMonth = yearMonth.plusMonths(1);
      rewardPeriod.setStartDateInSeconds(timeToSecondsAtDayStart(yearMonth.atDay(1), zoneId));
      rewardPeriod.setEndDateInSeconds(timeToSecondsAtDayStart(nextMonth.atDay(1), zoneId));
      break;
    case QUARTER:
      int monthQuarterIndex = ((yearMonth.getMonthValue() - 1) / 3) * 3 + 1;

      YearMonth startQuarterMonth = YearMonth.of(yearMonth.getYear(), monthQuarterIndex);
      YearMonth endQuarterMonth = startQuarterMonth.plusMonths(3);
      rewardPeriod.setStartDateInSeconds(timeToSecondsAtDayStart(startQuarterMonth.atDay(1), zoneId));
      rewardPeriod.setEndDateInSeconds(timeToSecondsAtDayStart(endQuarterMonth.atDay(1), zoneId));
      break;
    }
    return rewardPeriod;
  }
}
