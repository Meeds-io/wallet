package org.exoplatform.addon.wallet.storage;

import static org.exoplatform.addon.wallet.utils.WalletUtils.computeWalletFromIdentity;
import static org.exoplatform.addon.wallet.utils.WalletUtils.getIdentityById;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.dao.WalletAccountDAO;
import org.exoplatform.addon.wallet.dao.WalletPrivateKeyDAO;
import org.exoplatform.addon.wallet.entity.WalletEntity;
import org.exoplatform.addon.wallet.entity.WalletPrivateKeyEntity;
import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

public class WalletStorage {
  private static final Log    LOG = ExoLogger.getLogger(WalletStorage.class);

  private WalletAccountDAO    walletAccountDAO;

  private WalletPrivateKeyDAO privateKeyDAO;

  private AbstractCodec       codec;

  public WalletStorage(WalletAccountDAO walletAccountDAO, WalletPrivateKeyDAO privateKeyDAO, CodecInitializer codecInitializer) {
    this.walletAccountDAO = walletAccountDAO;
    this.privateKeyDAO = privateKeyDAO;

    try {
      this.codec = codecInitializer.getCodec();
    } catch (TokenServiceInitializationException e) {
      LOG.error("Error initializing codecs", e);
    }
  }

  /**
   * Retrieves the list registered wallets
   * 
   * @return {@link Set} of {@link Wallet} details with associated addresses
   */
  public Set<Wallet> listWallets() {
    List<WalletEntity> walletEntities = walletAccountDAO.findAll();
    if (walletEntities == null || walletEntities.isEmpty()) {
      return Collections.emptySet();
    } else {
      return walletEntities.stream().map(this::fromEntity).collect(Collectors.toSet());
    }
  }

  /**
   * @return associated wallets counts
   */
  public long getWalletsCount() {
    return walletAccountDAO.count();
  }

  /**
   * @param identityId user/space technical identty id
   * @return {@link Wallet} details for identity
   */
  public Wallet getWalletByIdentityId(long identityId) {
    WalletEntity walletEntity = walletAccountDAO.find(identityId);
    if (walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }

  /**
   * @param address wallet address
   * @return {@link Wallet} details identified by address
   */
  public Wallet getWalletByAddress(String address) {
    WalletEntity walletEntity = walletAccountDAO.findByAddress(address.toLowerCase());
    if (walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }

  /**
   * @param wallet wallet details to save
   * @param isNew whether this is a new wallet association or not
   */
  public void saveWallet(Wallet wallet, boolean isNew) {
    WalletEntity walletEntity = toEntity(wallet);

    if (isNew) {
      walletAccountDAO.create(walletEntity);
    } else {
      walletAccountDAO.update(walletEntity);
    }
  }

  /**
   * Removes a wallet identitied by user/space identity technical id
   * 
   * @param identityId user/space technical identty id
   * @return removed {@link Wallet}
   */
  public Wallet removeWallet(long identityId) {
    WalletEntity walletEntity = walletAccountDAO.find(identityId);
    walletAccountDAO.delete(walletEntity);
    return fromEntity(walletEntity);
  }

  public String getWalletPrivateKey(long walletId) {
    WalletEntity walletEntity = walletAccountDAO.find(walletId);
    if (walletEntity != null && walletEntity.getPrivateKey() != null
        && StringUtils.isNotBlank(walletEntity.getPrivateKey().getKeyContent())) {
      String privateKey = walletEntity.getPrivateKey().getKeyContent();
      return decodeWalletKey(privateKey);
    }
    return null;
  }

  public void removeWalletPrivateKey(long walletId) {
    WalletEntity walletEntity = walletAccountDAO.find(walletId);
    WalletPrivateKeyEntity privateKey = walletEntity == null ? null : walletEntity.getPrivateKey();
    if (privateKey != null) {
      privateKeyDAO.delete(privateKey);
    }
  }

  public void saveWalletPrivateKey(long walletId, String content) {
    WalletEntity walletEntity = walletAccountDAO.find(walletId);
    if (walletEntity == null) {
      throw new IllegalStateException("Wallet with id " + walletId + " wasn't found");
    }
    WalletPrivateKeyEntity privateKey = walletEntity.getPrivateKey();
    if (privateKey == null) {
      privateKey = new WalletPrivateKeyEntity();
      privateKey.setId(null);
      privateKey.setWallet(walletEntity);
    } else if (StringUtils.isNotBlank(privateKey.getKeyContent())) {
      LOG.info("Replacing wallet {}/{} private key", walletEntity.getType(), walletEntity.getId());
    }
    privateKey.setKeyContent(encodeWalletKey(content));
    if (privateKey.getId() == null) {
      privateKeyDAO.create(privateKey);
    } else {
      privateKeyDAO.update(privateKey);
    }
  }

  private String decodeWalletKey(String content) {
    return this.codec.decode(content);
  }

  private String encodeWalletKey(String content) {
    return this.codec.encode(content);
  }

  private Wallet fromEntity(WalletEntity walletEntity) {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(walletEntity.getId());
    wallet.setAddress(walletEntity.getAddress());
    wallet.setPassPhrase(walletEntity.getPassPhrase());
    wallet.setEnabled(walletEntity.isEnabled());
    wallet.setInitializationState(walletEntity.getInitializationState().name());
    wallet.setHasPrivateKey(walletEntity.getPrivateKey() != null);

    Identity identity = getIdentityById(walletEntity.getId());
    computeWalletFromIdentity(wallet, identity);
    return wallet;
  }

  private WalletEntity toEntity(Wallet wallet) {
    WalletEntity walletEntity = new WalletEntity();
    walletEntity.setId(wallet.getTechnicalId());
    walletEntity.setAddress(wallet.getAddress().toLowerCase());
    walletEntity.setEnabled(wallet.isEnabled());
    walletEntity.setInitializationState(WalletInitializationState.valueOf(wallet.getInitializationState()));
    walletEntity.setPassPhrase(wallet.getPassPhrase());
    walletEntity.setType(WalletType.getType(wallet.getType()));
    return walletEntity;
  }

}
