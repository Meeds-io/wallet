package org.exoplatform.addon.wallet.reward.entity;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "RewardPlugin")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_REWARD_PLUGIN")
@NamedQueries({
    @NamedQuery(name = "RewardPlugin.getRewardPluginsByRewardId", query = "SELECT rp FROM RewardPlugin rp WHERE rp.reward.id = :rewardId"),
    @NamedQuery(name = "RewardPlugin.getRewardPluginsByRewardIdAndPluginId", query = "SELECT rp FROM RewardPlugin rp WHERE rp.reward.id = :rewardId AND rp.pluginId = :pluginId"),
})
public class WalletRewardPluginEntity implements Serializable {

  private static final long  serialVersionUID = 4475704534821391132L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_REWARD_PLUGIN_ID", sequenceName = "SEQ_WALLET_REWARD_PLUGIN_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_REWARD_PLUGIN_ID")
  @Column(name = "REWARD_PLUGIN_ID")
  private Long               id;

  @Column(name = "PLUGIN_ID")
  private String             pluginId;

  @Column(name = "POOL_USED")
  private boolean            poolUsed;

  @Column(name = "POINTS")
  private double             points;

  @Column(name = "AMOUNT")
  private double             amount;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "REWARD_ID", referencedColumnName = "REWARD_ID")
  private WalletRewardEntity reward;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPluginId() {
    return pluginId;
  }

  public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
  }

  public boolean isPoolUsed() {
    return poolUsed;
  }

  public void setPoolUsed(boolean poolUsed) {
    this.poolUsed = poolUsed;
  }

  public double getPoints() {
    return points;
  }

  public void setPoints(double points) {
    this.points = points;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public WalletRewardEntity getReward() {
    return reward;
  }

  public void setReward(WalletRewardEntity reward) {
    this.reward = reward;
  }

}
