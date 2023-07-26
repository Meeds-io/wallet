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

import javax.persistence.TypedQuery;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.wallet.entity.WalletEntity;

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
    query.setParameter("address", StringUtils.lowerCase(address));
    List<WalletEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

  public List<WalletEntity> findListByAddress(String address) {
    TypedQuery<WalletEntity> query = getEntityManager().createNamedQuery("Wallet.findByAddress",
                                                                         WalletEntity.class);
    query.setParameter("address", StringUtils.lowerCase(address));
    return query.getResultList();
  }

}
