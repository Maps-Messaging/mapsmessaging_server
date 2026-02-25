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

package io.mapsmessaging.utilities.filtering;

import io.mapsmessaging.dto.rest.config.protocol.NamespaceFilterDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceFiltersTest {

  @Test
  void findMatchShouldReturnNullWhenEmpty() {
    NamespaceFilters filters = new NamespaceFilters();

    NamespaceFilter match = filters.findMatch("/root/system/topic");
    assertNull(match);
  }

  @Test
  void addFilterShouldMatchExactNamespaceAndChildrenViaLongestPrefix() {
    NamespaceFilters filters = new NamespaceFilters();

    NamespaceFilterDTO rootSystem = dto("root/system", 0, null, false);
    NamespaceFilterDTO rootSystemSub = dto("root/system/sub", 0, null, false);

    filters.addFilter(rootSystem);
    filters.addFilter(rootSystemSub);

    NamespaceFilter match1 = filters.findMatch("root/system");
    assertNotNull(match1);
    assertEquals("root/system", match1.getConfig().getNamespace());

    NamespaceFilter match2 = filters.findMatch("/root/system/sub");
    assertNotNull(match2);
    assertEquals("root/system/sub", match2.getConfig().getNamespace());

    NamespaceFilter match3 = filters.findMatch("/root/system/sub/leaf/topic");
    assertNotNull(match3);
    assertEquals("root/system/sub", match3.getConfig().getNamespace());

    NamespaceFilter match4 = filters.findMatch("/root/system/other");
    assertNotNull(match4);
    assertEquals("root/system", match4.getConfig().getNamespace());
  }

  @Test
  void normalizeShouldTreatLeadingAndTrailingSlashesAsSameNamespace() {
    NamespaceFilters filters = new NamespaceFilters();

    filters.addFilter(dto("/root/system/", 0, null, false));

    NamespaceFilter match1 = filters.findMatch("root/system");
    NamespaceFilter match2 = filters.findMatch("/root/system");
    NamespaceFilter match3 = filters.findMatch("/root/system/");
    NamespaceFilter match4 = filters.findMatch("/root/system/topic");

    assertNotNull(match1);
    assertNotNull(match2);
    assertNotNull(match3);
    assertNotNull(match4);

    assertEquals("/root/system/", match1.getConfig().getNamespace(), "DTO is stored as-is; trie uses normalized keying only");
    assertSame(match1, match2);
    assertSame(match1, match3);
    assertSame(match1, match4);
  }

  @Test
  void findMatchShouldPreferMostSpecificPrefixWhenMultipleFiltersExist() {
    NamespaceFilters filters = new NamespaceFilters();

    filters.addFilter(dto("root", 0, null, false));
    filters.addFilter(dto("root/system", 0, null, false));
    filters.addFilter(dto("root/system/a", 0, null, false));

    NamespaceFilter match = filters.findMatch("/root/system/a/b/c");
    assertNotNull(match);
    assertEquals("root/system/a", match.getConfig().getNamespace());
  }

  @Test
  void addAllShouldInsertFiltersAndFindMatchShouldWork() throws Exception {
    NamespaceFilters filters = new NamespaceFilters();

    List<NamespaceFilter> list = new ArrayList<>();
    list.add(new NamespaceFilter(dto("root/system", 0, null, false)));
    list.add(new NamespaceFilter(dto("root/system/sub", 0, null, false)));

    filters.addAll(list);

    NamespaceFilter match = filters.findMatch("/root/system/sub/topic");
    assertNotNull(match);
    assertEquals("root/system/sub", match.getConfig().getNamespace());
  }

  @Test
  void getAllFiltersShouldReturnAllInsertedFilters() {
    NamespaceFilters filters = new NamespaceFilters();

    NamespaceFilterDTO a = dto("root/system", 0, null, false);
    NamespaceFilterDTO b = dto("root/system/sub", 0, null, false);
    NamespaceFilterDTO c = dto("public/info", 0, null, false);

    filters.addFilter(a);
    filters.addFilter(b);
    filters.addFilter(c);

    List<NamespaceFilter> all = filters.getAllFilters();
    assertEquals(3, all.size());

    assertTrue(all.stream().anyMatch(f -> "root/system".equals(f.getConfig().getNamespace())));
    assertTrue(all.stream().anyMatch(f -> "root/system/sub".equals(f.getConfig().getNamespace())));
    assertTrue(all.stream().anyMatch(f -> "public/info".equals(f.getConfig().getNamespace())));
  }

  @Test
  void addFilterShouldIgnoreInvalidSelectorAndNotAddFilter() {
    NamespaceFilters filters = new NamespaceFilters();

    // Intentionally garbage, should fail compilation and be swallowed
    NamespaceFilterDTO bad = dto("root/system", 0, "THIS IS NOT A SELECTOR !!!", false);

    filters.addFilter(bad);

    NamespaceFilter match = filters.findMatch("/root/system/topic");
    assertNull(match, "Invalid selector should cause filter construction to fail; addFilter swallows IOException so nothing is added");
    assertTrue(filters.getAllFilters().isEmpty());
  }

  @Test
  void addFilterWithValidSelectorShouldStoreFilterAndExecutorShouldNotBeNull() {
    NamespaceFilters filters = new NamespaceFilters();

    // Use something extremely basic. If your selector grammar differs, adjust to a known-good expression.
    NamespaceFilterDTO ok = dto("root/system", 0, "1 = 1", false);

    filters.addFilter(ok);

    NamespaceFilter match = filters.findMatch("/root/system/topic");
    assertNotNull(match);
    assertEquals("root/system", match.getConfig().getNamespace());
    assertNotNull(match.getExecutor(), "Valid selector should compile into an executor");
  }

  private NamespaceFilterDTO dto(String namespace, int depth, String selector, boolean forcePriority) {
    NamespaceFilterDTO dto = new NamespaceFilterDTO();
    dto.setNamespace(namespace);
    dto.setDepth(depth);
    dto.setSelector(selector);
    dto.setForcePriority(forcePriority);
    return dto;
  }
}