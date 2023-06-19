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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.Wallet;

@RunWith(MockitoJUnitRunner.class)
public class GamificationWalletInitializationListenerTest {

  private static final String   WALLET_ADDRESS = "0x123...7FED";

  private static final long     IDENTITY_ID    = 555l;

  @Mock
  private ListenerService       listenerService;

  @Mock
  private Wallet                wallet;

  @Mock
  private Event<Object, String> event;

  @Test
  public void testInitializeWallet() throws Exception {
    GamificationWalletInitializationListener gamificationListener = new GamificationWalletInitializationListener(listenerService);
    when(wallet.getTechnicalId()).thenReturn(IDENTITY_ID);
    when(wallet.getAddress()).thenReturn(WALLET_ADDRESS);
    when(event.getSource()).thenReturn(wallet);

    gamificationListener.onEvent(event);

    verify(listenerService,
           times(1)).broadcast(eq(GAMIFICATION_BROADCAST_ACTION_EVENT),
                               argThat((Map<String, String> source) -> source.get(GAMIFICATION_EVENT_ID)
                                                                             .equals(GAMIFICATION_CREATE_WALLET_EVENT)
                                   && source.get(GAMIFICATION_EARNER_ID)
                                            .equals(String.valueOf(IDENTITY_ID))
                                   && source.get(GAMIFICATION_RECEIVER_ID)
                                            .equals(String.valueOf(IDENTITY_ID))
                                   && source.get(GAMIFICATION_OBJECT_TYPE)
                                            .equals(GAMIFICATION_WALLET_OBJECT_TYPE)
                                   && source.get(GAMIFICATION_OBJECT_ID)
                                            .equals(WALLET_ADDRESS)),
                               eq(null));

  }

}
