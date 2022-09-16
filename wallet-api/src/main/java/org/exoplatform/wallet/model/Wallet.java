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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Wallet implements Serializable, Cloneable {

  private static final long serialVersionUID     = 8011288624609310945L;

  // wallet user/space Remote ID
  private String            id;

  // A string is used instead of enum, because of cache clustering
  // problems with enums
  private String            type;

  // Social Identity Id === Wallet technical id
  private long              technicalId;

  @Exclude
  private long              spaceId;

  @Exclude
  private String            name;

  @Exclude
  private String            address;

  @Exclude
  private boolean           isEnabled;

  @Exclude
  private String            initializationState;

  @Exclude
  private boolean           isDisabledUser;

  @Exclude
  private boolean           isDeletedUser;

  @Exclude
  private String            avatar;

  @Exclude
  @ToString.Exclude
  private String            passPhrase;

  @Exclude
  @ToString.Exclude
  private boolean           isSpaceAdministrator = false;

  @Exclude
  @ToString.Exclude
  private boolean           hasPrivateKey        = false;

  @Exclude
  @ToString.Exclude
  private boolean           backedUp             = false;

  /* Wallet state on Blockchain */

  @Exclude
  @ToString.Exclude
  private Double            etherBalance         = null;

  /* Wallet state on Contract on Blockchain */

  @Exclude
  @ToString.Exclude
  private Double            tokenBalance         = null;

  @Exclude
  @ToString.Exclude
  private Boolean           isInitialized        = null;

  @Override
  public Wallet clone() { // NOSONAR
    return new Wallet(id,
                      type,
                      technicalId,
                      spaceId,
                      name,
                      address,
                      isEnabled,
                      initializationState,
                      isDisabledUser,
                      isDeletedUser,
                      avatar,
                      passPhrase,
                      isSpaceAdministrator,
                      hasPrivateKey,
                      backedUp,
                      etherBalance,
                      tokenBalance,
                      isInitialized);
  }
}
