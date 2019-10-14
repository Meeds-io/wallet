/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.wallet.dao;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.TransactionEntity;

public class WalletTransactionDAO extends GenericDAOJPAImpl<TransactionEntity, Long> {

  private static final String HASH_PARAM                 = "hash";

  private static final String START_DATE                 = "startDate";

  private static final String END_DATE                   = "endDate";

  private static final String ADDRESS_PARAM              = "address";

  private static final String CONTRACT_ADDRESS_PARAM     = "contractAddress";

  private static final String CONTRACT_METHOD_NAME_PARAM = "methodName";

  private static final String NETWORK_ID_PARAM           = "networkId";

  public List<TransactionEntity> getContractTransactions(String contractAddress,
                                                         String contractMethodName,
                                                         int limit) {
    contractAddress = StringUtils.lowerCase(contractAddress);

    String queryName = "WalletTransaction.getContractTransactions";
    if (StringUtils.isNotBlank(contractMethodName)) {
      queryName = "WalletTransaction.getContractTransactionsWithMethodName";
    }

    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery(queryName,
                                                                              TransactionEntity.class);
    query.setParameter(CONTRACT_ADDRESS_PARAM, contractAddress.toLowerCase());
    if (StringUtils.isNotBlank(contractMethodName)) {
      query.setParameter(CONTRACT_METHOD_NAME_PARAM, contractMethodName);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public List<TransactionEntity> getTransactions(long networkId, int limit) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getNetworkTransactions",
                                                                              TransactionEntity.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public List<TransactionEntity> getWalletTransactions(long networkId,
                                                       String address,
                                                       String contractAddress,
                                                       String contractMethodName,
                                                       int limit,
                                                       boolean onlyPending,
                                                       boolean includeAdministrationTransactions) {

    address = StringUtils.lowerCase(address);
    contractAddress = StringUtils.lowerCase(contractAddress);
    StringBuilder queryString = new StringBuilder("SELECT tx FROM WalletTransaction tx WHERE tx.networkId = ");
    queryString.append(networkId);

    if (!includeAdministrationTransactions) {
      queryString.append(" AND tx.isAdminOperation = FALSE");
    }

    queryString.append(" AND (tx.fromAddress = '");
    queryString.append(address);
    queryString.append("' OR tx.toAddress = '");
    queryString.append(address);
    queryString.append("' OR tx.byAddress = '");
    queryString.append(address);
    queryString.append("')");

    if (StringUtils.isNotBlank(contractMethodName)) {
      queryString.append(" AND tx.contractMethodName = '");
      queryString.append(contractMethodName);
      queryString.append("'");
    }

    if (onlyPending) {
      queryString.append(" AND tx.isPending = TRUE");
    }

    if (StringUtils.isNotBlank(contractAddress)) {
      queryString.append(" AND tx.contractAddress = '");
      queryString.append(contractAddress);
      queryString.append("' ");
    }

    queryString.append(" ORDER BY tx.createdDate DESC");
    TypedQuery<TransactionEntity> query = getEntityManager().createQuery(queryString.toString(), TransactionEntity.class);
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public List<TransactionEntity> getPendingTransactions(long networkId) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getPendingTransactions",
                                                                              TransactionEntity.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    List<TransactionEntity> resultList = query.getResultList();
    return resultList == null ? null : resultList;
  }

  public TransactionEntity getTransactionByHash(String hash) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getTransactionByHash",
                                                                              TransactionEntity.class);
    query.setParameter(HASH_PARAM, StringUtils.lowerCase(hash));
    List<TransactionEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

  public long getMaxUsedNonce(long networkId, String fromAddress) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("WalletTransaction.getMaxUsedNonce",
                                                                 Long.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    query.setParameter(ADDRESS_PARAM, StringUtils.lowerCase(fromAddress));
    Long result = query.getSingleResult();
    return result == null ? 0 : result;
  }

  public double countReceivedContractAmount(String contractAddress,
                                            String address,
                                            ZonedDateTime startDate,
                                            ZonedDateTime endDate) {
    TypedQuery<Double> query = getEntityManager().createNamedQuery("WalletTransaction.countReceivedContractAmount",
                                                                   Double.class);
    query.setParameter(CONTRACT_ADDRESS_PARAM, StringUtils.lowerCase(contractAddress));
    query.setParameter(ADDRESS_PARAM, StringUtils.lowerCase(address));
    query.setParameter(START_DATE, toMilliSeconds(startDate));
    query.setParameter(END_DATE, toMilliSeconds(endDate));
    Double result = query.getSingleResult();
    return result == null ? 0 : result;
  }

  public double countSentContractAmount(String contractAddress,
                                        String address,
                                        ZonedDateTime startDate,
                                        ZonedDateTime endDate) {
    TypedQuery<Double> query = getEntityManager().createNamedQuery("WalletTransaction.countSentContractAmount",
                                                                   Double.class);
    query.setParameter(CONTRACT_ADDRESS_PARAM, StringUtils.lowerCase(contractAddress));
    query.setParameter(ADDRESS_PARAM, StringUtils.lowerCase(address));
    query.setParameter(START_DATE, toMilliSeconds(startDate));
    query.setParameter(END_DATE, toMilliSeconds(endDate));
    Double result = query.getSingleResult();
    return result == null ? 0 : result;
  }

  public List<TransactionEntity> getTransactionsToSend(long networkId) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getTransactionsToSend",
                                                                              TransactionEntity.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    return query.getResultList();
  }

  public long countPendingTransactionSent(long networkId, String fromAddress) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("WalletTransaction.countPendingTransactionSent",
                                                                 Long.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    query.setParameter(ADDRESS_PARAM, StringUtils.lowerCase(fromAddress));
    Long result = query.getSingleResult();
    return result == null ? 0 : result;
  }

  public long countPendingTransactionAsSender(long networkId, String fromAddress) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("WalletTransaction.countPendingTransactionAsSender",
                                                                 Long.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    query.setParameter(ADDRESS_PARAM, StringUtils.lowerCase(fromAddress));
    Long result = query.getSingleResult();
    return result == null ? 0 : result;
  }

  private long toMilliSeconds(ZonedDateTime date) {
    return date.toInstant().toEpochMilli();
  }

}
