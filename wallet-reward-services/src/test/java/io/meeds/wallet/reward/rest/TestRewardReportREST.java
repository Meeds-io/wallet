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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import io.meeds.wallet.model.RewardPeriod;
import io.meeds.wallet.model.RewardPeriodType;
import io.meeds.wallet.model.RewardReport;
import io.meeds.wallet.model.RewardSettings;
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
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute").param("page", "0").param("size", "12").with(testSimpleUser()));
    response.andExpect(status().isForbidden());
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
  void computeRewardsAdmin() throws Exception {
    when(rewardSettingsService.getSettings()).thenReturn(new RewardSettings());
    RewardPeriod rewardPeriod = new RewardPeriod();
    RewardReport rewardReport = new RewardReport();
    rewardReport.setPeriod(rewardPeriod);
    when(rewardReportService.computeRewards(any(LocalDate.class))).thenReturn(rewardReport);
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute").param("page", "0").param("size", "12").with(testAdminUser()));
    verify(rewardSettingsService, times(1)).getSettings();
    response.andExpect(status().isOk());
  }

  @Test
  void computeRewardsByUserAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute/user").param("date", "2024-09-10"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void computeRewardsByUserSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/compute/user").param("date", "2024-09-10").with(testSimpleUser()));
    response.andExpect(status().isOk());
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
            .accept(MediaType.APPLICATION_JSON).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void sendRewardsAdmin() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH + "/send").content(asJsonString(rewardPeriod()))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).with(testAdminUser()));
    verify(rewardReportService, times(1)).sendRewards(any(LocalDate.class), anyString());
    response.andExpect(status().isOk());
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

    ResultActions response = mockMvc.perform(get(REST_PATH + "/periods").param("from", "0")
            .param("to", "0")
                                                                    .with(testAdminUser()));
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

  @SneakyThrows
  public static String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
