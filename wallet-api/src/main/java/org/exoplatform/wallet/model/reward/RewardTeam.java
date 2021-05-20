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
  private boolean                deleted;

  @Exclude
  private RewardTeamMember       manager;

  @Exclude
  private List<RewardTeamMember> members;

}
