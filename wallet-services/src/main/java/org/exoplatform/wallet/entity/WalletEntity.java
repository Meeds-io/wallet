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
package org.exoplatform.wallet.entity;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.*;

import org.exoplatform.wallet.model.WalletState;
import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wallet.model.WalletType;

@Entity(name = "Wallet")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_ACCOUNT")
@NamedQueries({
    @NamedQuery(name = "Wallet.findByAddress", query = "SELECT w FROM Wallet w WHERE w.address = :address"),
    @NamedQuery(name = "Wallet.findActiveWallets", query = "SELECT w FROM Wallet w WHERE w.isActive = true"),
    @NamedQuery(name = "Wallet.findByActiveStateAndIdentity", query = "SELECT w FROM Wallet w WHERE w.id.identityId = :id and w.isActive = :active"),
    @NamedQuery(name = "Wallet.findByIdentity", query = "SELECT w FROM Wallet w WHERE w.id.identityId = :id and w.type = :type"),
    @NamedQuery(name = "Wallet.findByIdentityAndProvider", query = "SELECT w FROM Wallet w WHERE w.id.identityId = :id and w.id.provider = :provider"),
})
public class WalletEntity implements Serializable {
  private static final long                       serialVersionUID = -1622032986992776281L;

  @EmbeddedId
  private WalletEntityKey                          id;

  @Column(name = "IDENTITY_TYPE", nullable = false)
  private WalletType                              type;

  @Column(name = "ADDRESS", unique = true, nullable = false)
  private String                                  address;

  @Column(name = "PHRASE", nullable = false)
  private String                                  passPhrase;

  @Column(name = "ENABLED", nullable = false)
  private boolean                                 isEnabled;

  @Column(name = "BACKED_UP", nullable = false)
  private boolean                                 isBackedUp;

  @Column(name = "ACTIVE", nullable = false)
  private boolean                                 isActive;
  
  @Column(name = "INITIALIZATION_STATE")
  private WalletState                             initializationState;

  @OneToOne(fetch = FetchType.EAGER, mappedBy = "wallet", cascade = CascadeType.REMOVE)
  private WalletPrivateKeyEntity                  privateKey;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "wallet", cascade = CascadeType.REMOVE)
  private Collection<WalletBlockchainStateEntity> blockchainState;

  public WalletEntityKey getId() {
    return id;
  }

  public void setId(WalletEntityKey id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public boolean isBackedUp() {
    return isBackedUp;
  }

  public void setBackedUp(boolean isBackedUp) {
    this.isBackedUp = isBackedUp;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public WalletType getType() {
    return type;
  }

  public void setType(WalletType type) {
    this.type = type;
  }

  public String getPassPhrase() {
    return passPhrase;
  }

  public void setPassPhrase(String passPhrase) {
    this.passPhrase = passPhrase;
  }

  public WalletState getInitializationState() {
    return initializationState;
  }

  public void setInitializationState(WalletState initializationState) {
    this.initializationState = initializationState;
  }

  public WalletPrivateKeyEntity getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(WalletPrivateKeyEntity privateKey) {
    this.privateKey = privateKey;
  }

  public Collection<WalletBlockchainStateEntity> getBlockchainState() {
    return blockchainState;
  }

  public void setBlockchainState(Collection<WalletBlockchainStateEntity> blockchainState) {
    this.blockchainState = blockchainState;
  }

}
