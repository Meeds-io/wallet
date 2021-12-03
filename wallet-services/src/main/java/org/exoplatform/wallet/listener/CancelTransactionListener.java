package org.exoplatform.wallet.listener;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTransactionService;

import java.util.List;

import static org.exoplatform.wallet.utils.WalletUtils.TRANSACTION_MODIFIED_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.getCurrentUserId;

public class CancelTransactionListener extends Listener<Object, String> {
    private static final Log LOG = ExoLogger.getLogger(CancelTransactionListener.class);

    private WalletTransactionService walletTransactionService;

    private ListenerService listenerService;

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
        if(pendingTransactions != null){
            pendingTransactions.forEach(transactionDetail -> {
                transactionDetail.setPending(false);
                transactionDetail.setSucceeded(false);
                getTransactionService().saveTransactionDetail(transactionDetail, true);
                try {
                    getListenerService().broadcast(TRANSACTION_MODIFIED_EVENT, null, transactionDetail);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private ListenerService getListenerService() {
        if (listenerService == null) {
            listenerService = CommonsUtils.getService(ListenerService.class);
        }
        return listenerService;
    }

    private WalletTransactionService getTransactionService() {
        if (walletTransactionService == null) {
            walletTransactionService = CommonsUtils.getService(WalletTransactionService.class);
        }
        return walletTransactionService;
    }
}
