package org.exoplatform.wallet.service;

import java.util.List;
import java.util.Locale;

import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.model.transaction.TransactionStatistics;

/**
 * Manage stored transaction details in eXo internal datasource
 */
public interface WalletTransactionService {

  /**
   * @return {@link List} of pending {@link TransactionDetail} marked as pending
   *         on blockchain
   */
  public List<TransactionDetail> getPendingTransactions();

  /**
   * @param address wallet address
   * @param contractAddress contract address to use to filter transactions
   * @param contractMethodName the contract method name to use to filter on
   *          transactions
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
  List<TransactionDetail> getTransactions(String address,
                                          String contractAddress,
                                          String contractMethodName,
                                          String hash,
                                          int limit,
                                          boolean onlyPending,
                                          boolean administration,
                                          String currentUser) throws IllegalAccessException;

  /**
   * Retrives the Transaction statistics of a user on a designated contract by
   * period of time
   * 
   * @param address
   * @param periodicity
   * @param selectedDate
   * @param locale
   * @return {@link TransactionStatistics} with sent and received amounts and
   *         labels
   */
  TransactionStatistics getTransactionStatistics(String address,
                                                 String periodicity,
                                                 String selectedDate,
                                                 Locale locale);

  /**
   * @param hash transaction hash
   * @return the transaction detail corresponding to the hash parameter,
   *         retrieved from internal database
   */
  TransactionDetail getTransactionByHash(String hash);

  /**
   * @param hash transaction hash
   * @param currentUser current username that is getting transaction details
   * @return the transaction detail corresponding to the hash parameter,
   *         retrieved from internal database
   */
  TransactionDetail getTransactionByHash(String hash, String currentUser);

  /**
   * @param fromAddress
   * @param currentUserId
   * @return the next nonce to include in transaction to send on blockchain. If
   *         no pending transaction return 0 to let get nonce from blockchain
   *         directly for more consistency.
   * @throws IllegalAccessException when user is not owner of wallet address
   */
  long getNonce(String fromAddress, String currentUserId) throws IllegalAccessException;

  /**
   * @param fromAddress
   * @return the next nonce to include in transaction to send on blockchain. If
   *         no pending transaction return 0 to let get nonce from blockchain
   *         directly for more consistency.
   */
  long getNonce(String fromAddress);

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
   * @throws IllegalAccessException if current user is not allowed to save
   *           transaction to sender and receiver wallet
   */
  void saveTransactionDetail(TransactionDetail transactionDetail, String currentUser) throws IllegalAccessException;

  /**
   * @return max days to wait until marking a non existing transaction on
   *         blockchain as failed
   */
  long getPendingTransactionMaxDays();

  /**
   * @return {@link List} of {@link TransactionDetail} having
   *         {@link TransactionDetail#getRawTransaction()} to send on blockchain
   */
  List<TransactionDetail> getTransactionsToSend();

  /**
   * Determines whether the user can send transaction to blockchain or not. In
   * fact, this will avoid to send mutiple parallel transactions to the
   * blockchain and to avoid transaction nonce collision. A transaction can be
   * replaced by another one when it uses the same nonce and with a higher gas
   * price. The problem here is that we can't determine for sure (using API that
   * access to the blockchain) the next available nonce to use for transaction
   * to send (inconsistent information can be retrieved from blockchain when
   * tens of thousands of transactions are pending on blockchain when we use
   * eth_getTransactionCount(address, 'pending')).
   * 
   * @param senderAddress wallet address of transaction sender
   * @return true if address can send a transaction to blockchain.
   */
  boolean canSendTransactionToBlockchain(String senderAddress);

  /**
   * @return max attempts of sending a transaction
   */
  long getMaxAttemptsToSend();

  /**
   * @return true if all transactions including uknown ones detected on contract
   *         has to be logged, else false
   */
  boolean isLogAllTransaction();

}
