/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.wallet.listener;

import static org.exoplatform.addon.wallet.statistic.StatisticUtils.*;
import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.statistic.StatisticUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * This listener will be triggered when a wallet state is modified. This is made
 * for statistics collection purpose.
 */
public class WalletStateListener extends Listener<Wallet, String> {
  private static final Log LOG = ExoLogger.getLogger(WalletStateListener.class);

  @Override
  public void onEvent(Event<Wallet, String> event) throws Exception {
    String issuer = event.getData();
    Wallet wallet = event.getSource();

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("", wallet);
    parameters.put(LOCAL_SERVICE, "wallet");

    String eventName = event.getEventName();
    if (StringUtils.equals(WALLET_ENABLED_EVENT, eventName)) {
      parameters.put(OPERATION, "enable");
    } else if (StringUtils.equals(WALLET_DISABLED_EVENT, eventName)) {
      parameters.put(OPERATION, "disable");
    } else if (StringUtils.equals(WALLET_INITIALIZATION_MODIFICATION_EVENT, eventName)) {
      if (StringUtils.equalsIgnoreCase(wallet.getInitializationState(), WalletInitializationState.DENIED.name())) {
        parameters.put(OPERATION, "reject");
      } else {
        LOG.debug("Wallet state modification not handeled: {}", wallet.getInitializationState());
        return;
      }
    } else {
      LOG.warn("Event name not handled: {}", eventName);
      return;
    }
    parameters.put(STATUS, "ok");
    parameters.put(STATUS_CODE, "200");
    parameters.put(DURATION, "");
    if (StringUtils.isNotBlank(issuer)) {
      Identity identity = getIdentityByTypeAndId(WalletType.USER, issuer);
      if (identity == null) {
        LOG.warn("Can't find identity with remote id: {}" + issuer);
      } else {
        parameters.put("user_social_id", identity);
      }
    }

    StatisticUtils.addStatisticEntry(parameters);
  }

}
