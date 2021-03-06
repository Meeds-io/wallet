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

import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;

public class WalletAdminIdentityProvider extends IdentityProvider<String> {

  public static final String NAME = "WALLET_ADMIN";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String findByRemoteId(String remoteId) {
    return remoteId;
  }

  @Override
  public Identity createIdentity(String remoteId) {
    return new Identity(NAME, remoteId);
  }

  @Override
  public void populateProfile(Profile profile, String remoteId) {
    // No specific properties
  }

}
