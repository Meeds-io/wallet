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

import static org.exoplatform.wallet.utils.RewardUtils.formatTime;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RewardPeriod implements Serializable {
  private static final long serialVersionUID = -4860665131754056537L;

  private RewardPeriodType  rewardPeriodType;

  private String            timeZone         = ZoneId.systemDefault().getId();

  private long              startDateInSeconds;

  private long              endDateInSeconds;

  public RewardPeriod(RewardPeriodType rewardPeriodType) {
    this.rewardPeriodType = rewardPeriodType;
  }

  public static RewardPeriod getCurrentPeriod(RewardSettings rewardSettings) {
    ZoneId zoneId = rewardSettings == null ? ZoneId.systemDefault() : rewardSettings.zoneId();
    return getPeriodOfTime(rewardSettings, ZonedDateTime.now(zoneId));
  }

  public static RewardPeriod getPeriodOfTime(RewardSettings rewardSettings, ZonedDateTime zonedDateTime) {
    ZoneId zoneId = rewardSettings == null ? ZoneId.systemDefault() : rewardSettings.zoneId();
    RewardPeriodType rewardPeriodType = null;
    if (rewardSettings == null || rewardSettings.getPeriodType() == null) {
      rewardPeriodType = RewardPeriodType.DEFAULT;
    } else {
      rewardPeriodType = rewardSettings.getPeriodType();
    }
    return rewardPeriodType.getPeriodOfTime(zonedDateTime.withZoneSameLocal(zoneId));
  }

  public ZoneId zoneId() {
    return ZoneId.of(timeZone);
  }

  public LocalDate getPeriodMedianDate() {
    return LocalDate.ofInstant(Instant.ofEpochSecond((endDateInSeconds + startDateInSeconds) / 2), zoneId());
  }

  public String getStartDateFormatted(String lang) {
    return formatTime(startDateInSeconds, lang);
  }

  public String getEndDateFormatted(String lang) {
    return formatTime(endDateInSeconds, lang);
  }

}
