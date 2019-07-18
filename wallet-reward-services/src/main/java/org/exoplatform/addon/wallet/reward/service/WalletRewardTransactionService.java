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
package org.exoplatform.addon.wallet.reward.service;

import static org.exoplatform.addon.wallet.utils.RewardUtils.REWARD_CONTEXT;
import static org.exoplatform.addon.wallet.utils.RewardUtils.REWARD_SCOPE;
import static org.exoplatform.addon.wallet.utils.WalletUtils.getIdentityById;
import static org.exoplatform.addon.wallet.utils.WalletUtils.getIdentityByTypeAndId;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.WalletType;
import org.exoplatform.addon.wallet.model.reward.RewardTransaction;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * A storage service to save/load reward transactions
 */
public class WalletRewardTransactionService implements RewardTransactionService {

  private SettingService settingService;

  public WalletRewardTransactionService(SettingService settingService) {
    this.settingService = settingService;
  }

  @Override
  public List<RewardTransaction> getRewardTransactions(String periodType, long startDateInSeconds) {
    String periodTransactionsParamName = getPeriodTransactionsParamName(periodType, startDateInSeconds);
    SettingValue<?> periodTransactionsValue =
                                            settingService.get(REWARD_CONTEXT, REWARD_SCOPE, periodTransactionsParamName);
    String periodTransactionsString = periodTransactionsValue == null ? "" : periodTransactionsValue.getValue().toString();
    String[] periodTransactionsArray = periodTransactionsString.isEmpty() ? new String[0] : periodTransactionsString.split(",");
    return Arrays.stream(periodTransactionsArray).map(transaction -> {
      RewardTransaction rewardTransaction = RewardTransaction.fromStoredValue(transaction);
      Identity receiverIdentity = null;
      if (rewardTransaction.getReceiverIdentityId() > 0) {
        receiverIdentity = getIdentityById(rewardTransaction.getReceiverIdentityId());
      } else {
        receiverIdentity = getIdentityByTypeAndId(WalletType.getType(rewardTransaction.getReceiverType()),
                                                  rewardTransaction.getReceiverId());
      }
      long receiverIdentityId = receiverIdentity == null ? 0 : Long.parseLong(receiverIdentity.getId());
      rewardTransaction.setReceiverIdentityId(receiverIdentityId);
      return rewardTransaction;
    }).collect(Collectors.toList());
  }

  @Override
  public void saveRewardTransaction(RewardTransaction rewardTransaction) {
    if (rewardTransaction == null) {
      throw new IllegalArgumentException("rewardTransaction parameter is mandatory");
    }
    if (StringUtils.isBlank(rewardTransaction.getHash())) {
      throw new IllegalArgumentException("transaction hash parameter is mandatory");
    }
    if (StringUtils.isBlank(rewardTransaction.getPeriodType())) {
      throw new IllegalArgumentException("transaction PeriodType parameter is mandatory");
    }
    if (rewardTransaction.getStartDateInSeconds() == 0) {
      throw new IllegalArgumentException("transaction 'period start date' parameter is mandatory");
    }
    if (rewardTransaction.getReceiverIdentityId() == 0) {
      if (StringUtils.isBlank(rewardTransaction.getReceiverType())) {
        throw new IllegalArgumentException("transaction ReceiverType parameter is mandatory");
      }
      if (StringUtils.isBlank(rewardTransaction.getReceiverId())) {
        throw new IllegalArgumentException("transaction ReceiverId parameter is mandatory");
      }
    }

    String rewardPeriodTransactionsParamName = getPeriodTransactionsParamName(rewardTransaction.getPeriodType(),
                                                                              rewardTransaction.getStartDateInSeconds());
    SettingValue<?> periodTransactionsValue = settingService.get(REWARD_CONTEXT,
                                                                 REWARD_SCOPE,
                                                                 rewardPeriodTransactionsParamName);
    String rewardPeriodTransactionsString = periodTransactionsValue == null ? "" : periodTransactionsValue.getValue().toString();

    if (!rewardPeriodTransactionsString.contains(rewardTransaction.getHash())) {
      String contentToPrepend = rewardTransaction.getToStoreValue();
      rewardPeriodTransactionsString = rewardPeriodTransactionsString.isEmpty() ? contentToPrepend
                                                                                : contentToPrepend + ","
                                                                                    + rewardPeriodTransactionsString;
      settingService.set(REWARD_CONTEXT,
                         REWARD_SCOPE,
                         rewardPeriodTransactionsParamName,
                         SettingValue.create(rewardPeriodTransactionsString));
    }
  }

  private String getPeriodTransactionsParamName(String periodType, long startDateInSeconds) {
    return periodType + startDateInSeconds;
  }
}
