/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2026 Meeds Association
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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.SimpleExoCache;

import io.meeds.wallet.dao.AddressLabelDAO;
import io.meeds.wallet.entity.AddressLabelEntity;
import io.meeds.wallet.model.WalletAddressLabel;

/**
 * The labels a node loaded stay on that node; a change made on any node
 * evicts them on every node.
 */
public class AddressLabelStorageCacheTest {

  private AddressLabelDAO                         addressLabelDAO;

  private ExoCache<String, Set<WalletAddressLabel>> cache;

  private AddressLabelStorage                     storage;

  @Before
  public void setUp() {
    addressLabelDAO = mock(AddressLabelDAO.class);
    AddressLabelEntity entity = new AddressLabelEntity();
    entity.setId(1L);
    entity.setIdentityId(2L);
    entity.setAddress("0x1");
    entity.setLabel("label");
    when(addressLabelDAO.findAll()).thenReturn(List.of(entity));
    when(addressLabelDAO.create(any())).thenReturn(entity);
    cache = spy(new SimpleExoCache<>());
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.<String, Set<WalletAddressLabel>> getCacheInstance(AddressLabelStorage.LABELS_CACHE_NAME)).thenReturn(cache);
    storage = new AddressLabelStorage(addressLabelDAO, cacheService);
  }

  @Test
  public void testTheLoadedLabelsStayOnThisNode() {
    assertEquals(1, storage.getAllLabels().size());
    storage.getAllLabels();

    verify(addressLabelDAO, times(1)).findAll();
    verify(cache).putLocal(any(), any());
    verify(cache, never()).put(any(), any());
  }

  @Test
  public void testAChangeMadeOnAnyNodeEvictsTheLoadedLabels() {
    storage.getAllLabels();

    // As the eviction a change on another node sends
    cache.clearCache();
    storage.getAllLabels();
    storage.saveLabel(new WalletAddressLabel(0, 2L, "label", "0x1"));
    storage.getAllLabels();
    storage.removeLabel(new WalletAddressLabel(1L, 2L, "label", "0x1"));
    storage.getAllLabels();

    verify(addressLabelDAO, times(4)).findAll();
    verify(cache, times(2)).remove(any());
  }
}
