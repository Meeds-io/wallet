package org.exoplatform.addon.wallet.reward.rest;

import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.addon.wallet.model.reward.WalletReward;
import org.exoplatform.addon.wallet.reward.service.RewardService;
import org.exoplatform.addon.wallet.utils.WalletUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to compute rewards
 */
@Path("/wallet/api/reward/")
@RolesAllowed("rewarding")
public class RewardBudgetREST implements ResourceContainer {
  private static final Log LOG = ExoLogger.getLogger(RewardBudgetREST.class);

  private RewardService    rewardService;

  public RewardBudgetREST(RewardService rewardService) {
    this.rewardService = rewardService;
  }

  /**
   * Compute rewards per period
   * 
   * @param periodDateInSeconds
   * @return
   */
  @GET
  @Path("compute")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("rewarding")
  public Response computeRewards(@QueryParam("periodDateInSeconds") long periodDateInSeconds) {
    try {
      Set<WalletReward> rewards = rewardService.computeReward(periodDateInSeconds);
      return Response.ok(rewards).build();
    } catch (Exception e) {
      LOG.warn("Error getting computed reward", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(500).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  /**
   * Compute rewards per period
   * 
   * @param periodDateInSeconds
   * @return
   */
  @GET
  @Path("send")
  @RolesAllowed("rewarding")
  public Response sendRewards(@QueryParam("periodDateInSeconds") long periodDateInSeconds) {
    try {
      rewardService.sendRewards(periodDateInSeconds, WalletUtils.getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error getting computed reward", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(500).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

}
