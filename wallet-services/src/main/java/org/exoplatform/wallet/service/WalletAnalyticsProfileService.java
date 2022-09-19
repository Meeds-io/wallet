/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
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
package org.exoplatform.wallet.service;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.RootContainer;

/**
 * A container listener that will allow to add wallet analytics page when
 * feature is enabled
 */
public class WalletAnalyticsProfileService implements Startable {

  @Override
  public void start() {
    if (StringUtils.equals(PropertyManager.getProperty("exo.feature.wallet.analytics.enabled"), "true")) {
      RootContainer.addProfiles(Collections.singleton("wallet-analytics"));
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

}
