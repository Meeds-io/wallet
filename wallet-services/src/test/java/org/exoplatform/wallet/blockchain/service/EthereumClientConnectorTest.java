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

import static org.exoplatform.wallet.utils.WalletUtils.CONTRACT_TRANSACTION_MINED_EVENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.concurrent.ConcurrentFIFOExoCache;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.test.BaseWalletTest;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

@RunWith(MockitoJUnitRunner.class)
public class EthereumClientConnectorTest extends BaseWalletTest {

  @Mock
  private Web3j                   web3j;

  @Mock
  private CacheService            cacheService;

  @Mock
  private WebSocketClient         webSocketClient;

  @Mock
  private WebSocketService        web3jService;

  @Mock
  private ListenerService         listenerService;

  private boolean                 webSocketConnected;

  private boolean                 subscriptionDisposed;

  private EthereumClientConnector service;

  @Before
  public void setUp() throws InterruptedException {
    when(cacheService.getCacheInstance(any())).thenReturn(new ConcurrentFIFOExoCache<>());
    instantiateService();
    when(webSocketClient.reconnectBlocking()).thenAnswer(invocation -> webSocketConnected = true);
    when(webSocketClient.isOpen()).thenAnswer(invocation -> webSocketConnected);
    setAsDisconnected();
  }

  @After
  public void tearDown() {
    service.stop();
    setAsDisconnected();
  }

  @Test
  public void testServiceStartWithDefaultSettings() {
    service.start();

    assertFalse(service.isListeningToBlockchain());
    assertFalse(service.isPermanentlyScanBlockchain());
    assertEquals(EthereumClientConnector.DEFAULT_POLLING_TIME, service.getPollingInterval());
  }

  @Test
  public void testServiceStartWithPermanentListening() {
    System.setProperty("exo.wallet.blockchain.permanentlyScan", "true");
    System.setProperty("exo.wallet.blockchain.polling.intervalInSeconds", "32000");
    instantiateService();
    service.start();

    assertTrue(service.isPermanentlyScanBlockchain());
    assertEquals(32000000, service.getPollingInterval());
  }

  @Test
  public void testServiceStop() throws Exception {
    service.start();
    assertFalse(service.isConnected());
    setAsConnected();
    assertTrue(service.isConnected());

    service.stop();
    assertFalse(service.isConnected());
  }

  @Test
  public void testConnect() throws Exception {
    service.start();
    assertTrue(service.connect());
    assertTrue(service.isConnected());
    assertTrue(service.connect());

    service.stop();
    assertFalse(service.isConnected());
    assertFalse(service.connect());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRenewTransactionListeningSubscription() throws Exception {
    service.start();
    service.connect();
    assertTrue(service.isConnected());

    long lastWatchedBlockNumber = 12l;
    service.setLastWatchedBlockNumber(lastWatchedBlockNumber);

    Flowable<org.web3j.protocol.core.methods.response.Log> flowable = mock(Flowable.class);
    when(web3j.ethLogFlowable(any())).thenReturn(flowable);

    Disposable ethFilterSubscribtion = mock(Disposable.class);
    AtomicReference<Consumer<? super org.web3j.protocol.core.methods.response.Log>> onNext = new AtomicReference<>();
    AtomicReference<Consumer<? super Throwable>> onError = new AtomicReference<>();
    when(flowable.subscribe(any(), any())).thenAnswer(invocation -> {
      onNext.set(invocation.getArgument(0, Consumer.class));
      onError.set(invocation.getArgument(1, Consumer.class));
      return ethFilterSubscribtion;
    });

    subscriptionDisposed = false;
    when(ethFilterSubscribtion.isDisposed()).thenAnswer(invocation -> subscriptionDisposed);
    doAnswer(invocation -> subscriptionDisposed = true).when(ethFilterSubscribtion).dispose();

    Future<Disposable> future = service.renewTransactionListeningSubscription(0);
    Disposable disposable = future.get();
    assertEquals(ethFilterSubscribtion, disposable);

    Log log = newLog();
    onNext.get().accept(log);
    verify(listenerService, times(1)).broadcast(CONTRACT_TRANSACTION_MINED_EVENT,
                                                null,
                                                new ContractTransactionEvent(log.getTransactionHash(),
                                                                             log.getAddress(),
                                                                             log.getData(),
                                                                             log.getTopics(),
                                                                             log.getBlockNumber().longValue()));
    assertTrue(service.isListeningToBlockchain());
    assertFalse(ethFilterSubscribtion.isDisposed());
    service.cancelTransactionListeningToBlockchain();
    assertTrue(ethFilterSubscribtion.isDisposed());
  }

  @Test
  public void testGetNotFoundTransaction() throws Exception {
    assertNull(service.getTransaction("test"));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetTransactionWhenThrowException() throws Exception {
    String transactionHash = "test";
    Request request = mock(Request.class);
    when(request.send()).thenThrow(IOException.class);

    when(web3j.ethGetTransactionByHash(transactionHash)).thenReturn(request);

    service.start();

    assertNull(service.getTransaction(transactionHash));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetTransactionTwiceWhenNullUsingCache() throws Exception {
    String transactionHash = "test";
    Request request = mock(Request.class);
    when(web3j.ethGetTransactionByHash(transactionHash)).thenReturn(request);

    service.start();

    assertNull(service.getTransaction(transactionHash));
    assertNull(service.getTransaction(transactionHash));

    verify(web3j, times(2)).ethGetTransactionByHash(transactionHash);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetTransactionTwiceUsingCacheWithAResult() throws Exception {
    String transactionHash = "test";
    Request request = mock(Request.class);
    EthTransaction ethTransaction = mock(EthTransaction.class);
    Transaction transaction = mock(Transaction.class);
    when(request.send()).thenReturn(ethTransaction);
    when(ethTransaction.getResult()).thenReturn(transaction);

    when(web3j.ethGetTransactionByHash(transactionHash)).thenReturn(request);

    service.start();

    assertEquals(transaction, service.getTransaction(transactionHash));
    assertEquals(transaction, service.getTransaction(transactionHash));

    verify(web3j, times(1)).ethGetTransactionByHash(transactionHash);
  }

  @Test
  public void testGetNotFoundTransactionReceipt() throws Exception {
    assertNull(service.getTransactionReceipt("test"));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetTransactionReceiptWhenThrowException() throws Exception {
    String transactionHash = "test";
    Request request = mock(Request.class);
    when(request.send()).thenThrow(IOException.class);

    when(web3j.ethGetTransactionReceipt(transactionHash)).thenReturn(request);

    service.start();

    assertNull(service.getTransactionReceipt(transactionHash));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetTransactionReceiptTwiceWhenNullUsingCache() throws Exception {
    String transactionHash = "test";
    Request request = mock(Request.class);
    when(web3j.ethGetTransactionReceipt(transactionHash)).thenReturn(request);

    service.start();

    assertNull(service.getTransactionReceipt(transactionHash));
    assertNull(service.getTransactionReceipt(transactionHash));

    verify(web3j, times(2)).ethGetTransactionReceipt(transactionHash);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetTransactionReceiptTwiceUsingCacheWithAResult() throws Exception {
    String transactionHash = "test";
    Request request = mock(Request.class);
    EthGetTransactionReceipt ethTransactionReceipt = mock(EthGetTransactionReceipt.class);
    TransactionReceipt transactionReceipt = mock(TransactionReceipt.class);
    when(request.send()).thenReturn(ethTransactionReceipt);
    when(ethTransactionReceipt.getResult()).thenReturn(transactionReceipt);

    service.start();

    when(web3j.ethGetTransactionReceipt(transactionHash)).thenReturn(request);

    assertEquals(transactionReceipt, service.getTransactionReceipt(transactionHash));
    assertEquals(transactionReceipt, service.getTransactionReceipt(transactionHash));

    verify(web3j, times(1)).ethGetTransactionReceipt(transactionHash);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testSendTransactionToBlockchainAsync() throws Exception {
    String rawTransaction = "test";
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setRawTransaction(rawTransaction);

    Request request = mock(Request.class);
    when(web3j.ethSendRawTransaction(rawTransaction)).thenReturn(request);

    service.sendTransactionToBlockchain(transactionDetail);

    verify(request, times(1)).sendAsync();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetLastestBlockNumberThrowExceptionWhenIOException() throws Exception {
    String rawTransaction = "test";
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setRawTransaction(rawTransaction);

    Request request = mock(Request.class);
    when(web3j.ethBlockNumber()).thenReturn(request);
    when(request.send()).thenThrow(IOException.class);

    service.start();

    assertThrows(IllegalStateException.class, () -> service.getLastestBlockNumber());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetLastestBlockNumberSync() throws Exception {
    String rawTransaction = "test";
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setRawTransaction(rawTransaction);

    Request request = mock(Request.class);
    when(web3j.ethBlockNumber()).thenReturn(request);
    EthBlockNumber ethBlockNumber = mock(EthBlockNumber.class);
    when(request.send()).thenReturn(ethBlockNumber);
    when(ethBlockNumber.getBlockNumber()).thenReturn(BigInteger.TWO);

    service.start();

    assertEquals(2l, service.getLastestBlockNumber());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetGasPrice() throws Exception {
    Request request = mock(Request.class);
    when(web3j.ethGasPrice()).thenReturn(request);
    EthGasPrice ethGasPrice = mock(EthGasPrice.class);
    when(request.send()).thenReturn(ethGasPrice);
    when(ethGasPrice.getGasPrice()).thenReturn(BigInteger.TWO);

    assertEquals(BigInteger.TWO, service.getGasPrice());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetLatestNonceByDefault() throws Exception {
    String walletAddress = "address";

    Request request = mock(Request.class);
    when(web3j.ethGetTransactionCount(walletAddress, DefaultBlockParameterName.LATEST)).thenReturn(request);
    EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
    when(request.send()).thenReturn(ethGetTransactionCount);
    when(ethGetTransactionCount.getTransactionCount()).thenReturn(BigInteger.TEN);

    assertEquals(BigInteger.TEN, service.getNonce(walletAddress));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testGetNonceByBlock() throws Exception {
    String walletAddress = "address";

    Request request = mock(Request.class);
    when(web3j.ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)).thenReturn(request);
    EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
    when(request.send()).thenReturn(ethGetTransactionCount);
    when(ethGetTransactionCount.getTransactionCount()).thenReturn(BigInteger.TEN);

    assertEquals(BigInteger.TEN, service.getNonce(walletAddress, DefaultBlockParameterName.PENDING));
  }

  @Test
  public void testSetPollingIntervalWhenLowerThanMinimum() throws Exception {
    assertThrows(IllegalStateException.class, () -> service.setPollingInterval(EthereumClientConnector.MINIMUM_POLLING_TIME - 1));
  }

  @Test
  public void testSetPollingIntervalWhenGreaterThanMinimum() throws Exception {
    int pollingInterval = EthereumClientConnector.MINIMUM_POLLING_TIME + 1;
    service.setPollingInterval(pollingInterval);
    assertEquals(pollingInterval, service.getPollingInterval());
  }

  @Test
  public void testGetPollingIntervalDefaultValue() throws Exception {
    assertEquals(EthereumClientConnector.DEFAULT_POLLING_TIME, service.getPollingInterval());
  }

  @Test
  public void testSetLastWatchedBlockNumberGreaterThanExisting() throws Exception {
    long blockNumber = 12l;
    service.setLastWatchedBlockNumber(blockNumber);
    assertEquals(blockNumber, service.getLastWatchedBlockNumber());

    service.setLastWatchedBlockNumber(blockNumber + 1);

    assertEquals(blockNumber + 1, service.getLastWatchedBlockNumber());
  }

  @Test
  public void testSetLastWatchedBlockNumberLowerThanExisting() throws Exception {
    long blockNumber = 12l;
    service.setLastWatchedBlockNumber(blockNumber);
    assertEquals(blockNumber, service.getLastWatchedBlockNumber());

    service.setLastWatchedBlockNumber(blockNumber - 1);

    assertEquals(blockNumber, service.getLastWatchedBlockNumber());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRenewTransactionListeningSubscriptionWhenException() throws Exception {
    service.start();
    service.connect();
    assertTrue(service.isConnected());

    long lastWatchedBlockNumber = 12l;
    service.setLastWatchedBlockNumber(lastWatchedBlockNumber);

    Flowable<org.web3j.protocol.core.methods.response.Log> flowable = mock(Flowable.class);
    when(web3j.ethLogFlowable(any())).thenReturn(flowable);

    when(flowable.subscribe(any(), any())).thenThrow(new UnsupportedOperationException());

    Future<Disposable> future = service.renewTransactionListeningSubscription(123);
    assertNotNull(future);
    assertNull(future.get());
    verify(listenerService, never()).broadcast(any(), any(), any());

    // The subscription has to be re-attempted by scheduled job
    // even if an exception occurs while subscribing the first time
    assertTrue(service.isListeningToBlockchain());
    assertNotNull(service.getConnectionVerifierFuture());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRenewTransactionListeningSubscription_RetryOnSubscriptionError() throws Exception {
    service.start();
    service.connect();
    assertTrue(service.isConnected());

    long lastWatchedBlockNumber = 12l;
    service.setLastWatchedBlockNumber(lastWatchedBlockNumber);

    Flowable<org.web3j.protocol.core.methods.response.Log> flowable = mock(Flowable.class);
    when(web3j.ethLogFlowable(any())).thenReturn(flowable);

    Disposable ethFilterSubscribtion = mock(Disposable.class);
    AtomicReference<Consumer<? super org.web3j.protocol.core.methods.response.Log>> onNext = new AtomicReference<>();
    AtomicReference<Consumer<? super Throwable>> onError = new AtomicReference<>();
    when(flowable.subscribe(any(), any())).thenAnswer(invocation -> {
      onNext.set(invocation.getArgument(0, Consumer.class));
      onError.set(invocation.getArgument(1, Consumer.class));
      return ethFilterSubscribtion;
    });

    subscriptionDisposed = true;
    when(ethFilterSubscribtion.isDisposed()).thenAnswer(invocation -> subscriptionDisposed);
    doAnswer(invocation -> subscriptionDisposed = true).when(ethFilterSubscribtion).dispose();

    assertTrue(ethFilterSubscribtion.isDisposed());

    Future<Disposable> future = service.renewTransactionListeningSubscription(0);
    Disposable disposable = future.get();
    assertEquals(ethFilterSubscribtion, disposable);

    verify(flowable, timeout(5000).times(1)).subscribe(any(), any());
    onError.get().accept(new UnsupportedOperationException());
    assertTrue(ethFilterSubscribtion.isDisposed());

    Log log = newLog();
    onNext.get().accept(log);
    subscriptionDisposed = false;
    verify(listenerService, times(1)).broadcast(CONTRACT_TRANSACTION_MINED_EVENT,
                                                null,
                                                new ContractTransactionEvent(log.getTransactionHash(),
                                                                             log.getAddress(),
                                                                             log.getData(),
                                                                             log.getTopics(),
                                                                             log.getBlockNumber().longValue()));
    assertTrue(service.isListeningToBlockchain());
    assertFalse(ethFilterSubscribtion.isDisposed());
    service.cancelTransactionListeningToBlockchain();
    assertTrue(ethFilterSubscribtion.isDisposed());
  }

  private void instantiateService() {
    if (service != null && service.isConnected()) {
      service.stop();
    }
    service = new EthereumClientConnector(cacheService);
    service.setWeb3j(web3j);
    service.setWeb3jService(web3jService);
    service.setWebSocketClient(webSocketClient);
    service.setListenerService(listenerService);
  }

  private void setAsConnected() {
    webSocketConnected = true;
  }

  private void setAsDisconnected() {
    webSocketConnected = false;
  }
}
