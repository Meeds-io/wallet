package org.exoplatform.wallet.service;

import java.math.BigInteger;
import java.util.Set;

import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;

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
   * @return created wallet
   * @throws IllegalAccessException if current user is not allowed to create
   *           admin wallet account
   */
  Wallet createAdminAccount(String privateKey, String issuerUsername) throws IllegalAccessException;

  /**
   * @return Admin wallet address
   */
  String getAdminWalletAddress();

  /**
   * @param rawTransaction raw transaction to send to blockchain
   * @return real generated transaction hash using Web3j
   */
  String generateHash(String rawTransaction);

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
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
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
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
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
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  TransactionDetail sendEther(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  /**
   * Send token (on blockchain) to a receiver wallet address using 'Admin'
   * wallet. The transaction issuer, label and message will be stored in
   * transaction details inside eXo server only.
   * 
   * @param transactionDetail
   * @param issuerUsername
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  TransactionDetail sendToken(TransactionDetail transactionDetail, String issuerUsername) throws Exception; // NOSONAR

  /**
   * Get token balance of a wallet address (on blockchain)
   * 
   * @param address
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  BigInteger balanceOf(String address) throws Exception;// NOSONAR

  /**
   * Get ether balance of a wallet address (on blockchain)
   * 
   * @param address
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  BigInteger getEtherBalanceOf(String address) throws Exception;// NOSONAR

  /**
   * Checks whether the wallet is initialized or not (on blockchain)
   * 
   * @param address
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  boolean isInitializedAccount(String address) throws Exception;// NOSONAR

  /**
   * Get admin level of a wallet address from token (on blockchain)
   * 
   * @param address
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  int getAdminLevel(String address) throws Exception;// NOSONAR

  /**
   * Checks if a wallet address is approved on token (on blockchain)
   * 
   * @param address
   * @return {@link TransactionDetail} with the hash of the transaction sent in
   *         blockchain
   * @throws Exception
   */
  boolean isApprovedAccount(String address) throws Exception;// NOSONAR

  /**
   * Retrieves contract details from blockchain, like: - Sell price - Owner -
   * Symbol - Name ...
   * 
   * @param contractDetail existing contract detail retrieved from internal
   *          database to refresh its attributes.
   * @param contractModifications list of called method names to change contract
   *          state on blockchain. This parameter will be used to know which
   *          methods to call to refresh contract state in order to optimize the
   *          number of calls to Blockchain
   */
  void refreshContractDetailFromBlockchain(ContractDetail contractDetail, Set<String> contractModifications);

  /**
   * Retrieves wallet details from blockchain
   * 
   * @param wallet object to refresh
   * @param contractDetail contract details attributes
   * @param walletModifications list of called method names to change wallet
   *          state on blockchain. This parameter will be used to know which
   *          methods to call to refresh wallet state in order to optimize the
   *          number of calls to Blockchain
   * @throws Exception
   */
  void retrieveWalletInformationFromBlockchain(Wallet wallet,
                                               ContractDetail contractDetail,
                                               Set<String> walletModifications) throws Exception; // NOSONAR

}
