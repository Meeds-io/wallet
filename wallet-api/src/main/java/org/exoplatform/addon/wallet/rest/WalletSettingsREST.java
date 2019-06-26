/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.addon.wallet.rest;

import static org.exoplatform.addon.wallet.utils.WalletUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.wallet.model.settings.InitialFundsSettings;
import org.exoplatform.addon.wallet.model.settings.UserSettings;
import org.exoplatform.addon.wallet.service.WalletService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load user settings
 */
@Path("/wallet/api/settings")
public class WalletSettingsREST implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(WalletSettingsREST.class);

  private WalletService    walletService;

  public WalletSettingsREST(WalletService walletService) {
    this.walletService = walletService;
  }

  /**
   * Get global settings of aplication
   * 
   * @param spaceId space wallet id to display
   * @return REST response with global settings with current user preferences
   *         and current space settings
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getSettings(@QueryParam("spaceId") String spaceId) {
    String currentUser = getCurrentUserId();
    try {
      UserSettings userSettings = walletService.getUserSettings(spaceId, currentUser);
      return Response.ok(userSettings).build();
    } catch (Exception e) {
      LOG.error("Error getting settings for user {} and spaceId {}", currentUser, spaceId, e);
      return Response.status(403).build();
    }
  }

  /**
   * Save intial funds settings
   * 
   * @param initialFundsSettings initial funds settings to save
   * @return REST response with status
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveInitialFunds")
  @RolesAllowed("rewarding")
  public Response saveInitialFundsSettings(InitialFundsSettings initialFundsSettings) {
    if (initialFundsSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
    }

    try {
      walletService.saveInitialFundsSettings(initialFundsSettings);
      LOG.info("{} saved initialFunds settings details '{}'", getCurrentUserId(), initialFundsSettings.toString());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Error saving global settings", e);
      return Response.serverError().build();
    }
  }
}
