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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/wallet/api/admin/transaction")
@RolesAllowed("rewarding")
@Tag(name = "/wallet/api/admin/transaction", description = "Manages admin wallet transactions to send on blockchain")
public class WalletAdminTransactionREST implements ResourceContainer {

  private static final Log        LOG                           = ExoLogger.getLogger(WalletAdminTransactionREST.class);

  private WalletTokenAdminService walletTokenAdminService;

  @POST
  @Path("intiialize")
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Send blockchain transaction using Admin wallet to initialize wallet identified by its address",
          method = "POST",
          description = "Send blockchain transaction using Admin wallet to initialize wallet identified by its address and returns transaction hash")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response initializeWallet(@Parameter(description = "receiver wallet address", required = true) @FormParam("receiver") String receiver,
                                  @Parameter(description = "ether amount to send to wallet") @FormParam("etherAmount") double etherAmount,
                                  @Parameter(description = "token amount to send to wallet") @FormParam("tokenAmount") double tokenAmount,
                                  @Parameter(description = "transaction label") @FormParam("transactionLabel") String transactionLabel,
                                  @Parameter(description = "transaction message to send to receiver with transaction") @FormParam("transactionMessage") String transactionMessage) {
    String currentUserId = getCurrentUserId();
    if (StringUtils.isBlank(receiver)) {
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      // Send Ether
      TransactionDetail etherTransactionDetail = new TransactionDetail();
      etherTransactionDetail.setTo(receiver);
      etherTransactionDetail.setValue(etherAmount);
      etherTransactionDetail.setLabel(transactionLabel);
      etherTransactionDetail.setMessage(transactionMessage);
      getWalletTokenAdminService().sendEther(etherTransactionDetail, currentUserId);

      // Send Token
      TransactionDetail transactionDetail = new TransactionDetail();
      transactionDetail.setTo(receiver);
      transactionDetail.setContractAmount(tokenAmount);
      transactionDetail.setLabel(transactionLabel);
      transactionDetail.setMessage(transactionMessage);
      transactionDetail = getWalletTokenAdminService().sendToken(transactionDetail, currentUserId);

      return Response.ok(transactionDetail == null ? "" : transactionDetail.getHash()).build();
    } catch (Exception e) {
      LOG.error("Error initializing wallet {}", receiver, e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("sendEther")
  @RolesAllowed("rewarding")
  @Operation(
          summary = "Send ether using blockchain transaction from Admin wallet",
          method = "POST",
          description = "Send ether using blockchain transaction from Admin wallet and returns transaction hash")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response sendEther(@Parameter(description = "receiver wallet address", required = true) @FormParam("receiver") String receiver,
                            @Parameter(description = "ether amount to send", required = true) @FormParam("etherAmount") double etherAmount,
                            @Parameter(description = "transaction label") @FormParam("transactionLabel") String transactionLabel,
                            @Parameter(description = "transaction message to send to receiver with transaction") @FormParam("transactionMessage") String transactionMessage) {
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
  @Operation(
          summary = "Send tokens using blockchain transaction from Admin wallet",
          method = "POST",
          description = "Send tokens using blockchain transaction from Admin wallet and returns transaction hash")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response sendToken(@Parameter(description = "receiver wallet address", required = true) @FormParam("receiver") String receiver,
                            @Parameter(description = "transaction label") @FormParam("transactionLabel") String transactionLabel,
                            @Parameter(description = "transaction message to send to receiver with transaction") @FormParam("transactionMessage") String transactionMessage,
                            @Parameter(description = "value of token to send to receiver") @FormParam("tokenAmount") double tokenAmount) {
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
