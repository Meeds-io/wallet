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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
import {getRewardReportPeriods} from './js/service';

export function init() {
  extensionRegistry.registerExtension('engagementCenterAchievements', 'achievements-extensions', {
    type: 'wallet',
    computedCanUpdateStatus: {},
    canUpdateStatus(createdDate) {
      if (createdDate in this.computedCanUpdateStatus) {
        return this.computedCanUpdateStatus[createdDate];
      } else {
        this.computedCanUpdateStatus[createdDate] = getRewardReportPeriods(0, -1)
          .then(period => {
            this.computedCanUpdateStatus[createdDate] = period.filter(rewardPeriod => this.isInPeriod(rewardPeriod, createdDate)).length === 0;
            return this.computedCanUpdateStatus[createdDate];
          });
        return this.computedCanUpdateStatus[createdDate];
      }
    },
    isInPeriod(period, timestamp) {
      return timestamp >= period?.startDateInSeconds && timestamp <= period?.endDateInSeconds;
    },
    cannotUpdateStatusLabel: 'gamification.achievement.cannotUpdateStatus.tooltip',
  });
}