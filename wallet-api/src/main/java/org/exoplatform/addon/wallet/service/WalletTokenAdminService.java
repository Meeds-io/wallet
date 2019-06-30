package org.exoplatform.addon.wallet.service;

import java.math.BigInteger;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;

/**
 * Communicates with blockchain to send transactions and retrieve information
 * using admin wallet
 */
public interface WalletTokenAdminService {

  /**
   * Generates admin account wallet and store it internally in eXo Server
   */
  void createAdminAccount();

  /**
   * Creates admin account wallet using provided private key and store it
   * internally in eXo Server
   * 
   * @param privateKey admin account wallet private key
   * @param issuerUsername current user creating wallet
   * @throws IllegalAccessException if current user is not allowed to create
   *           admin wallet account
   */
  void createAdminAccount(String privateKey, String issuerUsername) throws IllegalAccessException;

  /**
   * @return Admin wallet object
   */
  Wallet getAdminWallet();

  /**
   * @return Admin wallet address
   */
  String getAdminWalletAddress();

  /**
   * Send rewarded token amounts (on blockchain) to a receiver wallet address
   * using 'Admin' wallet. The amount sent could be different from rewarded
   * amount. A label and a message are associated to the transaction. Those
   * properties aren't sent on blockchain, but stored in database. The
   * transaction issuer will be stored in transaction details stored internally
   * in eXo server.
   * 
   * @param transactionDetail
   * @param issuerUsername
   * @return
   * @throws Exception
   */
  TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  /**
   * Initializes (on blockchain) a receiver wallet address using 'Admin' wallet
   * by using funds transmitted in transaction detail. The transaction issuer,
   * label and message will be stored in transaction details inside eXo server
   * only.
   * 
   * @param transactionDetail
   * @param issuerUsername
   * @return
   * @throws Exception
   */
  TransactionDetail initialize(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  /**
   * Send ether (on blockchain) to a receiver wallet address using 'Admin'
   * wallet. The transaction issuer, label and message will be stored in
   * transaction details inside eXo server only.
   * 
   * @param transactionDetail
   * @param issuerUsername
   * @return
   * @throws Exception
   */
  TransactionDetail sendEther(TransactionDetail transactionDetail, String issuerUsername) throws Exception;

  /**
   * Get token balance of a wallet address (on blockchain)
   * 
   * @param address
   * @return
   * @throws Exception
   */
  BigInteger balanceOf(String address) throws Exception;// NOSONAR

  /**
   * Get ether balance of a wallet address (on blockchain)
   * 
   * @param address
   * @return
   * @throws Exception
   */
  BigInteger getEtherBalanceOf(String address) throws Exception;// NOSONAR

  /**
   * Checks whether the wallet is initialized or not (on blockchain)
   * 
   * @param address
   * @return
   * @throws Exception
   */
  boolean isInitializedAccount(String address) throws Exception;// NOSONAR

  /**
   * Checks whether a wallet address is an admin on token with at least level 1
   * (on blockchain)
   * 
   * @param address
   * @return
   * @throws Exception
   */
  boolean isAdminAccount(String address) throws Exception;// NOSONAR

  /**
   * Get admin level of a wallet address from token (on blockchain)
   * 
   * @param address
   * @return
   * @throws Exception
   */
  int getAdminLevel(String address) throws Exception;// NOSONAR

  /**
   * Checks if a wallet address is approved on token (on blockchain)
   * 
   * @param address
   * @return
   * @throws Exception
   */
  boolean isApprovedAccount(String address) throws Exception;// NOSONAR

  /**
   * Retrieves contract details from blockchain, like: - Sell price - Owner -
   * Symbol - Name ...
   * 
   * @param contractAddress
   * @return
   */
  ContractDetail getContractDetailFromBlockchain(String contractAddress);

}
