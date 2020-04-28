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
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardStatus;

@Entity(name = "RewardPeriod")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_REWARD_PERIOD")
@NamedQueries({
    @NamedQuery(name = "RewardPeriod.findRewardPeriodByTypeAndTime", query = "SELECT rp FROM RewardPeriod rp WHERE rp.periodType = :periodType AND rp.startTime <= :periodTime AND rp.endTime > :periodTime"),
    @NamedQuery(name = "RewardPlugin.findRewardPeriodsByStatus", query = "SELECT rp FROM RewardPeriod rp WHERE rp.status = :status"),
})
public class WalletRewardPeriodEntity implements Serializable {

  private static final long serialVersionUID = -6286934482105645678L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_REWARD_PERIOD_ID", sequenceName = "SEQ_WALLET_REWARD_PERIOD_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_REWARD_PERIOD_ID")
  @Column(name = "REWARD_PERIOD_ID")
  private Long              id;

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "PERIOD_TYPE", nullable = false)
  private RewardPeriodType  periodType;

  @Column(name = "START_TIME", nullable = false)
  private Long              startTime;

  @Column(name = "END_TIME", nullable = false)
  private Long              endTime;

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "STATUS", nullable = false)
  private RewardStatus      status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public RewardPeriodType getPeriodType() {
    return periodType;
  }

  public void setPeriodType(RewardPeriodType periodType) {
    this.periodType = periodType;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public RewardStatus getStatus() {
    return status;
  }

  public void setStatus(RewardStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "WalletRewardPeriodEntity [id=" + id + ", periodType=" + periodType + ", startTime=" + startTime + ", endTime="
        + endTime + ", status=" + status + "]";
  }

}
