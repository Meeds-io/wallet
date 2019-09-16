package org.exoplatform.addon.wallet.reward.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.addon.wallet.model.reward.RewardPeriodType;
import org.exoplatform.addon.wallet.model.reward.RewardStatus;
import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "RewardPeriod")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_REWARD_PERIOD")
@NamedQueries({
    @NamedQuery(name = "RewardPeriod.getPeriodByTypeAndTime", query = "SELECT rp FROM RewardPeriod rp WHERE rp.periodType = :periodType AND rp.startTime >= :periodTime AND rp.endTime < :periodTime"),
})
public class WalletRewardPeriodEntity implements Serializable {

  private static final long serialVersionUID = -6286934482105645678L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_REWARD_PERIOD_ID", sequenceName = "SEQ_WALLET_REWARD_PERIOD_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_REWARD_PERIOD_ID")
  @Column(name = "REWARD_PERIOD_ID")
  private Long              id;

  @Column(name = "PERIOD_TYPE", nullable = false)
  private RewardPeriodType  periodType;

  @Column(name = "START_TIME", nullable = false)
  private Long              startTime;

  @Column(name = "END_TIME", nullable = false)
  private Long              endTime;

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

}
