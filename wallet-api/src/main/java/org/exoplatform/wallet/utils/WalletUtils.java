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
package org.exoplatform.wallet.utils;

import static org.exoplatform.wallet.statistic.StatisticUtils.LOCAL_SERVICE;
import static org.exoplatform.wallet.statistic.StatisticUtils.OPERATION;
import static org.exoplatform.wallet.statistic.StatisticUtils.STATUS;
import static org.exoplatform.wallet.statistic.StatisticUtils.STATUS_CODE;
import static org.exoplatform.wallet.statistic.StatisticUtils.transformCapitalWithUnderscore;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletState;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.model.settings.GlobalSettings;
import org.exoplatform.wallet.model.transaction.FundsRequest;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.service.WalletService;
import org.exoplatform.wallet.service.WalletTokenAdminService;
import org.exoplatform.wallet.statistic.StatisticUtils;
import org.exoplatform.ws.frameworks.json.JsonGenerator;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.impl.ObjectBuilder;

/**
 * Utils class to provide common tools and constants
 */
public class WalletUtils {
  private static final Log LOG = ExoLogger.getLogger(WalletUtils.class);

  private WalletUtils() {
  }

  @SuppressWarnings("all")
  public static final char[]                          SIMPLE_CHARS                             = new char[] {
      'A', 'B', 'C', 'D',
      'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c',
      'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1',
      '2', '3', '4', '5', '6', '7', '8', '9'
  };

  public static final String                          COMETD_CHANNEL                           = "/eXo/Application/Addons/Wallet";

  public static final int                             TRANSACTION_EFFECTIVELY_SENT_CODE        = -200;

  public static final int                             ETHER_TO_WEI_DECIMALS                    = 18;

  public static final int                             GWEI_TO_WEI_DECIMALS                     = 9;

  public static final JsonParser                      JSON_PARSER                              = new JsonParserImpl();

  public static final JsonGenerator                   JSON_GENERATOR                           = new JsonGeneratorImpl();

  public static final String                          EMPTY_HASH                               =
                                                                 "0x0000000000000000000000000000000000000000000000000000000000000000";

  public static final String                          NETWORK_ID                               = "networkId";

  public static final String                          NETWORK_URL                              = "networkURL";

  public static final String                          NETWORK_WS_URL                           = "networkWSURL";

  public static final String                          TOKEN_ADDRESS                            = "tokenAddress";

  public static final String                          DEFAULT_INITIAL_USER_FUND                = "defaultInitialFunds";

  public static final String                          DYNAMIC_GAS_PRICE_UPDATE_INTERVAL        = "gasPriceUpdateInterval";

  public static final String                          GAS_LIMIT                                = "gasLimit";

  public static final long                            DEFAULT_MIN_GAS_PRICE                    = 4000000000L;

  public static final long                            DEFAULT_NORMAL_GAS_PRICE                 = 10000000000L;

  public static final long                            DEFAULT_MAX_GAS_PRICE                    = 20000000000L;

  public static final String                          MIN_GAS_PRICE                            = "cheapGasPrice";

  public static final String                          NORMAL_GAS_PRICE                         = "normalGasPrice";

  public static final String                          MAX_GAS_PRICE                            = "fastGasPrice";

  public static final String                          LAST_BLOCK_NUMBER_KEY_NAME               =
                                                                                 "ADDONS_ETHEREUM_LAST_BLOCK_NUMBER";

  public static final String                          SCOPE_NAME                               = "ADDONS_ETHEREUM_WALLET";

  public static final String                          INITIAL_FUNDS_KEY_NAME                   = "INITIAL_FUNDS";

  public static final String                          SETTINGS_KEY_NAME                        =
                                                                        "ADDONS_ETHEREUM_WALLET_SETTINGS";

  public static final Context                         WALLET_CONTEXT                           = Context.GLOBAL;

  public static final Scope                           WALLET_SCOPE                             = Scope.APPLICATION.id(SCOPE_NAME);

  public static final String                          WALLET_USER_TRANSACTION_NAME             = "WALLET_USER_TRANSACTION";

  public static final String                          WALLET_BROWSER_PHRASE_NAME               = "WALLET_BROWSER_PHRASE";

  public static final String                          WALLET_INITIALIZED_SETTING_PARAM         = "WALLET_INITIALIZED";

  public static final String                          ADMIN_KEY_PARAMETER                      = "admin.wallet.key";

  public static final String                          ABI_PATH_PARAMETER                       = "contract.abi.path";

  public static final String                          BIN_PATH_PARAMETER                       = "contract.bin.path";

  public static final String                          ADMINISTRATORS_GROUP                     = "/platform/administrators";

  public static final String                          REWARDINGS_GROUP                         = "/platform/rewarding";

  public static final String                          WALLET_ADMIN_REMOTE_ID                   = "admin";

  public static final String                          PRINCIPAL_CONTRACT_ADMIN_NAME            = "Admin";

  public static final String                          WALLET_MODIFIED_EVENT                    = "exo.wallet.modified";

  public static final String                          CONTRACT_MODIFIED_EVENT                  = "exo.wallet.contract.modified";

  public static final String                          TRANSACTION_MINED_AND_UPDATED_EVENT      =
                                                                                          "exo.wallet.transaction.minedAndUpdated";

  public static final String                          TRANSACTION_SENT_TO_BLOCKCHAIN_EVENT     = "exo.wallet.transaction.sent";

  public static final String                          TRANSACTION_CREATED_EVENT                = "exo.wallet.transaction.created";

  public static final String                          TRANSACTION_MODIFIED_EVENT               =
                                                                                 "exo.wallet.transaction.modified";

  public static final String                          WALLET_ENABLED_EVENT                     = "exo.wallet.enabled";

  public static final String                          WALLET_DISABLED_EVENT                    = "exo.wallet.disabled";

  public static final String                          WALLET_DELETED_EVENT                     = "exo.wallet.deleted";

  public static final String                          WALLET_INITIALIZATION_MODIFICATION_EVENT =
                                                                                               "exo.wallet.initialization.state";

  public static final String                          CONTRACT_FUNC_TRANSFER                   = "transfer";

  public static final String                          CONTRACT_FUNC_TRANSFERFROM               = "transferFrom";

  public static final String                          CONTRACT_FUNC_TRANSFEROWNERSHIP          = "transferOwnership";

  public static final String                          CONTRACT_FUNC_TRANSFORMTOVESTED          = "transformToVested";

  public static final String                          CONTRACT_FUNC_APPROVE                    = "approve";

  public static final String                          CONTRACT_FUNC_INITIALIZEACCOUNT          = "initializeAccount";

  public static final String                          CONTRACT_FUNC_REWARD                     = "reward";

  public static final String                          CONTRACT_FUNC_ADDADMIN                   = "addAdmin";

  public static final String                          NEW_ADDRESS_ASSOCIATED_EVENT             =
                                                                                   "exo.wallet.addressAssociation.new";

  public static final String                          MODIFY_ADDRESS_ASSOCIATED_EVENT          =
                                                                                      "exo.wallet.addressAssociation.modification";

  public static final String                          NEW_BLOCK_MINED_EVENT                    = "exo.wallet.block.mined";

  public static final String                          TRANSACTION_MINED_EVENT                  = "exo.wallet.transaction.mined";

  public static final String                          CONTRACT_TRANSACTION_MINED_EVENT         =
                                                                                       "exo.wallet.transaction.minedForContract";

  public static final String                          KNOWN_TRANSACTION_REPLACED_EVENT         =
                                                                                       "exo.wallet.transaction.replaced";

  public static final String                          TRANSACTION_PENDING_MAX_DAYS             = "transaction.pending.maxDays";

  public static final String                          MAX_PENDING_TRANSACTIONS_TO_SEND         = "transaction.pending.maxToSend";

  public static final String                          MAX_SENDING_TRANSACTIONS_ATTEMPTS        =
                                                                                        "transaction.pending.maxSendingAttempts";

  public static final String                          WALLET_SENDER_NOTIFICATION_ID            = "EtherSenderNotificationPlugin";

  public static final String                          WALLET_RECEIVER_NOTIFICATION_ID          =
                                                                                      "EtherReceiverNotificationPlugin";

  public static final String                          FUNDS_REQUEST_NOTIFICATION_ID            = "FundsRequestNotificationPlugin";

  public static final String                          FUNDS_REQUEST_SENT                       = "sent";

  public static final String                          CONTRACT_ADDRESS                         = "contractAddress";

  public static final String                          AMOUNT                                   = "amount";

  public static final String                          SYMBOL                                   = "symbol";

  public static final String                          TOKEN_NAME                               = "tokenName";

  public static final String                          MESSAGE                                  = "message";

  public static final String                          HASH                                     = "hash";

  public static final String                          ACCOUNT_TYPE                             = "account_type";

  public static final String                          RECEIVER_TYPE                            = "receiver_type";

  public static final String                          AVATAR                                   = "avatar";

  public static final String                          SENDER_ID                                = "senderIdentityId";

  public static final String                          SENDER                                   = "sender";

  public static final String                          USER                                     = "userFullname";

  public static final String                          USER_URL                                 = "userUrl";

  public static final String                          SENDER_URL                               = "senderUrl";

  public static final String                          RECEIVER                                 = "receiver";

  public static final String                          RECEIVER_ID                              = "receiverIdentityId";

  public static final String                          RECEIVER_URL                             = "receiverUrl";

  public static final String                          FUNDS_ACCEPT_URL                         = "fundsAcceptUrl";

  public static final String                          OPERATION_GET_TRANSACTION_COUNT          = "eth_getTransactionCount";

  public static final String                          OPERATION_GET_FILTER_LOGS                = "getFilterLogs";

  public static final String                          OPERATION_NEW_FILTER                     = "eth_newFilter";

  public static final String                          OPERATION_UNINSTALL_FILTER               = "eth_uninstallFilter";

  public static final String                          OPERATION_GET_FILTER_CHANGES             = "eth_getFilterChanges";

  public static final String                          OPERATION_GET_GAS_PRICE                  = "eth_getGasPrice";

  public static final String                          OPERATION_READ_FROM_TOKEN                = "eth_call";

  public static final String                          OPERATION_GET_ETHER_BALANCE              = "eth_getBalance";

  public static final String                          OPERATION_GET_LAST_BLOCK_NUMBER          = "eth_blockNumber";

  public static final String                          OPERATION_GET_TRANSACTION                = "eth_getTransactionByHash";

  public static final String                          OPERATION_GET_BLOCK                      = "eth_getBlockByXXX";

  public static final String                          OPERATION_GET_BLOCK_BY_NUMBER            = "eth_getBlockByNumber";

  public static final String                          OPERATION_GET_BLOCK_BY_HASH              = "eth_getBlockByHash";

  public static final String                          OPERATION_GET_TRANSACTION_RECEIPT        = "eth_getTransactionReceipt";

  public static final String                          OPERATION_SEND_TRANSACTION               = "eth_sendRawTransaction";

  public static final ArgumentLiteral<Wallet>         FUNDS_REQUEST_SENDER_DETAIL_PARAMETER    =
                                                                                            new ArgumentLiteral<>(Wallet.class,
                                                                                                                  "senderFullName");

  public static final ArgumentLiteral<Wallet>         SENDER_ACCOUNT_DETAIL_PARAMETER          =
                                                                                      new ArgumentLiteral<>(Wallet.class,
                                                                                                            "senderAccountDetail");

  public static final ArgumentLiteral<Wallet>         RECEIVER_ACCOUNT_DETAIL_PARAMETER        =
                                                                                        new ArgumentLiteral<>(Wallet.class,
                                                                                                              "receiverAccountDetail");

  public static final ArgumentLiteral<FundsRequest>   FUNDS_REQUEST_PARAMETER                  =
                                                                              new ArgumentLiteral<>(FundsRequest.class,
                                                                                                    "fundsRequest");

  public static final ArgumentLiteral<ContractDetail> CONTRACT_DETAILS_PARAMETER               =
                                                                                 new ArgumentLiteral<>(ContractDetail.class,
                                                                                                       "contractDetails");

  public static final ArgumentLiteral<Double>         AMOUNT_PARAMETER                         =
                                                                       new ArgumentLiteral<>(Double.class, AMOUNT);

  public static final ArgumentLiteral<String>         MESSAGE_PARAMETER                        =
                                                                        new ArgumentLiteral<>(String.class, MESSAGE);

  public static final ArgumentLiteral<String>         HASH_PARAMETER                           =
                                                                     new ArgumentLiteral<>(String.class, HASH);

  public static final ArgumentLiteral<String>         SYMBOL_PARAMETER                         =
                                                                       new ArgumentLiteral<>(String.class, SYMBOL);

  public static final ArgumentLiteral<String>         CONTRACT_ADDRESS_PARAMETER               =
                                                                                 new ArgumentLiteral<>(String.class,
                                                                                                       CONTRACT_ADDRESS);

  public static final String                          RESOURCE_BUNDLE_NAME                     = "locale.addon.Wallet";

  public static final String                          TOKEN_FUNC_SETSELLPRICE                  = "setSellPrice";

  public static final String                          TOKEN_FUNC_INITIALIZEACCOUNT             = "initializeAccount";

  public static final String                          TOKEN_FUNC_DEPOSIT_FUNDS                 = "depositFunds";

  public static final String                          ETHER_FUNC_SEND_FUNDS                    = "ether_transfer";

  public static final String                          LOGIN_MESSAGE_ATTRIBUTE_NAME             = "login_message";

  public static final String                          GAMIFICATION_BROADCAST_ACTION_EVENT      =
                                                                                          "exo.gamification.generic.action";

  public static final String                          GAMIFICATION_EVENT_ID                    = "eventId";

  public static final String                          GAMIFICATION_EARNER_ID                   = "senderId";

  public static final String                          GAMIFICATION_RECEIVER_ID                 = "receiverId";

  public static final String                          GAMIFICATION_OBJECT_ID                   = "objectId";

  public static final String                          GAMIFICATION_OBJECT_TYPE                 = "objectType";

  public static final String                          GAMIFICATION_WALLET_OBJECT_TYPE          = "wallet";

  public static final String                          GAMIFICATION_CREATE_WALLET_EVENT         = "createWallet";

  public static final Random                          Random                                   = new Random();

  public static String                                blockchainUrlSuffix                      = null;                                 // NOSONAR

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }

  public static final long getCurrentUserIdentityId() {
    String username = getCurrentUserId();
    Identity identity = getIdentityByTypeAndId(WalletType.USER, username);
    return Long.parseLong(identity.getId());
  }

  public static List<String> getNotificationReceiversUsers(Wallet wallet, String excludedId) {
    if (WalletType.isSpace(wallet.getType())) {
      Space space = getSpace(wallet.getId());
      if (space == null) {
        return Collections.singletonList(wallet.getId());
      } else {
        String[] managers = space.getManagers();
        if (managers == null || managers.length == 0) {
          return Collections.emptyList();
        } else if (StringUtils.isBlank(excludedId)) {
          return Arrays.asList(managers);
        } else {
          return Arrays.stream(managers).filter(member -> !excludedId.equals(member)).toList();
        }
      }
    } else if (WalletType.isUser(wallet.getType())) {
      return Collections.singletonList(wallet.getId());
    } else if (WalletType.isAdmin(wallet.getType())) {
      try {
        return new ArrayList<>(getRewardAdministrators());
      } catch (Exception e) {
        LOG.error("Error while building notification receivers. Send notification to super administrator only", e);
        UserACL userACL = CommonsUtils.getService(UserACL.class);
        return Collections.singletonList(userACL.getSuperUser());
      }
    } else {
      return Collections.emptyList();
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
      // Not blocker for processing contents
      LOG.warn("Error getting profile link of {} {}", walletType, remoteId, e);
    }
    return StringUtils.isBlank(wallet.getName()) ? wallet.getAddress() : wallet.getName();
  }

  public static Identity getIdentityById(long identityId) {
    return getIdentityById(String.valueOf(identityId));
  }

  public static Identity getIdentityById(String identityId) {
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    return identityManager.getIdentity(identityId);
  }

  public static Identity getIdentityByTypeAndId(WalletType type, String remoteId) {
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    return identityManager.getOrCreateIdentity(type.getProviderId(), remoteId);
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
        wallet.setEnabled(false);
        wallet.setDeletedUser(true);
      } else {
        wallet.setType(type.getId());
        wallet.setId(identity.getRemoteId());
        wallet.setTechnicalId(Long.parseLong(identity.getId()));
      }
    } else {
      // Compute type and remoteId from technicalId
      Identity identity = getIdentityById(wallet.getTechnicalId());
      if (identity == null) {
        wallet.setEnabled(false);
        wallet.setDeletedUser(true);
      } else {
        WalletType type = WalletType.getType(identity.getProviderId());
        wallet.setType(type.getId());
        wallet.setId(identity.getRemoteId());
      }
    }
  }

  public static Set<String> getRewardAdministrators() throws Exception {
    OrganizationService organizationService = CommonsUtils.getService(OrganizationService.class);

    Set<String> adminUsers = new HashSet<>();
    Group rewardingGroup = organizationService.getGroupHandler().findGroupById(REWARDINGS_GROUP);
    if (rewardingGroup != null) {
      ListAccess<Membership> rewardingMembers = organizationService.getMembershipHandler()
                                                                   .findAllMembershipsByGroup(rewardingGroup);
      Membership[] members = rewardingMembers.load(0, rewardingMembers.getSize());
      for (Membership membership : members) {
        adminUsers.add(membership.getUserName());
      }
    }
    return adminUsers;
  }

  public static final boolean isUserRewardingAdmin(String username) {
    return isUserMemberOfGroupOrUser(username, REWARDINGS_GROUP);
  }

  public static final boolean isUserMemberOfGroupOrUser(String username, String permissionExpression) {
    if (StringUtils.isBlank(permissionExpression)) {
      throw new IllegalArgumentException("Permission expression is mandatory");
    }
    if (StringUtils.isBlank(username)) {
      return false;
    }

    org.exoplatform.services.security.Identity identity = CommonsUtils.getService(IdentityRegistry.class).getIdentity(username);
    if (identity == null) {
      try {
        identity = CommonsUtils.getService(Authenticator.class).createIdentity(username);
      } catch (Exception e) {
        LOG.warn("Error getting memberships of user {}", username, e);
      }
    }
    if (identity == null) {
      return false;
    }
    MembershipEntry membership = null;
    if (permissionExpression.contains(":")) {
      String[] permissionExpressionParts = permissionExpression.split(":");
      membership = new MembershipEntry(permissionExpressionParts[1], permissionExpressionParts[0]);
    } else if (permissionExpression.contains("/")) {
      membership = new MembershipEntry(permissionExpression, MembershipEntry.ANY_TYPE);
    } else {
      return StringUtils.equals(username, permissionExpression);
    }
    return identity.isMemberOf(membership);
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
            + "/SpaceWallet";
      }
    }
  }

  public static String getMyWalletLink() {
    UserPortalConfigService userPortalConfigService = CommonsUtils.getService(UserPortalConfigService.class);
    return "/" + PortalContainer.getInstance().getName() + "/" + userPortalConfigService.getMetaPortal() + "/wallet";
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
                                          .append(StringEscapeUtils.escapeHtml4(space.getDisplayName()))
                                          .append("</a>")
                                          .toString();
  }

  public static String encodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLEncoder.encode(content.trim(), "UTF-8");
    } catch (Exception e) {
      throw new IllegalStateException("Error encoding content", e);
    }
  }

  public static String decodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLDecoder.decode(content.trim(), "UTF-8");
    } catch (Exception e) {
      throw new IllegalStateException("Error decoding content", e);
    }
  }

  public static boolean isUserSpaceManager(String id, String modifier) {
    try {
      return checkUserIsSpaceManager(id, modifier, false);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Unexpected exception thrown");
    }
  }

  /**
   * Return true if user can access wallet detailed information
   * 
   * @param  wallet      wallet details to check
   * @param  currentUser user accessing wallet details
   * @return             true if has access, else false
   */
  public static boolean canAccessWallet(Wallet wallet, String currentUser) {
    if (StringUtils.isBlank(currentUser)) {
      return false;
    }
    String remoteId = wallet.getId();
    WalletType type = WalletType.getType(wallet.getType());
    boolean isUserAdmin = isUserRewardingAdmin(currentUser);
    // 'rewarding' group members can access to all wallets
    if (isUserAdmin) {
      return true;
    }

    // For 'Admin' wallet, only 'rewarding' group members can access it
    return (type.isUser() && StringUtils.equals(currentUser, remoteId))
        || (type.isSpace() && isUserSpaceMember(wallet.getId(), currentUser));
  }

  public static boolean isUserSpaceMember(String spaceId, String accesssor) {
    Space space = getSpace(spaceId);
    if (space == null) {
      throw new IllegalStateException("Space not found with id '" + spaceId + "'");
    }
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    return spaceService.canViewSpace(space, accesssor);
  }

  public static boolean checkUserIsSpaceManager(String spaceId,
                                                String modifier,
                                                boolean throwException) throws IllegalAccessException {
    Space space = getSpace(spaceId);
    if (space == null) {
      throw new IllegalStateException("Space not found with id '" + spaceId + "'");
    }
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    if (!spaceService.canManageSpace(space, modifier)) {
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
    if (identity == null) {
      wallet.setDeletedUser(true);
      wallet.setEnabled(false);
      return;
    }
    WalletType walletType = WalletType.getType(identity.getProviderId());
    wallet.setId(identity.getRemoteId());
    wallet.setTechnicalId(Long.parseLong(identity.getId()));
    wallet.setDisabledUser(!identity.isEnable());
    wallet.setDeletedUser(identity.isDeleted());
    wallet.setType(walletType.getId());
    wallet.setIsInitialized(Arrays.asList(WalletState.INITIALIZED.name(), WalletState.MODIFIED.name())
                                  .contains(wallet.getInitializationState()));
    if (walletType.isUser() || walletType.isSpace()) {
      wallet.setAvatar(identity.getProfile().getAvatarUrl());
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
      if (space != null) {
        wallet.setName(space.getDisplayName());
        wallet.setSpaceId(Long.parseLong(space.getId()));
      }
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
  }

  public static final <T> T fromJsonString(String value, Class<T> resultClass) {
    try {
      if (StringUtils.isBlank(value)) {
        return null;
      }
      JsonDefaultHandler jsonDefaultHandler = new JsonDefaultHandler();
      JSON_PARSER.parse(new ByteArrayInputStream(value.getBytes()), jsonDefaultHandler);
      return ObjectBuilder.createObject(resultClass, jsonDefaultHandler.getJsonObject());
    } catch (JsonException e) {
      throw new IllegalStateException("Error creating object from string : " + value, e);
    }
  }

  public static final String toJsonString(Object object) {
    try {
      return JSON_GENERATOR.createJsonObject(object).toString();
    } catch (JsonException e) {
      throw new IllegalStateException("Error parsing object to string " + object, e);
    }
  }

  public static final BigInteger convertToDecimals(double amount, int decimals) {
    return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(10).pow(decimals)).toBigInteger();
  }

  public static final double convertFromDecimals(BigInteger amount, int decimals) {
    return amount == null ? 0 :
                          BigDecimal.valueOf(amount.doubleValue())
                                    .divide(BigDecimal.valueOf(10).pow(decimals), MathContext.DECIMAL128)
                                    .doubleValue();
  }

  public static final GlobalSettings getSettings() {
    return getWalletService().getSettings();
  }

  public static final Long getGasLimit() {
    return getSettings().getNetwork().getGasLimit();
  }

  public static final ContractDetail getContractDetail() {
    GlobalSettings settings = getSettings();
    if (settings == null || settings.getContractDetail() == null) {
      throw new IllegalStateException("No principal contract address is configured");
    }
    return settings.getContractDetail();
  }

  public static final String getContractAddress() {
    GlobalSettings settings = getSettings();
    if (settings == null || StringUtils.isBlank(settings.getContractAddress())) {
      throw new IllegalStateException("No principal contract address is configured");
    }
    return settings.getContractAddress();
  }

  public static final long getNetworkId() {
    GlobalSettings settings = getSettings();
    return settings == null || settings.getNetwork() == null ? 0 : settings.getNetwork().getId();
  }

  public static final String getWebsocketURL() {
    GlobalSettings settings = getSettings();
    return settings == null || settings.getNetwork() == null ? null : settings.getNetwork().getWebsocketProviderURL();
  }

  public static final String formatNumber(Object amount, String lang) {
    if (StringUtils.isBlank(lang)) {
      lang = Locale.getDefault().getLanguage();
    }
    NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale(lang));
    numberFormat.setMaximumFractionDigits(2);
    return numberFormat.format(Double.parseDouble(amount.toString()));
  }

  public static final boolean isWalletEmpty(Wallet wallet) {
    return wallet == null || StringUtils.isBlank(wallet.getAddress());
  }

  public static final boolean isAdminAccount(String address) {
    if (getWalletTokenAdminService() == null) {
      return false;
    }
    String adminWalletAddress = getWalletTokenAdminService().getAdminWalletAddress();
    return StringUtils.equalsIgnoreCase(address, adminWalletAddress);
  }

  public static final String getResourceBundleKey(Locale locale, String key) {
    ResourceBundle resourceBundle = CommonsUtils.getService(ResourceBundleService.class)
                                                .getResourceBundle(RESOURCE_BUNDLE_NAME, locale);
    String label = resourceBundle == null ? null : resourceBundle.getString(key);
    if (StringUtils.isBlank(label)) {
      resourceBundle = CommonsUtils.getService(ResourceBundleService.class)
                                   .getResourceBundle(RESOURCE_BUNDLE_NAME, Locale.getDefault());
      label = resourceBundle == null ? null : resourceBundle.getString(key);
    }
    return label;
  }

  public static Locale getLocale(Wallet wallet) {
    Locale locale = null;
    if (WalletType.isUser(wallet.getType())) {
      locale = getUserLocale(wallet.getId());
    } else {
      locale = Locale.getDefault();
    }
    return locale;
  }

  public static final Locale getUserLocale(String username) {
    if (StringUtils.isNotBlank(username)) {
      OrganizationService organizationService = CommonsUtils.getService(OrganizationService.class);
      UserProfile profile = null;
      try {
        profile = organizationService.getUserProfileHandler().findUserProfileByName(username);
      } catch (Exception e) {
        LOG.warn("Error getting profile of user {}", username, e);
      }
      if (profile != null) {
        String lang = profile.getAttribute(Constants.USER_LANGUAGE);
        if (StringUtils.isNotBlank(lang)) {
          return LocaleUtils.toLocale(lang);
        }
      }
    }
    return Locale.getDefault();
  }

  public static final String getBlockchainURLSuffix() {
    if (blockchainUrlSuffix == null) {
      GlobalSettings settings = getSettings();
      if (settings != null && settings.getNetwork() != null
          && StringUtils.isNotBlank(settings.getNetwork().getWebsocketProviderURL())) {
        String websocketProviderURL = settings.getNetwork().getWebsocketProviderURL();
        String[] urlParts = websocketProviderURL.split("/");
        blockchainUrlSuffix = urlParts[urlParts.length - 1];
      }
    }
    return blockchainUrlSuffix;
  }

  public static final void logStatistics(TransactionDetail transactionDetail) {
    if (transactionDetail == null) {
      return;
    }
    String contractMethodName = transactionDetail.getContractMethodName();

    if (StringUtils.isBlank(contractMethodName)) {
      contractMethodName = ETHER_FUNC_SEND_FUNDS;
    }

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(LOCAL_SERVICE, "wallet");
    parameters.put(OPERATION, transformCapitalWithUnderscore(contractMethodName));
    parameters.put("blockchain_network_id", getNetworkId());
    parameters.put("blockchain_network_url_suffix", getBlockchainURLSuffix());

    if (transactionDetail.getIssuerId() > 0 || transactionDetail.getIssuer() != null) {
      parameters.put("user_social_id",
                     transactionDetail.getIssuerId() > 0 ? transactionDetail.getIssuerId()
                                                         : transactionDetail.getIssuer().getTechnicalId());
    }

    if (transactionDetail.getFromWallet() != null) {
      parameters.put(SENDER, transactionDetail.getFromWallet());
    } else if (transactionDetail.getFrom() != null) {
      parameters.put("sender_wallet_address", transactionDetail.getFrom());
    }

    if (transactionDetail.getToWallet() != null) {
      parameters.put(RECEIVER, transactionDetail.getToWallet());
    } else if (transactionDetail.getTo() != null) {
      parameters.put("receiver_wallet_address", transactionDetail.getTo());
    }

    if (transactionDetail.getByWallet() != null) {
      parameters.put("by", transactionDetail.getByWallet());
    } else if (transactionDetail.getBy() != null) {
      parameters.put("by_wallet_address", transactionDetail.getBy());
    }

    switch (contractMethodName) {
    case CONTRACT_FUNC_INITIALIZEACCOUNT:
      parameters.put("amount_ether", transactionDetail.getValue());
      parameters.put("amount_token", transactionDetail.getContractAmount());
      break;
    case ETHER_FUNC_SEND_FUNDS:
      parameters.put("amount_ether", transactionDetail.getValue());
      break;
    case CONTRACT_FUNC_TRANSFORMTOVESTED, CONTRACT_FUNC_TRANSFERFROM, CONTRACT_FUNC_TRANSFER, CONTRACT_FUNC_APPROVE, CONTRACT_FUNC_REWARD:
      parameters.put("amount_token", transactionDetail.getContractAmount());
      break;
    case CONTRACT_FUNC_ADDADMIN:
      parameters.put("admin_level", transactionDetail.getValue());
      break;
    default:
      break;
    }
    parameters.put("token_fee", transactionDetail.getTokenFee());
    parameters.put("ether_fee", transactionDetail.getEtherFee());
    parameters.put("gas_price",
                   convertFromDecimals(BigInteger.valueOf((long) transactionDetail.getGasPrice()), GWEI_TO_WEI_DECIMALS));
    parameters.put("gas_used", transactionDetail.getGasUsed());
    parameters.put("transaction", transactionDetail.getHash());
    parameters.put(STATUS, transactionDetail.isSucceeded() ? "ok" : "ko");
    parameters.put(STATUS_CODE, transactionDetail.isSucceeded() ? "200" : "500");
    StatisticUtils.addStatisticEntry(parameters);
  }

  public static final WalletTokenAdminService getWalletTokenAdminService() {
    return CommonsUtils.getService(WalletTokenAdminService.class);
  }

  public static final WalletService getWalletService() {
    return CommonsUtils.getService(WalletService.class);
  }

  public static final SpaceService getSpaceService() {
    return CommonsUtils.getService(SpaceService.class);
  }

  /**
   * Format Wallet Balance amount in currency format, without currency symbol
   * and switch user locale.
   * 
   * @param  balance    amount to format
   * @param  locale     designated locale to display balance
   * @param  simplified if true, the fractions will be ignored when the balance
   *                      is greater than 100.
   * @return            formatted balance in user locale
   */
  public static final String formatBalance(double balance, Locale locale, boolean simplified) {
    // Avoid to display fractions when the amount of balance is big
    if (simplified && balance > 100) {
      balance = Math.floor(balance);
    }
    NumberFormat decimalFormat = NumberFormat.getNumberInstance(locale);
    decimalFormat.setMaximumFractionDigits(2);
    decimalFormat.setMinimumFractionDigits(0);
    decimalFormat.setRoundingMode(RoundingMode.FLOOR);
    return decimalFormat.format(balance).trim();
  }

  public static final ListenerService getListenerService() {
    return CommonsUtils.getService(ListenerService.class);
  }

  public static void broadcastTransactionReplacedEvent(TransactionDetail oldTransaction, TransactionDetail newTransaction) {
    String oldHash = "";
    String newHash = "";
    try {
      if (oldTransaction == null || newTransaction == null || oldTransaction.isPending() || !(newTransaction.isPending())) {
        return;
      }
      oldHash = oldTransaction.getHash();
      newHash = newTransaction.getHash();
      if (oldTransaction.getContractAmount() != newTransaction.getContractAmount()) {
        LOG.info("Transaction {} had replaced {} with the same nonce but they don't have same amount: {} != {}",
                 oldHash,
                 newHash,
                 oldTransaction.getContractAmount(),
                 newTransaction.getContractAmount());
        return;
      }
      if (!StringUtils.equalsIgnoreCase(oldTransaction.getFrom(), newTransaction.getFrom())) {
        throw new IllegalStateException("Issuer of transaction replacement must be the same wallet address: "
            + oldTransaction.getFrom() + " != " + newTransaction.getFrom());
      }
      if (!StringUtils.equalsIgnoreCase(oldTransaction.getTo(), newTransaction.getTo())) {
        LOG.info("Transaction {} had replaced {} with the same nonce but they don't have same target wallet: {} != {}",
                 oldHash,
                 newHash,
                 oldTransaction.getTo(),
                 newTransaction.getTo());
        return;
      }
      if (!StringUtils.equalsIgnoreCase(oldTransaction.getContractAddress(), newTransaction.getContractAddress())) {
        LOG.info("Transaction {} had replaced {} with the same nonce but they don't have same target contract: {} != {}",
                 oldHash,
                 newHash,
                 oldTransaction.getContractAddress(),
                 newTransaction.getContractAddress());
        return;
      }
      if (!StringUtils.equalsIgnoreCase(oldTransaction.getContractMethodName(), newTransaction.getContractMethodName())) {
        LOG.info("Transaction {} had replaced {} with the same nonce but they don't have same target contract method: {} != {}",
                 oldHash,
                 newHash,
                 oldTransaction.getContractMethodName(),
                 newTransaction.getContractMethodName());
        return;
      }
      Map<String, Object> transaction = transactionToMap(newTransaction);
      transaction.put("oldHash", oldHash);
      getListenerService().broadcast(KNOWN_TRANSACTION_REPLACED_EVENT, null, transaction);
    } catch (Exception e) {
      LOG.warn("Error while broadcasting transaction replaced event: from {} to {}", oldHash, newHash, e);
    }
  }

  public static Map<String, Object> transactionToMap(TransactionDetail transactionDetail) {
    Map<String, Object> transaction = new HashMap<>();
    transaction.put("hash", transactionDetail.getHash());
    transaction.put("from", transactionDetail.getFromWallet() == null ? 0 : transactionDetail.getFromWallet().getTechnicalId());
    transaction.put("to", transactionDetail.getToWallet() == null ? 0 : transactionDetail.getToWallet().getTechnicalId());
    transaction.put(CONTRACT_ADDRESS, transactionDetail.getContractAddress());
    transaction.put("contractAmount", transactionDetail.getContractAmount());
    transaction.put("contractMethodName", transactionDetail.getContractMethodName());
    transaction.put("etherAmount", transactionDetail.getValue());
    transaction.put("status", transactionDetail.isSucceeded());
    transaction.put("issuerId", transactionDetail.getIssuerId());
    return transaction;
  }

  public static String generateToken(HttpSession session) {
    String token = getToken(session);
    if (token == null) {
      token = Random.nextLong() + "-" + Random.nextLong() + "-" + Random.nextLong();
      session.setAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME, token); // NOSONAR
    }
    return token;
  }

  public static String getToken(HttpSession session) {
    return session == null ? null : (String) session.getAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME);
  }

}
