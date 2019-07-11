package org.exoplatform.addon.wallet.reward.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.YearMonth;
import java.util.List;

import org.junit.Test;

import org.exoplatform.addon.wallet.model.reward.RewardPeriodType;
import org.exoplatform.addon.wallet.model.reward.RewardTransaction;
import org.exoplatform.addon.wallet.reward.service.WalletRewardTransactionService;
import org.exoplatform.addon.wallet.reward.test.BaseWalletRewardTest;
import org.exoplatform.addon.wallet.utils.RewardUtils;

public class WalletRewardTransactionServiceTest extends BaseWalletRewardTest {

  /**
   * Check that service is instantiated
   */
  @Test
  public void testServiceInstantiated() {
    WalletRewardTransactionService rewardTransactionService = getService(WalletRewardTransactionService.class);
    assertNotNull(rewardTransactionService);
  }

  @Test
  public void testGetTransaction() {
    WalletRewardTransactionService rewardTransactionService = getService(WalletRewardTransactionService.class);
    long startDateInSeconds = RewardUtils.timeToSeconds(YearMonth.of(2019, 01)
                                                                 .atEndOfMonth()
                                                                 .atStartOfDay());
    List<RewardTransaction> rewardTransactions = rewardTransactionService.getRewardTransactions(RewardPeriodType.MONTH.name(),
                                                                                                startDateInSeconds);
    assertNotNull(rewardTransactions);
    assertEquals(0, rewardTransactions.size());
  }

  @Test
  public void testSaveTransaction() {
    WalletRewardTransactionService rewardTransactionService = getService(WalletRewardTransactionService.class);

    long startDateInSeconds = RewardUtils.timeToSeconds(YearMonth.of(2019, 02)
                                                                 .atEndOfMonth()
                                                                 .atStartOfDay());

    String transactionHash = generateTransactionHash();
    double tokensSent = 2d;
    long receiverIdentityId = IDENTITY_ID;
    String periodType = RewardPeriodType.MONTH.name();
    String txStatus = RewardUtils.TRANSACTION_STATUS_PENDING;

    RewardTransaction rewardTransaction = new RewardTransaction();
    rewardTransaction.setHash(transactionHash);
    rewardTransaction.setPeriodType(periodType);
    rewardTransaction.setReceiverIdentityId(receiverIdentityId);
    rewardTransaction.setStartDateInSeconds(startDateInSeconds);
    rewardTransaction.setStatus(txStatus);
    rewardTransaction.setTokensSent(tokensSent);
    rewardTransactionService.saveRewardTransaction(rewardTransaction);

    List<RewardTransaction> rewardTransactions = rewardTransactionService.getRewardTransactions(periodType,
                                                                                                startDateInSeconds);
    assertNotNull(rewardTransactions);
    assertEquals(1, rewardTransactions.size());

    RewardTransaction savedRewardTransaction = rewardTransactions.get(0);
    assertEquals(rewardTransaction, savedRewardTransaction);
  }

}
