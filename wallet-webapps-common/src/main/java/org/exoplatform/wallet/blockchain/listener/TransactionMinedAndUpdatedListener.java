package org.exoplatform.wallet.blockchain.listener;

import org.exoplatform.services.listener.*;
import org.exoplatform.wallet.blockchain.service.EthereumClientConnector;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;

@Asynchronous
public class TransactionMinedAndUpdatedListener extends Listener<Object, TransactionDetail> {

  private WalletTransactionService walletTransactionService;

  private EthereumClientConnector  web3jConnector;

  public TransactionMinedAndUpdatedListener(WalletTransactionService walletTransactionService,
                                            EthereumClientConnector web3jConnector) {
    this.walletTransactionService = walletTransactionService;
    this.web3jConnector = web3jConnector;
  }

  @Override
  public void onEvent(Event<Object, TransactionDetail> event) throws Exception {
    TransactionDetail transactionDetail = event.getData();
    if (transactionDetail.isSucceeded() || web3jConnector.getTransaction(transactionDetail.getHash()) != null) {
      walletTransactionService.cancelTransactionsWithSameNonce(transactionDetail);
    }

    if (!this.web3jConnector.isPermanentlyScanBlockchain() && this.walletTransactionService.countPendingTransactions() == 0) {
      this.web3jConnector.stopListeningToBlockchain();
    }
  }
}
