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
package org.exoplatform.addon.wallet.rest;

import static org.exoplatform.addon.wallet.utils.WalletUtils.getCurrentUserId;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.service.WalletContractService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/delete a contract as default
 * displayed contracts for end users
 */
@Path("/wallet/api/contract")
@RolesAllowed("users")
public class WalletContractREST implements ResourceContainer {

  private static final Log      LOG = ExoLogger.getLogger(WalletContractREST.class);

  private WalletContractService contractService;

  public WalletContractREST(WalletContractService contractService) {
    this.contractService = contractService;
  }

  /**
   * Return saved contract details by address
   * 
   * @param address token contract address
   * @return REST Response with contract details
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getContract")
  @RolesAllowed("users")
  public Response getContract(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Empty contract address");
      return Response.status(400).build();
    }
    try {
      ContractDetail contractDetail = contractService.getContractDetail(address);
      if (contractDetail == null) {
        contractDetail = new ContractDetail();
      }
      return Response.ok(contractDetail).build();
    } catch (Exception e) {
      LOG.warn("Error getting contract details: " + address, e);
      return Response.serverError().build();
    }
  }

  /**
   * Return contract bin content
   * 
   * @param name contract name to retrieve
   * @return REST Response with contract bin content
   */
  @GET
  @Path("bin/{name}")
  @RolesAllowed("rewarding")
  public Response getBin(@PathParam("name") String name) {
    if (StringUtils.isBlank(name)) {
      LOG.warn("Empty resource name");
      return Response.status(400).build();
    }
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      LOG.error(getCurrentUserId() + " has used a forbidden path character is used: '..' or '/' or '\\'");
      return Response.status(403).build();
    }
    try {
      String contractBin = contractService.getContractFileContent(name, "bin");
      return Response.ok(contractBin).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving contract BIN: " + name, e);
      return Response.serverError().build();
    }
  }

  /**
   * Return contract abi content
   * 
   * @param name contract name to retrieve
   * @return REST Response with contract abi content
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("abi/{name}")
  @RolesAllowed("rewarding")
  public Response getAbi(@PathParam("name") String name) {
    if (StringUtils.isBlank(name)) {
      LOG.warn("Empty resource name");
      return Response.status(400).build();
    }
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      LOG.error(getCurrentUserId() + " has used a forbidden path character is used: '..' or '/' or '\\'");
      return Response.status(403).build();
    }
    try {
      String contractAbi = contractService.getContractFileContent(name, "json");
      return Response.ok(contractAbi).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving contract ABI: " + name, e);
      return Response.serverError().build();
    }
  }

  /**
   * Refreshes token contract details from blockchain
   * 
   * @return REST response with status
   */
  @GET
  @Path("refresh")
  @RolesAllowed("rewarding")
  public Response refreshContract() {
    try {
      contractService.refreshContractDetail();
      LOG.info("User {} is refreshing Token from blockchain", getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Error refreshing Token from blockchain", e);
      return Response.serverError().build();
    }
  }

}
