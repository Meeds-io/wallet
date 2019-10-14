package org.exoplatform.wallet.service;

import static org.exoplatform.wallet.utils.WalletUtils.COMETD_CHANNEL;
import static org.exoplatform.wallet.utils.WalletUtils.toJsonString;

import java.util.Collection;

import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.WebSocketMessage;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;

public class WalletWebSocketService {

  private static final Log      LOG = ExoLogger.getLogger(WalletWebSocketService.class);

  private ContinuationService   continuationService;

  private EXoContinuationBayeux continuationBayeux;

  private String                cometdContextName;

  public void sendMessage(String eventId, Collection<String> recipientUsers, boolean sendToAll, Object... objects) {
    getContinuationService();

    WebSocketMessage messageObject = new WebSocketMessage(eventId, objects);
    String message = toJsonString(messageObject);

    if (sendToAll) {
      continuationService.sendBroadcastMessage(COMETD_CHANNEL, message);
    } else if (recipientUsers != null && !recipientUsers.isEmpty()) {
      for (String recipientUser : recipientUsers) {
        if (continuationService.isPresent(recipientUser)) {
          continuationService.sendMessage(recipientUser, COMETD_CHANNEL, message);
        }
      }
    }
  }

  protected String getCometdContextName() {
    if (cometdContextName == null) {
      getContinuationBayeux();
      cometdContextName = (continuationBayeux == null ? "cometd" : continuationBayeux.getCometdContextName());
    }
    return cometdContextName;
  }

  protected String getUserToken(String username) {
    try {
      if (getContinuationService() != null) {
        return getContinuationService().getUserToken(username);
      }
    } catch (Exception e) {
      LOG.warn("Could not retrieve continuation token for user " + username, e);
    }
    return "";
  }

  private EXoContinuationBayeux getContinuationBayeux() {
    if (continuationBayeux == null) {
      continuationBayeux = CommonsUtils.getService(EXoContinuationBayeux.class);
    }
    return continuationBayeux;
  }

  private ContinuationService getContinuationService() {
    if (continuationService == null) {
      continuationService = CommonsUtils.getService(ContinuationService.class);
    }
    return continuationService;
  }
}
