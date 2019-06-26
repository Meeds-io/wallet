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
package org.exoplatform.addon.wallet.rest;

import static org.exoplatform.addon.wallet.utils.WalletUtils.getCurrentUserId;

import java.util.List;
import java.util.Locale;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.model.transaction.TransactionStatistics;
import org.exoplatform.addon.wallet.service.WalletTransactionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to retrieve detailed information about
 * users and spaces Ethereum transactions
 */
@Path("/wallet/api/transaction")
@RolesAllowed("users")
public class WalletTransactionREST implements ResourceContainer {

  private static final String      EMPTY_ADDRESS_ERROR = "Bad request sent to server with empty address {}";

  private static final Log         LOG                 = ExoLogger.getLogger(WalletTransactionREST.class);

  private WalletTransactionService transactionService;

  public WalletTransactionREST(WalletTransactionService transactionService) {
    this.transactionService = transactionService;
  }

  /**
   * Store transaction hash in sender, receiver and contract accounts
   * 
   * @param transactionDetail transaction details to save
   * @return REST response with status
   */
  @POST
  @Path("saveTransactionDetails")
  @RolesAllowed("users")
  public Response saveTransactionDetails(TransactionDetail transactionDetail) {
    if (transactionDetail == null || StringUtils.isBlank(transactionDetail.getHash())
        || StringUtils.isBlank(transactionDetail.getFrom())) {
      LOG.warn("Bad request sent to server with empty transaction details: {}",
               transactionDetail == null ? "" : transactionDetail.toString());
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      transactionService.saveTransactionDetail(transactionDetail, currentUserId, false);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      LOG.warn("User {} is attempting to save transaction {}", currentUserId, transactionDetail, e);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Error saving transaction message", e);
      return Response.serverError().build();
    }
  }

  /**
   * Get last pending transaction of an address
   * 
   * @param address wallet address to retrieve its transactions
   * @return REST Response with the last pending transaction of an address
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getLastPendingTransactionSent")
  @RolesAllowed("users")
  public Response getLastPendingTransactionSent(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      TransactionDetail transactionDetail = transactionService.getAddressLastPendingTransactionSent(address,
                                                                                                    currentUserId);
      return Response.ok(transactionDetail).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User {} attempts to display transactions of address {}", currentUserId, address);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Error getting transactions of wallet " + address, e);
      return Response.serverError().build();
    }
  }

  /**
   * @param contractAddress contract address
   * @param address wallet address
   * @param periodicity month / week
   * @return REST Response with the transactions statistics of period of time
   *         for a designated wallet
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getTransactionsAmounts")
  @RolesAllowed("users")
  public Response getTransactionsAmounts(@QueryParam("contractAddress") String contractAddress,
                                         @QueryParam("address") String address,
                                         @QueryParam("periodicity") String periodicity,
                                         @QueryParam("lang") String lang) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }

    try {
      TransactionStatistics transactionStatistics = transactionService.getTransactionStatistics(contractAddress,
                                                                                                address,
                                                                                                periodicity,
                                                                                                new Locale(lang));
      return Response.ok(transactionStatistics).build();
    } catch (Exception e) {
      LOG.error("Error getting transactions statistics of wallet " + address, e);
      return Response.serverError().build();
    }
  }

  /**
   * Get list of transactions of an address
   * 
   * @param address wallet address to retrieve its transactions
   * @param contractAddress filtered contract address transactions
   * @param hash transaction hash to include in the returned list
   * @param limit transactions list limit
   * @param onlyPending whether retrieve only pending transactions of wallet or
   *          all
   * @param administration whether include or not administrative transactions or
   *          not
   * @return REST Response with the list of transactions details
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("getTransactions")
  @RolesAllowed("users")
  public Response getTransactions(@QueryParam("address") String address,
                                  @QueryParam("contractAddress") String contractAddress,
                                  @QueryParam("contractMethodName") String contractMethodName,
                                  @QueryParam("hash") String hash,
                                  @QueryParam("limit") int limit,
                                  @QueryParam("pending") boolean onlyPending,
                                  @QueryParam("administration") boolean administration) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }

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
      LOG.warn("User {} attempts to display transactions of address {}", currentUserId, address);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Error getting transactions of wallet " + address, e);
      return Response.serverError().build();
    }
  }
}
