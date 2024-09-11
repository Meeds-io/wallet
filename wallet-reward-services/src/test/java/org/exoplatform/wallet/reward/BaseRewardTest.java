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
package org.exoplatform.wallet.reward;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = {
        "spring.liquibase.change-log=" + BaseRewardTest.CHANGELOG_PATH,
})
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-wallet-reward-configuration.xml"),
})
public abstract class BaseRewardTest extends AbstractSpringTest {

  public static final String MODULE_NAME      = "io.meeds.kudos";

  public static final String CHANGELOG_PATH   = "classpath:db/changelog/reward-rdbms.db.changelog-master.xml";

  @BeforeEach
  public void setUp() {
    getContainer();
    begin();
  }

}
