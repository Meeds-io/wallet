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
package org.exoplatform.wallet.storage.cached;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.dao.WalletTransactionDAO;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.storage.TransactionStorage;

public class CachedTransactionStorage extends TransactionStorage {

  private FutureExoCache<String, TransactionDetail, Object> transactionFutureCache = null;

  public CachedTransactionStorage(CacheService cacheService, ListenerService listenerService, WalletTransactionDAO walletTransactionDAO) {
    super(listenerService, walletTransactionDAO);

    ExoCache<String, TransactionDetail> transactionCache = cacheService.getCacheInstance("wallet.transaction");

    // Future cache is used for clustered environment improvements (usage of
    // putLocal VS put)
    this.transactionFutureCache = new FutureExoCache<>(new Loader<String, TransactionDetail, Object>() {
      @Override
      public TransactionDetail retrieve(Object context, String hash) throws Exception {
        return CachedTransactionStorage.super.getTransactionByHash(hash);
      }
    }, transactionCache);
  }

  @Override
  public TransactionDetail getTransactionByHash(String hash) {
    TransactionDetail transactionDetail = this.transactionFutureCache.get(null, hash.toLowerCase());
    return transactionDetail == null ? null : transactionDetail.clone();
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    super.saveTransactionDetail(transactionDetail);
    this.transactionFutureCache.remove(transactionDetail.getHash().toLowerCase());
  }

  public void clearCache() {
    transactionFutureCache.clear();
  }

}
