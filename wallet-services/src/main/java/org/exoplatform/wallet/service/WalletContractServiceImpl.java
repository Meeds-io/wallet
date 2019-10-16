package org.exoplatform.wallet.service;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.settings.GlobalSettings;

public class WalletContractServiceImpl implements WalletContractService, Startable {

  private static final Log        LOG                                    =
                                      ExoLogger.getLogger(WalletContractServiceImpl.class);

  private static final String     ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE = "address parameter is mandatory";

  private ConfigurationManager    configurationManager;

  private String                  contractAbiPath;

  private JSONArray               contractAbi;

  private String                  contractBinaryPath;

  private String                  contractBinary;

  private SettingService          settingService;

  private ListenerService         listenerService;

  private WalletService           walletService;

  private WalletTokenAdminService walletTokenAdminService;

  public WalletContractServiceImpl(SettingService settingService,
                                   ConfigurationManager configurationManager,
                                   InitParams params) {
    this.configurationManager = configurationManager;
    this.settingService = settingService;

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
    settingService.set(WALLET_CONTEXT,
                       WALLET_SCOPE,
                       StringUtils.lowerCase(contractAddress),
                       SettingValue.create(contractDetailString));
    getWalletService().setConfiguredContractDetail(contractDetail);
  }

  @Override
  public ContractDetail getContractDetail(String address) {
    if (StringUtils.isBlank(address)) {
      return null;
    }

    SettingValue<?> contractDetailValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, StringUtils.lowerCase(address));
    if (contractDetailValue != null && contractDetailValue.getValue() != null) {
      String value = contractDetailValue.getValue().toString();
      try {
        return fromJsonString(value, ContractDetail.class);
      } catch (Exception e) {
        LOG.debug("Remove old data stored in settings service for wallet with address '{}', having as value '{}'",
                  address,
                  value,
                  e);
        // Remove value coming from old data
        settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, StringUtils.lowerCase(address));
        return null;
      }
    }
    return null;
  }

  @Override
  public void refreshContractDetail(Set<String> contractModifications) {
    GlobalSettings settings = getSettings();
    String contractAddress = settings.getContractAddress();
    ContractDetail contractDetail = getContractDetail(contractAddress);
    if (contractDetail == null) {
      contractDetail = new ContractDetail();
      contractDetail.setAddress(contractAddress);
    }
    getWalletTokenAdminService().refreshContractDetailFromBlockchain(contractDetail, contractModifications);
    getWalletService().setConfiguredContractDetail(contractDetail);
    try {
      getListenerService().broadcast(CONTRACT_MODIFIED_EVENT, null, contractDetail);
    } catch (Exception e) {
      LOG.error("Error while broadcasting contract modification event", e);
    }
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
                                          .getResourceAsStream("org/exoplatform/wallet/contract/" + name + "."
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

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

}
