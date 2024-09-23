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
package io.meeds.wallet.reward.rest;


import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import io.meeds.wallet.wallet.model.reward.RewardPeriod;
import io.meeds.wallet.wallet.model.reward.RewardPeriodType;
import io.meeds.wallet.wallet.model.reward.RewardPeriodWithFullDate;
import io.meeds.wallet.wallet.model.reward.RewardSettings;
import io.meeds.wallet.reward.service.RewardSettingsService;
import io.meeds.wallet.wallet.utils.RewardUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("settings/reward")
@Tag(name = "reward/settings", description = "Manage reward module settings") // NOSONAR
public class RewardSettingsREST {

  private static final Log      LOG = ExoLogger.getLogger(RewardSettingsREST.class);

  @Autowired
  private RewardSettingsService rewardSettingsService;

  @GetMapping
  @Secured("rewarding")
  @Operation(
          summary = "Get reward settings",
          method = "GET",
          description = "returns reward settings object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public RewardSettings getSettings() {
    return rewardSettingsService.getSettings();
  }

  @PostMapping
  @Secured("rewarding")
  @Operation(
          summary = "Get reward settings",
          method = "POST",
          description = "returns reward settings object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveSettings(@RequestBody
                               RewardSettings rewardSettings) {
    try {
      rewardSettingsService.saveSettings(rewardSettings);
      return Response.noContent().build();
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @DeleteMapping
  @Secured("rewarding")
  @Operation(summary = "Deletes reward settings.", description = "Deletes reward settings.", method = "DELETE")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public void deleteSettings() {
    rewardSettingsService.deleteSettings();
  }

  @GetMapping(path = "getDates")
  @Secured("users")
  @Operation(
          summary = "Get dates corresponding to chosen period type and start date",
          method = "GET",
          description = "returns reward period dates object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getRewardDates(@Parameter(description = "A date with format yyyy-MM-dd", required = true)
                                 @RequestParam("date")
                                 String date) {
    if (StringUtils.isBlank(date)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request sent to server with empty 'date' parameter");
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
