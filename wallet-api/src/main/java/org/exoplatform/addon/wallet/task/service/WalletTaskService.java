/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.addon.wallet.task.service;

import java.util.Set;

import org.exoplatform.addon.wallet.model.task.WalletAdminTask;

/**
 * A storage service to save/load wallet admin tasks
 */
public interface WalletTaskService {

  /**
   * Retrieves the list of tasks of a user
   * 
   * @param currentUser username to retrieve his tasks
   * @return a {@link Set} of admin tasks
   */
  public Set<WalletAdminTask> listTasks(String currentUser);

  /**
   * @param taskType
   * @return a task by identified type
   */
  public Set<WalletAdminTask> getTasksByType(String taskType);

  /**
   * Creates/Updates an admin task
   * 
   * @param task to create/update task
   * @param assignee assignee of task
   */
  public void save(WalletAdminTask task, String assignee);

  /**
   * Mark a task as completed
   * 
   * @param taskId technical wallet task id
   */
  public void markCompleted(long taskId);

}
