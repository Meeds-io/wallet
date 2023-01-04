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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardPeriodWithFullDate;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.reward.service.RewardReportService;
import org.exoplatform.wallet.reward.service.RewardSettingsService;
import org.exoplatform.wallet.utils.RewardUtils;
import org.exoplatform.wallet.utils.WalletUtils;


@Path("/wallet/api/reward/")
@RolesAllowed("rewarding")
@Tag(name = "/wallet/api/reward", description = "Manage wallet rewards")
public class RewardReportREST implements ResourceContainer {
  private static final String ERROR_PARAM = "error";

  private static final Log    LOG = ExoLogger.getLogger(RewardReportREST.class);

  private RewardReportService   rewardReportService;

  private RewardSettingsService rewardSettingsService;

  public RewardReportREST(RewardReportService rewardReportService, RewardSettingsService rewardSettingsService) {
    this.rewardReportService = rewardReportService;
    this.rewardSettingsService = rewardSettingsService;
  }

  @GET
  @Path("compute")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Compute rewards of wallets per a chosen period of time",
          method = "GET",
          description = "returns a set of wallet reward object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response computeRewards(
                                 @Parameter(description = "A date with format yyyy-MM-dd", required = true)
                                 @QueryParam("date")
                                 String date) {
    if (StringUtils.isBlank(date)) {
      return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad request sent to server with empty 'date' parameter").build();
    }
    try {
      RewardPeriod rewardPeriod = getRewardPeriod(date);
      RewardReport rewardReport = rewardReportService.computeRewards(rewardPeriod.getPeriodMedianDate());
      rewardReport.setPeriod(new RewardPeriodWithFullDate(rewardReport.getPeriod()));
      return Response.ok(rewardReport).build();
    } catch (Exception e) {
      LOG.error("Error getting computed reward", e);
      JSONObject object = new JSONObject();
      try {
        object.append(ERROR_PARAM, e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  @GET
  @Path("compute/user")
  @RolesAllowed("users")
  @Operation(
          summary = "Compute rewards of user wallet per a chosen period of time",
          method = "GET",
          description = "returns a wallet reward object")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response computeRewardsByUser(
                                      @Parameter(description = "A date with format yyyy-MM-dd", required = true)
                                      @QueryParam("date")
                                      String date) {
    if (StringUtils.isBlank(date)) {
      return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad request sent to server with empty 'date' parameter").build();
    }
    try {
      RewardPeriod rewardPeriod = getRewardPeriod(date);
      RewardReport rewardReport = rewardReportService.computeRewardsByUser(rewardPeriod.getPeriodMedianDate(), WalletUtils.getCurrentUserIdentityId());
      rewardReport.setPeriod(new RewardPeriodWithFullDate(rewardReport.getPeriod()));
      return Response.ok(rewardReport).build();
    } catch (Exception e) {
      LOG.error("Error getting user's computed reward", e);
      JSONObject object = new JSONObject();
      try {
        object.append(ERROR_PARAM, e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  @GET
  @Path("send")
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Send rewards of wallets per a chosen period of time",
          method = "GET",
          description = "Send rewards of wallets per a chosen period of time and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response sendRewards(
                              @Parameter(description = "A date with format yyyy-MM-dd", required = true)
                              @QueryParam("date")
                              String date) {
    try {
      if (StringUtils.isBlank(date)) {
        return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad request sent to server with empty 'date' parameter").build();
      }
      RewardPeriod rewardPeriod = getRewardPeriod(date);
      rewardReportService.sendRewards(rewardPeriod.getPeriodMedianDate(), WalletUtils.getCurrentUserId());
      return Response.noContent().build();
    } catch (Exception e) {
      LOG.error("Error getting computed reward", e);
      JSONObject object = new JSONObject();
      try {
        object.append(ERROR_PARAM, e.getMessage());
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
  @Operation(
          summary = "Return list of rewards for current user with a limit of items to return",
          method = "GET",
          description = "return list of rewards per user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response listRewards(@Parameter(description = "limit of items to load") @QueryParam("limit") int limit) {
    try {
      List<WalletReward> rewards = rewardReportService.listRewards(WalletUtils.getCurrentUserId(), limit);
      rewards.forEach(reward -> reward.setPeriod(new RewardPeriodWithFullDate(reward.getPeriod())));
      return Response.ok(rewards).build();
    } catch (Exception e) {
      LOG.error("Error getting list of reward for current user", e);
      JSONObject object = new JSONObject();
      try {
        object.append(ERROR_PARAM, e.getMessage());
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
  @Operation(
          summary = "Return sum of rewards for user",
          method = "GET",
          description = "return sum of rewards per user")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response countRewards(@Context Request request, @Parameter(description = "user id", required = true) @QueryParam("userId") String userId ) {
    try {
      Double sumRewards = rewardReportService.countRewards(userId);
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
        object.append(ERROR_PARAM, e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(HTTPStatus.INTERNAL_ERROR).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  private RewardPeriod getRewardPeriod(String date) {
    RewardSettings settings = rewardSettingsService.getSettings();
    ZoneId zoneId = settings.zoneId();
    RewardPeriodType rewardPeriodType = settings.getPeriodType();

    ZonedDateTime zonedDateTime = RewardUtils.parseRFC3339ToZonedDateTime(date, zoneId);
    return rewardPeriodType.getPeriodOfTime(zonedDateTime);
  }

}
