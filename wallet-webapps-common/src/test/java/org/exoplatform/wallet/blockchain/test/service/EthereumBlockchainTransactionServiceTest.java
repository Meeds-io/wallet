/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.wallet.blockchain.test.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.wallet.blockchain.service.EthereumBlockchainTransactionService;
import org.exoplatform.wallet.blockchain.service.EthereumClientConnector;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;

public class EthereumBlockchainTransactionServiceTest {

  private static final long        ONE_DAY_IN_MS                = 864000000L;

  private static final long        MAX_TRANSACTION_PENDING_DAYS = 1L;

  private static final long        MAX_SENDING_ATTEMPTS         = 3L;

  private static final String      RAW_TX                       = "RAW_TX";

  private static final String      TO_ADDRESS                   = "TO_ADDRESS";

  private static final String      FROM_ADDRESS                 = "FROM_ADDRESS";

  private static final String      TX_HASH                      = "txHash";

  private SettingService           settingService;                               // NOSONAR

  private EthereumClientConnector  ethereumClientConnector;                      // NOSONAR

  private WalletTransactionService walletTransactionService;

  private WalletAccountService     walletAccountService;                         // NOSONAR

  @Test
  public void testCheckPendingTransactionsWhenNoPendingTransactions() {
    EthereumBlockchainTransactionService blockchainTransactionService = newServiceInstance();

    // Check no exception is thrown when no pending transaction to send
    blockchainTransactionService.checkPendingTransactions();
  }

  @Test
  public void testCheckPendingTransactionsWhenOnePendingTransaction() {
    EthereumBlockchainTransactionService blockchainTransactionService = newServiceInstance();

    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setHash(TX_HASH);
    transactionDetail.setFrom(FROM_ADDRESS);
    transactionDetail.setTo(TO_ADDRESS);
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TX);
    int sendingAttemptCount = 1;
    transactionDetail.setSendingAttemptCount(sendingAttemptCount);
    long sentTimestamp = System.currentTimeMillis();
    transactionDetail.setSentTimestamp(sentTimestamp);

    Mockito.when(walletTransactionService.getPendingTransactions()).thenReturn(Collections.singletonList(transactionDetail));
    Mockito.when(walletTransactionService.getTransactionByHash(Matchers.eq(TX_HASH))).thenReturn(transactionDetail);

    // Check pending transaction not sent after MEX_PENDING_DAYS and verify that
    // it will be sent again
    blockchainTransactionService.checkPendingTransactions();

    // Verify that transaction hasn't been modified because the transaction was
    // sent less than MAX_TRANSACTION_PENDING_DAYS ago
    Mockito.verify(walletTransactionService, Mockito.times(0)).saveTransactionDetail(Matchers.any(), Matchers.anyBoolean());

    assertEquals("SendingTimetamp shouldn't be modified", sentTimestamp, transactionDetail.getSentTimestamp());
    assertEquals("SendingAttemptCount shouldn't be incremented", sendingAttemptCount, transactionDetail.getSendingAttemptCount());

    // Change time of transaction sending so that the transaction is resent
    // again
    sentTimestamp = sentTimestamp - ONE_DAY_IN_MS * MAX_TRANSACTION_PENDING_DAYS - 1;
    transactionDetail.setSentTimestamp(sentTimestamp);

    // Check pending transaction not sent after MEX_PENDING_DAYS and verify that
    // it will be sent again
    blockchainTransactionService.checkPendingTransactions();

    // Verify that transaction has been saved with no sentTimestamp and
    // increased SendingAttemptCount
    Mockito.verify(walletTransactionService, Mockito.times(sendingAttemptCount))
           .saveTransactionDetail(Matchers.eq(transactionDetail), Matchers.eq(false));

    assertEquals("SendingTimetamp should be reset to resend transaction to blockchain", 0, transactionDetail.getSentTimestamp());
    assertEquals("SendingAttemptCount is not incremented", sendingAttemptCount + 1L, transactionDetail.getSendingAttemptCount());
  }

  private EthereumBlockchainTransactionService newServiceInstance() {
    settingService = Mockito.mock(SettingService.class);
    ethereumClientConnector = Mockito.mock(EthereumClientConnector.class);
    walletTransactionService = Mockito.mock(WalletTransactionService.class);
    walletAccountService = Mockito.mock(WalletAccountService.class);

    Mockito.when(walletTransactionService.getMaxAttemptsToSend()).thenReturn(MAX_SENDING_ATTEMPTS);
    Mockito.when(walletTransactionService.getPendingTransactionMaxDays()).thenReturn(MAX_TRANSACTION_PENDING_DAYS);

    return new EthereumBlockchainTransactionService(settingService,
                                                    ethereumClientConnector,
                                                    walletTransactionService,
                                                    walletAccountService);
  }

}
