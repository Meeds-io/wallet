/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
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
 *
 */
package io.meeds.wallet.reward.entity;

import java.io.Serializable;

import org.hibernate.annotations.DynamicUpdate;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity(name = "RewardPeriodSummary")
@DynamicUpdate
@Table(name = "ADDONS_WALLET_REWARD_PERIOD_SUMMARY")
@Data
public class WalletRewardPeriodSummaryEntity implements Serializable {

  @Id
  @PortableSequence(name = "SEQ_WALLET_REWARD_SUMMARY_ID")
  private Long                     id;

  @OneToOne
  @JoinColumn(name = "REWARD_PERIOD_ID", nullable = false, unique = true)
  private WalletRewardPeriodEntity rewardPeriod;

  @Column(name = "POINTS")
  private Double                   points;

  @Column(name = "TOKENS_SENT")
  private double                   tokensSent;

  @Column(name = "TOKENS_TO_SEND")
  private double                   tokensToSend;

  @Column(name = "PARTICIPANTS_COUNT")
  private Long                     participantsCount;

  @Column(name = "RECIPIENTS_COUNT")
  private Long                     recipientsCount;

  @Column(name = "ACHIEVEMRNTS_COUNT")
  private Long                     achievementsCount;

  @Column(name = "COMPLETELY_PROCESSED")
  private Boolean                  completelyProcessed;

  @Column(name = "SENT_DATE")
  private Long                     sentDate;
}
