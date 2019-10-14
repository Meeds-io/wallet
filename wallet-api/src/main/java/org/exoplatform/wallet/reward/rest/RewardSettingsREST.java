package org.exoplatform.wallet.reward.rest;

import static org.exoplatform.wallet.utils.RewardUtils.getCurrentUserId;
import static org.exoplatform.wallet.utils.RewardUtils.timeFromSeconds;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.reward.*;
import org.exoplatform.wallet.reward.service.RewardSettingsService;

import io.swagger.annotations.*;

@Path("/wallet/api/reward/settings")
@RolesAllowed("rewarding")
@Api(value = "/wallet/api/reward/settings", description = "Manage reward module settings") // NOSONAR
public class RewardSettingsREST implements ResourceContainer {
  private static final Log      LOG = ExoLogger.getLogger(RewardSettingsREST.class);

  private RewardSettingsService rewardSettingsService;

  public RewardSettingsREST(RewardSettingsService rewardSettingsService) {
    this.rewardSettingsService = rewardSettingsService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Get reward settings", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns reward settings object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
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
  @ApiOperation(value = "Get reward settings", httpMethod = "POST", response = Response.class, produces = "application/json", notes = "returns reward settings object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response saveSettings(@ApiParam(value = "Reward settings object", required = true) RewardSettings rewardSettings) {
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
  @ApiOperation(value = "Get dates corresponding to chosen period type and start date", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns reward period dates object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getRewardDates(@ApiParam(value = "Reward period type", required = true) @QueryParam("periodType") String periodType,
                                 @ApiParam(value = "A chosen date to calculate its period", required = true) @QueryParam("dateInSeconds") long dateInSeconds) {
    if (dateInSeconds == 0) {
      LOG.warn("Bad request sent to server with empty 'dateInSeconds' parameter");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(periodType)) {
      LOG.warn("Bad request sent to server with empty 'periodType' parameter");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    RewardPeriodType rewardPeriodType = RewardPeriodType.valueOf(periodType);
    RewardPeriod rewardPeriod = rewardPeriodType.getPeriodOfTime(timeFromSeconds(dateInSeconds));
    return Response.ok(rewardPeriod).build();
  }

}
