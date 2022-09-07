package org.exoplatform.wallet.listener;

import static org.exoplatform.wallet.utils.WalletUtils.getCurrentUserId;

import java.util.List;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;

public class CancelTransactionListener extends Listener<Object, String> {

  private WalletTransactionService walletTransactionService;

  @Override
  public void onEvent(Event<Object, String> event) throws Exception {
    Wallet wallet = (Wallet) event.getSource();
    String currentUserId = getCurrentUserId();
    List<TransactionDetail> pendingTransactions = getTransactionService().getTransactions(wallet.getAddress(),
                                                                                          null,
                                                                                          null,
                                                                                          null,
                                                                                          0,
                                                                                          true,
                                                                                          false,
                                                                                          currentUserId);
    if (pendingTransactions != null) {
      pendingTransactions.forEach(transactionDetail -> {
        transactionDetail.setPending(false);
        transactionDetail.setSucceeded(false);
        getTransactionService().saveTransactionDetail(transactionDetail, true);
      });
    }
  }

  private WalletTransactionService getTransactionService() {
    if (walletTransactionService == null) {
      walletTransactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return walletTransactionService;
  }
}
