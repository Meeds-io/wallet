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
import static org.exoplatform.wallet.utils.WalletUtils.hasKnownWalletInTransaction;
import static org.exoplatform.wallet.utils.WalletUtils.isWalletEmpty;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
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
import org.exoplatform.wallet.service.WalletTransactionService;

public class EthereumBlockchainTransactionService implements BlockchainTransactionService, Startable {

  private static final Log                 LOG                     =
                                               ExoLogger.getLogger(EthereumBlockchainTransactionService.class);

  private static final String              TRANSFER_SIG            = EventEncoder.encode(MeedsToken.TRANSFER_EVENT);

  private static final String              APPROVAL_SIG            = EventEncoder.encode(MeedsToken.APPROVAL_EVENT);

  private static final String              TRANSFER_OWNERSHIP_SIG  = EventEncoder.encode(MeedsToken.OWNERSHIPTRANSFERRED_EVENT);

  private static final Map<String, String> CONTRACT_METHODS_BY_SIG = new HashMap<>();

  static {
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_SIG, MeedsToken.FUNC_TRANSFER);
    CONTRACT_METHODS_BY_SIG.put(APPROVAL_SIG, MeedsToken.FUNC_APPROVE);
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_OWNERSHIP_SIG, MeedsToken.FUNC_TRANSFEROWNERSHIP);
  }

  private EthereumClientConnector  ethereumClientConnector;

  private WalletAccountService     accountService;

  private WalletTransactionService transactionService;

  private SettingService           settingService;

  private ListenerService          listenerService;

  private long                     networkId;

  public EthereumBlockchainTransactionService(SettingService settingService,
                                              EthereumClientConnector ethereumClientConnector,
                                              WalletTransactionService transactionService,
                                              WalletAccountService accountService,
                                              ListenerService listenerService) {
    this.settingService = settingService;
    this.ethereumClientConnector = ethereumClientConnector;
    this.transactionService = transactionService;
    this.accountService = accountService;
    this.listenerService = listenerService;
  }

  @Override
  public void start() {
    networkId = getNetworkId();
    long lastWatchedBlockNumber = getLastWatchedBlockNumber();
    if (lastWatchedBlockNumber <= 0) {
      lastWatchedBlockNumber = ethereumClientConnector.getLastestBlockNumber();
      saveLastWatchedBlockNumber(lastWatchedBlockNumber);
    }
    ethereumClientConnector.setLastWatchedBlockNumber(lastWatchedBlockNumber);

    if (ethereumClientConnector.isPermanentlyScanBlockchain() || transactionService.countPendingTransactions() > 0) {
      startWatchingBlockchain();
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public void sendPendingTransactionsToBlockchain() {
    List<TransactionDetail> transactions = transactionService.getTransactionsToSend();
    Set<String> walletAddressesWithTransactionsSent = new HashSet<>();
    for (TransactionDetail transactionDetail : transactions) { // NOSONAR
      String from = transactionDetail.getFrom();
      long sendingAttemptCount = transactionDetail.getSendingAttemptCount();

      if (sendingAttemptCount > transactionService.getMaxAttemptsToSend()) {
        // Mark transaction as error and stop trying sending it to blockchain
        transactionDetail.setPending(false);
        transactionDetail.setSucceeded(false);
        transactionService.saveTransactionDetail(transactionDetail, true);
        continue;
      }

      if (!transactionDetail.isBoost()
          && (walletAddressesWithTransactionsSent.contains(from) || !transactionService.canSendTransactionToBlockchain(from))) {
        continue;
      }
      walletAddressesWithTransactionsSent.add(from);

      try {
        CompletableFuture<EthSendTransaction> sendTransactionToBlockchain =
                                                                          ethereumClientConnector.sendTransactionToBlockchain(transactionDetail);
        sendTransactionToBlockchain.thenAcceptAsync(ethSendTransaction -> {
          PortalContainer container = PortalContainer.getInstance();
          ExoContainerContext.setCurrentContainer(container);
          RequestLifeCycle.begin(container);
          try {
            handleTransactionSendingRequest(transactionDetail, ethSendTransaction, null);
          } finally {
            RequestLifeCycle.end();
          }
        });
        sendTransactionToBlockchain.exceptionally(throwable -> {
          PortalContainer container = PortalContainer.getInstance();
          ExoContainerContext.setCurrentContainer(container);
          RequestLifeCycle.begin(container);
          try {
            handleTransactionSendingRequest(transactionDetail, null, throwable);
          } finally {
            RequestLifeCycle.end();
          }
          return null;
        });

        transactionDetail.setSendingAttemptCount(sendingAttemptCount + 1);
        transactionDetail.setSentTimestamp(System.currentTimeMillis());

        transactionService.saveTransactionDetail(transactionDetail, false);

        listenerService.broadcast(TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT, transactionDetail, transactionDetail);
      } catch (Throwable e) {
        if (isIOException(e)) {
          LOG.error("IO Error while sending transaction {} to blockchain", transactionDetail, e);
        } else {
          LOG.error("Error while sending transaction {} to blockchain", transactionDetail, e);

          transactionDetail.setSendingAttemptCount(sendingAttemptCount + 1);
          transactionService.saveTransactionDetail(transactionDetail, false);
        }
      }
    }
  }

  @Override
  public TransactionDetail refreshTransactionFromBlockchain(String transactionHash) {
    TransactionDetail transactionDetail = transactionService.getTransactionByHash(transactionHash);
    Transaction transaction = ethereumClientConnector.getTransaction(transactionHash);
    retrieveTransactionDetailsFromBlockchain(transactionDetail, transaction);
    return transactionService.getTransactionByHash(transactionHash);
  }

  @Override
  public long refreshBlockchainGasPrice() throws IOException {
    return ethereumClientConnector.getGasPrice().longValue();
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
  public void startWatchingBlockchain() {
    ethereumClientConnector.renewTransactionListeningSubscription(getLastWatchedBlockNumber() + 1);
  }

  @Override
  public void stopWatchingBlockchain() {
    if (!ethereumClientConnector.isPermanentlyScanBlockchain()) {
      ethereumClientConnector.stopListeningToBlockchain();
    }
    saveLastWatchedBlockNumber(ethereumClientConnector.getLastWatchedBlockNumber());
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
      transactionDetail.setHash(transactionHash);
    }

    TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(transactionHash);
    if (transactionReceipt == null) {
      throw new IllegalStateException("Couldn't find transaction receipt with hash '" + transactionHash + "' on blockchain");
    }

    // When transaction status in database is different from blockchain
    // transaction status
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
    if (hasKnownWalletInTransaction(transactionDetail)) {
      transactionService.saveTransactionDetail(transactionDetail, broadcastMinedTransaction);
    }

    if (transactionDetail.isSucceeded()) {
      long transactionsWithSameNonce = transactionService.countTransactionsByNonce(transactionDetail);
      if (transactionsWithSameNonce > 1) {
        // Transaction was replaced by another one which was succeeded, let's
        // mark the latter with canceled
        transactionService.cancelTransactionsWithSameNonce(transactionDetail);
      }
    }
  }

  private boolean isTransactionPendingOnBlockchain(Transaction transaction) {
    String blockHash = transaction.getBlockHash();
    return StringUtils.isBlank(blockHash) || StringUtils.equalsIgnoreCase(EMPTY_HASH, blockHash)
        || transaction.getBlockNumber() == null;
  }

  private void checkPendingTransactionValidity(TransactionDetail transactionDetail) {
    long pendingTransactionMaxDays = transactionService.getPendingTransactionMaxDays();
    long maxAttemptsToSend = transactionService.getMaxAttemptsToSend();
    String transactionHash = transactionDetail.getHash();
    long sentTimestamp = transactionDetail.getRawTransaction() == null
        || transactionDetail.getSendingAttemptCount() == 0 ? transactionDetail.getTimestamp()
                                                           : transactionDetail.getSentTimestamp();
    Duration duration = Duration.ofMillis(System.currentTimeMillis() - sentTimestamp);
    boolean transactionSendingTimedOut = pendingTransactionMaxDays > 0 && duration.toDays() >= pendingTransactionMaxDays;
    if (transactionSendingTimedOut) {
      // Transaction Not found on blockchain for more than max waiting
      // days, marking transaction as failed in internal database if it's
      // not sent using a raw transaction stored in internal database or
      // if the sending attempts has reached the maximum allowed attempts
      if (transactionDetail.getRawTransaction() == null) {
        transactionDetail.setPending(false);
        // Mark transaction as failed
        transactionDetail.setSucceeded(false);
        // Cancel usage of transaction nonce to be able to reuse it if
        // transaction is not arrived really on blockchain (tx can be
        // purged from MemPool of miners when there are too many pending
        // transaction: could happen on mainnet only)
        transactionDetail.setNonce(0);
        LOG.info("Transaction '{}' was NOT FOUND on blockchain for more than '{}' days, so mark it as failed",
                 transactionHash,
                 pendingTransactionMaxDays);
        transactionService.saveTransactionDetail(transactionDetail, true);
      } else if (transactionDetail.getSendingAttemptCount() <= maxAttemptsToSend) {
        // Raw transaction was sent to blockchain, but it seems that it
        // wasn't sent successfully, thus it will be sent again
        LOG.info("Transaction '{}' was NOT FOUND on blockchain for more than '{}' days, it will be resent again",
                 transactionHash,
                 duration.toDays());

        transactionDetail.setSentTimestamp(0);
        transactionService.saveTransactionDetail(transactionDetail, false);
      } else if (transactionDetail.getSendingAttemptCount() > maxAttemptsToSend) {
        // We need to cancel transaction in database, to make it possible to
        // resend it
        transactionDetail.setPending(false);
        transactionDetail.setSucceeded(false);
        transactionService.saveTransactionDetail(transactionDetail, false);
      }
    }

    if (transactionDetail.isPending() && transactionDetail.getSentTimestamp() == 0
        && transactionDetail.getSendingAttemptCount() > maxAttemptsToSend) {
      LOG.info("Mark transaction '{}' as failed since it's pending and not found in Blockchain for more than '{}' days",
               transactionHash,
               pendingTransactionMaxDays);
      transactionDetail.setPending(false);
      transactionDetail.setSucceeded(false);
      // Cancel usage of transaction nonce to be able to reuse it if
      // transaction is not arrived really on blockchain (tx can be
      // purged from MemPool of miners when there are too many pending
      // transaction: could happen on mainnet only)
      transactionDetail.setNonce(0);
      transactionService.saveTransactionDetail(transactionDetail, true);
      return;
    }

    if (transactionDetail.isPending()) {
      long nonce = transactionDetail.getNonce();
      BigInteger lastMinedTransactionNonce = null;
      try {
        lastMinedTransactionNonce =
                                  ethereumClientConnector.getNonce(transactionDetail.getFrom(), DefaultBlockParameterName.LATEST);
      } catch (IOException e) {
        LOG.warn("Error retrieving last nonce of {}", transactionDetail.getFrom(), e);
        return;
      }
      if (lastMinedTransactionNonce != null && nonce < lastMinedTransactionNonce.longValue()) {
        transactionDetail.setPending(false);
        LOG.info("Transaction '{}' was sent with same nonce as previous mined transaction, so mark it as not pending anymore",
                 transactionHash);
        transactionService.saveTransactionDetail(transactionDetail, true);
      }
    }
  }

  private boolean handleTransactionSendingRequest(final TransactionDetail transactionDetail,
                                                  EthSendTransaction transaction,
                                                  Throwable exception) {
    boolean transactionModified = false;

    String transactionHash = transactionDetail.getHash();

    LOG.debug("Handle transaction sending return: tx hash = {} , eth tx = {}, exception = {}",
              transactionHash,
              transaction,
              exception);

    if (isIOException(exception)) {
      LOG.warn("IO Error when sending transaction {}", transactionHash, exception);
      return transactionModified;
    }

    try {
      if (transaction != null && transaction.getTransactionHash() != null
          && !StringUtils.equalsIgnoreCase(transaction.getTransactionHash(), transactionHash)) {
        LOG.debug("Transaction with hash {} has been confirmed with different hash {}",
                  transactionHash,
                  transaction.getTransactionHash());
        transactionHash = transaction.getTransactionHash();
        transactionDetail.setHash(transactionHash);
        transactionModified = true;
      }

      Error transactionError = transaction == null ? null : transaction.getError();
      boolean hasError = exception != null
          || (transaction != null && (transactionError != null || StringUtils.isBlank(transaction.getResult())));
      if (hasError) {
        String exceptionMessage = exception == null ? "No result returned" : exception.getMessage();
        String errorMessage = transaction == null || transactionError == null ? exceptionMessage
                                                                              : getTransactionErrorMessage(transactionError);
        LOG.warn("Error when sending transaction {} - {}.", transactionDetail, errorMessage, exception);

        Transaction transactionFromBlockchain = ethereumClientConnector.getTransaction(transactionHash);

        // An exception occurred
        if (transactionFromBlockchain == null) {
          TransactionReceipt receipt = ethereumClientConnector.getTransactionReceipt(transactionHash);
          transactionDetail.setPending(receipt == null && transactionError == null);
          transactionDetail.setSucceeded(receipt != null && receipt.isStatusOK());
          transactionModified = true;
        }
      }
    } finally {
      if (transactionModified) {
        transactionService.saveTransactionDetail(transactionDetail, false);
      }
    }

    return transactionModified;
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
    } else {
      transactionDetail.setTo(receiverAddress);
      transactionDetail.setTokenFee(0);
      transactionDetail.setEtherFee(0);
      transactionDetail.setContractAddress(null);
      transactionDetail.setContractMethodName(null);
      transactionDetail.setContractAmount(0);
      transactionDetail.setNoContractFunds(false);
      return;
    }

    if (!transactionReceipt.isStatusOK()) {
      return;
    }

    String hash = transactionDetail.getHash();

    List<org.web3j.protocol.core.methods.response.Log> logs = transactionReceipt.getLogs();
    if (logs != null && !logs.isEmpty()) {
      Integer contractDecimals = contractDetail.getDecimals();

      int logsSize = logs.size();
      LOG.debug("Retrieving information from blockchain for transaction {} with {} LOGS", hash, logsSize);
      int i = 0;
      boolean transactionLogTreated = false;
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

        if (!transactionLogTreated) {
          String methodName = CONTRACT_METHODS_BY_SIG.get(topic);
          if (StringUtils.isBlank(methodName)) {
            continue;
          }
          transactionDetail.setContractMethodName(methodName);
          if (StringUtils.equals(methodName, MeedsToken.FUNC_TRANSFER)) {
            EventValues parameters = Contract.staticExtractEventParameters(MeedsToken.TRANSFER_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
            transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
            BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
            transactionDetail.setContractAmountDecimal(amount, contractDecimals);
            if (!StringUtils.equals(transactionReceipt.getFrom(), transactionDetail.getFrom())) {
              transactionDetail.setBy(transactionReceipt.getFrom());
              transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFERFROM);
            }
            transactionDetail.setAdminOperation(false);
          } else if (StringUtils.equals(methodName, MeedsToken.FUNC_APPROVE)) {
            transactionLogTreated = true;
            EventValues parameters = Contract.staticExtractEventParameters(MeedsToken.APPROVAL_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
            transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
            BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
            transactionDetail.setContractAmountDecimal(amount, contractDecimals);
            transactionDetail.setAdminOperation(false);
          } else if (StringUtils.equals(methodName, MeedsToken.FUNC_TRANSFEROWNERSHIP)) {
            transactionLogTreated = true;
            EventValues parameters = Contract.staticExtractEventParameters(MeedsToken.OWNERSHIPTRANSFERRED_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
            transactionDetail.setAdminOperation(true);
          } else if (!transactionLogTreated && (i + 1) == logsSize) {
            LOG.warn("Can't find contract method name of transaction {}", transactionDetail);
          }
        }
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

}
