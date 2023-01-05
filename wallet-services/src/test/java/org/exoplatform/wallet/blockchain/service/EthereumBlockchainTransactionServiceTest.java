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
package org.exoplatform.wallet.blockchain.service;

import static org.exoplatform.wallet.utils.WalletUtils.LAST_BLOCK_NUMBER_KEY_NAME;
import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_EFFECTIVELY_SENT_CODE;
import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import com.google.javascript.jscomp.jarjar.com.google.common.base.Objects;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.blockchain.BlockchainRequestException;
import org.exoplatform.wallet.blockchain.MaxRequestRateReachedException;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletTransactionService;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.exoplatform.wallet.utils.WalletUtils;

@RunWith(MockitoJUnitRunner.class)
public class EthereumBlockchainTransactionServiceTest extends BaseWalletTest {

  @Mock
  private WalletAccountService                 accountService;

  @Mock
  private WalletTransactionService             transactionService;

  @Mock
  private ListenerService                      listenerService;

  @Mock
  private SettingService                       settingService;

  @Mock
  private EthereumClientConnector              ethereumClientConnector;

  private EthereumBlockchainTransactionService service;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    service = new EthereumBlockchainTransactionService(container,
                                                       null,
                                                       settingService,
                                                       ethereumClientConnector,
                                                       transactionService,
                                                       accountService,
                                                       listenerService);
  }

  @Test
  public void testOnServiceStartWhenNotPermanentListeningAndNoPendingTransactions() throws Exception {
    long blockNumber = 2559l;
    when(ethereumClientConnector.getLastestBlockNumber()).thenReturn(blockNumber);
    when(transactionService.countTransactions()).thenReturn(1l);

    service.startAsync();
    verify(ethereumClientConnector, times(1)).setLastWatchedBlockNumber(anyLong());

    verify(settingService, times(1)).set(any(), any(), any(), argThat(value -> Objects.equal(value.getValue(), blockNumber)));
    verify(ethereumClientConnector, times(1)).setLastWatchedBlockNumber(blockNumber);
    verify(ethereumClientConnector, never()).renewTransactionListeningSubscription(anyLong());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testOnServiceStartWhenPermanentListening() throws Exception {
    long blockNumber = 2559l;
    SettingValue value = SettingValue.create(blockNumber);
    when(settingService.get(any(), any(), any())).thenReturn(value);
    when(ethereumClientConnector.isPermanentlyScanBlockchain()).thenReturn(true);
    when(ethereumClientConnector.getLastestBlockNumber()).thenReturn(blockNumber);
    when(transactionService.countTransactions()).thenReturn(1l);

    service.startAsync();

    verify(ethereumClientConnector, atLeast(1)).setLastWatchedBlockNumber(blockNumber);
    verify(ethereumClientConnector, times(1)).renewTransactionListeningSubscription(anyLong());
    verify(settingService, times(1)).set(any(), any(), eq(LAST_BLOCK_NUMBER_KEY_NAME + 0), any());
  }

  @Test
  public void testOnServiceStartWhenNotPermanentListeningButHavePendingTransactions() throws Exception {
    when(transactionService.countContractPendingTransactionsSent()).thenReturn(1l);

    service.startAsync();
    verify(ethereumClientConnector, times(1)).renewTransactionListeningSubscription(anyLong());
  }

  @Test
  public void testHasManagedWalletInTransactionWhenNoTopics() throws Exception {
    ContractTransactionEvent contractTransactionEvent = newContractTransactionEvent();
    contractTransactionEvent.setTopics(Collections.emptyList());
    assertFalse(service.hasManagedWalletInTransaction(contractTransactionEvent));
  }

  @Test
  public void testHasManagedWalletInTransactionWhenNoWalletsAndHavingTopics() throws Exception {
    assertFalse(service.hasManagedWalletInTransaction(newContractTransactionEvent()));
  }

  @Test
  public void testHasManagedWalletInTransactionWhenKnownSenderWallet() throws Exception {
    String fromAddress = "0x2b7e115f52171d164529fdb1ac72571e608a474e";

    when(accountService.getWalletByAddress(argThat(address -> StringUtils.equalsIgnoreCase(fromAddress,
                                                                                           address)))).thenReturn(new Wallet());

    assertTrue(service.hasManagedWalletInTransaction(newContractTransactionEvent()));
  }

  @Test
  public void testHasManagedWalletInTransactionWhenKnownReceiverWallet() throws Exception {
    String toAddress = "0x1d94f732223996e9f773261e82340889934a6c03";

    when(accountService.getWalletByAddress(argThat(address -> StringUtils.equalsIgnoreCase(toAddress,
                                                                                           address)))).thenReturn(new Wallet());

    assertTrue(service.hasManagedWalletInTransaction(newContractTransactionEvent()));
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionNotFoundOnBlockchainNorDB() throws Exception {
    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain("transactionHash"));
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnBlockchainButDB() throws Exception {
    String transactionHash = "transactionHash";
    Transaction transaction = mock(Transaction.class);
    when(ethereumClientConnector.getTransaction(transactionHash)).thenReturn(transaction);

    when(transaction.getHash()).thenReturn(transactionHash);
    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));

    when(transaction.getBlockHash()).thenReturn(WalletUtils.EMPTY_HASH);
    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));

    when(transaction.getBlockHash()).thenReturn("blockHash");
    when(transaction.getBlockNumber()).thenReturn(BigInteger.TEN);
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(ethereumClientConnector, never()).getTransactionReceipt(any());
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_NoTimeoutNoSendingAttempt() throws Exception {
    String transactionHash = "transactionHash";
    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(0);
    transactionDetail.setSendingAttemptCount(0);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_WithTimeoutNoSendingAttemptMaxReached() throws Exception {
    String transactionHash = "transactionHash";
    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis() - 86400000l - 1);
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(null);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());

    // Internal wallet transaction
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(
                                                               argThat(transaction -> !transaction.isPending()
                                                                   && !transaction.isSucceeded() && transaction.getNonce() == 0),
                                                               eq(true));
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_NoTimeoutWithSendingAttemptMaxReached() throws Exception {
    String transactionHash = "transactionHash";
    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(1);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(transactionDetail.getSendingAttemptCount());

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBButBlockchain_NoTimeoutWithSendingAttemptMaxAndInvalidNonce() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    long nonce = 25l;

    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(nonce);
    transactionDetail.setSendingAttemptCount(0);
    transactionDetail.setFrom(fromAddress);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(3l);
    when(ethereumClientConnector.getNonce(fromAddress)).thenReturn(BigInteger.valueOf(nonce));

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());

    transactionDetail.setSendingAttemptCount(transactionService.getMaxAttemptsToSend());
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());

    when(ethereumClientConnector.getNonce(fromAddress)).thenReturn(BigInteger.valueOf(nonce + 2));
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(
                                                               argThat(transaction -> !transaction.isPending()
                                                                   && !transaction.isSucceeded() && transaction.getNonce() == 0),
                                                               eq(true));
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBAndBlockchain_EtherTransaction() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    String toAddress = "toAddress";
    long nonce = 25l;
    BigInteger transactionNonce = BigInteger.valueOf(nonce);

    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    Transaction transaction = mock(Transaction.class);
    when(ethereumClientConnector.getTransaction(transactionHash)).thenReturn(transaction);
    when(transaction.getHash()).thenReturn(transactionHash);
    when(transaction.getBlockHash()).thenReturn("blockHash");
    when(transaction.getBlockNumber()).thenReturn(BigInteger.TEN);

    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(nonce);
    transactionDetail.setSendingAttemptCount(1);
    transactionDetail.setFrom(fromAddress);

    assertThrows(IllegalStateException.class, () -> service.refreshTransactionFromBlockchain(transactionHash));
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());

    TransactionReceipt transactionReceipt = mock(TransactionReceipt.class);
    when(ethereumClientConnector.getTransactionReceipt(transactionHash)).thenReturn(transactionReceipt);
    BigInteger gasUsed = BigInteger.ONE;
    BigInteger gasPrice = BigInteger.TWO;
    BigInteger etherValue = BigInteger.valueOf(4);

    when(transactionReceipt.getGasUsed()).thenReturn(gasUsed);
    when(transaction.getGasPrice()).thenReturn(gasPrice);
    when(transaction.getNonce()).thenReturn(transactionNonce);
    when(transaction.getValue()).thenReturn(etherValue);
    when(transaction.getFrom()).thenReturn(fromAddress);
    when(transaction.getTo()).thenReturn(toAddress);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(argThat(transactionTmp -> {
      assertFalse(transactionTmp.isPending());
      assertFalse(transactionTmp.isSucceeded());
      assertTrue(StringUtils.isBlank(transactionTmp.getContractAddress()));
      assertTrue(StringUtils.isBlank(transactionTmp.getContractMethodName()));
      assertEquals(0, transactionTmp.getContractAmount(), 0);
      assertEquals(gasPrice.doubleValue(), transactionTmp.getGasPrice(), 0);
      assertEquals(gasUsed.intValue(), transactionTmp.getGasUsed());
      assertEquals(transactionNonce.longValue(), transactionTmp.getNonce());
      assertEquals(toAddress, transactionTmp.getTo());
      assertEquals(fromAddress, transactionTmp.getFrom());
      assertEquals(WalletUtils.convertFromDecimals(etherValue, WalletUtils.ETHER_TO_WEI_DECIMALS), transactionTmp.getValue(), 0);
      return true;
    }), eq(true));

    verify(transactionService, never()).cancelTransactionsWithSameNonce(any());
    when(transactionReceipt.isStatusOK()).thenReturn(true);
    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
  }

  @Test
  public void testRefreshTransactionFromBlockchainWhenTransactionFoundOnDBAndBlockchain_ContractTransaction() throws Exception {
    ContractTransactionEvent contractTransactionEvent = newContractTransactionEvent();
    String transactionHash = contractTransactionEvent.getTransactionHash();
    String fromAddress = "0x2b7e115f52171d164529fdb1ac72571e608a474e";
    long nonce = 25l;
    BigInteger transactionNonce = BigInteger.valueOf(nonce);
    String contractAddress = WalletUtils.getContractAddress();

    TransactionDetail transactionDetail = new TransactionDetail();
    when(transactionService.getTransactionByHash(transactionHash)).thenReturn(transactionDetail);

    Transaction transaction = mock(Transaction.class);
    when(ethereumClientConnector.getTransaction(transactionHash)).thenReturn(transaction);

    TransactionReceipt transactionReceipt = mock(TransactionReceipt.class);
    when(ethereumClientConnector.getTransactionReceipt(transactionHash)).thenReturn(transactionReceipt);

    org.web3j.protocol.core.methods.response.Log log = mock(org.web3j.protocol.core.methods.response.Log.class);
    when(log.getTopics()).thenReturn(contractTransactionEvent.getTopics());
    when(log.getData()).thenReturn(contractTransactionEvent.getData());

    BigInteger gasUsed = BigInteger.ONE;
    BigInteger gasPrice = BigInteger.TWO;
    BigInteger etherValue = BigInteger.valueOf(4);

    transactionDetail.setHash(transactionHash);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setSentTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(nonce);
    transactionDetail.setSendingAttemptCount(1);
    transactionDetail.setFrom(fromAddress);

    when(transactionReceipt.getGasUsed()).thenReturn(gasUsed);
    when(transactionReceipt.isStatusOK()).thenReturn(true);
    when(transactionReceipt.getLogs()).thenReturn(Collections.singletonList(log));
    when(transaction.getGasPrice()).thenReturn(gasPrice);
    when(transaction.getNonce()).thenReturn(transactionNonce);
    when(transaction.getValue()).thenReturn(etherValue);
    when(transaction.getFrom()).thenReturn(fromAddress);
    when(transaction.getTo()).thenReturn(contractAddress);
    when(transaction.getHash()).thenReturn(transactionHash);
    when(transaction.getBlockHash()).thenReturn("blockHash");
    when(transaction.getBlockNumber()).thenReturn(BigInteger.TEN);

    service.refreshTransactionFromBlockchain(transactionHash);
    verify(transactionService, times(1)).saveTransactionDetail(argThat(transactionTmp -> {
      assertFalse(transactionTmp.isPending());
      assertTrue(transactionTmp.isSucceeded());
      assertEquals(gasPrice.doubleValue(), transactionTmp.getGasPrice(), 0);
      assertEquals(gasUsed.intValue(), transactionTmp.getGasUsed());
      assertEquals(transactionNonce.longValue(), transactionTmp.getNonce());
      assertEquals(fromAddress, transactionTmp.getFrom());
      assertEquals(WalletUtils.convertFromDecimals(etherValue, WalletUtils.ETHER_TO_WEI_DECIMALS), transactionTmp.getValue(), 0);
      assertEquals(MeedsToken.FUNC_TRANSFER, transactionTmp.getContractMethodName());
      assertTrue(transactionTmp.getContractAmount() > 0);
      assertEquals(contractAddress, transactionTmp.getContractAddress());
      return true;
    }), eq(true));

    verify(transactionService, times(1)).cancelTransactionsWithSameNonce(transactionDetail);
  }

  @Test
  public void testSendPendingTransactionsToBlockchain_NoTransactions() throws Exception {
    service.sendPendingTransactionsToBlockchain();

    verify(ethereumClientConnector, never()).sendTransactionToBlockchain(any());
  }

  @Test
  public void testSendPendingTransactionsToBlockchain_TimedOutPendingTransaction() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis() - 86400000l - 1);
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);

    List<TransactionDetail> sentTransactions = service.sendPendingTransactionsToBlockchain();
    assertNotNull(sentTransactions);
    assertEquals(0, sentTransactions.size());

    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
    verify(ethereumClientConnector, never()).sendTransactionToBlockchain(any());

    transactionDetail.setTimestamp(System.currentTimeMillis());

    service.sendPendingTransactionsToBlockchain();

    verify(ethereumClientConnector, times(1)).sendTransactionToBlockchain(any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_MaxAttemptsToSendReachedButNotSentToBlockchain() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);

    TransactionDetail originalTransactionDetail = transactionDetail.clone();

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);
    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });
    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(mock(EthSendTransaction.class), new IOException());
    });
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);

    assertThrows(BlockchainRequestException.class, () -> service.sendPendingTransactionsToBlockchain());

    verify(ethereumClientConnector, times(1)).sendTransactionToBlockchain(any());
    verify(listenerService, never()).broadcast(anyString(), any(), any());
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
    assertEquals(originalTransactionDetail, transactionDetail);
  }

  @Test
  public void testSendPendingTransactionsToBlockchain_MaxAttemptsToSendReachedAndAlreadySentToBlockchain() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(1l);
    transactionDetail.setSentTimestamp(System.currentTimeMillis());

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    service.sendPendingTransactionsToBlockchain();

    verify(ethereumClientConnector, never()).sendTransactionToBlockchain(any());
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
  }

  @Test
  public void testSendPendingTransactionsToBlockchain_IsNotBoostAndIsMaxParallelRequestsReached() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    TransactionDetail originalTransactionDetail = transactionDetail.clone();

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    // Keep it on purpose to always check verifications are 0 even when this
    // condition is true
    lenient().when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(false);

    service.sendPendingTransactionsToBlockchain();

    verify(ethereumClientConnector, never()).sendTransactionToBlockchain(any());
    verify(listenerService, never()).broadcast(any(), any(), any());
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
    assertEquals(originalTransactionDetail, transactionDetail);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_IsBoostAndIsMaxParallelRequestsReached() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setBoost(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    TransactionDetail originalTransactionDetail = transactionDetail.clone();

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    // Keep it on purpose to always check verifications are 0 even when this
    // condition is true
    lenient().when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);

    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);
    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });
    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(mock(EthSendTransaction.class), new IOException());
    });

    assertThrows(BlockchainRequestException.class, () -> service.sendPendingTransactionsToBlockchain());

    verify(ethereumClientConnector, times(1)).sendTransactionToBlockchain(any());
    verify(listenerService, never()).broadcast(anyString(), any(), any());
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
    assertEquals(originalTransactionDetail, transactionDetail);
  }

  @Test
  public void testSendPendingTransactionsToBlockchain_IsSendingBlockchainRequestFutureNull() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    TransactionDetail originalTransactionDetail = transactionDetail.clone();

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    when(ethereumClientConnector.sendTransactionToBlockchain(transactionDetail)).thenReturn(null);

    service.sendPendingTransactionsToBlockchain();

    verify(ethereumClientConnector, times(1)).sendTransactionToBlockchain(any());
    verify(listenerService, never()).broadcast(anyString(), any(), any());
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
    assertEquals(originalTransactionDetail, transactionDetail);
  }

  @Test
  public void testSendPendingTransactionsToBlockchain_CancelModificationsWhenSendingException() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setNonce(25l);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    when(ethereumClientConnector.sendTransactionToBlockchain(transactionDetail)).thenThrow(new IOException());

    assertThrows(BlockchainRequestException.class, () -> service.sendPendingTransactionsToBlockchain());

    verify(ethereumClientConnector, times(1)).sendTransactionToBlockchain(any());
    verify(listenerService, never()).broadcast(anyString(), any(), any());
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_ResentWithAttemptIncrementWhenIOExceptionFromWeb3() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);

    TransactionDetail transactionDetail2 = transactionDetail.clone();
    transactionDetail2.setHash(transactionHash + "2");
    when(transactionService.getTransactionsToSend()).thenReturn(Arrays.asList(transactionDetail, transactionDetail2));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });

    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(mock(EthSendTransaction.class), new IOException());
    });

    assertThrows(BlockchainRequestException.class, () -> service.sendPendingTransactionsToBlockchain());

    verify(listenerService, never()).broadcast(anyString(), any(), any());
    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_ResentWithAttemptIncrementWhenSentTransactionIsNull() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });

    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(null, null);
    });

    List<TransactionDetail> pendingTransactions = service.sendPendingTransactionsToBlockchain();
    assertNotNull(pendingTransactions);
    assertEquals(0, pendingTransactions.size());

    verify(transactionService, times(1)).saveTransactionDetail(argThat(transaction -> transaction.isPending()
        && !transaction.isSucceeded() && transaction.getNonce() == NONCE), eq(false));
    verify(listenerService, times(1)).broadcast(eq(TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT), any(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_ResentWithAttemptIncrementWhenSentTransactionHasDiffrentHash() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });

    service.sendPendingTransactionsToBlockchain();
    EthSendTransaction ethTransaction = mock(EthSendTransaction.class);
    String newHash = "transactionHash2";
    when(ethTransaction.getTransactionHash()).thenReturn(newHash);
    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(ethTransaction, null);
    });

    List<TransactionDetail> pendingTransactions = service.sendPendingTransactionsToBlockchain();
    assertNotNull(pendingTransactions);
    assertEquals(1, pendingTransactions.size());
    TransactionDetail handledTransactionDetail = pendingTransactions.get(0);
    assertNotNull(handledTransactionDetail);
    assertTrue(handledTransactionDetail.getSentTimestamp() > 0);
    assertEquals(TRANSACTION_EFFECTIVELY_SENT_CODE, handledTransactionDetail.getSendingAttemptCount());
    assertEquals(newHash, handledTransactionDetail.getHash());

    verify(transactionService, times(1)).saveTransactionDetail(argThat(transaction -> transaction.isPending()
        && !transaction.isSucceeded() && transaction.getNonce() == NONCE), eq(false));
    verify(listenerService, times(1)).broadcast(TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT,
                                                handledTransactionDetail,
                                                handledTransactionDetail);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_ResentWithAttemptIncrementWhenSentTransactionHasError() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });

    service.sendPendingTransactionsToBlockchain();
    EthSendTransaction ethTransaction = mock(EthSendTransaction.class);
    Error transactionError = mock(org.web3j.protocol.core.Response.Error.class);
    when(ethTransaction.getError()).thenReturn(transactionError);

    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(ethTransaction, null);
    });

    List<TransactionDetail> pendingTransactions = service.sendPendingTransactionsToBlockchain();
    assertNotNull(pendingTransactions);
    assertEquals(0, pendingTransactions.size());

    verify(transactionService, times(1)).saveTransactionDetail(argThat(transaction -> transaction.isPending()
        && !transaction.isSucceeded() && transaction.getNonce() == NONCE), eq(false));
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_RefreshWhenSentTransactionHasNonceTooLowErrorAndReceipt() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });

    EthSendTransaction ethTransaction = mock(EthSendTransaction.class);
    Error transactionError = mock(org.web3j.protocol.core.Response.Error.class);
    when(ethTransaction.getError()).thenReturn(transactionError);
    TransactionReceipt receipt = mock(TransactionReceipt.class);
    when(ethereumClientConnector.getTransactionReceipt(transactionHash)).thenReturn(receipt);
    when(transactionError.getMessage()).thenReturn("Error: nonce too low");

    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(ethTransaction, null);
    });

    List<TransactionDetail> pendingTransactions = service.sendPendingTransactionsToBlockchain();
    assertNotNull(pendingTransactions);
    assertEquals(0, pendingTransactions.size());

    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_StopSendingWhenRequestRateLimitReached() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });

    EthSendTransaction ethTransaction = mock(EthSendTransaction.class);
    Error transactionError = mock(org.web3j.protocol.core.Response.Error.class);
    when(ethTransaction.getError()).thenReturn(transactionError);
    when(transactionError.getCode()).thenReturn(429);

    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(ethTransaction, null);
    });

    assertThrows(MaxRequestRateReachedException.class, () -> service.sendPendingTransactionsToBlockchain());

    verify(transactionService, never()).saveTransactionDetail(any(), anyBoolean());
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_ResentWithAttemptIncrementWhenSentTransactionHasUnrecoverableError() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });
    EthSendTransaction ethTransaction = mock(EthSendTransaction.class);
    Error transactionError = mock(org.web3j.protocol.core.Response.Error.class);
    when(ethTransaction.getError()).thenReturn(transactionError);
    when(transactionError.getMessage()).thenReturn("Error: insufficient funds");
    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(ethTransaction, null);
    });

    List<TransactionDetail> pendingTransactions = service.sendPendingTransactionsToBlockchain();
    assertNotNull(pendingTransactions);
    assertEquals(0, pendingTransactions.size());

    verify(transactionService, times(1)).saveTransactionDetail(
                                                               argThat(transaction -> !transaction.isPending()
                                                                   && !transaction.isSucceeded() && transaction.getNonce() == 0),
                                                               eq(true));
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSendPendingTransactionsToBlockchain_AvoidBroadcastingTransactionWhenAlreadySentToBlockchain() throws Exception {
    String transactionHash = "transactionHash";
    String fromAddress = "fromAddress";
    TransactionDetail transactionDetail = new TransactionDetail();

    transactionDetail.setHash(transactionHash);
    transactionDetail.setFrom(fromAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setPending(true);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setRawTransaction(RAW_TRANSACTION);
    transactionDetail.setSendingAttemptCount(0);
    when(transactionService.getTransactionsToSend()).thenReturn(Collections.singletonList(transactionDetail));

    when(transactionService.getPendingTransactionMaxDays()).thenReturn(1l);
    when(transactionService.getMaxAttemptsToSend()).thenReturn(1l);
    when(transactionService.canSendTransactionToBlockchain(fromAddress)).thenReturn(true);
    CompletableFuture<EthSendTransaction> future = mock(CompletableFuture.class);
    CompletableFuture<TransactionDetail> resultFuture = mock(CompletableFuture.class);

    AtomicReference<BiFunction<EthSendTransaction, Throwable, TransactionDetail>> handler = new AtomicReference<>();
    when(ethereumClientConnector.sendTransactionToBlockchain(any())).thenReturn(future);
    when(future.handle(any())).thenAnswer(invocation -> {
      handler.set(invocation.getArgument(0, BiFunction.class));
      return resultFuture;
    });
    EthSendTransaction ethTransaction = mock(EthSendTransaction.class);
    Error transactionError = mock(org.web3j.protocol.core.Response.Error.class);
    when(ethTransaction.getError()).thenReturn(transactionError);
    when(transactionError.getMessage()).thenReturn("Error: Already known");
    when(resultFuture.get()).thenAnswer(invocation -> {
      return handler.get().apply(ethTransaction, null);
    });

    List<TransactionDetail> pendingTransactions = service.sendPendingTransactionsToBlockchain();
    assertNotNull(pendingTransactions);
    assertEquals(1, pendingTransactions.size());
    TransactionDetail handledTransactionDetail = pendingTransactions.get(0);
    assertNotNull(handledTransactionDetail);
    assertTrue(handledTransactionDetail.getSentTimestamp() > 0);
    assertEquals(TRANSACTION_EFFECTIVELY_SENT_CODE, handledTransactionDetail.getSendingAttemptCount());
    assertTrue(handledTransactionDetail.isPending());
    assertFalse(handledTransactionDetail.isSucceeded());
    assertEquals(NONCE, handledTransactionDetail.getNonce());

    verify(transactionService, times(1)).saveTransactionDetail(argThat(transaction -> transaction.isPending()
        && !transaction.isSucceeded() && transaction.getNonce() == NONCE), eq(false));
    verify(listenerService, times(1)).broadcast(TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT,
                                                handledTransactionDetail,
                                                handledTransactionDetail);
  }

}
