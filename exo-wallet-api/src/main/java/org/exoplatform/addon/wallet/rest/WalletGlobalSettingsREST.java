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

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.GlobalSettings;
import org.exoplatform.addon.wallet.service.WalletService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load global settings such as
 * ethereum network and default contracts
 */
@Path("/wallet/api/global-settings")
public class WalletGlobalSettingsREST implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(WalletGlobalSettingsREST.class);

  private WalletService    walletService;

  public WalletGlobalSettingsREST(WalletService walletService) {
    this.walletService = walletService;
  }

  /**
   * Get global settings of aplication
   * 
   * @param networkId used blockchain network id
   * @param spaceId space wallet id to display
   * @return REST response with global settings with current user preferences
   *         and current space settings
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getSettings(@QueryParam("networkId") Long networkId, @QueryParam("spaceId") String spaceId) {
    try {
      GlobalSettings globalSettings = walletService.getSettings(networkId, spaceId, getCurrentUserId());
      return Response.ok(globalSettings.toJSONString(true)).build();
    } catch (Exception e) {
      LOG.warn("Error getting settings for network {} and spaceId {}", networkId, spaceId, e);
      return Response.status(403).build();
    }
  }

  /**
   * Save global settings
   * 
   * @param globalSettings global settings details to save
   * @return REST response with status
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("save")
  @RolesAllowed("administrators")
  public Response saveSettings(GlobalSettings globalSettings) {
    if (globalSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(globalSettings.getProviderURL())) {
      LOG.warn("Bad request sent to server with empty setting 'providerURL'");
      return Response.status(400).build();
    }
    if (globalSettings.getDefaultNetworkId() == null) {
      LOG.warn("Bad request sent to server with empty setting 'defaultNetworkId'");
      return Response.status(400).build();
    }

    if (globalSettings.getDefaultGas() == null) {
      LOG.warn("Bad request sent to server with empty setting 'defaultGas'");
      return Response.status(400).build();
    }

    try {
      walletService.saveSettings(globalSettings);
      LOG.info("{} saved wallet settings details '{}'", getCurrentUserId(), globalSettings.toJSONString(false));
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error saving global settings", e);
      return Response.serverError().build();
    }
  }
}
