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

  private WalletAccountDAO         walletAccountDAO;

  private WalletPrivateKeyDAO      privateKeyDAO;

  private WalletBlockchainStateDAO blockchainStateDAO;

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
    Wallet wallet = fromEntity(walletEntity);
    if (StringUtils.isNotBlank(contractAddress)) {
      retrieveWalletApprovalBlockchainState(wallet, contractAddress);
    }
    return wallet;
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
    Wallet wallet = fromEntity(walletEntity);
    if (StringUtils.isNotBlank(contractAddress)) {
      retrieveWalletApprovalBlockchainState(wallet, contractAddress);
    }
    return wallet;
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
      wallet.setRewardBalance(blockchainStateEntity.getRewardBalance());
      wallet.setVestingBalance(blockchainStateEntity.getVestingBalance());
      wallet.setAdminLevel(blockchainStateEntity.getAdminLevel());
      wallet.setIsApproved(blockchainStateEntity.isApproved());
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
    blockchainStateEntity.setRewardBalance(wallet.getRewardBalance() == null ? 0 : wallet.getRewardBalance());
    blockchainStateEntity.setVestingBalance(wallet.getVestingBalance() == null ? 0 : wallet.getVestingBalance());
    blockchainStateEntity.setAdminLevel(wallet.getAdminLevel() == null ? 0 : wallet.getAdminLevel());
    blockchainStateEntity.setApproved(wallet.getIsApproved() != null && wallet.getIsApproved());
    blockchainStateEntity.setInitialized(wallet.getIsInitialized() != null && wallet.getIsInitialized());
    if (isNew) {
      blockchainStateDAO.create(blockchainStateEntity);
    } else {
      blockchainStateDAO.update(blockchainStateEntity);
    }
  }

  private void retrieveWalletApprovalBlockchainState(Wallet wallet, String contractAddress) {
    if (wallet == null) {
      throw new IllegalArgumentException("wallet is mandatory");
    }

    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalArgumentException("contractAddress is mandatory");
    }

    WalletBlockchainStateEntity blockchainStateEntity = blockchainStateDAO.findByWalletIdAndContract(wallet.getTechnicalId(),
                                                                                                     contractAddress);
    if (blockchainStateEntity != null) {
      wallet.setIsApproved(blockchainStateEntity.isApproved());
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
    wallet.setBackedUp(walletEntity.isBackedUp());
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
    WalletEntity walletEntity = new WalletEntity();
    walletEntity.setId(wallet.getTechnicalId());
    walletEntity.setAddress(wallet.getAddress().toLowerCase());
    walletEntity.setEnabled(wallet.isEnabled());
    walletEntity.setInitializationState(WalletInitializationState.valueOf(wallet.getInitializationState()));
    walletEntity.setPassPhrase(wallet.getPassPhrase());
    walletEntity.setBackedUp(wallet.isBackedUp());
    walletEntity.setType(WalletType.getType(wallet.getType()));
    return walletEntity;
  }

}
