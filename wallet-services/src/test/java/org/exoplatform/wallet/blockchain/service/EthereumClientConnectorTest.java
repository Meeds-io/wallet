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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketService;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.test.BaseWalletTest;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

@RunWith(MockitoJUnitRunner.class)
public class EthereumClientConnectorTest extends BaseWalletTest {

  @Mock
  private Web3j                   web3j;

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

  @Test
  public void testWaitConnection() throws Exception {
    service.start();
    service.connect();
    assertTrue(service.isConnected());

    service.stop();
    assertThrows(IllegalStateException.class, () -> service.waitConnection());
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
    service.stopListeningToBlockchain();
    assertTrue(ethFilterSubscribtion.isDisposed());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRenewTransactionListeningSubscriptionException() throws Exception {
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
    verify(listenerService, times(0)).broadcast(any(), any(), any());

    assertFalse(service.isListeningToBlockchain());
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
    service.stopListeningToBlockchain();
    assertTrue(ethFilterSubscribtion.isDisposed());
  }

  private void instantiateService() {
    if (service != null && service.isConnected()) {
      service.stop();
    }
    service = new EthereumClientConnector(mock(CacheService.class));
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
