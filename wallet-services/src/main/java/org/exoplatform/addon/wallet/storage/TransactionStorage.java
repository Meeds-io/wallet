package org.exoplatform.addon.wallet.storage;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.math.BigInteger;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.wallet.dao.WalletTransactionDAO;
import org.exoplatform.addon.wallet.entity.TransactionEntity;
import org.exoplatform.addon.wallet.model.TransactionDetail;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class TransactionStorage {

  private static final Log     LOG                         = ExoLogger.getLogger(TransactionStorage.class);

  private static final long    MINIMUM_CREATED_DATE_MILLIS =
                                                           LocalDate.of(2018, 1, 1)
                                                                    .atStartOfDay(ZoneOffset.systemDefault())
                                                                    .toEpochSecond()
                                                               * 1000;

  private WalletTransactionDAO walletTransactionDAO;

  public TransactionStorage(WalletTransactionDAO walletTransactionDAO) {
    this.walletTransactionDAO = walletTransactionDAO;
  }

  public List<TransactionDetail> getPendingTransactions(long networkId) {
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingTransactions(networkId);
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
  }

  public List<TransactionDetail> getContractTransactions(long networkId,
                                                         String contractAddress,
                                                         String contractMethodName,
                                                         int limit) {
    List<TransactionEntity> transactions = walletTransactionDAO.getContractTransactions(networkId,
                                                                                        StringUtils.lowerCase(contractAddress),
                                                                                        contractMethodName,
                                                                                        limit);
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
  }

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
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());

  }

  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    TransactionEntity transactionEntity = toEntity(transactionDetail);
    if (transactionEntity.getId() == 0) {
      transactionEntity = walletTransactionDAO.create(transactionEntity);
      transactionDetail.setId(transactionEntity.getId());
    } else {
      walletTransactionDAO.update(transactionEntity);
    }
  }

  public TransactionDetail getAddressLastPendingTransactionSent(long networkId, String address) {
    TransactionEntity transactionEntity = walletTransactionDAO.getAddressLastPendingTransactionSent(networkId, address);
    return fromEntity(transactionEntity);
  }

  public TransactionDetail getTransactionByHash(String hash) {
    hash = formatTransactionHash(hash);
    TransactionEntity transactionEntity = walletTransactionDAO.getTransactionByHash(hash);
    return fromEntity(transactionEntity);
  }

  /**
   * Return contract amount received during a period of time
   * 
   * @param networkId
   * @param contractAddress
   * @param address
   * @param startDate
   * @param endDate
   */
  public double countReceivedContractAmount(long networkId,
                                            String contractAddress,
                                            String address,
                                            ZonedDateTime startDate,
                                            ZonedDateTime endDate) {
    return walletTransactionDAO.countReceivedContractAmount(networkId, contractAddress, address, startDate, endDate);
  }

  public double countSentContractAmount(long networkId,
                                        String contractAddress,
                                        String address,
                                        ZonedDateTime startDate,
                                        ZonedDateTime endDate) {
    return walletTransactionDAO.countSentContractAmount(networkId, contractAddress, address, startDate, endDate);
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
    // FIXME Workaround for old bug when adding timestamp in seconds // NOSONAR
    if (entity.getCreatedDate() > 0 && entity.getCreatedDate() < MINIMUM_CREATED_DATE_MILLIS) {
      if (LOG.isDebugEnabled()) {
        LOG.warn("Transaction {} has a 'CreatedDate' in seconds, converting it to milliseconds.", entity.getHash());
      }
      entity.setCreatedDate(entity.getCreatedDate() * 1000);
      walletTransactionDAO.update(entity);
    }
    // FIXME workaround to update ether amount that was stored in milliseconds // NOSONAR
    if (entity.getValue() > 10000L && (StringUtils.isBlank(entity.getContractAddress())
        || StringUtils.isBlank(entity.getContractMethodName())
        || StringUtils.equals(entity.getContractMethodName(), ERTTokenV2.FUNC_INITIALIZEACCOUNT)
        || StringUtils.equals(entity.getContractMethodName(), ERTTokenV2.FUNC_SETSELLPRICE))) {
      if (LOG.isDebugEnabled()) {
        LOG.warn("[from DB] Transaction {} has a value in WEI, converting it to ether.", entity.getHash());
      }
      entity.setValue(convertFromDecimals(new BigInteger(String.valueOf(entity.getValue())), ETHER_TO_WEI_DECIMALS));
      walletTransactionDAO.update(entity);
    }
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
    if (detail.getTimestamp() == 0) {
      entity.setCreatedDate(System.currentTimeMillis());
    } else if (detail.getTimestamp() < MINIMUM_CREATED_DATE_MILLIS) {
      if (LOG.isDebugEnabled()) {
        LOG.warn("[to store on DB] Transaction {} has a 'CreatedDate' in seconds, converting it to milliseconds.",
                 entity.getHash());
      }
      detail.setTimestamp(entity.getCreatedDate() * 1000);
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
