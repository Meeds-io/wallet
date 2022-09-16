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
package org.exoplatform.wallet.blockchain.service;

import static org.exoplatform.wallet.utils.WalletUtils.CONTRACT_TRANSACTION_MINED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.GWEI_TO_WEI_DECIMALS;
import static org.exoplatform.wallet.utils.WalletUtils.OPERATION_GET_GAS_PRICE;
import static org.exoplatform.wallet.utils.WalletUtils.OPERATION_GET_LAST_BLOCK_NUMBER;
import static org.exoplatform.wallet.utils.WalletUtils.OPERATION_GET_TRANSACTION;
import static org.exoplatform.wallet.utils.WalletUtils.OPERATION_GET_TRANSACTION_COUNT;
import static org.exoplatform.wallet.utils.WalletUtils.OPERATION_GET_TRANSACTION_RECEIPT;
import static org.exoplatform.wallet.utils.WalletUtils.OPERATION_SEND_TRANSACTION;
import static org.exoplatform.wallet.utils.WalletUtils.convertFromDecimals;
import static org.exoplatform.wallet.utils.WalletUtils.getContractAddress;
import static org.exoplatform.wallet.utils.WalletUtils.getNetworkId;
import static org.exoplatform.wallet.utils.WalletUtils.getWebsocketURL;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.EventEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.websocket.WebSocketClient;
import org.web3j.protocol.websocket.WebSocketListener;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.utils.Async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.future.FutureCache;
import org.exoplatform.services.cache.future.FutureExoCache;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.statistic.ExoWalletStatistic;
import org.exoplatform.wallet.statistic.ExoWalletStatisticService;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A Web3j connector class to interact with Ethereum Blockchain
 */
public class EthereumClientConnector implements ExoWalletStatisticService, Startable {

  public static final int                                 MINIMUM_POLLING_TIME         = 2 * 1000;

  public static final int                                 DEFAULT_POLLING_TIME         = 15 * 1000;

  private static final Log                                LOG                          =
                                                              ExoLogger.getLogger(EthereumClientConnector.class);

  private Web3j                                           web3j                        = null;

  private WebSocketClient                                 webSocketClient              = null;

  private WebSocketService                                web3jService                 = null;

  private ListenerService                                 listenerService              = null;

  private FutureCache<String, Transaction, Object>        transactionFutureCache       = null;

  private FutureCache<String, TransactionReceipt, Object> receiptFutureCache           = null;

  private ScheduledExecutorService                        subscriptionVerifierExecutor = null;

  private ScheduledExecutorService                        connectionVerifierExecutor   = null;

  private ScheduledFuture<?>                              connectionVerifierFuture;

  private boolean                                         permanentlyScanBlockchain    = false;

  private boolean                                         listeningToBlockchain        = false;

  private boolean                                         serviceStopping              = false;

  private long                                            networkId                    = 0;

  private String                                          websocketURL                 = null;

  private String                                          websocketURLSuffix           = null;

  private Disposable                                      ethFilterSubscribtion;

  private boolean                                         subscriptionInProgress       = false;

  private long                                            pollingInterval;

  private long                                            lastWatchedBlockNumber;

  public EthereumClientConnector(CacheService cacheService) {
    String pollingIntervalParam = System.getProperty("exo.wallet.blockchain.polling.intervalInSeconds");
    if (StringUtils.isNotBlank(pollingIntervalParam)) {
      setPollingInterval(Long.parseLong(pollingIntervalParam) * 1000);
    } else {
      setPollingInterval(DEFAULT_POLLING_TIME);
    }

    String permanentlyScanParam = System.getProperty("exo.wallet.blockchain.permanentlyScan", "false");
    permanentlyScanBlockchain = Boolean.parseBoolean(permanentlyScanParam);

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-websocket-connector-%d").build();
    connectionVerifierExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

    namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-contract-flowable-%d").build();
    subscriptionVerifierExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

    ExoCache<String, Transaction> transactionCache = cacheService.getCacheInstance("wallet.blockchain.transaction");
    ExoCache<String, TransactionReceipt> receiptCache = cacheService.getCacheInstance("wallet.blockchain.transactionReceipt");
    transactionFutureCache = new FutureExoCache<>((context, hash) -> getTransactionFromBlockchain(hash), transactionCache);
    receiptFutureCache = new FutureExoCache<>((context, hash) -> getTransactionReceiptFromBlockchain(hash), receiptCache);
  }

  @Override
  public void start() {
    this.websocketURL = getWebsocketURL();
    this.networkId = getNetworkId();

    // Default unhandeled errors management
    if (RxJavaPlugins.getErrorHandler() == null) {
      RxJavaPlugins.setErrorHandler(e -> {
        if (e instanceof UndeliverableException) {
          e = e.getCause();
        }
        LOG.warn("Unhandeled error happened while communicating with blockchain", e);
      });
    }

    if (permanentlyScanBlockchain) {
      startPeriodicConnectionVerifier();
    }
    // Blockchain connection verifier
    subscriptionVerifierExecutor.scheduleWithFixedDelay(this::checkSubscription, 10, 15, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    this.serviceStopping = true;
    connectionVerifierExecutor.shutdownNow();
    subscriptionVerifierExecutor.shutdownNow();
    stopListeningToBlockchain();
    closeConnection();
  }

  public boolean isPermanentlyScanBlockchain() {
    return permanentlyScanBlockchain;
  }

  public boolean isListeningToBlockchain() {
    return listeningToBlockchain;
  }

  /**
   * Get transaction by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction object
   */
  public Transaction getTransaction(String transactionHash) {
    return transactionFutureCache.get(null, StringUtils.lowerCase(transactionHash));
  }

  /**
   * Get transaction by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction object
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_TRANSACTION)
  public Transaction getTransactionFromBlockchain(String transactionHash) {
    try {
      EthTransaction ethTransaction = getWeb3j().ethGetTransactionByHash(transactionHash).send();
      return ethTransaction == null ? null : ethTransaction.getResult();
    } catch (IOException e) {
      throw new IllegalStateException("Connection error with Blockchain while attempting to retrieve transaction "
          + transactionHash, e);
    }
  }

  /**
   * Get transaction receipt by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction receipt object
   */
  public TransactionReceipt getTransactionReceipt(String transactionHash) {
    return receiptFutureCache.get(null, StringUtils.lowerCase(transactionHash));
  }

  /**
   * Get transaction receipt by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction receipt object
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_TRANSACTION_RECEIPT)
  public TransactionReceipt getTransactionReceiptFromBlockchain(String transactionHash) {
    try {
      EthGetTransactionReceipt ethTransactionReceipt = getWeb3j().ethGetTransactionReceipt(transactionHash).send();
      return ethTransactionReceipt == null ? null : ethTransactionReceipt.getResult();
    } catch (IOException e) {
      throw new IllegalStateException("Connection error with Blockchain while attempting to retrieve transaction receipt "
          + transactionHash, e);
    }
  }

  /**
   * @return last mined block number from blockchain
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_LAST_BLOCK_NUMBER)
  public long getLastestBlockNumber() {
    try {
      BigInteger blockNumber = getWeb3j().ethBlockNumber().send().getBlockNumber();
      return blockNumber.longValue();
    } catch (IOException e) {
      throw new IllegalStateException("Connection error with Blockchain while attempting to retrieve block number", e);
    }
  }

  /**
   * Send raw transaction specified in Transaction detail
   * 
   * @param transactionDetail {@link TransactionDetail} having rawTransaction to
   *          send to blockchain
   * @return {@link CompletableFuture} for transaction sent asynchronously to
   *         blockchain
   * @throws IOException if an error occurs while sending transaction to
   *           blockchain
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_SEND_TRANSACTION)
  public CompletableFuture<EthSendTransaction> sendTransactionToBlockchain(TransactionDetail transactionDetail) throws IOException {
    return getWeb3j().ethSendRawTransaction(transactionDetail.getRawTransaction()).sendAsync();
  }

  /**
   * Retruns nonce corresponding for a given wallet address: * if using
   * {@link DefaultBlockParameterName#LATEST}, this will return last mined
   * transaction nonce * if using {@link DefaultBlockParameterName#PENDING},
   * this will return last pending transaction nonce that can be used to compute
   * next pending transaction nonce
   *
   * @param walletAddress wallet address to determine its next nonce
   * @param blockParameterName {@link DefaultBlockParameterName} value
   * @return next transaction nonce
   * @throws IOException if an I/O problem happens when connecting to blockchain
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_TRANSACTION_COUNT)
  public BigInteger getNonce(String walletAddress, DefaultBlockParameterName blockParameterName) throws IOException {
    if (blockParameterName == null) {
      blockParameterName = DefaultBlockParameterName.LATEST;
    }
    return getWeb3j().ethGetTransactionCount(walletAddress, blockParameterName).send().getTransactionCount();
  }

  /**
   * Retruns last mined transaction nonce
   *
   * @param walletAddress wallet address to determine its next nonce
   * @return next transaction nonce
   * @throws IOException if an I/O problem happens when connecting to blockchain
   */
  public BigInteger getNonce(String walletAddress) throws IOException {
    return getNonce(walletAddress, null);
  }

  /**
   * @return current
   * @throws IOException if an error occurs while sending transaction to
   *           blockchain
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_GAS_PRICE)
  public BigInteger getGasPrice() throws IOException {
    return getWeb3j().ethGasPrice().send().getGasPrice();
  }

  public Web3j getWeb3j() {
    this.checkConnection(false);
    return web3j;
  }

  @Override
  public Map<String, Object> getStatisticParameters(String statisticType, Object result, Object... methodArgs) { // NOSONAR
    Map<String, Object> parameters = new HashMap<>();

    if (networkId > 0 && StringUtils.isNotBlank(websocketURL)) {
      if (websocketURLSuffix == null) {
        String[] urlParts = websocketURL.split("/");
        websocketURLSuffix = urlParts[urlParts.length - 1];
      }
      parameters.put("blockchain_network_url_suffix", websocketURLSuffix);
      parameters.put("blockchain_network_id", networkId);
    }

    switch (statisticType) {
    case OPERATION_GET_TRANSACTION:
      parameters.put("transaction_hash", methodArgs[0]);
      break;
    case OPERATION_GET_TRANSACTION_RECEIPT:
      parameters.put("transaction_hash", methodArgs[0]);
      break;
    case OPERATION_GET_TRANSACTION_COUNT:
      parameters.put("wallet_address", methodArgs[0]);
      break;
    case OPERATION_GET_GAS_PRICE:
      BigInteger gasPriceInWei = (BigInteger) result;
      parameters.put("ga_price_gwei",
                     convertFromDecimals(BigInteger.valueOf(gasPriceInWei == null ? 0 : gasPriceInWei.longValue()),
                                         GWEI_TO_WEI_DECIMALS));
      break;
    case OPERATION_GET_LAST_BLOCK_NUMBER:
      parameters.put("last_block_number", result);
      break;
    case OPERATION_SEND_TRANSACTION:
      TransactionDetail transactionDetail = (TransactionDetail) methodArgs[0];
      String methodName = transactionDetail.getContractMethodName();
      parameters.put("hash", transactionDetail.getHash());
      parameters.put("nonce", transactionDetail.getNonce());
      parameters.put("sender", transactionDetail.getFromWallet());
      parameters.put("receiver", transactionDetail.getToWallet());
      parameters.put("gas_price",
                     convertFromDecimals(BigInteger.valueOf((long) transactionDetail.getGasPrice()), GWEI_TO_WEI_DECIMALS));

      if (StringUtils.isNotBlank(transactionDetail.getContractAddress())) {
        parameters.put("contract_address", transactionDetail.getContractAddress());
      }
      if (StringUtils.isNotBlank(methodName)) {
        parameters.put("contract_method", methodName);
      }

      double contractAmount = transactionDetail.getContractAmount();
      if (contractAmount > 0 && (StringUtils.equals(MeedsToken.FUNC_TRANSFER, methodName)
          || StringUtils.equals(MeedsToken.FUNC_TRANSFERFROM, methodName))) {
        parameters.put("amount_token", contractAmount);
      }

      double valueAmount = transactionDetail.getValue();
      if (valueAmount > 0) {
        parameters.put("amount_ether", valueAmount);
      }
      break;
    default:
      LOG.warn("Statistic type {} not managed", statisticType);
      return null; // NOSONAR
    }
    return parameters;
  }

  public synchronized Future<Disposable> renewTransactionListeningSubscription(long lastWatchedBlockNumber) {
    this.setLastWatchedBlockNumber(lastWatchedBlockNumber);
    return renewTransactionListeningSubscription();
  }

  public synchronized void cancelTransactionListeningToBlockchain() {
    stopListeningToBlockchain();
    stopPeriodicConnectionVerifier();
  }

  public void setPollingInterval(long pollingInterval) {
    if (pollingInterval < MINIMUM_POLLING_TIME) {
      throw new IllegalStateException("Polling interval " + pollingInterval + " shouldn't be less than block time "
          + MINIMUM_POLLING_TIME);
    }
    this.pollingInterval = pollingInterval;
  }

  public long getPollingInterval() {
    return pollingInterval;
  }

  public void setLastWatchedBlockNumber(long blockNumber) {
    this.lastWatchedBlockNumber = Math.max(this.lastWatchedBlockNumber, blockNumber);
  }

  public long getLastWatchedBlockNumber() {
    return lastWatchedBlockNumber;
  }

  protected void setWebSocketClient(WebSocketClient webSocketClient) {
    this.webSocketClient = webSocketClient;
  }

  protected void setWeb3j(Web3j web3j) {
    this.web3j = web3j;
  }

  protected void setWeb3jService(WebSocketService web3jService) {
    this.web3jService = web3jService;
  }

  protected boolean isConnected() {
    return web3j != null && web3jService != null && webSocketClient != null && webSocketClient.isOpen();
  }

  protected boolean connect() throws Exception { // NOSONAR
    return connect(false);
  }

  protected synchronized boolean connect(boolean periodicCheck) throws Exception { // NOSONAR
    if (periodicCheck) {
      startPeriodicConnectionVerifier();
    }
    if (isConnected()) {
      return true;
    }
    if (this.serviceStopping) {
      LOG.info("Stopping server, thus no new connection is attempted again");
      return false;
    }
    if (StringUtils.isBlank(websocketURL)) {
      LOG.info("No configured URL for Ethereum Websocket connection");
      return false;
    }
    if (!websocketURL.startsWith("ws:") && !websocketURL.startsWith("wss:")) {
      LOG.warn("Bad format for configured URL " + websocketURL + " for Ethereum Websocket connection");
      return false;
    }

    if (this.web3j != null && this.web3jService != null && this.webSocketClient != null) {
      LOG.info("Reconnect to blockchain endpoint {}", websocketURL);
      return this.webSocketClient.reconnectBlocking();
    } else {
      LOG.info("Connecting to Ethereum network endpoint {}", websocketURL);
      this.webSocketClient = new WebSocketClient(new URI(websocketURL));
      this.webSocketClient.setConnectionLostTimeout(10);
      this.web3jService = new WebSocketService(webSocketClient, true);
      this.webSocketClient.setListener(new WebSocketListener() {
        @Override
        public void onMessage(String message) throws IOException {
          LOG.debug("A new message is delivered from blockchain: {}", message);
        }

        @Override
        public void onError(Exception e) {
          LOG.warn("Error connecting to blockchain: {}", (e == null ? "" : e.getMessage()));
        }

        @Override
        public void onClose() {
          LOG.debug("Blockchain websocket connection closed");
        }
      });
      this.web3jService.connect();
      web3j = Web3j.build(web3jService, getPollingInterval(), Async.defaultExecutorService());
      LOG.info("Connection established to Ethereum network endpoint {}", websocketURL);
      return true;
    }
  }

  protected boolean checkConnection(boolean periodicCheck) {
    try {
      return connect(periodicCheck);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Throwable e) { // NOSONAR
      LOG.warn("Error while checking connection status to Etherreum Websocket endpoint: {}", e);
    }
    return false;
  }

  protected void closeConnection() {
    if (web3j != null) {
      LOG.info("Closing blockchain connection");
      try {
        web3j.shutdown();
        web3j = null;
        web3jService = null;
        webSocketClient = null;
      } catch (Throwable e) { // NOSONAR
        LOG.warn("Error closing old web3j connection: {}", e.getMessage());
      }
    }
    if (web3jService != null && webSocketClient != null && webSocketClient.isOpen()) {
      try {
        web3jService.close();
        web3jService = null;
        webSocketClient = null;
      } catch (Throwable e1) { // NOSONAR
        LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
      }
    }
    if (webSocketClient != null && webSocketClient.isOpen()) {
      try {
        webSocketClient.close();
        webSocketClient = null;
      } catch (Throwable e1) { // NOSONAR
        LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
      }
    }
  }

  protected void setListenerService(ListenerService listenerService) {
    this.listenerService = listenerService;
  }

  protected ScheduledFuture<?> getConnectionVerifierFuture() { // NOSONAR
    return connectionVerifierFuture;
  }

  protected synchronized void checkSubscription() {
    if (this.listeningToBlockchain && !this.subscriptionInProgress
        && (this.ethFilterSubscribtion == null || this.ethFilterSubscribtion.isDisposed())) {
      try {
        if (checkConnection(false)) {
          Future<Disposable> subscribion = subscribeToBlockchain();
          subscribion.get();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        LOG.warn("Error subscribing to blockchain", e);
      }
    }
  }

  private synchronized void startPeriodicConnectionVerifier() {
    if (connectionVerifierFuture == null) {
      LOG.info("Start periodic WebSocket endpoint connection status check");
      // Blockchain connection verifier
      connectionVerifierFuture = connectionVerifierExecutor.scheduleWithFixedDelay(() -> this.checkConnection(false),
                                                                                   0,
                                                                                   30,
                                                                                   TimeUnit.SECONDS);
    }
  }

  private void stopPeriodicConnectionVerifier() {
    if (connectionVerifierFuture != null && !permanentlyScanBlockchain) {
      connectionVerifierFuture.cancel(false);
      connectionVerifierFuture = null;
      LOG.info("Stop periodic WebSocket endpoint connection status check");
    }
  }

  private Future<Disposable> renewTransactionListeningSubscription() {
    try {
      checkConnection(true);
      if (!this.listeningToBlockchain
          || (!this.subscriptionInProgress && (this.ethFilterSubscribtion == null || this.ethFilterSubscribtion.isDisposed()))) {
        // Close old subscription if exists
        stopListeningToBlockchain();

        // Renew subscription that will trigger an event each time a transaction
        // is mined on contract
        return subscribeToBlockchain();
      }
    } finally {
      this.listeningToBlockchain = true;
    }
    return null;
  }

  private Future<Disposable> subscribeToBlockchain() {
    this.listeningToBlockchain = true;
    this.subscriptionInProgress = true;
    LOG.info("Start watching mined contract transactions from blockchain from block '{}'", this.lastWatchedBlockNumber);
    DefaultBlockParameterNumber lastWatchedBlock = getLastWatchedBlock();
    return CompletableFuture.supplyAsync(() -> {
      try {
        org.web3j.protocol.core.methods.request.EthFilter ethFilter =
                                                                    new org.web3j.protocol.core.methods.request.EthFilter(lastWatchedBlock,
                                                                                                                          DefaultBlockParameterName.LATEST,
                                                                                                                          getContractAddress());
        ethFilter.addSingleTopic(EventEncoder.encode(MeedsToken.TRANSFER_EVENT));
        Flowable<org.web3j.protocol.core.methods.response.Log> flowable = getWeb3j().ethLogFlowable(ethFilter);
        ethFilterSubscribtion = flowable.subscribe(this::handleNewContractTransactionMined,
                                                   exception -> LOG.debug("Error event received when watching events on contract",
                                                                          exception));
        return ethFilterSubscribtion;
      } catch (Exception e) {
        LOG.warn("Error while subscribing to Blockchain Filter Contract events", e);
        return null;
      } finally {
        this.subscriptionInProgress = false;
      }
    });
  }

  private synchronized void stopListeningToBlockchain() {
    try {
      if (this.ethFilterSubscribtion != null && !this.ethFilterSubscribtion.isDisposed()) {
        LOG.info("Close mined contract transactions subscription");
        try {
          this.ethFilterSubscribtion.dispose();
        } catch (Exception e) {
          LOG.warn("Error when closing old subscription", e.getMessage());
        }
      }
    } finally {
      this.listeningToBlockchain = false;
    }
  }

  private void handleNewContractTransactionMined(org.web3j.protocol.core.methods.response.Log log) throws Exception {
    String transactionHash = log.getTransactionHash();
    if (StringUtils.isBlank(transactionHash)) {
      return;
    }
    long blockNumber = log.getBlockNumber().longValue();
    this.setLastWatchedBlockNumber(blockNumber);
    getListenerService().broadcast(CONTRACT_TRANSACTION_MINED_EVENT,
                                   null,
                                   new ContractTransactionEvent(transactionHash,
                                                                log.getAddress(),
                                                                log.getData(),
                                                                log.getTopics(),
                                                                blockNumber));
  }

  private DefaultBlockParameterNumber getLastWatchedBlock() {
    return new DefaultBlockParameterNumber(this.lastWatchedBlockNumber);
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

}
