package org.exoplatform.wallet.blockchain.listener;

import org.exoplatform.wallet.blockchain.service.EthereumClientConnector;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;

public class TransactionSentToBlockchainListener extends Listener<Object, TransactionDetail> {

  private WalletTransactionService walletTransactionService;

  private EthereumClientConnector web3jConnector;

  public TransactionSentToBlockchainListener(WalletTransactionService walletTransactionService,
                                 EthereumClientConnector web3jConnector) {
    this.walletTransactionService = walletTransactionService;
    this.web3jConnector = web3jConnector;
  }

  @Override
  public void onEvent(Event<Object, TransactionDetail> event) throws Exception {
    if (!this.web3jConnector.isPermanentlyScanBlockchain()
        && this.walletTransactionService.countPendingTransactions() > 0) {
      long lastestBlockNumber = this.web3jConnector.getLastestBlockNumber();
      this.web3jConnector.renewBlockSubscription(lastestBlockNumber);
    }
  }
}
