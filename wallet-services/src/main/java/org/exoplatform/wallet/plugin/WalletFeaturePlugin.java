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
package org.exoplatform.wallet.plugin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.commons.api.settings.FeaturePlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.security.*;
import org.exoplatform.wallet.model.settings.GlobalSettings;
import org.exoplatform.wallet.service.WalletService;
import org.exoplatform.wallet.utils.WalletUtils;

/**
 * A plugin added to {@link ExoFeatureService} that determines if 'wallet' is
 * enabled for a user or not
 */
public class WalletFeaturePlugin extends FeaturePlugin {

  private static final String  WALLET_FEATURE_NAME = "wallet";

  private ConversationRegistry conversationRegistry;

  private WalletService        walletService;

  @Override
  public String getName() {
    return WALLET_FEATURE_NAME;
  }

  @Override
  public boolean isFeatureActiveForUser(String featureName, String username) {
    List<StateKey> stateKeys = getConversationRegistry().getStateKeys(username);
    for (StateKey stateKey : stateKeys) {
      ConversationState state = getConversationRegistry().getState(stateKey);
      Boolean walletEnabled = (Boolean) state.getAttribute("wallet.enabled");
      if (walletEnabled != null) {
        return walletEnabled;
      }
    }
    GlobalSettings settings = getWalletService().getSettings();
    if (settings == null) {
      return false;
    }
    String accessPermission = settings.getAccessPermission();
    if (StringUtils.isBlank(accessPermission)) {
      return true;
    }
    boolean walletEnabled = WalletUtils.isUserMemberOfSpaceOrGroupOrUser(username, accessPermission);
    for (StateKey stateKey : stateKeys) {
      ConversationState state = getConversationRegistry().getState(stateKey);
      state.setAttribute("wallet.enabled", walletEnabled);
    }
    return walletEnabled;
  }

  /**
   * The Service can't be injected by constructor to avoid cyclic dependency
   * 
   * @return instance of {@link ConversationRegistry} injected in current
   *         container
   */
  private ConversationRegistry getConversationRegistry() {
    if (conversationRegistry == null) {
      conversationRegistry = CommonsUtils.getService(ConversationRegistry.class);
    }
    return conversationRegistry;
  }

  /**
   * The Service can't be injected by constructor to avoid cyclic dependency
   * 
   * @return instance of {@link WalletService} injected in current container
   */
  private WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }

}
