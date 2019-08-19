package org.exoplatform.addon.wallet.blockchain.listener;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.service.WalletWebSocketService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.*;

public class WebSocketContractListener extends Listener<Object, ContractDetail> {

  private WalletWebSocketService webSocketService;

  @Override
  public void onEvent(Event<Object, ContractDetail> event) throws Exception {
    ContractDetail contractDetail = event.getData();
    getWebSocketService().sendMessage(event.getEventName(), null, true, contractDetail);
  }

  private WalletWebSocketService getWebSocketService() {
    if (webSocketService == null) {
      webSocketService = CommonsUtils.getService(WalletWebSocketService.class);
    }
    return webSocketService;
  }

}
