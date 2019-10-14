package org.exoplatform.wallet.service;

import java.util.Map;
import java.util.Set;

import org.exoplatform.wallet.model.*;

/**
 * A service to manage wallets of different types: user, space, admin
 */
public interface WalletAccountService {

  /**
   * Retrieves the list registered wallets
   * 
   * @return list of associated wallets to users and spaces
   */
  Set<Wallet> listWallets();

  /**
   * Retrieve wallets count
   * 
   * @return associated wallets count
   */
  long getWalletsCount();

  /**
   * Retrieve wallet details by identity technical id
   * 
   * @param identityId User/Space identity technical id
   * @return {@link Wallet} wallet details identified by identity technical id
   */
  Wallet getWalletByIdentityId(long identityId);

  /**
   * Retrieve wallet details by identity type and remoteId accessed by a user
   * 
   * @param type 'user' or 'space'
   * @param remoteId username or space pretty name
   * @param currentUser current username saving wallet private key
   * @return {@link Wallet} wallet details identified by type and remote Id
   */
  Wallet getWalletByTypeAndId(String type, String remoteId, String currentUser);

  /**
   * Retrieve wallet details by identity type and remoteId
   * 
   * @param type 'user' or 'space'
   * @param remoteId username or space pretty name
   * @return {@link Wallet} wallet details identified by type and remote Id
   */
  Wallet getWalletByTypeAndId(String type, String remoteId);

  /**
   * Retrieves admin wallet from internal database
   * 
   * @return {@link Wallet} of administrator
   */
  Wallet getAdminWallet();

  /**
   * Save wallet private key for a wallet identified by identity type and
   * remoteId
   * 
   * @param type 'user' or 'space'
   * @param remoteId username or space pretty name
   * @param content crypted private key
   * @param currentUser current username
   * @throws IllegalAccessException when the current user is not allowed to save
   *           the encrypted private key of wallet
   */
  void savePrivateKeyByTypeAndId(String type,
                                 String remoteId,
                                 String content,
                                 String currentUser) throws IllegalAccessException;

  /**
   * Retrieve wallet private key by identity type and remoteId
   * 
   * @param type 'user' or 'space'
   * @param remoteId username or space pretty name
   * @param currentUser current username getting wallet private key
   * @return encrypted wallet private key identified by type and remote Id
   * @throws IllegalAccessException when the current user is not allowed to get
   *           the encrypted private key of wallet
   */
  String getPrivateKeyByTypeAndId(String type, String remoteId, String currentUser) throws IllegalAccessException;

  /**
   * Retrieve wallet private key by identity type and remoteId
   * 
   * @param type 'user' or 'space'
   * @param remoteId username or space pretty name
   * @return encrypted wallet private key identified by type and remote Id
   */
  String getPrivateKeyByTypeAndId(String type, String remoteId);

  /**
   * Removes wallet private key by identity type and remoteId
   * 
   * @param type 'user' or 'space'
   * @param remoteId username or space pretty name
   * @param currentUser current username removing wallet private key
   * @throws IllegalAccessException when the current user is not an owner of
   *           wallet
   */
  void removePrivateKeyByTypeAndId(String type, String remoteId, String currentUser) throws IllegalAccessException;

  /**
   * Retrieve wallet by address with blockchain state if current user can access
   * wallet data
   * 
   * @param address address of wallet to retrieve
   * @param currentUser current username accessing wallet information
   * @return {@link Wallet} wallet details identified by type and remote Id
   */
  Wallet getWalletByAddress(String address, String currentUser);

  /**
   * Retrieve wallet by address
   * 
   * @param address address of wallet to retrieve
   * @return {@link Wallet} wallet details identified by type and remote Id
   */
  Wallet getWalletByAddress(String address);

  /**
   * Save wallet state on blockchain
   * 
   * @param wallet
   * @param contractAddress
   */
  void saveWalletBlockchainState(Wallet wallet, String contractAddress);

  /**
   * Change wallet backup state
   * 
   * @param identityId user/space technical identty id
   * @param backupState true if backedUp else false
   * @param currentUser current username saving wallet backup state
   * @return modified {@link Wallet}
   * @throws IllegalAccessException when currentUser is not owner of wallet
   */
  Wallet saveWalletBackupState(String currentUser, long identityId, boolean backupState) throws IllegalAccessException;

  /**
   * Save wallet address to currentUser or to a space managed by current user
   * 
   * @param wallet {@link Wallet} wallet details to save
   * @param currentUser current username saving wallet details
   * @throws IllegalAccessException when the current user is not able to save a
   *           new address to the wallet
   */
  void saveWalletAddress(Wallet wallet, String currentUser) throws IllegalAccessException;

  /**
   * Save wallet instance in internal database
   * 
   * @param wallet
   * @param isNew
   * @return save {@link Wallet}
   */
  Wallet saveWallet(Wallet wallet, boolean isNew);

  /**
   * Remove User or Space wallet address association
   * 
   * @param address wallet address association to remove
   * @param currentUser current username removing wallet details
   * @throws IllegalAccessException if current user is not an administrator
   */
  void removeWalletByAddress(String address, String currentUser) throws IllegalAccessException;

  /**
   * Remove wallet address association by type and remote id
   * 
   * @param type USER/SPACE/ADMIN, see {@link WalletType}
   * @param remoteId username or space pretty name
   * @param currentUser current username saving wallet details
   * @throws IllegalAccessException
   */
  void removeWalletByTypeAndId(String type, String remoteId, String currentUser) throws IllegalAccessException;

  /**
   * Enable/Disable User or Space wallet
   * 
   * @param address address of wallet to enable/disable
   * @param enable whether enable or disable wallet
   * @param currentUser username of current user making the operation
   * @return true if modified else false
   * @throws IllegalAccessException if current user is not an administrator
   */
  boolean enableWalletByAddress(String address, boolean enable, String currentUser) throws IllegalAccessException;

  /**
   * @param wallet
   * @param currentUser
   * @return true if user is accessing his wallet or is accessing a space that
   *         he manages wallet
   */
  boolean isWalletOwner(Wallet wallet, String currentUser);

  /**
   * Saves label if label is not empty else, delete it
   * 
   * @param label label details object to process
   * @param currentUser current user making the label
   *          creation/modification/deletion
   * @return {@link WalletAddressLabel} saved or deleted label details
   */
  WalletAddressLabel saveOrDeleteAddressLabel(WalletAddressLabel label, String currentUser);

  /**
   * List of labels that current user can access
   * 
   * @param currentUser current username accessing the list of addresses labels
   * @return a {@link Set} of label details
   */
  Set<WalletAddressLabel> getAddressesLabelsVisibleBy(String currentUser);

  /**
   * Change wallet initialization status
   * 
   * @param address wallet address
   * @param initializationState wallet initialization status of type
   *          {@link WalletInitializationState}
   * @param currentUserId user changing wallet status
   * @throws IllegalAccessException if current user is not allowed to modify
   *           wallet initialization status
   */
  void setInitializationStatus(String address,
                               WalletInitializationState initializationState,
                               String currentUserId) throws IllegalAccessException;

  /**
   * Change wallet initialization status
   * 
   * @param address wallet address
   * @param initializationState wallet initialization status of type
   *          {@link WalletInitializationState}
   */
  void setInitializationStatus(String address, WalletInitializationState initializationState);

  /**
   * Creates admin account wallet in server side
   * 
   * @param privateKey admin account wallet private key
   * @param currentUser current user creating wallet
   * @throws IllegalAccessException if current user is not allowed to create
   *           admin wallet account
   */
  void createAdminAccount(String privateKey, String currentUser) throws IllegalAccessException;

  /**
   * @return admin account password from configuration
   */
  String getAdminAccountPassword();

  /**
   * Refreshes wallets from blockchain
   * 
   * @param walletsModifications modified wallets on blockchain with the set of
   *          invoked methods on contract
   */
  void refreshWalletsFromBlockchain(Map<String, Set<String>> walletsModifications);

  /**
   * Refreshes wallet state from blockchain
   * 
   * @param wallet
   * @param contractDetail
   * @param walletsModifications
   */
  void refreshWalletFromBlockchain(Wallet wallet, ContractDetail contractDetail, Map<String, Set<String>> walletsModifications);

  /**
   * Retrieve wallet state from internal database
   * 
   * @param wallet object to refresh
   */
  void retrieveWalletBlockchainState(Wallet wallet);

  /**
   * @return true if wallet admin is enabled on Token contract else return false
   */
  boolean isAdminAccountEnabled();

}
