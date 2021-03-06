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
package org.exoplatform.wallet.rest;

import static org.exoplatform.wallet.utils.WalletUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.settings.InitialFundsSettings;
import org.exoplatform.wallet.model.settings.UserSettings;
import org.exoplatform.wallet.service.WalletService;

import io.swagger.annotations.*;

@Path("/wallet/api/settings")
@Api(value = "/wallet/api/settings", description = "Manages wallet module settings") // NOSONAR
public class WalletSettingsREST implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(WalletSettingsREST.class);

  private WalletService    walletService;

  public WalletSettingsREST(WalletService walletService) {
    this.walletService = walletService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Retrieves user settings by including space settings when added in parameters", httpMethod = "GET", produces = "application/json", response = Response.class, notes = "returns user settings object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getSettings(@ApiParam(value = "Space pretty name", required = false) @QueryParam("spaceId") String spaceId,
                              @ApiParam(value = "Space pretty name", required = false) @QueryParam("administration") boolean isAdministration) {
    String currentUser = getCurrentUserId();
    try {
      UserSettings userSettings = walletService.getUserSettings(spaceId, currentUser, isAdministration);
      return Response.ok(userSettings).build();
    } catch (Exception e) {
      LOG.error("Error getting settings for user {} and spaceId {}", currentUser, spaceId, e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveInitialFunds")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Saves initial funds settings", httpMethod = "POST", response = Response.class, consumes = "application/json", notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response saveInitialFundsSettings(@ApiParam(value = "Intial funds settings", required = true) InitialFundsSettings initialFundsSettings) {
    if (initialFundsSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
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
