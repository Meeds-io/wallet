package org.exoplatform.addon.wallet.service;

import java.io.IOException;

public interface BlockchainTransactionService {

  /**
   * checks transactions marked as pending in DB and verify their status on
   * blockchain. If mined, the status gets updated on DB, else wait for next
   * trigger time.
   */
  void checkPendingTransactions();

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

}
