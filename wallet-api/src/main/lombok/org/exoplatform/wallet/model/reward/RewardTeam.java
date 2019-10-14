package org.exoplatform.wallet.model.reward;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.ToString;

@Data
@ToString
public class RewardTeam implements Serializable {

  private static final long      serialVersionUID = 4475704534821391132L;

  private Long                   id;

  @Exclude
  private String                 name;

  @Exclude
  private String                 description;

  @Exclude
  private RewardBudgetType       rewardType;

  @Exclude
  private Double                 budget;

  @Exclude
  private Long                   spaceId;

  @Exclude
  private String                 spacePrettyName;

  @Exclude
  private boolean                disabled;

  @Exclude
  private RewardTeamMember       manager;

  @Exclude
  private List<RewardTeamMember> members;

}
