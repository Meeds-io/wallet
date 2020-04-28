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
package org.exoplatform.wallet.model;

import java.io.Serializable;

import lombok.*;
import lombok.EqualsAndHashCode.Exclude;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetail implements Serializable, Cloneable {

  private static final long serialVersionUID = 1459881604949041768L;

  private String            address;

  @Exclude
  private String            name;

  @Exclude
  private String            symbol;

  @Exclude
  private Integer           decimals;

  @Exclude
  private Long              networkId;

  @Exclude
  private String            owner;

  @Exclude
  private String            sellPrice;

  @Exclude
  private String            totalSupply;

  @Exclude
  private String            contractType;

  @Exclude
  private Boolean           isPaused;

  @Exclude
  private Double            etherBalance;

  public ContractDetail clone() { // NOSONAR
    try {
      return (ContractDetail) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException("Error while cloning object: " + this, e);
    }
  }
}
