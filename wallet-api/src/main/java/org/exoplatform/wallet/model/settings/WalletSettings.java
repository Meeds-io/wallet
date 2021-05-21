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
package org.exoplatform.wallet.model.settings;

import java.io.Serializable;
import java.util.Set;

import org.exoplatform.wallet.model.WalletAddressLabel;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@NoArgsConstructor
@ToString
public class WalletSettings implements Serializable, Cloneable {

  private static final long       serialVersionUID = -5725443183560646198L;

  @Exclude
  @ToString.Exclude
  private Integer                 dataVersion      = 0;

  private String                  walletAddress    = null;

  @Exclude
  @ToString.Exclude
  private String                  phrase           = null;

  @Exclude
  @ToString.Exclude
  private boolean                 hasKeyOnServerSide;

  @Exclude
  @ToString.Exclude
  private Set<WalletAddressLabel> addresesLabels;

  public WalletSettings clone() { // NOSONAR
    try {
      return (WalletSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
