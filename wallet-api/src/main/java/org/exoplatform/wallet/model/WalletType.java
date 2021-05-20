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
package org.exoplatform.wallet.model;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.wallet.plugin.WalletAdminIdentityProvider;

public enum WalletType {
  USER,
  SPACE,
  ADMIN;

  private String id;

  private String providerId;

  private WalletType() {
    this.id = this.name().toLowerCase();

    switch (this.ordinal()) {
    case 0:
      this.providerId = OrganizationIdentityProvider.NAME;
      break;
    case 1:
      this.providerId = SpaceIdentityProvider.NAME;
      break;
    case 2:
      this.providerId = WalletAdminIdentityProvider.NAME;
      break;
    default:
    }
  }

  public String getId() {
    return id;
  }

  public String getProviderId() {
    return providerId;
  }

  public boolean isSpace() {
    return this == SPACE;
  }

  public boolean isUser() {
    return this == USER;
  }

  public boolean isAdmin() {
    return this == ADMIN;
  }

  public static WalletType getType(String type) {
    if (StringUtils.isBlank(type)) {
      return WalletType.USER;
    }
    switch (type.toUpperCase()) {
    case "SPACE":
      return WalletType.SPACE;
    case "ADMIN":
      return WalletType.ADMIN;
    case "USER":
      return WalletType.USER;
    default:
    }
    switch (type) {
    case SpaceIdentityProvider.NAME:
      return WalletType.SPACE;
    case WalletAdminIdentityProvider.NAME:
      return WalletType.ADMIN;
    case OrganizationIdentityProvider.NAME:
      return WalletType.USER;
    default:
    }
    return WalletType.USER;
  }

  public static boolean isSpace(String type) {
    return getType(type) == SPACE;
  }

  public static boolean isUser(String type) {
    return getType(type) == USER;
  }

  public static boolean isAdmin(String type) {
    return getType(type) == ADMIN;
  }

}
