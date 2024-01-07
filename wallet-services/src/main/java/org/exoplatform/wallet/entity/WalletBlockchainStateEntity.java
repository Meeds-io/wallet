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

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletBlockchainState")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_BLOCKCHAIN_STATE")
@NamedQueries({
  @NamedQuery(
      name = "WalletBlockchainState.findByWalletIdAndContract",
      query = "SELECT wb FROM WalletBlockchainState wb WHERE wb.wallet.id = :walletId AND  wb.contractAddress = :contractAddress ORDER BY wb.id DESC"
  ),
})
public class WalletBlockchainStateEntity implements Serializable {
  private static final long serialVersionUID = -7294965683405044055L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_STATE", sequenceName = "SEQ_WALLET_STATE", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_STATE")
  @Column(name = "BLOCKCHAIN_STATE_ID")
  private Long              id;

  @Column(name = "CONTRACT_ADDRESS", nullable = false)
  private String            contractAddress;

  @Column(name = "ETHER_BALANCE", nullable = false)
  private double            etherBalance;

  @Column(name = "TOKEN_BALANCE", nullable = false)
  private double            tokenBalance;

  @Column(name = "REWARD_BALANCE", nullable = false)
  private double            rewardBalance;

  @Column(name = "IS_INITIALIZED", nullable = false)
  private boolean           isInitialized;

  @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
  @JoinColumn(name = "WALLET_ID")
  private WalletEntity      wallet;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public double getEtherBalance() {
    return etherBalance;
  }

  public void setEtherBalance(double etherBalance) {
    this.etherBalance = etherBalance;
  }

  public double getTokenBalance() {
    return tokenBalance;
  }

  public void setTokenBalance(double tokenBalance) {
    this.tokenBalance = tokenBalance;
  }

  public double getRewardBalance() {
    return rewardBalance;
  }

  public void setRewardBalance(double rewardBalance) {
    this.rewardBalance = rewardBalance;
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void setInitialized(boolean isInitialized) {
    this.isInitialized = isInitialized;
  }

  public WalletEntity getWallet() {
    return wallet;
  }

  public void setWallet(WalletEntity wallet) {
    this.wallet = wallet;
  }

}
