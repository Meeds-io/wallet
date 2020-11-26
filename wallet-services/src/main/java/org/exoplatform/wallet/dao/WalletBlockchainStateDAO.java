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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.entity.WalletBlockchainStateEntity;

public class WalletBlockchainStateDAO extends GenericDAOJPAImpl<WalletBlockchainStateEntity, Long> {

  private static final Log LOG = ExoLogger.getLogger(WalletBlockchainStateDAO.class);

  public WalletBlockchainStateEntity findByWalletIdAndContract(long walletId, String contractAddress) {
    TypedQuery<WalletBlockchainStateEntity> query =
                                                  getEntityManager().createNamedQuery("WalletBlockchainState.findByWalletIdAndContract",
                                                                                      WalletBlockchainStateEntity.class);
    query.setParameter("walletId", walletId);
    query.setParameter("contractAddress", contractAddress);
    try {
      List<WalletBlockchainStateEntity> resultList = query.getResultList();
      if (resultList == null || resultList.isEmpty()) {
        return null;
      } else {
        if (resultList.size() > 1) {
          LOG.debug("Multiple WalletBlockchainStateEntity was found for wallet with id {}", walletId);
        }
        return resultList.get(0);
      }
    } catch (NoResultException e) {
      return null;
    }
  }

}
