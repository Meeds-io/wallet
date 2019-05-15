package org.exoplatform.addon.wallet.model;

import static org.exoplatform.addon.wallet.utils.WalletUtils.jsonArrayToList;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.json.*;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GlobalSettings implements Serializable, Cloneable {

  private static final String USER_WALLET_PARAM                      = "wallet";

  private static final long   serialVersionUID                       = -4672745644323864680L;

  private static final String CONTRACT_BIN_PARAM                     = "contractBin";

  private static final String CONTRACT_ABI_PARAM                     = "contractAbi";

  private static final String DEFAULT_CONTRACTS_TO_DISPLAY_PARAM     = "defaultContractsToDisplay";

  private static final String PRINCIPAL_CONTRACT_ADMIN_ADDRESS_PARAM = "principalContractAdminAddress";

  private static final String PRINCIPAL_CONTRACT_ADMIN_NAME_PARAM    = "principalContractAdminName";

  private static final String USER_PREFERENCES_PARAM                 = "userPreferences";

  private static final String USER_HAS_SERVER_KEY_PARAM              = "hasKeyOnServerSide";

  private static final String IS_WALLET_ENABLED_PARAM                = "isWalletEnabled";

  private static final String IS_ADMIN_PARAM                         = "isAdmin";

  private static final String DEFAULT_OVERVIEW_ACCOUNTS_PARAM        = "defaultOverviewAccounts";

  private static final String DEFAULT_PRINCIPAL_ACCOUNT_PARAM        = "defaultPrincipalAccount";

  private static final String INITIAL_FUNDS_PARAM                    = "initialFunds";

  private static final String DATA_VERSION_PARAM                     = "dataVersion";

  private static final String MAX_GAS_PRICE_PARAM                    = "maxGasPrice";

  private static final String NORMAL_GAS_PRICE_PARAM                 = "normalGasPrice";

  private static final String MIN_GAS_PRICE_PARAM                    = "minGasPrice";

  private static final String DEFAULT_GAS_PARAM                      = "defaultGas";

  private static final String DEFAULT_NETWORK_ID_PARAM               = "defaultNetworkId";

  private static final String WEBSOCKET_PROVIDER_URL_PARAM           = "websocketProviderURL";

  private static final String PROVIDER_URL_PARAM                     = "providerURL";

  private static final String FUNDS_HOLDER_TYPE_PARAM                = "fundsHolderType";

  private static final String FUNDS_HOLDER_PARAM                     = "fundsHolder";

  private static final String INITIAL_FUNDS_REQUEST_MESSAGE_PARAM    = "initialFundsRequestMessage";

  private static final String ACCESS_PERMISSION_PARAM                = "accessPermission";

  private static final String ENABLE_DELEGATION_PARAM                = "enableDelegation";

  private Integer             dataVersion                            = 0;

  private boolean             enableDelegation                       = false;

  private String              accessPermission                       = null;

  private String              fundsHolder                            = null;

  private String              initialFundsRequestMessage             = null;

  private String              fundsHolderType                        = null;

  private String              providerURL                            = null;

  private String              websocketProviderURL                   = null;

  private Long                defaultNetworkId                       = 0L;

  private Long                defaultGas                             = 0L;

  private Long                minGasPrice                            = 4000000000L;

  private Long                normalGasPrice                         = 8000000000L;

  private Long                maxGasPrice                            = 15000000000L;

  private String              principalContractAdminName             = "Admin";

  private String              principalContractAdminAddress          = null;

  private String              defaultPrincipalAccount                = null;

  private Set<String>         defaultOverviewAccounts;

  private Map<String, Double> initialFunds;

  // Computed
  private boolean             walletEnabled                          = true;

  // Computed
  private boolean             isAdmin                                = false;

  // Computed: managed in other storage location
  private WalletPreferences   userPreferences;

  // Computed: managed in other storage location
  private Set<String>         defaultContractsToDisplay;

  // Computed: managed in file system storage
  private transient JSONArray contractAbi                            = null;

  // Computed: managed in file system storage
  private String              contractBin                            = null;

  public String toJSONString(boolean includeTransient) {
    return toJSONObject(includeTransient).toString();
  }

  public JSONObject toJSONObject(boolean includeTransient) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put(ENABLE_DELEGATION_PARAM, enableDelegation);
      if (StringUtils.isNotBlank(accessPermission)) {
        jsonObject.put(ACCESS_PERMISSION_PARAM, accessPermission);
      }
      if (StringUtils.isNotBlank(initialFundsRequestMessage)) {
        jsonObject.put(INITIAL_FUNDS_REQUEST_MESSAGE_PARAM, initialFundsRequestMessage);
      }
      if (StringUtils.isNotBlank(fundsHolder)) {
        jsonObject.put(FUNDS_HOLDER_PARAM, fundsHolder);
      }
      if (StringUtils.isNotBlank(fundsHolderType)) {
        jsonObject.put(FUNDS_HOLDER_TYPE_PARAM, fundsHolderType);
      }
      if (StringUtils.isNotBlank(providerURL)) {
        jsonObject.put(PROVIDER_URL_PARAM, providerURL);
      }
      if (StringUtils.isNotBlank(websocketProviderURL)) {
        jsonObject.put(WEBSOCKET_PROVIDER_URL_PARAM, websocketProviderURL);
      }
      if (defaultNetworkId != null && defaultNetworkId != 0) {
        jsonObject.put(DEFAULT_NETWORK_ID_PARAM, defaultNetworkId);
      }
      if (defaultGas != null && defaultGas != 0) {
        jsonObject.put(DEFAULT_GAS_PARAM, defaultGas);
      }
      if (minGasPrice != null && minGasPrice != 0) {
        jsonObject.put(MIN_GAS_PRICE_PARAM, minGasPrice);
      }
      if (normalGasPrice != null && normalGasPrice != 0) {
        jsonObject.put(NORMAL_GAS_PRICE_PARAM, normalGasPrice);
      }
      if (maxGasPrice != null && maxGasPrice != 0) {
        jsonObject.put(MAX_GAS_PRICE_PARAM, maxGasPrice);
      }
      if (dataVersion != null && dataVersion != 0) {
        jsonObject.put(DATA_VERSION_PARAM, dataVersion);
      }
      if (initialFunds != null && !initialFunds.isEmpty()) {
        JSONArray array = new JSONArray();
        Set<String> addresses = initialFunds.keySet();
        for (String address : addresses) {
          JSONObject obj = new JSONObject();
          obj.put("address", address);
          obj.put("amount", initialFunds.get(address));
          array.put(obj);
        }
        jsonObject.put(INITIAL_FUNDS_PARAM, array);
      }
      if (StringUtils.isNotBlank(defaultPrincipalAccount)) {
        jsonObject.put(DEFAULT_PRINCIPAL_ACCOUNT_PARAM, defaultPrincipalAccount);
      }
      if (defaultOverviewAccounts != null && !defaultOverviewAccounts.isEmpty()) {
        jsonObject.put(DEFAULT_OVERVIEW_ACCOUNTS_PARAM, new JSONArray(defaultOverviewAccounts));
      }
      if (includeTransient) {
        jsonObject.put(IS_ADMIN_PARAM, isAdmin);
        jsonObject.put(IS_WALLET_ENABLED_PARAM, walletEnabled);
        if (userPreferences != null) {
          JSONObject userPrefsObject = userPreferences.toJSONObject();
          if (userPreferences.getWallet() != null) {
            userPrefsObject.put(USER_WALLET_PARAM, new JSONObject(userPreferences.getWallet()));
            userPrefsObject.put(USER_HAS_SERVER_KEY_PARAM, userPreferences.isHasKeyOnServerSide());
          }
          jsonObject.put(USER_PREFERENCES_PARAM, userPrefsObject);
        }
        if (StringUtils.isNotBlank(principalContractAdminName)) {
          jsonObject.put(PRINCIPAL_CONTRACT_ADMIN_NAME_PARAM, principalContractAdminName);
        }
        if (StringUtils.isNotBlank(principalContractAdminAddress)) {
          jsonObject.put(PRINCIPAL_CONTRACT_ADMIN_ADDRESS_PARAM, principalContractAdminAddress);
        }
        if (defaultContractsToDisplay != null && !defaultContractsToDisplay.isEmpty()) {
          jsonObject.put(DEFAULT_CONTRACTS_TO_DISPLAY_PARAM, new JSONArray(defaultContractsToDisplay));
        }
        if (contractAbi != null && contractAbi.length() > 0) {
          jsonObject.put(CONTRACT_ABI_PARAM, contractAbi);
        }
        if (StringUtils.isNotBlank(contractBin)) {
          jsonObject.put(CONTRACT_BIN_PARAM, contractBin);
        }
      }
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting Object to JSON", e);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    return toJSONString(false);
  }

  public static final GlobalSettings parseStringToObject(GlobalSettings defaultSettings, String jsonString) {
    if (defaultSettings == null) {
      defaultSettings = new GlobalSettings();
    }
    if (StringUtils.isBlank(jsonString)) {
      return defaultSettings.clone();
    }
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      GlobalSettings globalSettings = new GlobalSettings();

      String storedFundsHolder = jsonObject.has(FUNDS_HOLDER_PARAM) ? jsonObject.getString(FUNDS_HOLDER_PARAM)
                                                                    : defaultSettings.getFundsHolder();
      globalSettings.setFundsHolder(storedFundsHolder);

      String storedFundsHolderType = jsonObject.has(FUNDS_HOLDER_TYPE_PARAM) ? jsonObject.getString(FUNDS_HOLDER_TYPE_PARAM)
                                                                             : defaultSettings.getFundsHolderType();
      globalSettings.setFundsHolderType(storedFundsHolderType);

      Map<String, Double> storedInitialFunds =
                                             jsonObject.has(INITIAL_FUNDS_PARAM) ? toMap(jsonObject.getJSONArray(INITIAL_FUNDS_PARAM))
                                                                                 : defaultSettings.getInitialFunds();
      globalSettings.setInitialFunds(storedInitialFunds);

      String storedAccessPermission = jsonObject.has(ACCESS_PERMISSION_PARAM) ? jsonObject.getString(ACCESS_PERMISSION_PARAM)
                                                                              : defaultSettings.getAccessPermission();
      globalSettings.setAccessPermission(storedAccessPermission);

      String storedInitialfundsRequestMessage =
                                              jsonObject.has(INITIAL_FUNDS_REQUEST_MESSAGE_PARAM) ? jsonObject.getString(INITIAL_FUNDS_REQUEST_MESSAGE_PARAM)
                                                                                                  : defaultSettings.getInitialFundsRequestMessage();
      globalSettings.setInitialFundsRequestMessage(storedInitialfundsRequestMessage);

      String storedProviderURL = jsonObject.has(PROVIDER_URL_PARAM) ? jsonObject.getString(PROVIDER_URL_PARAM)
                                                                    : defaultSettings.getProviderURL();
      globalSettings.setProviderURL(storedProviderURL);

      String storedWebsocketProviderURL =
                                        jsonObject.has(WEBSOCKET_PROVIDER_URL_PARAM) ? jsonObject.getString(WEBSOCKET_PROVIDER_URL_PARAM)
                                                                                     : defaultSettings.getWebsocketProviderURL();
      globalSettings.setWebsocketProviderURL(storedWebsocketProviderURL);

      long storedDefaultNetworkId = jsonObject.has(DEFAULT_NETWORK_ID_PARAM) ? jsonObject.getLong(DEFAULT_NETWORK_ID_PARAM)
                                                                             : defaultSettings.getDefaultNetworkId();
      globalSettings.setDefaultNetworkId(storedDefaultNetworkId);

      long storedDefaultGas = jsonObject.has(DEFAULT_GAS_PARAM) ? jsonObject.getLong(DEFAULT_GAS_PARAM)
                                                                : defaultSettings.getDefaultGas();
      globalSettings.setDefaultGas(storedDefaultGas);

      long storedMinGasPrice = jsonObject.has(MIN_GAS_PRICE_PARAM) ? jsonObject.getLong(MIN_GAS_PRICE_PARAM)
                                                                   : defaultSettings.getMinGasPrice();
      globalSettings.setMinGasPrice(storedMinGasPrice);

      long storedNormalGasPrice = jsonObject.has(NORMAL_GAS_PRICE_PARAM) ? jsonObject.getLong(NORMAL_GAS_PRICE_PARAM)
                                                                         : defaultSettings.getNormalGasPrice();
      globalSettings.setNormalGasPrice(storedNormalGasPrice);

      long storedMaxGasPrice = jsonObject.has(MAX_GAS_PRICE_PARAM) ? jsonObject.getLong(MAX_GAS_PRICE_PARAM)
                                                                   : defaultSettings.getMaxGasPrice();
      globalSettings.setMaxGasPrice(storedMaxGasPrice);

      boolean storedEnableDelegation = jsonObject.has(ENABLE_DELEGATION_PARAM) ? jsonObject.getBoolean(ENABLE_DELEGATION_PARAM)
                                                                               : defaultSettings.isEnableDelegation();
      globalSettings.setEnableDelegation(storedEnableDelegation);

      String storedDefaultPrincipalAccount =
                                           jsonObject.has(DEFAULT_PRINCIPAL_ACCOUNT_PARAM) ? jsonObject.getString(DEFAULT_PRINCIPAL_ACCOUNT_PARAM)
                                                                                           : defaultSettings.getDefaultPrincipalAccount();
      globalSettings.setDefaultPrincipalAccount(storedDefaultPrincipalAccount);

      globalSettings.setDefaultOverviewAccounts(jsonArrayToList(jsonObject, DEFAULT_OVERVIEW_ACCOUNTS_PARAM));

      globalSettings.setDataVersion(jsonObject.has(DATA_VERSION_PARAM) ? jsonObject.getInt(DATA_VERSION_PARAM) : 0);
      return globalSettings;
    } catch (JSONException e) {
      throw new IllegalStateException("Error while converting JSON String to Object", e);
    }
  }

  public static final GlobalSettings parseStringToObject(String jsonString) {
    return parseStringToObject(null, jsonString);
  }

  private static Map<String, Double> toMap(JSONArray storedInitialFunds) throws JSONException {
    HashMap<String, Double> map = new HashMap<>();
    if (storedInitialFunds == null || storedInitialFunds.length() == 0) {
      return map;
    }
    for (int i = 0; i < storedInitialFunds.length(); i++) {
      JSONObject obj = storedInitialFunds.getJSONObject(i);
      map.put(obj.getString("address"), obj.getDouble("amount"));
    }
    return map;
  }

  @Override
  @SuppressWarnings("all")
  public GlobalSettings clone() {
    try {
      GlobalSettings clonedSettings = (GlobalSettings) super.clone();
      clonedSettings.setUserPreferences(userPreferences);
      clonedSettings.setAdmin(false);
      clonedSettings.setWalletEnabled(false);
      return clonedSettings;
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Can't clone settings");
    }
  }
}
