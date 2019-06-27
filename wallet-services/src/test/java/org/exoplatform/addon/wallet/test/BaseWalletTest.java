package org.exoplatform.addon.wallet.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;

import org.exoplatform.container.PortalContainer;

public class BaseWalletTest {

  protected static PortalContainer container;

  @BeforeClass
  public static void beforeTest() {
    container = PortalContainer.getInstance();
    assertNotNull("Container shouldn't be null", container);
    assertTrue("Container should have been started", container.isStarted());
  }

  public <T> T getService(Class<T> componentType) {
    return container.getComponentInstanceOfType(componentType);
  }
}
