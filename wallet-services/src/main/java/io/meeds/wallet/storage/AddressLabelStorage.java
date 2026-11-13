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
package io.meeds.wallet.storage;

import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.wallet.dao.AddressLabelDAO;
import io.meeds.wallet.entity.AddressLabelEntity;
import io.meeds.wallet.model.WalletAddressLabel;

public class AddressLabelStorage {

  /**
   * The cache of the labels each node loaded: a node keeps its load, and a
   * change made on any node evicts it on every node. A load that read the store
   * before a change and caches its result after the eviction keeps the old
   * value until the entry expires: at most the cache's live time, 30 seconds by
   * default.
   */
  public static final String                     LABELS_CACHE_NAME = "wallet.addressLabels";

  private static final String                    LABELS_KEY        = "labels";

  private AddressLabelDAO                        addressLabelDAO;

  private ExoCache<String, Set<WalletAddressLabel>> labelsCache;

  public AddressLabelStorage(AddressLabelDAO addressLabelDAO, CacheService cacheService) {
    this.addressLabelDAO = addressLabelDAO;
    this.labelsCache = cacheService.getCacheInstance(LABELS_CACHE_NAME);
  }

  public Set<WalletAddressLabel> getAllLabels() {
    Set<WalletAddressLabel> allLabels = labelsCache.get(LABELS_KEY);
    if (allLabels != null) {
      return allLabels;
    }
    List<AddressLabelEntity> allLabelsEntities = addressLabelDAO.findAll();
    if (allLabelsEntities == null || allLabelsEntities.isEmpty()) {
      return Collections.emptySet();
    }
    allLabels = Collections.unmodifiableSet(allLabelsEntities.stream()
                                                             .map(this::fromEntity)
                                                             .collect(Collectors.toSet()));
    // Kept on this node: a change evicts it on every node
    labelsCache.putLocal(LABELS_KEY, allLabels);
    return allLabels;
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
    labelsCache.remove(LABELS_KEY);
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
