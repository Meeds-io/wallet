package org.exoplatform.addon.wallet.test.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.exoplatform.addon.wallet.model.Wallet;
import org.exoplatform.addon.wallet.service.WalletAccountService;
import org.exoplatform.addon.wallet.test.BaseWalletTest;
import org.exoplatform.services.listener.ListenerService;
import org.junit.Test;

public class WalletAccountServiceTest extends BaseWalletTest {

  @Test
  public void testContainerStart() {
    // assertNotNull(getService(WalletTokenAdminService.class));
    assertNotNull(getService(ListenerService.class));
    assertNotNull(getService(WalletAccountService.class));
  }

  /**
   * Test if default settings are injected in walletAcountService
   */
  @Test
  public void testDefaultParameters() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);

  }

  /**
   * Test get wallet count
   */
  @Test
  public void testgetWalletsCount() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    long walletCount = walletAccountService.getWalletsCount();
    assertNotNull("Wallet count shouldn't be null", walletCount);
  }

  /**
   * Test save wallet
   */
  @Test
  public void testsaveWallet() {
    WalletAccountService walletAccountService = getService(WalletAccountService.class);
    Wallet wallet = new Wallet();
    String id = "wallet0";
    String name = "user";
    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    String type = "walletAccount";
    String initializationState = "NEW";
    String passPhrase = "Save wallet";
    int technicalId = 1;
    wallet.setId(id);
    wallet.setType(type);
    wallet.setAddress(address);
    wallet.setTechnicalId(technicalId);
    wallet.setName(name);
    wallet.setInitializationState(initializationState);
    wallet.setPassPhrase(passPhrase);
    walletAccountService.saveWallet(wallet);

    assertEquals("Wallet id shouldn't be null", id, walletAccountService.getWalletByAddress(address).getId());
    assertEquals("Wallet name shouldn't be null", name, walletAccountService.getWalletByAddress(address).getName());
    assertEquals("Wallet type shouldn't be null", type, walletAccountService.getWalletByAddress(address).getType());

  }

}
