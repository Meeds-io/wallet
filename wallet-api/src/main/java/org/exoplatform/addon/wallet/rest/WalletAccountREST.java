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

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.addon.wallet.model.transaction.FundsRequest;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.service.WalletService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to retrieve detailed information about
 * users and spaces with the associated Ethereum account addresses
 */
@Path("/wallet/api/account")
@RolesAllowed("users")
public class WalletAccountREST implements ResourceContainer {

  private static final String  WALLET_NOT_FOUND_MESSAGE = "Wallet was not found with address {}";

  private static final String  BAD_REQUEST_MESSAGE      = "Bad request sent to server with empty address";

  private static final String  EMPTY_ADDRESS_ERROR      = "Bad request sent to server with empty address {}";

  private static final Log     LOG                      = ExoLogger.getLogger(WalletAccountREST.class);

  private WalletService        walletService;

  private WalletAccountService accountService;

  public WalletAccountREST(WalletService walletService, WalletAccountService accountService) {
    this.walletService = walletService;
    this.accountService = accountService;
  }

  /**
   * Retrieves the user or space details by username or space pretty name
   * 
   * @param remoteId username or space pretty name
   * @param type 'user' or 'space'
   * @return Rest Response with wallet details object
   */
  @Path("detailsById")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getWalletByTypeAndID(@QueryParam("id") String remoteId, @QueryParam("type") String type) {
    if (StringUtils.isBlank(remoteId) || StringUtils.isBlank(type)) {
      LOG.warn("Bad request sent to server with id '{}' and type '{}'", remoteId, type);
      return Response.status(400).build();
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

  /**
   * Retrieves the user or space details associated to an address
   * 
   * @param address wallet address
   * @return Rest Response with wallet details object
   */
  @Path("detailsByAddress")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getWalletByAddress(@QueryParam("address") String address) {
    try {
      if (StringUtils.isBlank(address)) {
        LOG.warn(EMPTY_ADDRESS_ERROR, address);
        return Response.status(400).build();
      }
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet != null) {
        if (WalletType.isSpace(wallet.getType())) {
          wallet.setSpaceAdministrator(isUserSpaceManager(wallet.getId(), getCurrentUserId()));
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

  /**
   * Enable/Disable wallet
   * 
   * @param address address of wallet to enable/disable
   * @param enable if true enable wallet else disable it
   * @return Rest Response of request
   */
  @Path("enable")
  @GET
  @RolesAllowed("rewarding")
  public Response enableWalletByAddress(@QueryParam("address") String address, @QueryParam("enable") boolean enable) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }
    try {
      accountService.enableWalletByAddress(address, enable, getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Can't delete address '{}' association", address, e);
      return Response.serverError().build();
    }
  }

  /**
   * Remove the user or space details associated to an address
   * 
   * @param address wallet address to remove
   * @return Rest Response of request
   */
  @Path("remove")
  @GET
  @RolesAllowed("rewarding")
  public Response removeWalletByAddress(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }
    try {
      accountService.removeWalletByAddress(address, getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Can't delete address '{}' association", address, e);
      return Response.serverError().build();
    }
  }

  @Path("setInitializationStatus")
  @GET
  @RolesAllowed("rewarding")
  public Response setInitializationStatus(@QueryParam("address") String address, @QueryParam("status") String status) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }
    try {
      accountService.setInitializationStatus(address,
                                             WalletInitializationState.valueOf(status.toUpperCase()),
                                             getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Can't set wallet initialized status '{}'", status, e);
      return Response.serverError().build();
    }
  }

  @Path("requestAuthorization")
  @GET
  public Response requestAuthorization(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(EMPTY_ADDRESS_ERROR, address);
      return Response.status(400).build();
    }
    try {
      accountService.setInitializationStatus(address,
                                             WalletInitializationState.MODIFIED,
                                             getCurrentUserId());
      return Response.ok().build();
    } catch (Exception e) {
      LOG.warn("Can't request authorization for wallet {}", address, e);
      return Response.serverError().build();
    }
  }

  /**
   * Save address a user or space associated address
   * 
   * @param wallet wallet details to save
   * @return Rest Response with saved pass phrase of wallet
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveAddress")
  @RolesAllowed("users")
  public Response saveWallet(Wallet wallet) {
    if (wallet == null) {
      LOG.warn("Bad request sent to server with empty data");
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.debug("User '{}' is saving new wallet address for {} '{}' with address '{}'",
              currentUserId,
              wallet.getType(),
              wallet.getId(),
              wallet.getAddress());
    try {
      Wallet storedWallet = accountService.getWalletByTypeAndId(wallet.getType(), wallet.getId(), currentUserId);
      if (storedWallet == null) {
        accountService.saveWalletAddress(wallet, currentUserId, true);
        return Response.ok(wallet.getPassPhrase()).build();
      } else {
        storedWallet.setAddress(wallet.getAddress());
        accountService.saveWalletAddress(storedWallet, currentUserId, true);
        return Response.ok(storedWallet.getPassPhrase()).build();
      }
    } catch (IllegalAccessException | IllegalStateException e) {
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving address: User " + currentUserId + " attempts to save address of "
          + wallet.getType() + " '" + wallet.getId() + "' using address " + wallet.getAddress(), e);
      return Response.status(500).build();
    }
  }

  /**
   * Save private key of a wallet in server side
   * 
   * @param address wallet address
   * @param privateKey encrypted wallet private key
   * @return Rest Response with operation status
   */
  @POST
  @Path("savePrivateKey")
  @RolesAllowed("users")
  public Response savePrivateKey(@FormParam("address") String address,
                                 @FormParam("remoteId") String remoteId,
                                 @FormParam("privateKey") String privateKey) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(BAD_REQUEST_MESSAGE);
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.info("User '{}' is saving new wallet private key for address {}",
             currentUserId,
             address);
    try {
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug(WALLET_NOT_FOUND_MESSAGE, address);
        return Response.status(400).build();
      }
      accountService.savePrivateKeyByTypeAndId(wallet.getType(), wallet.getId(), privateKey, currentUserId);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error saving wallet private key by user '{}' for address {}",
               currentUserId,
               address,
               e);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving wallet private key: User {} attempts to save wallet private key of address '{}'",
                currentUserId,
                address,
                e);
      return Response.status(500).build();
    }
  }

  /**
   * Get private key of a wallet stored in server side
   * 
   * @param address wallet address
   * @return Rest Response with operation status
   */
  @GET
  @Path("getPrivateKey")
  @RolesAllowed("users")
  public Response getPrivateKey(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(BAD_REQUEST_MESSAGE);
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    try {
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug(WALLET_NOT_FOUND_MESSAGE, address);
        return Response.status(400).build();
      }

      String privateKeyEncrypted = accountService.getPrivateKeyByTypeAndId(wallet.getType(), wallet.getId(), currentUserId);
      return Response.ok(privateKeyEncrypted).build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error getting wallet private key by user '{}' for address {}",
               currentUserId,
               address,
               e);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while user {} attempts to get wallet private key with address {}",
                currentUserId,
                address,
                e);
      return Response.status(500).build();
    }
  }

  /**
   * Get private key of a wallet stored in server side
   * 
   * @param address wallet address
   * @return Rest Response with operation status
   */
  @GET
  @Path("removePrivateKey")
  @RolesAllowed("users")
  public Response removePrivateKey(@QueryParam("address") String address) {
    if (StringUtils.isBlank(address)) {
      LOG.warn(BAD_REQUEST_MESSAGE);
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    LOG.info("User '{}' is removing wallet private key stored on server for address {}",
             currentUserId,
             address);
    try {
      Wallet wallet = accountService.getWalletByAddress(address);
      if (wallet == null) {
        LOG.debug(WALLET_NOT_FOUND_MESSAGE, address);
        return Response.status(400).build();
      }
      accountService.removePrivateKeyByTypeAndId(wallet.getType(), wallet.getId(), currentUserId);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      LOG.warn("Error removing wallet private key by user '{}' for address {}",
               currentUserId,
               address,
               e);
      return Response.status(403).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving wallet private key: User {} attempts to save wallet private key of address {}",
                currentUserId,
                address,
                e);
      return Response.status(500).build();
    }
  }

  /**
   * Save wallet address label
   * 
   * @param label a label details to save for a given address
   * @return Rest Response with saved label detail
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveOrDeleteAddressLabel")
  @RolesAllowed("rewarding")
  public Response saveOrDeleteAddressLabel(WalletAddressLabel label) {
    if (label == null) {
      LOG.warn("Bad request sent to server with empty data");
      return Response.status(400).build();
    }

    try {
      label = accountService.saveOrDeleteAddressLabel(label, getCurrentUserId());
      return Response.ok(label).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while saving address label: User " + getCurrentUserId() + ", label: {}", label, e);
      return Response.status(500).build();
    }
  }

  /**
   * Sends a fund request notifications
   * 
   * @param fundsRequest fund request details to send
   * @return Rest Response of request
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("requestFunds")
  @RolesAllowed("users")
  public Response requestFunds(FundsRequest fundsRequest) {
    if (fundsRequest == null) {
      LOG.warn("Bad request sent to server with empty funds request");
      return Response.status(400).build();
    }

    if (StringUtils.isBlank(fundsRequest.getAddress())) {
      LOG.warn("Bad request sent to server with empty sender address");
      return Response.status(400).build();
    }

    String receipientRemoteId = fundsRequest.getReceipient();
    String receipientType = fundsRequest.getReceipientType();

    if (StringUtils.isBlank(receipientRemoteId) || StringUtils.isBlank(receipientType)) {
      LOG.warn("Bad request sent to server with empty receipient");
      return Response.status(400).build();
    }

    try {
      walletService.requestFunds(fundsRequest, getCurrentUserId());
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while user '" + getCurrentUserId() + "' requesting funds for wallet  '"
          + fundsRequest.getAddress() + "'", e);
      return Response.status(500).build();
    }
  }

  /**
   * Mark a notification as sent
   * 
   * @param notificationId web notification id of fund request to mark as sent
   * @return Rest Response of request
   */
  @GET
  @Path("markFundRequestAsSent")
  @RolesAllowed("users")
  public Response markFundRequestAsSent(@QueryParam("notificationId") String notificationId) {
    if (StringUtils.isBlank(notificationId)) {
      LOG.warn("Bad request sent to server with empty notificationId");
      return Response.status(400).build();
    }

    String currentUser = getCurrentUserId();
    try {
      walletService.markFundRequestAsSent(notificationId, currentUser);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while marking fund request with id '" + notificationId + "' for user '"
          + getCurrentUserId() + "'", e);
      return Response.status(500).build();
    }
  }

  /**
   * Returns fund request status
   * 
   * @param notificationId web notification id of fund request to check if sent
   * @return Rest Response 'true' if sent else 'false'
   */
  @GET
  @Path("fundRequestSent")
  @Produces(MediaType.TEXT_PLAIN)
  @RolesAllowed("users")
  public Response isFundRequestSent(@QueryParam("notificationId") String notificationId) {
    if (StringUtils.isBlank(notificationId)) {
      LOG.warn("Bad request sent to server with empty notificationId");
      return Response.status(400).build();
    }

    String currentUser = getCurrentUserId();
    try {
      boolean fundRequestSent = walletService.isFundRequestSent(notificationId, currentUser);
      return Response.ok(String.valueOf(fundRequestSent)).build();
    } catch (IllegalAccessException e) {
      return Response.status(403).build();
    } catch (IllegalStateException e) {
      return Response.status(400).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving fund request status", e);
      return Response.serverError().build();
    }
  }

  /**
   * Get list of wallet accounts
   * 
   * @return Rest Responseof type json with the list of wallets
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("list")
  @RolesAllowed("rewarding")
  public Response getWallets() {
    try {
      return Response.ok(accountService.listWallets()).build();
    } catch (Exception e) {
      LOG.warn("Error retrieving list of wallets", e);
      return Response.serverError().build();
    }
  }
}
