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
package org.exoplatform.addon.wallet.blockchain.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.core.methods.response.EthLog.LogResult;
import org.web3j.protocol.websocket.*;
import org.web3j.tx.*;
import org.web3j.tx.response.NoOpProcessor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.addon.wallet.statistic.ExoWalletStatistic;
import org.exoplatform.addon.wallet.statistic.ExoWalletStatisticService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A Web3j connector class to interact with Ethereum Blockchain
 */
public class EthereumClientConnector implements ExoWalletStatisticService, Startable {

  private static final Log         LOG                        = ExoLogger.getLogger(EthereumClientConnector.class);

  private Web3j                    web3j                      = null;

  private WebSocketClient          webSocketClient            = null;

  private WebSocketService         web3jService               = null;

  private ScheduledExecutorService connectionVerifierExecutor = null;

  private boolean                  connectionInProgress       = false;

  private boolean                  serviceStarted             = false;

  private boolean                  serviceStopping            = false;

  private String                   websocketURL               = null;

  public EthereumClientConnector() {
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-websocket-connector-%d").build();
    connectionVerifierExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
  }

  @Override
  public void start() {
    this.start(false);
  }

  public void start(boolean blocking) {
    this.websocketURL = getWebsocketURL();
    this.serviceStarted = true;

    // Blockchain connection verifier
    connectionVerifierExecutor.scheduleAtFixedRate(() -> {
      try {
        if (isConnected()) {
          return;
        } else {
          connect();
        }
      } catch (Throwable e) {
        if (LOG.isDebugEnabled()) {
          LOG.warn("Error while checking connection status to Etherreum Websocket endpoint", e);
        } else {
          LOG.warn("Error while checking connection status to Etherreum Websocket endpoint: {}", e.getMessage());
        }
      }
    }, 0, 10, TimeUnit.SECONDS);

    if (blocking) {
      waitConnection();
    }
  }

  @Override
  public void stop() {
    this.serviceStopping = true;
    connectionVerifierExecutor.shutdownNow();
    closeConnection();
  }

  /**
   * Return {@link TransactionManager} to use when interacting with blockchain
   * 
   * @param credentials wallet credentials to use for this transaction manager
   * @return {@link TransactionManager}
   */
  public TransactionManager getTransactionManager(Credentials credentials) {
    if (credentials == null) {
      return new ReadonlyTransactionManager(getWeb3j(), Address.DEFAULT.toString());
    } else {
      return new FastRawTransactionManager(getWeb3j(), credentials, new NoOpProcessor(getWeb3j()));
    }
  }

  /**
   * Get transaction by hash
   * 
   * @param transactionHash transaction hash to retrieve
   * @return Web3j Transaction object
   */
  @ExoWalletStatistic(service = "blockchain", local = false, operation = OPERATION_GET_TRANSACTION)
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
  @ExoWalletStatistic(service = "blockchain", local = false, operation = OPERATION_GET_TRANSACTION_RECEIPT)
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
  @ExoWalletStatistic(service = "blockchain", local = false, operation = OPERATION_GET_LAST_BLOCK_NUMBER)
  public long getLastestBlockNumber() throws IOException {
    BigInteger blockNumber = getWeb3j().ethBlockNumber().send().getBlockNumber();
    return blockNumber.longValue();
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
  @ExoWalletStatistic(service = "blockchain", local = false, operation = OPERATION_FILTER_CONTRACT_TRANSACTIONS)
  public Set<String> getContractTransactions(String contractsAddress,
                                             long fromBlock,
                                             long toBlock) throws IOException {
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

  @Override
  public Map<String, Object> getStatisticParameters(String statisticType, Object result, Object... methodArgs) {
    Map<String, Object> parameters = new HashMap<>();

    GlobalSettings settings = getSettings();
    if (settings != null && settings.getNetwork() != null
        && StringUtils.isNotBlank(settings.getNetwork().getWebsocketProviderURL())) {
      parameters.put("blockchain_network_id", settings.getNetwork().getId());
      String websocketProviderURL = settings.getNetwork().getWebsocketProviderURL();
      String[] urlParts = websocketProviderURL.split("/");
      parameters.put("blockchain_network_url_suffix", urlParts[urlParts.length - 1]);
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
    default:
      LOG.warn("Statistic type {} not managed", statisticType);
      return null;
    }
    return parameters;
  }

  /**
   * Retruns current nonce to use for next transaction to use on blockchain for
   * a selected wallet address
   * 
   * @param walletAddress wallet adres to determine its next nonce
   * @return next transaction nonce
   * @throws IOException if an I/O problem happens when connecting to blockchain
   */
  @ExoWalletStatistic(service = "blockchain", local = false, operation = OPERATION_GET_TRANSACTION_COUNT)
  public BigInteger getNonce(String walletAddress) throws IOException {
    return getWeb3j().ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                     .send()
                     .getTransactionCount();
  }

  public Web3j getWeb3j() {
    this.waitConnection();
    return web3j;
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
      throw new IllegalStateException("An error is thrown while waiting for connection on blockchain", e);
    }
  }

  private boolean connect() throws Exception { // NOSONAR
    if (this.connectionInProgress) {
      LOG.debug("Web3 connection is in progress");
      return false;
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
        web3j = Web3j.build(web3jService);
        LOG.info("Connection established to Ethereum network endpoint {}", websocketURL);
        return true;
      }
    } finally {
      this.connectionInProgress = false;
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

}
