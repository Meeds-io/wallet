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
package org.exoplatform.wallet.storage;

import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.wallet.dao.AddressLabelDAO;
import org.exoplatform.wallet.entity.AddressLabelEntity;
import org.exoplatform.wallet.model.WalletAddressLabel;

public class AddressLabelStorage {

  private AddressLabelDAO         addressLabelDAO;

  private Set<WalletAddressLabel> allLabels;

  public AddressLabelStorage(AddressLabelDAO addressLabelDAO) {
    this.addressLabelDAO = addressLabelDAO;
  }

  public Set<WalletAddressLabel> getAllLabels() {
    if (this.allLabels != null) {
      return this.allLabels;
    }
    List<AddressLabelEntity> allLabelsEntities = addressLabelDAO.findAll();
    if (allLabelsEntities == null || allLabelsEntities.isEmpty()) {
      return Collections.emptySet();
    }
    this.allLabels = Collections.unmodifiableSet(allLabelsEntities.stream()
                                                                  .map(this::fromEntity)
                                                                  .collect(Collectors.toSet()));
    return this.allLabels;
  }

  public WalletAddressLabel saveLabel(WalletAddressLabel label) {
    if (label == null) {
      throw new IllegalArgumentException("label is mandatory");
    }
    AddressLabelEntity entity = toEntity(label);
    if (entity.getId() == null) {
      entity = addressLabelDAO.create(entity);
    } else {
      entity = addressLabelDAO.update(entity);
    }

    clearCache();
    return fromEntity(entity);
  }

  public WalletAddressLabel getLabel(long id) {
    return fromEntity(addressLabelDAO.find(id));
  }

  public void removeLabel(WalletAddressLabel label) {
    if (label.getId() > 0) {
      addressLabelDAO.delete(toEntity(label));
    }
    clearCache();
  }

  public void clearCache() {
    this.allLabels = null;
  }

  private AddressLabelEntity toEntity(WalletAddressLabel label) {
    AddressLabelEntity entity = new AddressLabelEntity();
    entity.setId(label.getId() == 0 ? null : label.getId());
    entity.setIdentityId(label.getIdentityId());
    entity.setAddress(label.getAddress());
    entity.setLabel(label.getLabel());
    return entity;
  }

  private WalletAddressLabel fromEntity(AddressLabelEntity entity) {
    if (entity == null) {
      return null;
    }
    return new WalletAddressLabel(entity.getId(),
                                  entity.getIdentityId(),
                                  entity.getLabel(),
                                  entity.getAddress());
  }

}
