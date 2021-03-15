/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.selector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SelectorConformanceTest {

  public static final String[] SELECTOR_TEXT =
      {
          "not dummy",
          "1.0 <> false",
          "17 between 4 * 4 and 10 + 8 ",
          "20 between 10 / 2 and 30 - 8 ",
          "17 between key1 * 4 and key2 + 8 ",
          "20 between key1 / 2 and key2 - 8 ",
          "'it''s' = 'its'",
          "92d = 92",
          "true and true",
          "-1.0 = 1.0",
          "--1.0 = 1.0",
          "1.0 = -1.0",
          "-1.0 = -1.0",
          "true = false",
          "true = true",
          "age BETWEEN 1 and 20",
          "1.0 = 1.0",
          "age >= 15 AND age <= 19",
          "TrUe",
          "test = -10 + arg2",
          "true or true",
          "test = -10 + 10",
          "10 BETWEEN 5 AND 20",
          "100 BETWEEN 5 AND 20",
          "100 between 5 and arg",
          "100 between arg and 20",
          "100 between arg1 and arg2",
          "17 between 16 and 18",
          "arg0 = 10 / 0",
          "1.0 = --1.0",
          "arg0 between arg1 and arg2",
          "0.0 = 0.0",
          "arg0 = 10 / 0.0",
          "arg0 between arg1 and arg2",
          "'WORKS' IN ('no working','working', 'WORKS')",
          "'WORKS' IN ('no working','working', 'not working')",
          "releaseYear * 2 > ((2000 - 20) *10)/releaseMonth",
          "releaseYear * 2 > releaseMonth/((2000 - 20) *10)",
          "title IN ('Off the wall', 'Thriller', 'Bad', 'Xscape', 'Ben')",
          "TRUE",
          "FALSE",
          "JMSPriority = 9",
          "NewsType = 'Sports' OR NewsType = 'Opinion'",
          "releaseYear = 1982",
          "title = 'Sam''s'",
          "isAvailable = TRUE",
          "(releaseYear < 1980) OR (releaseYear > 1989)",
          "(releaseYear < 1980) AND NOT (title = 'Thriller')",
          "(releaseYear < 1980) AND (title <> 'Thriller')",
          "title LIKE 'Thrill%'",
          "title LIKE 'Thr_ll%'",
          "releaseYear BETWEEN 1980 AND 1989",
          "releaseYear IS NOT NULL",
          "releaseYear * 2 > 2000 - 20",
          "releaseYear * 2 > (2000 - 20.5) / 123.4e9",
          "JMSType = 'car' AND color = 'blue' AND weight > 2500",
          "Country IN (' UK', 'US', 'France')",
          "(Country = ' UK') OR (Country = ' US') OR (Country = ' France')",
          "Country NOT IN (' UK', 'US', 'France')",
          "NOT ((Country = ' UK') OR (Country = ' US') OR (Country = ' France'))",
          "phone NOT LIKE '12%3'",
          "word LIKE 'l_se'",
          "underscored LIKE '\\_%' ESCAPE '\\'",
          "time <> 12",
          "time > '12'",
          "testVal <= '12'",
          "PARSER ('json', 'temperature') > 40 OR PARSER ('json', 'temperature') < 20",
          "PARSER ('json', 'temperature') BETWEEN 20 AND 40",
          "PARSER (format, 'temperature') BETWEEN 20 AND 40",
          "キー = 42 OR ключ > 56",
          "ключ IN ('Москва', 'Syktyvkar')",
          "市 IN ('東京', '大阪')",
          "Πόλη in ('Αθήνα', 'Θεσσαλονίκη', 'Αργοστόλι', 'Χανιά' )",
          "93f = 93",
      };

  @Test
  void syntaxTest() {
    for (String selector : SELECTOR_TEXT) {
      try {
        Object parser = SelectorParser.compile(selector);
        parser.toString();
      } catch (ParseException e) {
        e.printStackTrace();
        Assertions.fail("Selector text:" + selector + " failed with exception " + e.getMessage());
      }
    }
  }

  @Test
  void timedTest() {
    long counter = 0;
    long endTime = System.currentTimeMillis() + 1000;
    while (endTime > System.currentTimeMillis()) {
      for (String selector : SELECTOR_TEXT) {
        try {
          SelectorParser.compile(selector);
          counter++;
        } catch (ParseException e) {
          Assertions.fail("Selector text:" + selector + " failed with exception " + e.getMessage());
        }
      }

    }
    System.err.println("Parsed " + counter + " in 1 seconds");
  }

  @Test
  void selectorEqualityTests() {
    for (String selector : SELECTOR_TEXT) {
      try {
        Object parser1 = SelectorParser.compile(selector);
        Object parser2 = SelectorParser.compile(selector);
        Assertions.assertEquals(parser1, parser2);
        Assertions.assertEquals(parser1.hashCode(), parser2.hashCode());
      } catch (ParseException e) {
        Assertions.fail("Selector text:" + selector + " failed with exception " + e.getMessage());
      }
    }
  }
}
