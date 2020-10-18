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

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "RewardTeamMember")
@ExoEntity
@Table(name = "ADDONS_WALLET_GAM_TEAM_MEMBER")
public class RewardTeamMemberEntity implements Serializable {

  private static final long serialVersionUID = 2213798785625662208L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_GAM_TEAM_MEMBER_ID", sequenceName = "SEQ_WALLET_GAM_TEAM_MEMBER_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_GAM_TEAM_MEMBER_ID")
  @Column(name = "MEMBER_ID")
  private Long              id;

  @Column(name = "MEMBER_IDENTITY_ID", nullable = false)
  private Long              identityId;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "TEAM_ID", referencedColumnName = "TEAM_ID")
  private RewardTeamEntity  team;

  public long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public long getIdentityId() {
    return identityId;
  }

  public void setIdentityId(Long identityId) {
    this.identityId = identityId;
  }

  public RewardTeamEntity getTeam() {
    return team;
  }

  public void setTeam(RewardTeamEntity team) {
    this.team = team;
  }
}
