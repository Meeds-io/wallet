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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.wallet.reward.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import io.meeds.wallet.model.*;
import io.meeds.wallet.utils.WalletUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;
import io.meeds.wallet.reward.service.RewardReportService;
import io.meeds.wallet.reward.service.RewardSettingsService;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = { RewardReportREST.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class TestRewardReportREST {

  private static final String REST_PATH     = "/reward";     // NOSONAR

  private static final String SIMPLE_USER   = "simple";

  private static final String ADMIN_USER    = "admin";

  private static final String TEST_PASSWORD = "testPassword";

  static final ObjectMapper   OBJECT_MAPPER;

  static {
    // Workaround when Jackson is defined in shared library with different
    // version and without artifact jackson-datatype-jsr310
    OBJECT_MAPPER = JsonMapper.builder()
                              .configure(JsonReadFeature.ALLOW_MISSING_VALUES, true)
                              .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                              .build();
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  @MockBean
  private RewardReportService   rewardReportService;

  @MockBean
  private RewardSettingsService rewardSettingsService;

  @Autowired
  private SecurityFilterChain   filterChain;

  @Autowired
  private WebApplicationContext context;

  private MockMvc               mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filterChain.getFilters().toArray(new Filter[0])).build();
  }

  @Test
  void computeRewardsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute").param("page", "0").param("size", "12"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeRewardsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute").param("page", "0")
                                                                        .param("size", "12")
                                                                        .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeRewardsAdmin() throws Exception {
    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());
    RewardPeriod rewardPeriod = new RewardPeriod();
    RewardReport rewardReport = new RewardReport();
    rewardReport.setPeriod(rewardPeriod);
    when(rewardReportService.computeRewards(any(LocalDate.class))).thenReturn(rewardReport);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute").param("page", "0").param("size", "12").with(testAdminUser()));
    verify(rewardSettingsService, times(1)).getSettings();
    response.andExpect(status().isOk());

    RewardSettings rewardSettings = new RewardSettings();
    rewardSettings.setPeriodType(RewardPeriodType.MONTH);
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);

    response = mockMvc.perform(get(REST_PATH + "/compute").param("page", "0").param("size", "12").with(testAdminUser()));
    response.andExpect(status().isOk());


    rewardSettings = new RewardSettings();
    rewardSettings.setPeriodType(RewardPeriodType.QUARTER);
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings);

    response = mockMvc.perform(get(REST_PATH + "/compute").param("page", "0").param("size", "12").with(testAdminUser()));
    response.andExpect(status().isOk());

  }

  @Test
  void getWalletRewardsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/rewards").param("periodId", "1")
                                                                        .param("status", "VALID")
                                                                        .param("sortField", "tokensSent")
                                                                        .param("sortDir", "desc"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getWalletRewardsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/rewards").param("periodId", "1")
                                                                        .param("status", "VALID")
                                                                        .param("sortField", "tokensSent")
                                                                        .param("sortDir", "desc")
                                                                        .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getWalletRewardsAdmin() throws Exception {
    when(rewardReportService.findWalletRewardsByPeriodIdAndStatus(anyLong(), anyString(), any(ZoneId.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(walletReward())));

    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());

    ResultActions response = mockMvc.perform(get(REST_PATH + "/rewards").param("periodId", "1")
                                                                                  .param("status", "VALID")
                                                                                  .param("sortField", "tokensSent")
                                                                                  .param("sortDir", "desc").with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void computeRewardsByPeriodAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/period/compute").content(asJsonString(rewardPeriod()))
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeRewardsByPeriodSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/period/compute").content(asJsonString(rewardPeriod()))
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .accept(MediaType.APPLICATION_JSON)
                                                                                .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeRewardsByPeriodAdmin() throws Exception {
    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());
    RewardPeriod rewardPeriod = new RewardPeriod();
    RewardReport rewardReport = new RewardReport();
    rewardReport.setPeriod(rewardPeriod);
    when(rewardReportService.computeRewards(any(LocalDate.class))).thenReturn(rewardReport);
    ResultActions response = mockMvc.perform(post(REST_PATH + "/period/compute").content(asJsonString(rewardPeriod))
                                                                                .contentType(MediaType.APPLICATION_JSON)
                                                                                .accept(MediaType.APPLICATION_JSON)
                                                                                .with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void computeDistributionForecastAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/forecast").content(asJsonString(rewardPeriod()))
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeDistributionForecastSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/forecast").content(asJsonString(rewardPeriod()))
                                                                          .contentType(MediaType.APPLICATION_JSON)
                                                                          .accept(MediaType.APPLICATION_JSON)
                                                                          .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeDistributionForecastAdmin() throws Exception {
    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());
    RewardPeriod rewardPeriod = new RewardPeriod();
    RewardReport rewardReport = new RewardReport();
    rewardReport.setPeriod(rewardPeriod);
    when(rewardReportService.computeRewards(any(LocalDate.class))).thenReturn(rewardReport);
    ResultActions response = mockMvc.perform(post(REST_PATH + "/forecast").content(asJsonString(rewardPeriod))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void computeRewardsByUserAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute/user").param("date", "2024-09-10"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeRewardsByUserSimpleUser() throws Exception {
    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());
    RewardPeriod rewardPeriod = new RewardPeriod();
    RewardReport rewardReport = new RewardReport();
    rewardReport.setPeriod(rewardPeriod);
    when(rewardReportService.computeRewardsByUser(any(LocalDate.class) , anyLong())).thenReturn(rewardReport);

    try (MockedStatic<WalletUtils> walletUtilsMockedStatic = Mockito.mockStatic(WalletUtils.class)) {
      walletUtilsMockedStatic.when(WalletUtils::getCurrentUserIdentityId).thenReturn(1L);
      ResultActions response = mockMvc.perform(get(REST_PATH + "/compute/user").param("date", "2024-09-10").with(testSimpleUser()));
      response.andExpect(status().isOk());
      response = mockMvc.perform(get(REST_PATH + "/compute/user").param("date", "").with(testSimpleUser()));
      response.andExpect(status().isBadRequest());

    }
  }

  @Test
  void sendRewardsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/send").content(asJsonString(rewardPeriod()))
                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                      .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void sendRewardsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/send").content(asJsonString(rewardPeriod()))
                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                      .accept(MediaType.APPLICATION_JSON)
                                                                      .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void sendRewardsAdmin() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/send").content(asJsonString(rewardPeriod()))
                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                      .accept(MediaType.APPLICATION_JSON)
                                                                      .with(testAdminUser()));
    verify(rewardReportService, times(1)).sendRewards(any(LocalDate.class), anyString());
    response.andExpect(status().isOk());

    // When
    doThrow(new IllegalAccessException()).when(rewardReportService).sendRewards(any(LocalDate.class), anyString());
    response = mockMvc.perform(post(REST_PATH + "/send").content(asJsonString(rewardPeriod()))
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .accept(MediaType.APPLICATION_JSON)
                                                        .with(testAdminUser()));
    response.andExpect(status().isUnauthorized());

  }

  @Test
  void listRewardsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/list").param("limit", "10"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void listRewardsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/list").param("limit", "10").with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void getRewardReportPeriodsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/periods").param("from", "0").param("to", "10"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getRewardReportPeriodsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/periods").param("from", "0")
                                                                        .param("to", "10")
                                                                        .with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getRewardReportPeriodsAdmin() throws Exception {
    when(rewardReportService.findRewardReportPeriods(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(newRewardPeriod())));
    when(rewardReportService.findRewardPeriodsBetween(anyLong(), anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(newRewardPeriod())));

    ResultActions response = mockMvc.perform(get(REST_PATH + "/periods").param("from", "0")
            .param("to", "0")
                                                                    .with(testAdminUser()));
    response.andExpect(status().isOk());

    response = mockMvc.perform(get(REST_PATH + "/periods").param("from", "11211")
            .param("to", "225255")
            .with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void countRewardsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/countRewards").param("userId", "1"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void countRewardsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/countRewards").param("userId", "1").with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  private RequestPostProcessor testAdminUser() {
    return user(ADMIN_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("rewarding"));
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("users"));
  }

  private RewardPeriod newRewardPeriod() {
    return new RewardPeriod();
  }

  private RewardPeriod rewardPeriod() {
    return new RewardPeriod(RewardPeriodType.WEEK, ZoneId.systemDefault().getId(), 1725832800, 1726437600);
  }

  private WalletReward walletReward() {
    return new WalletReward(new Wallet(), new TransactionDetail(), 1L, 100.0, 40.0, rewardPeriod(), 1);
  }

  @SneakyThrows
  public static String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
