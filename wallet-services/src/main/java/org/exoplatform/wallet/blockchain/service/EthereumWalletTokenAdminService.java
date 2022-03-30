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
package org.exoplatform.wallet.blockchain.service;

import static org.exoplatform.wallet.utils.WalletUtils.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.picocontainer.Startable;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.wallet.contract.MeedsToken;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.model.*;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.settings.GlobalSettings;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.statistic.ExoWalletStatistic;
import org.exoplatform.wallet.statistic.ExoWalletStatisticService;
import org.exoplatform.wallet.storage.WalletStorage;
import org.exoplatform.wallet.service.*;


public class EthereumWalletTokenAdminService implements WalletTokenAdminService, Startable, ExoWalletStatisticService {
  private static final Log         LOG                                     =
                                       ExoLogger.getLogger(EthereumWalletTokenAdminService.class);

  private static final String      NO_CONFIGURED_CONTRACT_ADDRESS          = "No configured contract address";

  private static final String      TRANSACTION_DETAIL_IS_MANDATORY         = "Transaction detail is mandatory";

  private static final String      RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY =
                                                                           "receiver address parameter is mandatory";

  private UserACL                  userACL;

  private WalletService            walletService;

  private WalletContractService    walletContractService;

  private EthereumClientConnector  clientConnector;

  private WalletAccountService     accountService;

  private WalletStorage            accountStorage;

  private WalletTransactionService transactionService;

  private MeedsToken               ertInstance;

  private long                     networkId                               = 0;

  private String                   websocketURL                            = null;

  private String                   websocketURLSuffix                      = null;

  private String                   configuredContractAddress;

  private Integer                  configuredContractDecimals;

  private String             adminPrivateKey;

  public EthereumWalletTokenAdminService(EthereumClientConnector clientConnector) {
    this.clientConnector = clientConnector;
  }

  @Override
  public void start() {
    try {
      GlobalSettings settings = getSettings();
      if (settings == null) {
        LOG.warn("No wallet settings are found");
        return;
      }

      if (settings.getNetwork() == null || settings.getNetwork().getId() <= 0
              || StringUtils.isBlank(settings.getNetwork().getProviderURL())
              || StringUtils.isBlank(settings.getNetwork().getWebsocketProviderURL())) {
        LOG.warn("No valid blockchain network settings are found: {}", settings.getNetwork());
        return;
      }

      if (StringUtils.isBlank(settings.getContractAddress())) {
        LOG.warn("No contract address is configured");
        return;
      }
      this.adminPrivateKey = System.getProperty("exo.wallet.admin.privateKey", null);
      this.configuredContractAddress = getContractAddress();
      this.websocketURL = getWebsocketURL();
      this.networkId = getNetworkId();

      ContractDetail contractDetail = getContractService().getContractDetail(configuredContractAddress);
      if (contractDetail == null) {
        contractDetail = new ContractDetail();
        contractDetail.setAddress(configuredContractAddress);
        refreshContractDetailFromBlockchain(contractDetail, null);
        getWalletService().setConfiguredContractDetail(contractDetail);
      }
      configuredContractDecimals = getPrincipalContractDetail().getDecimals();
    } catch (Exception e) {
      LOG.warn("Error refreshing contract detail from blockchain with address {}", configuredContractAddress, e);
    }
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {

      // Create admin wallet if not exists
      Wallet wallet = getAccountService().getAdminWallet();
      if (wallet == null || StringUtils.isBlank(wallet.getAddress())) {
        if (StringUtils.isBlank(adminPrivateKey)) {
          createAdminAccount();
        } else {
          try {
            createAdminAccount(adminPrivateKey, getUserACL().getSuperUser());
            LOG.warn("Admin wallet private key has been imported, you can delete it from property to keep it safe");
          } catch (Exception e) {
            createAdminAccount();
          }
        }
      } else {
        LOG.warn("Admin wallet private key has been already imported, you can delete it from property to keep it safe!");
      }
    }catch (Exception e) {

    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public void createAdminAccount() {
    try {
      this.createAdminAccount(null, getUserACL().getSuperUser());
      LOG.info("Admin wallet created");
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("This exception shouldn't be thrown because no ACL check is made on server side method call",
                                      e);
    }
  }

  @Override
  public Wallet createAdminAccount(String privateKey, String currentUser) throws IllegalAccessException {
    if (!isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("User " + currentUser + " is not allowed to create admin wallet");
    }

    Identity identity = getIdentityByTypeAndId(WalletType.ADMIN, WALLET_ADMIN_REMOTE_ID);
    if (identity == null) {
      throw new IllegalStateException("Can't find identity of admin wallet");
    }

    long identityId = Long.parseLong(identity.getId());
    Wallet wallet = getAccountService().getWalletByIdentityId(identityId);
    if (wallet != null && wallet.getAddress() != null
        && getAccountService().getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID) != null) {
      throw new IllegalStateException("Admin wallet has already an associated wallet, thus can't overwrite it");
    }

    ECKeyPair ecKeyPair = null;
    if (StringUtils.isBlank(privateKey)) {
      try {
        ecKeyPair = Keys.createEcKeyPair();
      } catch (Exception e) {
        throw new IllegalStateException("Error creating new wallet keys pair", e);
      }
    } else {
      if (!WalletUtils.isValidPrivateKey(privateKey)) {
        throw new IllegalStateException("Private key isn't valid");
      }
      ecKeyPair = Credentials.create(privateKey).getEcKeyPair();
    }

    WalletFile adminWallet = null;
    try {
      adminWallet = org.web3j.crypto.Wallet.createLight(accountService.getAdminAccountPassword(), ecKeyPair);
    } catch (CipherException e) {
      throw new IllegalStateException("Error creating new wallet", e);
    }

    wallet = new Wallet();
    wallet.setEnabled(true);
    wallet.setId(WALLET_ADMIN_REMOTE_ID);
    wallet.setType(WalletType.ADMIN.getId());
    wallet.setAddress("0x" + adminWallet.getAddress());
    wallet.setTechnicalId(identityId);
    wallet.setProvider(WalletProvider.INTERNAL_WALLET.name());

    getAccountService().saveWalletAddress(wallet, currentUser);
    try {
      String walletJson = toJsonString(adminWallet);
      getAccountStorage().saveWalletPrivateKey(identityId, walletJson);
    } catch (Exception e) {
      // Make sure to delete corresponding wallet when the private key isn't
      // saved
      getAccountService().removeWalletByAddress(wallet.getAddress(), currentUser);
    }

    return wallet;
  }

  @Override
  public String getAdminWalletAddress() {
    Wallet adminWallet = getAccountService().getAdminWallet();
    return adminWallet == null ? null : adminWallet.getAddress();
  }


  @Override
  public final boolean isInitializedAccount(Wallet wallet) throws Exception {
    return (wallet.getIsInitialized() != null && wallet.getIsInitialized());
  }

  @Override
  public final BigInteger balanceOf(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    String contractAddress = checkContractAddress();
    return (BigInteger) executeReadOperation(contractAddress,
                                             MeedsToken.FUNC_BALANCEOF,
                                             address);
  }

  @Override
  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_GET_ETHER_BALANCE)
  public final BigInteger getEtherBalanceOf(String address) throws Exception { // NOSONAR
    Web3j web3j = getClientConnector().getWeb3j();
    if (web3j == null) {
      throw new IllegalStateException("Can't get ether balance of " + address + " . Connection is not established.");
    }
    return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
  }

  @Override
  public final TransactionDetail initialize(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiverAddress = transactionDetail.getTo();
    if (StringUtils.isBlank(receiverAddress)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (isInitializedAccount(transactionDetail.getToWallet())) {
      throw new IllegalStateException("Wallet {} is already initialized");
    }

    if (StringUtils.isBlank(configuredContractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }

    // Send tokens
    sendToken(transactionDetail, issuerUsername);
    TransactionDetail etherTransaction = transactionDetail.clone();
    // Send Ethers
    sendEther(etherTransaction, issuerUsername);
    getAccountService().setInitializationStatus(receiverAddress, WalletState.INITIALIZED);

    return transactionDetail;
  }

  @Override
  public TransactionDetail sendEther(TransactionDetail transactionDetail, String currentUserId) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiverAddress = transactionDetail.getTo();
    if (StringUtils.isBlank(receiverAddress)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (transactionDetail.getValue() <= 0) {
      throw new IllegalArgumentException("ether amount parameter has to be a positive amount");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, currentUserId);

    String adminWalletAddress = getAdminWalletAddress();
    BigInteger etherAmount = transactionDetail.getValueDecimal(ETHER_TO_WEI_DECIMALS);

    BigInteger adminEtherBalance = getEtherBalanceOf(adminWalletAddress);
    if (adminEtherBalance.compareTo(etherAmount) < 0) {
      throw new IllegalStateException("Wallet admin hasn't enough ether to initialize " + etherAmount.longValue() + " WEI to "
          + receiverAddress);
    }

    generateRawTransaction(receiverAddress,
                           null,
                           etherAmount,
                           transactionDetail);

    transactionDetail.setNetworkId(this.networkId);
    transactionDetail.setFrom(adminWalletAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    transactionDetail.setGasPrice(getAdminGasPrice());

    getTransactionService().saveTransactionDetail(transactionDetail, false);
    return transactionDetail;
  }

  @Override
  public final TransactionDetail sendToken(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    if (transactionDetail == null) {
      throw new IllegalArgumentException(TRANSACTION_DETAIL_IS_MANDATORY);
    }
    String receiverAddress = transactionDetail.getTo();
    if (StringUtils.isBlank(receiverAddress)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (transactionDetail.getContractAmount() <= 0) {
      throw new IllegalArgumentException("token amount parameter has to be positive");
    }

    checkAdminWalletIsValid();

    setIssuer(transactionDetail, issuerUsername);

    if (StringUtils.isBlank(configuredContractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    BigInteger tokenAmount = transactionDetail.getContractAmountDecimal(configuredContractDecimals);
    Function transferFunction = getTransferFunctionCall(receiverAddress, tokenAmount);
    generateRawTransaction(configuredContractAddress,
                           transferFunction,
                           BigInteger.ZERO,
                           transactionDetail);

    transactionDetail.setNetworkId(this.networkId);
    transactionDetail.setFrom(getAdminWalletAddress());
    transactionDetail.setContractAddress(configuredContractAddress);
    transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFER);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);

    getTransactionService().saveTransactionDetail(transactionDetail, false);
    return transactionDetail;
  }

  @Override
  public final TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    return sendToken(transactionDetail, issuerUsername);
  }

  @Override
  public void boostAdminTransactions() throws Exception {
    List<TransactionDetail> pendingTransactions = getTransactionService().getPendingTransactions();
    for (TransactionDetail transactionDetail : pendingTransactions) {
      if (transactionDetail.getFrom().equals(getAdminWalletAddress()) && // Is it an admin transaction ?
          transactionDetail.getGasPrice() < getAdminGasPrice().doubleValue() && // Current gas price is higher than the one used to send
                                                                  // original transaction
          getTransactionService().countTransactionsByNonce(transactionDetail) == 1) { // It should not have been already boosted
        TransactionDetail boostedTransaction = transactionDetail.clone();
        boostedTransaction.setId(0L);
        boostedTransaction.setNonce(transactionDetail.getNonce());
        boostedTransaction.setBoost(true);
        boostedTransaction.setSentTimestamp(0);
        boostedTransaction.setSendingAttemptCount(0);
        boostedTransaction.setPending(true);
        boostedTransaction.setSucceeded(false);
        if (transactionDetail.getContractAmount() > 0) {
          // Issuer is already set, we set it to null here
          sendToken(boostedTransaction, null);
        } else {
          // Issuer is already set, we set it to null here
          sendEther(boostedTransaction, null);
        }
        broadcastTransactionReplacedEvent(transactionDetail, boostedTransaction);
      }
    }
  }

  @Override
  public void retrieveWalletInformationFromBlockchain(Wallet wallet,
                                                      ContractDetail contractDetail,
                                                      Set<String> walletModifications) throws Exception {
    if (wallet == null) {
      throw new IllegalArgumentException("wallet is mandatory");
    }
    if (StringUtils.isBlank(wallet.getAddress())) {
      LOG.debug("No wallet address: {}", wallet);
      return;
    }
    if (contractDetail == null || StringUtils.isBlank(contractDetail.getAddress()) || contractDetail.getDecimals() == null) {
      throw new IllegalArgumentException("contractDetail is mandatory");
    }

    String walletAddress = wallet.getAddress();

    // Always refresh ether balance of wallet
    BigInteger walletEtherBalance = getEtherBalanceOf(walletAddress);
    wallet.setEtherBalance(convertFromDecimals(walletEtherBalance, ETHER_TO_WEI_DECIMALS));

    if (wallet.getTokenBalance() == null
        || walletModifications == null
        || walletModifications.contains(MeedsToken.FUNC_TRANSFER)
        || walletModifications.contains(MeedsToken.FUNC_TRANSFERFROM)
        || walletModifications.contains(MeedsToken.FUNC_APPROVE)) {
      BigInteger walletTokenBalance =
                                    (BigInteger) executeReadOperation(configuredContractAddress,
                                                                      MeedsToken.FUNC_BALANCEOF,
                                                                      walletAddress);
      wallet.setTokenBalance(convertFromDecimals(walletTokenBalance, configuredContractDecimals));
    }
  }

  @Override
  public void refreshContractDetailFromBlockchain(ContractDetail contractDetail, Set<String> contractModifications) {
    if (contractDetail == null) {
      throw new IllegalArgumentException("contractDetail is mandatory");
    }
    String contractAddress = contractDetail.getAddress();
    try {
      if (contractDetail.getNetworkId() == null || contractDetail.getNetworkId() <= 0) {
        contractDetail.setNetworkId(this.networkId);
      }

      if (contractDetail.getDecimals() == null || contractDetail.getDecimals() <= 0) {
        BigInteger decimals = (BigInteger) executeReadOperation(contractAddress, MeedsToken.FUNC_DECIMALS);
        contractDetail.setDecimals(decimals.intValue());
      }

      if (StringUtils.isEmpty(contractDetail.getName())) {
        String name = (String) executeReadOperation(contractAddress, MeedsToken.FUNC_NAME);
        contractDetail.setName(name);
      }

      if (StringUtils.isEmpty(contractDetail.getSymbol())) {
        String symbol = (String) executeReadOperation(contractAddress, MeedsToken.FUNC_SYMBOL);
        contractDetail.setSymbol(symbol);
      }

      if (StringUtils.isEmpty(contractDetail.getTotalSupply())) {
        BigInteger totalSupply = (BigInteger) executeReadOperation(contractAddress, MeedsToken.FUNC_TOTALSUPPLY);
        contractDetail.setTotalSupply(String.valueOf(convertFromDecimals(totalSupply, contractDetail.getDecimals())));
      }

      if (contractDetail.getEtherBalance() == null
          || contractModifications == null
          || contractModifications.contains(TOKEN_FUNC_DEPOSIT_FUNDS)
          || contractModifications.contains(MeedsToken.FUNC_TRANSFER)
          || contractModifications.contains(MeedsToken.FUNC_TRANSFERFROM)
          || contractModifications.contains(MeedsToken.FUNC_APPROVE)) {
        BigInteger contractEtherBalance = getEtherBalanceOf(contractAddress);
        contractDetail.setEtherBalance(convertFromDecimals(contractEtherBalance, ETHER_TO_WEI_DECIMALS));
      }
      getContractService().saveContractDetail(contractDetail);
    } catch (Exception e) {
      throw new IllegalStateException("Error while retrieving contract details from blockchain with address: " + contractAddress,
                                      e);
    }
  }

  @ExoWalletStatistic(service = "org/exoplatform/wallet/blockchain", local = false, operation = OPERATION_READ_FROM_TOKEN)
  public Object executeReadOperation(final String contractAddress,
                                     final String methodName,
                                     final Object... arguments) throws Exception {
    MeedsToken contractInstance = getContractInstance(contractAddress);
    Method methodToInvoke = getMethod(methodName);
    if (methodToInvoke == null) {
      throw new IllegalStateException("Can't find method " + methodName + " in Token instance");
    }
    RemoteCall<?> response = (RemoteCall<?>) methodToInvoke.invoke(contractInstance, arguments);
    return response.send();
  }

  @Override
  public Map<String, Object> getStatisticParameters(String operation, Object result, Object... methodArgs) {
    Map<String, Object> parameters = new HashMap<>();

    if (networkId > 0 && StringUtils.isNotBlank(websocketURL)) {
      if (websocketURLSuffix == null) {
        String[] urlParts = websocketURL.split("/");
        websocketURLSuffix = urlParts[urlParts.length - 1];
      }
      parameters.put("blockchain_network_url_suffix", websocketURLSuffix);
      parameters.put("blockchain_network_id", networkId);
    }

    switch (operation) {
    case OPERATION_GET_ETHER_BALANCE:
      parameters.put("address", methodArgs[0]);
      break;
    case OPERATION_READ_FROM_TOKEN:
      parameters.put("contract_address", methodArgs[0]);
      parameters.put("contract_method", methodArgs[1]);
      break;
    default:
      LOG.warn("Statistic type {} is not managed", operation);
      return null;
    }
    return parameters;
  }

  @Override
  public String generateHash(String rawTransaction) {
    return Hash.sha3(rawTransaction);
  }

  private void generateRawTransaction(String toAddress,
                                      Function function,
                                      BigInteger etherValueInWei,
                                      TransactionDetail transactionDetail) throws Exception { // NOSONAR
    Credentials adminCredentials = getAdminCredentials();
    if (adminCredentials == null) {
      throw new IllegalStateException("Can't find admin credentials");
    }

    Long adminGasPrice = getAdminGasPrice();
    BigInteger gasPrice = BigInteger.valueOf(adminGasPrice);
    transactionDetail.setGasPrice(adminGasPrice);
    BigInteger gasLimit = BigInteger.valueOf(getGasLimit());
    if(transactionDetail.getNonce() == 0) {
      BigInteger nonceToUse = getAdminNonce();
      transactionDetail.setNonce(nonceToUse.longValue());
    }

    RawTransaction rawTransaction = null;
    if (function != null) {
      String transactionData = FunctionEncoder.encode(function);
      rawTransaction = RawTransaction.createTransaction(BigInteger.valueOf(transactionDetail.getNonce()),
                                                        gasPrice,
                                                        gasLimit,
                                                        toAddress,
                                                        etherValueInWei,
                                                        transactionData);
    } else {
      rawTransaction = RawTransaction.createEtherTransaction(BigInteger.valueOf(transactionDetail.getNonce()),
                                                             gasPrice,
                                                             gasLimit,
                                                             toAddress,
                                                             etherValueInWei);
    }

    byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, getNetworkId(), adminCredentials);
    String rawTransactionString = Numeric.toHexString(signedMessage);
    transactionDetail.setRawTransaction(rawTransactionString);
    transactionDetail.setHash(generateHash(rawTransactionString));
  }

  private BigInteger getAdminNonce() throws IOException {
    String adminAddress = getAdminWalletAddress();
    long blockchainNonce = getClientConnector().getNonce(adminAddress, DefaultBlockParameterName.PENDING).longValue();
    long storedNonce = getTransactionService().getNonce(adminAddress);
    return BigInteger.valueOf(Long.max(blockchainNonce, storedNonce));
  }

  private MeedsToken getContractInstance(final String contractAddress) {
    // Retrieve cached contract instance
    if (this.ertInstance == null) {
      BigInteger gasPrice = BigInteger.valueOf(getAdminGasPrice());
      BigInteger gasLimit = BigInteger.valueOf(getGasLimit());
      Web3j web3j = getClientConnector().getWeb3j();

      this.ertInstance = MeedsToken.load(contractAddress,
                                         web3j,
                                         new ReadonlyTransactionManager(web3j, Address.DEFAULT.toString()),
                                         new StaticGasProvider(gasPrice, gasLimit));
    }
    return this.ertInstance;
  }

  private String checkContractAddress() {
    if (StringUtils.isBlank(configuredContractAddress)) {
      throw new IllegalStateException(NO_CONFIGURED_CONTRACT_ADDRESS);
    }
    return configuredContractAddress;
  }

  private void setIssuer(TransactionDetail transactionDetail, String issuerUsername) {
    if (StringUtils.isNotBlank(issuerUsername)) {
      Wallet issuerWallet = getAccountService().getWalletByTypeAndId(WalletType.USER.name(), issuerUsername);
      if (issuerWallet == null) {
        throw new IllegalStateException("Can't find identity of user with id " + issuerUsername);
      }
      transactionDetail.setIssuer(issuerWallet);
    }
  }

  private final void checkAdminWalletIsValid() throws Exception {
    String adminAddress = getAdminWalletAddress();
    if (adminAddress == null) {
      throw new IllegalStateException("No admin wallet is set");
    }
  }

  private Credentials getAdminCredentials() {
    ECKeyPair adminWalletKeys = (ECKeyPair) getAdminWalletKeys();
    if (adminWalletKeys == null) {
      return null;
    } else {
      return Credentials.create(adminWalletKeys);
    }
  }

  private Object getAdminWalletKeys() {
    String privateKey = getAccountService().getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID);
    if (StringUtils.isBlank(privateKey)) {
      return null;
    }
    WalletFile adminWallet = null;
    try {
      ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
      adminWallet = objectMapper.readerFor(WalletFile.class).readValue(privateKey);
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while parsing admin wallet keys", e);
    }
    try {
      return org.web3j.crypto.Wallet.decrypt(accountService.getAdminAccountPassword(), adminWallet);
    } catch (CipherException e) {
      throw new IllegalStateException("Can't descrypt stored admin wallet", e);
    }
  }

  private Method getMethod(String methodName) {
    Method methodToInvoke = null;
    Method[] methods = MeedsToken.class.getDeclaredMethods();
    for (Method method : methods) {
      if (StringUtils.equals(methodName, method.getName())) {
        methodToInvoke = method;
      }
    }
    return methodToInvoke;
  }

  private ContractDetail getPrincipalContractDetail() {
    GlobalSettings settings = getSettings();
    return settings.getContractDetail();
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  private WalletStorage getAccountStorage() {
    if (accountStorage == null) {
      accountStorage = CommonsUtils.getService(WalletStorage.class);
    }
    return accountStorage;
  }

  private WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

  private WalletContractService getContractService() {
    if (walletContractService == null) {
      walletContractService = CommonsUtils.getService(WalletContractService.class);
    }
    return walletContractService;
  }

  private WalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(WalletService.class);
    }
    return walletService;
  }

  private EthereumClientConnector getClientConnector() {
    return clientConnector;
  }

  private UserACL getUserACL() {
    if (userACL == null) {
      userACL = CommonsUtils.getService(UserACL.class);
    }
    return userACL;
  }

  @SuppressWarnings("rawtypes")
  private Function getTransferFunctionCall(String toAddress,
                                           BigInteger tokenAmount) {
    return new Function(MeedsToken.FUNC_TRANSFER,
                        Arrays.<Type> asList(new Address(toAddress),
                                             new Uint256(tokenAmount)),
                        Collections.<TypeReference<?>> emptyList());
  }
}
