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

import static org.exoplatform.wallet.utils.WalletUtils.EMPTY_HASH;
import static org.exoplatform.wallet.utils.WalletUtils.ETHER_TO_WEI_DECIMALS;
import static org.exoplatform.wallet.utils.WalletUtils.LAST_BLOCK_NUMBER_KEY_NAME;
import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_CONTEXT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_SCOPE;
import static org.exoplatform.wallet.utils.WalletUtils.getContractDetail;
import static org.exoplatform.wallet.utils.WalletUtils.getNetworkId;
import static org.exoplatform.wallet.utils.WalletUtils.isWalletEmpty;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.javascript.jscomp.jarjar.com.google.re2j.Pattern;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.ContractTransactionEvent;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletService;
import org.exoplatform.wallet.service.WalletTransactionService;

public class EthereumBlockchainTransactionService implements BlockchainTransactionService, Startable {

  private static final Log                 LOG                               =
                                               ExoLogger.getLogger(EthereumBlockchainTransactionService.class);

  private static final Pattern             GAS_PRICE_TOO_LOW_MESSAGE_PATTERN = Pattern.compile("transaction gas price.*too low");

  private static final Pattern             NONCE_TOO_LOW_MESSAGE_PATTERN     = Pattern.compile("nonce (is )?too low");

  private static final String              TRANSFER_SIG                      = EventEncoder.encode(MeedsToken.TRANSFER_EVENT);

  private static final Map<String, String> CONTRACT_METHODS_BY_SIG           = new HashMap<>();

  static {
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_SIG, MeedsToken.FUNC_TRANSFER);
  }

  private PortalContainer          container;

  private EthereumClientConnector  ethereumClientConnector;

  private WalletAccountService     accountService;

  private WalletTransactionService transactionService;

  private SettingService           settingService;

  private ListenerService          listenerService;

  private long                     networkId;

  private Queue<TransactionDetail> transactionDetailsToRefresh = new PriorityBlockingQueue<>(1, this::compareTransactionDate);

  private ScheduledExecutorService transactionRefreshExecutor  = null;

  public EthereumBlockchainTransactionService(PortalContainer container,
                                              WalletService walletService, // NOSONAR
                                                                           // added
                                                                           // for
                                                                           // Dependency
                                                                           // injection
                                              SettingService settingService,
                                              EthereumClientConnector ethereumClientConnector,
                                              WalletTransactionService transactionService,
                                              WalletAccountService accountService,
                                              ListenerService listenerService) {
    this.container = container;
    this.settingService = settingService;
    this.ethereumClientConnector = ethereumClientConnector;
    this.transactionService = transactionService;
    this.accountService = accountService;
    this.listenerService = listenerService;

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-transaction-refresh-%d").build();
    transactionRefreshExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
  }

  @Override
  public void start() {
    networkId = getNetworkId();
    PortalContainer.addInitTask(container.getPortalContext(), new RootContainer.PortalContainerPostInitTask() {
      @Override
      public void execute(ServletContext context, PortalContainer portalContainer) {
        CompletableFuture.runAsync(EthereumBlockchainTransactionService.this::startAsync);
        transactionRefreshExecutor.scheduleWithFixedDelay(EthereumBlockchainTransactionService.this::processTransactionRefreshingFromBlockchain,
                                                          0,
                                                          ethereumClientConnector.getPollingInterval(),
                                                          TimeUnit.MILLISECONDS);
      }
    });
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public List<TransactionDetail> sendPendingTransactionsToBlockchain() {
    List<TransactionDetail> transactionsToSend = transactionService.getTransactionsToSend();
    if (CollectionUtils.isEmpty(transactionsToSend)) {
      return Collections.emptyList();
    }
    long startTime = System.currentTimeMillis();
    LOG.info("Start sending {} transactions to blockchain", transactionsToSend.size());
    List<TransactionDetail> sentTransactions = new ArrayList<>();
    try {
      for (TransactionDetail transactionDetail : transactionsToSend) {
        TransactionDetail trantactionDetailSent = this.sendTransactionToBlockchain(transactionDetail);
        if (trantactionDetailSent != null && trantactionDetailSent.isPending() && trantactionDetailSent.getSentTimestamp() > 0) {
          sentTransactions.add(trantactionDetailSent);
        }
      }
      return sentTransactions;
    } finally {
      LOG.info("End sending {}/{} pending transactions to blockchain in {}ms",
               sentTransactions.size(),
               transactionsToSend.size(),
               System.currentTimeMillis() - startTime);
    }
  }

  @Override
  public void addTransactionToRefreshFromBlockchain(TransactionDetail transactionDetail) {
    if (transactionDetailsToRefresh.stream()
                                   .noneMatch(otherTransactionDetail -> StringUtils.equalsIgnoreCase(otherTransactionDetail.getHash(),
                                                                                                     transactionDetail.getHash()))) {
      transactionDetailsToRefresh.add(transactionDetail);
    }
  }

  @Override
  public TransactionDetail refreshTransactionFromBlockchain(String transactionHash) {
    TransactionDetail transactionDetail = transactionService.getTransactionByHash(transactionHash);
    if (transactionDetail != null && !transactionDetail.isPending() && transactionDetail.isSucceeded()) {
      // Already refreshed no need to change it
      return transactionDetail;
    }
    Transaction transaction = ethereumClientConnector.getTransaction(transactionHash);
    retrieveTransactionDetailsFromBlockchain(transactionDetail, transaction);
    if (transaction != null && transaction.getBlockNumber() != null
        && transaction.getBlockNumber().longValue() > getLastWatchedBlockNumber()) {
      long blockNumber = transaction.getBlockNumber().longValue();
      saveLastWatchedBlockNumber(blockNumber);
      ethereumClientConnector.setLastWatchedBlockNumber(blockNumber);
    }
    return transactionService.getTransactionByHash(transactionHash);
  }

  @Override
  public double getGasPrice() throws IOException {
    return ethereumClientConnector.getGasPrice().doubleValue();
  }

  @Override
  public boolean hasManagedWalletInTransaction(ContractTransactionEvent transactionEvent) {
    if (CollectionUtils.isNotEmpty(transactionEvent.getTopics())) {
      List<String> topics = transactionEvent.getTopics();
      String methodName = CONTRACT_METHODS_BY_SIG.get(topics.get(0));
      if (StringUtils.equals(methodName, MeedsToken.FUNC_TRANSFER)) {
        org.web3j.protocol.core.methods.response.Log log = new org.web3j.protocol.core.methods.response.Log();
        log.setAddress(transactionEvent.getContractAddress());
        log.setData(transactionEvent.getData());
        log.setTopics(topics);
        EventValues parameters = Contract.staticExtractEventParameters(MeedsToken.TRANSFER_EVENT, log);
        if (parameters == null) {
          return false;
        }
        String from = parameters.getIndexedValues().get(0).getValue().toString();
        String to = parameters.getIndexedValues().get(1).getValue().toString();
        if (accountService.getWalletByAddress(from) != null) {
          return true;
        }
        if (accountService.getWalletByAddress(to) != null) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public long getLastWatchedBlockNumber() {
    SettingValue<?> lastBlockNumberValue =
                                         settingService.get(WALLET_CONTEXT, WALLET_SCOPE, LAST_BLOCK_NUMBER_KEY_NAME + networkId);
    if (lastBlockNumberValue != null && lastBlockNumberValue.getValue() != null) {
      return Long.parseLong(lastBlockNumberValue.getValue().toString());
    }
    return 0;
  }

  @Override
  public void saveLastWatchedBlockNumber(long lastWatchedBlockNumber) {
    LOG.debug("Save watched block number {} on network {}", lastWatchedBlockNumber, networkId);
    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       LAST_BLOCK_NUMBER_KEY_NAME + networkId,
                       SettingValue.create(lastWatchedBlockNumber));
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Future startWatchingBlockchain() {
    long startWatchingBlockNumber = getLastWatchedBlockNumber() + 1;
    return ethereumClientConnector.renewTransactionListeningSubscription(startWatchingBlockNumber);
  }

  @Override
  public void stopWatchingBlockchain() {
    try {
      ethereumClientConnector.cancelTransactionListeningToBlockchain();
    } finally {
      saveLastWatchedBlockNumber(ethereumClientConnector.getLastWatchedBlockNumber());
    }
  }

  protected void startAsync() {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      long lastWatchedBlockNumber = getLastWatchedBlockNumber();
      long pendingContractTransactionsSent = transactionService.countContractPendingTransactionsSent();
      if (lastWatchedBlockNumber <= 0 || pendingContractTransactionsSent == 0) {
        lastWatchedBlockNumber = ethereumClientConnector.getLastestBlockNumber();
        saveLastWatchedBlockNumber(lastWatchedBlockNumber);
      }
      ethereumClientConnector.setLastWatchedBlockNumber(lastWatchedBlockNumber);
      if (ethereumClientConnector.isPermanentlyScanBlockchain() || pendingContractTransactionsSent > 0) {
        startWatchingBlockchain();
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  private void processTransactionRefreshingFromBlockchain() {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      // Limit to 10 to not exceed rate limit all time
      int limit = transactionDetailsToRefresh.size() > 10 ? 10 : transactionDetailsToRefresh.size();
      for (int i = 0; i < limit; i++) {
        TransactionDetail transactionDetail = transactionDetailsToRefresh.poll();
        String hash = transactionDetail.getHash();
        try {
          refreshTransactionFromBlockchain(hash);
        } catch (Exception e) {
          LOG.warn("Error while refreshing transaction with has {}. Retry it after few seconds.", hash, e);
          addTransactionToRefreshFromBlockchain(transactionDetail);
        }
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  private void retrieveTransactionDetailsFromBlockchain(TransactionDetail transactionDetail, // NOSONAR
                                                        Transaction transaction) {
    if (transaction == null || isTransactionPendingOnBlockchain(transaction)) {
      if (transactionDetail == null) {
        throw new IllegalStateException("Nothing to verify for a null transaction from DB and Blockchain.");
      } else {
        LOG.debug("Transaction {} is marked as pending in database and is not yet found on blockchain",
                  transactionDetail.getHash());
        checkPendingTransactionValidity(transactionDetail);
        return;
      }
    }

    ContractDetail contractDetail = getContractDetail();
    if (contractDetail == null) {
      throw new IllegalStateException("Principal contract detail wasn't found in database");
    }

    String transactionHash = transaction.getHash();
    if (transactionDetail == null) {
      String contractAddress = transaction.getTo();
      if (!StringUtils.equalsIgnoreCase(contractDetail.getAddress(), contractAddress)) {
        LOG.debug("Transaction '{}' is not a contract transaction, thus it will not be added into database", transactionHash);
        return;
      }

      transactionDetail = new TransactionDetail();
      transactionDetail.setNetworkId(networkId);
      transactionDetail.setContractAddress(contractAddress);
      transactionDetail.setHash(transactionHash);
    }

    TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(transactionHash);
    if (transactionReceipt == null) {
      throw new IllegalStateException("Couldn't find transaction receipt with hash '" + transactionHash + "' on blockchain");
    }

    // When transaction status in database is different from blockchain
    // transaction status or when it was pending before
    boolean broadcastMinedTransaction = transactionDetail.isPending()
        || (transactionDetail.isSucceeded() != transactionReceipt.isStatusOK());

    computeTransactionDetail(transactionDetail, contractDetail, transaction, transactionReceipt);

    // Compute wallets
    if (StringUtils.isNotBlank(transactionDetail.getFrom()) && isWalletEmpty(transactionDetail.getFromWallet())) {
      Wallet wallet = accountService.getWalletByAddress(transactionDetail.getFrom());
      transactionDetail.setFromWallet(wallet);
    }
    if (StringUtils.isNotBlank(transactionDetail.getTo()) && isWalletEmpty(transactionDetail.getToWallet())) {
      Wallet wallet = accountService.getWalletByAddress(transactionDetail.getTo());
      transactionDetail.setToWallet(wallet);
    }
    if (StringUtils.isNotBlank(transactionDetail.getBy()) && isWalletEmpty(transactionDetail.getByWallet())) {
      Wallet wallet = accountService.getWalletByAddress(transactionDetail.getBy());
      transactionDetail.setByWallet(wallet);
    }

    // Check if it has a know wallet from internal database before saving
    transactionService.saveTransactionDetail(transactionDetail, broadcastMinedTransaction);

    if (transactionDetail.isSucceeded()) {
      // Transaction was replaced by another one which was succeeded, let's
      // mark the latter with canceled
      transactionService.cancelTransactionsWithSameNonce(transactionDetail);
    }
  }

  private boolean isTransactionPendingOnBlockchain(Transaction transaction) {
    String blockHash = transaction.getBlockHash();
    return StringUtils.isBlank(blockHash) || StringUtils.equalsIgnoreCase(EMPTY_HASH, blockHash)
        || transaction.getBlockNumber() == null;
  }

  private boolean checkPendingTransactionValidity(TransactionDetail transactionDetail) {
    if (!transactionDetail.isPending()) {
      return false;
    }
    boolean maxSendingTentativesReached = isMaxSendingTentativesReached(transactionDetail);
    boolean markTransactionAsFailed = isTransactionTimedOut(transactionDetail);

    // Check on nonce validity only if the transaction is not to be marked as
    // failed already (due to outdated)
    // and the maximum tentatives to send is reached
    boolean checkNonceValidity = !markTransactionAsFailed && maxSendingTentativesReached;
    if (checkNonceValidity) {
      String senderAddress = transactionDetail.getFrom();
      long nonce = transactionDetail.getNonce();
      if (isSameNonceAlreadyMined(senderAddress, nonce)) {
        markTransactionAsFailed = true;
      }
    }

    if (markTransactionAsFailed) {
      // Transaction Not found on blockchain for more than max waiting
      // days, marking transaction as failed in internal database if it's
      // not sent using a raw transaction stored in internal database or
      // if the sending attempts has reached the maximum allowed attempts
      transactionDetail.setPending(false);
      // Mark transaction as failed
      transactionDetail.setSucceeded(false);
      // Cancel usage of transaction nonce to be able to reuse it if
      // transaction is not arrived really on blockchain (tx can be
      // purged from MemPool of miners when there are too many pending
      // transaction: could happen on mainnet only)
      transactionDetail.setNonce(0);
      LOG.info("Transaction '{}' was NOT FOUND on blockchain for more than '{}' days, and after '{}' tentatives to send",
               transactionDetail.getHash(),
               transactionService.getPendingTransactionMaxDays(),
               transactionDetail.getSendingAttemptCount());
      transactionService.saveTransactionDetail(transactionDetail, true);
      return false;
    }
    return !maxSendingTentativesReached || transactionDetail.getSentTimestamp() == 0;
  }

  private boolean isMaxSendingTentativesReached(TransactionDetail transactionDetail) {
    // Check if there were transactions that was mined with a higher Nonce
    // Thus automatically mark this one as failed and reset its nonce
    long sendingAttemptCount = transactionDetail.getSendingAttemptCount();
    long maxAttemptsToSend = transactionService.getMaxAttemptsToSend();
    return sendingAttemptCount > 0 && sendingAttemptCount >= maxAttemptsToSend;
  }

  private boolean isTransactionTimedOut(TransactionDetail transactionDetail) {
    boolean isInternalWalletTransaction = StringUtils.isNotBlank(transactionDetail.getRawTransaction());
    boolean transactionSendingTimedOut = false;
    if (isInternalWalletTransaction) {
      long timestamp = transactionDetail.getTimestamp();
      long pendingTransactionMaxDays = transactionService.getPendingTransactionMaxDays();
      Duration duration = Duration.ofMillis(System.currentTimeMillis() - timestamp);
      transactionSendingTimedOut =
                                 timestamp > 0 && pendingTransactionMaxDays > 0 && duration.toDays() >= pendingTransactionMaxDays;
    }
    return transactionSendingTimedOut;
  }

  private boolean isSameNonceAlreadyMined(String senderAddress, long nonce) {
    try {
      BigInteger nextNonce = ethereumClientConnector.getNonce(senderAddress);
      return nextNonce != null && nonce < nextNonce.longValue();
    } catch (Exception e) {
      LOG.warn("Error retrieving last nonce of {}", senderAddress, e);
      return false;
    }
  }

  private boolean handleTransactionSendingRequest(TransactionDetail transactionDetail, // NOSONAR
                                                  EthSendTransaction transaction,
                                                  Throwable exception) {
    String transactionHash = transactionDetail.getHash();

    if (isIOException(exception)) {
      LOG.warn("IO Error when sending transaction {}", transactionHash, exception);
      // Decrement previously incremented sending tentative. The error was due
      // to network connection error, thus it shouldn't increment the send
      // tentative count
      return false;
    }
    transactionDetail.increaseSendingAttemptCount();

    if (transaction != null && transaction.getTransactionHash() != null) {
      transactionHash = transaction.getTransactionHash();
      transactionDetail.setHash(transactionHash);
      setSentTimestampIfNotSet(transactionDetail);
    }

    boolean alreadyMined = false;
    boolean sentToBlockchain = false;
    try {
      Error transactionError = transaction == null ? null : transaction.getError();
      boolean hasError = exception != null || (transactionError != null && StringUtils.isBlank(transaction.getResult()));
      if (hasError) {
        if (isAlreadySentError(transactionError)) {
          // Trigger sent to blockchain only when it's the first time sending
          sentToBlockchain = true;
          setSentTimestampIfNotSet(transactionDetail);
          // Mark transaction as really pending on blockchain
          // with a magic number = (Max * 2) to avoid resending it again
          markTransactionAsSent(transactionDetail);
        } else if (isNonceTooLow(transactionError) && hasTransactionReceipt(transactionDetail.getHash())) {
          alreadyMined = true;
          addTransactionToRefreshFromBlockchain(transactionDetail);
        } else if (isUnrecoverableError(transactionError)) {
          transactionDetail.setNonce(0);
          transactionDetail.setPending(false);
          transactionDetail.setSucceeded(false);
          LOG.warn("Error when sending transaction {} with an unrecoverable Error: [{}]. Mark it as failed.",
                   transactionDetail.getHash(),
                   getTransactionErrorMessage(transactionError));
        } else {
          logTransactionError(transactionDetail, exception, transactionError);
        }
      } else {
        sentToBlockchain = true;
      }
    } catch (Exception e) {
      LOG.warn("Error handling Transaction '{}' Sending", transactionHash, e);
    }
    // Avoid saving mined transaction to not asynchronously
    // save transaction at the same time than other asynchronous jobs which may
    // erase changes coming from Blockchain listening Job
    if (!alreadyMined) {
      // Broadcast mined only when pending status has changed
      boolean broadcastMined = !transactionDetail.isPending();
      transactionService.saveTransactionDetail(transactionDetail, broadcastMined);
    }
    return sentToBlockchain;
  }

  private void markTransactionAsSent(TransactionDetail transactionDetail) {
    transactionDetail.setSendingAttemptCount(transactionService.getMaxAttemptsToSend() * 2);
  }

  private boolean hasTransactionReceipt(String hash) {
    return ethereumClientConnector.getTransactionReceipt(hash) != null;
  }

  private void setSentTimestampIfNotSet(TransactionDetail transactionDetail) {
    if (transactionDetail.getSentTimestamp() == 0) {
      transactionDetail.setSentTimestamp(System.currentTimeMillis());
    }
  }

  private boolean isUnrecoverableError(Error transactionError) {
    String message = StringUtils.lowerCase(getTransactionErrorMessage(transactionError));
    return message != null && (StringUtils.containsAny(message,
                                                       "insufficient funds",
                                                       "base fee exceeds gas limit",
                                                       "replacement transaction underpriced",
                                                       "only replay-protected")
        || GAS_PRICE_TOO_LOW_MESSAGE_PATTERN.matcher(message).find() || isNonceTooLow(transactionError));
  }

  private boolean isAlreadySentError(Error transactionError) {
    String message = StringUtils.lowerCase(getTransactionErrorMessage(transactionError));
    return StringUtils.containsIgnoreCase(message, "already known");
  }

  private boolean isNonceTooLow(Error transactionError) {
    String message = StringUtils.lowerCase(getTransactionErrorMessage(transactionError));
    return NONCE_TOO_LOW_MESSAGE_PATTERN.matcher(message).find();
  }

  private void logTransactionError(TransactionDetail transactionDetail, Throwable exception, Error transactionError) {
    String exceptionMessage = exception == null ? "No result returned" : exception.getMessage();
    String errorMessage = transactionError == null ? exceptionMessage : getTransactionErrorMessage(transactionError);
    LOG.warn("Error when sending transaction {}. Error: [{}].", transactionDetail.getHash(), errorMessage, exception);
  }

  private void computeTransactionDetail(TransactionDetail transactionDetail,
                                        ContractDetail contractDetail,
                                        Transaction transaction,
                                        TransactionReceipt transactionReceipt) {
    transactionDetail.setFrom(transaction.getFrom());
    transactionDetail.setSucceeded(transactionReceipt.isStatusOK());
    transactionDetail.setGasUsed(transactionReceipt.getGasUsed().intValue());
    transactionDetail.setGasPrice(transaction.getGasPrice().doubleValue());
    transactionDetail.setPending(false);
    transactionDetail.setNonce(transaction.getNonce().longValue());
    if (transactionDetail.getTimestamp() <= 0) {
      transactionDetail.setTimestamp(System.currentTimeMillis());
    }

    if (transaction.getValue().compareTo(BigInteger.ZERO) >= 0) {
      BigInteger weiAmount = transaction.getValue();
      transactionDetail.setValueDecimal(weiAmount, ETHER_TO_WEI_DECIMALS);
    }

    String contractAddress = contractDetail.getAddress();
    String receiverAddress = transaction.getTo();

    boolean isContractTransaction = StringUtils.equalsIgnoreCase(contractAddress, receiverAddress);
    if (isContractTransaction) {
      transactionDetail.setContractAddress(contractAddress);
      if (transactionReceipt.isStatusOK()) {
        computeContractTransactionDetails(transactionDetail, contractDetail, transactionReceipt.getLogs());
      }
    } else {
      transactionDetail.setTo(receiverAddress);
      transactionDetail.setTokenFee(0);
      transactionDetail.setEtherFee(0);
      transactionDetail.setContractAddress(null);
      transactionDetail.setContractMethodName(null);
      transactionDetail.setContractAmount(0);
      transactionDetail.setNoContractFunds(false);
    }
  }

  private void computeContractTransactionDetails(TransactionDetail transactionDetail, // NOSONAR
                                                 ContractDetail contractDetail,
                                                 List<org.web3j.protocol.core.methods.response.Log> logs) {
    if (logs != null && !logs.isEmpty()) {
      Integer contractDecimals = contractDetail.getDecimals();

      int logsSize = logs.size();
      String hash = transactionDetail.getHash();
      LOG.debug("Retrieving information from blockchain for transaction {} with {} LOGS", hash, logsSize);
      int i = 0;
      while (i < logsSize) {
        org.web3j.protocol.core.methods.response.Log log = logs.get(i++);

        List<String> topics = log.getTopics();
        if (topics == null || topics.isEmpty()) {
          LOG.warn("Transaction {} has NO topics", hash);
          transactionDetail.setSucceeded(false);
          continue;
        }

        String topic = topics.get(0);
        LOG.debug("Treating transaction log {} with {} topics", hash, topics.size());

        String methodName = CONTRACT_METHODS_BY_SIG.get(topic);
        if (StringUtils.isNotBlank(methodName)) {
          transactionDetail.setContractAddress(contractDetail.getAddress());
          transactionDetail.setContractMethodName(methodName);
          if (StringUtils.equals(methodName, MeedsToken.FUNC_TRANSFER)) {
            EventValues parameters = Contract.staticExtractEventParameters(MeedsToken.TRANSFER_EVENT, log);
            readTransactionDetailsFromEventParameters(transactionDetail, parameters, contractDecimals);
            return;
          }
        }
      }
    }
  }

  private TransactionDetail sendTransactionToBlockchain(TransactionDetail transactionDetail) {
    if (!checkPendingTransactionValidity(transactionDetail)) {
      return null;
    }

    if (!transactionDetail.isBoost() && transactionDetail.getSendingAttemptCount() == 0
        && !transactionService.canSendTransactionToBlockchain(transactionDetail.getFrom())) {
      return null;
    }

    try {
      CompletableFuture<EthSendTransaction> future = ethereumClientConnector.sendTransactionToBlockchain(transactionDetail);
      if (future != null) {
        return future.handleAsync((ethSendTransaction, throwable) -> {
          ExoContainerContext.setCurrentContainer(container);
          RequestLifeCycle.begin(container);
          try {
            boolean sent = handleTransactionSendingRequest(transactionDetail, ethSendTransaction, throwable);
            if (sent) {
              broadcastTransactionSentToBlockchain(transactionDetail);
            }
            return transactionDetail;
          } finally {
            RequestLifeCycle.end();
          }
        }).get();
      }
    } catch (Throwable e) { // NOSONAR
      if (isIOException(e)) {
        LOG.error("IO Error while sending transaction {} to blockchain", transactionDetail, e);
        // Avoid incrementing SendingAttemptCount when it's a temporary fail
        // of sending transaction
      } else {
        LOG.error("Error while sending transaction {} to blockchain", transactionDetail, e);
        transactionDetail.increaseSendingAttemptCount();
      }
      transactionDetail.setSentTimestamp(0);
      transactionService.saveTransactionDetail(transactionDetail, false);
    }
    return null;
  }

  private void broadcastTransactionSentToBlockchain(TransactionDetail transactionDetail) {
    try {
      listenerService.broadcast(TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT, transactionDetail, transactionDetail);
    } catch (Exception e) {
      LOG.warn("Error when triggering event after transaction '{}' sent to blockchain", transactionDetail.getHash(), e);
    }
  }

  private void readTransactionDetailsFromEventParameters(TransactionDetail transactionDetail,
                                                         EventValues parameters,
                                                         Integer contractDecimals) {
    if (parameters != null) {
      String senderAddress = transactionDetail.getFrom();
      transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
      transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
      BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
      transactionDetail.setContractAmountDecimal(amount, contractDecimals);
      transactionDetail.setAdminOperation(false);
      if (StringUtils.equals(MeedsToken.FUNC_TRANSFER, transactionDetail.getContractMethodName())
          && !StringUtils.equals(senderAddress, transactionDetail.getFrom())) {
        transactionDetail.setBy(senderAddress);
        transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFERFROM);
      }
    }
  }

  private String getTransactionErrorMessage(Error transactionError) {
    if (transactionError == null) {
      return null;
    }
    return "Code: " + transactionError.getCode() + ", Message: " + transactionError.getMessage() + ", Data: "
        + transactionError.getData();
  }

  private boolean isIOException(Throwable exception) {
    if (exception == null) {
      return false;
    }
    if (exception instanceof IOException) {
      return true;
    } else if (exception.getCause() == exception) {
      return false;
    } else {
      return isIOException(exception.getCause());
    }
  }

  private int compareTransactionDate(TransactionDetail transactionDetail, TransactionDetail otherTransactionDetail) {
    return (int) (transactionDetail.getTimestamp() - otherTransactionDetail.getTimestamp());
  }

}
