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
package org.exoplatform.wallet.reward.rest;

import static org.exoplatform.wallet.utils.RewardUtils.getCurrentUserId;

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
import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.reward.service.RewardTeamService;


@Path("/wallet/api/reward/team")
@RolesAllowed("rewarding")
@Tag(name = "/wallet/api/reward/team", description = "Manage reward teams (pools)")
public class RewardTeamREST implements ResourceContainer {
  private static final Log  LOG = ExoLogger.getLogger(RewardTeamREST.class);

  private RewardTeamService rewardTeamService;

  public RewardTeamREST(RewardTeamService rewardTeamService) {
    this.rewardTeamService = rewardTeamService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("list")
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Get reward teams with their members",
          method = "GET",
          description= "returns the list of reward team objects")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response listTeams() {
    try {
      return Response.ok(rewardTeamService.getTeams()).build();
    } catch (Exception e) {
      LOG.error("Error getting computed reward", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("remove")
  @RolesAllowed("rewarding")
  @Operation(summary = "Remove a reward team", method = "GET", description = "Remove a reward team and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response removeTeam(@Parameter(description = "Reward team technical id", required = true) @QueryParam("id") Long id) {
    if (id == null || id == 0) {
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      RewardTeam team = rewardTeamService.removeTeam(id);
      LOG.info("{} removed reward pool {}", getCurrentUserId(), team.toString());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Error removing reward pool with id: " + id, e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("save")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Save a reward team",
          method = "POST",
          description = "Save a reward team and returns the saved reward team object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveTeam(@Parameter(description = "Reward team object", required = true) RewardTeam rewardTeam) {
    if (rewardTeam == null) {
      LOG.warn("Bad request sent to server with empty team");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      rewardTeam = rewardTeamService.saveTeam(rewardTeam);
      LOG.info("{} saved reward pool {}", getCurrentUserId(), rewardTeam.getName());
      return Response.ok(rewardTeam).build();
    } catch (Exception e) {
      LOG.error("Error saving reward pool", e);
      return Response.serverError().build();
    }
  }

}
