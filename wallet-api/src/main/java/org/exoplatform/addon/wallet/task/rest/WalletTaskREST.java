package org.exoplatform.addon.wallet.task.rest;

import java.util.Collections;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.addon.wallet.model.task.WalletAdminTask;
import org.exoplatform.addon.wallet.task.service.WalletTaskService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import io.swagger.annotations.*;

@Path("/wallet/api/task")
@RolesAllowed("users")
@Api(value = "/wallet/api/task", description = "Manage wallet administration tasks") // NOSONAR
public class WalletTaskREST implements ResourceContainer {
  private static final Log  LOG = ExoLogger.getLogger(WalletTaskREST.class);

  private WalletTaskService walletTaskService;

  public WalletTaskREST(WalletTaskService walletTaskService) {
    this.walletTaskService = walletTaskService;
  }

  @GET
  @Path("list")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get list of wallet administration tasks assigned to a user", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns list of wallet admin task objects")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response listTasks() {
    ConversationState currentState = ConversationState.getCurrent();
    if (currentState == null || currentState.getIdentity() == null || currentState.getIdentity().getUserId() == null
        || currentState.getIdentity().getRoles() == null || !currentState.getIdentity().getRoles().contains("rewarding")) {
      return Response.ok(Collections.emptyList()).build();
    }
    try {
      Set<WalletAdminTask> tasks = walletTaskService.listTasks(currentState.getIdentity().getUserId());
      return Response.ok(tasks).build();
    } catch (Exception e) {
      LOG.error("Error getting listing tasks", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(500).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }

  @GET
  @Path("markCompleted")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Marks a task identified by its technical id as completed", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns empty object")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response markCompleted(@ApiParam(value = "Task technical id", required = true) @QueryParam("taskId") long taskId) {
    try {
      walletTaskService.markCompleted(taskId);
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error marking task with id {} as completed", taskId, e);
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
