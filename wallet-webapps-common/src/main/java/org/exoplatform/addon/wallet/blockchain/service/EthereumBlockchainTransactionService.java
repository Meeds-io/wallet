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

import static org.exoplatform.addon.wallet.contract.ERTTokenV2.*;
import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.protocol.core.methods.response.*;

import org.exoplatform.addon.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.WalletInitializationState;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumBlockchainTransactionService implements BlockchainTransactionService, Startable {

  private static final Log                 LOG                         =
                                               ExoLogger.getLogger(EthereumBlockchainTransactionService.class);

  private static final String              TRANSFER_SIG                = EventEncoder.encode(ERTTokenV2.TRANSFER_EVENT);

  private static final String              APPROVAL_SIG                = EventEncoder.encode(ERTTokenV2.APPROVAL_EVENT);

  private static final String              ADDED_ADMIN_METHOD_SIG      = EventEncoder.encode(ERTTokenV2.ADDEDADMIN_EVENT);

  private static final String              REMOVED_ADMIN_SIG           = EventEncoder.encode(ERTTokenV2.REMOVEDADMIN_EVENT);

  private static final String              APPROVED_ACCOUNT_SIG        =
                                                                EventEncoder.encode(ERTTokenV2.APPROVEDACCOUNT_EVENT);

  private static final String              DISAPPROVED_ACCOUNT_SIG     =
                                                                   EventEncoder.encode(ERTTokenV2.DISAPPROVEDACCOUNT_EVENT);

  private static final String              CONTRACT_PAUSED_SIG         =
                                                               EventEncoder.encode(ERTTokenV2.CONTRACTPAUSED_EVENT);

  private static final String              CONTRACT_UNPAUSED_SIG       =
                                                                 EventEncoder.encode(ERTTokenV2.CONTRACTUNPAUSED_EVENT);

  private static final String              DEPOSIT_RECEIVED_SIG        =
                                                                EventEncoder.encode(ERTTokenV2.DEPOSITRECEIVED_EVENT);

  private static final String              TOKEN_PRICE_CHANGED_SIG     =
                                                                   EventEncoder.encode(ERTTokenV2.TOKENPRICECHANGED_EVENT);

  private static final String              TRANSFER_OWNERSHIP_SIG      =
                                                                  EventEncoder.encode(ERTTokenV2.TRANSFEROWNERSHIP_EVENT);

  private static final String              ACCOUNT_INITIALIZATION_SIG  =
                                                                      EventEncoder.encode(ERTTokenV2.INITIALIZATION_EVENT);

  private static final String              ACCOUNT_REWARD_SIG          = EventEncoder.encode(ERTTokenV2.REWARD_EVENT);

  private static final String              ACCOUNT_VESTED_SIG          = EventEncoder.encode(ERTTokenV2.VESTING_EVENT);

  private static final String              TRANSFER_VESTING_SIG        =
                                                                EventEncoder.encode(ERTTokenV2.VESTINGTRANSFER_EVENT);

  private static final String              UPGRADED_SIG                = EventEncoder.encode(ERTTokenV2.UPGRADED_EVENT);

  private static final String              DATA_UPGRADED_SIG           = EventEncoder.encode(ERTTokenV2.UPGRADEDDATA_EVENT);

  private static final String              NOSUFFICIENTFUND_EVENT_HASH =
                                                                       EventEncoder.encode(ERTTokenV2.NOSUFFICIENTFUND_EVENT);

  private static final String              TRANSACTIONFEE_EVENT_HASH   =
                                                                     EventEncoder.encode(ERTTokenV2.TRANSACTIONFEE_EVENT);

  private static final Map<String, String> CONTRACT_METHODS_BY_SIG     = new HashMap<>();

  static {
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_SIG, FUNC_TRANSFER);
    CONTRACT_METHODS_BY_SIG.put(APPROVAL_SIG, FUNC_APPROVE);
    CONTRACT_METHODS_BY_SIG.put(ADDED_ADMIN_METHOD_SIG, FUNC_ADDADMIN);
    CONTRACT_METHODS_BY_SIG.put(REMOVED_ADMIN_SIG, FUNC_REMOVEADMIN);
    CONTRACT_METHODS_BY_SIG.put(APPROVED_ACCOUNT_SIG, FUNC_APPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(DISAPPROVED_ACCOUNT_SIG, FUNC_DISAPPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(CONTRACT_PAUSED_SIG, FUNC_PAUSE);
    CONTRACT_METHODS_BY_SIG.put(CONTRACT_UNPAUSED_SIG, FUNC_UNPAUSE);
    CONTRACT_METHODS_BY_SIG.put(DEPOSIT_RECEIVED_SIG, TOKEN_FUNC_DEPOSIT_FUNDS);
    CONTRACT_METHODS_BY_SIG.put(TOKEN_PRICE_CHANGED_SIG, FUNC_SETSELLPRICE);
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_OWNERSHIP_SIG, FUNC_TRANSFEROWNERSHIP);
    CONTRACT_METHODS_BY_SIG.put(ACCOUNT_INITIALIZATION_SIG, FUNC_INITIALIZEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(ACCOUNT_REWARD_SIG, FUNC_REWARD);
    CONTRACT_METHODS_BY_SIG.put(ACCOUNT_VESTED_SIG, FUNC_TRANSFORMTOVESTED);
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_VESTING_SIG, FUNC_TRANSFER);
    CONTRACT_METHODS_BY_SIG.put(UPGRADED_SIG, FUNC_UPGRADEIMPLEMENTATION);
    CONTRACT_METHODS_BY_SIG.put(DATA_UPGRADED_SIG, FUNC_UPGRADEDATA);
  }

  private EthereumClientConnector  ethereumClientConnector;

  private WalletAccountService     accountService;

  private WalletTransactionService transactionService;

  private SettingService           settingService;

  public EthereumBlockchainTransactionService(EthereumClientConnector ethereumClientConnector) {
    this.ethereumClientConnector = ethereumClientConnector;
  }

  @Override
  public void start() {
    try {
      long lastWatchedBlockNumber = getLastWatchedBlockNumber();
      if (lastWatchedBlockNumber <= 0) {
        lastWatchedBlockNumber = ethereumClientConnector.getLastestBlockNumber();
        saveLastWatchedBlockNumber(lastWatchedBlockNumber);
      }
      ethereumClientConnector.renewBlockSubscription(lastWatchedBlockNumber + 1);
    } catch (Exception e) {
      LOG.error("Error while getting latest block number from blockchain with network id: {}", getNetworkId(), e);
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public void checkPendingTransactions(long pendingTransactionMaxDays) {
    List<TransactionDetail> pendingTransactions = getTransactionService().getPendingTransactions();
    if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
      LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database",
                pendingTransactions.size());

      for (TransactionDetail pendingTransactionDetail : pendingTransactions) {
        try { // NOSONAR
          checkTransactionStatusOnBlockchain(pendingTransactionDetail.getHash(), true);
        } catch (Exception e) {
          LOG.warn("Error treating pending transaction: {}", pendingTransactionDetail, e);
        }
      }
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
      TransactionDetail transaction = getTransactionService().getTransactionByHash(transactionHash);
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
    List<TransactionDetail> transactions = getTransactionService().getTransactionsToSend();
    Set<String> walletAddressesWithTransactionsSent = new HashSet<>();
    for (TransactionDetail transactionDetail : transactions) {
      String from = transactionDetail.getFrom();
      if (transactionDetail.getSendingAttemptCount() > getTransactionService().getMaxAttemptsToSend()) {
        // Mark transaction as error and stop trying sending it to blockchain
        transactionDetail.setPending(false);
        transactionDetail.setSucceeded(false);
        getTransactionService().saveTransactionDetail(transactionDetail, true);
        continue;
      }

      if (walletAddressesWithTransactionsSent.contains(from) || !getTransactionService().canSendTransactionToBlockchain(from)) {
        continue;
      }
      walletAddressesWithTransactionsSent.add(from);

      try {
        ethereumClientConnector.sendTransactionToBlockchain(transactionDetail)
                               .whenComplete(handleTransactionSending(transactionDetail));
        transactionDetail.setSentTimestamp(System.currentTimeMillis());
      } catch (IOException e) {
        LOG.error("Error while sending transaction {} to blockchain", transactionDetail, e);
      }
      transactionDetail.setSendingAttemptCount(transactionDetail.getSendingAttemptCount() + 1);
      getTransactionService().saveTransactionDetail(transactionDetail, false);
    }
  }

  @Override
  public void checkPendingTransactionValidity(TransactionDetail transactionDetail) {
    if (transactionDetail == null || !transactionDetail.isPending()) {
      return;
    }
    long pendingTransactionMaxDays = getTransactionService().getPendingTransactionMaxDays();
    if (pendingTransactionMaxDays > 0) {
      long creationTimestamp = transactionDetail.getTimestamp();
      if (creationTimestamp > 0) {
        Duration duration = Duration.ofMillis(System.currentTimeMillis() - creationTimestamp);
        if (duration.toDays() >= pendingTransactionMaxDays) {
          String transactionHash = transactionDetail.getHash();

          Transaction transaction = ethereumClientConnector.getTransaction(transactionHash);
          if (transaction == null) {// Transaction Not found on blockchain for
                                    // more than max waiting days, marking
                                    // transaction as failed in internal
                                    // database
            transactionDetail.setPending(false);
            // Mark transaction as failed, ContractTransactionVerifierJob will
            // remake it as success if it detects it when it would be mined
            transactionDetail.setSucceeded(false);
            // Cancel usage of transaction nonce to be able to reuse it if
            // transaction is not arrived really on blockchain (tx can be purged
            // from MemPool of miners when there are too many pending
            // transaction: could happen on mainnet only)
            transactionDetail.setNonce(0);
            LOG.debug("Transaction '{}' was NOT FOUND on blockchain for more than '{}' days, so mark it as failed",
                      transactionHash,
                      pendingTransactionMaxDays);
            getTransactionService().saveTransactionDetail(transactionDetail, true);
          } else {
            LOG.debug("Transaction '{}' was FOUND on blockchain for more than '{}' days, so avoid marking it as failed",
                      transactionHash,
                      pendingTransactionMaxDays);
            checkTransactionStatusOnBlockchain(transactionHash, transaction, true);
          }
        }
      }
    }
  }

  @Override
  public void checkTransactionStatusOnBlockchain(String transactionHash, boolean pendingTransactionFromDatabase) {
    Transaction transaction = ethereumClientConnector.getTransaction(transactionHash);
    checkTransactionStatusOnBlockchain(transactionHash, transaction, pendingTransactionFromDatabase);
  }

  @Override
  public long refreshBlockchainGasPrice() throws IOException {
    return ethereumClientConnector.getGasPrice().longValue();
  }

  private void checkTransactionStatusOnBlockchain(String transactionHash,
                                                  Transaction transaction,
                                                  boolean pendingTransactionFromDatabase) {
    if (transaction == null) {
      if (pendingTransactionFromDatabase) {
        TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
        if (transactionDetail == null) {
          throw new IllegalStateException("Transaction with hash " + transactionHash
              + " wasn't found in internal database while it should have been retrieved from it.");
        }
        LOG.debug("Transaction {} is marked as pending in database and is not yet found on blockchain", transactionHash);
        if (transactionDetail.getRawTransaction() == null) {
          checkPendingTransactionValidity(transactionDetail);
        }
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

    TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
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
      transactionDetail.setNetworkId(getNetworkId());
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
      LOG.debug("Transaction '{}' seems to be already marked as not pending, skip processing it", transactionHash);
      return;
    }

    computeTransactionDetail(transactionDetail, contractDetail, transaction, transactionReceipt);

    if (pendingTransactionFromDatabase) {
      // Only save modifications if it's coming from database
      getTransactionService().saveTransactionDetail(transactionDetail, broadcastMinedTransaction);
    } else {
      // Compute wallets
      if (StringUtils.isNotBlank(transactionDetail.getFrom()) && isWalletEmpty(transactionDetail.getFromWallet())) {
        transactionDetail.setFromWallet(getAccountService().getWalletByAddress(transactionDetail.getFrom()));
      }
      if (StringUtils.isNotBlank(transactionDetail.getTo()) && isWalletEmpty(transactionDetail.getToWallet())) {
        transactionDetail.setToWallet(getAccountService().getWalletByAddress(transactionDetail.getTo()));
      }
      if (StringUtils.isNotBlank(transactionDetail.getBy()) && isWalletEmpty(transactionDetail.getByWallet())) {
        transactionDetail.setByWallet(getAccountService().getWalletByAddress(transactionDetail.getBy()));
      }

      // Check if it has a know wallet from internal database before saving
      if (hasKnownWalletInTransaction(transactionDetail)) {
        getTransactionService().saveTransactionDetail(transactionDetail, broadcastMinedTransaction);
      } else if (getTransactionService().isLogAllTransaction()) {
        logStatistics(transactionDetail);
      }
    }
  }

  private BiConsumer<? super EthSendTransaction, ? super Throwable> handleTransactionSending(final TransactionDetail transactionDetail) {
    return (transaction, exception) -> {
      if (transaction != null && transaction.getTransactionHash() != null) {
        LOG.debug("Transaction {} has been confirmed with hash {}",
                  transactionDetail.getHash(),
                  transaction.getTransactionHash());
        transactionDetail.setHash(transaction.getTransactionHash());
      }
      // Exception occurred while sending to blockchain
      if (exception != null) {
        LOG.warn("Error when sending transaction {}.", transactionDetail, exception);
        Transaction transactionFromBlockchain = ethereumClientConnector.getTransaction(transaction.getTransactionHash());
        // An exception occurred
        if (transactionFromBlockchain == null) {
          transactionDetail.setSendingAttemptCount(transactionDetail.getSendingAttemptCount() + 1);
        } else {
          transactionDetail.setPending(false);
          transactionDetail.setSucceeded(false);
        }
      }
      getTransactionService().saveTransactionDetail(transactionDetail, false);
    };
  }

  public void computeTransactionDetail(TransactionDetail transactionDetail,
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
      if (StringUtils.equals(transactionDetail.getContractMethodName(), FUNC_INITIALIZEACCOUNT)) {
        getAccountService().setInitializationStatus(transactionDetail.getTo(), WalletInitializationState.MODIFIED);
      }
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

        if (NOSUFFICIENTFUND_EVENT_HASH.equals(topic)) {
          transactionDetail.setNoContractFunds(true);
          continue;
        } else if (TRANSACTIONFEE_EVENT_HASH.equals(topic)) {
          EventValues parameters = ERTTokenV2.staticExtractEventParameters(TRANSACTIONFEE_EVENT, log);
          BigInteger tokenFee = (BigInteger) parameters.getNonIndexedValues().get(1).getValue();
          BigInteger etherFee = (BigInteger) parameters.getNonIndexedValues().get(2).getValue();
          transactionDetail.setTokenFee(convertFromDecimals(tokenFee, contractDecimals));
          transactionDetail.setEtherFee(convertFromDecimals(etherFee, ETHER_TO_WEI_DECIMALS));
          continue;
        }

        if (!transactionLogTreated) {
          String methodName = CONTRACT_METHODS_BY_SIG.get(topic);
          if (StringUtils.isBlank(methodName)) {
            continue;
          }
          transactionDetail.setContractMethodName(methodName);
          if (StringUtils.equals(methodName, FUNC_TRANSFER)) {
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(TRANSFER_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
            transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
            BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
            transactionDetail.setContractAmountDecimal(amount, contractDecimals);
            if (!StringUtils.equals(transactionReceipt.getFrom(), transactionDetail.getFrom())) {
              transactionDetail.setBy(transactionReceipt.getFrom());
              transactionDetail.setContractMethodName(FUNC_TRANSFERFROM);
            }
            transactionDetail.setAdminOperation(false);
          } else if (StringUtils.equals(methodName, FUNC_APPROVE)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(APPROVAL_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
            transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
            BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
            transactionDetail.setContractAmountDecimal(amount, contractDecimals);
            transactionDetail.setAdminOperation(false);
          } else if (StringUtils.equals(methodName, FUNC_APPROVEACCOUNT)) {
            if (logsSize > 1) {
              // Implicit acccount approval
              continue;
            }
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(APPROVEDACCOUNT_EVENT, log);
            transactionDetail.setFrom(transactionReceipt.getFrom());
            if (parameters == null) {
              continue;
            }
            transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_DISAPPROVEACCOUNT)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(DISAPPROVEDACCOUNT_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(transactionReceipt.getFrom());
            transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_ADDADMIN)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(ADDEDADMIN_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(transactionReceipt.getFrom());
            transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
            transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(1).getValue()).longValue());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_REMOVEADMIN)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(REMOVEDADMIN_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(transactionReceipt.getFrom());
            transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_UPGRADEDATA)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(UPGRADEDDATA_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(0).getValue()).longValue());
            transactionDetail.setTo(parameters.getNonIndexedValues().get(1).getValue().toString());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_UPGRADEIMPLEMENTATION)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(UPGRADED_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(0).getValue()).longValue());
            transactionDetail.setTo(parameters.getNonIndexedValues().get(1).getValue().toString());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, TOKEN_FUNC_DEPOSIT_FUNDS)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(DEPOSITRECEIVED_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(parameters.getNonIndexedValues().get(0).getValue().toString());
            BigInteger weiAmount = (BigInteger) parameters.getNonIndexedValues().get(1).getValue();
            transactionDetail.setValueDecimal(weiAmount, ETHER_TO_WEI_DECIMALS);
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_SETSELLPRICE)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(TOKENPRICECHANGED_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(0).getValue()).longValue());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_TRANSFORMTOVESTED)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(VESTING_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setTo(parameters.getIndexedValues().get(0).getValue().toString());
            BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
            transactionDetail.setContractAmountDecimal(amount, contractDecimals);
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_TRANSFEROWNERSHIP)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(TRANSFEROWNERSHIP_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
            transactionDetail.setAdminOperation(true);
          } else if (StringUtils.equals(methodName, FUNC_INITIALIZEACCOUNT)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(INITIALIZATION_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
            transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
            BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
            transactionDetail.setContractAmountDecimal(amount, contractDecimals);
            BigInteger weiAmount = (BigInteger) parameters.getNonIndexedValues().get(1).getValue();
            transactionDetail.setValueDecimal(weiAmount, ETHER_TO_WEI_DECIMALS);
            transactionDetail.setAdminOperation(false);

            if (transactionDetail.isSucceeded()) {
              getAccountService().setInitializationStatus(transactionDetail.getTo(), WalletInitializationState.INITIALIZED);
            } else {
              getAccountService().setInitializationStatus(transactionDetail.getTo(), WalletInitializationState.MODIFIED);
            }
          } else if (StringUtils.equals(methodName, FUNC_REWARD)) {
            transactionLogTreated = true;
            EventValues parameters = ERTTokenV2.staticExtractEventParameters(REWARD_EVENT, log);
            if (parameters == null) {
              continue;
            }
            transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
            transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
            // Transfered tokens amount
            BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
            transactionDetail.setValueDecimal(amount, contractDecimals);
            // Reward amount
            amount = (BigInteger) parameters.getNonIndexedValues().get(1).getValue();
            transactionDetail.setContractAmountDecimal(amount, contractDecimals);
            transactionDetail.setAdminOperation(false);
          } else if (!transactionLogTreated && (i + 1) == logsSize) {
            LOG.warn("Can't find contract method name of transaction {}", transactionDetail);
          }
        }
      }
    }
  }

  private long getLastWatchedBlockNumber() {
    long networkId = getNetworkId();
    SettingValue<?> lastBlockNumberValue = getSettingService().get(WALLET_CONTEXT,
                                                                   WALLET_SCOPE,
                                                                   LAST_BLOCK_NUMBER_KEY_NAME + networkId);
    if (lastBlockNumberValue != null && lastBlockNumberValue.getValue() != null) {
      return Long.valueOf(lastBlockNumberValue.getValue().toString());
    }
    return 0;
  }

  private void saveLastWatchedBlockNumber(long lastWatchedBlockNumber) {
    long networkId = getNetworkId();

    LOG.debug("Save watched block number {} on network {}", lastWatchedBlockNumber, networkId);
    getSettingService().set(WALLET_CONTEXT,
                            WALLET_SCOPE,
                            LAST_BLOCK_NUMBER_KEY_NAME + networkId,
                            SettingValue.create(lastWatchedBlockNumber));
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = CommonsUtils.getService(SettingService.class);
    }
    return settingService;
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  private WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

}
