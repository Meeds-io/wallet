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
package io.meeds.wallet.wallet.model.reward;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.meeds.wallet.wallet.utils.RewardUtils;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Exclude;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class RewardPeriodWithFullDate extends RewardPeriod {

  private static final long serialVersionUID = -7244890529668520077L;

  public RewardPeriodWithFullDate(RewardPeriod rewardPeriod) {
    ZoneId zoneId = rewardPeriod.zoneId();
    ZonedDateTime startZonedDateTime =
                                     ZonedDateTime.ofInstant(Instant.ofEpochSecond(rewardPeriod.getStartDateInSeconds()), zoneId);
    ZonedDateTime endZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(rewardPeriod.getEndDateInSeconds() - 1), zoneId);
    this.startDate = RewardUtils.formatDateTime(startZonedDateTime);
    this.endDate = RewardUtils.formatDateTime(endZonedDateTime);
    this.setId(rewardPeriod.getId());
    this.setRewardPeriodType(rewardPeriod.getRewardPeriodType());
    this.setStartDateInSeconds(rewardPeriod.getStartDateInSeconds());
    this.setEndDateInSeconds(rewardPeriod.getEndDateInSeconds());
    this.setTimeZone(rewardPeriod.getTimeZone());
  }

  @Exclude
  private String startDate;

  @Exclude
  private String endDate;

}
