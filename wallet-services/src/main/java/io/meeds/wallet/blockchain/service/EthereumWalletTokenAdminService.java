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
package io.meeds.wallet.blockchain.service;

import static io.meeds.wallet.utils.WalletUtils.ETHER_TO_WEI_DECIMALS;
import static io.meeds.wallet.utils.WalletUtils.OPERATION_READ_FROM_TOKEN;
import static io.meeds.wallet.utils.WalletUtils.WALLET_ADMIN_REMOTE_ID;
import static io.meeds.wallet.utils.WalletUtils.convertFromDecimals;
import static io.meeds.wallet.utils.WalletUtils.getContractDetail;
import static io.meeds.wallet.utils.WalletUtils.getGasLimit;
import static io.meeds.wallet.utils.WalletUtils.getIdentityByTypeAndId;
import static io.meeds.wallet.utils.WalletUtils.getNetworkId;
import static io.meeds.wallet.utils.WalletUtils.getSettings;
import static io.meeds.wallet.utils.WalletUtils.getWebsocketURL;
import static io.meeds.wallet.utils.WalletUtils.isUserRewardingAdmin;
import static io.meeds.wallet.utils.WalletUtils.toJsonString;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.future.FutureCache;
import org.exoplatform.services.cache.future.FutureExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.contract.MeedsToken;

import io.meeds.wallet.model.ContractDetail;
import io.meeds.wallet.model.GlobalSettings;
import io.meeds.wallet.model.TransactionDetail;
import io.meeds.wallet.model.Wallet;
import io.meeds.wallet.model.WalletType;
import io.meeds.wallet.service.WalletAccountService;
import io.meeds.wallet.service.WalletContractService;
import io.meeds.wallet.service.WalletService;
import io.meeds.wallet.service.WalletTokenAdminService;
import io.meeds.wallet.service.WalletTransactionService;
import io.meeds.wallet.statistic.ExoWalletStatistic;
import io.meeds.wallet.statistic.ExoWalletStatisticService;
import io.meeds.wallet.storage.WalletStorage;

public class EthereumWalletTokenAdminService implements WalletTokenAdminService, Startable, ExoWalletStatisticService {
  private static final Log                        LOG                                     =
                                                      ExoLogger.getLogger(EthereumWalletTokenAdminService.class);

  private static final String                     ADMIN_WALLET_CHECK_MESSAGE              =
                                                                             "Can't access admin wallet keys. Please verify that Codec Key File and 'exo.wallet.admin.key' property value remains unchanged between startups";

  private static final String                     NO_CONFIGURED_CONTRACT_ADDRESS          = "No configured contract address";

  private static final String                     TRANSACTION_DETAIL_IS_MANDATORY         = "Transaction detail is mandatory";

  private static final String                     RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY =
                                                                                          "receiver address parameter is mandatory";

  private PortalContainer                         container;

  private UserACL                                 userACL;

  private WalletService                           walletService;

  private WalletContractService                   walletContractService;

  private EthereumClientConnector                 clientConnector;

  private WalletAccountService                    accountService;

  private WalletStorage                           accountStorage;

  private WalletTransactionService                transactionService;

  private MeedsToken                              ertInstance;

  private long                                    networkId                               = 0;

  private String                                  websocketURL                            = null;

  private String                                  websocketURLSuffix                      = null;

  private String                                  configuredContractAddress;

  private Integer                                 configuredContractDecimals;

  private String                                  adminPrivateKey;

  private long                                    adminBlockchainNonce;

  private long                                    adminBlockchainNonceLastUpdate;

  private boolean                                 checkAdminKey = true;

  private FutureCache<String, BigInteger, Object> tokenBalanceFutureCache                 = null;

  private FutureCache<String, BigInteger, Object> etherBalanceFutureCache                 = null;

  public EthereumWalletTokenAdminService(PortalContainer container,
                                         CacheService cacheService,
                                         WalletService walletService,
                                         EthereumClientConnector clientConnector) {
    this.container = container;
    this.walletService = walletService;
    this.clientConnector = clientConnector;
    ExoCache<String, BigInteger> tokenBalanceCache = cacheService.getCacheInstance("wallet.blockchain.tokenBalance");
    tokenBalanceCache.setLiveTime(5);
    ExoCache<String, BigInteger> etherBalanceCache = cacheService.getCacheInstance("wallet.blockchain.etherBalance");
    etherBalanceCache.setLiveTime(5);
    tokenBalanceFutureCache = new FutureExoCache<>((context, address) -> getTokenBalanceOfFromBlockchain(address),
                                                   tokenBalanceCache);
    etherBalanceFutureCache = new FutureExoCache<>((context, address) -> getClientConnector().getEtherBalanceOf(address),
                                                   etherBalanceCache);
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
      this.checkAdminKey = !"false".equals(System.getProperty("exo.wallet.admin.checkAdminKey"));
      this.configuredContractAddress = settings.getContractAddress();
      this.websocketURL = getWebsocketURL();
      this.networkId = getNetworkId();

      ContractDetail contractDetail = getContractService().getContractDetail(configuredContractAddress);
      if (contractDetail == null) {
        contractDetail = new ContractDetail();
        contractDetail.setAddress(configuredContractAddress);
        contractDetail.setDecimals(18);
        contractDetail.setName("Meeds Token");
        contractDetail.setSymbol("MEED");
        contractDetail.setNetworkId(this.networkId);
        getContractService().saveContractDetail(contractDetail);
        walletService.setConfiguredContractDetail(contractDetail);
      }
      configuredContractDecimals = getPrincipalContractDetail().getDecimals();
    } catch (Exception e) {
      LOG.warn("Error refreshing contract detail from blockchain with address {}", configuredContractAddress, e);
    }
    initAdminWallet();
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

    getAccountService().saveWalletAddress(wallet, currentUser);
    try {
      String walletJson = toJsonString(adminWallet);
      getAccountStorage().saveWalletPrivateKey(identityId, walletJson);
    } catch (Exception e) {
      // Make sure to delete corresponding wallet when the private key isn't
      // saved
      getAccountService().removeWalletByAddress(wallet.getAddress(), currentUser);
    }

    if (StringUtils.isNotBlank(privateKey)) {
      getAccountService().refreshWalletFromBlockchain(wallet, getContractDetail(), null);
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
  public final BigInteger getTokenBalanceOf(String address) {
    return tokenBalanceFutureCache.get(null, address.toLowerCase());
  }

  @Override
  public final BigInteger getEtherBalanceOf(String address) throws Exception { // NOSONAR
    return etherBalanceFutureCache.get(null, address.toLowerCase());
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

    generateRawTransaction(receiverAddress, null, etherAmount, transactionDetail);

    transactionDetail.setNetworkId(this.networkId);
    transactionDetail.setFrom(adminWalletAddress);
    transactionDetail.setTimestamp(System.currentTimeMillis());
    transactionDetail.setAdminOperation(false);
    transactionDetail.setPending(true);
    transactionDetail.setGasPrice(walletService.getGasPrice());

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
    try {
      generateRawTransaction(configuredContractAddress, transferFunction, BigInteger.ZERO, transactionDetail);
      transactionDetail.setPending(true);
    } catch (Exception e) {
      transactionDetail.setPending(false);
      transactionDetail.setNonce(0);
    } finally {
      transactionDetail.setNetworkId(this.networkId);
      transactionDetail.setFrom(getAdminWalletAddress());
      transactionDetail.setContractAddress(configuredContractAddress);
      transactionDetail.setContractMethodName(MeedsToken.FUNC_TRANSFER);
      transactionDetail.setTimestamp(System.currentTimeMillis());
      transactionDetail.setAdminOperation(false);
      if (StringUtils.isNotBlank(transactionDetail.getHash())) {
        getTransactionService().saveTransactionDetail(transactionDetail, false);
      }
    }
    return transactionDetail;
  }

  @Override
  public final TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception {
    return sendToken(transactionDetail, issuerUsername);
  }

  @Override
  public void boostAdminTransactions() {
    List<TransactionDetail> pendingTransactions =
                                                getTransactionService().getPendingWalletTransactionsSent(getAdminWalletAddress());
    double gasPrice = walletService.getGasPrice();
    for (TransactionDetail transactionDetail : pendingTransactions) {
      boolean newGasPriceIsHigher = transactionDetail.getGasPrice() < gasPrice;
      boolean alreadyBoosted = transactionDetail.isBoost();
      long sentTimestamp = transactionDetail.getSentTimestamp();
      boolean exceededWaitTime = sentTimestamp > 0 && (System.currentTimeMillis() - sentTimestamp) > 7200000;
      if (newGasPriceIsHigher && !alreadyBoosted && exceededWaitTime) {
        LOG.info("Boost transaction {} which was sent since {}", transactionDetail.getHash(), new Date(sentTimestamp));
        TransactionDetail boostedTransaction = transactionDetail.clone();
        boostedTransaction.setId(0L);
        boostedTransaction.setHash(null);
        boostedTransaction.setBoost(true);
        boostedTransaction.setSentTimestamp(0);
        boostedTransaction.setSendingAttemptCount(0);
        boostedTransaction.setPending(true);
        try {
          if (transactionDetail.getContractAmount() > 0) {
            // Issuer is already set, we set it to null here
            sendToken(boostedTransaction, null);
          } else {
            // Issuer is already set, we set it to null here
            sendEther(boostedTransaction, null);
          }
          transactionDetail.setPending(false);
          transactionDetail.setDropped(true);
          transactionDetail.setNonce(0);
          transactionService.saveTransactionDetail(transactionDetail, true);
        } catch (Exception e) {
          LOG.warn("Can't boost transaction {} with the new one {}",
                   transactionDetail.getHash(),
                   boostedTransaction.getHash(),
                   e);
        }
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
      return;
    }
    if (contractDetail == null || StringUtils.isBlank(contractDetail.getAddress()) || contractDetail.getDecimals() == null) {
      throw new IllegalArgumentException("contractDetail is mandatory");
    }

    String walletAddress = wallet.getAddress();

    // Always refresh ether balance of wallet
    BigInteger walletEtherBalance = getEtherBalanceOf(walletAddress);
    wallet.setEtherBalance(convertFromDecimals(walletEtherBalance, ETHER_TO_WEI_DECIMALS));

    if (wallet.getTokenBalance() == null || walletModifications == null || walletModifications.contains(MeedsToken.FUNC_TRANSFER)
        || walletModifications.contains(MeedsToken.FUNC_TRANSFERFROM)) {
      BigInteger walletTokenBalance = getTokenBalanceOf(walletAddress);
      wallet.setTokenBalance(convertFromDecimals(walletTokenBalance, configuredContractDecimals));
    }
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

    if (!StringUtils.equals(OPERATION_READ_FROM_TOKEN, operation)) {
      LOG.warn("Statistic type {} is not managed", operation);
      return null; // NOSONAR must be null if operation not known
    }
    parameters.put("contract_address", methodArgs[0]);
    parameters.put("contract_method", methodArgs[1]);
    return parameters;
  }

  @Override
  public String generateHash(String rawTransaction) {
    return Hash.sha3(rawTransaction);
  }

  public final BigInteger getTokenBalanceOfFromBlockchain(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    String contractAddress = checkContractAddress();
    return (BigInteger) executeReadOperation(contractAddress, MeedsToken.FUNC_BALANCEOF, address);
  }

  @ExoWalletStatistic(service = "blockchain", local = false, operation = OPERATION_READ_FROM_TOKEN)
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

  @ExoTransactional
  public void initAdminWallet() {
    // Create admin wallet if not exists
    Wallet adminWallet = getAccountService().getAdminWallet();
    if (adminWallet == null || StringUtils.isBlank(adminWallet.getAddress())) {
      createAdminWalletAsync();
    } else if (this.checkAdminKey){
      checkAdminWallet();
    }
  }

  private void checkAdminWallet() {
    Credentials credentials;
    try {
      credentials = getAdminCredentials();
    } catch (Exception e) {
      throw new IllegalStateException(ADMIN_WALLET_CHECK_MESSAGE, e);
    }
    if (credentials == null) {
      throw new IllegalStateException(ADMIN_WALLET_CHECK_MESSAGE);
    } else if (StringUtils.isNotBlank(adminPrivateKey)) {
      LOG.warn("Admin wallet private key has been already imported, you can delete it from property to keep it safe!");
    }
  }

  private void createAdminWalletAsync() {
    CompletableFuture.runAsync(() -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
      try {
        if (StringUtils.isBlank(adminPrivateKey)) {
          createAdminAccount();
        } else {
          createAdminAccount(adminPrivateKey, getUserACL().getSuperUser());
          LOG.warn("Admin wallet private key has been imported, you can delete it from property to keep it safe");
        }
      } catch (Exception e) {
        LOG.warn("Error while creating Admin wallet", e);
      } finally {
        RequestLifeCycle.end();
      }
    });
  }

  private void generateRawTransaction(String toAddress,
                                      Function function,
                                      BigInteger etherValueInWei,
                                      TransactionDetail transactionDetail) throws Exception { // NOSONAR
    Credentials adminCredentials = getAdminCredentials();
    if (adminCredentials == null) {
      throw new IllegalStateException("Can't find admin credentials");
    }

    double adminGasPrice = walletService.getGasPrice();
    BigInteger gasPrice = BigDecimal.valueOf(adminGasPrice).toBigInteger();
    transactionDetail.setGasPrice(adminGasPrice);
    BigInteger gasLimit = BigInteger.valueOf(getGasLimit());
    if (transactionDetail.getNonce() == 0) {
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
    if (System.currentTimeMillis() - adminBlockchainNonceLastUpdate > 15000) {
      try {
        adminBlockchainNonce = getClientConnector().getNonce(adminAddress, DefaultBlockParameterName.PENDING).longValue();
        adminBlockchainNonceLastUpdate = System.currentTimeMillis();
      } catch (Exception e) {
        LOG.warn("Error retrieving nonce of admin wallet, use last sent nonce", e);
      }
    }
    long storedNonce = getTransactionService().getNonce(adminAddress);
    return BigInteger.valueOf(Long.max(adminBlockchainNonce, storedNonce));
  }

  private MeedsToken getContractInstance(final String contractAddress) {
    Web3j web3j = getClientConnector().getWeb3j(false);
    // Retrieve cached contract instance
    if (this.ertInstance == null) {
      double adminGasPrice = walletService.getGasPrice();
      BigInteger gasPrice = BigDecimal.valueOf(adminGasPrice).toBigInteger();
      BigInteger gasLimit = BigInteger.valueOf(getGasLimit());

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

  private final void checkAdminWalletIsValid() {
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
  private Function getTransferFunctionCall(String toAddress, BigInteger tokenAmount) {
    return new Function(MeedsToken.FUNC_TRANSFER,
                        Arrays.<Type> asList(new Address(toAddress), new Uint256(tokenAmount)),
                        Collections.<TypeReference<?>> emptyList());
  }

}
