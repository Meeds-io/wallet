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

import io.meeds.wallet.wallet.model.reward.RewardPeriodType;
import io.meeds.wallet.wallet.model.reward.RewardStatus;
import io.meeds.wallet.reward.entity.WalletRewardPeriodEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

@Component
public interface RewardPeriodDAO extends JpaRepository<WalletRewardPeriodEntity, Long> {

  @Query("""
          SELECT rp FROM RewardPeriod rp WHERE (rp.startTime >= :from AND rp.startTime <= :to) OR (rp.endTime >= :from AND rp.endTime <= :to) ORDER BY rp.startTime DESC
      """)
  Page<WalletRewardPeriodEntity> findRewardPeriodsBetween(@Param("from") long from, @Param("to") long to, Pageable pageable);

  @Query("""
          SELECT rp FROM RewardPeriod rp WHERE rp.periodType = :periodType AND rp.startTime <= :periodTime AND rp.endTime > :periodTime
      """)
  WalletRewardPeriodEntity findRewardPeriodByTypeAndTime(@Param("periodType") RewardPeriodType periodType, @Param("periodTime") long periodTime);

  List<WalletRewardPeriodEntity> findWalletRewardPeriodEntitiesByStatus(RewardStatus status);

}
