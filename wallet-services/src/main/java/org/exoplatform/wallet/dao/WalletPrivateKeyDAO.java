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
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.WalletPrivateKeyEntity;
import org.exoplatform.wallet.model.WalletProvider;

public class WalletPrivateKeyDAO extends GenericDAOJPAImpl<WalletPrivateKeyEntity, Long> {

  public WalletPrivateKeyEntity findByWalletId(long walletId) {
    TypedQuery<WalletPrivateKeyEntity> query = getEntityManager().createNamedQuery("WalletKey.findByWalletId",
                                                                                   WalletPrivateKeyEntity.class);
    query.setParameter("walletId", walletId);
    query.setParameter("provider", WalletProvider.MEEDS_WALLET);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(List<WalletPrivateKeyEntity> entities) {
    throw new UnsupportedOperationException();
  }

}
