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
package org.exoplatform.wallet.test.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import org.exoplatform.wallet.dao.WalletAccountDAO;
import org.exoplatform.wallet.dao.WalletBlockchainStateDAO;
import org.exoplatform.wallet.entity.WalletBlockchainStateEntity;
import org.exoplatform.wallet.entity.WalletEntity;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.test.BaseWalletTest;

public class WalletBlockchainStateDAOTest extends BaseWalletTest {

  /**
   * Check that service is instantiated and functional
   */
  @Test
  public void testServiceInstantiated() {
    WalletBlockchainStateDAO walletBlockchainStateDAO = getService(WalletBlockchainStateDAO.class);

    List<WalletBlockchainStateEntity> allStates = walletBlockchainStateDAO.findAll();
    assertNotNull("Returned states shouldn't be null", allStates);
    assertEquals("Returned states list should be empty", 0, allStates.size());
  }

  /**
   * Test get list of transactions of a chosen contract
   */
  @Test
  public void testGetContractTransactions() {
    WalletAccountDAO walletAccountDAO = getService(WalletAccountDAO.class);
    WalletBlockchainStateDAO walletBlockchainStateDAO = getService(WalletBlockchainStateDAO.class);

    String contractAddress = "0xe9dfec7864af9e581a85ce3987d026be0f509ac9";

    WalletEntity walletEntity = new WalletEntity();

    String address = "0xc76987D43b77C45d51653b6eB110b9174aCCE8fb";
    walletEntity.setId(1L);
    walletEntity.setAddress(address);
    walletEntity.setPassPhrase("passphrase");
    walletEntity.setType(WalletType.USER);
    walletEntity = walletAccountDAO.create(walletEntity);
    entitiesToClean.add(walletEntity);

    WalletBlockchainStateEntity blockchainStateEntity = new WalletBlockchainStateEntity();
    blockchainStateEntity.setContractAddress(contractAddress);
    blockchainStateEntity.setWallet(walletEntity);
    WalletBlockchainStateEntity blockchainStateEntity1 = walletBlockchainStateDAO.create(blockchainStateEntity);
    entitiesToClean.add(blockchainStateEntity1);

    blockchainStateEntity = new WalletBlockchainStateEntity();
    blockchainStateEntity.setContractAddress(contractAddress);
    blockchainStateEntity.setWallet(walletEntity);
    WalletBlockchainStateEntity blockchainStateEntity2 = walletBlockchainStateDAO.create(blockchainStateEntity);
    entitiesToClean.add(blockchainStateEntity2);

    blockchainStateEntity = walletBlockchainStateDAO.findByWalletIdAndContract(walletEntity.getId(), contractAddress);
    assertNotNull("Can't find wallet state", blockchainStateEntity);
    assertEquals(blockchainStateEntity2.getId(), blockchainStateEntity.getId());
  }

}
