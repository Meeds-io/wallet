package org.exoplatform.wallet.blockchain.listener;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletWebSocketService;

public class WebSocketTransactionListener extends Listener<Object, TransactionDetail> {

  private WalletWebSocketService webSocketService;

  @Override
  public void onEvent(Event<Object, TransactionDetail> event) throws Exception {
    TransactionDetail transactionDetail = event.getData();
    getWebSocketService().sendMessage(event.getEventName(), null, true, transactionDetail.getHash());
  }

  private WalletWebSocketService getWebSocketService() {
    if (webSocketService == null) {
      webSocketService = CommonsUtils.getService(WalletWebSocketService.class);
    }
    return webSocketService;
  }
}
