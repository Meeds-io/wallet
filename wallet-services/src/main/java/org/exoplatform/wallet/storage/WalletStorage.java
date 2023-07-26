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
package org.exoplatform.wallet.storage;

import static org.exoplatform.wallet.utils.WalletUtils.computeWalletFromIdentity;
import static org.exoplatform.wallet.utils.WalletUtils.getIdentityById;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.dao.*;
import org.exoplatform.wallet.entity.*;
import org.exoplatform.wallet.model.*;
import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.CodecInitializer;

public class WalletStorage {
  private static final Log         LOG = ExoLogger.getLogger(WalletStorage.class);

  private final WalletAccountDAO         walletAccountDAO;

  private final WalletAccountBackupDAO   walletAccountBackupDAO;

  private final WalletPrivateKeyDAO      privateKeyDAO;

  private final WalletBlockchainStateDAO blockchainStateDAO;

  private AbstractCodec            codec;

  public WalletStorage(WalletAccountDAO walletAccountDAO,
                       WalletAccountBackupDAO walletAccountBackupDAO, WalletPrivateKeyDAO privateKeyDAO,
                       WalletBlockchainStateDAO blockchainStateDAO,
                       CodecInitializer codecInitializer) {
    this.walletAccountDAO = walletAccountDAO;
    this.walletAccountBackupDAO = walletAccountBackupDAO;
    this.privateKeyDAO = privateKeyDAO;
    this.blockchainStateDAO = blockchainStateDAO;

    if (codecInitializer != null) {
      try {
        this.codec = codecInitializer.getCodec();
      } catch (Exception e) {
        LOG.error("Error initializing codecs", e);
      }
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
      return walletEntities.stream()
                           .map(this::fromEntity)
                           .filter(wallet -> StringUtils.isNotBlank(wallet.getType()))
                           .collect(Collectors.toSet());
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
   * @param contractAddress contract address to use for wallet blockchain state
   * @return {@link Wallet} details for identity
   */
  public Wallet getWalletByIdentityId(long identityId, String contractAddress) {
    WalletEntity walletEntity = walletAccountDAO.find(identityId);
    if (walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }

  /**
   * @param address wallet address
   * @param contractAddress contract address to use for wallet blockchain state
   * @return {@link Wallet} details identified by address
   */
  public Wallet getWalletByAddress(String address, String contractAddress) {
    WalletEntity walletEntity = walletAccountDAO.findByAddress(address);
    if (walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }

  /**
   * Get wallet blockchain state from internal database
   * 
   * @param wallet object to refresh
   * @param contractAddress contract address to use for wallet blockchain state
   */
  public void retrieveWalletBlockchainState(Wallet wallet, String contractAddress) {
    if (wallet == null) {
      throw new IllegalArgumentException("wallet is mandatory");
    }

    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalArgumentException("contractAddress is mandatory");
    }

    WalletBlockchainStateEntity blockchainStateEntity = blockchainStateDAO.findByWalletIdAndContract(wallet.getTechnicalId(),
                                                                                                     contractAddress);
    if (blockchainStateEntity != null) {
      wallet.setEtherBalance(blockchainStateEntity.getEtherBalance());
      wallet.setTokenBalance(blockchainStateEntity.getTokenBalance());
      wallet.setIsInitialized(blockchainStateEntity.isInitialized());
    }
  }

  /**
   * @param wallet wallet details to save
   * @param isNew whether this is a new wallet association or not
   * @return saved wallet entity
   * @throws AddressAlreadyInUseException when the address is already used by another wallet
   */
  public Wallet saveWallet(Wallet wallet, boolean isNew) throws AddressAlreadyInUseException {
    WalletEntity walletEntity = toEntity(wallet);
    if (StringUtils.isNotBlank(wallet.getAddress())) {
      List<WalletEntity> walletEntities = walletAccountDAO.findListByAddress(wallet.getAddress());
      if (CollectionUtils.isNotEmpty(walletEntities)
          && walletEntities.stream()
                           .anyMatch(w -> w.getId() != wallet.getTechnicalId()
                               && w.getInitializationState() != WalletState.DELETED)) {
        throw new AddressAlreadyInUseException();
      }
    }
    if (isNew) {
      walletEntity = walletAccountDAO.create(walletEntity);
    } else {
      walletEntity = walletAccountDAO.update(walletEntity);
    }
    return fromEntity(walletEntity);
  }

  /**
   * Change wallet backup state
   * 
   * @param identityId user/space technical identty id
   * @param backupState true if backedUp else false
   * @return modified {@link Wallet}
   */
  public Wallet saveWalletBackupState(long identityId, boolean backupState) {
    WalletEntity walletEntity = walletAccountDAO.find(identityId);
    walletEntity.setBackedUp(backupState);
    walletEntity = walletAccountDAO.update(walletEntity);
    return fromEntity(walletEntity);
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

  /**
   * Find Wallet encoded private key by wallet identifier.
   * 
   * @param walletId wallet unique identifier that is equals to identity ID
   * @return private key encoded content
   */
  public String getWalletPrivateKey(long walletId) {
    WalletPrivateKeyEntity privateKeyEntity = privateKeyDAO.findByWalletId(walletId);
    if (privateKeyEntity != null && StringUtils.isNotBlank(privateKeyEntity.getKeyContent())) {
      String privateKey = privateKeyEntity.getKeyContent();
      return decodeWalletKey(privateKey);
    }
    return null;
  }

  /**
   * Remove Wallet encoded private key by wallet identifier
   * 
   * @param walletId wallet unique identifier that is equals to identity ID
   */
  public void removeWalletPrivateKey(long walletId) {
    WalletPrivateKeyEntity privateKeyEntity = privateKeyDAO.findByWalletId(walletId);
    if (privateKeyEntity != null) {
      WalletEntity wallet = privateKeyEntity.getWallet();
      privateKeyDAO.delete(privateKeyEntity);
      wallet.setPrivateKey(null);
      walletAccountDAO.update(wallet);
    }
  }

  /**
   * Save wallet private key
   * 
   * @param walletId wallet unique identifier that is equals to identity ID
   * @param content private key content
   */
  public void saveWalletPrivateKey(long walletId, String content) {
    if (StringUtils.isBlank(content)) {
      throw new IllegalArgumentException("content is mandatory");
    }
    WalletEntity walletEntity = walletAccountDAO.find(walletId);
    if (walletEntity == null) {
      throw new IllegalStateException("Wallet with id " + walletId + " wasn't found");
    }
    WalletPrivateKeyEntity privateKey = walletEntity.getPrivateKey();
    if (privateKey == null) {
      privateKey = new WalletPrivateKeyEntity();
      privateKey.setId(null);
      privateKey.setWallet(walletEntity);
      walletEntity.setPrivateKey(privateKey);
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

  /**
   * Save wallet state in blockchain
   * 
   * @param wallet wallet to save its state
   * @param contractAddress address of the contract on which the state is
   *          associated
   */
  public void saveWalletBlockchainState(Wallet wallet, String contractAddress) {
    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalArgumentException("contractAddress is mandatory");
    }
    if (wallet == null) {
      throw new IllegalArgumentException("wallet is mandatory");
    }
    long walletId = wallet.getTechnicalId();
    if (walletId <= 0) {
      throw new IllegalArgumentException("wallet ID is mandatory");
    }
    WalletBlockchainStateEntity blockchainStateEntity = blockchainStateDAO.findByWalletIdAndContract(walletId, contractAddress);
    boolean isNew = blockchainStateEntity == null;
    if (isNew) {
      blockchainStateEntity = new WalletBlockchainStateEntity();
      WalletEntity walletEntity = walletAccountDAO.find(walletId);
      if (walletEntity == null) {
        throw new IllegalStateException("Can't find wallet with id: " + walletId);
      }
      blockchainStateEntity.setWallet(walletEntity);
    }
    blockchainStateEntity.setContractAddress(contractAddress);
    blockchainStateEntity.setEtherBalance(wallet.getEtherBalance() == null ? 0 : wallet.getEtherBalance());
    blockchainStateEntity.setTokenBalance(wallet.getTokenBalance() == null ? 0 : wallet.getTokenBalance());
    blockchainStateEntity.setInitialized(wallet.getIsInitialized() != null && wallet.getIsInitialized());
    if (isNew) {
      blockchainStateDAO.create(blockchainStateEntity);
    } else {
      blockchainStateDAO.update(blockchainStateEntity);
    }
  }

  private String decodeWalletKey(String content) {
    return this.codec.decode(content);
  }

  private String encodeWalletKey(String content) {
    return this.codec.encode(content);
  }

  /**
   * Checks whether identity has a wallet Backup or not
   * 
   * @param walletId {@link Wallet} unique identifier
   * @return true if an internal wallet address is backed up, else return false
   */
  public boolean hasWalletBackup(long walletId) {
    return walletAccountBackupDAO.findByWalletId(walletId) != null;
  }

  /**
   * Checks whether identity has a wallet or not
   * 
   * @param walletId {@link Wallet} unique identifier
   * @return true if wallet exists for selected identity
   */
  public boolean hasWallet(long walletId) {
    return walletAccountDAO.find(walletId) != null;
  }

  /**
   * Switches {@link Wallet} to internal provider and transaction signer
   * 
   * @param walletId {@link Wallet} unique identifier
   */
  @ExoTransactional
  public void switchToInternalWallet(long walletId) {
    WalletBackupEntity walletBackup = walletAccountBackupDAO.findByWalletId(walletId);
    String address = walletBackup.getAddress();

    WalletEntity walletEntity = walletAccountDAO.find(walletId);
    walletEntity.setAddress(StringUtils.lowerCase(address));
    walletEntity.setProvider(WalletProvider.INTERNAL_WALLET);
    walletEntity.setInitializationState(WalletState.MODIFIED);
    walletAccountDAO.update(walletEntity);
    walletAccountBackupDAO.delete(walletBackup);
  }

  /**
   * Switches {@link Wallet} to a new wallet provider and transaction signer
   * 
   * @param walletId {@link Wallet} unique identifier
   * @param provider {@link WalletProvider} that must be different from Internal Wallet 
   * @param newAddress Selected address provided from new Wallet Provider Tool
   */
  @ExoTransactional
  public void switchToWalletProvider(long walletId, WalletProvider provider, String newAddress) {
    WalletEntity walletEntity = walletAccountDAO.find(walletId);
    if (walletEntity.getProvider() == WalletProvider.INTERNAL_WALLET && walletEntity.getInitializationState() != WalletState.DELETED) {
      WalletBackupEntity walletBackupEntity = walletAccountBackupDAO.findByWalletId(walletId);
      boolean isNew = walletBackupEntity == null;
      if (isNew) {
        walletBackupEntity = new WalletBackupEntity();
        walletBackupEntity.setWallet(walletEntity);
      }
      walletBackupEntity.setAddress(walletEntity.getAddress());
      if (isNew) {
        walletAccountBackupDAO.create(walletBackupEntity);
      } else {
        walletAccountBackupDAO.update(walletBackupEntity);
      }
    }

    walletEntity.setAddress(StringUtils.lowerCase(newAddress));
    walletEntity.setProvider(provider);
    if(walletEntity.getInitializationState() == WalletState.DELETED){
      walletEntity.setInitializationState(WalletState.MODIFIED);
    }
    walletAccountDAO.update(walletEntity);
  }

  private Wallet fromEntity(WalletEntity walletEntity) {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(walletEntity.getId());
    wallet.setAddress(walletEntity.getAddress());
    wallet.setPassPhrase(walletEntity.getPassPhrase());
    wallet.setEnabled(walletEntity.isEnabled());
    wallet.setInitializationState(walletEntity.getInitializationState().name());
    wallet.setBackedUp(walletEntity.isBackedUp());
    wallet.setProvider(walletEntity.getProvider().name());
    if (walletEntity.getPrivateKey() == null) {
      WalletPrivateKeyEntity privateKey = privateKeyDAO.findByWalletId(walletEntity.getId());
      wallet.setHasPrivateKey(privateKey != null);
    } else {
      wallet.setHasPrivateKey(true);
    }

    Identity identity = getIdentityById(walletEntity.getId());
    computeWalletFromIdentity(wallet, identity);
    return wallet;
  }

  private WalletEntity toEntity(Wallet wallet) {
    WalletEntity walletEntity = null;
    if (wallet.getTechnicalId() > 0) {
      walletEntity = walletAccountDAO.find(wallet.getTechnicalId());
    }
    if (walletEntity == null) {
      walletEntity = new WalletEntity();
    }
    walletEntity.setId(wallet.getTechnicalId());
    walletEntity.setAddress(StringUtils.lowerCase(wallet.getAddress()));
    walletEntity.setEnabled(wallet.isEnabled());
    walletEntity.setInitializationState(WalletState.valueOf(wallet.getInitializationState()));
    walletEntity.setPassPhrase(wallet.getPassPhrase());
    walletEntity.setBackedUp(wallet.isBackedUp());
    walletEntity.setType(WalletType.getType(wallet.getType()));
    if (wallet.getProvider() != null) {
      walletEntity.setProvider(WalletProvider.valueOf(wallet.getProvider()));
    } else if (walletEntity.getProvider() == null) {
      walletEntity.setProvider(WalletProvider.INTERNAL_WALLET);
    }
    return walletEntity;
  }

}
