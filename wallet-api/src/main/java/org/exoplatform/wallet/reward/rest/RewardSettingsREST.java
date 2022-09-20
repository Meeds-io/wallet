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

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardPeriodWithFullDate;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.service.RewardSettingsService;
import org.exoplatform.wallet.utils.RewardUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/wallet/api/reward/settings")
@RolesAllowed("rewarding")
@Tag(name = "/wallet/api/reward/settings", description = "Manage reward module settings")
public class RewardSettingsREST implements ResourceContainer {
  private static final Log      LOG = ExoLogger.getLogger(RewardSettingsREST.class);

  private RewardSettingsService rewardSettingsService;

  public RewardSettingsREST(RewardSettingsService rewardSettingsService) {
    this.rewardSettingsService = rewardSettingsService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Get reward settings",
          method = "GET",
          description = "returns reward settings object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getSettings() {
    try {
      RewardSettings settings = rewardSettingsService.getSettings();
      return Response.ok(settings == null ? new RewardSettings() : settings).build();
    } catch (Exception e) {
      LOG.error("Error getting reward settings", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("save")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Get reward settings",
          method = "POST",
          description = "returns reward settings object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveSettings(@Parameter(description = "Reward settings object", required = true) RewardSettings rewardSettings) {
    if (rewardSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      rewardSettingsService.saveSettings(rewardSettings);
      LOG.info("{} saved reward settings '{}'", getCurrentUserId(), rewardSettings.toString());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Error saving reward settings", e);
      return Response.serverError().build();
    }
  }

  @Path("getDates")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Get dates corresponding to chosen period type and start date",
          method = "GET",
          description = "returns reward period dates object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getRewardDates(
                                 @Parameter(description = "A date with format yyyy-MM-dd", required = true)
                                 @QueryParam("date")
                                 String date) {
    if (StringUtils.isBlank(date)) {
      return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad request sent to server with empty 'date' parameter").build();
    }
    RewardSettings settings = rewardSettingsService.getSettings();
    ZoneId zoneId = settings.zoneId();
    RewardPeriodType rewardPeriodType = settings.getPeriodType();

    ZonedDateTime zonedDateTime = RewardUtils.parseRFC3339ToZonedDateTime(date, zoneId);
    RewardPeriod rewardPeriod = rewardPeriodType.getPeriodOfTime(zonedDateTime);
    RewardPeriodWithFullDate periodWithFullDate = new RewardPeriodWithFullDate(rewardPeriod);
    return Response.ok(periodWithFullDate).build();
  }
}
