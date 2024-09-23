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
package io.meeds.wallet.reward;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.meeds.wallet.reward.dao.RewardDAO;
import io.meeds.wallet.reward.dao.RewardPeriodDAO;
import io.meeds.wallet.reward.service.RewardSettingsService;
import io.meeds.wallet.reward.service.WalletRewardSettingsService;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;

import org.exoplatform.commons.utils.MapResourceBundle;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import io.meeds.wallet.dao.AddressLabelDAO;
import io.meeds.wallet.dao.WalletAccountDAO;
import io.meeds.wallet.dao.WalletBlockchainStateDAO;
import io.meeds.wallet.dao.WalletPrivateKeyDAO;
import io.meeds.wallet.dao.WalletTransactionDAO;
import io.meeds.wallet.entity.AddressLabelEntity;
import io.meeds.wallet.entity.TransactionEntity;
import io.meeds.wallet.entity.WalletEntity;
import io.meeds.wallet.entity.WalletPrivateKeyEntity;
import io.meeds.wallet.wallet.model.ContractDetail;
import io.meeds.wallet.wallet.model.Wallet;
import io.meeds.wallet.wallet.model.WalletAddressLabel;
import io.meeds.wallet.wallet.model.WalletProvider;
import io.meeds.wallet.wallet.model.WalletState;
import io.meeds.wallet.wallet.model.reward.RewardBudgetType;
import io.meeds.wallet.wallet.model.reward.RewardSettings;
import io.meeds.wallet.wallet.model.reward.RewardTeam;
import io.meeds.wallet.wallet.model.reward.RewardTeamMember;
import io.meeds.wallet.wallet.model.transaction.TransactionDetail;
import io.meeds.wallet.wallet.service.WalletService;
import io.meeds.wallet.wallet.utils.RewardUtils;
import io.meeds.wallet.wallet.utils.WalletUtils;

public abstract class BaseWalletRewardTest extends AbstractKernelTest {

    private static final Log                LOG                  = ExoLogger.getLogger(BaseWalletRewardTest.class);

    protected static final long             IDENTITY_ID          = 1L;

    protected static final String           ADDRESS              = "walletAddress";

    protected static final String           PHRASE               = "passphrase";

    protected static final String           INITIALIZATION_STATE = WalletState.INITIALIZED.name();

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

    protected static final String           PROVIDER             = WalletProvider.INTERNAL_WALLET.name();

    protected PortalContainer               container;

    private static RewardSettings           defaultSettings      = null;

    public static Map<Long, Double> getEarnedPoints(Set<Long> identityIds) {
        return identityIds.stream().collect(Collectors.toMap(Function.identity(), x -> x.doubleValue()));
    }

    private Random               random          = new Random(1);

    protected List<Serializable> entitiesToClean = new ArrayList<>();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (container == null) {
            assertNotNull("Container shouldn't be null", getContainer());
            assertTrue("Container should have been started", getContainer().isStarted());
            IdentityManager identityManager = getContainer().getComponentInstanceOfType(IdentityManager.class);
            SpaceService spaceService = getContainer().getComponentInstanceOfType(SpaceService.class);

            container = getContainer();
            registerResourceBundleService();

            RewardSettingsService rewardSettingsService = getService(RewardSettingsService.class);
            defaultSettings = rewardSettingsService.getSettings();
            setContractDetails();
        }
        begin();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        RewardDAO rewardDAO = getService(RewardDAO.class);
        RewardPeriodDAO rewardPeriodDAO = getService(RewardPeriodDAO.class);
        WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
        AddressLabelDAO addressLabelDAO = getService(AddressLabelDAO.class);
        WalletPrivateKeyDAO walletPrivateKeyDAO = getService(WalletPrivateKeyDAO.class);
        WalletTransactionDAO walletTransactionDAO = getService(WalletTransactionDAO.class);
        WalletBlockchainStateDAO walletBlockchainStateDAO = getService(WalletBlockchainStateDAO.class);
        WalletRewardSettingsService rewardSettingsService = getService(WalletRewardSettingsService.class);

        RewardSettings storedSettings = rewardSettingsService.getSettings();
        assertEquals(defaultSettings, storedSettings);

        restartTransaction();

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

        end();
    }

    protected <T> T getService(Class<T> componentType) {
        return container.getComponentInstanceOfType(componentType);
    }

    protected RewardTeam newRewardTeam() {
        long[] memberIdentityIds = {
                MEMBER_IDENTITY_ID
        };

        return newRewardTeam(memberIdentityIds);
    }

    protected RewardTeam newRewardTeam(long[] memberIdentityIds) {
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
        wallet.setIsInitialized(true);
        wallet.setEtherBalance(0d);
        wallet.setTokenBalance(0d);
        wallet.setInitializationState(INITIALIZATION_STATE);
        return wallet;
    }

    protected void updateWalletBlockchainState(Wallet wallet) {
        wallet.setIsInitialized(true);
        wallet.setEtherBalance(0d);
        wallet.setTokenBalance(0d);
        wallet.setInitializationState(INITIALIZATION_STATE);
    }

    protected String generateTransactionHash() {
        StringBuilder hashStringBuffer = new StringBuilder("0x");
        for (int i = 0; i < 64; i++) {
            hashStringBuffer.append(Integer.toHexString(random.nextInt(16)));
        }
        return hashStringBuffer.toString();
    }

    private void registerResourceBundleService() {
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

    private void setContractDetails() {
        WalletService walletService = getService(WalletService.class);
        ContractDetail contractDetail = new ContractDetail();
        contractDetail.setName("name");
        contractDetail.setSymbol("symbol");
        contractDetail.setDecimals(12);
        contractDetail.setAddress(WalletUtils.getContractAddress());
        contractDetail.setContractType("3");
        contractDetail.setNetworkId(1l);
        walletService.setConfiguredContractDetail(contractDetail);
    }
}