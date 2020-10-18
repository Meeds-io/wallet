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

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class NetworkSettings implements Serializable, Cloneable {

  private static final long serialVersionUID     = 2237799362889884389L;

  private long              id                   = 0L;

  private String            providerURL          = null;

  private String            websocketProviderURL = null;

  private Long              gasLimit             = 0L;

  private Long              minGasPrice          = 0L;

  private Long              normalGasPrice       = 0L;

  private Long              maxGasPrice          = 0L;

  public NetworkSettings clone() { // NOSONAR
    try {
      return (NetworkSettings) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
