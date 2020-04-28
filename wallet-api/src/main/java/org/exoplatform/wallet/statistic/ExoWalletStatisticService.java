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
package org.exoplatform.wallet.statistic;

import java.util.Map;

/**
 * This service is used to add statistic log entry by using annotation
 * {@link ExoWalletStatistic} on methods
 */
public interface ExoWalletStatisticService {
  /**
   * Retrieve statistic log parameters
   * 
   * @param operation
   * @param result
   * @param methodArgs
   * @return a {@link Map} of parameters to include in statistic log
   */
  Map<String, Object> getStatisticParameters(String operation, Object result, Object... methodArgs);
}
