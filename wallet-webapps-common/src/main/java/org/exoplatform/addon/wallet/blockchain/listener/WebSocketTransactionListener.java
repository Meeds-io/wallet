package org.exoplatform.addon.wallet.blockchain.listener;

import org.json.JSONObject;

import org.exoplatform.addon.wallet.service.WalletWebSocketService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.*;

@Asynchronous
public class WebSocketTransactionListener extends Listener<Object, JSONObject> {

  private WalletWebSocketService webSocketService;

  @Override
  public void onEvent(Event<Object, JSONObject> event) throws Exception {
    JSONObject transactionDetail = event.getData();
    getWebSocketService().sendMessage(event.getEventName(), null, true, transactionDetail.get("hash"));
  }

  private WalletWebSocketService getWebSocketService() {
    if (webSocketService == null) {
      webSocketService = CommonsUtils.getService(WalletWebSocketService.class);
    }
    return webSocketService;
  }
}
