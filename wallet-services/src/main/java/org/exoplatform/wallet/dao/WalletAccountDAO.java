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
package org.exoplatform.wallet.dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.WalletEntity;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.model.WalletType;

public class WalletAccountDAO extends GenericDAOJPAImpl<WalletEntity, Long> {

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(List<WalletEntity> entities) {
    throw new UnsupportedOperationException();
  }

  public WalletEntity findByAddress(String address) {
    TypedQuery<WalletEntity> query = getEntityManager().createNamedQuery("Wallet.findByAddress",
                                                                         WalletEntity.class);
    query.setParameter("address", address);
    List<WalletEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

  public List<WalletEntity> findActiveWallets() {
    TypedQuery<WalletEntity> query = getEntityManager().createNamedQuery("Wallet.findActiveWallets",
            WalletEntity.class);
    return query.getResultList();
  }
  public WalletEntity findByActiveStateAndIdentity(Long id, boolean isActive) {
    TypedQuery<WalletEntity> query = getEntityManager().createNamedQuery("Wallet.findByActiveStateAndIdentity",
            WalletEntity.class);
    query.setParameter("id", id);
    query.setParameter("active", isActive);
    try {
      return query.getSingleResult();
    } catch (NonUniqueResultException | NoResultException e) {
      return null;
    }
  }

  public List<WalletEntity> findUserWallets(Long id, WalletType type) {
    TypedQuery<WalletEntity> query = getEntityManager().createNamedQuery("Wallet.findByIdentity",
            WalletEntity.class);
    query.setParameter("id", id);
    query.setParameter("type", type);
    return query.getResultList();
  }

  public WalletEntity findByIdentityIdAndProvider(Long identityId, WalletProvider provider) {
    TypedQuery<WalletEntity> query = getEntityManager().createNamedQuery("Wallet.findByIdentityAndProvider",
            WalletEntity.class);
    query.setParameter("id", identityId);
    query.setParameter("provider", provider);
    List<WalletEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }
}
