package org.exoplatform.addon.wallet.service;

import java.util.Set;

import org.exoplatform.addon.wallet.model.*;

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
   * Save wallet private key for a wallet identified by identity type and
   * remoteId
   * 
   * @param type 'user' or 'space'
   * @param remoteId username or space pretty name
   * @param content crypted private key
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
   * Retrieve wallet by address
   * 
   * @param address address of wallet to retrieve
   * @return {@link Wallet} wallet details identified by type and remote Id
   */
  Wallet getWalletByAddress(String address);

  /**
   * Save wallet to storage
   * 
   * @param wallet wallet to save
   */
  void saveWallet(Wallet wallet);

  /**
   * Save wallet address to currentUser or to a space managed by current user
   * 
   * @param wallet {@link Wallet} wallet details to save
   * @param currentUser current username saving wallet details
   * @param broadcast broadcast saving event or not
   * @throws IllegalAccessException when the current user is not able to save a
   *           new address to the wallet
   */
  void saveWalletAddress(Wallet wallet, String currentUser, boolean broadcast) throws IllegalAccessException;

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
   * @throws IllegalAccessException if current user is not an administrator
   */
  void enableWalletByAddress(String address, boolean enable, String currentUser) throws IllegalAccessException;

  /**
   * Throws an exception if the user is not allowed to modify wallet information
   * 
   * @param wallet wallet details to save
   * @param storedWallet stored wallet in database
   * @param currentUser current username that is making the modification
   * @throws IllegalAccessException if current user is not allowed to modify
   *           wallet
   */
  void checkCanSaveWallet(Wallet wallet, Wallet storedWallet, String currentUser) throws IllegalAccessException;

  /**
   * Return true if user is accessing his wallet or is accessing a space that he
   * manages wallet
   * 
   * @param wallet
   * @param currentUser
   * @return
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
   * Retrieves admin account password from configuration
   */
  String getAdminAccountPassword();

}
