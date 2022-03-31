/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
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

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletBackupEntity")
@ExoEntity
@Table(name = "ADDONS_WALLET_ACCOUNT_BACKUP")
@NamedQuery(name = "WalletBackupEntity.findByWalletId", query = "SELECT wb FROM WalletBackupEntity wb WHERE wb.wallet.id = :walletId")
public class WalletBackupEntity implements Serializable {

  private static final long serialVersionUID = -8212394514591241477L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_BACKUP_ID", sequenceName = "SEQ_WALLET_BACKUP_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_BACKUP_ID")
  @Column(name = "WALLET_BACKUP_ID")

  private Long         id;

  @Column(name = "ADDRESS")
  private String       address;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "WALLET_ID")
  private WalletEntity wallet;

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

  public WalletEntity getWallet() {
    return wallet;
  }

  public void setWallet(WalletEntity wallet) {
    this.wallet = wallet;
  }
}
