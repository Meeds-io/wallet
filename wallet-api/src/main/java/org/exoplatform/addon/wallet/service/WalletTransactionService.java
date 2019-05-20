package org.exoplatform.addon.wallet.service;

import java.util.List;
import java.util.Set;

import org.exoplatform.addon.wallet.model.TransactionDetail;

public interface WalletTransactionService {

  /**
   * @param networkId blockchain network id
   * @return {@link List} of pending {@link TransactionDetail}
   */
  public List<TransactionDetail> getPendingTransactions(long networkId);

  /**
   * @param networkId blockchain network id
   * @return transactions hashes that are marked as pensing in internal database
   */
  public Set<String> getPendingTransactionHashes(long networkId);

  /**
   * @param networkId blockchain network id
   * @param address wallet address
   * @param contractAddress contract address to use to filter transactions
   * @param hash the transaction hash to include in resulted transactions
   * @param limit limit size of returned transactions unless the hash parameter
   *          is not null, in that case, continue searching in transactions list
   *          until the hash is included in results
   * @param onlyPending whether filtering on pending transactions only or not
   * @param administration include administration transactions or not
   * @param currentUser the user accessing the list of transactions
   * @return the list of transactions corresponding to filter parameters
   * @throws IllegalAccessException if the current user isn't allowed to access
   *           wallet transactions
   */
  public List<TransactionDetail> getTransactions(long networkId,
                                                 String address,
                                                 String contractAddress,
                                                 String hash,
                                                 int limit,
                                                 boolean onlyPending,
                                                 boolean administration,
                                                 String currentUser) throws IllegalAccessException;

  /**
   * @param hash transaction hash
   * @return the transaction detail corresponding to the hash parameter,
   *         retrieved from internal database
   */
  public TransactionDetail getTransactionByHash(String hash);

  /**
   * @param networkId blockchain network id
   * @param address wallet address
   * @param currentUser user accessing last pending transaction of wallet
   * @return last transaction marked as pending in internal database
   *         corresponding to the wallet identified by an address
   * @throws IllegalAccessException if the current user is not an admin and is
   *           not the owner of the wallet
   */
  public TransactionDetail getAddressLastPendingTransactionSent(long networkId,
                                                                String address,
                                                                String currentUser) throws IllegalAccessException;

  /**
   * Save transaction details in database
   * 
   * @param transactionDetail transaction detail to save
   * @param broadcastMinedTransaction whether the transaction has been mined on
   *          blockchain or not
   */
  void saveTransactionDetail(TransactionDetail transactionDetail, boolean broadcastMinedTransaction);

  /**
   * Save transaction details in database
   *
   * @param transactionDetail transaction detail to save
   * @param currentUser current username that is saving transaction
   * @param broadcastMinedTransaction whether the transaction has been mined on
   *          blockchain or not
   * @throws IllegalAccessException if current user is not allowed to save
   *           transaction to sender and receiver wallet
   */
  public void saveTransactionDetail(TransactionDetail transactionDetail,
                                    String currentUser,
                                    boolean broadcastMinedTransaction) throws IllegalAccessException;

  /**
   * @return watched transactions count treated since the server startup
   */
  public long getWatchedTreatedTransactionsCount();

  /**
   * @return max days to wait until marking a non existing transaction on
   *         blockchain as failed
   */
  public long getPendingTransactionMaxDays();

}
