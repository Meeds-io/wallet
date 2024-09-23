/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Lab contact@meedslab.com
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
package io.meeds.wallet.reward.listener;

import static io.meeds.wallet.wallet.utils.RewardUtils.REWARD_REPORT_NOTIFICATION_PARAM;
import static io.meeds.wallet.wallet.utils.RewardUtils.REWARD_SUCCESS_NOTIFICATION_ID;
import static org.mockito.Mockito.*;

import org.exoplatform.commons.api.notification.command.NotificationCommand;
import org.exoplatform.commons.api.notification.command.NotificationExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import io.meeds.wallet.wallet.model.reward.RewardReport;

@SpringBootTest(classes = { RewardSucceedNotificationListener.class })
class RewardSucceedNotificationListenerTest {

  private static final String               EVENT_NAME = "exo.wallet.reward.report.success";

  @MockBean
  private Event<RewardReport, Object>       event;

  @MockBean
  private RewardReport                      rewardReport;

  @MockBean
  private NotificationContext               notificationContext;

  @MockBean
  private ListenerService                   listenerService;

  @Autowired
  private RewardSucceedNotificationListener rewardSucceedNotificationListener;

  @BeforeEach
  void setUp() {
    rewardSucceedNotificationListener.init();
  }

  @Test
  void testInit() {
    verify(listenerService).addListener(EVENT_NAME, rewardSucceedNotificationListener);
  }

  @Test
  void testOnEvent() {
    when(event.getSource()).thenReturn(rewardReport);

    try (MockedStatic<NotificationContextImpl> mockedStatic = Mockito.mockStatic(NotificationContextImpl.class)) {
      mockedStatic.when(NotificationContextImpl::cloneInstance).thenReturn(notificationContext);

      NotificationExecutor notificationExecutor = mock(NotificationExecutor.class);
      when(notificationContext.getNotificationExecutor()).thenReturn(notificationExecutor);

      NotificationCommand notificationCommand = mock(NotificationCommand.class);
      when(notificationContext.makeCommand(PluginKey.key(REWARD_SUCCESS_NOTIFICATION_ID))).thenReturn(notificationCommand);

      when(notificationExecutor.with(notificationCommand)).thenReturn(notificationExecutor);

      rewardSucceedNotificationListener.onEvent(event);

      verify(notificationContext).append(eq(REWARD_REPORT_NOTIFICATION_PARAM), eq(rewardReport));

      // Verify that the notification command was created and executed
      verify(notificationExecutor).with(notificationCommand);
      verify(notificationExecutor).execute(notificationContext);
    }
  }
}
