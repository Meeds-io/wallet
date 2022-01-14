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
package org.exoplatform.wallet.reward.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "Reward")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_REWARD")
@NamedQueries({
    @NamedQuery(name = "Reward.findRewardsByPeriodId", query = "SELECT rw FROM Reward rw WHERE rw.period.id = :periodId"),
    @NamedQuery(name = "Reward.findRewardsByIdentityId", query = "SELECT rw FROM Reward rw JOIN rw.period WHERE rw.identityId = :identityId ORDER BY rw.period.startTime DESC, rw.period.endTime ASC"),
    @NamedQuery(name = "Reward.countRewardsByIdentityId", query = "SELECT SUM(rw.tokensSent) FROM Reward rw WHERE rw.identityId = :identityId"),
    @NamedQuery(name = "Reward.findRewardByIdentityIdAndPeriodId", query = "SELECT rw FROM Reward rw WHERE rw.identityId = :identityId AND rw.period.id = :periodId"),
    @NamedQuery(name = "Reward.updateTransactionHash", query = "UPDATE Reward rw SET rw.transactionHash = :newHash WHERE rw.transactionHash = :oldHash"),
})
public class WalletRewardEntity implements Serializable {

  private static final long        serialVersionUID = 4475704534821391132L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_REWARD_ID", sequenceName = "SEQ_WALLET_REWARD_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_REWARD_ID")
  @Column(name = "REWARD_ID")
  private Long                     id;

  @Column(name = "IDENTITY_ID", nullable = false)
  private long                     identityId;

  @Column(name = "TRANSACTION_HASH")
  private String                   transactionHash;

  @Column(name = "ENABLED")
  private boolean                  enabled;

  @Column(name = "TOKENS_SENT")
  private double                   tokensSent;

  @Column(name = "TOKENS_TO_SEND")
  private double                   tokensToSend;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "REWARD_PERIOD_ID", referencedColumnName = "REWARD_PERIOD_ID")
  private WalletRewardPeriodEntity period;

  @ManyToOne(fetch = FetchType.EAGER, optional = true)
  @JoinColumn(name = "TEAM_ID", referencedColumnName = "TEAM_ID")
  private RewardTeamEntity         team;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public long getIdentityId() {
    return identityId;
  }

  public void setIdentityId(long identityId) {
    this.identityId = identityId;
  }

  public String getTransactionHash() {
    return transactionHash;
  }

  public void setTransactionHash(String transactionHash) {
    this.transactionHash = transactionHash;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public double getTokensSent() {
    return tokensSent;
  }

  public void setTokensSent(double tokensSent) {
    this.tokensSent = tokensSent;
  }

  public double getTokensToSend() {
    return tokensToSend;
  }

  public void setTokensToSend(double tokensToSend) {
    this.tokensToSend = tokensToSend;
  }

  public WalletRewardPeriodEntity getPeriod() {
    return period;
  }

  public void setPeriod(WalletRewardPeriodEntity period) {
    this.period = period;
  }

  public RewardTeamEntity getTeam() {
    return team;
  }

  public void setTeam(RewardTeamEntity team) {
    this.team = team;
  }

}
