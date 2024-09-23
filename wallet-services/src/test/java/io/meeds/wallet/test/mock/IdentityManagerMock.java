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
package io.meeds.wallet.test.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.IdentityProviderPlugin;
import org.exoplatform.social.core.identity.SpaceMemberFilterListAccess.Type;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.*;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import io.meeds.wallet.wallet.plugin.WalletAdminIdentityProvider;
import io.meeds.wallet.wallet.utils.WalletUtils;

public class IdentityManagerMock implements IdentityManager {
  List<Identity> identities = new ArrayList<>();

  public IdentityManagerMock() {
    for (int i = 0; i < 10; i++) {
      Identity identity = new Identity(String.valueOf(i));
      identity.setDeleted(false);
      identity.setEnable(true);
      identity.setProviderId(OrganizationIdentityProvider.NAME);
      identity.setRemoteId("root" + i);
      identity.setId(String.valueOf(i));
      Profile profile = new Profile(identity);
      identity.setProfile(profile);
      identities.add(identity);
    }

    for (int i = 0; i < 10; i++) {
      Identity identity = new Identity(String.valueOf(i));
      identity.setDeleted(false);
      identity.setEnable(true);
      identity.setProviderId(SpaceIdentityProvider.NAME);
      identity.setRemoteId("space" + i);
      identity.setId(String.valueOf(i + 10));
      Profile profile = new Profile(identity);
      identity.setProfile(profile);
      identities.add(identity);
    }

    String adminIdentityId = "600";
    Identity adminIdentity = new Identity(adminIdentityId);
    adminIdentity.setDeleted(false);
    adminIdentity.setEnable(true);
    adminIdentity.setProviderId(WalletAdminIdentityProvider.NAME);
    adminIdentity.setRemoteId(WalletUtils.WALLET_ADMIN_REMOTE_ID);
    adminIdentity.setId(adminIdentityId);
    Profile adminProfile = new Profile(adminIdentity);
    adminIdentity.setProfile(adminProfile);
    identities.add(adminIdentity);
  }

  public Identity getOrCreateIdentity(String providerId, String remoteId, boolean isProfileLoaded) {
    return identities.stream()
                     .filter(identity -> StringUtils.equals(identity.getProviderId(), providerId)
                         && StringUtils.equals(identity.getRemoteId(), remoteId))
                     .findFirst()
                     .orElse(null);
  }

  public Identity getIdentity(String identityId, boolean isProfileLoaded) {
    return identities.stream()
                     .filter(identity -> StringUtils.equals(identity.getId(), identityId))
                     .findFirst()
                     .orElse(null);
  }

  public void registerIdentityProviders(IdentityProviderPlugin plugin) {
    // NOOP
  }

  public void addProfileListener(ProfileListenerPlugin plugin) {
    // NOOP
  }

  public Identity getIdentity(String providerId, String remoteId, boolean loadProfile) {
    return getOrCreateIdentity(providerId, remoteId, loadProfile);
  }

  public boolean identityExisted(String providerId, String remoteId) {
    return getOrCreateIdentity(providerId, remoteId, true) != null;
  }

  public Identity getIdentity(String id) {
    return getIdentity(id, true);
  }

  public Identity getOrCreateIdentity(String providerId, String remoteId) {
    return getOrCreateIdentity(providerId, remoteId, true);
  }

  public Identity updateIdentity(Identity identity) {
    return identity;
  }

  public List<Identity> getLastIdentities(int limit) {
    throw new UnsupportedOperationException();
  }

  public void deleteIdentity(Identity identity) {
    throw new UnsupportedOperationException();
  }

  public void hardDeleteIdentity(Identity identity) {
    throw new UnsupportedOperationException();
  }

  public ListAccess<Identity> getConnectionsWithListAccess(Identity identity) {
    throw new UnsupportedOperationException();
  }

  public Profile getProfile(Identity identity) {
    return identity.getProfile();
  }

  public InputStream getAvatarInputStream(Identity identity) throws IOException {
    throw new UnsupportedOperationException();
  }

  public InputStream getBannerInputStream(Identity identity) throws IOException {
    throw new UnsupportedOperationException();
  }

  public void updateProfile(Profile specificProfile) {
    // empty
  }

  public ListAccess<Identity> getIdentitiesByProfileFilter(String providerId,
                                                           ProfileFilter profileFilter,
                                                           boolean isProfileLoaded) {
    throw new UnsupportedOperationException();
  }

  public ListAccess<Identity> getIdentitiesForUnifiedSearch(String providerId, ProfileFilter profileFilter) {
    throw new UnsupportedOperationException();
  }

  public ListAccess<Identity> getSpaceIdentityByProfileFilter(Space space,
                                                              ProfileFilter profileFilter,
                                                              Type type,
                                                              boolean isProfileLoaded) {
    throw new UnsupportedOperationException();
  }

  public void addIdentityProvider(IdentityProvider<?> identityProvider) {
    throw new UnsupportedOperationException();
  }

  public void removeIdentityProvider(IdentityProvider<?> identityProvider) {
    throw new UnsupportedOperationException();
  }

  public void registerProfileListener(ProfileListenerPlugin profileListenerPlugin) {
    throw new UnsupportedOperationException();
  }

  public void processEnabledIdentity(String remoteId, boolean isEnable) {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getIdentitiesByProfileFilter(String providerId, ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getIdentitiesByProfileFilter(String providerId,
                                                     ProfileFilter profileFilter,
                                                     long offset,
                                                     long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getIdentitiesByProfileFilter(ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getIdentitiesByProfileFilter(ProfileFilter profileFilter, long offset, long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getIdentitiesFilterByAlphaBet(String providerId, ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getIdentitiesFilterByAlphaBet(String providerId,
                                                      ProfileFilter profileFilter,
                                                      long offset,
                                                      long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getIdentitiesFilterByAlphaBet(ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  public long getIdentitiesCount(String providerId) {
    throw new UnsupportedOperationException();
  }

  public List<Identity> getConnections(Identity ownerIdentity) throws Exception {
    throw new UnsupportedOperationException();
  }

  public IdentityStorage getIdentityStorage() {
    throw new UnsupportedOperationException();
  }

  public IdentityStorage getStorage() {
    throw new UnsupportedOperationException();
  }

  public void registerProfileListener(ProfileListener listener) {
    throw new UnsupportedOperationException();
  }

  public void unregisterProfileListener(ProfileListener listener) {
    throw new UnsupportedOperationException();
  }

}
