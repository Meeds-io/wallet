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
package org.exoplatform.wallet.rest;

import static org.exoplatform.wallet.utils.WalletUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.service.WalletContractService;


@Path("/wallet/api/contract")
@RolesAllowed("users")
@Tag(name = "/wallet/api/contract", description = "Manages internally stored token contract detail")
public class WalletContractREST implements ResourceContainer {

  private static final Log      LOG = ExoLogger.getLogger(WalletContractREST.class);

  private WalletContractService contractService;

  public WalletContractREST(WalletContractService contractService) {
    this.contractService = contractService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Retrieves stored contract details in internal datasource",
          method = "GET",
          description = "Retrieves stored contract details in internal datasource")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getContract(@Parameter(description = "contract address", required = true) @QueryParam("address") String address) {
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
  @Operation(summary = "Retrieves contract binary", method = "GET", description = "returns contract bin content")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getBin(@Parameter(description = "contract name", required = true) @PathParam("name") String name) {
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
  @Operation(
          summary = "Retrieves contract ABI",
          method = "GET",
          description = "returns contract ABI object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getAbi(@Parameter(description = "contract name", required = true) @PathParam("name") String name) {
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
