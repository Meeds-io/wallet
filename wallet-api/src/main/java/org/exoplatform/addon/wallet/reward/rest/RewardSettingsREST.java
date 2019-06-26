package org.exoplatform.addon.wallet.reward.rest;

import static org.exoplatform.addon.wallet.utils.RewardUtils.getCurrentUserId;
import static org.exoplatform.addon.wallet.utils.RewardUtils.timeFromSeconds;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.reward.*;
import org.exoplatform.addon.wallet.reward.service.RewardSettingsService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load reward settings
 */
@Path("/wallet/api/reward/settings")
@RolesAllowed("rewarding")
public class RewardSettingsREST implements ResourceContainer {
  private static final Log      LOG = ExoLogger.getLogger(RewardSettingsREST.class);

  private RewardSettingsService rewardSettingsService;

  public RewardSettingsREST(RewardSettingsService rewardSettingsService) {
    this.rewardSettingsService = rewardSettingsService;
  }

  /**
   * @return Reward general settings
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  public Response getSettings() {
    try {
      RewardSettings settings = rewardSettingsService.getSettings();
      return Response.ok(settings == null ? new RewardSettings() : settings).build();
    } catch (Exception e) {
      LOG.error("Error getting reward settings", e);
      return Response.serverError().build();
    }
  }

  /**
   * Save settings of reward
   * 
   * @param rewardSettings
   * @return
   */
  @POST
  @Path("save")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  public Response saveSettings(RewardSettings rewardSettings) {
    if (rewardSettings == null) {
      LOG.warn("Bad request sent to server with empty settings");
      return Response.status(400).build();
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

  /**
   * Retrieves reward perdiod start and end dates
   * 
   * @param periodType
   * @param dateInSeconds
   * @return
   */
  @Path("getDates")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRewardDates(@QueryParam("periodType") String periodType,
                                 @QueryParam("dateInSeconds") long dateInSeconds) {
    if (dateInSeconds == 0) {
      LOG.warn("Bad request sent to server with empty 'dateInSeconds' parameter");
      return Response.status(400).build();
    }
    if (StringUtils.isBlank(periodType)) {
      LOG.warn("Bad request sent to server with empty 'periodType' parameter");
      return Response.status(400).build();
    }
    RewardPeriodType rewardPeriodType = RewardPeriodType.valueOf(periodType);
    RewardPeriod rewardPeriod = rewardPeriodType.getPeriodOfTime(timeFromSeconds(dateInSeconds));
    return Response.ok(rewardPeriod.toString()).build();
  }

}
