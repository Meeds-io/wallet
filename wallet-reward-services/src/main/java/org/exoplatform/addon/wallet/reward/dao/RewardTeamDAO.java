package org.exoplatform.addon.wallet.reward.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.addon.wallet.reward.entity.RewardTeamEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class RewardTeamDAO extends GenericDAOJPAImpl<RewardTeamEntity, Long> {

  public List<RewardTeamEntity> findTeamsByMemberId(long identityId) {
    TypedQuery<RewardTeamEntity> query = getEntityManager().createNamedQuery("RewardTeam.findTeamsByMemberId",
                                                                             RewardTeamEntity.class);
    query.setParameter("identityId", identityId);
    List<RewardTeamEntity> result = query.getResultList();
    return result == null ? Collections.emptyList() : result;
  }

}
