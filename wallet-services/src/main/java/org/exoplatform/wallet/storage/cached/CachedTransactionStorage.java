package org.exoplatform.wallet.storage.cached;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.wallet.dao.WalletTransactionDAO;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.storage.TransactionStorage;

public class CachedTransactionStorage extends TransactionStorage {

  private FutureExoCache<String, TransactionDetail, Object> transactionFutureCache = null;

  public CachedTransactionStorage(CacheService cacheService, WalletTransactionDAO walletTransactionDAO) {
    super(walletTransactionDAO);

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
