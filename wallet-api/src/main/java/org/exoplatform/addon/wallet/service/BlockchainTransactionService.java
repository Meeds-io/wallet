package org.exoplatform.addon.wallet.service;

import java.io.IOException;

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

}
