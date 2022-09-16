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
package org.exoplatform.wallet.service;

import static org.exoplatform.wallet.utils.WalletUtils.ABI_PATH_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.BIN_PATH_PARAMETER;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_CONTEXT;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_SCOPE;
import static org.exoplatform.wallet.utils.WalletUtils.fromJsonString;
import static org.exoplatform.wallet.utils.WalletUtils.toJsonString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.ContractDetail;

public class WalletContractServiceImpl implements WalletContractService, Startable {

  private static final Log     LOG                                    = ExoLogger.getLogger(WalletContractServiceImpl.class);

  private static final String  ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE = "address parameter is mandatory";

  private static final String  SYMBOL_PROPERTY_NAME                   = "exo.wallet.blockchain.token.symbol";

  private static final String  CRYPTOCURRENCY_PROPERTY_NAME           = "exo.wallet.blockchain.network.cryptocurrency";

  private ConfigurationManager configurationManager;

  private String               contractAbiPath;

  private JSONArray            contractAbi;

  private String               contractBinaryPath;

  private String               contractBinary;

  private SettingService       settingService;

  private WalletService        walletService;

  public WalletContractServiceImpl(SettingService settingService, ConfigurationManager configurationManager, InitParams params) {
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
    setCustomTokenSymbol(contractDetail);
    setNetworkCryptoCurrency(contractDetail);
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
        ContractDetail contractDetail= fromJsonString(value, ContractDetail.class);
        setCustomTokenSymbol(contractDetail);
        setNetworkCryptoCurrency(contractDetail);
        return contractDetail;
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
      return IOUtils.toString(abiInputStream, Charset.defaultCharset());
    }
  }

  private WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }

  private void setNetworkCryptoCurrency(ContractDetail contractDetail) {
    String cryptocurrency = PropertyManager.getProperty(CRYPTOCURRENCY_PROPERTY_NAME);

    if (StringUtils.isNotBlank(cryptocurrency)) {
      contractDetail.setCryptocurrency(cryptocurrency);
    }else {
      contractDetail.setCryptocurrency("E");
    }
  }

  private void setCustomTokenSymbol(ContractDetail contractDetail) {
    String symbol = PropertyManager.getProperty(SYMBOL_PROPERTY_NAME);
    if (StringUtils.isNotBlank(symbol)) {
      contractDetail.setSymbol(symbol);
    }else if (StringUtils.isNotBlank(contractDetail.getSymbol())) {
      contractDetail.setSymbol(contractDetail.getSymbol().substring(0,1));
    }
  }

}
