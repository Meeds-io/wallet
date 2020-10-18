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

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletKey")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_KEY")
@NamedQueries({
    @NamedQuery(name = "WalletKey.findByWalletId", query = "SELECT wk FROM WalletKey wk WHERE wk.wallet.id = :walletId"),
})
public class WalletPrivateKeyEntity implements Serializable {
  private static final long serialVersionUID = -7294965683405044055L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_KEY", sequenceName = "SEQ_WALLET_KEY")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_KEY")
  @Column(name = "KEY_ID")
  private Long              id;

  @Column(name = "CONTENT", nullable = false)
  private String            keyContent;

  @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
  @JoinColumn(name = "WALLET_ID")
  private WalletEntity      wallet;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getKeyContent() {
    return keyContent;
  }

  public void setKeyContent(String keyContent) {
    this.keyContent = keyContent;
  }

  public WalletEntity getWallet() {
    return wallet;
  }

  public void setWallet(WalletEntity wallet) {
    this.wallet = wallet;
  }

}
