/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wallet.reward.notification;

import static org.exoplatform.wallet.utils.RewardUtils.*;
import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.util.ArrayList;
import java.util.Set;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;

public class RewardSuccessNotificationPlugin extends BaseNotificationPlugin {
  private static final Log LOG = ExoLogger.getLogger(RewardSuccessNotificationPlugin.class);

  public RewardSuccessNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return REWARD_SUCCESS_NOTIFICATION_ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    RewardReport rewardReport = ctx.value(REWARD_REPORT_NOTIFICATION_PARAM);

    Set<String> recipients;
    try {
      recipients = getRewardAdministrators();
    } catch (Exception e) {
      LOG.error("Error making notification of reward report " + rewardReport, e);
      return null;
    }
    if (recipients == null || recipients.isEmpty()) {
      return null;
    }

    ContractDetail contractDetail = getContractDetail();
    RewardPeriod period = rewardReport.getPeriod();

    return NotificationInfo.instance()
                           .to(new ArrayList<>(recipients))
                           .with(TOKEN_NAME, contractDetail.getName())
                           .with(SYMBOL, contractDetail.getSymbol())
                           .with(REWARD_PERIOD_TYPE, period.getRewardPeriodType().name())
                           .with(REWARD_START_PERIOD_DATE, String.valueOf(period.getStartDateInSeconds()))
                           .with(REWARD_END_PERIOD_DATE, String.valueOf(period.getEndDateInSeconds()))
                           .with(REWARD_SUCCESS_COUNT, String.valueOf(rewardReport.getSuccessTransactionCount()))
                           .with(REWARD_FAIL_COUNT, String.valueOf(rewardReport.getFailedTransactionCount()))
                           .with(REWARD_PENDING_COUNT, String.valueOf(rewardReport.getPendingTransactionCount()))
                           .with(REWARD_TRANSACTION_COUNT, String.valueOf(rewardReport.getTransactionsCount()))
                           .with(REWARD_VALID_MEMBERS_COUNT, String.valueOf(rewardReport.getValidRewardCount()))
                           .with(AMOUNT, String.valueOf(rewardReport.getTokensSent()))
                           .key(getKey())
                           .end();
  }
}
