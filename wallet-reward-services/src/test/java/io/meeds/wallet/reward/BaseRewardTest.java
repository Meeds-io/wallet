/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.reward;

import io.meeds.wallet.wallet.model.ContractDetail;
import io.meeds.wallet.wallet.service.WalletService;
import io.meeds.wallet.wallet.utils.WalletUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;

import io.meeds.kernel.test.AbstractSpringTest;
import io.meeds.kernel.test.KernelExtension;
import io.meeds.spring.AvailableIntegration;

@ExtendWith({ SpringExtension.class, KernelExtension.class })
@SpringBootApplication(scanBasePackages = {
        BaseRewardTest.MODULE_NAME,
        AvailableIntegration.KERNEL_TEST_MODULE,
        AvailableIntegration.LIQUIBASE_MODULE,
        AvailableIntegration.JPA_MODULE,
        AvailableIntegration.WEB_MODULE,
})
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-wallet-reward-configuration.xml"),
})
public abstract class BaseRewardTest extends AbstractSpringTest {

  public static final String MODULE_NAME      = "io.meeds.wallet";

  @BeforeEach
  public void setUp() {
    getContainer();
    setContractDetails();
    begin();
  }
  private void setContractDetails() {
    WalletService walletService = getContainer().getComponentInstanceOfType(WalletService.class);
    ContractDetail contractDetail = new ContractDetail();
    contractDetail.setName("name");
    contractDetail.setSymbol("symbol");
    contractDetail.setDecimals(12);
    contractDetail.setAddress(WalletUtils.getContractAddress());
    contractDetail.setContractType("3");
    contractDetail.setNetworkId(1L);
    walletService.setConfiguredContractDetail(contractDetail);
  }

}
