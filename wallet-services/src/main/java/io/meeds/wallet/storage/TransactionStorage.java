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
package io.meeds.wallet.storage;

import static io.meeds.wallet.wallet.utils.WalletUtils.TRANSACTION_CREATED_EVENT;
import static io.meeds.wallet.wallet.utils.WalletUtils.TRANSACTION_MODIFIED_EVENT;
import static io.meeds.wallet.wallet.utils.WalletUtils.formatTransactionHash;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import io.meeds.wallet.dao.WalletTransactionDAO;
import io.meeds.wallet.entity.TransactionEntity;
import io.meeds.wallet.wallet.model.Wallet;
import io.meeds.wallet.wallet.model.transaction.TransactionDetail;

public class TransactionStorage {

  private static final Log     LOG = ExoLogger.getLogger(TransactionStorage.class);

  private WalletTransactionDAO walletTransactionDAO;

  private ListenerService      listenerService;

  public TransactionStorage(ListenerService listenerService, WalletTransactionDAO walletTransactionDAO) {
    this.listenerService = listenerService;
    this.walletTransactionDAO = walletTransactionDAO;
  }

  /**
   * @param address {@link Wallet} address
   * @param networkId blockchain network id
   * @return {@link List} of {@link TransactionDetail} of type ether transfer
   *         marked as pending in internal database
   */
  public List<TransactionDetail> getPendingEtherTransactions(String address, long networkId) {
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingEtherTransactions(address, networkId);
    return fromEntities(transactions);
  }

  public List<TransactionDetail> getPendingWalletTransactionsNotSent(String address, long networkId) {
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingWalletTransactionsNotSent(address, networkId);
    return fromEntities(transactions);
  }

  public List<TransactionDetail> getPendingWalletTransactionsSent(String address, long networkId) {
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingWalletTransactionsSent(address, networkId);
    return fromEntities(transactions);
  }

  public long countContractPendingTransactionsSent(long networkId) {
    return walletTransactionDAO.countContractPendingTransactionsSent(networkId);
  }

  public long countContractPendingTransactionsToSend(long networkId) {
    return walletTransactionDAO.countContractPendingTransactionsToSend(networkId);
  }

  /**
   * @param networkId blockchain network id
   * @return {@link List} of {@link TransactionDetail} not yet sent on
   *         blockchain
   */
  public List<TransactionDetail> getTransactionsToSend(long networkId) {
    List<TransactionEntity> transactions = walletTransactionDAO.getTransactionsToSend(networkId);
    return fromEntities(transactions);
  }

  /**
   * @param networkId blockchain network id
   * @param fromAddress wallet address
   * @return the max used nonce from stored transactions in internal database
   */
  public long getMaxUsedNonce(long networkId, String fromAddress) {
    return walletTransactionDAO.getMaxUsedNonce(networkId, fromAddress);
  }

  /**
   * @param contractAddress filter transactions by a contract address
   * @param contractMethodName filter transactions by a contract method
   * @param limit size limit of transactions to retrieve
   * @return {@link List} of {@link TransactionDetail} with corresponding filter
   *         entries
   */
  public List<TransactionDetail> getContractTransactions(String contractAddress,
                                                         String contractMethodName,
                                                         int limit) {
    List<TransactionEntity> transactions = walletTransactionDAO.getContractTransactions(StringUtils.lowerCase(contractAddress),
                                                                                        contractMethodName,
                                                                                        limit);
    return fromEntities(transactions);
  }

  /**
   * @param networkId blockchain network id
   * @param limit size limit of transactions to retrieve
   * @return {@link List} of {@link TransactionDetail} for selected blockchain
   *         network id
   */
  public List<TransactionDetail> getTransactions(long networkId, int limit) {
    List<TransactionEntity> transactions = walletTransactionDAO.getTransactions(networkId, limit);
    return fromEntities(transactions);
  }

  /**
   * @param networkId blockchain network id
   * @param address wallet address
   * @param contractAddress filter transactions by a contract address
   * @param contractMethodName filter transactions by a contract method
   * @param hash retrieve include in the list of transactions this hash even if
   *          the limit is reached
   * @param limit size limit of transactions to retrieve
   * @param pending whether include pending or not
   * @param administration whether include administration transactions or not
   * @return {@link List} of {@link TransactionDetail} with corresponding filter
   *         entries
   */
  public List<TransactionDetail> getWalletTransactions(long networkId, // NOSONAR
                                                       String address,
                                                       String contractAddress,
                                                       String contractMethodName,
                                                       String hash,
                                                       int limit,
                                                       boolean pending,
                                                       boolean administration) {
    List<TransactionEntity> transactions = getWalletTransactions(networkId,
                                                                 address,
                                                                 contractAddress,
                                                                 contractMethodName,
                                                                 limit,
                                                                 pending,
                                                                 administration);
    boolean limitNotReached = transactions.size() == limit;
    int limitToSearchForHash = limit * 2;
    if ((StringUtils.isNotBlank(hash) && limitNotReached
        && transactions.stream().noneMatch(transaction -> StringUtils.equalsIgnoreCase(transaction.getHash(), hash)))) {
      return getWalletTransactions(networkId,
                                   address,
                                   contractAddress,
                                   contractMethodName,
                                   hash,
                                   limitToSearchForHash,
                                   pending,
                                   administration);
    }
    return fromEntities(transactions);
  }

  private List<TransactionEntity> getWalletTransactions(long networkId, // NOSONAR
                                                        String address,
                                                        String contractAddress,
                                                        String contractMethodName,
                                                        int limit,
                                                        boolean pending,
                                                        boolean administration) {
    return walletTransactionDAO.getWalletTransactions(networkId,
                                                      address,
                                                      contractAddress,
                                                      contractMethodName,
                                                      limit,
                                                      pending,
                                                      administration);
  }

  /**
   * Saves a decoded transaction detail in internal database
   * 
   * @param transactionDetail decoded transaction detail
   */
  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    if (transactionDetail.getTimestamp() <= 0) {
      transactionDetail.setTimestamp(System.currentTimeMillis());
    }
    if (transactionDetail.getSentTimestamp() <= 0 && StringUtils.isBlank(transactionDetail.getRawTransaction())) {
      // Transaction sent by external wallet, thus add sending timestamp
      transactionDetail.setSentTimestamp(transactionDetail.getTimestamp());
    }
    TransactionEntity transactionEntity = toEntity(transactionDetail);
    if (transactionEntity.getId() == 0) {
      transactionEntity = walletTransactionDAO.create(transactionEntity);
      transactionDetail.setId(transactionEntity.getId());
      broadcastTransactionEvent(transactionDetail, TRANSACTION_CREATED_EVENT);
    } else {
      walletTransactionDAO.update(transactionEntity);
      broadcastTransactionEvent(transactionDetail, TRANSACTION_MODIFIED_EVENT);
    }
  }

  /**
   * Return list of transactions for a given address that corresponds to a nonce
   * 
   * @param networkId blockchain network id
   * @param transactionHash Transaction hash that will replace others
   * @param fromAddress transaction sender address
   * @param nonce Nonce of the transaction
   * @return {@link List} of {@link TransactionDetail}
   */
  public List<TransactionDetail> getPendingTransactionsWithSameNonce(long networkId,
                                                                     String transactionHash,
                                                                     String fromAddress,
                                                                     long nonce) {
    List<TransactionEntity> transactionEntities = walletTransactionDAO.getPendingTransactionsWithSameNonce(networkId,
                                                                                                           transactionHash,
                                                                                                           fromAddress,
                                                                                                           nonce);
    return fromEntities(transactionEntities);
  }

  /**
   * Count the number of transactions for a given address that corresponds to a
   * given nonce and that are always marked as pending
   *
   * @param networkId blockchain network id
   * @param transactionHash Transaction hash to exclude from count
   * @param fromAddress transaction sender address
   * @param nonce Nonce of the transaction
   * @return {@link List} of {@link TransactionDetail}
   */
  public long countPendingTransactionsWithSameNonce(long networkId, String transactionHash, String fromAddress, long nonce) {
    return walletTransactionDAO.countPendingTransactionsWithSameNonce(networkId, transactionHash, fromAddress, nonce);
  }
  
  /**
   * Retrieve a {@link TransactionDetail} identified by its blockchain hash
   * 
   * @param hash blockchain transaction hash
   * @return {@link TransactionDetail}
   */
  public TransactionDetail getTransactionByHash(String hash) {
    hash = formatTransactionHash(hash);
    TransactionEntity transactionEntity = walletTransactionDAO.getTransactionByHash(hash);
    return fromEntity(transactionEntity);
  }

  /**
   * Retrieve a {@link TransactionDetail} identified by its blockchain hash
   *
   * @param hash blockchain transaction hash
   * @return {@link TransactionDetail}
   */
  public TransactionDetail getPendingTransactionByHash(String hash) {
    hash = formatTransactionHash(hash);
    TransactionEntity transactionEntity = walletTransactionDAO.getPendingTransactionByHash(hash);
    return fromEntity(transactionEntity);
  }

  /**
   * @param networkId blockchain network id
   * @param fromAddress transaction sender address
   * @return count of pending transactions sent on a blockchain network for a
   *         specified user
   */
  public long countPendingTransactionSent(long networkId, String fromAddress) {
    return walletTransactionDAO.countPendingTransactionSent(networkId, fromAddress);
  }

  /**
   * @param networkId blockchain network id
   * @param fromAddress transaction sender address
   * @return count of pending transactions of a sender
   */
  public long countPendingTransactionAsSender(long networkId, String fromAddress) {
    return walletTransactionDAO.countPendingTransactionAsSender(networkId, fromAddress);
  }

  /**
   * @param contractAddress blockchain contract address
   * @param address wallet address
   * @param startDate start date of selected period
   * @param endDate end date of selected period
   * @return sum of token amounts received during selected period
   */
  public double countReceivedContractAmount(String contractAddress,
                                            String address,
                                            ZonedDateTime startDate,
                                            ZonedDateTime endDate) {
    return walletTransactionDAO.countReceivedContractAmount(contractAddress, address, startDate, endDate);
  }

  /**
   * @param contractAddress blockchain contract address
   * @param address wallet address
   * @param startDate start date of selected period
   * @param endDate end date of selected period
   * @return sum of token amounts sent during selected period
   */
  public double countSentContractAmount(String contractAddress,
                                        String address,
                                        ZonedDateTime startDate,
                                        ZonedDateTime endDate) {
    return walletTransactionDAO.countSentContractAmount(contractAddress, address, startDate, endDate);
  }

  public long countTransactions() {
    return walletTransactionDAO.count();
  }

  private void broadcastTransactionEvent(TransactionDetail transactionDetail, String eventName) {
    try {
      listenerService.broadcast(eventName, transactionDetail, transactionDetail);
    } catch (Exception e) {
      LOG.warn("Error when broadcasting event '{}' on transaction '{}'", eventName, transactionDetail.getHash());
    }
  }

  private List<TransactionDetail> fromEntities(List<TransactionEntity> transactions) {
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().sequential().map(this::fromEntity).collect(Collectors.toList());
  }

  private TransactionDetail fromEntity(TransactionEntity entity) {
    if (entity == null) {
      return null;
    }
    TransactionDetail detail = new TransactionDetail();
    detail.setId(entity.getId());
    detail.setAdminOperation(entity.isAdminOperation());
    detail.setContractAddress(entity.getContractAddress());
    detail.setContractAmount(entity.getContractAmount());
    detail.setContractMethodName(entity.getContractMethodName());
    detail.setTimestamp(entity.getCreatedDate());
    detail.setValue(entity.getValue());
    detail.setIssuerId(entity.getIssuerIdentityId());
    detail.setHash(entity.getHash());
    detail.setFrom(entity.getFromAddress());
    detail.setTo(entity.getToAddress());
    detail.setBy(entity.getByAddress());
    detail.setLabel(entity.getLabel());
    detail.setMessage(entity.getMessage());
    detail.setNetworkId(entity.getNetworkId());
    detail.setPending(entity.isPending());
    detail.setSucceeded(entity.isSuccess());
    detail.setDropped(entity.isDropped());
    detail.setGasPrice(entity.getGasPrice());
    detail.setGasUsed(entity.getGasUsed());
    detail.setTokenFee(entity.getTokenFee());
    detail.setEtherFee(entity.getEtherFee());
    detail.setNoContractFunds(entity.isNoContractFunds());
    detail.setNonce(entity.getNonce());
    detail.setSentTimestamp(entity.getSentDate());
    detail.setSendingAttemptCount(entity.getSendingAttemptCount());
    detail.setRawTransaction(entity.getRawTransaction());
    detail.setBoost(entity.isBoost());
    return detail;
  }

  private TransactionEntity toEntity(TransactionDetail detail) {
    TransactionEntity entity = new TransactionEntity();
    if (detail.getId() > 0) {
      entity.setId(detail.getId());
    }
    entity.setNetworkId(detail.getNetworkId());
    entity.setHash(formatTransactionHash(detail.getHash()));
    entity.setFromAddress(StringUtils.lowerCase(detail.getFrom()));
    entity.setToAddress(StringUtils.lowerCase(detail.getTo()));
    entity.setByAddress(StringUtils.lowerCase(detail.getBy()));
    entity.setContractAddress(StringUtils.lowerCase(detail.getContractAddress()));
    entity.setContractAmount(detail.getContractAmount());
    entity.setContractMethodName(detail.getContractMethodName());
    entity.setAdminOperation(detail.isAdminOperation());
    entity.setLabel(detail.getLabel());
    entity.setMessage(detail.getMessage());
    entity.setPending(detail.isPending());
    entity.setSuccess(detail.isSucceeded());
    entity.setDropped(detail.isDropped());
    entity.setValue(detail.getValue());
    entity.setGasPrice(detail.getGasPrice());
    entity.setTokenFee(detail.getTokenFee());
    entity.setEtherFee(detail.getEtherFee());
    entity.setGasUsed(detail.getGasUsed());
    entity.setNoContractFunds(detail.isNoContractFunds());
    entity.setNonce(detail.getNonce());
    entity.setBoost(detail.isBoost());
    entity.setSentDate(detail.getSentTimestamp());
    entity.setSendingAttemptCount(detail.getSendingAttemptCount());
    entity.setRawTransaction(detail.getRawTransaction());
    if (detail.getTimestamp() == 0) {
      entity.setCreatedDate(System.currentTimeMillis());
    }
    entity.setCreatedDate(detail.getTimestamp());
    if (detail.getIssuer() != null) {
      entity.setIssuerIdentityId(detail.getIssuer().getTechnicalId());
    } else if (detail.getIssuerId() > 0) {
      entity.setIssuerIdentityId(detail.getIssuerId());
    }
    return entity;
  }

}
