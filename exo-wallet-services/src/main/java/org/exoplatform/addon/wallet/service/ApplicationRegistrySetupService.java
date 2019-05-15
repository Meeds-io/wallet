/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.wallet.service;

import java.util.*;

import org.exoplatform.application.registry.*;
import org.exoplatform.commons.cluster.StartableClusterAware;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.ApplicationType;

/**
 * This Service installs application registry category and application for
 * EthereumSpaceWallet portlet if the application registry was already populated
 */
public class ApplicationRegistrySetupService implements StartableClusterAware {

  private static final List<String>  EVERYONE_PERMISSION_LIST = Collections.singletonList("Everyone");

  private static final String        WALLET_CATEGORY_NAME     = "EthereumWallet";

  private ExoContainer               container;

  private ApplicationRegistryService applicationRegistryService;

  private boolean                    isDone;

  public ApplicationRegistrySetupService(PortalContainer container, ApplicationRegistryService applicationRegistryService) {
    this.container = container;
    this.applicationRegistryService = applicationRegistryService;
  }

  @Override
  public void start() {
    RequestLifeCycle.begin(container);
    try {
      ApplicationCategory applicationCategory = applicationRegistryService.getApplicationCategory(WALLET_CATEGORY_NAME);
      if (applicationCategory == null) {
        applicationCategory = new ApplicationCategory();
        applicationCategory.setAccessPermissions(EVERYONE_PERMISSION_LIST);
        applicationCategory.setName(WALLET_CATEGORY_NAME);
        applicationCategory.setDescription("Ethereum Wallet");
        applicationCategory.setDisplayName("Ethereum Wallet");
        Application application = new Application();
        applicationCategory.setApplications(Collections.singletonList(application));
        application.setAccessPermissions(new ArrayList<String>(EVERYONE_PERMISSION_LIST));
        application.setDisplayName("Ethereum Space Wallet");
        application.setDescription("Ethereum Space Wallet");
        application.setApplicationName("EthereumSpaceWallet");
        application.setCategoryName(WALLET_CATEGORY_NAME);
        application.setContentId("exo-ethereum-wallet/EthereumSpaceWallet");
        application.setType(ApplicationType.PORTLET);

        applicationRegistryService.save(applicationCategory);
        applicationRegistryService.save(applicationCategory, application);
      }
      this.isDone = true;
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
    // Nothing to shutdown
  }

  @Override
  public boolean isDone() {
    return isDone;
  }
}
