package org.exoplatform.wallet.service;

import java.io.IOException;

import org.exoplatform.wallet.model.transaction.TransactionDetail;

public interface BlockchainTransactionService {

  /**
   * Scans newly mined blocks in Blockchain to verify if there are transactions
   * on configured token or wallet. If found, save it in DB.
   * 
   * @throws IOException
   */
  void scanNewerBlocks() throws IOException;

  /**
   * Sends raw transactions to blockchain
   */
  void sendRawTransactions();

  /**
   * Checks transaction identified by its hash on blockchain to see if it's
   * mined. If mined, the information will be retrieved from mined transaction
   * and saved on database (by replacing existing while the transaction was
   * pendinginformation for more data integrity)
   * 
   * @param transactionHash
   * @param pendingTransactionFromDatabase
   */
  void checkTransactionStatusOnBlockchain(String transactionHash, boolean pendingTransactionFromDatabase);

  /**
   * This will refresh transaction from blockchain if the transaction is marked
   * as pending or marked as error
   * 
   * @param hash hash of transaction to update
   * @return refreshed {@link TransactionDetail} from blockchain
   */
  TransactionDetail refreshTransactionFromBlockchain(String hash);

  /**
   * Checks that a transaction marked as pending in internal database is valid
   * and available on blockchain. If not and if it has exceede
   * {@link WalletTransactionService#getPendingTransactionMaxDays()}, then mark
   * it as 'failed'. I will be remade as 'Success' if
   * ContractTransactionVerifierJob detects a Contract Log
   * 
   * @param transactionDetail
   */
  void checkPendingTransactionValidity(TransactionDetail transactionDetail);

  /**
   * Refreshes gas price from blockchain and cache it
   * 
   * @return last gas price retrieved from blockchain
   * @throws IOException when an error occurs while requesting gas price from
   *           blockchain
   */
  long refreshBlockchainGasPrice() throws IOException;

  /**
   * checks transactions marked as pending in DB and verify their status on
   * blockchain. If mined, the status gets updated on DB, else wait for next
   * trigger time.
   */
  void checkPendingTransactions();

}
