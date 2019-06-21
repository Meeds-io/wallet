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
package org.exoplatform.addon.wallet.service;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.EthLog.LogResult;
import org.web3j.protocol.websocket.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A Web3j connector class to interact with Ethereum Blockchain
 */
public class EthereumClientConnector {

  private static final Log         LOG                        = ExoLogger.getLogger(EthereumClientConnector.class);

  private Web3j                    web3j                      = null;

  private WebSocketClient          webSocketClient            = null;

  private WebSocketService         web3jService               = null;

  private ScheduledExecutorService connectionVerifierExecutor = null;

  private boolean                  connectionInProgress       = false;

  private boolean                  serviceStarted             = false;

  private boolean                  serviceStopping            = false;

  private String                   websoketURL                = null;

  public EthereumClientConnector() {
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-websocket-connector-%d").build();
    connectionVerifierExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
  }

  public void start(String websoketURL) {
    this.websoketURL = websoketURL;
    this.serviceStarted = true;

    // Blockchain connection verifier
    connectionVerifierExecutor.scheduleWithFixedDelay(() -> {
      try {
        if (!initWeb3Connection()) {
          return;
        }
      } catch (Throwable e) {
        LOG.warn("Error while checking connection status to Etherreum Websocket endpoint: {}", e.getMessage());
        return;
      }
    }, 5, 10, TimeUnit.SECONDS);
  }

  public void stop() {
    this.serviceStopping = true;
    connectionVerifierExecutor.shutdownNow();
    resetConnection();
  }

  /**
   * Get transaction by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction object
   * @throws InterruptedException when Blockchain request is interrupted
   */
  public Transaction getTransaction(String transactionHash) throws InterruptedException {
    waitConnection();
    EthTransaction ethTransaction;
    try {
      ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
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
   * Get block by hash
   * 
   * @param blockHash block hash to retrieve
   * @return Web3j Block object
   * @throws InterruptedException when Blockchain request is interrupted
   */
  public Block getBlock(String blockHash) throws InterruptedException {
    waitConnection();
    EthBlock ethBlock;
    try {
      ethBlock = web3j.ethGetBlockByHash(blockHash, false).send();
    } catch (IOException e) {
      LOG.info("Connection interrupted while getting Block '{}' information. Reattempt until getting it. Reason: {}",
               blockHash,
               e.getMessage());
      return getBlock(blockHash);
    }
    if (ethBlock != null && ethBlock.getResult() != null) {
      return ethBlock.getResult();
    }
    return null;
  }

  /**
   * Get transaction receipt by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction receipt object
   * @throws InterruptedException when Blockchain request is interrupted
   */
  public TransactionReceipt getTransactionReceipt(String transactionHash) throws InterruptedException {
    waitConnection();
    EthGetTransactionReceipt ethGetTransactionReceipt;
    try {
      ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send();
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
   * @return true if the connection to the blockchain is established
   */
  public boolean isConnected() {
    return web3j != null && web3jService != null && webSocketClient != null && webSocketClient.isOpen();
  }

  /**
   * @return last mined block number from blockchain
   * @throws InterruptedException
   * @throws IOException
   */
  public long getLastestBlockNumber() throws InterruptedException, IOException {
    waitConnection();
    Block block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
    return block.getNumber().longValue();
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
   * @throws InterruptedException if an interruption is made while getting
   *           information from blockchain
   */
  public Set<String> getContractTransactions(String contractsAddress,
                                             long fromBlock,
                                             long toBlock) throws IOException, InterruptedException {
    waitConnection();
    org.web3j.protocol.core.methods.request.EthFilter filter =
                                                             new org.web3j.protocol.core.methods.request.EthFilter(new DefaultBlockParameterNumber(fromBlock),
                                                                                                                   new DefaultBlockParameterNumber(toBlock),
                                                                                                                   contractsAddress);
    EthLog contractTransactions = web3j.ethGetLogs(filter).send();

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

  public Web3j getWeb3j() throws InterruptedException {
    this.waitConnection();
    return web3j;
  }

  public void waitConnection() throws InterruptedException {
    if (this.serviceStarted && StringUtils.isBlank(websoketURL)) {
      throw new IllegalStateException("No websocket connection is configured for ethereum blockchain");
    }
    if (this.serviceStopping) {
      throw new IllegalStateException("Server is stopping, thus no Web3 request should be emitted");
    }
    while (!isConnected()) {
      if (this.serviceStarted && StringUtils.isBlank(websoketURL)) {
        throw new IllegalStateException("No websocket connection is configured for ethereum blockchain");
      }
      if (this.serviceStopping) {
        throw new IllegalStateException("Server is stopping, thus no Web3 request should be emitted");
      }
      LOG.info("Wait until Websocket connection to blockchain is established to retrieve information");
      Thread.sleep(5000);
    }
  }

  private void resetConnection() {
    if (web3j != null) {
      LOG.info("Resetting blockchain connection");
      try {
        web3j.shutdown();
      } catch (Throwable e) {
        LOG.warn("Error closing old web3j connection: {}", e.getMessage());
        if (this.web3jService != null && webSocketClient != null && webSocketClient.isOpen()) {
          try {
            web3jService.close();
          } catch (Throwable e1) {
            LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
          }
        } else if (webSocketClient != null && webSocketClient.isOpen()) {
          try {
            webSocketClient.close();
          } catch (Throwable e1) {
            LOG.warn("Error closing old websocket connection: {}", e1.getMessage());
          }
        }
      }
      web3j = null;
      web3jService = null;
      webSocketClient = null;
    }
  }

  private boolean initWeb3Connection() throws Exception { // NOSONAR
    if (this.connectionInProgress) {
      LOG.info("Web3 connection initialization in progress, skip transaction processing until it's initialized");
      return false;
    }
    if (this.serviceStopping) {
      LOG.info("Stopping server, thus no new connection is attempted again");
      return false;
    }
    String websocketProviderURL = websoketURL;
    if (StringUtils.isBlank(websocketProviderURL)) {
      LOG.info("No configured URL for Ethereum Websocket connection");
      resetConnection();
      return false;
    }
    if (!websocketProviderURL.startsWith("ws:") && !websocketProviderURL.startsWith("wss:")) {
      LOG.warn("Bad format for configured URL " + websocketProviderURL + " for Ethereum Websocket connection");
      resetConnection();
      return false;
    }
    if (isConnected()) {
      return false;
    }

    this.connectionInProgress = true;
    try {
      if (this.web3j != null && this.web3jService != null && this.webSocketClient != null) {
        LOG.info("Reconnect to blockchain endpoint {}", websoketURL);
        boolean reconnected = this.webSocketClient.reconnectBlocking();
        if (!reconnected) {
          throw new IllegalStateException("Can't reconnect to ethereum blockchain: " + websoketURL);
        }
      } else {
        LOG.info("Connecting to Ethereum network endpoint {}", websoketURL);
        this.webSocketClient = new WebSocketClient(new URI(websoketURL));
        this.webSocketClient.setConnectionLostTimeout(10);
        this.web3jService = new WebSocketService(webSocketClient, true);
        this.webSocketClient.setListener(getWebsocketListener());
        this.web3jService.connect();
        Thread.sleep(10000);
        web3j = Web3j.build(web3jService);
        LOG.info("Connection established to Ethereum network endpoint {}", websoketURL);
      }
      return true;
    } finally {
      this.connectionInProgress = false;
    }
  }

  private WebSocketListener getWebsocketListener() {
    return new WebSocketListener() {
      @Override
      public void onMessage(String message) throws IOException {
        LOG.debug("A new message is received in testConnection method");
      }

      @Override
      public void onError(Exception e) {
        LOG.warn(getConnectionFailedMessage());
      }

      @Override
      public void onClose() {
        LOG.debug("Websocket connection closed for testConnection method");
      }
    };
  }

  private String getConnectionFailedMessage() {
    return "Connection failed to " + websoketURL;
  }

}
