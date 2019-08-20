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

import static org.exoplatform.addon.wallet.utils.StatisticUtils.*;
import static org.exoplatform.addon.wallet.utils.WalletUtils.getContractAddress;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.utils.StatisticUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

/**
 * This listener will be triggered when a new address is associated to a user or
 * a space for statistics collection purpose.
 */
public class WalletCreationListener extends Listener<Wallet, String> {

  @Override
  public void onEvent(Event<Wallet, String> event) throws Exception {
    String contractAddress = getContractAddress();
    if (StringUtils.isBlank(contractAddress)) {
      return;
    }
    String issuer = event.getData();
    Wallet wallet = event.getSource();

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("", wallet);
    parameters.put(LOCAL_SERVICE, "wallet");
    parameters.put(OPERATION, "create_wallet");
    parameters.put(STATUS, "ok");
    parameters.put(STATUS_CODE, "200");
    parameters.put(DURATION, "");
    parameters.put("issuer", issuer);

    StatisticUtils.addStatisticEntry(parameters);
  }

}
