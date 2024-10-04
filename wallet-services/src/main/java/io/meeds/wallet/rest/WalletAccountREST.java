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
package io.meeds.wallet.rest;

import static io.meeds.wallet.utils.WalletUtils.*;

import java.util.Set;

import javax.annotation.security.RolesAllowed;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.meeds.wallet.model.*;
import io.meeds.wallet.service.WalletAccountService;
import io.meeds.wallet.service.WalletService;
import io.meeds.wallet.utils.WalletUtils;


@Path("/wallet/api/account")
@Tag(name = "/wallet/api/account", description = "Manages wallets objects associated to users, spaces and admin")
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
  @Operation(
          summary = "Retrieves the user or space wallet identified by username or space pretty name",
          method = "GET",
          description = "returns the associated Wallet object, if not found it will return an empty object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getWalletByTypeAndID(@Parameter(description = "username or space pretty name", required = true) @QueryParam("id") String remoteId,
                                       @Parameter(description = "'user' or 'space'", required = true) @QueryParam("type") String type) {
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
  @Operation(
          summary = "Retrieves the user or space wallet identified by an address",
          method = "GET",
          description = "returns the associated Wallet object, if not found it will return an empty object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getWalletByAddress(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address) {
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
  @Operation(
          summary = "Enable or disable a wallet identified by its address",
          method = "GET",
          description = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response enableWalletByAddress(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address,
                                        @Parameter(description = "true to enable wallet, else false", required = true) @QueryParam("enable") boolean enable) {
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
  @Operation(
          summary = "Modify initialization status of wallet",
          method = "GET",
          description = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response setInitializationStatus(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address,
                                          @Parameter(description = "initialization status: new, modified, pending, initialized or denied", required = true) @QueryParam("status") String status) {
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
              WalletState.valueOf(status.toUpperCase()),
                                             getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Can't set wallet initialized status '{}'", status, e);
      return Response.serverError().build();
    }
  }
  @Path("deleteWallet")
  @GET
  @RolesAllowed("users")
  @Operation(
          summary = "Modify initialization status of wallet to delete",
          method = "GET",
          description = "returns empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteWallet(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      accountService.setInitializationStatus(address,
                                             WalletState.DELETED,
                                             getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Can't set wallet status '{}'", WalletState.DELETED, e);
      return Response.serverError().build();
    }
  }

  @Path("requestAuthorization")
  @GET
  @Operation(
          summary = "Modify initialization status from DENIED to MODIFIED",
          description = "This is used in case when a wallet has been denied access, in that case, a new authorization request can be done",
          method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response requestAuthorization(@Parameter(description = "wallet address to change its status", required = true) @QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_MESSAGE);
      return Response.status(HTTPStatus.BAD_REQUEST).build();
    }
    try {
      accountService.setInitializationStatus(address,
                                             WalletState.MODIFIED,
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
  @Operation(
          summary = "Saves wallet backup state",
          method = "POST",
          description = "Saves wallet backup state and returns the modified wallet")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveWalletBackupState(@Parameter(description = "wallet technical id", required = true) @FormParam("walletId") long walletId,
                                        @Parameter(description = "whether wallet backedUp or not", required = true) @FormParam("backedUp") boolean backedUp) {
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
  @Operation(
          summary = "Associates a wallet address to a user or a space",
          method = "POST",
          description = "Associates a wallet address to a user or a space and returns the generated password for newly saved wallet")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "409", description = "Conflicted operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveWallet(@Parameter(description = "wallet details to save", required = true) Wallet wallet) {
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
        storedWallet.setBackedUp(false);
        accountService.saveWalletAddress(storedWallet, currentUserId);
        return Response.ok(storedWallet.getPassPhrase()).build();
      }
    } catch (AddressAlreadyInUseException e) {
      LOG.warn("User '{}' is attempting to add a wallet address {} that is already in use",
               currentUserId,
               wallet.getAddress());
      return Response.status(HTTPStatus.CONFLICT).build();
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
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Path("provider")
  @RolesAllowed("users")
  @Operation(
          summary = "Switches user wallet provider",
          method = "POST",
          description = "Switches user wallet provider and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveWalletProvider(@Parameter(description = "New Wallet provider", required = true)  @FormParam("provider")
                                       WalletProvider provider,
                                     @Parameter(description = "Selected Wallet Address of provider")  @FormParam("address")
                                     String address,
                                     @Parameter(description = "Signed Raw message by external Wallet Provider")  @FormParam("rawMessage")
                                     String rawMessage,
                                     @Parameter(description = "Signed message by external Wallet Provider")  @FormParam("signedMessage")
                                     String signedMessage,
                                     @Context HttpServletRequest request) {
    if (provider == null) {
      return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad request sent to server with empty provider").build();
    }

    if (provider != WalletProvider.INTERNAL_WALLET) {
      if (StringUtils.isBlank(address)) {
        return Response.status(HTTPStatus.BAD_REQUEST).entity(EMPTY_ADDRESS_MESSAGE).build();
      } else if (StringUtils.isBlank(rawMessage) || StringUtils.isBlank(signedMessage)) {
        return Response.status(HTTPStatus.BAD_REQUEST)
                       .entity("Must Sign a raw message to verify that user has the private key of selected address")
                       .build();
      }
    }

    long currentUserIdentityId = getCurrentUserIdentityId();
    LOG.debug("User '{}' is saving new provider for his wallet", currentUserIdentityId);
    try {
      if (provider == WalletProvider.INTERNAL_WALLET) {
        accountService.switchToInternalWallet(currentUserIdentityId);
      } else {
        String token = WalletUtils.getToken(request.getSession());
        if (!StringUtils.contains(rawMessage, token)) {
          return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad request sent to server with invalid signed message").build();
        }
        accountService.switchWalletProvider(currentUserIdentityId, provider, address, rawMessage, signedMessage);
      }
      return Response.noContent().build();
    } catch (AddressAlreadyInUseException e) {
      LOG.warn("User '{}' is attempting to add a wallet address {} that is already in use",
               currentUserIdentityId,
               address,
               e);
      return Response.status(HTTPStatus.CONFLICT).build();
    } catch (Exception e) {
      LOG.warn("Unknown error occurred while switching identity '{}' wallet provider to '{}' ",
               currentUserIdentityId,
               provider,
               e);
      return Response.status(HTTPStatus.INTERNAL_ERROR).entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("savePrivateKey")
  @RolesAllowed("users")
  @Operation(
          summary = "Save encrypted private key of a wallet",
          method = "POST",
          description = "Save encrypted private key of a wallet and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response savePrivateKey(@Parameter(description = "wallet address to save its encrypted private key", required = true) @FormParam("address") String address,
                                 @Parameter(description = "encrypted wallet private key", required = true) @FormParam("privateKey") String privateKey) {
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
  @Operation(
          summary = "Get encrypted private key of a wallet",
          method = "GET",
          description = "returns encoded wallet private key in String format")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getPrivateKey(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address) {
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
  @Operation(
          summary = "Removes associated private key of a wallet",
          method = "GET",
          description = "Removes associated private key of a wallet and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response removePrivateKey(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address) {
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
  @Operation(
          summary = "Saves or deletes a label associated to an address",
          method = "POST",
          description = "Saves or deletes a label associated to an address. If label is empty, then deletes it, else saves it. returns saved label object")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response saveOrDeleteAddressLabel(@RequestBody(description = "blockchain address label", required = true) WalletAddressLabel label) {
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
  @Operation(
          summary = "Sends a fund request to a user or space",
          method = "POST",
          description = "Sends a fund request to a user or space and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response requestFunds(@Parameter(description = "funds request object", required = true) FundsRequest fundsRequest) {
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
  @Operation(
          summary = "Mark a web notification of funds request as sent",
          method = "GET",
          description = "Mark a web notification of funds request as sent and returns an empty response")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response markFundRequestAsSent(@Parameter(description = "web notification id", required = true) @QueryParam("notificationId") String notificationId) {
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
  @Operation(
          summary = "Returns fund request status",
          method = "GET",
          description = "returns fund request status (true if notification sent, else false)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response isFundRequestSent(@Parameter(description = "web notification id", required = true) @QueryParam("notificationId") String notificationId) {
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
  @Operation(
          summary = "Get list of wallet accounts",
          method = "GET",
          description = "Get list of wallet accounts (array of wallets objects)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
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
  @RolesAllowed("users")
  @Operation(
          summary = "force refresh wallet from blockchain",
          method = "GET",
          description = "force refresh wallet from blockchain")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response refreshWalletFromBlockchain(@Parameter(description = "wallet address", required = true) @QueryParam("address") String address) {
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
