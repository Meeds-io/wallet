/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wallet.reward.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wallet.model.reward.RewardTeam;
import org.exoplatform.wallet.model.reward.RewardTeamMember;
import org.exoplatform.wallet.reward.dao.RewardTeamDAO;
import org.exoplatform.wallet.reward.entity.RewardTeamEntity;
import org.exoplatform.wallet.reward.entity.RewardTeamMemberEntity;

/**
 * A storage service to save/load reward teams
 */
public class WalletRewardTeamStorage implements RewardTeamStorage {

  private RewardTeamDAO rewardTeamDAO;

  public WalletRewardTeamStorage(RewardTeamDAO rewardTeamDAO) {
    this.rewardTeamDAO = rewardTeamDAO;
  }

  @Override
  public List<RewardTeam> getTeams() {
    List<RewardTeamEntity> teamEntities = rewardTeamDAO.findNotDeletedTeams();
    return teamEntities.stream().map(teamEntity -> toDTO(teamEntity)).collect(Collectors.toList());
  }

  @Override
  public RewardTeam saveTeam(RewardTeam rewardTeam) {
    if (rewardTeam == null) {
      throw new IllegalArgumentException("Empty team to save");
    }
    RewardTeamEntity teamEntity = fromDTO(rewardTeam);
    if (teamEntity.getId() == null || teamEntity.getId() == 0) {
      teamEntity = rewardTeamDAO.create(teamEntity);
    } else {
      teamEntity = rewardTeamDAO.update(teamEntity);
    }
    return toDTO(rewardTeamDAO.find(teamEntity.getId()));
  }

  @Override
  public RewardTeam removeTeam(Long teamId) {
    if (teamId == null || teamId == 0) {
      throw new IllegalArgumentException("Team id is required");
    }
    RewardTeamEntity entity = rewardTeamDAO.find(teamId);
    if (entity != null) {
      entity.setDeleted(true);
      rewardTeamDAO.update(entity);
    }
    return toDTO(entity);
  }

  @Override
  public RewardTeam getTeamById(long teamId) {
    if (teamId == 0) {
      throw new IllegalArgumentException("Team id is required");
    }
    RewardTeamEntity entity = rewardTeamDAO.find(teamId);
    return toDTO(entity);
  }

  @Override
  public List<RewardTeam> findTeamsByMemberId(long identityId) {
    List<RewardTeamEntity> entities = rewardTeamDAO.findTeamsByMemberId(identityId);
    return entities.stream().map(team -> toDTO(team)).collect(Collectors.toList());
  }

  @Override
  public RewardTeam getTeamsById(long teamId) {
    RewardTeamEntity teamEntity = rewardTeamDAO.find(teamId);
    return toDTO(teamEntity);
  }

  private static RewardTeamEntity fromDTO(RewardTeam rewardTeam) {
    if (rewardTeam == null) {
      return null;
    }
    RewardTeamEntity teamEntity = new RewardTeamEntity();
    teamEntity.setId(rewardTeam.getId() == null || rewardTeam.getId() == 0 ? null : rewardTeam.getId());
    teamEntity.setName(rewardTeam.getName());
    teamEntity.setDescription(rewardTeam.getDescription());
    teamEntity.setBudget(rewardTeam.getBudget());
    teamEntity.setRewardType(rewardTeam.getRewardType());
    teamEntity.setDisabled(rewardTeam.isDisabled());
    teamEntity.setDeleted(rewardTeam.isDeleted());
    if (rewardTeam.getManager() != null && rewardTeam.getManager().getIdentityId() != 0) {
      teamEntity.setManager(rewardTeam.getManager().getIdentityId());
    }
    if (rewardTeam.getSpaceId() != null && rewardTeam.getSpaceId() != 0) {
      teamEntity.setSpaceId(rewardTeam.getSpaceId());
    }
    if (rewardTeam.getMembers() != null && !rewardTeam.getMembers().isEmpty()) {
      teamEntity.setMembers(rewardTeam.getMembers()
                                      .stream()
                                      .map(rewardTeamMember -> getRewardTeamMemberEntity(teamEntity,
                                                                                         rewardTeamMember))
                                      .collect(Collectors.toSet()));
    }
    return teamEntity;
  }

  private static RewardTeam toDTO(RewardTeamEntity teamEntity) {
    if (teamEntity == null) {
      return null;
    }
    RewardTeam rewardTeam = new RewardTeam();
    rewardTeam.setId(teamEntity.getId());
    rewardTeam.setName(teamEntity.getName());
    rewardTeam.setDescription(teamEntity.getDescription());
    rewardTeam.setBudget(teamEntity.getBudget());
    rewardTeam.setManager(getRewardTeamMember(teamEntity.getManager()));
    rewardTeam.setRewardType(teamEntity.getRewardType());
    rewardTeam.setDisabled(teamEntity.getDisabled());
    rewardTeam.setDeleted(teamEntity.getDeleted());
    if (teamEntity.getSpaceId() != null && teamEntity.getSpaceId() != 0) {
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      Space space = spaceService.getSpaceById(String.valueOf(teamEntity.getSpaceId()));
      if (space != null) {
        rewardTeam.setSpaceId(teamEntity.getSpaceId());
        rewardTeam.setSpacePrettyName(space.getPrettyName());
      }
    }
    if (teamEntity.getMembers() != null && !teamEntity.getMembers().isEmpty()) {
      List<RewardTeamMember> list = teamEntity.getMembers()
                                              .stream()
                                              .map(teamMemberEntity -> getRewardTeamMember(teamMemberEntity))
                                              .collect(Collectors.toList());
      rewardTeam.setMembers(new ArrayList<>(list));
    }
    return rewardTeam;
  }

  private static RewardTeamMemberEntity getRewardTeamMemberEntity(RewardTeamEntity teamEntity,
                                                                  RewardTeamMember rewardTeamMember) {
    if (rewardTeamMember == null) {
      return null;
    }
    RewardTeamMemberEntity teamMemberEntity = new RewardTeamMemberEntity();
    teamMemberEntity.setId(rewardTeamMember.getTechnicalId() == null
        || rewardTeamMember.getTechnicalId() == 0 ? null : rewardTeamMember.getTechnicalId());
    teamMemberEntity.setIdentityId(rewardTeamMember.getIdentityId());
    teamMemberEntity.setTeam(teamEntity);
    return teamMemberEntity;
  }

  private static RewardTeamMember getRewardTeamMember(RewardTeamMemberEntity teamMemberEntity) {
    if (teamMemberEntity == null) {
      return null;
    }
    RewardTeamMember rewardTeamMember = getRewardTeamMember(teamMemberEntity.getIdentityId());
    if (rewardTeamMember == null) {
      return null;
    }
    rewardTeamMember.setTechnicalId(teamMemberEntity.getId());
    return rewardTeamMember;
  }

  private static RewardTeamMember getRewardTeamMember(Long identityId) {
    if (identityId == null || identityId == 0) {
      return null;
    }
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity identity = identityManager.getIdentity(String.valueOf(identityId), true);
    if (identity == null) {
      return null;
    }
    RewardTeamMember rewardTeamMember = new RewardTeamMember();
    rewardTeamMember.setId(identity.getRemoteId());
    rewardTeamMember.setProviderId(identity.getProviderId());
    rewardTeamMember.setIdentityId(Long.parseLong(identity.getId()));
    return rewardTeamMember;
  }

}
