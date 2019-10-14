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
