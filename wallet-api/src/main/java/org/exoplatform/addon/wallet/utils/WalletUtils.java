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
package org.exoplatform.addon.wallet.utils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.*;

import org.exoplatform.addon.wallet.model.*;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.ws.frameworks.json.JsonGenerator;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.*;

/**
 * Utils class to provide common tools and constants
 */
public class WalletUtils {
  private static final Log LOG = ExoLogger.getLogger(WalletUtils.class);

  private WalletUtils() {
  }

  @SuppressWarnings("all")
  public static final char[]                          SIMPLE_CHARS                          = new char[] { 'A', 'B', 'C', 'D',
      'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c',
      'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1',
      '2', '3', '4', '5', '6', '7', '8', '9' };

  public static final int                             ETHER_TO_WEI_DECIMALS                 = 18;

  public static final JsonParser                      JSON_PARSER                           = new JsonParserImpl();

  public static final JsonGenerator                   JSON_GENERATOR                        = new JsonGeneratorImpl();

  public static final String                          EMPTY_HASH                            =
                                                                 "0x0000000000000000000000000000000000000000000000000000000000000000";

  public static final String                          DEFAULT_NETWORK_ID                    = "defaultNetworkId";

  public static final String                          DEFAULT_NETWORK_URL                   = "defaultNetworkURL";

  public static final String                          DEFAULT_NETWORK_WS_URL                = "defaultNetworkWSURL";

  public static final String                          DEFAULT_ACCESS_PERMISSION             = "defaultAccessPermission";

  public static final String                          LAST_BLOCK_NUMBER_KEY_NAME            =
                                                                                 "ADDONS_ETHEREUM_LAST_BLOCK_NUMBER";

  public static final String                          DEFAULT_GAS                           = "defaultGas";

  public static final String                          MIN_GAS_PRICE                         = "minGasPrice";

  public static final String                          NORMAL_GAS_PRICE                      = "normalGasPrice";

  public static final String                          MAX_GAS_PRICE                         = "maxGasPrice";

  public static final String                          DEFAULT_CONTRACTS_ADDRESSES           = "defaultContractAddresses";

  public static final String                          SCOPE_NAME                            = "ADDONS_ETHEREUM_WALLET";

  public static final String                          GLOBAL_SETTINGS_KEY_NAME              = "GLOBAL_SETTINGS";

  public static final String                          SETTINGS_KEY_NAME                     = "ADDONS_ETHEREUM_WALLET_SETTINGS";

  public static final Context                         WALLET_CONTEXT                        = Context.GLOBAL;

  public static final Scope                           WALLET_SCOPE                          = Scope.APPLICATION.id(SCOPE_NAME);

  public static final String                          WALLET_DEFAULT_CONTRACTS_NAME         = "WALLET_DEFAULT_CONTRACTS";

  public static final String                          WALLET_USER_TRANSACTION_NAME          = "WALLET_USER_TRANSACTION";

  public static final String                          WALLET_BROWSER_PHRASE_NAME            = "WALLET_BROWSER_PHRASE";

  public static final String                          ADMIN_KEY_PARAMETER                   = "admin.wallet.key";

  public static final String                          ABI_PATH_PARAMETER                    = "contract.abi.path";

  public static final String                          BIN_PATH_PARAMETER                    = "contract.bin.path";

  public static final int                             GLOBAL_DATA_VERSION                   = 6;

  public static final int                             USER_DATA_VERSION                     = 1;

  public static final int                             DEFAULT_GAS_UPGRADE_VERSION           = 1;

  public static final int                             DEFAULT_GAS_PRICE_UPGRADE_VERSION     = 1;

  public static final String                          ADMINISTRATORS_GROUP                  = "/platform/administrators";

  public static final String                          REWARDINGS_GROUP                      = "/platform/rewarding";

  public static final String                          WALLET_ADMIN_REMOTE_ID                = "admin";

  public static final String                          PRINCIPAL_CONTRACT_ADMIN_NAME         = "Admin";

  public static final String                          GLOAL_SETTINGS_CHANGED_EVENT          =
                                                                                   "exo.addon.wallet.settings.changed";

  public static final String                          NEW_ADDRESS_ASSOCIATED_EVENT          =
                                                                                   "exo.addon.wallet.addressAssociation.new";

  public static final String                          ADMIN_WALLET_MODIFIED_EVENT           =
                                                                                  "exo.addon.wallet.admin.modified";

  public static final String                          MODIFY_ADDRESS_ASSOCIATED_EVENT       =
                                                                                      "exo.addon.wallet.addressAssociation.modification";

  public static final String                          KNOWN_TRANSACTION_MINED_EVENT         =
                                                                                    "exo.addon.wallet.transaction.mined";

  public static final String                          NEW_TRANSACTION_EVENT                 =
                                                                            "exo.addon.wallet.transaction.loaded";

  public static final String                          TRANSACTION_PENDING_MAX_DAYS          = "transaction.pending.maxDays";

  public static final String                          WALLET_SENDER_NOTIFICATION_ID         = "EtherSenderNotificationPlugin";

  public static final String                          WALLET_RECEIVER_NOTIFICATION_ID       = "EtherReceiverNotificationPlugin";

  public static final String                          FUNDS_REQUEST_NOTIFICATION_ID         = "FundsRequestNotificationPlugin";

  public static final String                          FUNDS_REQUEST_SENT                    = "sent";

  public static final String                          CONTRACT_ADDRESS                      = "contractAddress";

  public static final String                          AMOUNT                                = "amount";

  public static final String                          SYMBOL                                = "symbol";

  public static final String                          MESSAGE                               = "message";

  public static final String                          HASH                                  = "hash";

  public static final String                          ACCOUNT_TYPE                          = "account_type";

  public static final String                          RECEIVER_TYPE                         = "receiver_type";

  public static final String                          AVATAR                                = "avatar";

  public static final String                          SENDER                                = "sender";

  public static final String                          USER                                  = "userFullname";

  public static final String                          USER_URL                              = "userUrl";

  public static final String                          SENDER_URL                            = "senderUrl";

  public static final String                          RECEIVER                              = "receiver";

  public static final String                          RECEIVER_URL                          = "receiverUrl";

  public static final String                          FUNDS_ACCEPT_URL                      = "fundsAcceptUrl";

  public static final ArgumentLiteral<Wallet>         FUNDS_REQUEST_SENDER_DETAIL_PARAMETER =
                                                                                            new ArgumentLiteral<>(Wallet.class,
                                                                                                                  "senderFullName");

  public static final ArgumentLiteral<Wallet>         SENDER_ACCOUNT_DETAIL_PARAMETER       =
                                                                                      new ArgumentLiteral<>(Wallet.class,
                                                                                                            "senderAccountDetail");

  public static final ArgumentLiteral<Wallet>         RECEIVER_ACCOUNT_DETAIL_PARAMETER     =
                                                                                        new ArgumentLiteral<>(Wallet.class,
                                                                                                              "receiverAccountDetail");

  public static final ArgumentLiteral<FundsRequest>   FUNDS_REQUEST_PARAMETER               =
                                                                              new ArgumentLiteral<>(FundsRequest.class,
                                                                                                    "fundsRequest");

  public static final ArgumentLiteral<ContractDetail> CONTRACT_DETAILS_PARAMETER            =
                                                                                 new ArgumentLiteral<>(ContractDetail.class,
                                                                                                       "contractDetails");

  public static final ArgumentLiteral<Double>         AMOUNT_PARAMETER                      =
                                                                       new ArgumentLiteral<>(Double.class, AMOUNT);

  public static final ArgumentLiteral<String>         MESSAGE_PARAMETER                     =
                                                                        new ArgumentLiteral<>(String.class, MESSAGE);

  public static final ArgumentLiteral<String>         HASH_PARAMETER                        =
                                                                     new ArgumentLiteral<>(String.class, HASH);

  public static final ArgumentLiteral<String>         SYMBOL_PARAMETER                      =
                                                                       new ArgumentLiteral<>(String.class, SYMBOL);

  public static final ArgumentLiteral<String>         CONTRACT_ADDRESS_PARAMETER            =
                                                                                 new ArgumentLiteral<>(String.class,
                                                                                                       CONTRACT_ADDRESS);

  public static final String                          NEW_WALLET_TASK_TYPE                  = "new-wallet";

  public static final String                          MODIFY_WALLET_TASK_TYPE               = "modify-wallet";

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }

  public static List<String> getNotificationReceiversUsers(Wallet toAccount, String excludedId) {
    if (WalletType.isSpace(toAccount.getType())) {
      Space space = getSpace(toAccount.getId());
      if (space == null) {
        return Collections.singletonList(toAccount.getId());
      } else {
        String[] managers = space.getManagers();
        if (managers == null || managers.length == 0) {
          return Collections.emptyList();
        } else if (StringUtils.isBlank(excludedId)) {
          return Arrays.asList(managers);
        } else {
          return Arrays.stream(managers).filter(member -> !excludedId.equals(member)).collect(Collectors.toList());
        }
      }
    } else {
      return Collections.singletonList(toAccount.getId());
    }
  }

  public static String getPermanentLink(Wallet wallet) {
    if (wallet == null) {
      throw new IllegalArgumentException("Wallet is mandatory");
    }
    String remoteId = wallet.getId();
    String walletType = wallet.getType();
    try {
      if (StringUtils.isBlank(remoteId) || StringUtils.isBlank(walletType)
          || (!WalletType.isUser(walletType) && !WalletType.isSpace(walletType))) {
        return wallet.getName();
      } else if (WalletType.isUser(walletType)) {
        return LinkProvider.getProfileLink(remoteId);
      } else if (WalletType.isSpace(walletType)) {
        Space space = getSpace(remoteId);
        if (space == null) {
          throw new IllegalStateException("Can't find space with id " + remoteId);
        }
        return getPermanentLink(space);
      }
    } catch (Exception e) {
      LOG.error("Error getting profile link of {} {}", walletType, remoteId, e);
    }
    return StringUtils.isBlank(wallet.getName()) ? wallet.getAddress() : wallet.getName();
  }

  public static Identity getIdentityById(long identityId) {
    return getIdentityById(String.valueOf(identityId));
  }

  public static Identity getIdentityById(String identityId) {
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    return identityManager.getIdentity(identityId, true);
  }

  public static Identity getIdentityByTypeAndId(WalletType type, String remoteId) {
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    return identityManager.getOrCreateIdentity(type.getProviderId(), remoteId, true);
  }

  public static String getSpacePrettyName(String id) {
    Space space = getSpace(id);
    return space == null ? id : space.getPrettyName();
  }

  public static Space getSpace(String id) {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    if (id.indexOf("/spaces/") >= 0) {
      return spaceService.getSpaceByGroupId(id);
    }
    Space space = spaceService.getSpaceByPrettyName(id);
    if (space == null) {
      space = spaceService.getSpaceByGroupId("/spaces/" + id);
      if (space == null) {
        space = spaceService.getSpaceByDisplayName(id);
        if (space == null) {
          space = spaceService.getSpaceByUrl(id);
          if (space == null) {
            space = spaceService.getSpaceById(id);
          }
        }
      }
    }
    return space;
  }

  public static void computeWalletIdentity(Wallet wallet) {
    if (wallet.getTechnicalId() == 0) {
      // Compute technicalId from type and remoteId

      String remoteId = wallet.getId();
      if (StringUtils.isBlank(remoteId)) {
        throw new IllegalStateException("Wallet identityId and remoteId are empty, thus it can't be saved");
      }

      // If wallet.getType(), we will assume that it will be of user type
      WalletType type = WalletType.getType(wallet.getType());
      if (type.isSpace()) {
        // Ensure to have a fresh prettyName
        remoteId = getSpacePrettyName(remoteId);
        wallet.setId(remoteId);
      }
      Identity identity = getIdentityByTypeAndId(type, remoteId);
      if (identity == null) {
        throw new IllegalStateException("Can't find identity with id " + remoteId + " and type " + type);
      }
      wallet.setType(type.getId());
      wallet.setId(identity.getRemoteId());
      wallet.setTechnicalId(Long.parseLong(identity.getId()));
    } else {
      // Compute type and remoteId from technicalId
      Identity identity = getIdentityById(wallet.getTechnicalId());
      if (identity == null) {
        throw new IllegalStateException("Can't find identity with identity id " + wallet.getTechnicalId());
      }
      WalletType type = WalletType.getType(identity.getProviderId());
      wallet.setType(type.getId());
      wallet.setId(identity.getRemoteId());
    }
  }

  public static final boolean isUserAdmin(String username) {
    return isUserMemberOf(username, ADMINISTRATORS_GROUP);
  }

  public static final boolean isUserRewardingAdmin(String username) {
    return isUserMemberOf(username, REWARDINGS_GROUP);
  }

  public static final boolean isUserMemberOf(String username, String permissionExpression) {
    if (StringUtils.isBlank(permissionExpression)) {
      throw new IllegalArgumentException("Permission expression is mandatory");
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("Username is mandatory");
    }

    if (permissionExpression.contains(":")) {
      throw new UnsupportedOperationException("Permission check with role/membershipType isn't implemented ");
    } else if (permissionExpression.contains("/")) {
      org.exoplatform.services.security.Identity identity = CommonsUtils.getService(IdentityRegistry.class).getIdentity(username);
      if (identity != null) {
        return identity.isMemberOf(permissionExpression);
      }

      Collection<Group> groupsOfUser;
      try {
        groupsOfUser = CommonsUtils.getService(OrganizationService.class).getGroupHandler().findGroupsOfUser(username);
      } catch (Exception e) {
        LOG.warn("Error getting groups of user " + username);
        return false;
      }
      if (groupsOfUser == null || groupsOfUser.isEmpty()) {
        return false;
      }
      for (Group group : groupsOfUser) {
        if (permissionExpression.equals(group.getId())) {
          return true;
        }
      }
      return false;
    } else {
      return StringUtils.equals(username, permissionExpression);
    }
  }

  public static String getWalletLink(String receiverType, String receiverId) {
    if (receiverType == null || receiverId == null || WalletType.isUser(receiverType)) {
      return CommonsUtils.getCurrentDomain() + getMyWalletLink();
    } else {
      Space space = getSpace(receiverId);
      if (space == null) {
        return CommonsUtils.getCurrentDomain() + getMyWalletLink();
      } else {
        String groupId = space.getGroupId().split("/")[2];
        return CommonsUtils.getCurrentDomain() + LinkProvider.getActivityUriForSpace(space.getPrettyName(), groupId)
            + "/EthereumSpaceWallet";
      }
    }
  }

  public static String getMyWalletLink() {
    UserPortalConfigService userPortalConfigService = CommonsUtils.getService(UserPortalConfigService.class);
    return "/" + PortalContainer.getInstance().getName() + "/" + userPortalConfigService.getDefaultPortal() + "/wallet";
  }

  public static String getPermanentLink(Space space) {
    if (space == null) {
      return null;
    }
    String groupId = space.getGroupId().split("/")[2];
    String spaceUrl = LinkProvider.getActivityUriForSpace(space.getPrettyName(), groupId);
    if (StringUtils.isBlank(spaceUrl)) {
      return CommonsUtils.getCurrentDomain();
    }

    spaceUrl = CommonsUtils.getCurrentDomain() + spaceUrl;
    return new StringBuilder("<a href=\"").append(spaceUrl)
                                          .append("\" target=\"_blank\">")
                                          .append(StringEscapeUtils.escapeHtml(space.getDisplayName()))
                                          .append("</a>")
                                          .toString();
  }

  public static Set<String> jsonArrayToList(JSONObject jsonObject, String key) throws JSONException {
    Set<String> set = null;
    if (jsonObject.has(key)) {
      set = new HashSet<>();
      JSONArray arrayValue = jsonObject.getJSONArray(key);
      for (int i = 0; i < arrayValue.length(); i++) {
        set.add(arrayValue.getString(i));
      }
    }
    return set;
  }

  public static String encodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLEncoder.encode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }

  public static String decodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLDecoder.decode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }

  public static boolean isUserSpaceManager(String id, String modifier) {
    try {
      return checkUserIsSpaceManager(id, modifier, false);
    } catch (IllegalAccessException e) {
      return false;
    }
  }

  /**
   * Return true if user can access wallet detailed information
   * 
   * @param wallet wallet details to check
   * @param currentUser user accessing wallet details
   * @return true if has access, else false
   */
  public static boolean canAccessWallet(Wallet wallet, String currentUser) {
    String remoteId = wallet.getId();
    WalletType type = WalletType.getType(wallet.getType());
    boolean isUserAdmin = isUserAdmin(currentUser);

    if (isUserAdmin) {
      return true;
    }

    return (type.isUser() && StringUtils.equals(currentUser, remoteId))
        || (type.isSpace() && isUserSpaceMember(wallet.getId(), currentUser))
        || (type.isAdmin() && (isUserAdmin(currentUser) || isUserRewardingAdmin(currentUser)));
  }

  public static boolean isUserSpaceMember(String spaceId, String accesssor) {
    Space space = getSpace(spaceId);
    if (space == null) {
      LOG.warn("Space not found with id '{}'", spaceId);
      throw new IllegalStateException("Space not found with id '" + spaceId + "'");
    }
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    return spaceService.isSuperManager(accesssor)
        || spaceService.isMember(space, accesssor)
        || spaceService.isManager(space, accesssor);
  }

  public static boolean checkUserIsSpaceManager(String spaceId,
                                                String modifier,
                                                boolean throwException) throws IllegalAccessException {
    Space space = getSpace(spaceId);
    if (space == null) {
      LOG.warn("Space not found with id '{}'", spaceId);
      throw new IllegalStateException();
    }
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    if (!spaceService.isManager(space, modifier) && !spaceService.isSuperManager(modifier)) {
      if (throwException) {
        LOG.error("User '{}' attempts to access wallet address of space '{}'", modifier, space.getDisplayName());
        throw new IllegalAccessException();
      } else {
        return false;
      }
    }
    return true;
  }

  public static final void computeWalletFromIdentity(Wallet wallet, Identity identity) {
    WalletType walletType = WalletType.getType(identity.getProviderId());
    wallet.setId(identity.getRemoteId());
    wallet.setTechnicalId(Long.parseLong(identity.getId()));
    wallet.setDisabledUser(!identity.isEnable());
    wallet.setDeletedUser(identity.isDeleted());
    wallet.setType(walletType.getId());
    if (walletType.isUser() || walletType.isSpace()) {
      wallet.setAvatar(LinkProvider.buildAvatarURL(identity.getProviderId(), identity.getRemoteId()));
    }
    if (walletType.isUser()) {
      wallet.setName(identity.getProfile().getFullName());
    } else if (walletType.isAdmin()) {
      if (StringUtils.equals(identity.getRemoteId(), WALLET_ADMIN_REMOTE_ID)) {
        wallet.setName(PRINCIPAL_CONTRACT_ADMIN_NAME);
      } else {
        // Space auto generated wallet
      }
    } else if (walletType.isSpace()) {
      Space space = getSpace(identity.getRemoteId());
      wallet.setName(space.getDisplayName());
      wallet.setSpaceId(Long.parseLong(space.getId()));
    }
  }

  public static final String formatTransactionHash(String transactionHash) {
    if (transactionHash == null) {
      return null;
    }
    transactionHash = transactionHash.trim().toLowerCase();
    if (transactionHash.length() == 64 && !transactionHash.startsWith("0x")) {
      transactionHash = "0x" + transactionHash;
    }
    if (transactionHash.length() != 66) {
      throw new IllegalStateException("Transaction hash " + transactionHash + " isn't well formatted. It should be of length 66");
    }
    if (!transactionHash.startsWith("0x")) {
      throw new IllegalStateException("Transaction hash " + transactionHash + " isn't well formatted. It should starts with 0x");
    }
    return transactionHash;
  }

  public static final void hideWalletOwnerPrivateInformation(Wallet wallet) {
    if (wallet == null) {
      return;
    }
    wallet.setPassPhrase(null);
    wallet.setHasKeyOnServerSide(false);
  }

  public static final <T> T fromJsonString(String value, Class<T> resultClass) throws JsonException {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    JsonDefaultHandler jsonDefaultHandler = new JsonDefaultHandler();
    JSON_PARSER.parse(new ByteArrayInputStream(value.getBytes()), jsonDefaultHandler);
    return ObjectBuilder.createObject(resultClass, jsonDefaultHandler.getJsonObject());
  }

  public static final String toJsonString(Object object) throws JsonException {
    return JSON_GENERATOR.createJsonObject(object).toString();
  }

  public static final BigInteger convertToDecimals(double amount, int decimals) {
    return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(10).pow(decimals)).toBigInteger();
  }

  public static final double convertFromDecimals(BigInteger amount, int decimals) {
    return BigDecimal.valueOf(amount.doubleValue()).divide(BigDecimal.valueOf(10).pow(decimals)).doubleValue();
  }

}
