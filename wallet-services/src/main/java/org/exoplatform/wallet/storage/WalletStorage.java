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

import org.apache.commons.lang3.StringUtils;

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

  private final WalletPrivateKeyDAO      privateKeyDAO;

  private final WalletBlockchainStateDAO blockchainStateDAO;

  private AbstractCodec            codec;

  public WalletStorage(WalletAccountDAO walletAccountDAO,
                       WalletPrivateKeyDAO privateKeyDAO,
                       WalletBlockchainStateDAO blockchainStateDAO,
                       CodecInitializer codecInitializer) {
    this.walletAccountDAO = walletAccountDAO;
    this.privateKeyDAO = privateKeyDAO;
    this.blockchainStateDAO = blockchainStateDAO;

    try {
      this.codec = codecInitializer.getCodec();
    } catch (Exception e) {
      LOG.error("Error initializing codecs", e);
    }
  }

  /**
   * Retrieves the list registered wallets
   * 
   * @return {@link Set} of {@link Wallet} details with associated addresses
   */
  public Set<Wallet> listWallets() {
    List<WalletEntity> walletEntities = walletAccountDAO.findActiveWallets();
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
   * @param identityId user/space technical identity id
   * @param contractAddress contract address to use for wallet blockchain state
   * @return {@link Wallet} details for identity
   */
  public Wallet getWalletByIdentityId(long identityId, String contractAddress) {
    WalletEntity walletEntity = walletAccountDAO.findByActiveStateAndIdentity(identityId, true);
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
    WalletEntity walletEntity = walletAccountDAO.findByAddress(address.toLowerCase());
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
    WalletEntityKey key = new WalletEntityKey(wallet.getTechnicalId(), WalletProvider.valueOf(wallet.getProvider()));
    WalletBlockchainStateEntity blockchainStateEntity = blockchainStateDAO.findByWalletIdAndContract(key,
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
   */
  public Wallet saveWallet(Wallet wallet, boolean isNew) {
    WalletEntity walletEntity = toEntity(wallet);
    if (isNew) {
      walletEntity = walletAccountDAO.create(walletEntity);
    } else {
      walletEntity = walletAccountDAO.update(walletEntity);
    }
    return fromEntity(walletEntity);
  }

  /**
   * Change wallet backup state, this is a feature for Meeds wallets only
   * 
   * @param identityId user/space technical identty id
   * @param backupState true if backedUp else false
   * @return modified {@link Wallet}
   */
  public Wallet saveWalletBackupState(long identityId, boolean backupState) {
    WalletEntity walletEntity = walletAccountDAO.findByIdentityIdAndProvider(identityId, WalletProvider.MEEDS_WALLET);
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
  public Wallet removeWallet(long identityId, String provider) {
    WalletEntity walletEntity = walletAccountDAO.findByIdentityIdAndProvider(identityId, WalletProvider.valueOf(provider));
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
    WalletEntity walletEntity = walletAccountDAO.findByIdentityIdAndProvider(walletId, WalletProvider.MEEDS_WALLET);
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
    WalletEntityKey key = new WalletEntityKey(wallet.getTechnicalId(), WalletProvider.valueOf(wallet.getProvider()));
    WalletBlockchainStateEntity blockchainStateEntity = blockchainStateDAO.findByWalletIdAndContract(key, contractAddress);
    boolean isNew = blockchainStateEntity == null;
    if (isNew) {
      blockchainStateEntity = new WalletBlockchainStateEntity();
      WalletEntity walletEntity = walletAccountDAO.findByIdentityIdAndProvider(key.getIdentityId(), key.getProvider());
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

  /**
   * find active/inactive wallet by user
   * @param identityId user identity ID
   * @param isActive state of wallet : active or inactive
   * @return Wallet or null
   */
  public Wallet findByUserAndActiveState(String identityId, boolean isActive) {
    Long id = Long.valueOf(identityId);
    WalletEntity walletEntity = walletAccountDAO.findByActiveStateAndIdentity(id, isActive);
    if(walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }


  /**
   * find active/inactive wallet by user
   * @param identityId user identity ID
   * @param provider WalletProvider : Meeds wallet, Metamask etc ...
   * @return Wallet or null
   */
  public Wallet findByIdentityIdAndProvider(long identityId, WalletProvider provider) {
    WalletEntity walletEntity = walletAccountDAO.findByIdentityIdAndProvider(identityId, provider);
    if(walletEntity == null) {
      return null;
    }
    return fromEntity(walletEntity);
  }

  /**
   * Activate wallet from a given provider
   * @param identityId User identity ID
   * @param provider Provider name
   * @return activated wallet
   */
  public Wallet activateWallet(long identityId, WalletProvider provider){
    List<Wallet> userWallets = getUserWallets(identityId);
    Wallet activatedWallet = null;
    for(Wallet wallet : userWallets) {
      if(provider.name().equals(wallet.getProvider())) {
        wallet.setActive(true);
        activatedWallet = saveWallet(wallet, false);
      } else {
        wallet.setActive(false);
        saveWallet(wallet, false);
      }
    }
    return activatedWallet;
  }

  public List<Wallet> getUserWallets(long identityId) {
    List<WalletEntity> walletEntities = walletAccountDAO.findUserWallets(identityId, WalletType.USER);
    if(walletEntities == null || walletEntities.isEmpty()) {
      return Collections.emptyList();
    } else {
      return walletEntities.stream().map(this::fromEntity).collect(Collectors.toList());
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
    wallet.setTechnicalId(walletEntity.getId().getIdentityId());
    wallet.setAddress(walletEntity.getAddress());
    wallet.setPassPhrase(walletEntity.getPassPhrase());
    wallet.setEnabled(walletEntity.isEnabled());
    wallet.setActive(walletEntity.isActive());
    wallet.setInitializationState(walletEntity.getInitializationState().name());
    wallet.setProvider(walletEntity.getId().getProvider().name());
    wallet.setBackedUp(walletEntity.isBackedUp());
    if (walletEntity.getPrivateKey() == null) {
      WalletPrivateKeyEntity privateKey = privateKeyDAO.findByWalletId(walletEntity.getId().getIdentityId());
      wallet.setHasPrivateKey(privateKey != null);
    } else {
      wallet.setHasPrivateKey(true);
    }

    Identity identity = getIdentityById(walletEntity.getId().getIdentityId());
    computeWalletFromIdentity(wallet, identity);
    return wallet;
  }

  private WalletEntity toEntity(Wallet wallet) {
    WalletEntity walletEntity = new WalletEntity();
    WalletEntityKey key = new WalletEntityKey(wallet.getTechnicalId(), WalletProvider.valueOf(wallet.getProvider()));
    walletEntity.setId(key);
    walletEntity.setAddress(wallet.getAddress().toLowerCase());
    walletEntity.setEnabled(wallet.isEnabled());
    walletEntity.setActive(wallet.isActive());
    walletEntity.setInitializationState(WalletState.valueOf(wallet.getInitializationState()));
    walletEntity.setPassPhrase(wallet.getPassPhrase());
    walletEntity.setBackedUp(wallet.isBackedUp());
    walletEntity.setType(WalletType.getType(wallet.getType()));
    return walletEntity;
  }

}
