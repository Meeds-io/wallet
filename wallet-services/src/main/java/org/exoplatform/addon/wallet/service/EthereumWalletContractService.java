package org.exoplatform.addon.wallet.service;

import static org.exoplatform.addon.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.picocontainer.Startable;

import org.exoplatform.addon.wallet.model.ContractDetail;
import org.exoplatform.addon.wallet.model.settings.GlobalSettings;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumWalletContractService implements WalletContractService, Startable {

  private static final Log        LOG                                    =
                                      ExoLogger.getLogger(EthereumWalletContractService.class);

  private static final String     ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE = "address parameter is mandatory";

  private ConfigurationManager    configurationManager;

  private String                  contractAbiPath;

  private JSONArray               contractAbi;

  private String                  contractBinaryPath;

  private String                  contractBinary;

  private WalletService           walletService;

  private WalletTokenAdminService walletTokenAdminService;

  private SettingService          settingService;

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
  public boolean isContract(String address) {
    return getContractDetail(address) != null;
  }

  @Override
  public void saveContractDetail(ContractDetail contractDetail) {
    if (contractDetail == null) {
      throw new IllegalArgumentException("contractDetail is mandatory");
    }
    String contractAddress = contractDetail.getAddress();
    if (StringUtils.isBlank(contractAddress)) {
      throw new IllegalArgumentException(ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE);
    }

    String contractDetailString = toJsonString(contractDetail);
    getSettingService().set(WALLET_CONTEXT,
                            WALLET_SCOPE,
                            contractAddress,
                            SettingValue.create(contractDetailString));
  }

  @Override
  public ContractDetail getContractDetail(String address) {
    if (StringUtils.isBlank(address)) {
      return null;
    }

    SettingValue<?> contractDetailValue = getSettingService().get(WALLET_CONTEXT, WALLET_SCOPE, address);
    if (contractDetailValue != null) {
      return fromJsonString((String) contractDetailValue.getValue(), ContractDetail.class);
    }
    return null;
  }

  @Override
  public void refreshContractDetail() {
    GlobalSettings settings = getSettings();
    String contractAddress = settings.getContractAddress();
    ContractDetail contractDetail = getWalletTokenAdminService().loadContractDetailFromBlockchain(contractAddress);
    getWalletService().setConfiguredContractDetail(contractDetail);
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

  private WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }

  private WalletTokenAdminService getWalletTokenAdminService() {
    if (walletTokenAdminService == null) {
      walletTokenAdminService = CommonsUtils.getService(WalletTokenAdminService.class);
    }
    return walletTokenAdminService;
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = CommonsUtils.getService(SettingService.class);
    }
    return settingService;
  }

}
