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
package org.exoplatform.wallet.job;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.wallet.service.WalletTokenAdminService;

@RunWith(MockitoJUnitRunner.class)
public class BoostAdminTransactionJobTest {

  @Mock
  WalletTokenAdminService  walletTokenAdminService;

  BoostAdminTransactionJob boostAdminTransactionJob;

  @Before
  public void setUp() {
    boostAdminTransactionJob = new BoostAdminTransactionJob();
    boostAdminTransactionJob.walletTokenAdminService = walletTokenAdminService;
  }

  @Test
  public void testRunJob() throws Exception {
    boostAdminTransactionJob.execute(null);

    verify(walletTokenAdminService, times(1)).boostAdminTransactions();
  }

  @Test
  public void testRunJobWithExceptionDoesntExit() throws Exception {
    doThrow(new RuntimeException("FAKE EXCEPTION")).when(walletTokenAdminService).boostAdminTransactions();

    boostAdminTransactionJob.execute(null);

    verify(walletTokenAdminService, times(1)).boostAdminTransactions();
  }

}
