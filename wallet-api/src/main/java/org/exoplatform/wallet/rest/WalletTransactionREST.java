/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import java.util.List;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.model.transaction.TransactionStatistics;
import org.exoplatform.wallet.service.*;

import io.swagger.annotations.*;

@Path("/wallet/api/transaction")
@RolesAllowed("users")
@Api(value = "/wallet/api/transaction", description = "Manages internally stored transactions") // NOSONAR
public class WalletTransactionREST implements ResourceContainer {

  private static final String          EMPTY_ADDRESS_ERROR = "Bad request sent to server with empty address {}";

  private static final Log             LOG                 = ExoLogger.getLogger(WalletTransactionREST.class);

  private BlockchainTransactionService blockchainTransactionService;

  private WalletTransactionService     transactionService;

  private WalletTokenAdminService      walletTokenAdminService;

  public WalletTransactionREST(WalletTransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @POST
  @Path("saveTransactionDetails")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Save transaction details in internal datasource", httpMethod = "POST", response = Response.class, consumes = "application/json", produces = "application/json", notes = "returns saved transaction detail")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response saveTransactionDetails(@ApiParam(value = "transaction detail object", required = true) TransactionDetail transactionDetail) {
    if (transactionDetail == null || StringUtils.isBlank(transactionDetail.getFrom())) {
      LOG.warn("Bad request sent to server with empty transaction details: {}",
               transactionDetail == null ? "" : transactionDetail.toString());
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUserId = getCurrentUserId();
    // Fix generated transaction hash from Web3js to use generated hash from
    // Web3j
    if (transactionDetail.getId() == 0 && StringUtils.isNotBlank(transactionDetail.getRawTransaction())) {
      String transactionHash = getWalletTokenAdminService().generateHash(transactionDetail.getRawTransaction());
      transactionDetail.setHash(transactionHash);
    } else if (StringUtils.isBlank(transactionDetail.getHash())) {
      LOG.warn("Bad request sent to server with empty transaction hash");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    } else {
      transactionDetail.setSentTimestamp(System.currentTimeMillis());
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
  @ApiOperation(value = "Get saved transaction in internal database by hash", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns transaction detail")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getSavedTransactionByHash(@ApiParam(value = "transaction hash", required = true) @QueryParam("hash") String hash) {
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
  @ApiOperation(value = "Get nonce to include in next transaction to send for a wallet", httpMethod = "GET", response = Response.class, notes = "returns transaction nonce")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getNonce(@ApiParam(value = "Transaction sender address", required = true) @QueryParam("from") String fromAddress) {
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
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getTransactionsAmounts")
  @RolesAllowed("users")
  @ApiOperation(value = "Get token amounts sent per each period of time by a wallet identified by its address", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns transaction statistics object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getTransactionsAmounts(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address,
                                         @ApiParam(value = "periodicity : month or year", required = true) @QueryParam("periodicity") String periodicity,
                                         @ApiParam(value = "Selected date", required = false) @QueryParam("date") String selectedDate,
                                         @ApiParam(value = "user locale language", required = false) @QueryParam("lang") String lang) {
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
  @ApiOperation(value = "Get list of transactions of an address", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns list of transaction detail object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getTransactions(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address,
                                  @ApiParam(value = "token contract address to filter with", required = false) @QueryParam("contractAddress") String contractAddress,
                                  @ApiParam(value = "token contract method to filter with", required = false) @QueryParam("contractMethodName") String contractMethodName,
                                  @ApiParam(value = "transaction hash to include in response", required = false) @QueryParam("hash") String hash,
                                  @ApiParam(value = "limit transactions to retrieve", required = false) @QueryParam("limit") int limit,
                                  @ApiParam(value = "whether to include only pending or not", required = false) @QueryParam("pending") boolean onlyPending,
                                  @ApiParam(value = "whether to include administration transactions or not", required = false) @QueryParam("administration") boolean administration) {
    String currentUserId = getCurrentUserId();
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
  @ApiOperation(value = "refresh transaction detail from blockchain", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "return transaction detail refreshed from blockchain")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response refreshTransactionFromBlockchain(@ApiParam(value = "transaction hash", required = true) @QueryParam("hash") String hash) {
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
