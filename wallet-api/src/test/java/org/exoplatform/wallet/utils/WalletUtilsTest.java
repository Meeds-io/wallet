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
package org.exoplatform.wallet.utils;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class WalletUtilsTest {

  @Test
  public void testFormatBalance() {
    assertEquals("0", WalletUtils.formatBalance(0.0000001d, Locale.FRENCH, true));
    assertEquals("0", WalletUtils.formatBalance(0.0000001d, Locale.ENGLISH, true));
    assertEquals("0,01", WalletUtils.formatBalance(0.01d, Locale.FRENCH, true));
    assertEquals("0.1", WalletUtils.formatBalance(0.1d, Locale.ENGLISH, true));
    assertEquals("2 222 222 222 222", WalletUtils.formatBalance(2222222222222.0000001d, Locale.FRENCH, true));
    assertEquals("2,222,222,222,222", WalletUtils.formatBalance(2222222222222.0000001d, Locale.ENGLISH, true));
    assertEquals("222 222 222", WalletUtils.formatBalance(222222222.0500001d, Locale.FRENCH, true));
    assertEquals("222,222,222", WalletUtils.formatBalance(222222222.5000001d, Locale.ENGLISH, true));
    assertEquals("222 222 222,05", WalletUtils.formatBalance(222222222.0500001d, Locale.FRENCH, false));
    assertEquals("222,222,222.5", WalletUtils.formatBalance(222222222.5000001d, Locale.ENGLISH, false));
  }

}
