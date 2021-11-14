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

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.reward.service.RewardReportService;
import org.exoplatform.wallet.utils.WalletUtils;

import io.swagger.annotations.*;

@Path("/wallet/api/reward/")
@RolesAllowed("rewarding")
@Api(value = "/wallet/api/reward", description = "Manage wallet rewards") // NOSONAR
public class RewardReportREST implements ResourceContainer {
  private static final Log    LOG = ExoLogger.getLogger(RewardReportREST.class);

  private RewardReportService rewardReportService;

  public RewardReportREST(RewardReportService rewardReportService) {
    this.rewardReportService = rewardReportService;
  }

  @GET
  @Path("compute")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Compute rewards of wallets per a chosen period of time", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns a set of wallet reward object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response computeRewards(@ApiParam(value = "Start date of period in milliseconds", required = true) @QueryParam("periodDateInSeconds") long periodDateInSeconds) {
    if (periodDateInSeconds == 0) {
      periodDateInSeconds = System.currentTimeMillis() / 1000;
    }

    try {
      RewardReport rewardReport = rewardReportService.computeRewards(periodDateInSeconds);
      return Response.ok(rewardReport).build();
    } catch (Exception e) {
      LOG.error("Error getting computed reward", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  @GET
  @Path("send")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Send rewards of wallets per a chosen period of time", httpMethod = "GET", response = Response.class, notes = "return empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response sendRewards(@ApiParam(value = "Start date of period in milliseconds", required = true) @QueryParam("periodDateInSeconds") long periodDateInSeconds) {
    try {
      rewardReportService.sendRewards(periodDateInSeconds, WalletUtils.getCurrentUserId());
      return Response.noContent().build();
    } catch (Exception e) {
      LOG.error("Error getting computed reward", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  @GET
  @Path("list")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Return list of rewards for current user with a limit of items to return", httpMethod = "GET", produces = "application/json", response = Response.class, notes = "return list of rewards per user")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response listRewards(@ApiParam(value = "limit of items to load", required = false) @QueryParam("limit") int limit) {
    try {
      List<WalletReward> rewards = rewardReportService.listRewards(WalletUtils.getCurrentUserId(), limit);
      return Response.ok(rewards).build();
    } catch (Exception e) {
      LOG.error("Error getting list of reward for current user", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  @GET
  @Path("countRewards")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Return sum of rewards for current user", httpMethod = "GET", produces = "application/json", response = Response.class, notes = "return sum of rewards per user")
  @ApiResponses(value = {
          @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
          @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
          @ApiResponse(code = 500, message = "Internal server error") })
  public Response countRewards(@Context Request request) {
    try {
      Double sumRewards = rewardReportService.countRewards(WalletUtils.getCurrentUserId());
      EntityTag eTag = new EntityTag(String.valueOf(sumRewards));
      Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
      if (builder == null) {
        JSONObject result = new JSONObject();
        result.put("sumRewards", sumRewards);
        builder = Response.ok(result.toString(), MediaType.APPLICATION_JSON);
        builder.tag(eTag);
      }
      return builder.build();
    } catch (Exception e) {
      LOG.error("Error getting sum of reward for current user", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

}
