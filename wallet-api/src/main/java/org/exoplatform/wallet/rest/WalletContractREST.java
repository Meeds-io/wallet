/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wallet.rest;

import static org.exoplatform.wallet.utils.WalletUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.service.WalletContractService;

import io.swagger.annotations.*;

@Path("/wallet/api/contract")
@RolesAllowed("users")
@Api(value = "/wallet/api/contract", description = "Manages internally stored token contract detail") // NOSONAR
public class WalletContractREST implements ResourceContainer {

  private static final Log      LOG = ExoLogger.getLogger(WalletContractREST.class);

  private WalletContractService contractService;

  public WalletContractREST(WalletContractService contractService) {
    this.contractService = contractService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Retrieves stored contract details in internal datasource", httpMethod = "GET", produces = "application/json", response = Response.class, notes = "returns contract detail object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getContract(@ApiParam(value = "contract address", required = true) @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Empty contract address");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      ContractDetail contractDetail = contractService.getContractDetail(address);
      if (contractDetail == null) {
        contractDetail = new ContractDetail();
      }
      return Response.ok(contractDetail).build();
    } catch (Exception e) {
      LOG.error("Error getting contract details: " + address, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("bin/{name}")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Retrieves contract binary", httpMethod = "GET", response = Response.class, notes = "returns contract bin content")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getBin(@ApiParam(value = "contract name", required = true) @PathParam("name") String name) {
    if (StringUtils.isBlank(name)) {
      LOG.warn("Empty resource name");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      LOG.error(getCurrentUserId() + " has used a forbidden path character is used: '..' or '/' or '\\'");
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    }
    try {
      String contractBin = contractService.getContractFileContent(name, "bin");
      return Response.ok(contractBin).build();
    } catch (Exception e) {
      LOG.error("Error retrieving contract BIN: " + name, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("abi/{name}")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Retrieves contract ABI", httpMethod = "GET", produces = "application/json", response = Response.class, notes = "returns contract ABI object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getAbi(@ApiParam(value = "contract name", required = true) @PathParam("name") String name) {
    if (StringUtils.isBlank(name)) {
      LOG.warn("Empty resource name");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      LOG.error(getCurrentUserId() + " has used a forbidden path character is used: '..' or '/' or '\\'");
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    }
    try {
      String contractAbi = contractService.getContractFileContent(name, "json");
      return Response.ok(contractAbi).build();
    } catch (Exception e) {
      LOG.error("Error retrieving contract ABI: " + name, e);
      return Response.serverError().build();
    }
  }
}
