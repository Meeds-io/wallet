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

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.model.transaction.TransactionStatistics;
import org.exoplatform.wallet.service.*;

@Path("/wallet/api/transaction")
@RolesAllowed("users")
@Tag(name = "/wallet/api/transaction", description = "Manages internally stored transactions") // NOSONAR
public class WalletTransactionREST implements ResourceContainer {

  private static final String          EMPTY_ADDRESS_ERROR = "Bad request sent to server with empty address {}";

  private static final Log             LOG                 = ExoLogger.getLogger(WalletTransactionREST.class);

  private BlockchainTransactionService blockchainTransactionService;

  private WalletTransactionService     transactionService;

  private WalletTokenAdminService      walletTokenAdminService;

  private WalletService                walletService;

  public WalletTransactionREST(WalletTransactionService transactionService, WalletService walletService) {
    this.transactionService = transactionService;
    this.walletService = walletService;
  }

  @POST
  @Path("saveTransactionDetails")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Save transaction details in internal datasource",
          method = "POST",
          description = "Save transaction details in internal datasource")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveTransactionDetails(@RequestBody(description = "transaction detail object", required = true) TransactionDetail transactionDetail) {
    if (transactionDetail == null || StringUtils.isBlank(transactionDetail.getFrom())) {
      LOG.warn("Bad request sent to server with empty transaction details: {}",
               transactionDetail == null ? "" : transactionDetail.toString());
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUserId = getCurrentUserId();
    // Fix generated transaction hash from Web3js to use generated hash from
    // Web3j
    if (StringUtils.isNotBlank(transactionDetail.getRawTransaction())) {
      String transactionHash = getWalletTokenAdminService().generateHash(transactionDetail.getRawTransaction());
      transactionDetail.setHash(transactionHash);
    } else if (StringUtils.isBlank(transactionDetail.getHash())) {
      LOG.warn("Bad request sent to server with empty transaction hash");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      transactionService.saveTransactionDetail(transactionDetail, currentUserId);
      return Response.ok(transactionDetail).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User {} is attempting to save transaction {}", currentUserId, transactionDetail, e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error saving transaction message {}", transactionDetail, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getSavedTransactionByHash")
  @RolesAllowed("users")
  @Operation(
          summary = "Get saved transaction in internal database by hash",
          method = "GET",
          description = "Get saved transaction in internal database by hash and returns transaction detail")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getSavedTransactionByHash(@Parameter(description = "transaction hash", required = true) @QueryParam("hash") String hash) {
    if (StringUtils.isBlank(hash)) {
      LOG.warn("Empty transaction hash", hash);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      TransactionDetail transactionDetail = transactionService.getTransactionByHash(hash, getCurrentUserId());
      return Response.ok(transactionDetail).build();
    } catch (Exception e) {
      LOG.error("Error getting transaction with hash {}", hash, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("getNonce")
  @RolesAllowed("users")
  @Operation(
          summary = "Get nonce to include in next transaction to send for a wallet",
          method = "GET",
          description = "Get nonce to include in next transaction to send for a wallet")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getNonce(@Parameter(description = "Transaction sender address", required = true) @QueryParam("from") String fromAddress) {
    if (StringUtils.isBlank(fromAddress)) {
      LOG.warn("Empty transaction sender", fromAddress);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUser = getCurrentUserId();
    try {
      long nonce = transactionService.getNonce(fromAddress, currentUser);
      return Response.ok(String.valueOf(nonce)).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User {} attempts to display last nonce of address {}", currentUser, fromAddress, e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error getting nonce for address {}", fromAddress, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("getGasPrice")
  @RolesAllowed("users")
  @Operation(summary = "Get current gas price from blockchain", method = "GET", description = "Get current gas price from blockchain to be used to send a transaction")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getGasPrice() {
    double gasPrice = walletService.getGasPrice();
    return Response.ok(BigDecimal.valueOf(gasPrice).toBigInteger().toString()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getTransactionsAmounts")
  @RolesAllowed("users")
  @Operation(
          summary = "Get token amounts sent per each period of time by a wallet identified by its address",
          method = "GET",
          description = "returns transaction statistics object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getTransactionsAmounts(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address,
                                         @Parameter(description = "periodicity : month or year", required = true) @QueryParam("periodicity") String periodicity,
                                         @Parameter(description = "Selected date") @QueryParam("date") String selectedDate,
                                         @Parameter(description = "user locale language") @QueryParam("lang") String lang) {
    if (StringUtils.isBlank(periodicity)) {
      LOG.warn("Bad request sent to server with empty periodicity parameter");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      TransactionStatistics transactionStatistics = transactionService.getTransactionStatistics(address,
                                                                                                periodicity,
                                                                                                selectedDate,
                                                                                                new Locale(lang));
      return Response.ok(transactionStatistics).build();
    } catch (Exception e) {
      LOG.error("Error getting transactions statistics of wallet " + address, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getTransactions")
  @RolesAllowed("users")
  @Operation(
          summary = "Get list of transactions of an address",
          method = "GET",
          description = "returns list of transaction detail object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getTransactions(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address,
                                  @Parameter(description = "token contract address to filter with") @QueryParam("contractAddress") String contractAddress,
                                  @Parameter(description = "token contract method to filter with") @QueryParam("contractMethodName") String contractMethodName,
                                  @Parameter(description = "transaction hash to include in response") @QueryParam("hash") String hash,
                                  @Parameter(description = "limit transactions to retrieve") @QueryParam("limit") int limit,
                                  @Parameter(description = "whether to include only pending or not") @QueryParam("pending") boolean onlyPending,
                                  @Parameter(description = "whether to include administration transactions or not") @QueryParam("administration") boolean administration) {
    String currentUserId = getCurrentUserId();
    if(StringUtils.isBlank(address)) {
      LOG.warn("Bad request sent to server with empty wallet address");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      List<TransactionDetail> transactionDetails = transactionService.getTransactions(address,
                                                                                      contractAddress,
                                                                                      contractMethodName,
                                                                                      hash,
                                                                                      limit,
                                                                                      onlyPending,
                                                                                      administration,
                                                                                      currentUserId);
      return Response.ok(transactionDetails).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User {} attempts to display transactions of address {}", currentUserId, address, e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Error getting transactions of wallet " + address, e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("refreshTransactionFromBlockchain")
  @RolesAllowed("rewarding")
  @Operation(
          summary = "refresh transaction detail from blockchain",
          method = "GET",
          description = "return transaction detail refreshed from blockchain")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "200", description = "Invalid query input"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response refreshTransactionFromBlockchain(@Parameter(description = "transaction hash", required = true) @QueryParam("hash") String hash) {
    if (StringUtils.isBlank(hash)) {
      LOG.warn("Bad request sent to server with empty transaction hash");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      TransactionDetail transactionDetail = getBlockchainTransactionService().refreshTransactionFromBlockchain(hash);
      return Response.ok(transactionDetail).build();
    } catch (Exception e) {
      LOG.error("Error refreshing transaction with hash {} from blockchain", hash, e);
      return Response.serverError().build();
    }
  }

  /**
   * Workaround: {@link WalletTokenAdminService} retrieved here instead of
   * dependency injection using constructor because the service is added after
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

  /**
   * Workaround: {@link BlockchainTransactionService} retrieved here instead of
   * dependency injection using constructor because the service is added after
   * PortalContainer startup. (See PLF-8123)
   * 
   * @return blockchain transaction service
   */
  public BlockchainTransactionService getBlockchainTransactionService() {
    if (blockchainTransactionService == null) {
      blockchainTransactionService = CommonsUtils.getService(BlockchainTransactionService.class);
    }
    return blockchainTransactionService;
  }

}
