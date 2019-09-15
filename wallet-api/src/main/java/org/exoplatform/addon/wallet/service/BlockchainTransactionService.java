package org.exoplatform.addon.wallet.service;

import java.io.IOException;

import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;

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
   * Checks that a transaction marked as pending in internal database is valid
   * and available on blockchain. If not and if it has exceede
   * {@link WalletTransactionService#getPendingTransactionMaxDays()}, then mark
   * it as 'failed'. I will be remade as 'Success' if
   * ContractTransactionVerifierJob detects a Contract Log
   * 
   * @param transactionDetail
   */
  void checkPendingTransactionValidity(TransactionDetail transactionDetail);

}
