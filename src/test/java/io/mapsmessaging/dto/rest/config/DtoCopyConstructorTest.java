/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.dto.rest.config.aggregator.AggregatorContributionMode;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.impl.JsonQueryTransformationDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DtoCopyConstructorTest {

  @Test
  void authManager_copyConstructor_copiesValuesAndIsolatesAuthConfigMap() {
    AuthManagerConfigDTO source = new AuthManagerConfigDTO();
    Map<String, Object> authConfig = new HashMap<>();
    authConfig.put("realm", "primary");
    source.setAuthConfig(authConfig);
    source.setAuthenticationEnabled(false);
    source.setAuthorisationEnabled(true);
    source.setMinimumPasswordLength(20);
    source.setMaximumPasswordLength(64);
    source.setMinimumLowercase(2);
    source.setMinimumUppercase(3);
    source.setMinimumDigits(4);
    source.setMinimumSpecial(5);
    source.setAllowedSpecialCharacters("!$");
    source.setRejectWhitespace(false);
    source.setRejectContainsUsername(false);
    source.setMaximumConsecutiveIdenticalCharacters(6);
    source.setPasswordRegex("regex");
    source.setPasswordHistoryCount(7);
    source.setPasswordMaxAgeDays(8);
    source.setMaxFailuresBeforeLock(9);
    source.setInitialLockSeconds(10);
    source.setMaxLockSeconds(11);
    source.setFailureDecaySeconds(12);
    source.setEnableSoftDelay(false);
    source.setSoftDelayMillisPerFailure(13);
    source.setMaxSoftDelayMillis(14);

    AuthManagerConfigDTO copy = new AuthManagerConfigDTO(source);
    authConfig.put("realm", "changed");

    assertEquals("AuthManagerConfigDTO", copy.getType());
    assertFalse(copy.isAuthenticationEnabled());
    assertTrue(copy.isAuthorisationEnabled());
    assertEquals(Map.of("realm", "primary"), copy.getAuthConfig());
    assertEquals(20, copy.getMinimumPasswordLength());
    assertEquals(64, copy.getMaximumPasswordLength());
    assertEquals(2, copy.getMinimumLowercase());
    assertEquals(3, copy.getMinimumUppercase());
    assertEquals(4, copy.getMinimumDigits());
    assertEquals(5, copy.getMinimumSpecial());
    assertEquals("!$", copy.getAllowedSpecialCharacters());
    assertFalse(copy.isRejectWhitespace());
    assertFalse(copy.isRejectContainsUsername());
    assertEquals(6, copy.getMaximumConsecutiveIdenticalCharacters());
    assertEquals("regex", copy.getPasswordRegex());
    assertEquals(7, copy.getPasswordHistoryCount());
    assertEquals(8, copy.getPasswordMaxAgeDays());
    assertEquals(9, copy.getMaxFailuresBeforeLock());
    assertEquals(10, copy.getInitialLockSeconds());
    assertEquals(11, copy.getMaxLockSeconds());
    assertEquals(12, copy.getFailureDecaySeconds());
    assertFalse(copy.isEnableSoftDelay());
    assertEquals(13, copy.getSoftDelayMillisPerFailure());
    assertEquals(14, copy.getMaxSoftDelayMillis());
    assertThrows(UnsupportedOperationException.class, () -> copy.getAuthConfig().put("new", "value"));
  }

  @Test
  void authManager_copyConstructor_preservesNullAuthConfig() {
    AuthManagerConfigDTO source = new AuthManagerConfigDTO();

    AuthManagerConfigDTO copy = new AuthManagerConfigDTO(source);

    assertNull(copy.getAuthConfig());
  }

  @Test
  void aggregatorInput_copyConstructor_copiesConfiguredValues() {
    AggregatorInputConfigDTO source = new AggregatorInputConfigDTO();
    List<TransformationConfigDTO> transformers = new ArrayList<>();
    transformers.add(new JsonQueryTransformationDTO());
    source.setTopicName("input/topic");
    source.setSelector("priority > 1");
    source.setTransformer(transformers);
    source.setContributionMode(AggregatorContributionMode.FIRST);

    AggregatorInputConfigDTO copy = new AggregatorInputConfigDTO(source);

    assertEquals("input/topic", copy.getTopicName());
    assertEquals("priority > 1", copy.getSelector());
    assertSame(transformers, copy.getTransformer());
    assertEquals(AggregatorContributionMode.FIRST, copy.getContributionMode());
  }
}
