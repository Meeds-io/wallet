package org.exoplatform.wallet.storage;

import static org.exoplatform.wallet.utils.WalletUtils.formatTransactionHash;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.wallet.dao.WalletTransactionDAO;
import org.exoplatform.wallet.entity.TransactionEntity;
import org.exoplatform.wallet.model.transaction.TransactionDetail;

public class TransactionStorage {

  private WalletTransactionDAO walletTransactionDAO;

  public TransactionStorage(WalletTransactionDAO walletTransactionDAO) {
    this.walletTransactionDAO = walletTransactionDAO;
  }

  /**
   * @param networkId blockchain network id
   * @return {@link List} of {@link TransactionDetail} marked as pending in
   *         internal database
   */
  public List<TransactionDetail> getPendingTransaction(long networkId) {
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingTransactions(networkId);
    return fromEntities(transactions);
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
  public List<TransactionDetail> getWalletTransactions(long networkId,
                                                       String address,
                                                       String contractAddress,
                                                       String contractMethodName,
                                                       String hash,
                                                       int limit,
                                                       boolean pending,
                                                       boolean administration) {

    address = StringUtils.lowerCase(address);
    List<TransactionEntity> transactions = walletTransactionDAO.getWalletTransactions(networkId,
                                                                                      address,
                                                                                      contractAddress,
                                                                                      contractMethodName,
                                                                                      limit,
                                                                                      pending,
                                                                                      administration);
    boolean limitNotReached = transactions != null && transactions.size() == limit;
    if (StringUtils.isNotBlank(hash) && limitNotReached
        && transactions.stream().noneMatch(transaction -> StringUtils.equalsIgnoreCase(transaction.getHash(), hash))) {
      return getWalletTransactions(networkId,
                                   address,
                                   contractAddress,
                                   contractMethodName,
                                   hash,
                                   limit * 2,
                                   pending,
                                   administration);
    }
    return fromEntities(transactions);

  }

  /**
   * Saves a decoded transaction detail in internal database
   * 
   * @param transactionDetail decoded transaction detail
   */
  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    TransactionEntity transactionEntity = toEntity(transactionDetail);
    if (transactionDetail.getTimestamp() <= 0) {
      transactionDetail.setTimestamp(System.currentTimeMillis());
    }
    if (transactionEntity.getId() == 0) {
      transactionEntity = walletTransactionDAO.create(transactionEntity);
      transactionDetail.setId(transactionEntity.getId());
    } else {
      walletTransactionDAO.update(transactionEntity);
    }
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

  private List<TransactionDetail> fromEntities(List<TransactionEntity> transactions) {
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
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
    detail.setGasPrice(entity.getGasPrice());
    detail.setGasUsed(entity.getGasUsed());
    detail.setTokenFee(entity.getTokenFee());
    detail.setEtherFee(entity.getEtherFee());
    detail.setNoContractFunds(entity.isNoContractFunds());
    detail.setNonce(entity.getNonce());
    detail.setSentTimestamp(entity.getSentDate());
    detail.setSendingAttemptCount(entity.getSendingAttemptCount());
    detail.setRawTransaction(entity.getRawTransaction());
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
    entity.setValue(detail.getValue());
    entity.setGasPrice(detail.getGasPrice());
    entity.setTokenFee(detail.getTokenFee());
    entity.setEtherFee(detail.getEtherFee());
    entity.setGasUsed(detail.getGasUsed());
    entity.setNoContractFunds(detail.isNoContractFunds());
    entity.setNonce(detail.getNonce());
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
