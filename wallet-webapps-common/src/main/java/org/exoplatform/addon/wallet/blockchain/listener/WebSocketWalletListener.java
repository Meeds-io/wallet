package org.exoplatform.addon.wallet.blockchain.listener;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.service.WalletWebSocketService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.*;

@Asynchronous
public class WebSocketWalletListener extends Listener<Object, Wallet> {

  private WalletWebSocketService webSocketService;

  @Override
  public void onEvent(Event<Object, Wallet> event) throws Exception {
    Wallet wallet = event.getData();
    getWebSocketService().sendMessage(event.getEventName(), null, true, wallet.getAddress());
  }

  private WalletWebSocketService getWebSocketService() {
    if (webSocketService == null) {
      webSocketService = CommonsUtils.getService(WalletWebSocketService.class);
    }
    return webSocketService;
  }
}
