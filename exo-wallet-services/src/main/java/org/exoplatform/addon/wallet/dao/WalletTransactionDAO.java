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
package org.exoplatform.addon.wallet.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.entity.TransactionEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class WalletTransactionDAO extends GenericDAOJPAImpl<TransactionEntity, Long> {

  private static final String HASH_PARAM             = "hash";

  private static final String ADDRESS_PARAM          = "address";

  private static final String CONTRACT_ADDRESS_PARAM = "contractAddress";

  private static final String NETWORK_ID_PARAM       = "networkId";

  public List<TransactionEntity> getContractTransactions(long networkId, String contractAddress, int limit) {
    contractAddress = StringUtils.lowerCase(contractAddress);

    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getContractTransactions",
                                                                              TransactionEntity.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    query.setParameter(CONTRACT_ADDRESS_PARAM, contractAddress.toLowerCase());
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public List<TransactionEntity> getWalletTransactions(long networkId,
                                                       String address,
                                                       String contractAddress,
                                                       int limit,
                                                       boolean pending,
                                                       boolean administration) {

    address = StringUtils.lowerCase(address);
    contractAddress = StringUtils.lowerCase(contractAddress);
    StringBuilder queryString = new StringBuilder("SELECT tx FROM WalletTransaction tx WHERE tx.networkId = ");
    queryString.append(networkId);

    if (!administration) {
      queryString.append(" AND tx.isAdminOperation = FALSE");
    }

    queryString.append(" AND (tx.fromAddress = '");
    queryString.append(address);
    queryString.append("' OR tx.toAddress = '");
    queryString.append(address);
    queryString.append("' OR tx.byAddress = '");
    queryString.append(address);
    queryString.append("')");

    if (pending) {
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
    return query.getResultList();
  }

  public TransactionEntity getTransactionByHash(String hash) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getTransactionByHash",
                                                                              TransactionEntity.class);
    query.setParameter(HASH_PARAM, StringUtils.lowerCase(hash));
    List<TransactionEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

  public TransactionEntity getAddressLastPendingTransactionSent(long networkId, String address) {
    TypedQuery<TransactionEntity> query =
                                        getEntityManager().createNamedQuery("WalletTransaction.getAddressLastPendingTransactionSent",
                                                                            TransactionEntity.class);
    query.setParameter(ADDRESS_PARAM, StringUtils.lowerCase(address));
    query.setParameter(NETWORK_ID_PARAM, networkId);
    query.setMaxResults(1);

    List<TransactionEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

}
