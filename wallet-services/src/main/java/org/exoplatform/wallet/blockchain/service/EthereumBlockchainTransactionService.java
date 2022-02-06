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

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response.Error;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.Contract;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.*;


public class EthereumBlockchainTransactionService implements BlockchainTransactionService, Startable {

  private static final Log                 LOG                         =
                                               ExoLogger.getLogger(EthereumBlockchainTransactionService.class);

  private static final String              TRANSFER_SIG                = EventEncoder.encode(MeedsToken.TRANSFER_EVENT);

  private static final String              APPROVAL_SIG                = EventEncoder.encode(MeedsToken.APPROVAL_EVENT);

  private static final String              TRANSFER_OWNERSHIP_SIG      =
                                                                  EventEncoder.encode(MeedsToken.OWNERSHIPTRANSFERRED_EVENT);

  private static final Map<String, String> CONTRACT_METHODS_BY_SIG     = new HashMap<>();

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
                                              WalletAccountService accountService) {
    this.settingService = settingService;
    this.ethereumClientConnector = ethereumClientConnector;
    this.transactionService = transactionService;
    this.accountService = accountService;
  }

  @Override
  public void start() {
    try {
      networkId = getNetworkId();
      if (ethereumClientConnector.isPermanentlyScanBlockchain()) {
        startWatchingBlockchain(getLastWatchedBlockNumber());
      }
    } catch (Exception e) {
      LOG.error("Error while getting latest block number from blockchain with network id: {}", networkId, e);
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public void checkPendingTransactions() {
    List<TransactionDetail> pendingTransactions = transactionService.getPendingTransactions();

    if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
      LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database",
                pendingTransactions.size());

      startWatchingBlockchain(getLastWatchedBlockNumber());

      for (TransactionDetail pendingTransactionDetail : pendingTransactions) {
        try { // NOSONAR
          checkTransactionStatusOnBlockchain(pendingTransactionDetail.getHash(), true);
        } catch (Exception e) {
          LOG.warn("Error treating pending transaction: {}", pendingTransactionDetail, e);
        }
      }
    } else if (!ethereumClientConnector.isPermanentlyScanBlockchain()) {
      stopWatchingBlockchain();
    }
  }

  @Override
  public void scanNewerBlocks() throws IOException {
    long lastEthereumBlockNumber = ethereumClientConnector.getLastestBlockNumber();
    long lastWatchedBlockNumber = getLastWatchedBlockNumber();
    if (lastEthereumBlockNumber <= lastWatchedBlockNumber) {
      LOG.debug("No new blocks to verify. last watched = {}. last blockchain block = {}",
                lastWatchedBlockNumber,
                lastEthereumBlockNumber);
      return;
    }

    String contractAddress = getContractAddress();
    Set<String> transactionHashes = ethereumClientConnector.getContractTransactions(contractAddress,
                                                                                    lastWatchedBlockNumber,
                                                                                    lastEthereumBlockNumber);

    LOG.debug("{} transactions has been found on contract {} between block {} and {}",
              transactionHashes.size(),
              contractAddress,
              lastWatchedBlockNumber,
              lastEthereumBlockNumber);

    boolean processed = true;
    for (String transactionHash : transactionHashes) {
      TransactionDetail transaction = transactionService.getTransactionByHash(transactionHash);
      if (transaction != null && transaction.isSucceeded()) {
        continue;
      }

      try {
        checkTransactionStatusOnBlockchain(transactionHash, false);
      } catch (Exception e) {
        LOG.warn("Error processing transaction with hash: {}", transactionHash, e);
        processed = false;
      }
    }
    if (processed) {
      // Save last verified block for contracts transactions
      saveLastWatchedBlockNumber(lastEthereumBlockNumber);
    }
  }

  @Override
  public void sendRawTransactions() {
    List<TransactionDetail> transactions = transactionService.getTransactionsToSend();
    Set<String> walletAddressesWithTransactionsSent = new HashSet<>();
    for (TransactionDetail transactionDetail : transactions) {
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

        getListenerService().broadcast(TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT, transactionDetail, transactionDetail);
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
  public void checkPendingTransactionValidity(TransactionDetail transactionDetail) {
    if (transactionDetail == null || !transactionDetail.isPending()) {
      return;
    }
    long pendingTransactionMaxDays = transactionService.getPendingTransactionMaxDays();

    long maxAttemptsToSend = transactionService.getMaxAttemptsToSend();

    String transactionHash = transactionDetail.getHash();
    long sentTimestamp = transactionDetail.getRawTransaction() == null || transactionDetail.getSendingAttemptCount() == 0 ? transactionDetail.getTimestamp()
                                                                       : transactionDetail.getSentTimestamp();
    Duration duration = Duration.ofMillis(System.currentTimeMillis() - sentTimestamp);

    boolean transactionSendingTimedOut = pendingTransactionMaxDays > 0 && duration.toDays() >= pendingTransactionMaxDays;
    if (transactionSendingTimedOut) {
      // Verify if transaction reached blockchain successfully, if reached, we
      // will have to wait its mining status
      Transaction transaction = ethereumClientConnector.getTransaction(transactionHash);
      if (transaction == null) {
        // Transaction Not found on blockchain for more than max waiting
        // days, marking transaction as failed in internal database if it's
        // not sent using a raw transaction stored in internal database or
        // if the sending attempts has reached the maximum allowed attempts
        if (transactionDetail.getRawTransaction() == null) {
          transactionDetail.setPending(false);
          // Mark transaction as failed, ContractTransactionVerifierJob will
          // remake it as success if it detects it when it would be mined
          transactionDetail.setSucceeded(false);
          // Cancel usage of transaction nonce to be able to reuse it if
          // transaction is not arrived really on blockchain (tx can be
          // purged
          // from MemPool of miners when there are too many pending
          // transaction: could happen on mainnet only)
          transactionDetail.setNonce(0);
          LOG.info("Transaction '{}' was NOT FOUND on blockchain for more than '{}' days, so mark it as failed",
                   transactionHash,
                   pendingTransactionMaxDays);
          transactionService.saveTransactionDetail(transactionDetail, true);
          return;
        } else if (transactionDetail.getSendingAttemptCount() <= maxAttemptsToSend) {
          // Raw transaction was sent to blockchain, but it seems that it
          // wasn't sent successfully, thus it will be sent again
          LOG.info("Transaction '{}' was NOT FOUND on blockchain for more than '{}' days, it will be resent again",
                   transactionHash,
                   duration.toDays());

          transactionDetail.setSentTimestamp(0);
          transactionService.saveTransactionDetail(transactionDetail, false);
          return;
        } else if(transactionDetail.getSendingAttemptCount() > maxAttemptsToSend) {
          // We need to cancel transaction in database, to make it possible to resend it
          transactionDetail.setPending(false);
          transactionDetail.setSucceeded(false);
          transactionService.saveTransactionDetail(transactionDetail, false);
        }
      } else {
        LOG.info("Transaction '{}' was FOUND on blockchain for more than '{}' days, so avoid marking it as failed",
                 transactionHash,
                 pendingTransactionMaxDays);
        checkTransactionStatusOnBlockchain(transactionHash, transaction, true);
        return;
      }
    }

    if (transactionDetail.isPending() && transactionDetail.getSentTimestamp() == 0
        && transactionDetail.getSendingAttemptCount() > maxAttemptsToSend) {
      transactionDetail.setPending(false);
      transactionDetail.setSucceeded(false);
      // Cancel usage of transaction nonce to be able to reuse it if
      // transaction is not arrived really on blockchain (tx can be
      // purged from MemPool of miners when there are too many pending
      // transaction: could happen on mainnet only)
      transactionDetail.setNonce(0);
      LOG.info("Transaction '{}' was NOT FOUND on blockchain after attempting sending it for '{}' times, so mark it as failed",
               transactionHash,
               pendingTransactionMaxDays);
      transactionService.saveTransactionDetail(transactionDetail, true);
      return;
    }

    if (transactionDetail.isPending()) {
      long nonce = transactionDetail.getNonce();
      BigInteger lastMinedTransactionNonce = null;
      try {
        lastMinedTransactionNonce = ethereumClientConnector.getNonce(transactionDetail.getFrom(),
                                                                     DefaultBlockParameterName.LATEST);
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

  @Override
  public void checkTransactionStatusOnBlockchain(String transactionHash, boolean pendingTransactionFromDatabase) {
    Transaction transaction = ethereumClientConnector.getTransaction(transactionHash);
    checkTransactionStatusOnBlockchain(transactionHash, transaction, pendingTransactionFromDatabase);
  }

  @Override
  public TransactionDetail refreshTransactionFromBlockchain(String hash) {
    TransactionDetail transactionDetail = transactionService.getTransactionByHash(hash);
    if (transactionDetail == null) {
      return null;
    }
    checkTransactionStatusOnBlockchain(hash, true);
    return transactionService.getTransactionByHash(hash);
  }

  @Override
  public long refreshBlockchainGasPrice() throws IOException {
    return ethereumClientConnector.getGasPrice().longValue();
  }

  private void stopWatchingBlockchain() {
    ethereumClientConnector.stopListeningToBlockchain();
  }

  private void startWatchingBlockchain(long lastWatchedBlockNumber) {
    try {
      if (!ethereumClientConnector.isListeningToBlockchain()) {
        if (lastWatchedBlockNumber <= 0) {
          lastWatchedBlockNumber = ethereumClientConnector.getLastestBlockNumber();
          saveLastWatchedBlockNumber(lastWatchedBlockNumber);
        }
        ethereumClientConnector.renewBlockSubscription(lastWatchedBlockNumber + 1);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error watching blockchain starting from block " + lastWatchedBlockNumber, e);
    }
  }

  private void checkTransactionStatusOnBlockchain(String transactionHash,
                                                  Transaction transaction,
                                                  boolean pendingTransactionFromDatabase) {
    if (transaction == null) {
      if (pendingTransactionFromDatabase) {
        TransactionDetail transactionDetail = transactionService.getPendingTransactionByHash(transactionHash);
        if (transactionDetail == null) {
          throw new IllegalStateException("Transaction with hash " + transactionHash
              + " wasn't found in internal database while it should have been retrieved from it.");
        }
        LOG.debug("Transaction {} is marked as pending in database and is not yet found on blockchain", transactionHash);
        checkPendingTransactionValidity(transactionDetail);
      } else {
        throw new IllegalStateException("Transaction with hash " + transactionHash
            + " is not marked as pending but the transaction wasn't found on blockchain");
      }
      return;
    } else {
      String blockHash = transaction.getBlockHash();
      if (StringUtils.isBlank(blockHash) || StringUtils.equalsIgnoreCase(EMPTY_HASH, blockHash)
          || transaction.getBlockNumber() == null) {
        if (!pendingTransactionFromDatabase) {
          throw new IllegalStateException("Transaction " + transactionHash
              + " is marked as pending in blockchain while it's not marked as pending in database");
        }
        LOG.debug("Transaction {} is marked as pending in database and is always pending on blockchain", transactionHash);
        return;
      }
    }

    ContractDetail contractDetail = getContractDetail();
    if (contractDetail == null) {
      throw new IllegalStateException("Principal contract detail wasn't found in database");
    }

    TransactionDetail transactionDetail = transactionService.getTransactionByHash(transactionHash);
    if (transactionDetail == null) {
      if (pendingTransactionFromDatabase) {
        throw new IllegalStateException("Transaction with hash " + transactionHash
            + " wasn't found in internal database while it should have been retrieved from it.");
      }

      String contractAddress = transaction.getTo();
      if (!StringUtils.equalsIgnoreCase(contractDetail.getAddress(), contractAddress)) {
        LOG.debug("Transaction '{}' is not a contract transaction, thus it will not be added into database", transactionHash);
        return;
      }

      transactionDetail = new TransactionDetail();
      transactionDetail.setNetworkId(networkId);
      transactionDetail.setHash(transactionHash);
      transactionDetail.setPending(true);
    }

    TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(transactionHash);
    if (transactionReceipt == null) {
      throw new IllegalStateException("Couldn't find transaction receipt with hash '" + transactionHash + "' on blockchain");
    }

    // When transaction status in database is different from blockchain
    // transaction status
    boolean broadcastMinedTransaction = transactionDetail.isPending()
        || (transactionDetail.isSucceeded() != transactionReceipt.isStatusOK());

    if (pendingTransactionFromDatabase && !transactionDetail.isPending()) {
      long transactionsWithSameNonce = transactionService.countTransactionsByNonce(transactionDetail);
      if(transactionsWithSameNonce > 1) {
        // Transaction was replaced by another one which was succeeded, let's mark the latter with canceled
        transactionService.cancelTransactionsWithSameNonce(transactionDetail);
      } else {
        LOG.debug("Transaction '{}' seems to be already marked as not pending, skip processing it", transactionHash);
        return;
      }
    }

    computeTransactionDetail(transactionDetail, contractDetail, transaction, transactionReceipt);

    if (pendingTransactionFromDatabase) {
      // Only save modifications if it's coming from database
      transactionService.saveTransactionDetail(transactionDetail, broadcastMinedTransaction);
    } else {
      // Compute wallets
      if (StringUtils.isNotBlank(transactionDetail.getFrom()) && isWalletEmpty(transactionDetail.getFromWallet())) {
        transactionDetail.setFromWallet(accountService.getWalletByAddress(transactionDetail.getFrom()));
      }
      if (StringUtils.isNotBlank(transactionDetail.getTo()) && isWalletEmpty(transactionDetail.getToWallet())) {
        transactionDetail.setToWallet(accountService.getWalletByAddress(transactionDetail.getTo()));
      }
      if (StringUtils.isNotBlank(transactionDetail.getBy()) && isWalletEmpty(transactionDetail.getByWallet())) {
        transactionDetail.setByWallet(accountService.getWalletByAddress(transactionDetail.getBy()));
      }

      // Check if it has a know wallet from internal database before saving
      if (hasKnownWalletInTransaction(transactionDetail)) {
        transactionService.saveTransactionDetail(transactionDetail, broadcastMinedTransaction);
      } else if (transactionService.isLogAllTransaction()) {
        logStatistics(transactionDetail);
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
        String exceptionMessage = exception == null ? "No result returned"
                                                    : exception.getMessage();
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

  private long getLastWatchedBlockNumber() {
    SettingValue<?> lastBlockNumberValue = settingService.get(WALLET_CONTEXT,
                                                              WALLET_SCOPE,
                                                              LAST_BLOCK_NUMBER_KEY_NAME + networkId);
    if (lastBlockNumberValue != null && lastBlockNumberValue.getValue() != null) {
      return Long.parseLong(lastBlockNumberValue.getValue().toString());
    }
    return 0;
  }

  private void saveLastWatchedBlockNumber(long lastWatchedBlockNumber) {
    LOG.debug("Save watched block number {} on network {}", lastWatchedBlockNumber, networkId);
    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       LAST_BLOCK_NUMBER_KEY_NAME + networkId,
                       SettingValue.create(lastWatchedBlockNumber));
  }

  private String getTransactionErrorMessage(Error transactionError) {
    if (transactionError == null) {
      return null;
    }
    return "Code: " + transactionError.getCode() + ", Message: " + transactionError.getMessage() + ", Data: "
        + transactionError.getData();
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
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
