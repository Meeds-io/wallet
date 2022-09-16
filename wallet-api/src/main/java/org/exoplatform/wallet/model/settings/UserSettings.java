/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.wallet.model.settings;

import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.utils.WalletUtils;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class UserSettings extends GlobalSettings {

  private static final long serialVersionUID     = -1053053050527461491L;

  private Wallet            wallet               = null;

  @Exclude
  private WalletSettings    userPreferences;

  @Exclude
  private boolean           walletEnabled        = true;

  @Exclude
  private boolean           metamaskEnabled      = false;

  /**
   * @deprecated since the blockchains uses EIP-1559, the gasPrice has to be
   *             always dynamic. This is kept for backward compatibility
   * @since 15 Sept. 2022
   */
  @Exclude
  @Deprecated(since = "Versions starting from 15 Sept. 2022", forRemoval = false)
  private boolean           isUseDynamicGasPrice = true; // NOSONAR

  @Exclude
  private String            cometdChannel        = WalletUtils.COMETD_CHANNEL;

  @Exclude
  private String            cometdToken;

  @Exclude
  private String            cometdContext;

  public UserSettings(GlobalSettings globalSettings) {
    super(globalSettings);
    setNetwork(getNetwork().clone());
    getNetwork().setWebsocketProviderURL(null);
  }
}
