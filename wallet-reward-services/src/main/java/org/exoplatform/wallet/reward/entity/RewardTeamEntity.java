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
    @NamedQuery(name = "RewardTeam.findTeamsByMemberId", query = "SELECT rt FROM RewardTeam rt JOIN rt.members mem WHERE mem.identityId = :identityId ORDER BY rt.id DESC"),
})
public class RewardTeamEntity implements Serializable {

  private static final long           serialVersionUID = 4475704534821391132L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_GAM_TEAM_ID", sequenceName = "SEQ_WALLET_GAM_TEAM_ID")
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

  public Set<RewardTeamMemberEntity> getMembers() {
    return members;
  }

  public void setMembers(Set<RewardTeamMemberEntity> members) {
    this.members = members;
  }
}
