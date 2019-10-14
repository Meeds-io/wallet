package org.exoplatform.wallet.reward.test;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.*;
import org.mockito.Mockito;

import org.exoplatform.commons.utils.MapResourceBundle;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.wallet.dao.*;
import org.exoplatform.wallet.entity.*;
import org.exoplatform.wallet.model.*;
import org.exoplatform.wallet.model.reward.*;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.api.RewardPlugin;
import org.exoplatform.wallet.reward.dao.*;
import org.exoplatform.wallet.reward.entity.RewardTeamEntity;
import org.exoplatform.wallet.reward.service.WalletRewardSettingsService;
import org.exoplatform.wallet.reward.test.service.WalletRewardSettingsServiceTest;
import org.exoplatform.wallet.service.WalletService;
import org.exoplatform.wallet.utils.RewardUtils;
import org.exoplatform.wallet.utils.WalletUtils;

public abstract class BaseWalletRewardTest {

  protected static PortalContainer        container;

  private static final Log                LOG                  = ExoLogger.getLogger(BaseWalletRewardTest.class);

  protected static final long             IDENTITY_ID          = 1L;

  protected static final String           ADDRESS              = "walletAddress";

  protected static final String           PHRASE               = "passphrase";

  protected static final String           INITIALIZATION_STATE = WalletInitializationState.INITIALIZED.name();

  protected static final boolean          IS_ENABLED           = true;

  protected static final String           CURRENT_USER         = "root1";

  protected static final long             MANAGER_IDENTITY_ID  = IDENTITY_ID;

  protected static final long             MEMBER_IDENTITY_ID   = 2l;

  protected static final long             SPACE_ID             = 5l;

  protected static final String           TEAM_NAME            = "name";

  protected static final String           TEAM_DESCRIPTION     = "Team description";

  protected static final double           TEAM_BUDGET          = 2d;

  protected static final RewardBudgetType TEAM_BUDGET_TYPE     = RewardBudgetType.COMPUTED;

  protected static final String           CUSTOM_PLUGIN_NAME   = "plugin name";

  protected static final String           CUSTOM_PLUGIN_ID     = "custom";

  protected static final RewardPlugin     CUSTOM_REWARD_PLUGIN = new RewardPlugin() {
                                                                 @Override
                                                                 public String getName() {
                                                                   return CUSTOM_PLUGIN_NAME;
                                                                 }

                                                                 @Override
                                                                 public String getPluginId() {
                                                                   return CUSTOM_PLUGIN_ID;
                                                                 }

                                                                 @Override
                                                                 public Map<Long, Double> getEarnedPoints(Set<Long> identityIds,
                                                                                                          long startDateInSeconds,
                                                                                                          long endDateInSeconds) {
                                                                   return WalletRewardSettingsServiceTest.getEarnedPoints(identityIds);
                                                                 }
                                                               };

  private static RewardSettings           defaultSettings      = null;

  public static Map<Long, Double> getEarnedPoints(Set<Long> identityIds) {
    return identityIds.stream().collect(Collectors.toMap(Function.identity(), x -> x.doubleValue()));
  }

  private Random               random          = new Random(1);

  protected List<Serializable> entitiesToClean = new ArrayList<>();

  @BeforeClass
  public static void beforeTest() {
    container = PortalContainer.getInstance();
    assertNotNull("Container shouldn't be null", container);
    assertTrue("Container should have been started", container.isStarted());

    ExoContainerContext.setCurrentContainer(container);
    registerResourceBundleService();

    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);
    defaultSettings = rewardSettingsService.getSettings();
    setContractDetails();
  }

  @Before
  public void beforeMethodTest() {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
  }

  @After
  public void afterMethodTest() {
    RewardDAO rewardDAO = getService(RewardDAO.class);
    RewardPeriodDAO rewardPeriodDAO = getService(RewardPeriodDAO.class);
    RewardPluginDAO rewardPluginDAO = getService(RewardPluginDAO.class);
    WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
    AddressLabelDAO addressLabelDAO = getService(AddressLabelDAO.class);
    WalletPrivateKeyDAO walletPrivateKeyDAO = getService(WalletPrivateKeyDAO.class);
    WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
    RewardTeamDAO rewardTeamDAO = getService(RewardTeamDAO.class);
    WalletBlockchainStateDAO walletBlockchainStateDAO = getService(WalletBlockchainStateDAO.class);
    WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);

    RewardSettings storedSettings = rewardSettingsService.getSettings();
    assertEquals(defaultSettings, storedSettings);

    RequestLifeCycle.end();

    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);

    rewardPluginDAO.deleteAll();
    rewardDAO.deleteAll();
    rewardPeriodDAO.deleteAll();
    walletBlockchainStateDAO.deleteAll();

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
        } else if (entity instanceof RewardTeamEntity) {
          rewardTeamDAO.delete((RewardTeamEntity) entity);
        } else if (entity instanceof RewardTeam) {
          RewardTeamEntity teamEntity = rewardTeamDAO.find(((RewardTeam) entity).getId());
          rewardTeamDAO.delete(teamEntity);
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

  protected static <T> T getService(Class<T> componentType) {
    return container.getComponentInstanceOfType(componentType);
  }

  protected static void restartTransaction() {
    RequestLifeCycle.end();
    RequestLifeCycle.begin(container);
  }

  protected static RewardTeam newRewardTeam() {
    long[] memberIdentityIds = { MEMBER_IDENTITY_ID };

    return newRewardTeam(memberIdentityIds);
  }

  protected static RewardTeam newRewardTeam(long[] memberIdentityIds) {
    RewardTeam rewardTeam = new RewardTeam();
    rewardTeam.setBudget(TEAM_BUDGET);
    rewardTeam.setDescription(TEAM_DESCRIPTION);
    rewardTeam.setDisabled(false);

    RewardTeamMember manager = new RewardTeamMember();
    manager.setIdentityId(MANAGER_IDENTITY_ID);
    rewardTeam.setManager(manager);

    List<RewardTeamMember> members = new ArrayList<>();
    for (long memberIdentityId : memberIdentityIds) {
      RewardTeamMember member = new RewardTeamMember();
      member.setIdentityId(memberIdentityId);
      members.add(member);
    }
    rewardTeam.setMembers(members);

    rewardTeam.setName(TEAM_NAME);
    rewardTeam.setRewardType(TEAM_BUDGET_TYPE);
    rewardTeam.setSpaceId(SPACE_ID);
    return rewardTeam;
  }

  protected Wallet newWallet(long identityId) {
    Wallet wallet = new Wallet();
    wallet.setTechnicalId(identityId);
    wallet.setAddress(ADDRESS + identityId);
    wallet.setPassPhrase(PHRASE);
    wallet.setEnabled(IS_ENABLED);
    wallet.setIsApproved(true);
    wallet.setIsInitialized(true);
    wallet.setAdminLevel(0);
    wallet.setEtherBalance(0d);
    wallet.setTokenBalance(0d);
    wallet.setRewardBalance(0d);
    wallet.setVestingBalance(0d);
    wallet.setInitializationState(INITIALIZATION_STATE);
    return wallet;
  }

  protected void updateWalletBlockchainState(Wallet wallet) {
    wallet.setIsApproved(true);
    wallet.setIsInitialized(true);
    wallet.setAdminLevel(0);
    wallet.setEtherBalance(0d);
    wallet.setTokenBalance(0d);
    wallet.setRewardBalance(0d);
    wallet.setVestingBalance(0d);
    wallet.setInitializationState(INITIALIZATION_STATE);
  }

  protected String generateTransactionHash() {
    StringBuilder hashStringBuffer = new StringBuilder("0x");
    for (int i = 0; i < 64; i++) {
      hashStringBuffer.append(Integer.toHexString(random.nextInt(16)));
    }
    return hashStringBuffer.toString();
  }

  protected RewardSettings cloneSettings(RewardSettings defaultSettings) {
    RewardSettings newSettings = defaultSettings.clone();
    HashSet<RewardPluginSettings> defaultPluginSettings = new HashSet<>(newSettings.getPluginSettings());
    Set<RewardPluginSettings> pluginSettings = new HashSet<>();
    for (RewardPluginSettings rewardPluginSetting : defaultPluginSettings) {
      pluginSettings.add(rewardPluginSetting.clone());
    }
    newSettings.setPluginSettings(pluginSettings);
    return newSettings;
  }

  private static void registerResourceBundleService() {
    ResourceBundleService resourceBundleService = Mockito.mock(ResourceBundleService.class);
    MapResourceBundle resourceBundle = new MapResourceBundle(Locale.getDefault());
    resourceBundle.add(RewardUtils.REWARD_TRANSACTION_LABEL_KEY, "{0} is rewarded {1} {2} for period: {3} to {4}");
    resourceBundle.add(RewardUtils.REWARD_TRANSACTION_WITH_POOL_MESSAGE_KEY,
                       "You have earned {0} {1} in reward for your {2} {3} earned in pool {4} for period: {5} to {6}");
    resourceBundle.add(RewardUtils.REWARD_TRANSACTION_NO_POOL_MESSAGE_KEY,
                       "You have earned {0} {1} in reward for your {2} {3} for period: {4} to {5}");
    Mockito.when(resourceBundleService.getResourceBundle(Mockito.anyString(), Mockito.any())).thenReturn(resourceBundle);
    if (container.getComponentInstanceOfType(ResourceBundleService.class) != null) {
      container.unregisterComponent(ResourceBundleService.class);
    }
    container.registerComponentInstance(ResourceBundleService.class, resourceBundleService);
  }

  private static void setContractDetails() {
    WalletService walletService = getService(WalletService.class);
    ContractDetail contractDetail = new ContractDetail();
    contractDetail.setName("name");
    contractDetail.setSymbol("symbol");
    contractDetail.setDecimals(12);
    contractDetail.setAddress(WalletUtils.getContractAddress());
    contractDetail.setContractType("3");
    contractDetail.setNetworkId(1l);
    contractDetail.setSellPrice("0.002");
    walletService.setConfiguredContractDetail(contractDetail);
  }
}
