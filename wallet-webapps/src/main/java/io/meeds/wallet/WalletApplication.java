/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
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
 *
 */
package io.meeds.wallet;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.PropertySource;

import io.meeds.spring.AvailableIntegration;
import io.meeds.spring.kernel.PortalApplicationContextInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = { 
  WalletApplication.MODULE_NAME,
  AvailableIntegration.KERNEL_MODULE,
  AvailableIntegration.JPA_MODULE, 
  AvailableIntegration.WEB_MODULE, 
}, 
exclude = {
  LiquibaseAutoConfiguration.class,
})
@EnableJpaRepositories(basePackages = WalletApplication.MODULE_NAME)
@PropertySource("classpath:application.properties")
@PropertySource("classpath:application-common.properties")
public class WalletApplication extends PortalApplicationContextInitializer {

  public static final String MODULE_NAME = "io.meeds.wallet";

}
