package org.exoplatform.wallet.blockchain.listener;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.BlockchainTransactionService;
import org.exoplatform.wallet.service.WalletTransactionService;

/**
 * A listener that will be triggered when a transaction is effectively sent to
 * blockchain through internal or external wallet provider
 */
public class TransactionSentToBlockchainListener extends Listener<Object, TransactionDetail> {

  private WalletTransactionService     walletTransactionService;

  private BlockchainTransactionService blockchainTransactionService;

  public TransactionSentToBlockchainListener(WalletTransactionService walletTransactionService,
                                             BlockchainTransactionService blockchainTransactionService) {
    this.walletTransactionService = walletTransactionService;
    this.blockchainTransactionService = blockchainTransactionService;
  }

  @Override
  public void onEvent(Event<Object, TransactionDetail> event) throws Exception {
    if (this.walletTransactionService.countContractPendingTransactionsSent() > 0) {
      this.blockchainTransactionService.startWatchingBlockchain();
    }
  }

}
