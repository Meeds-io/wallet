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
package org.exoplatform.wallet.service;

import java.util.*;

import org.exoplatform.application.registry.*;
import org.exoplatform.commons.cluster.StartableClusterAware;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.ApplicationType;

/**
 * This Service installs application registry category and application for
 * SpaceWallet portlet if the application registry was already populated
 */
public class ApplicationRegistrySetupService implements StartableClusterAware {
  private static final String        SPACE_WALLET_APP_ID       = "wallet/SpaceWallet";

  private static final String        SPACE_WALLET_PORTLET_NAME = "SpaceWallet";

  private static final List<String>  EVERYONE_PERMISSION_LIST  = Collections.singletonList("Everyone");

  private static final String        OLD_WALLET_CATEGORY_NAME  = "EthereumWallet";

  private static final String        WALLET_CATEGORY_NAME      = "Wallet";

  private ExoContainer               container;

  private ApplicationRegistryService applicationRegistryService;

  private boolean                    isDone;

  public ApplicationRegistrySetupService(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void start() {
    RequestLifeCycle.begin(container);
    try {
      ApplicationCategory oldCategory = getApplicationRegistryService().getApplicationCategory(OLD_WALLET_CATEGORY_NAME);
      if (oldCategory != null) {
        getApplicationRegistryService().remove(oldCategory);
      }

      ApplicationCategory walletCategory = getApplicationRegistryService().getApplicationCategory(WALLET_CATEGORY_NAME);
      if (walletCategory == null) {
        walletCategory = new ApplicationCategory();
        walletCategory.setAccessPermissions(EVERYONE_PERMISSION_LIST);
        walletCategory.setName(WALLET_CATEGORY_NAME);
        walletCategory.setDescription("Wallet applications");
        walletCategory.setDisplayName(WALLET_CATEGORY_NAME);

        Application application = new Application();
        walletCategory.setApplications(Collections.singletonList(application));
        application.setAccessPermissions(new ArrayList<String>(EVERYONE_PERMISSION_LIST));
        application.setDisplayName("Space Wallet");
        application.setDescription("Space Wallet");
        application.setApplicationName(SPACE_WALLET_PORTLET_NAME);
        application.setCategoryName(WALLET_CATEGORY_NAME);
        application.setContentId(SPACE_WALLET_APP_ID);
        application.setType(ApplicationType.PORTLET);

        getApplicationRegistryService().save(walletCategory);
        getApplicationRegistryService().save(walletCategory, application);
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

  public ApplicationRegistryService getApplicationRegistryService() {
    if (applicationRegistryService == null) {
      applicationRegistryService = CommonsUtils.getService(ApplicationRegistryService.class);
    }
    return applicationRegistryService;
  }
}
