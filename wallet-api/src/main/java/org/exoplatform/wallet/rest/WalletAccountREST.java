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

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.wallet.model.*;
import org.exoplatform.wallet.model.transaction.FundsRequest;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.service.WalletService;

import io.swagger.annotations.*;

@Path("/wallet/api/account")
@Api(value = "/wallet/api/account", description = "Manages wallets objects associated to users, spaces and admin") // NOSONAR
@RolesAllowed("users")
public class WalletAccountREST implements ResourceContainer {

  private static final String  WALLET_NOT_FOUND_MESSAGE = "Wallet was not found with address {}";

  private static final String  EMPTY_ADDRESS_MESSAGE    = "Bad request sent to server with empty address";

  private static final Log     LOG                      = ExoLogger.getLogger(WalletAccountREST.class);

  private WalletService        walletService;

  private WalletAccountService accountService;

  public WalletAccountREST(WalletService walletService, WalletAccountService accountService) {
    this.walletService = walletService;
    this.accountService = accountService;
  }

  @Path("detailsById")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Retrieves the user or space wallet identified by username or space pretty name", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns the associated Wallet object, if not found it will return an empty object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response getWalletByTypeAndID(@ApiParam(value = "username or space pretty name", required = true) @QueryParam("id") String remoteId,
                                       @ApiParam(value = "'user' or 'space'", required = true) @QueryParam("type") String type) {
    if (StringUtils.isBlank(remoteId) || StringUtils.isBlank(type)) {
      LOG.warn("Bad request sent to server with id '{}' and type '{}'", remoteId, type);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      Wallet wallet = accountService.getWalletByTypeAndId(type, remoteId, getCurrentUserId());
      if (wallet != null) {
        return Response.ok(wallet).build();
      } else {
        return Response.ok("{}").build();
      }
    } catch (Exception e) {
      LOG.error("Error getting wallet by id {} and type {}", remoteId, type);
      return Response.serverError().build();
    }
  }

  @Path("detailsByAddress")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Retrieves the user or space wallet identified by an address", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns the associated Wallet object, if not found it will return an empty object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response getWalletByAddress(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address) {
    try {
      if (StringUtils.isBlank(address)) {
        LOG.warn(EMPTY_ADDRESS_MESSAGE);
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }
      String currentUser = getCurrentUserId();
      Wallet wallet = accountService.getWalletByAddress(address, currentUser);
      if (wallet != null) {
        if (WalletType.isSpace(wallet.getType())) {
          wallet.setSpaceAdministrator(isUserSpaceManager(wallet.getId(), currentUser));
        }
        hideWalletOwnerPrivateInformation(wallet);
        return Response.ok(wallet).build();
      } else {
        return Response.ok("{}").build();
      }
    } catch (Exception e) {
      LOG.error("Error getting wallet by address {}", address);
      return Response.serverError().build();
    }
  }

  @Path("enable")
  @GET
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Enable or disable a wallet identified by its address", httpMethod = "GET", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response enableWalletByAddress(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address,
                                        @ApiParam(value = "true to enable wallet, else false", required = true) @QueryParam("enable") boolean enable) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      accountService.enableWalletByAddress(address, enable, getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Can't delete address '{}' association", address, e);
      return Response.serverError().build();
    }
  }

  @Path("setInitializationStatus")
  @GET
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Modify initialization status of wallet", httpMethod = "GET", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response setInitializationStatus(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address,
                                          @ApiParam(value = "intialization status: new, modified, pending, initialized or denied", required = true) @QueryParam("status") String status) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(status)) {
      LOG.warn("Bad request sent to server with empty 'status' parameter");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      accountService.setInitializationStatus(address,
                                             WalletInitializationState.valueOf(status.toUpperCase()),
                                             getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Can't set wallet initialized status '{}'", status, e);
      return Response.serverError().build();
    }
  }

  @Path("requestAuthorization")
  @GET
  @ApiOperation(value = "Modify initialization status from DENIED to MODIFIED. This is used in case when a wallet has been denied access, in that case, a new authorization request can be done", httpMethod = "GET", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response requestAuthorization(@ApiParam(value = "wallet address to change its status", required = true) @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      accountService.setInitializationStatus(address,
                                             WalletInitializationState.MODIFIED,
                                             getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Can't request authorization for wallet {}", address, e);
      return Response.serverError().build();
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Path("saveBackupState")
  @RolesAllowed("users")
  @ApiOperation(value = "Saves wallet backup state", httpMethod = "POST", consumes = "application/json", response = Response.class, notes = "returns the modified wallet")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response saveWalletBackupState(@ApiParam(value = "wallet technical id", required = true) @FormParam("walletId") long walletId,
                                        @ApiParam(value = "whether wallet backedUp or not", required = true) @FormParam("backedUp") boolean backedUp) {
    if (walletId <= 0) {
      LOG.warn("Bad request sent to server with wrong wallet technical id");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.debug("User '{}' is saving wallet with id {} backup state {}",
              currentUserId,
              walletId,
              backedUp);
    try {
      Wallet wallet = accountService.saveWalletBackupState(currentUserId, walletId, backedUp);
      return Response.ok(wallet).build();
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not allowed to change backup state of wallet with id {}", currentUserId, walletId, e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving wallet backup state: User {} attempts to change backup state wallet with id {}",
                currentUserId,
                walletId,
                e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveAddress")
  @RolesAllowed("users")
  @ApiOperation(value = "Associates a wallet address to a user or a space", httpMethod = "POST", consumes = "application/json", response = Response.class, notes = "returns the generated password for newly saved wallet")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response saveWallet(@ApiParam(value = "wallet details to save", required = true) Wallet wallet) {
    if (wallet == null) {
      LOG.warn("Bad request sent to server with empty data");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.debug("User '{}' is saving new wallet address for {} '{}' with address '{}'",
              currentUserId,
              wallet.getType(),
              wallet.getId(),
              wallet.getAddress());
    try {
      Wallet storedWallet = accountService.getWalletByTypeAndId(wallet.getType(), wallet.getId(), currentUserId);
      if (storedWallet == null || StringUtils.isBlank(storedWallet.getAddress())) {
        accountService.saveWalletAddress(wallet, currentUserId);
        return Response.ok(wallet.getPassPhrase()).build();
      } else {
        storedWallet.setAddress(wallet.getAddress());
        accountService.saveWalletAddress(storedWallet, currentUserId);
        return Response.ok(storedWallet.getPassPhrase()).build();
      }
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' is not allowed to save wallet {}", currentUserId, wallet, e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving address: User " + currentUserId + " attempts to save address of "
          + wallet.getType() + " '" + wallet.getId() + "' using address " + wallet.getAddress(), e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @POST
  @Path("savePrivateKey")
  @RolesAllowed("users")
  @ApiOperation(value = "Save encrypted private key of a wallet", httpMethod = "POST", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response savePrivateKey(@ApiParam(value = "wallet address to save its encrypted private key", required = true) @FormParam("address") String address,
                                 @ApiParam(value = "encrypted wallet private key", required = true) @FormParam("privateKey") String privateKey) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    if (StringUtils.isBlank(privateKey)) {
      LOG.warn("Empty private key content");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.info("User '{}' is saving new wallet private key for address {}",
             currentUserId,
             address);
    try {
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug(WALLET_NOT_FOUND_MESSAGE, address);
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }
      accountService.savePrivateKeyByTypeAndId(wallet.getType(), wallet.getId(), privateKey, currentUserId);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error saving wallet private key by user '{}' for address {}",
               currentUserId,
               address,
               e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving wallet private key: User {} attempts to save wallet private key of address '{}'",
                currentUserId,
                address,
                e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @GET
  @Path("getPrivateKey")
  @RolesAllowed("users")
  @ApiOperation(value = "Get encrypted private key of a wallet", httpMethod = "GET", response = Response.class, notes = "returns encoded wallet private key in String format")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response getPrivateKey(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug(WALLET_NOT_FOUND_MESSAGE, address);
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }

      String privateKeyEncrypted = accountService.getPrivateKeyByTypeAndId(wallet.getType(), wallet.getId(), currentUserId);
      return Response.ok(privateKeyEncrypted).build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error getting wallet private key by user '{}' for address {}",
               currentUserId,
               address,
               e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while user {} attempts to get wallet private key with address {}",
                currentUserId,
                address,
                e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @GET
  @Path("removePrivateKey")
  @RolesAllowed("users")
  @ApiOperation(value = "Removes associated private key of a wallet", httpMethod = "GET", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response removePrivateKey(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.info("User '{}' is removing wallet private key stored on server for address {}",
             currentUserId,
             address);
    try {
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug(WALLET_NOT_FOUND_MESSAGE, address);
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }
      accountService.removePrivateKeyByTypeAndId(wallet.getType(), wallet.getId(), currentUserId);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error removing wallet private key by user '{}' for address {}",
               currentUserId,
               address,
               e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving wallet private key: User {} attempts to save wallet private key of address {}",
                currentUserId,
                address,
                e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveOrDeleteAddressLabel")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Saves or deletes a label associated to an address. If label is empty, then deletes it, else saves it.", httpMethod = "POST", consumes = "application/json", produces = "application/json", response = Response.class, notes = "returns saved label object")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response saveOrDeleteAddressLabel(@ApiParam(value = "blockchain address label", required = true) WalletAddressLabel label) {
    if (label == null) {
      LOG.warn("Bad request sent to server with empty data");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      label = accountService.saveOrDeleteAddressLabel(label, getCurrentUserId());
      return Response.ok(label).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving address label: User " + getCurrentUserId() + ", label: {}", label, e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("requestFunds")
  @RolesAllowed("users")
  @ApiOperation(value = "Sends a fund request to a user or space", httpMethod = "POST", consumes = "application/json", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response requestFunds(@ApiParam(value = "funds request object", required = true) FundsRequest fundsRequest) {
    if (fundsRequest == null) {
      LOG.warn("Bad request sent to server with empty funds request");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    if (StringUtils.isBlank(fundsRequest.getAddress())) {
      LOG.warn("Bad request sent to server with empty sender address");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String receipientRemoteId = fundsRequest.getReceipient();
    String receipientType = fundsRequest.getReceipientType();

    if (StringUtils.isBlank(receipientRemoteId) || StringUtils.isBlank(receipientType)) {
      LOG.warn("Bad request sent to server with empty receipient");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    try {
      walletService.requestFunds(fundsRequest, getCurrentUserId());
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (IllegalStateException e) {
      LOG.error("Unknown error occurred while user '" + getCurrentUserId() + "' requesting funds for wallet  '"
          + fundsRequest.getAddress() + "'", e);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while user '" + getCurrentUserId() + "' requesting funds for wallet  '"
          + fundsRequest.getAddress() + "'", e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @GET
  @Path("markFundRequestAsSent")
  @RolesAllowed("users")
  @ApiOperation(value = "Mark a web notification of funds request as sent", httpMethod = "GET", response = Response.class, notes = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response markFundRequestAsSent(@ApiParam(value = "web notification id", required = true) @QueryParam("notificationId") String notificationId) {
    if (StringUtils.isBlank(notificationId)) {
      LOG.warn("Bad request sent to server with empty notificationId");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUser = getCurrentUserId();
    try {
      walletService.markFundRequestAsSent(notificationId, currentUser);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (IllegalStateException e) {
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while marking fund request with id '" + notificationId + "' for user '"
          + getCurrentUserId() + "'", e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @GET
  @Path("fundRequestSent")
  @Produces(MediaType.TEXT_PLAIN)
  @RolesAllowed("users")
  @ApiOperation(value = "Returns fund request status", httpMethod = "GET", response = Response.class, notes = "returns true if notification sent, else false")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response isFundRequestSent(@ApiParam(value = "web notification id", required = true) @QueryParam("notificationId") String notificationId) {
    if (StringUtils.isBlank(notificationId)) {
      LOG.warn("Bad request sent to server with empty notificationId");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }

    String currentUser = getCurrentUserId();
    try {
      boolean fundRequestSent = walletService.isFundRequestSent(notificationId, currentUser);
      return Response.ok(String.valueOf(fundRequestSent)).build();
    } catch (IllegalAccessException e) {
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (IllegalStateException e) {
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving fund request status", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("list")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "Get list of wallet accounts", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "returns array of wallets objects")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response getWallets() {
    try {
      Set<Wallet> wallets = accountService.listWallets();
      return Response.ok(wallets).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving list of wallets", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("refreshWalletFromBlockchain")
  @RolesAllowed("rewarding")
  @ApiOperation(value = "force refresh wallet from blockchain", httpMethod = "GET", response = Response.class, notes = "returns no response")
  @ApiResponses(value = {
      @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "Invalid query input"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = HTTPStatus.INTERNAL_ERROR, message = "Internal server error") })
  public Response refreshWalletFromBlockchain(@ApiParam(value = "wallet address", required = true) @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Bad request sent to server with empty wallet address in parameter");
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet == null) {
        LOG.warn("Bad request sent to server with unknown wallet address: {}", address);
        return Response.status(HTTPStatus.BAD_REQUEST).build();
      }
      accountService.refreshWalletFromBlockchain(wallet, null, null);
      return Response.status(HTTPStatus.NO_CONTENT).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving list of wallets", e);
      return Response.serverError().build();
    }
  }
}
