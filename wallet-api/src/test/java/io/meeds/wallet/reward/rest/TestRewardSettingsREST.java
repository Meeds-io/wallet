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

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import io.meeds.wallet.wallet.model.reward.RewardBudgetType;
import io.meeds.wallet.wallet.model.reward.RewardPeriodType;
import io.meeds.wallet.wallet.model.reward.RewardSettings;
import io.meeds.wallet.reward.service.RewardSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;
import jakarta.servlet.Filter;

import java.time.ZoneId;

@SpringBootTest(classes = { RewardSettingsREST.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class TestRewardSettingsREST {

  private static final String REST_PATH     = "/settings/reward"; // NOSONAR

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
  void getSettingsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getSettingsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getSettingsAdmin() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH).with(testAdminUser()));
    verify(rewardSettingsService, times(1)).getSettings();
    response.andExpect(status().isOk());
  }

  @Test
  void saveSettingsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH).content(asJsonString(rewardSettings()))
                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                      .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void saveSettingsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH).content(asJsonString(rewardSettings()))
                                                                      .with(testSimpleUser())
                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                      .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void saveSettingsAdmin() throws Exception {
    ResultActions response = mockMvc.perform(post(REST_PATH).content(asJsonString(rewardSettings()))
                                                                      .with(testAdminUser())
                                                                      .contentType(MediaType.APPLICATION_JSON)
                                                                      .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
  }

  @Test
  void deleteSettingsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(delete(REST_PATH));
    response.andExpect(status().isForbidden());
  }

  @Test
  void deleteSettingsSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(delete(REST_PATH).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void deleteSettingsAdmin() throws Exception {
    ResultActions response = mockMvc.perform(delete(REST_PATH).with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void getRewardDatesAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/getDates").param("date", "2024-09-10"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getRewardDatesSimpleUser() throws Exception {
    ResultActions response = mockMvc.perform(get(REST_PATH + "/getDates").param("date", "").with(testSimpleUser()));
    response.andExpect(status().isBadRequest());
    when(rewardSettingsService.getSettings()).thenReturn(rewardSettings());
    response = mockMvc.perform(get(REST_PATH + "/getDates").param("date", "2024-09-10").with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  private RequestPostProcessor testAdminUser() {
    return user(ADMIN_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("rewarding"));
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("users"));
  }

  private RewardSettings rewardSettings() {
    return new RewardSettings(RewardPeriodType.MONTH,
                              ZoneId.systemDefault().getId(),
                              RewardBudgetType.FIXED_PER_MEMBER,
                              200,
                              200,
                              true);
  }

  @SneakyThrows
  public static String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
