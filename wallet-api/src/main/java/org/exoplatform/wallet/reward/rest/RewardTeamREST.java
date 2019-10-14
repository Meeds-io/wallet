package org.exoplatform.wallet.reward.rest;

import static org.exoplatform.wallet.utils.RewardUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.reward.service.RewardTeamService;

import io.swagger.annotations.*;

@Path("/wallet/api/reward/team")
@RolesAllowed("rewarding")
@Api(value = "/wallet/api/reward/team", description = "Manage reward teams (pools)") // NOSONAR
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
  @ApiOperation(value = "Get reward teams with their members", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns the list of reward team objects")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
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
  @ApiOperation(value = "Remove a reward team", httpMethod = "GET", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response removeTeam(@ApiParam(value = "Reward team technical id", required = true) @QueryParam("id") Long id) {
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
  @ApiOperation(value = "Save a reward team", httpMethod = "POST", response = Response.class, consumes = "application/json", produces = "application/json", notes = "returns saved reward team object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response saveTeam(@ApiParam(value = "Reward team object", required = true) RewardTeam rewardTeam) {
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
