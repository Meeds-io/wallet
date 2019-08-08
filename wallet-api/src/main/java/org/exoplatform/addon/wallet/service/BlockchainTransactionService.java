package org.exoplatform.addon.wallet.service;

import java.io.IOException;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;

public interface BlockchainTransactionService {

  /**
   * Compute contract transaction detail from blockchain. The contract address
   * is to be determined from blockchain transaction
   * 
   * @param transactionDetail
   * @param transactionReceipt
   */
  void computeContractTransactionDetail(TransactionDetail transactionDetail, Object transactionReceipt);

  /**
   * compute transaction detail from blockchain. The transaction can be of type
   * contract transaction or a simple ether transaction
   * 
   * @param transactionDetail
   * @param contractDetail
   * @return {@link TransactionDetail} object with details retrieved from
   *         blockchain
   * @throws InterruptedException
   */
  TransactionDetail computeTransactionDetail(TransactionDetail transactionDetail,
                                             ContractDetail contractDetail) throws InterruptedException;

  /**
   * compute a transaction detail from blockchain using the transaction hash and
   * the contract detail to use. The contract detail can be null, thus it will
   * be retrieved from blockchain transaction
   * 
   * @param hash
   * @param contractDetail
   * @return {@link TransactionDetail} object with details retrieved from
   *         blockchain
   * @throws InterruptedException
   */
  TransactionDetail computeTransactionDetail(String hash, ContractDetail contractDetail) throws InterruptedException;

  /**
   * checks transactions marked as pending in DB and verify their status on
   * blockchain. If mined, the status gets updated on DB, else wait for next
   * trigger time.
   * 
   * @param pendingTransactionMaxDays mas days to keep transactions not sent on
   *          blockchain as pending. If <= 0, no duration check
   * @return number of transactions marked as mined
   */
  int checkPendingTransactions(long pendingTransactionMaxDays);

  /**
   * Scans newly mined blocks in Blockchain to verify if there are transactions
   * on configured token or wallet. If found, save it in DB.
   * 
   * @throws InterruptedException
   * @throws IOException
   */
  void scanNewerBlocks() throws InterruptedException, IOException;

}
