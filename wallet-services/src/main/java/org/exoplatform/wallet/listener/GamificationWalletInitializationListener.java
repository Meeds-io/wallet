/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.wallet.listener;

import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_BROADCAST_ACTION_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_CREATE_WALLET_EVENT;
import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_EARNER_ID;
import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_EVENT_ID;
import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_OBJECT_ID;
import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_OBJECT_TYPE;
import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_RECEIVER_ID;
import static org.exoplatform.wallet.utils.WalletUtils.GAMIFICATION_WALLET_OBJECT_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.Wallet;

@Asynchronous
public class GamificationWalletInitializationListener extends Listener<Object, String> {

  private ListenerService listenerService;

  public GamificationWalletInitializationListener(ListenerService listenerService) {
    this.listenerService = listenerService;
  }

  @Override
  public void onEvent(Event<Object, String> event) throws Exception {
    Wallet wallet = (Wallet) event.getSource();
    Map<String, String> gam = new HashMap<>();
    gam.put(GAMIFICATION_EVENT_ID, GAMIFICATION_CREATE_WALLET_EVENT);
    gam.put(GAMIFICATION_EARNER_ID, String.valueOf(wallet.getTechnicalId()));
    gam.put(GAMIFICATION_RECEIVER_ID, String.valueOf(wallet.getTechnicalId()));
    gam.put(GAMIFICATION_OBJECT_TYPE, GAMIFICATION_WALLET_OBJECT_TYPE);
    gam.put(GAMIFICATION_OBJECT_ID, wallet.getAddress());
    listenerService.broadcast(GAMIFICATION_BROADCAST_ACTION_EVENT, gam, null);
  }

}
