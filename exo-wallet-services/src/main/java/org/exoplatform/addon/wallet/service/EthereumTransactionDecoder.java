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

import static org.exoplatform.addon.wallet.contract.ERTTokenV2.*;

import java.math.BigInteger;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumTransactionDecoder {

  private static final Log                 LOG                              =
                                               ExoLogger.getLogger(EthereumTransactionDecoder.class);

  private static final String              FUNC_DEPOSIT_FUNDS               = "depositFunds";

  private static final String              TRANSFER_SIGNATURE               =
                                                              org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.TRANSFER_EVENT);

  private static final String              APPROVAL_SIGNATURE               =
                                                              org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.APPROVAL_EVENT);

  private static final String              ADDED_ADMIN_METHOD_SIGNATURE     =
                                                                        org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.ADDEDADMIN_EVENT);

  private static final String              REMOVED_ADMIN_SIGNATURE          =
                                                                   org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.REMOVEDADMIN_EVENT);

  private static final String              APPROVED_ACCOUNT_SIGNATURE       =
                                                                      org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.APPROVEDACCOUNT_EVENT);

  private static final String              DISAPPROVED_ACCOUNT_SIGNATURE    =
                                                                         org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.DISAPPROVEDACCOUNT_EVENT);

  private static final String              CONTRACT_PAUSED_SIGNATURE        =
                                                                     org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.CONTRACTPAUSED_EVENT);

  private static final String              CONTRACT_UNPAUSED_SIGNATURE      =
                                                                       org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.CONTRACTUNPAUSED_EVENT);

  private static final String              DEPOSIT_RECEIVED_SIGNATURE       =
                                                                      org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.DEPOSITRECEIVED_EVENT);

  private static final String              TOKEN_PRICE_CHANGED_SIGNATURE    =
                                                                         org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.TOKENPRICECHANGED_EVENT);

  private static final String              TRANSFER_OWNERSHIP_SIGNATURE     =
                                                                        org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.TRANSFEROWNERSHIP_EVENT);

  private static final String              ACCOUNT_INITIALIZATION_SIGNATURE =
                                                                            org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.INITIALIZATION_EVENT);

  private static final String              ACCOUNT_REWARD_SIGNATURE         =
                                                                    org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.REWARD_EVENT);

  private static final String              ACCOUNT_VESTED_SIGNATURE         =
                                                                    org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.VESTING_EVENT);

  private static final String              TRANSFER_VESTING_SIGNATURE       =
                                                                      org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.VESTINGTRANSFER_EVENT);

  private static final String              UPGRADED_SIGNATURE               =
                                                              org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.UPGRADED_EVENT);

  private static final String              DATA_UPGRADED_SIGNATURE          =
                                                                   org.exoplatform.addon.wallet.fork.EventEncoder.encode(ERTTokenV2.UPGRADEDDATA_EVENT);

  private static final Map<String, String> CONTRACT_METHODS_BY_SIG          = new HashMap<>();

  static {
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_SIGNATURE, FUNC_TRANSFER);
    CONTRACT_METHODS_BY_SIG.put(APPROVAL_SIGNATURE, FUNC_APPROVE);
    CONTRACT_METHODS_BY_SIG.put(ADDED_ADMIN_METHOD_SIGNATURE, FUNC_ADDADMIN);
    CONTRACT_METHODS_BY_SIG.put(REMOVED_ADMIN_SIGNATURE, FUNC_REMOVEADMIN);
    CONTRACT_METHODS_BY_SIG.put(APPROVED_ACCOUNT_SIGNATURE, FUNC_APPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(DISAPPROVED_ACCOUNT_SIGNATURE, FUNC_DISAPPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(CONTRACT_PAUSED_SIGNATURE, FUNC_PAUSE);
    CONTRACT_METHODS_BY_SIG.put(CONTRACT_UNPAUSED_SIGNATURE, FUNC_UNPAUSE);
    CONTRACT_METHODS_BY_SIG.put(DEPOSIT_RECEIVED_SIGNATURE, FUNC_DEPOSIT_FUNDS);
    CONTRACT_METHODS_BY_SIG.put(TOKEN_PRICE_CHANGED_SIGNATURE, FUNC_SETSELLPRICE);
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_OWNERSHIP_SIGNATURE, FUNC_TRANSFEROWNERSHIP);
    CONTRACT_METHODS_BY_SIG.put(ACCOUNT_INITIALIZATION_SIGNATURE, FUNC_INITIALIZEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(ACCOUNT_REWARD_SIGNATURE, FUNC_REWARD);
    CONTRACT_METHODS_BY_SIG.put(ACCOUNT_VESTED_SIGNATURE, FUNC_TRANSFORMTOVESTED);
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_VESTING_SIGNATURE, FUNC_TRANSFER);
    CONTRACT_METHODS_BY_SIG.put(UPGRADED_SIGNATURE, FUNC_UPGRADEIMPLEMENTATION);
    CONTRACT_METHODS_BY_SIG.put(DATA_UPGRADED_SIGNATURE, FUNC_UPGRADEDATA);
  }

  private EthereumWalletContractService contractService;

  private WalletAccountService          accountService;

  private EthereumClientConnector       ethereumClientConnector;

  public EthereumTransactionDecoder(EthereumClientConnector ethereumClientConnector,
                                    EthereumWalletContractService contractService) {
    this.ethereumClientConnector = ethereumClientConnector;
    this.contractService = contractService;
  }

  public TransactionDetail computeTransactionDetail(long networkId,
                                                    String hash,
                                                    ContractDetail contractDetail) throws InterruptedException {
    if (StringUtils.isBlank(hash)) {
      throw new IllegalArgumentException("Transaction hash is empty");
    }
    if (networkId <= 0) {
      throw new IllegalArgumentException("Unknown network id: " + networkId);
    }

    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setNetworkId(networkId);
    transactionDetail.setHash(hash);
    return computeTransactionDetail(transactionDetail, contractDetail);
  }

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
    transactionDetail.setTimestamp(block.getTimestamp().longValue());

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
      contractDetail = contractService.getContractDetail(receiverAddress, transactionDetail.getNetworkId());
    }

    if (contractDetail != null) {
      transactionDetail.setContractAddress(contractDetail.getAddress());
      computeContractTransactionDetail(transactionDetail, transactionReceipt);
    }

    return transactionDetail;
  }

  public void computeContractTransactionDetail(TransactionDetail transactionDetail,
                                               TransactionReceipt transactionReceipt) {
    List<org.web3j.protocol.core.methods.response.Log> logs = transactionReceipt == null ? null : transactionReceipt.getLogs();
    transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());
    if (!transactionDetail.isSucceeded()) {
      if (StringUtils.equals(transactionDetail.getContractMethodName(), FUNC_INITIALIZEACCOUNT)) {
        getAccountService().setInitializationStatus(transactionDetail.getTo(), WalletInitializationState.MODIFIED);
      }
      return;
    }

    String contractAddress = transactionReceipt == null ? null : transactionReceipt.getTo();
    ContractDetail contractDetail =
                                  contractService.getContractDetail(contractAddress, transactionDetail.getNetworkId());
    if (contractDetail == null) {
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
  }

  @SuppressWarnings("rawtypes")
  private static EventValues extractEventParameters(Event event, org.web3j.protocol.core.methods.response.Log log) {
    List<String> topics = log.getTopics();
    String encodedEventSignature = org.exoplatform.addon.wallet.fork.EventEncoder.encode(event);
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

  private WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

}
