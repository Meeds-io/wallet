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
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RewardPeriod implements Serializable {
  private static final long serialVersionUID = -4860665131754056537L;

  private RewardPeriodType  rewardPeriodType;

  private long              startDateInSeconds;

  private long              endDateInSeconds;

  public RewardPeriod(RewardPeriodType rewardPeriodType) {
    this.rewardPeriodType = rewardPeriodType;
  }

  public static RewardPeriod getCurrentPeriod(RewardSettings rewardSettings) {
    return getPeriodOfTime(rewardSettings, LocalDateTime.now());
  }

  public static RewardPeriod getPeriodOfTime(RewardSettings rewardSettings, LocalDateTime localDateTime) {
    RewardPeriodType rewardPeriodType = null;
    if (rewardSettings == null || rewardSettings.getPeriodType() == null) {
      rewardPeriodType = RewardPeriodType.DEFAULT;
    } else {
      rewardPeriodType = rewardSettings.getPeriodType();
    }
    return rewardPeriodType.getPeriodOfTime(localDateTime);
  }

  public String getStartDateFormatted(String lang) {
    return formatTime(startDateInSeconds, lang);
  }

  public String getEndDateFormatted(String lang) {
    return formatTime(endDateInSeconds, lang);
  }

}
