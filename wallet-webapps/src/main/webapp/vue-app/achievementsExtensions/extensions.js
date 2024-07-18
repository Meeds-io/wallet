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
    rewardReportPeriods: null,
    init(from, to) {
      this.rewardReportPeriods = getRewardReportPeriods(from, to, 0, -1);
    },
    canUpdateStatus(createdDate, updateStatusExtension) {
      if (typeof this.computedCanUpdateStatus !== 'undefined' && createdDate in this.computedCanUpdateStatus) {
        return this.computedCanUpdateStatus[createdDate];
      } else {
        this.computedCanUpdateStatus = {};
        if (updateStatusExtension !== null && typeof updateStatusExtension !== 'undefined') {
          this.rewardReportPeriods = updateStatusExtension?.rewardReportPeriods;
        }
        this.rewardReportPeriods = (this.rewardReportPeriods === null || typeof this.rewardReportPeriods === 'undefined') ? getRewardReportPeriods(null, null, 0, -1) : this.rewardReportPeriods;
        this.computedCanUpdateStatus[createdDate] = this.rewardReportPeriods
          .then(period => {
            this.computedCanUpdateStatus[createdDate] = period.filter(rewardPeriod => createdDate >= rewardPeriod?.startDateInSeconds && createdDate <= rewardPeriod?.endDateInSeconds).length === 0;
            return this.computedCanUpdateStatus[createdDate];
          });
        return this.computedCanUpdateStatus[createdDate];
      }
    },
    cannotUpdateStatusLabel: 'gamification.achievement.cannotUpdateStatus.tooltip',
  });
}