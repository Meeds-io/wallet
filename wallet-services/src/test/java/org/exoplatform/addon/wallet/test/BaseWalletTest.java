package org.exoplatform.addon.wallet.test;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import org.exoplatform.addon.wallet.dao.*;
import org.exoplatform.addon.wallet.entity.*;
import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.model.WalletAddressLabel;
import org.exoplatform.addon.wallet.model.transaction.TransactionDetail;
import org.exoplatform.addon.wallet.storage.TransactionStorage;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class BaseWalletTest {

  protected static PortalContainer container;

  private static final Log         LOG             = ExoLogger.getLogger(BaseWalletTest.class);

  protected List<Serializable>     entitiesToClean = new ArrayList<>();

  private Random                   randon          = new Random(1);

  @BeforeClass
  public static void beforeTest() {
    container = PortalContainer.getInstance();
    assertNotNull("Container shouldn't be null", container);
    assertTrue("Container should have been started", container.isStarted());
  }

  @Before
  public void beforeMethodTest() {
    RequestLifeCycle.begin(container);
  }

  @After
  public void afterMethodTest() {
    WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
    AddressLabelDAO addressLabelDAO = getService(AddressLabelDAO.class);
    WalletPrivateKeyDAO walletPrivateKeyDAO = getService(WalletPrivateKeyDAO.class);
    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);

    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);

    if (!entitiesToClean.isEmpty()) {
      for (Serializable entity : entitiesToClean) {
        if (entity instanceof WalletEntity) {
          walletAccountDAO.delete((WalletEntity) entity);
        } else if (entity instanceof WalletPrivateKeyEntity) {
          walletPrivateKeyDAO.delete((WalletPrivateKeyEntity) entity);
        } else if (entity instanceof TransactionEntity) {
          walletTransactionDAO.delete((TransactionEntity) entity);
        } else if (entity instanceof AddressLabelEntity) {
          addressLabelDAO.delete((AddressLabelEntity) entity);
        } else if (entity instanceof WalletAddressLabel) {
          AddressLabelEntity labelEntity = addressLabelDAO.find(((WalletAddressLabel) entity).getId());
          addressLabelDAO.delete(labelEntity);
        } else if (entity instanceof TransactionDetail) {
          TransactionEntity transactionEntity = walletTransactionDAO.find(((TransactionDetail) entity).getId());
          walletTransactionDAO.delete(transactionEntity);
        } else if (entity instanceof Wallet) {
          long walletId = ((Wallet) entity).getTechnicalId();
          WalletEntity walletEntity = walletAccountDAO.find(walletId);
          walletAccountDAO.delete(walletEntity);

          WalletPrivateKeyEntity walletPrivateKey = walletPrivateKeyDAO.find(walletId);
          if (walletPrivateKey != null) {
            walletPrivateKeyDAO.delete(walletPrivateKey);
          }
        } else {
          throw new IllegalStateException("Entity not managed" + entity);
        }
      }
    }

    int walletCount = walletAccountDAO.findAll().size();
    int walletPrivateKeyCount = walletPrivateKeyDAO.findAll().size();
    int walletAddressLabelsCount = addressLabelDAO.findAll().size();
    int walletTransactionsCount = walletTransactionDAO.findAll().size();

    LOG.info("objects count wallets = {}, private keys = {}, address labels = {}, transactions count = {}",
             walletCount,
             walletPrivateKeyCount,
             walletAddressLabelsCount,
             walletTransactionsCount);
    assertEquals("The previous test didn't cleaned wallets entities correctly, should add entities to clean into 'entitiesToClean' list.",
                 0,
                 walletCount);
    assertEquals("The previous test didn't cleaned wallet addresses labels correctly, should add entities to clean into 'entitiesToClean' list.",
                 0,
                 walletAddressLabelsCount);
    assertEquals("The previous test didn't cleaned wallets private keys entities correctly, should add entities to clean into 'entitiesToClean' list.",
                 0,
                 walletPrivateKeyCount);
    assertEquals("The previous test didn't cleaned wallets transactions entities correctly, should add entities to clean into 'entitiesToClean' list.",
                 0,
                 walletTransactionsCount);

    RequestLifeCycle.end();
  }

  protected List<TransactionEntity> generateTransactions(String walletAddress,
                                                         String contractAddress,
                                                         String contractMethodName) {
    String secondAddress = "0xeaaaec7864af9e581a85ce3987d026be0f509ac9";
    String thirdAddress = "0xeaaaec7864af9e581a85ce3987d026be0f509ac9";

    String otherContractAddress = "0xeeefec7864af9e581a85ce3987d026be0f509aaa";
    String otherContractMethodName = "transferFrom";

    List<TransactionEntity> transactionEntities = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      String contractAddressToUse = i % 2 == 0 ? contractAddress : otherContractAddress; // NOSONAR
      String contractMethodNameToUse = i % 3 == 0 ? contractMethodName : i % 3 == 1 ? otherContractMethodName : null; // NOSONAR

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
                                               i % 2 == 1, // isAdminOperation
                                               System.currentTimeMillis());
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
                                                boolean isAdminOperation,
                                                long createdDate) {

    if (StringUtils.isBlank(hash)) {
      hash = "0x" + randon.nextDouble();
    }
    if (StringUtils.isBlank(fromAddress)) {
      fromAddress = "0x" + randon.nextInt(1000);
    }
    if (createdDate == 0) {
      createdDate = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
    TransactionEntity transactionEntity = new TransactionEntity();
    transactionEntity.setNetworkId(1);
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
    transactionEntity = walletTransactionDAO.create(transactionEntity);
    entitiesToClean.add(transactionEntity);
    return transactionEntity;
  }

  protected TransactionDetail createTransactionDetail(String hash,
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
                                                      boolean isAdminOperation,
                                                      long createdDate) {

    if (StringUtils.isBlank(hash)) {
      hash = "0x" + randon.nextDouble();
    }
    if (StringUtils.isBlank(fromAddress)) {
      fromAddress = "0x" + randon.nextInt(1000);
    }
    if (createdDate == 0) {
      createdDate = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    TransactionStorage transactionStorage = getService(TransactionStorage.class);
    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setNetworkId(1);
    transactionDetail.setHash(StringUtils.lowerCase(hash));
    transactionDetail.setContractAddress(StringUtils.lowerCase(contractAddress));
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
    transactionStorage.saveTransactionDetail(transactionDetail);
    entitiesToClean.add(transactionDetail);
    return transactionDetail;
  }

  public <T> T getService(Class<T> componentType) {
    return container.getComponentInstanceOfType(componentType);
  }
}
