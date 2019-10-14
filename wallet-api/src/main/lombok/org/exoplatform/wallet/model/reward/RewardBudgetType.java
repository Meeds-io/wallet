package org.exoplatform.wallet.model.reward;

public enum RewardBudgetType {
  COMPUTED,
  FIXED,
  FIXED_PER_MEMBER,
  FIXED_PER_POINT;

  public static final RewardBudgetType DEFAULT = FIXED_PER_MEMBER;
}
