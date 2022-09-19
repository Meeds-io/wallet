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
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletTokenAdminService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/wallet/api/admin/transaction")
@RolesAllowed("rewarding")
@Api(value = "/wallet/api/admin/transaction", description = "Manages admin wallet transactions to send on blockchain") // NOSONAR
public class WalletAdminTransactionREST implements ResourceContainer {

  private static final Log        LOG                           = ExoLogger.getLogger(WalletAdminTransactionREST.class);

  private WalletTokenAdminService walletTokenAdminService;

  @POST
  @Path("sendEther")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Send ether using blockchain transaction from Admin wallet", httpMethod = "POST", response = Response.class, notes = "returns transaction hash")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response sendEther(@ApiParam(value = "receiver wallet address", required = true) @FormParam("receiver") String receiver,
                            @ApiParam(value = "ether amount to send", required = true) @FormParam("etherAmount") double etherAmount,
                            @ApiParam(value = "transaction label", required = false) @FormParam("transactionLabel") String transactionLabel,
                            @ApiParam(value = "transaction message to send to receiver with transaction", required = false) @FormParam("transactionMessage") String transactionMessage) {
    String currentUserId = getCurrentUserId();
    if (StringUtils.isBlank(receiver)) {
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    if (etherAmount <= 0) {
      LOG.warn("Wrong ether amount '{}' sent to server", etherAmount);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      TransactionDetail transactionDetail = new TransactionDetail();
      transactionDetail.setTo(receiver);
      transactionDetail.setValue(etherAmount);
      transactionDetail.setLabel(transactionLabel);
      transactionDetail.setMessage(transactionMessage);
      transactionDetail = getWalletTokenAdminService().sendEther(transactionDetail, currentUserId);
      return Response.ok(transactionDetail == null ? "" : transactionDetail.getHash()).build();
    } catch (Exception e) {
      LOG.error("Error sending ether to wallet {}", receiver, e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("sendToken")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Send tokens using blockchain transaction from Admin wallet", httpMethod = "POST", response = Response.class, notes = "returns transaction hash")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response sendToken(@ApiParam(value = "receiver wallet address", required = true) @FormParam("receiver") String receiver,
                            @ApiParam(value = "transaction label", required = false) @FormParam("transactionLabel") String transactionLabel,
                            @ApiParam(value = "transaction message to send to receiver with transaction", required = false) @FormParam("transactionMessage") String transactionMessage,
                            @ApiParam(value = "value of token to send to receiver", required = false) @FormParam("tokenAmount") double tokenAmount) {
    String currentUserId = getCurrentUserId();
    if (StringUtils.isBlank(receiver)) {
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      if (tokenAmount <= 0) {
        throw new IllegalStateException("Can't send tokens to wallet " + receiver + "because the value of token is not valid");
      }
      TransactionDetail transactionDetail = new TransactionDetail();
      transactionDetail.setTo(receiver);
      transactionDetail.setContractAmount(tokenAmount);
      transactionDetail.setLabel(transactionLabel);
      transactionDetail.setMessage(transactionMessage);
      transactionDetail = getWalletTokenAdminService().sendToken(transactionDetail, currentUserId);
      return Response.ok(transactionDetail == null ? "" : transactionDetail.getHash()).build();
    } catch (Exception e) {
      LOG.error("Error sending token to wallet {}", receiver, e);
      return Response.serverError().build();
    }
  }

  /**
   * Workaround: WalletTokenAdminService retrieved here instead of dependency
   * injection using constructor because the service is added after
   * PortalContainer startup. (See PLF-8123)
   * 
   * @return wallet token service
   */
  private WalletTokenAdminService getWalletTokenAdminService() {
    if (walletTokenAdminService == null) {
      walletTokenAdminService = CommonsUtils.getService(WalletTokenAdminService.class);
    }
    return walletTokenAdminService;
  }

}
