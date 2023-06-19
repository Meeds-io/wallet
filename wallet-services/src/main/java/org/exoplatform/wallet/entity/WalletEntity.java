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

import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.model.WalletState;
import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wallet.model.WalletType;

@Entity(name = "Wallet")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_ACCOUNT")
@NamedQuery(name = "Wallet.findByAddress", query = "SELECT w FROM Wallet w WHERE LOWER(w.address) = :address")
public class WalletEntity implements Serializable {
  private static final long                       serialVersionUID = -1622032986992776281L;

  @Id
  @Column(name = "IDENTITY_ID")
  private Long                                    id;

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

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "PROVIDER")
  protected WalletProvider                        provider;

  @Column(name = "INITIALIZATION_STATE")
  private WalletState initializationState;

  @OneToOne(fetch = FetchType.EAGER, mappedBy = "wallet", cascade = CascadeType.REMOVE)
  private WalletPrivateKeyEntity                  privateKey;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "wallet", cascade = CascadeType.REMOVE)
  private Collection<WalletBlockchainStateEntity> blockchainState;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
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

  public WalletProvider getProvider() {
    return provider;
  }

  public void setProvider(WalletProvider walletProvider) {
    this.provider = walletProvider;
  }
}
