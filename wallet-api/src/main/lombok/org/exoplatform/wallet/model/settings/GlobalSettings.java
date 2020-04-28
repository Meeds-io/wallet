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

import org.apache.commons.beanutils.BeanUtils;

import org.exoplatform.wallet.model.ContractDetail;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@NoArgsConstructor
public class GlobalSettings implements Serializable, Cloneable {

  private static final long    serialVersionUID = 8987967110410722896L;

  private NetworkSettings      network          = new NetworkSettings();

  private ContractDetail       contractDetail   = new ContractDetail();

  private InitialFundsSettings initialFunds     = null;

  private String               contractAddress  = null;

  private String               accessPermission = null;

  private boolean              enabled;

  @Exclude
  @ToString.Exclude
  private String               contractAbi      = null;                 // NOSONAR

  @Exclude
  @ToString.Exclude
  private String               contractBin      = null;

  public GlobalSettings(GlobalSettings globalSettings) {
    if (globalSettings != null) {
      try {
        BeanUtils.copyProperties(this, globalSettings);
      } catch (Exception e) {
        throw new IllegalStateException("Error while cloning attributes of global settings to current instance", e);
      }
    }
  }

  public GlobalSettings clone() { // NOSONAR
    try {
      return (GlobalSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }

}
