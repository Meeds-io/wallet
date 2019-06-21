package org.exoplatform.addon.wallet.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.picocontainer.Startable;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumWalletContractService implements WalletContractService, Startable {

  private static final Log     LOG                                    =
                                   ExoLogger.getLogger(EthereumWalletContractService.class);

  private static final String  ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE =
                                                                      "address parameter is mandatory";

  private ConfigurationManager configurationManager;

  private String               contractAbiPath;

  private JSONArray            contractAbi;

  private String               contractBinaryPath;

  private String               contractBinary;

  private SettingService       settingService;

  public EthereumWalletContractService(ConfigurationManager configurationManager, InitParams params) {
    this.configurationManager = configurationManager;

    if (params.containsKey(ABI_PATH_PARAMETER)) {
      contractAbiPath = params.getValueParam(ABI_PATH_PARAMETER).getValue();
    }
    if (StringUtils.isBlank(contractAbiPath)) {
      LOG.warn("Contract ABI path is empty, thus no contract deployment is possible");
    }
    if (params.containsKey(BIN_PATH_PARAMETER)) {
      contractBinaryPath = params.getValueParam(BIN_PATH_PARAMETER).getValue();
    }
    if (StringUtils.isBlank(contractBinaryPath)) {
      LOG.warn("Contract BIN path is empty, thus no contract deployment is possible");
    }
  }

  @Override
  public void start() {
    try {
      String contractAbiString = IOUtil.getStreamContentAsString(this.configurationManager.getInputStream(contractAbiPath));
      contractAbi = new JSONArray(contractAbiString);
      contractBinary = IOUtil.getStreamContentAsString(this.configurationManager.getInputStream(contractBinaryPath));
      if (!contractBinary.startsWith("0x")) {
        contractBinary = "0x" + contractBinary;
      }
    } catch (Exception e) {
      LOG.error("Can't read ABI/BIN files content", e);
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public boolean isContract(String address, long networkId) {
    return getContractDetail(address, networkId) != null;
  }

  @Override
  public void saveContract(ContractDetail contractDetail) {
    if (StringUtils.isBlank(contractDetail.getAddress())) {
      throw new IllegalArgumentException(ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE);
    }
    if (contractDetail.getNetworkId() == null || contractDetail.getNetworkId() == 0) {
      throw new IllegalArgumentException("networkId parameter is mandatory");
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + contractDetail.getNetworkId();

    String address = contractDetail.getAddress().toLowerCase();

    getSettingService().set(WALLET_CONTEXT,
                            WALLET_SCOPE,
                            address + contractDetail.getNetworkId(),
                            SettingValue.create(contractDetail.toJSONString()));

    if (contractDetail.isDefaultContract()) {
      // Save the contract address in the list of default contract addreses
      SettingValue<?> defaultContractsAddressesValue = getSettingService().get(WALLET_CONTEXT,
                                                                               WALLET_SCOPE,
                                                                               defaultContractsParamKey);
      String defaultContractsAddresses = defaultContractsAddressesValue == null ? address
                                                                                : defaultContractsAddressesValue.getValue()
                                                                                                                .toString()
                                                                                    + "," + address;
      getSettingService().set(WALLET_CONTEXT,
                              WALLET_SCOPE,
                              defaultContractsParamKey,
                              SettingValue.create(defaultContractsAddresses));
    }
  }

  @Override
  public boolean removeDefaultContract(String address, Long networkId) {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Can't remove empty address for contract");
      return false;
    }
    if (networkId == null || networkId == 0) {
      LOG.warn("Can't remove empty network id for contract");
      return false;
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    final String defaultAddressToSave = address.toLowerCase();
    SettingValue<?> defaultContractsAddressesValue = getSettingService().get(WALLET_CONTEXT,
                                                                             WALLET_SCOPE,
                                                                             defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String[] contractAddresses = defaultContractsAddressesValue.getValue().toString().split(",");
      Set<String> contractAddressList = Arrays.stream(contractAddresses)
                                              .filter(contractAddress -> !contractAddress.equalsIgnoreCase(defaultAddressToSave))
                                              .collect(Collectors.toSet());
      String contractAddressValue = StringUtils.join(contractAddressList, ",");

      getSettingService().remove(WALLET_CONTEXT, WALLET_SCOPE, address + networkId);
      getSettingService().set(WALLET_CONTEXT, WALLET_SCOPE, defaultContractsParamKey, SettingValue.create(contractAddressValue));
    }
    return true;
  }

  @Override
  public ContractDetail getContractDetail(String address, Long networkId) {
    if (StringUtils.isBlank(address)) {
      return null;
    }

    Set<String> defaultContracts = getDefaultContractsAddresses(networkId);
    if (defaultContracts != null && !defaultContracts.contains(address)) {
      return null;
    }

    SettingValue<?> contractDetailValue = getSettingService().get(WALLET_CONTEXT, WALLET_SCOPE, address + networkId);
    if (contractDetailValue != null) {
      return ContractDetail.parseStringToObject((String) contractDetailValue.getValue());
    }
    return null;
  }

  @Override
  public Set<String> getDefaultContractsAddresses(Long networkId) {
    if (networkId == null || networkId == 0) {
      return Collections.emptySet();
    }

    String defaultContractsParamKey = WALLET_DEFAULT_CONTRACTS_NAME + networkId;
    SettingValue<?> defaultContractsAddressesValue = getSettingService().get(WALLET_CONTEXT,
                                                                             WALLET_SCOPE,
                                                                             defaultContractsParamKey);
    if (defaultContractsAddressesValue != null) {
      String defaultContractsAddressesString = defaultContractsAddressesValue.getValue().toString().toLowerCase();
      String[] contractAddresses = defaultContractsAddressesString.split(",");
      return Arrays.stream(contractAddresses).map(String::toLowerCase).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  @Override
  public JSONArray getContractAbi() {
    return contractAbi;
  }

  @Override
  public String getContractBinary() {
    return contractBinary;
  }

  @Override
  public String getContractFileContent(String name, String extension) throws IOException {
    try (InputStream abiInputStream = this.getClass()
                                          .getClassLoader()
                                          .getResourceAsStream("org/exoplatform/addon/wallet/contract/" + name + "."
                                              + extension)) {
      return IOUtils.toString(abiInputStream);
    }
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = CommonsUtils.getService(SettingService.class);
    }
    return settingService;
  }
}
