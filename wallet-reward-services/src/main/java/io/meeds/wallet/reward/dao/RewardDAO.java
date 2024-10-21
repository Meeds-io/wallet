/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wallet.reward.dao;

import java.util.List;

import jakarta.transaction.Transactional;
import io.meeds.wallet.reward.entity.WalletRewardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

@Component
public interface RewardDAO extends JpaRepository<WalletRewardEntity, Long> {

  @Query("""
          SELECT rw FROM Reward rw WHERE rw.period.id = :periodId
      """)
  List<WalletRewardEntity> findRewardsByPeriodId(@Param("periodId") long periodId);

  @Query("""
          SELECT rw FROM Reward rw WHERE rw.period.id = :periodId AND
          (:isValid = TRUE AND (rw.tokensSent > 0 OR rw.tokensToSend > 0) OR :isValid = FALSE AND (rw.tokensSent <= 0 AND rw.tokensToSend <= 0))
      """)
  Page<WalletRewardEntity> findWalletRewardsByPeriodIdAndStatus(@Param("periodId") long periodId,
                                                                @Param("isValid") boolean isValid,
                                                                Pageable pageable);

  List<WalletRewardEntity> findWalletRewardEntitiesByIdentityId(long identityId, Pageable pageable);

  double countWalletRewardEntitiesByIdentityId(long identityId);

  @Query("""
          SELECT rw FROM Reward rw WHERE rw.identityId = :identityId AND rw.period.id = :periodId
      """)
  List<WalletRewardEntity> findRewardByIdentityIdAndPeriodId(@Param("identityId") long identityId, @Param("periodId") long periodId);

  @Modifying
  @Transactional
  @Query("""
      UPDATE Reward rw SET rw.transactionHash = :newHash WHERE rw.transactionHash = :oldHash
      """)
  void replaceRewardTransactions(@Param("oldHash") String oldHash, @Param("newHash") String newHash);
}
