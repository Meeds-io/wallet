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

import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.wallet.blockchain.ExoBlockchainTransaction;
import org.exoplatform.addon.wallet.blockchain.ExoBlockchainTransactionService;
import org.exoplatform.addon.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.WalletInitializationState;
import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.service.*;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumBlockchainTransactionService implements BlockchainTransactionService, ExoBlockchainTransactionService {

  private static final Log                 LOG                        =
                                               ExoLogger.getLogger(EthereumBlockchainTransactionService.class);

  private static final String              FUNC_DEPOSIT_FUNDS         = "depositFunds";

  private static final String              TRANSFER_SIG               = EventEncoder.encode(ERTTokenV2.TRANSFER_EVENT);

  private static final String              APPROVAL_SIG               = EventEncoder.encode(ERTTokenV2.APPROVAL_EVENT);

  private static final String              ADDED_ADMIN_METHOD_SIG     = EventEncoder.encode(ERTTokenV2.ADDEDADMIN_EVENT);

  private static final String              REMOVED_ADMIN_SIG          = EventEncoder.encode(ERTTokenV2.REMOVEDADMIN_EVENT);

  private static final String              APPROVED_ACCOUNT_SIG       = EventEncoder.encode(ERTTokenV2.APPROVEDACCOUNT_EVENT);

  private static final String              DISAPPROVED_ACCOUNT_SIG    = EventEncoder.encode(ERTTokenV2.DISAPPROVEDACCOUNT_EVENT);

  private static final String              CONTRACT_PAUSED_SIG        = EventEncoder.encode(ERTTokenV2.CONTRACTPAUSED_EVENT);

  private static final String              CONTRACT_UNPAUSED_SIG      = EventEncoder.encode(ERTTokenV2.CONTRACTUNPAUSED_EVENT);

  private static final String              DEPOSIT_RECEIVED_SIG       = EventEncoder.encode(ERTTokenV2.DEPOSITRECEIVED_EVENT);

  private static final String              TOKEN_PRICE_CHANGED_SIG    = EventEncoder.encode(ERTTokenV2.TOKENPRICECHANGED_EVENT);

  private static final String              TRANSFER_OWNERSHIP_SIG     = EventEncoder.encode(ERTTokenV2.TRANSFEROWNERSHIP_EVENT);

  private static final String              ACCOUNT_INITIALIZATION_SIG = EventEncoder.encode(ERTTokenV2.INITIALIZATION_EVENT);

  private static final String              ACCOUNT_REWARD_SIG         = EventEncoder.encode(ERTTokenV2.REWARD_EVENT);

  private static final String              ACCOUNT_VESTED_SIG         = EventEncoder.encode(ERTTokenV2.VESTING_EVENT);

  private static final String              TRANSFER_VESTING_SIG       = EventEncoder.encode(ERTTokenV2.VESTINGTRANSFER_EVENT);

  private static final String              UPGRADED_SIG               = EventEncoder.encode(ERTTokenV2.UPGRADED_EVENT);

  private static final String              DATA_UPGRADED_SIG          = EventEncoder.encode(ERTTokenV2.UPGRADEDDATA_EVENT);

  private static final Map<String, String> CONTRACT_METHODS_BY_SIG    = new HashMap<>();

  static {
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_SIG, FUNC_TRANSFER);
    CONTRACT_METHODS_BY_SIG.put(APPROVAL_SIG, FUNC_APPROVE);
    CONTRACT_METHODS_BY_SIG.put(ADDED_ADMIN_METHOD_SIG, FUNC_ADDADMIN);
    CONTRACT_METHODS_BY_SIG.put(REMOVED_ADMIN_SIG, FUNC_REMOVEADMIN);
    CONTRACT_METHODS_BY_SIG.put(APPROVED_ACCOUNT_SIG, FUNC_APPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(DISAPPROVED_ACCOUNT_SIG, FUNC_DISAPPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(CONTRACT_PAUSED_SIG, FUNC_PAUSE);
    CONTRACT_METHODS_BY_SIG.put(CONTRACT_UNPAUSED_SIG, FUNC_UNPAUSE);
    CONTRACT_METHODS_BY_SIG.put(DEPOSIT_RECEIVED_SIG, FUNC_DEPOSIT_FUNDS);
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

  private WalletContractService    contractService;

  private WalletTransactionService transactionService;

  private SettingService           settingService;

  private ListenerService          listenerService;

  private ClassLoader              webappClassLoader;

  public EthereumBlockchainTransactionService(EthereumClientConnector ethereumClientConnector, ClassLoader webappClassLoader) {
    this.ethereumClientConnector = ethereumClientConnector;
    this.webappClassLoader = webappClassLoader;
  }

  @Override
  public ClassLoader getWebappClassLoader() {
    return webappClassLoader;
  }

  @Override
  @ExoBlockchainTransaction
  public int checkPendingTransactions(long pendingTransactionMaxDays) {
    List<TransactionDetail> pendingTransactions = getTransactionService().getPendingTransactions();
    int transactionsMarkedAsMined = 0;
    if (pendingTransactions != null && !pendingTransactions.isEmpty()) {
      LOG.debug("Checking on blockchain the status of {} transactions marked as pending in database",
                pendingTransactions.size());
      for (TransactionDetail pendingTransactionDetail : pendingTransactions) {
        try { // NOSONAR
          boolean transactionMined = verifyTransactionStatusOnBlockchain(pendingTransactionDetail, pendingTransactionMaxDays);
          if (transactionMined) {
            transactionsMarkedAsMined++;
          }
        } catch (Exception e) {
          LOG.warn("Error treating pending transaction: {}", pendingTransactionDetail, e);
        }
      }
    }
    return transactionsMarkedAsMined;
  }

  @Override
  @ExoBlockchainTransaction
  public void scanNewerBlocks() throws InterruptedException, IOException {
    GlobalSettings settings = getSettings();
    if (settings == null) {
      LOG.debug("Empty settings, skip contract transaction verification");
      return;
    }
    long networkId = settings.getNetwork().getId();
    if (settings.getNetwork().getId() == 0) {
      LOG.debug("Empty network id in settings, skip contract transaction verification");
      return;
    }
    String wsURL = settings.getNetwork().getWebsocketProviderURL();
    if (StringUtils.isBlank(wsURL)) {
      LOG.debug("Empty Websocket URL in settings, skip contract transaction verification");
      return;
    }
    ContractDetail contractDetail = settings.getContractDetail();
    if (contractDetail == null || StringUtils.isBlank(contractDetail.getAddress())) {
      LOG.debug("Empty token address in settings, skip contract transaction verification");
      return;
    }

    long lastEthereumBlockNumber = ethereumClientConnector.getLastestBlockNumber();
    long lastWatchedBlockNumber = getLastWatchedBlockNumber(networkId);
    if (lastEthereumBlockNumber <= lastWatchedBlockNumber) {
      LOG.debug("No new blocks to verify. last watched = {}. last blockchain block = {}",
                lastWatchedBlockNumber,
                lastEthereumBlockNumber);
      return;
    }

    boolean processed = true;
    String contractAddress = contractDetail.getAddress();
    Set<String> transactionHashes = ethereumClientConnector.getContractTransactions(contractAddress,
                                                                                    lastWatchedBlockNumber,
                                                                                    lastEthereumBlockNumber);

    LOG.debug("{} transactions has been found on contract {} between block {} and {}",
              transactionHashes.size(),
              contractAddress,
              lastWatchedBlockNumber,
              lastEthereumBlockNumber);

    int addedTransactionsCount = 0;
    int modifiedTransactionsCount = 0;
    for (String transactionHash : transactionHashes) {
      TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
      if (transactionDetail == null) {
        processed = processTransaction(transactionHash, contractDetail) && processed;
        if (processed) {
          addedTransactionsCount++;
        }
      } else {
        LOG.debug(" - transaction {} already exists on database, ignore it.", transactionDetail);
        boolean changed = false;

        // Verify that the transaction has been decoded
        if (StringUtils.isBlank(transactionDetail.getContractAddress())
            || StringUtils.isBlank(transactionDetail.getContractMethodName())) {
          transactionDetail.setContractAddress(contractAddress);
          computeTransactionDetail(transactionDetail, contractDetail);
          changed = true;
        }

        // Broadcast event and send notifications when the transaction was
        // marked as pending
        boolean braodcastSavingTransaction = transactionDetail.isPending();
        transactionDetail.setPending(false);
        changed = changed || braodcastSavingTransaction;

        // If the transaction wasn't marked as succeeded, try to verify the
        // status from Blockchain again
        if (!transactionDetail.isSucceeded()) {
          TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(transactionHash);
          transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());
          changed = true;
        }

        // Save decoded transaction details after chaging its attributes
        if (changed && hasKnownWalletInTransaction(transactionDetail)) {
          getTransactionService().saveTransactionDetail(transactionDetail, braodcastSavingTransaction);
          modifiedTransactionsCount++;
        }
      }
    }

    LOG.debug("{} added and {} modified transactions has been stored using contract {} between block {} and {}",
              addedTransactionsCount,
              modifiedTransactionsCount,
              contractAddress,
              lastWatchedBlockNumber,
              lastEthereumBlockNumber);

    if (processed) {
      // Save last verified block for contracts transactions
      saveLastWatchedBlockNumber(networkId, lastEthereumBlockNumber);
    }
  }

  @Override
  @ExoBlockchainTransaction
  public TransactionDetail computeTransactionDetail(String hash,
                                                    ContractDetail contractDetail) throws InterruptedException {
    if (StringUtils.isBlank(hash)) {
      throw new IllegalArgumentException("Transaction hash is empty");
    }

    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setNetworkId(getNetworkId());
    transactionDetail.setHash(hash);
    return computeTransactionDetail(transactionDetail, contractDetail);
  }

  @Override
  @ExoBlockchainTransaction
  public TransactionDetail computeTransactionDetail(TransactionDetail transactionDetail,
                                                    ContractDetail contractDetail) throws InterruptedException {
    if (transactionDetail == null) {
      throw new IllegalArgumentException("Empty transaction detail parameter");
    }
    String hash = transactionDetail.getHash();
    if (StringUtils.isBlank(hash)) {
      throw new IllegalStateException("Transaction hash is empty");
    }
    if (transactionDetail.getNetworkId() <= 0) {
      throw new IllegalStateException("Unknown network id: " + transactionDetail.getNetworkId());
    }

    Transaction transaction = ethereumClientConnector.getTransaction(hash);
    if (transaction == null) {
      LOG.info("Can't find transaction with hash {}, it may be pending", hash);
      return transactionDetail;
    }

    Block block = ethereumClientConnector.getBlock(transaction.getBlockHash());
    transactionDetail.setTimestamp(block.getTimestamp().longValue() * 1000);

    String senderAddress = transaction.getFrom();
    transactionDetail.setFrom(senderAddress);
    BigInteger weiAmount = transaction.getValue();
    transactionDetail.setValueDecimal(weiAmount, 18);

    TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(hash);
    transactionDetail.setPending(transactionReceipt == null);
    transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());

    String receiverAddress = transaction.getTo();
    transactionDetail.setTo(receiverAddress);

    if (contractDetail == null && receiverAddress != null) {
      contractDetail = getContractService().getContractDetail(receiverAddress);
    }

    if (contractDetail != null && StringUtils.isNotBlank(contractDetail.getAddress())) {
      transactionDetail.setContractAddress(contractDetail.getAddress());
      computeContractTransactionDetail(contractDetail, transactionDetail, transactionReceipt);
    }

    return transactionDetail;
  }

  @Override
  @ExoBlockchainTransaction
  public void computeContractTransactionDetail(TransactionDetail transactionDetail,
                                               Object transactionReceipt) {
    computeContractTransactionDetail(null, transactionDetail, (TransactionReceipt) transactionReceipt);
  }

  private void computeContractTransactionDetail(ContractDetail contractDetail,
                                                TransactionDetail transactionDetail,
                                                TransactionReceipt transactionReceipt) {
    List<org.web3j.protocol.core.methods.response.Log> logs = transactionReceipt == null ? null : transactionReceipt.getLogs();
    transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());
    if (!transactionDetail.isSucceeded()) {
      if (StringUtils.equals(transactionDetail.getContractMethodName(), FUNC_INITIALIZEACCOUNT)) {
        getAccountService().setInitializationStatus(transactionDetail.getTo(), WalletInitializationState.MODIFIED);
      }
      return;
    }

    String toAddress = transactionReceipt == null ? null : transactionReceipt.getTo();
    if (contractDetail == null) {
      contractDetail = getContractService().getContractDetail(StringUtils.lowerCase(toAddress));
    }
    if (contractDetail == null || !StringUtils.equalsIgnoreCase(toAddress, contractDetail.getAddress())) {
      return;
    }
    Integer contractDecimals = contractDetail.getDecimals();

    String hash = transactionDetail.getHash();
    if (logs != null && !logs.isEmpty()) {
      int logsSize = logs.size();
      LOG.debug("Retrieving information from blockchain for transaction {} with {} LOGS", hash, logsSize);
      int i = 0;
      boolean transactionLogTreated = false;
      while (!transactionLogTreated && i < logsSize) {
        org.web3j.protocol.core.methods.response.Log log = logs.get(i++);

        List<String> topics = log.getTopics();
        if (topics == null || topics.isEmpty()) {
          LOG.warn("Transaction {} has NO topics", hash);
          transactionDetail.setSucceeded(false);
          continue;
        }
        String topic = topics.get(0);
        LOG.debug("Treating transaction {} with {} topics", hash, topics.size());
        String methodName = CONTRACT_METHODS_BY_SIG.get(topic);
        if (StringUtils.isBlank(methodName)) {
          continue;
        }
        transactionDetail.setContractMethodName(methodName);
        if (StringUtils.equals(methodName, FUNC_TRANSFER)) {
          EventValues parameters = extractEventParameters(TRANSFER_EVENT, log);
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
          EventValues parameters = extractEventParameters(APPROVAL_EVENT, log);
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
          EventValues parameters = extractEventParameters(APPROVEDACCOUNT_EVENT, log);
          transactionDetail.setFrom(transactionReceipt.getFrom());
          if (parameters == null) {
            continue;
          }
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_DISAPPROVEACCOUNT)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(DISAPPROVEDACCOUNT_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(transactionReceipt.getFrom());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_ADDADMIN)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(ADDEDADMIN_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(transactionReceipt.getFrom());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(1).getValue()).longValue());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_REMOVEADMIN)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(REMOVEDADMIN_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(transactionReceipt.getFrom());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_UPGRADEDATA)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(UPGRADEDDATA_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(0).getValue()).longValue());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(1).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_UPGRADEIMPLEMENTATION)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(UPGRADED_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(0).getValue()).longValue());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(1).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_DEPOSIT_FUNDS)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(DEPOSITRECEIVED_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(parameters.getNonIndexedValues().get(0).getValue().toString());
          BigInteger weiAmount = (BigInteger) parameters.getNonIndexedValues().get(1).getValue();
          transactionDetail.setValueDecimal(weiAmount, 18);
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_SETSELLPRICE)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(TOKENPRICECHANGED_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setContractAmount(((BigInteger) parameters.getNonIndexedValues().get(0).getValue()).longValue());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_TRANSFORMTOVESTED)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(VESTING_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setTo(parameters.getIndexedValues().get(0).getValue().toString());
          BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
          transactionDetail.setContractAmountDecimal(amount, contractDecimals);
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_TRANSFEROWNERSHIP)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(TRANSFEROWNERSHIP_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_INITIALIZEACCOUNT)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(INITIALIZATION_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
          transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
          BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
          transactionDetail.setContractAmountDecimal(amount, contractDecimals);
          BigInteger weiAmount = (BigInteger) parameters.getNonIndexedValues().get(1).getValue();
          transactionDetail.setValueDecimal(weiAmount, 18);
          transactionDetail.setAdminOperation(false);

          if (transactionDetail.isSucceeded()) {
            getAccountService().setInitializationStatus(transactionDetail.getTo(), WalletInitializationState.INITIALIZED);
          } else {
            getAccountService().setInitializationStatus(transactionDetail.getTo(), WalletInitializationState.MODIFIED);
          }
        } else if (StringUtils.equals(methodName, FUNC_REWARD)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(REWARD_EVENT, log);
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
  }

  @SuppressWarnings("rawtypes")
  private static EventValues extractEventParameters(Event event, org.web3j.protocol.core.methods.response.Log log) {
    List<String> topics = log.getTopics();
    String encodedEventSignature = EventEncoder.encode(event);
    if (!topics.get(0).equals(encodedEventSignature)) {
      return null;
    }

    List<Type> indexedValues = new ArrayList<>();
    List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());

    List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
    for (int i = 0; i < indexedParameters.size(); i++) {
      Type value = FunctionReturnDecoder.decodeIndexedValue(topics.get(i + 1), indexedParameters.get(i));
      indexedValues.add(value);
    }
    return new EventValues(indexedValues, nonIndexedValues);
  }

  private boolean verifyTransactionStatusOnBlockchain(TransactionDetail pendingTransactionDetail,
                                                      long pendingTransactionMaxDays) throws Exception {
    String hash = pendingTransactionDetail.getHash();
    Transaction transaction = ethereumClientConnector.getTransaction(hash);
    String blockHash = transaction == null ? null : transaction.getBlockHash();
    if (!StringUtils.isBlank(blockHash)
        && !StringUtils.equalsIgnoreCase(EMPTY_HASH, blockHash)
        && transaction.getBlockNumber() != null) {
      getListenerService().broadcast(NEW_TRANSACTION_EVENT, transaction, null);
      return true;
    } else if (transaction == null) {
      boolean emitFailedTransactionEvent = true;
      if (pendingTransactionMaxDays > 0) {
        long creationTimestamp = pendingTransactionDetail.getTimestamp();
        if (creationTimestamp > 0) {
          Duration duration = Duration.ofMillis(System.currentTimeMillis() - creationTimestamp);
          emitFailedTransactionEvent = duration.toDays() >= pendingTransactionMaxDays;
        }
      }
      if (emitFailedTransactionEvent) {
        LOG.debug("Transaction '{}' was not found on blockchain for more than '{}' days, so mark it as failed",
                  hash,
                  pendingTransactionMaxDays);
        getListenerService().broadcast(NEW_TRANSACTION_EVENT, pendingTransactionDetail, null);
        return true;
      }
    }
    return false;
  }

  private boolean processTransaction(String transactionHash, ContractDetail contractDetail) {
    boolean processed = true;
    try {
      LOG.debug(" - treating transaction {} that doesn't exist on database.", transactionHash);

      TransactionDetail transactionDetail = computeTransactionDetail(transactionHash,
                                                                     contractDetail);
      if (transactionDetail == null) {
        throw new IllegalStateException("Empty transaction detail is returned");
      }

      if (hasKnownWalletInTransaction(transactionDetail)) {
        LOG.debug("Saving new transaction that wasn't managed by UI: {}", transactionDetail);
        getTransactionService().saveTransactionDetail(transactionDetail, true);
      }
    } catch (Exception e) {
      processed = false;
      LOG.warn("Error processing transaction {}. It will be retried next time.", transactionHash, e);
    }
    return processed;
  }

  private long getLastWatchedBlockNumber(long networkId) {
    SettingValue<?> lastBlockNumberValue =
                                         getSettingService().get(WALLET_CONTEXT,
                                                                 WALLET_SCOPE,
                                                                 LAST_BLOCK_NUMBER_KEY_NAME + networkId);
    if (lastBlockNumberValue != null && lastBlockNumberValue.getValue() != null) {
      return Long.valueOf(lastBlockNumberValue.getValue().toString());
    }
    return 0;
  }

  private void saveLastWatchedBlockNumber(long networkId, long lastWatchedBlockNumber) {
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

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

  private WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

  private WalletContractService getContractService() {
    if (contractService == null) {
      contractService = CommonsUtils.getService(WalletContractService.class);
    }
    return contractService;
  }
}
