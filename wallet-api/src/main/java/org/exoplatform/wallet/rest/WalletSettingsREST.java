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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.settings.InitialFundsSettings;
import org.exoplatform.wallet.model.settings.UserSettings;
import org.exoplatform.wallet.service.WalletService;


@Path("/wallet/api/settings")
@Tag(name = "/wallet/api/settings", description = "Manage wallet settings operations")
public class WalletSettingsREST implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(WalletSettingsREST.class);

  private WalletService    walletService;

  public WalletSettingsREST(WalletService walletService) {
    this.walletService = walletService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Retrieves user settings by including space settings when added in parameters",
          method = "GET",
          description = "Retrieves user settings by including space settings when added in parameters")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getSettings(@Parameter(description = "Space pretty name") @QueryParam("spaceId") String spaceId,
                              @Parameter(description = "Space pretty name") @QueryParam("administration") boolean isAdministration) {
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
  @Operation(
          summary = "Saves initial funds settings",
          method = "POST",
          description = "Saves initial funds settings and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveInitialFundsSettings(@Parameter(description = "Initial funds settings", required = true) InitialFundsSettings initialFundsSettings) {
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
