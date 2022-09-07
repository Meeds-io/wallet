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
import static org.exoplatform.wallet.utils.WalletUtils.OPERATION_FILTER_CONTRACT_TRANSACTIONS;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.EventEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthLog.LogResult;
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

  private static final Log         LOG                        = ExoLogger.getLogger(EthereumClientConnector.class);

  private static final int         MINIMUM_POOLING_TIME       = 15 * 1000;

  private static final int         DEFAULT_POOLING_TIME       = 60 * 1000;

  private Web3j                    web3j                      = null;

  private WebSocketClient          webSocketClient            = null;

  private WebSocketService         web3jService               = null;

  private ListenerService          listenerService            = null;

  private ScheduledExecutorService connectionVerifierExecutor = null;

  private ExecutorService          flowableExecutor           = null;

  private boolean                  permanentlyScanBlockchain  = false;

  private boolean                  listeningToBlockchain      = false;

  private boolean                  connectionInProgress       = false;

  private boolean                  serviceStarted             = false;

  private boolean                  serviceStopping            = false;

  private long                     networkId                  = 0;

  private String                   websocketURL               = null;

  private String                   websocketURLSuffix         = null;

  private Disposable               blockSubscribtion;

  private long                     poolingInterval;

  private long                     lastWatchedBlockNumber;

  public EthereumClientConnector() {
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-websocket-connector-%d").build();
    connectionVerifierExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

    namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-contract-flowable-%d").build();
    flowableExecutor = Executors.newFixedThreadPool(2, namedThreadFactory);

    String poolingIntervalParam = System.getProperty("exo.wallet.blockchain.pooling.intervalInSeconds");
    if (StringUtils.isNotBlank(poolingIntervalParam)) {
      setPoolingInterval(Long.parseLong(poolingIntervalParam) * 1000);
    } else {
      setPoolingInterval(DEFAULT_POOLING_TIME);
    }

    String permanentlyScanParam = System.getProperty("exo.wallet.blockchain.permanentlyScan", "false");
    permanentlyScanBlockchain = Boolean.parseBoolean(permanentlyScanParam);
  }

  @Override
  public void start() {
    this.start(true);
  }

  public void start(boolean blocking) {
    this.websocketURL = getWebsocketURL();
    this.networkId = getNetworkId();
    this.serviceStarted = true;

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
      if (blocking) {
        waitConnection();
      }
      startListeningBlockchain();
    }
  }

  @Override
  public void stop() {
    this.serviceStopping = true;
    connectionVerifierExecutor.shutdownNow();
    stopListeningToBlockchain();
    closeConnection();
  }

  public void startListeningBlockchain() {
    // Blockchain connection verifier
    connectionVerifierExecutor.scheduleAtFixedRate(this::checkConnection, 0, 10, TimeUnit.SECONDS);
  }

  public void stopListeningToBlockchain() {
    if (this.blockSubscribtion != null && !this.blockSubscribtion.isDisposed()) {
      LOG.info("Close mined contract transactions subscription");
      try {
        this.blockSubscribtion.dispose();
      } catch (Exception e) {
        LOG.warn("Error when closing old subscription", e.getMessage());
      }
    }
    this.listeningToBlockchain = false;
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
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_TRANSACTION)
  public Transaction getTransaction(String transactionHash) {
    EthTransaction ethTransaction;
    try {
      ethTransaction = getWeb3j().ethGetTransactionByHash(transactionHash).send();
    } catch (IOException e) {
      LOG.info("Connection interrupted while getting Transaction '{}' information. Reattempt until getting it. Reason: {}",
               transactionHash,
               e.getMessage());
      return getTransaction(transactionHash);
    }
    if (ethTransaction != null) {
      return ethTransaction.getResult();
    }
    return null;
  }

  /**
   * Get transaction receipt by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction receipt object
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_TRANSACTION_RECEIPT)
  public TransactionReceipt getTransactionReceipt(String transactionHash) {
    EthGetTransactionReceipt ethGetTransactionReceipt;
    try {
      ethGetTransactionReceipt = getWeb3j().ethGetTransactionReceipt(transactionHash).send();
    } catch (IOException e) {
      LOG.info("Connection interrupted while getting Transaction receipt '{}' information. Reattempt until getting it. Reason: {}",
               transactionHash,
               e.getMessage());
      return getTransactionReceipt(transactionHash);
    }
    if (ethGetTransactionReceipt != null) {
      return ethGetTransactionReceipt.getResult();
    }
    return null;
  }

  /**
   * @return last mined block number from blockchain
   * @throws IOException when error sending transaction on blockchain
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
   * Retrieve from blockchain transaction hashes from contract starting from a
   * block number to a block number
   * 
   * @param contractsAddress blockchain contract address
   * @param fromBlock search starting from this block number
   * @param toBlock search until this block number
   * @return a {@link Set} of transaction hashes
   * @throws IOException if an error happens while getting information from
   *           blockchain
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_FILTER_CONTRACT_TRANSACTIONS)
  public Set<String> getContractTransactions(String contractsAddress, long fromBlock, long toBlock) throws IOException {
    org.web3j.protocol.core.methods.request.EthFilter filter =
                                                             new org.web3j.protocol.core.methods.request.EthFilter(new DefaultBlockParameterNumber(fromBlock),
                                                                                                                   new DefaultBlockParameterNumber(toBlock),
                                                                                                                   contractsAddress);
    EthLog contractTransactions = getWeb3j().ethGetLogs(filter).send();

    @SuppressWarnings("rawtypes")
    List<LogResult> logs = contractTransactions.getResult();
    Set<String> txHashes = new HashSet<>();
    if (logs != null && !logs.isEmpty()) {
      for (LogResult<?> logResult : logs) {
        org.web3j.protocol.core.methods.response.Log contractEventLog =
                                                                      (org.web3j.protocol.core.methods.response.Log) logResult.get();
        txHashes.add(contractEventLog.getTransactionHash());
      }
    }
    return txHashes;
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
  public CompletableFuture<EthSendTransaction> sendTransactionToBlockchain(final TransactionDetail transactionDetail) throws IOException {
    return getWeb3j().ethSendRawTransaction(transactionDetail.getRawTransaction()).sendAsync();
  }

  /**
   * Retruns nonce corresponding for a given wallet address: * if using
   * {@link DefaultBlockParameterName#LATEST}, this will return last mined
   * transaction nonce * if using {@link DefaultBlockParameterName#PENDING},
   * this will return last pending transaction nonce that can be used to compute
   * next pending transaction nonce
   *
   * @param walletAddress wallet adres to determine its next nonce
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
   * @return current
   * @throws IOException if an error occurs while sending transaction to
   *           blockchain
   */
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_GAS_PRICE)
  public BigInteger getGasPrice() throws IOException {
    return getWeb3j().ethGasPrice().send().getGasPrice();
  }

  public Web3j getWeb3j() {
    this.checkConnection();
    return web3j;
  }

  @Override
  public Map<String, Object> getStatisticParameters(String statisticType, Object result, Object... methodArgs) {
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
    case OPERATION_FILTER_CONTRACT_TRANSACTIONS:
      parameters.put("from_block_number", methodArgs[1]);
      parameters.put("to_block_number", methodArgs[2]);
      if (result instanceof Set) {
        parameters.put("transactions_count_received", ((Set<?>) result).size());
      } else {
        LOG.warn("Statistict type {} has an unexpected result class type", statisticType);
      }
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
      if (contractAmount > 0
          && (StringUtils.equals(MeedsToken.FUNC_TRANSFER, methodName) || StringUtils.equals(MeedsToken.FUNC_APPROVE, methodName)
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
      return null;
    }
    return parameters;
  }

  public void renewTransactionListeningSubscription(long lastWatchedBlockNumber) {
    checkConnection();
    if (this.connectionInProgress
        || (this.listeningToBlockchain && this.blockSubscribtion != null && !this.blockSubscribtion.isDisposed())) {
      return;
    }
    if (lastWatchedBlockNumber > 0) {
      this.lastWatchedBlockNumber = lastWatchedBlockNumber;
    }

    // Close old subscription if exists
    stopListeningToBlockchain();

    // Renew subscription that will trigger an event each time a transaction is
    // mined on contract
    subscribeToBlockchain();
  }

  public void setPoolingInterval(long poolingInterval) {
    if (poolingInterval < MINIMUM_POOLING_TIME) {
      throw new IllegalStateException("Pooling interval " + poolingInterval + " shouldn't be less than block time "
          + MINIMUM_POOLING_TIME);
    }
    this.poolingInterval = poolingInterval;
  }

  public long getPoolingInterval() {
    return poolingInterval;
  }

  public void setLastWatchedBlockNumber(long blockNumber) {
    this.lastWatchedBlockNumber = Math.max(this.lastWatchedBlockNumber, blockNumber);
  }

  public long getLastWatchedBlockNumber() {
    return lastWatchedBlockNumber;
  }

  private boolean isConnected() {
    return web3j != null && web3jService != null && webSocketClient != null && webSocketClient.isOpen();
  }

  private void waitConnection() {
    if (this.serviceStarted && StringUtils.isBlank(websocketURL)) {
      throw new IllegalStateException("No websocket connection is configured for ethereum blockchain");
    }
    if (this.serviceStopping) {
      throw new IllegalStateException("Server is stopping, thus no Web3 request should be emitted");
    }
    try {
      while (!isConnected()) {
        if (this.serviceStarted && StringUtils.isBlank(websocketURL)) {
          throw new IllegalStateException("No websocket connection is configured for ethereum blockchain");
        }
        if (this.serviceStopping) {
          throw new IllegalStateException("Server is stopping, thus no Web3 request should be emitted");
        }
        LOG.debug("Wait until Websocket connection to blockchain is established to retrieve information");
        Thread.sleep(5000);
      }
    } catch (Exception e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("An error is thrown while waiting for connection on blockchain", e);
    }
  }

  private synchronized boolean connect() throws Exception { // NOSONAR
    if (this.connectionInProgress) {
      LOG.debug("Web3 connection is in progress");
      return false;
    }
    if (isConnected()) {
      LOG.debug("Web3 connection is already made");
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

    this.connectionInProgress = true;
    try {
      if (this.web3j != null && this.web3jService != null && this.webSocketClient != null) {
        LOG.info("Reconnect to blockchain endpoint {}", websocketURL);
        boolean reconnected = this.webSocketClient.reconnectBlocking();
        if (reconnected) {
          LOG.info("Connection established to Ethereum network endpoint {}", websocketURL);
          if (this.listeningToBlockchain) {
            renewTransactionListeningSubscription(this.lastWatchedBlockNumber);
          }
        }
        return reconnected;
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
        web3j = Web3j.build(web3jService, getPoolingInterval(), Async.defaultExecutorService());
        LOG.info("Connection established to Ethereum network endpoint {}", websocketURL);
        return true;
      }
    } finally {
      this.connectionInProgress = false;
    }
  }

  private void checkConnection() {
    try {
      if (!isConnected()) {
        connect();
        if (permanentlyScanBlockchain) {
          waitConnection();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Throwable e) { // NOSONAR
      if (LOG.isDebugEnabled()) {
        LOG.warn("Error while checking connection status to Etherreum Websocket endpoint", e);
      } else {
        LOG.warn("Error while checking connection status to Etherreum Websocket endpoint: {}", e.getMessage());
      }
    }
  }

  private void closeConnection() {
    if (web3j != null) {
      LOG.info("Closing blockchain connection");
      try {
        web3j.shutdown();
        web3j = null;
        web3jService = null;
        webSocketClient = null;
      } catch (Throwable e) {
        LOG.warn("Error closing old web3j connection: {}", e.getMessage());
      }
    }
    if (web3jService != null && webSocketClient != null && webSocketClient.isOpen()) {
      try {
        web3jService.close();
        web3jService = null;
        webSocketClient = null;
      } catch (Throwable e1) {
        LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
      }
    }
    if (webSocketClient != null && webSocketClient.isOpen()) {
      try {
        webSocketClient.close();
        webSocketClient = null;
      } catch (Throwable e1) {
        LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
      }
    }
  }

  private void subscribeToBlockchain() {
    // Replay all blocks until last mined one
    this.listeningToBlockchain = true;
    LOG.info("Start watching mined contract transactions from blockchain from block '{}'", this.lastWatchedBlockNumber);
    DefaultBlockParameterNumber lastWatchedBlock = getLastWatchedBlock();
    flowableExecutor.execute(() -> {
      org.web3j.protocol.core.methods.request.EthFilter ethFilter =
                                                                  new org.web3j.protocol.core.methods.request.EthFilter(lastWatchedBlock,
                                                                                                                        DefaultBlockParameterName.LATEST,
                                                                                                                        getContractAddress());
      ethFilter.addSingleTopic(EventEncoder.encode(MeedsToken.TRANSFER_EVENT));
      Flowable<org.web3j.protocol.core.methods.response.Log> flowable = web3j.ethLogFlowable(ethFilter);
      blockSubscribtion = flowable.subscribe(this::handleNewContractTransactionMined, exception -> {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Subscription to mined contract transactions mining event is interrupted, reattempt subsciption it !",
                    exception);
        } else {
          LOG.info("Subscription to mined contract transactions mining event is interrupted with message: {}, reattempt subsciption it !",
                   getLastExceptionMessage(exception));
        }
        if (this.listeningToBlockchain) { // check if the listening was asked to
                                          // be stopped asynchronously
          subscribeToBlockchain();
        }
      });
    });
  }

  private String getLastExceptionMessage(Throwable exception) {
    if (exception == null) {
      return "NO EXCEPTION MESSAGE";
    } else if (exception.getCause() == null) {
      return exception.getMessage();
    } else {
      return getLastExceptionMessage(exception.getCause());
    }
  }

  private void handleNewContractTransactionMined(org.web3j.protocol.core.methods.response.Log log) throws Exception {
    String transactionHash = log.getTransactionHash();
    if (StringUtils.isBlank(transactionHash)) {
      return;
    }
    long blockNumber = log.getBlockNumber().longValue();
    this.lastWatchedBlockNumber = Math.max(this.lastWatchedBlockNumber, blockNumber);
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
