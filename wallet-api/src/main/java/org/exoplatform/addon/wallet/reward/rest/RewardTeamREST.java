package org.exoplatform.addon.wallet.reward.rest;

import static org.exoplatform.addon.wallet.utils.RewardUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.wallet.model.reward.RewardTeam;
import org.exoplatform.addon.wallet.reward.service.RewardTeamService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load reward pools
 */
@Path("/wallet/api/reward/team")
@RolesAllowed({ "rewarding", "administrators" })
public class RewardTeamREST implements ResourceContainer {
  private static final Log  LOG = ExoLogger.getLogger(RewardTeamREST.class);

  private RewardTeamService rewardTeamService;

  public RewardTeamREST(RewardTeamService rewardTeamService) {
    this.rewardTeamService = rewardTeamService;
  }

  /**
   * @return Reward teams with their members
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("list")
  @RolesAllowed({ "rewarding", "administrators" })
  public Response listTeams() {
    try {
      return Response.ok(rewardTeamService.getTeams()).build();
    } catch (Exception e) {
      LOG.warn("Error getting computed reward", e);
      return Response.serverError().build();
    }
  }

  /**
   * Remove a reward Team/Pool by id
   * 
   * @param id
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("remove")
  @RolesAllowed({ "rewarding", "administrators" })
  public Response removeTeam(@QueryParam("id") Long id) {
    if (id == null || id == 0) {
      return Response.status(400).build();
    }
    try {
      RewardTeam team = rewardTeamService.removeTeam(id);
      LOG.info("{} removed reward pool {}", getCurrentUserId(), team.toString());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error removing reward pool with id: " + id, e);
      return Response.serverError().build();
    }
  }

  /**
   * Add/modifiy a reward team
   * 
   * @param rewardTeam
   * @return
   */
  @POST
  @Path("save")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ "rewarding", "administrators" })
  public Response saveTeam(RewardTeam rewardTeam) {
    if (rewardTeam == null) {
      LOG.warn("Bad request sent to server with empty team");
      return Response.status(400).build();
    }
    try {
      rewardTeam = rewardTeamService.saveTeam(rewardTeam);
      LOG.info("{} saved reward pool {}", getCurrentUserId(), rewardTeam.getName());
      return Response.ok(rewardTeam).build();
    } catch (Exception e) {
      LOG.warn("Error saving reward pool", e);
      return Response.serverError().build();
    }
  }

}
