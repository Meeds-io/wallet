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
package io.meeds.wallet.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.mockito.Mockito;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import io.meeds.wallet.dao.AddressLabelDAO;
import io.meeds.wallet.dao.WalletAccountBackupDAO;
import io.meeds.wallet.dao.WalletAccountDAO;
import io.meeds.wallet.dao.WalletBlockchainStateDAO;
import io.meeds.wallet.dao.WalletPrivateKeyDAO;
import io.meeds.wallet.dao.WalletTransactionDAO;
import io.meeds.wallet.entity.AddressLabelEntity;
import io.meeds.wallet.entity.TransactionEntity;
import io.meeds.wallet.entity.WalletBackupEntity;
import io.meeds.wallet.entity.WalletBlockchainStateEntity;
import io.meeds.wallet.entity.WalletEntity;
import io.meeds.wallet.entity.WalletPrivateKeyEntity;
import io.meeds.wallet.model.ContractDetail;
import io.meeds.wallet.model.ContractTransactionEvent;
import io.meeds.wallet.model.TransactionDetail;
import io.meeds.wallet.model.Wallet;
import io.meeds.wallet.model.WalletAddressLabel;
import io.meeds.wallet.model.WalletState;
import io.meeds.wallet.model.WalletType;
import io.meeds.wallet.service.BlockchainTransactionService;
import io.meeds.wallet.service.WalletAccountService;
import io.meeds.wallet.service.WalletService;
import io.meeds.wallet.storage.AddressLabelStorage;
import io.meeds.wallet.storage.TransactionStorage;
import io.meeds.wallet.storage.WalletStorage;
import io.meeds.wallet.storage.cached.CachedAccountStorage;
import io.meeds.wallet.storage.cached.CachedTransactionStorage;
import io.meeds.wallet.test.mock.IdentityManagerMock;
import io.meeds.wallet.test.mock.SpaceServiceMock;
import io.meeds.wallet.utils.WalletUtils;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wallet-services-test-configuration.xml"),
})
public abstract class BaseWalletTest extends AbstractKernelTest {

  private static final Log       LOG                      = ExoLogger.getLogger(BaseWalletTest.class);

  protected static final String  WALLET_ADDRESS_1         = "0x1111111111111111111111111111111111111111";

  protected static final String  WALLET_ADDRESS_2         = "0x2222222222222222222222222222222222222222";

  protected static final String  WALLET_ADDRESS_3         = "0x3333333333333333333333333333333333333333";

  protected static final String  TRANSACTION_LABEL        = "TRANSACTION_LABEL";

  protected static final String  TRANSACTION_MESSAGE      = "TRANSACTION_MESSAGE";

  protected static final int     GAS_USED                 = 160000;

  protected static final double  GAS_PRICE                = 0.000000006d;

  protected static final double  GAS_PRICE_WEI            = 6000000000l;

  protected static final String  RAW_TRANSACTION          = "RAW_TRANSACTION";

  protected static final double  TOKEN_FEE                = (GAS_PRICE * GAS_USED) / 0.001;

  protected static final long    NONCE                    = 10;

  protected static final double  ETHER_VALUE              = 0.001;

  protected static final String  PHRASE                   = "passphrase";

  protected static final String  INITIALIZATION_STATE     = WalletState.INITIALIZED.name();

  protected static final String  TYPE                     = WalletType.SPACE.name();

  protected static final boolean IS_ENABLED               = true;

  protected static final String  CURRENT_USER             = "root1";

  protected static final long    CURRENT_USER_IDENTITY_ID = 1L;

  protected static final String  CURRENT_SPACE            = "space1";

  protected static final long    NETWORK_ID               = 137;

  protected static final int     CONTRACT_AMOUNT          = 500;

  protected static final String  USER_TEST                = "root9";

  protected static final long    USER_TEST_IDENTITY_ID    = 9;

  @Rule
  public TestName                testName                 = new TestName();

  protected Set<Serializable>    entitiesToClean          = null;

  protected Random               random                   = new Random(1);

  protected PortalContainer      container;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    if (container == null) {
      assertNotNull("Container shouldn't be null", getContainer());
      assertTrue("Container should have been started", getContainer().isStarted());
      IdentityManager identityManager = getContainer().getComponentInstanceOfType(IdentityManager.class);
      assertTrue("Identity Manager should be mocked, found: " + identityManager.getClass().getName(), identityManager instanceof IdentityManagerMock);
      SpaceService spaceService = getContainer().getComponentInstanceOfType(SpaceService.class);
      assertTrue("SpaceService should be mocked, found: " + spaceService.getClass().getName(), spaceService instanceof SpaceServiceMock);

      container = getContainer();

      BlockchainTransactionService blockchainTransactionService =
                                                                container.getComponentInstanceOfType(BlockchainTransactionService.class);
      if (blockchainTransactionService != null) {
        container.unregisterComponent(BlockchainTransactionService.class);
      }
      blockchainTransactionService = Mockito.mock(BlockchainTransactionService.class);
      Mockito.lenient().when(blockchainTransactionService.getGasPrice()).thenReturn(GAS_PRICE_WEI);
      container.registerComponentInstance(BlockchainTransactionService.class, blockchainTransactionService);

      setContractDetails();

      setRoot1AsAdmin();
    }
    entitiesToClean = new HashSet<>();
    begin();
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
    WalletAccountBackupDAO walletAccountBackupDAO = getService(WalletAccountBackupDAO.class);
    AddressLabelDAO addressLabelDAO = getService(AddressLabelDAO.class);
    WalletPrivateKeyDAO walletPrivateKeyDAO = getService(WalletPrivateKeyDAO.class);
    WalletBlockchainStateDAO walletBlockchainStateDAO = getService(WalletBlockchainStateDAO.class);
    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);

    LOG.info("Cleaning {} objects after test finished", entitiesToClean.size());

    restartTransaction();
    walletBlockchainStateDAO.deleteAll();
    restartTransaction();

    if (!entitiesToClean.isEmpty()) {
      Iterator<Serializable> iterator = entitiesToClean.iterator();
      while (iterator.hasNext()) {
        Serializable entity = iterator.next();
        if (entity == null) {
          iterator.remove();
          continue;
        }
        try {
          if (entity instanceof WalletBackupEntity) {
            WalletBackupEntity walletBackupEntity = (WalletBackupEntity) entity;
            if (walletBackupEntity.getId() > 0) {
              walletBackupEntity = walletAccountBackupDAO.find(walletBackupEntity.getId());
              if (walletBackupEntity != null) {
                walletAccountBackupDAO.delete(walletBackupEntity);
              }
              iterator.remove();
            }
          }
          if (entity instanceof WalletEntity) {
            WalletEntity wallet = (WalletEntity) entity;
            if (wallet.getId() > 0) {
              WalletBackupEntity walletBackup = walletAccountBackupDAO.findByWalletId(wallet.getId());
              if (walletBackup != null) {
                walletAccountBackupDAO.delete(walletBackup);
              }
              walletAccountDAO.delete(walletAccountDAO.find(wallet.getId()));
              iterator.remove();
            }
          } else if (entity instanceof WalletPrivateKeyEntity) {
            WalletPrivateKeyEntity privateKey = (WalletPrivateKeyEntity) entity;
            if (privateKey.getId() > 0) {
              walletPrivateKeyDAO.delete(walletPrivateKeyDAO.find(privateKey.getId()));
              iterator.remove();
            }
          } else if (entity instanceof TransactionEntity) {
            TransactionEntity transactionEntity = (TransactionEntity) entity;
            if (transactionEntity.getId() > 0) {
              walletTransactionDAO.delete(walletTransactionDAO.find(transactionEntity.getId()));
              iterator.remove();
            }
          } else if (entity instanceof AddressLabelEntity) {
            AddressLabelEntity addressEntity = (AddressLabelEntity) entity;
            if (addressEntity.getId() > 0) {
              addressLabelDAO.delete(addressLabelDAO.find(addressEntity.getId()));
              iterator.remove();
            }
          } else if (entity instanceof WalletAddressLabel) {
            AddressLabelEntity labelEntity = addressLabelDAO.find(((WalletAddressLabel) entity).getId());
            if (labelEntity.getId() > 0) {
              addressLabelDAO.delete(labelEntity);
              iterator.remove();
            }
          } else if (entity instanceof TransactionDetail) {
            long transactionId = ((TransactionDetail) entity).getId();
            if (transactionId > 0) {
              TransactionEntity transactionEntity = walletTransactionDAO.find(transactionId);
              if (transactionEntity != null) {
                walletTransactionDAO.delete(transactionEntity);
              }
              iterator.remove();
            }
          } else if (entity instanceof Wallet) {
            Wallet wallet = (Wallet) entity;
            long walletId = wallet.getTechnicalId();
            if (walletId > 0) {
              WalletBackupEntity walletBackup = walletAccountBackupDAO.findByWalletId(walletId);
              if (walletBackup != null) {
                walletAccountBackupDAO.delete(walletBackup);
              }
              WalletEntity walletEntity = walletAccountDAO.find(walletId);
              if (walletEntity != null) {
                WalletBlockchainStateEntity blockchainStateEntity =
                                                                  walletBlockchainStateDAO.findByWalletIdAndContract(walletId,
                                                                                                                     WalletUtils.getContractAddress());
                if (blockchainStateEntity != null) {
                  walletBlockchainStateDAO.delete(blockchainStateEntity);
                }
                walletAccountDAO.delete(walletEntity);
              }
              iterator.remove();
            }
          } else {
            LOG.warn("Entity not managed {}", entity);
          }
        } catch (Exception e) {
          LOG.warn("Error cleaning entities after test '{}' execution", this.getClass().getName(), e);
        }
      }
    }

    long walletCount = walletAccountDAO.count();
    long walletBackupCount = walletAccountBackupDAO.count();
    long walletPrivateKeyCount = walletPrivateKeyDAO.count();
    long walletAddressLabelsCount = addressLabelDAO.findAll().size();
    long walletTransactionsCount = walletTransactionDAO.count();

    LOG.info("objects count: remaining entities to clean: {}, wallets = {}, backupWallets = {}, private keys = {}, address labels = {}, transactions count = {}. Test method '{}#{}'",
             entitiesToClean,
             walletCount,
             walletBackupCount,
             walletPrivateKeyCount,
             walletAddressLabelsCount,
             walletTransactionsCount,
             this.getClass().getSimpleName(),
             testName.getMethodName());

    assertEquals("The previous test didn't cleaned wallets entities correctly, should add entities to clean into 'entitiesToClean' list: ",
                 0,
                 walletCount);
    assertEquals("The previous test didn't cleaned backupWallets entities correctly, should add entities to clean into 'entitiesToClean' list: ",
                 0,
                 walletBackupCount);
    assertEquals("The previous test didn't cleaned wallet addresses labels correctly, should add entities to clean into 'entitiesToClean' list.",
                 0,
                 walletAddressLabelsCount);
    assertEquals("The previous test didn't cleaned wallets private keys entities correctly, should add entities to clean into 'entitiesToClean' list.",
                 0,
                 walletPrivateKeyCount);
    assertEquals("The previous test didn't cleaned wallets transactions entities correctly, should add entities to clean into 'entitiesToClean' list.",
                 0,
                 walletTransactionsCount);

    AddressLabelStorage addressLabelStorage = getService(AddressLabelStorage.class);
    addressLabelStorage.clearCache();

    WalletStorage walletStorage = getService(WalletStorage.class);
    if (walletStorage instanceof CachedAccountStorage) {
      ((CachedAccountStorage) walletStorage).clearCache();
    }

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    if (transactionStorage instanceof CachedTransactionStorage) {
      ((CachedTransactionStorage) transactionStorage).clearCache();
    }

    end();
  }

  protected List<TransactionEntity> generateTransactions(String walletAddress,
                                                         String contractAddress,
                                                         String contractMethodName) {
    return generateTransactions(walletAddress, contractAddress, contractMethodName, 0);
  }

  protected List<TransactionEntity> generateTransactions(String walletAddress,
                                                         String contractAddress,
                                                         String contractMethodName,
                                                         long offsetTime) {
    String secondAddress = "0xeaaaec7864af9e581a85ce3987d026be0f509ac9";
    String thirdAddress = "0xeaaaec7864af9e581a85ce3987d026be0f509ac9";

    String otherContractAddress = "0xeeefec7864af9e581a85ce3987d026be0f509aaa";
    String otherContractMethodName = "transferFrom";

    List<TransactionEntity> transactionEntities = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      String contractAddressToUse = null;
      String contractMethodNameToUse = null;

      if (StringUtils.isNotBlank(contractAddress)) {
        contractAddressToUse = i % 2 == 0 ? contractAddress : otherContractAddress; // NOSONAR
      }
      if (StringUtils.isNotBlank(contractMethodName)) {
        contractMethodNameToUse = i % 3 == 0 ? contractMethodName : i % 3 == 1 ? otherContractMethodName : null; // NOSONAR
      }

      String to = i % 3 == 0 ? walletAddress : i % 3 == 1 ? secondAddress : thirdAddress; // NOSONAR
      String from = i % 3 == 0 ? thirdAddress : i % 3 == 1 ? walletAddress : secondAddress; // NOSONAR
      String by = i % 3 == 0 ? secondAddress : i % 3 == 1 ? thirdAddress : walletAddress; // NOSONAR

      TransactionEntity tx = createTransaction(null,
                                               contractAddressToUse,
                                               contractMethodNameToUse,
                                               1, // token amount
                                               3, // ether amount
                                               from,
                                               to,
                                               by,
                                               0,
                                               "label",
                                               "message",
                                               true, // isSuccess
                                               i % 2 == 0, // isPending
                                               i,
                                               i % 2 != 0, // isAdminOperation
                                               System.currentTimeMillis() + offsetTime);
      transactionEntities.add(tx);
    }
    return transactionEntities;
  }

  protected TransactionEntity createTransaction(String hash,
                                                String contractAddress,
                                                String contractMethodName,
                                                double contractAmount,
                                                double value,
                                                String fromAddress,
                                                String toAddress,
                                                String byAddress,
                                                long issuerIdentityId,
                                                String label,
                                                String message,
                                                boolean isSuccess,
                                                boolean isPending,
                                                long nonce,
                                                boolean isAdminOperation,
                                                long createdDate) {

    if (StringUtils.isBlank(hash)) {
      hash = generateTransactionHash();
    }
    if (StringUtils.isBlank(fromAddress)) {
      fromAddress = "0x" + random.nextInt(1000);
    }
    if (createdDate == 0) {
      createdDate = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
    TransactionEntity transactionEntity = new TransactionEntity();
    transactionEntity.setNetworkId(WalletUtils.getNetworkId());
    transactionEntity.setHash(StringUtils.lowerCase(hash));
    transactionEntity.setContractAddress(StringUtils.lowerCase(contractAddress));
    transactionEntity.setContractMethodName(contractMethodName);
    transactionEntity.setContractAmount(contractAmount);
    transactionEntity.setValue(value);
    transactionEntity.setFromAddress(StringUtils.lowerCase(fromAddress));
    transactionEntity.setToAddress(StringUtils.lowerCase(toAddress));
    transactionEntity.setByAddress(StringUtils.lowerCase(byAddress));
    transactionEntity.setIssuerIdentityId(issuerIdentityId);
    transactionEntity.setLabel(label);
    transactionEntity.setMessage(message);
    transactionEntity.setSuccess(isSuccess);
    transactionEntity.setPending(isPending);
    transactionEntity.setAdminOperation(isAdminOperation);
    transactionEntity.setCreatedDate(createdDate);
    transactionEntity.setNonce(nonce);
    transactionEntity = walletTransactionDAO.create(transactionEntity);
    entitiesToClean.add(transactionEntity);
    return transactionEntity;
  }

  protected TransactionDetail createTransactionDetail(String hash,
                                                      String contractMethodName,
                                                      double contractAmount,
                                                      double value,
                                                      String fromAddress,
                                                      String toAddress,
                                                      String byAddress,
                                                      long issuerIdentityId,
                                                      String label,
                                                      String message,
                                                      boolean isSuccess,
                                                      boolean isPending,
                                                      boolean isAdminOperation,
                                                      String rawTransaction,
                                                      long createdDate) {

    if (StringUtils.isBlank(hash)) {
      hash = generateTransactionHash();
    }
    if (StringUtils.isBlank(fromAddress)) {
      fromAddress = "0x" + random.nextInt(1000);
    }
    if (createdDate == 0) {
      createdDate = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    TransactionStorage transactionStorage = getService(TransactionStorage.class);

    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setNetworkId(NETWORK_ID);
    transactionDetail.setHash(StringUtils.lowerCase(hash));
    transactionDetail.setContractAddress(WalletUtils.getContractAddress());
    transactionDetail.setContractMethodName(contractMethodName);
    transactionDetail.setContractAmount(contractAmount);
    transactionDetail.setValue(value);
    transactionDetail.setFrom(StringUtils.lowerCase(fromAddress));
    transactionDetail.setTo(StringUtils.lowerCase(toAddress));
    transactionDetail.setBy(StringUtils.lowerCase(byAddress));
    transactionDetail.setIssuerId(issuerIdentityId);
    transactionDetail.setLabel(label);
    transactionDetail.setMessage(message);
    transactionDetail.setSucceeded(isSuccess);
    transactionDetail.setPending(isPending);
    transactionDetail.setAdminOperation(isAdminOperation);
    transactionDetail.setTimestamp(createdDate);
    transactionDetail.setGasPrice(GAS_PRICE);
    transactionDetail.setGasUsed(GAS_USED);
    transactionDetail.setTokenFee(TOKEN_FEE);
    transactionDetail.setNonce(NONCE);
    transactionDetail.setNoContractFunds(true);
    transactionDetail.setRawTransaction(rawTransaction);
    transactionStorage.saveTransactionDetail(transactionDetail);
    entitiesToClean.add(transactionDetail);
    return transactionDetail;
  }

  protected String generateTransactionHash() {
    StringBuilder hashStringBuffer = new StringBuilder("0x");
    for (int i = 0; i < 64; i++) {
      hashStringBuffer.append(Integer.toHexString(random.nextInt(16)));
    }
    return hashStringBuffer.toString();
  }

  protected <T> T getService(Class<T> componentType) {
    return container.getComponentInstanceOfType(componentType);
  }

  protected org.exoplatform.services.security.Identity buildUserIdentityAsAdmin(String currentUser) {
    String group = "/platform/rewarding";
    MembershipEntry entry = new MembershipEntry(group, MembershipEntry.ANY_TYPE);
    Set<MembershipEntry> entryTest = new HashSet<>();
    entryTest.add(entry);
    return new org.exoplatform.services.security.Identity(currentUser, entryTest);
  }

  protected Wallet newWallet() {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(CURRENT_USER_IDENTITY_ID);
    wallet.setAddress(WALLET_ADDRESS_1);
    wallet.setPassPhrase(PHRASE);
    wallet.setEnabled(IS_ENABLED);
    wallet.setInitializationState(INITIALIZATION_STATE);
    return wallet;
  }

  protected Wallet newWalletSpace() {
    Wallet walletSpace = new Wallet();
    walletSpace.setTechnicalId(CURRENT_USER_IDENTITY_ID);
    walletSpace.setAddress(WALLET_ADDRESS_1);
    walletSpace.setPassPhrase(PHRASE);
    walletSpace.setEnabled(IS_ENABLED);
    walletSpace.setId(CURRENT_SPACE);
    walletSpace.setInitializationState(INITIALIZATION_STATE);
    walletSpace.setSpaceAdministrator(IS_ENABLED);
    walletSpace.setType(TYPE);
    return walletSpace;
  }

  protected void addCurrentUserWallet() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = newWallet();
    try {
      walletAccountService.saveWalletAddress(wallet, CURRENT_USER);
    } catch (IllegalAccessException e) {
      fail("User should be able to save his wallet");
    }
    entitiesToClean.add(wallet);
  }

  private void setContractDetails() {
    WalletService walletService = container.getComponentInstanceOfType(WalletService.class);
    ContractDetail contractDetail = new ContractDetail();
    contractDetail.setName("name");
    contractDetail.setSymbol("symbol");
    contractDetail.setDecimals(12);
    contractDetail.setAddress(WalletUtils.getContractAddress());
    contractDetail.setContractType("3");
    contractDetail.setNetworkId(1l);
    walletService.setConfiguredContractDetail(contractDetail);
  }

  protected org.web3j.protocol.core.methods.response.Log newLog() {
    ContractTransactionEvent contractTransactionEvent = newContractTransactionEvent();
    org.web3j.protocol.core.methods.response.Log log = mock(org.web3j.protocol.core.methods.response.Log.class);
    when(log.getTopics()).thenReturn(contractTransactionEvent.getTopics());
    when(log.getData()).thenReturn(contractTransactionEvent.getData());
    when(log.getBlockNumber()).thenReturn(BigInteger.valueOf(contractTransactionEvent.getBlockNumber()));
    when(log.getTransactionHash()).thenReturn(contractTransactionEvent.getTransactionHash());
    when(log.getAddress()).thenReturn(contractTransactionEvent.getContractAddress());
    return log;
  }

  protected void setRoot1AsAdmin() {
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);

    Identity identity = buildUserIdentityAsAdmin(CURRENT_USER);
    identityRegistry.register(identity);
  }

  protected ContractTransactionEvent newContractTransactionEvent() {
    String hash = "transactionHash";
    String contractAddress = "0x334d85047da64738c065d36e10b2adeb965000d0";
    String data = "0x0000000000000000000000000000000000000000000000001bc16d674ec80000";
    List<String> topics = Arrays.asList("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                                        "0x0000000000000000000000002b7e115f52171d164529fdb1ac72571e608a474e",
                                        "0x0000000000000000000000001d94f732223996e9f773261e82340889934a6c03");
    long blockNumber = 28047704;
    ContractTransactionEvent contractTransactionEvent = new ContractTransactionEvent(hash,
                                                                                     contractAddress,
                                                                                     data,
                                                                                     topics,
                                                                                     blockNumber);
    return contractTransactionEvent;
  }
}
