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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.wallet.model.reward.RewardBudgetType;

@Entity(name = "RewardTeam")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_GAM_TEAM")
@NamedQueries({
    @NamedQuery(name = "RewardTeam.findNoDeletedTeams", query = "SELECT rt FROM RewardTeam rt WHERE rt.deleted = FALSE ORDER BY rt.id DESC"),
    @NamedQuery(name = "RewardTeam.findTeamsByMemberId", query = "SELECT rt FROM RewardTeam rt JOIN rt.members mem WHERE rt.deleted = FALSE AND mem.identityId = :identityId ORDER BY rt.id DESC"),
})
public class RewardTeamEntity implements Serializable {

  private static final long           serialVersionUID = 4475704534821391132L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_GAM_TEAM_ID", sequenceName = "SEQ_WALLET_GAM_TEAM_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_GAM_TEAM_ID")
  @Column(name = "TEAM_ID")
  private Long                        id;

  @Column(name = "TEAM_NAME", unique = true, nullable = false)
  private String                      name;

  @Column(name = "TEAM_DESCRIPTION")
  private String                      description;

  @Column(name = "TEAM_REWARD_TYPE")
  private RewardBudgetType            rewardType;

  @Column(name = "TEAM_BUDGET")
  private Double                      budget;

  @Column(name = "TEAM_SPACE_ID")
  private Long                        spaceId;

  @Column(name = "TEAM_MANAGER_ID")
  private Long                        manager;

  @Column(name = "TEAM_DISABLED")
  private Boolean                     disabled;

  @Column(name = "TEAM_DELETED")
  private Boolean                     deleted;

  @OneToMany(mappedBy = "team", fetch = FetchType.EAGER, cascade = { CascadeType.ALL }, orphanRemoval = true)
  private Set<RewardTeamMemberEntity> members          = new HashSet<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Double getBudget() {
    return budget;
  }

  public void setBudget(Double budget) {
    this.budget = budget;
  }

  public Long getSpaceId() {
    return spaceId;
  }

  public void setSpaceId(Long spaceId) {
    this.spaceId = spaceId;
  }

  public Long getManager() {
    return manager;
  }

  public void setManager(Long manager) {
    this.manager = manager;
  }

  public RewardBudgetType getRewardType() {
    return rewardType;
  }

  public void setRewardType(RewardBudgetType rewardType) {
    this.rewardType = rewardType;
  }

  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public Set<RewardTeamMemberEntity> getMembers() {
    return members;
  }

  public void setMembers(Set<RewardTeamMemberEntity> members) {
    this.members = members;
  }
}
