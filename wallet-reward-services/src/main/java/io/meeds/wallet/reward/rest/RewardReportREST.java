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

import static io.meeds.wallet.utils.RewardUtils.timeToSecondsAtDayStart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.core.*;

import io.meeds.wallet.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.wallet.reward.service.RewardReportService;
import io.meeds.wallet.reward.service.RewardSettingsService;
import io.meeds.wallet.utils.RewardUtils;
import io.meeds.wallet.utils.WalletUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("reward")
@Tag(name = "rewards", description = "Manage wallet rewards") // NOSONAR
public class RewardReportREST {
  
  private static final String   ERROR_PARAM            = "error";

  private static final String   ERROR_EMPTY_PARAM_DATE = "Bad request sent to server with empty 'date' parameter";

  private static final Log      LOG                    = ExoLogger.getLogger(RewardReportREST.class);

  @Autowired
  private RewardReportService   rewardReportService;

  @Autowired
  private RewardSettingsService rewardSettingsService;

  @GetMapping(path = "compute")
  @Secured("rewarding")
  @Operation(
          summary = "Compute rewards of wallets per a chosen period of time",
          method = "GET",
          description = "returns a set of wallet reward object")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public List<RewardReport> computeRewards(@Parameter(description = "Page")
                                           @RequestParam(value = "page", defaultValue = "0", required = false)
                                           int page,
                                           @Parameter(description = "Page size")
                                           @RequestParam(value = "size", defaultValue = "12", required = false)
                                           int size) {
    int skip = page * size;
    List<RewardPeriod> periods = generatePreviousPeriods(skip + size).subList(skip, skip + size);
    List<RewardReport> rewardReports = periods.parallelStream()
                                              .map(period -> {
                                                RewardReport rewardReport =
                                                                          rewardReportService.computeRewards(period.getPeriodMedianDate());
                                                rewardReport.setPeriod(new RewardPeriodWithFullDate(rewardReport.getPeriod()));
                                                return rewardReport;
                                              })
                                              .toList();
    rewardReports.forEach(rewardReport -> rewardReport.setPeriod(new RewardPeriodWithFullDate(rewardReport.getPeriod())));
    return rewardReports;
  }

  @PostMapping(path = "period/compute")
  @Secured("rewarding")
  @Operation(
          summary = "Compute rewards of wallets per a chosen period of time",
          method = "GET",
          description = "returns a set of wallet reward object")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public RewardReport computeRewardsByPeriod(@RequestBody
                                             RewardPeriod rewardPeriod) {
    RewardReport rewardReport = rewardReportService.computeRewards(rewardPeriod.getPeriodMedianDate());
    rewardReport.setPeriod(new RewardPeriodWithFullDate(rewardReport.getPeriod()));
    return rewardReport;
  }

  @PostMapping(path = "forecast")
  @Secured("rewarding")
  @Operation(
          summary = "Compute a distribution forecast corresponding to a chosen reward settings",
          method = "POST",
          description = "returns a distribution forecast corresponding to a chosen reward settings")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public DistributionForecast computeDistributionForecast(@RequestBody
                                             RewardSettings rewardSettings) {
    return rewardReportService.computeDistributionForecast(rewardSettings);
  }

  @GetMapping(path = "compute/user")
  @Secured("users")
  @Operation(
          summary = "Compute rewards of user wallet per a chosen period of time",
          method = "GET",
          description = "returns a wallet reward object")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response computeRewardsByUser(@Parameter(description = "A date with format yyyy-MM-dd", required = true)
                                       @RequestParam("date")
                                       String date) {
    if (StringUtils.isBlank(date)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERROR_EMPTY_PARAM_DATE);
    }
    RewardPeriod rewardPeriod = getRewardPeriod(date);
    RewardReport rewardReport = rewardReportService.computeRewardsByUser(rewardPeriod.getPeriodMedianDate(),
                                                                         WalletUtils.getCurrentUserIdentityId());
    rewardReport.setPeriod(new RewardPeriodWithFullDate(rewardReport.getPeriod()));
    return Response.ok(rewardReport).build();
  }


  @PostMapping(path = "send")
  @Secured("rewarding")
  @Operation(
          summary = "Send rewards of wallets per a chosen period of time",
          method = "GET",
          description = "Send rewards of wallets per a chosen period of time and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public void sendRewards(HttpServletRequest request,
                          @RequestBody
                          RewardPeriod rewardPeriod) {
    try {
      rewardReportService.sendRewards(rewardPeriod.getPeriodMedianDate(), request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  @GetMapping(path = "list")
  @Secured("users")
  @Operation(
          summary = "Return list of rewards for current user with a limit of items to return",
          method = "GET",
          description = "return list of rewards per user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response listRewards(@Parameter(description = "limit of items to load", required = true)
                              @RequestParam("limit")
                              int limit) {
    List<WalletReward> rewards = rewardReportService.listRewards(WalletUtils.getCurrentUserId(), limit);
    rewards.forEach(reward -> reward.setPeriod(new RewardPeriodWithFullDate(reward.getPeriod())));
    return Response.ok(rewards).build();
  }

  @GetMapping(path = "countRewards")
  @Secured("users")
  @Operation(
          summary = "Return sum of rewards for user",
          method = "GET",
          description = "return sum of rewards per user")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ResponseEntity<Double> countRewards(HttpServletRequest request,
                                             @Parameter(description = "user id", required = true)
                                             @RequestParam("userId")
                                             String userId) {
    Double sumRewards = rewardReportService.countRewards(userId);
    String eTagValue = String.valueOf(sumRewards.hashCode());

    String requestETag = request.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (requestETag != null && requestETag.equals(eTagValue)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    return ResponseEntity.ok().eTag(eTagValue).body(sumRewards);
  }

  @GetMapping(path = "periods")
  @Secured("rewarding")
  @Operation(summary = "Retrieves the list of periods sorted descending by start date", method = "GET", description = "returns the list of periods sorted descending by start date")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public PagedModel<EntityModel<RewardPeriod>> getRewardReportPeriods(Pageable pageable,
                                                                      PagedResourcesAssembler<RewardPeriod> assembler,
                                                                      @Parameter(description = "From date results to retrieve")
                                                                      @RequestParam(value = "from", defaultValue = "0", required = false)
                                                                      long from,
                                                                      @Parameter(description = "To date results to retrieve")
                                                                      @RequestParam(value = "to", defaultValue = "0", required = false)
                                                                      @DefaultValue("0")
                                                                      long to) {
    Page<RewardPeriod> rewardPeriods;
    if (from >= 0 && to > 0) {
      rewardPeriods = rewardReportService.findRewardPeriodsBetween(from, to, pageable);
    } else {
      rewardPeriods = rewardReportService.findRewardReportPeriods(pageable);
    }
    return assembler.toModel(rewardPeriods);
  }

  private RewardPeriod getRewardPeriod(String date) {
    RewardSettings settings = rewardSettingsService.getSettings();
    ZoneId zoneId = settings.zoneId();
    RewardPeriodType rewardPeriodType = settings.getPeriodType();

    ZonedDateTime zonedDateTime = RewardUtils.parseRFC3339ToZonedDateTime(date, zoneId);
    return rewardPeriodType.getPeriodOfTime(Objects.requireNonNull(zonedDateTime));
  }

  private List<RewardPeriod> generatePreviousPeriods(int count) {
    RewardSettings settings = rewardSettingsService.getSettings();
    ZoneId zoneId = settings.zoneId();
    RewardPeriodType periodType = settings.getPeriodType();

    ZonedDateTime currentDateTime = ZonedDateTime.now(zoneId);

    return IntStream.range(0, count).mapToObj(i -> {
      ZonedDateTime start;
      ZonedDateTime end;

      switch (periodType) {
      case WEEK -> {
        start = currentDateTime.minusWeeks(i).with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
        end = start.plusWeeks(1);
      }
      case MONTH -> {
        start = currentDateTime.minusMonths(i).with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
        end = start.plusMonths(1);
      }
      case QUARTER -> {
        ZonedDateTime zonedDateTime = currentDateTime.minusMonths(i * 3L);
        start = zonedDateTime.with(zonedDateTime.getMonth().firstMonthOfQuarter())
                             .with(TemporalAdjusters.firstDayOfMonth())
                             .truncatedTo(ChronoUnit.DAYS);
        end = start.plusMonths(3);
      }
      default -> throw new UnsupportedOperationException("Unknown period type");
      }

      RewardPeriod rewardPeriod = new RewardPeriod();
      rewardPeriod.setRewardPeriodType(periodType);
      rewardPeriod.setStartDateInSeconds(timeToSecondsAtDayStart(LocalDate.from(start), currentDateTime.getZone()));
      rewardPeriod.setEndDateInSeconds(timeToSecondsAtDayStart(LocalDate.from(end), currentDateTime.getZone()));

      return rewardPeriod;
    }).collect(Collectors.toList());
  }

}
