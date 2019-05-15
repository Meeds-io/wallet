package org.exoplatform.addon.wallet.storage.cached;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.dao.WalletAccountDAO;
import org.exoplatform.addon.wallet.dao.WalletPrivateKeyDAO;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.WalletCacheKey;
import org.exoplatform.addon.wallet.storage.AccountStorage;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.web.security.codec.CodecInitializer;

public class CachedAccountStorage extends AccountStorage {

  private FutureExoCache<WalletCacheKey, Wallet, Object> walletFutureCache = null;

  public CachedAccountStorage(CacheService cacheService,
                              WalletAccountDAO walletAccountDAO,
                              WalletPrivateKeyDAO privateKeyDAO,
                              CodecInitializer codecInitializer) {
    super(walletAccountDAO, privateKeyDAO, codecInitializer);

    ExoCache<WalletCacheKey, Wallet> walletCache = cacheService.getCacheInstance("wallet.account");

    // Future cache is used for clustered environment improvements (usage of
    // putLocal VS put)
    this.walletFutureCache = new FutureExoCache<>(new Loader<WalletCacheKey, Wallet, Object>() {
      @Override
      public Wallet retrieve(Object context, WalletCacheKey cacheKey) throws Exception {
        if (StringUtils.isBlank(cacheKey.getAddress())) {
          return CachedAccountStorage.super.getWalletByIdentityId(cacheKey.getIdentityId());
        } else {
          return CachedAccountStorage.super.getWalletByAddress(cacheKey.getAddress());
        }
      }
    }, walletCache);
  }

  @Override
  public Wallet getWalletByAddress(String address) {
    Wallet wallet = this.walletFutureCache.get(null, new WalletCacheKey(address));
    return wallet == null ? null : wallet.clone();
  }

  @Override
  public Wallet getWalletByIdentityId(long identityId) {
    Wallet wallet = this.walletFutureCache.get(null, new WalletCacheKey(identityId));
    return wallet == null ? null : wallet.clone();
  }

  @Override
  public void saveWallet(Wallet wallet, boolean isNew) {
    String oldAddress = null;
    if (!isNew) {
      // Retrieve old wallet address
      Wallet oldWallet = getWalletByIdentityId(wallet.getTechnicalId());
      oldAddress = oldWallet == null ? null : oldWallet.getAddress();
    }
    super.saveWallet(wallet, isNew);

    // Remove cached wallet
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getAddress()));
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getTechnicalId()));
    if (StringUtils.isNotBlank(oldAddress) && !StringUtils.equalsIgnoreCase(oldAddress, wallet.getAddress())) {
      this.walletFutureCache.remove(new WalletCacheKey(oldAddress));
    }
  }

  @Override
  public Wallet removeWallet(long identityId) {
    Wallet wallet = super.removeWallet(identityId);

    // Remove cached wallet
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getAddress()));
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getTechnicalId()));
    return wallet;
  }

  @Override
  public void removeWalletPrivateKey(long walletId) {
    super.removeWalletPrivateKey(walletId);

    Wallet wallet = getWalletByIdentityId(walletId);
    if (wallet != null) {
      // Remove cached wallet for 'hasKeyOnServerSide' property
      this.walletFutureCache.remove(new WalletCacheKey(wallet.getAddress()));
      this.walletFutureCache.remove(new WalletCacheKey(wallet.getTechnicalId()));
    }
  }

  @Override
  public void saveWalletPrivateKey(long walletId, String content) {
    super.saveWalletPrivateKey(walletId, content);

    Wallet wallet = getWalletByIdentityId(walletId);
    if (wallet != null) {
      // Remove cached wallet for 'hasKeyOnServerSide' property
      this.walletFutureCache.remove(new WalletCacheKey(wallet.getAddress()));
      this.walletFutureCache.remove(new WalletCacheKey(wallet.getTechnicalId()));
    }
  }

}
