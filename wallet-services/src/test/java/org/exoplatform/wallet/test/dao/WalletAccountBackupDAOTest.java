package org.exoplatform.wallet.test.dao;

import org.exoplatform.wallet.dao.WalletAccountBackupDAO;
import org.exoplatform.wallet.dao.WalletAccountDAO;
import org.exoplatform.wallet.entity.WalletBackupEntity;
import org.exoplatform.wallet.entity.WalletEntity;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.test.BaseWalletTest;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.fail;

public class WalletAccountBackupDAOTest extends BaseWalletTest {


    /**
     * Check that service is instantiated and functional
     */
    @Test
    public void testServiceInstantiated() {
        WalletAccountBackupDAO walletAccountBackupDAO = getService(WalletAccountBackupDAO.class);
        assertNotNull(walletAccountBackupDAO);

        List<WalletBackupEntity> allBackupWallets = walletAccountBackupDAO.findAll();
        assertNotNull("Returned wallets list shouldn't be null", allBackupWallets);
        assertEquals("Returned wallets should be empty", 0, allBackupWallets.size());
    }

    /**
     * Check that DAO deny wallets massive deletion
     */
    @Test
    public void testWalletMassiveDeletionDeny() {
        WalletAccountBackupDAO walletAccountBackupDAO = getService(WalletAccountBackupDAO.class);
        try {
            walletAccountBackupDAO.deleteAll();
            fail("Shouldn't be able to delete all wallets");
        } catch (UnsupportedOperationException e) {
            // Expected
        }

        try {
            walletAccountBackupDAO.deleteAll(Collections.emptyList());
            fail("Shouldn't be able to delete multiple wallets in single operation");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    /**
     * Check DAO query that must return a backup wallet with wallet id
     */
    @Test
    public void testFindByWalletId() {
      WalletAccountBackupDAO walletAccountBackupDAO = getService(WalletAccountBackupDAO.class);
      WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
      WalletEntity walletEntity = new WalletEntity();

      String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
      walletEntity.setId(1L);
      walletEntity.setAddress(address);
      walletEntity.setPassPhrase("passphrase");
      walletEntity.setType(WalletType.USER);
      walletEntity.setProvider(WalletProvider.valueOf(PROVIDER));
      walletEntity = walletAccountDAO.create(walletEntity);
      entitiesToClean.add(walletEntity);

      WalletBackupEntity walletBackupEntity = new WalletBackupEntity();
      walletBackupEntity.setId(null);
      walletBackupEntity.setWallet(walletEntity);
      walletBackupEntity.setAddress(WALLET_ADDRESS_1);

      walletBackupEntity = walletAccountBackupDAO.create(walletBackupEntity);
      entitiesToClean.add(walletBackupEntity);

      WalletBackupEntity savedBackupWallet = walletAccountBackupDAO.findByWalletId(walletBackupEntity.getWallet().getId());

      assertNotNull(savedBackupWallet);
      assertEquals(savedBackupWallet.getAddress(), walletBackupEntity.getAddress());
    }

}
