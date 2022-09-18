package org.exoplatform.wallet.service;

import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;

public class WalletAnalyticsProfileServiceTest {

  @Before
  public void setup() {
    PortalContainer.getInstance();
  }

  @Test
  public void testWalletAnalyticsDisabledByDefault() {
    assertFalse(RootContainer.getProfiles().contains("wallet-analytics"));
  }

}
