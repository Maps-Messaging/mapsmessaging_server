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

package org.maps.utilities.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.maps.BaseTest;

class EnvironmentConfigTests extends BaseTest {

  @Test
  void checkDefaultVersionLoad(){
    EnvironmentConfig config = EnvironmentConfig.getInstance();
    Assertions.assertNotEquals(config.getMajor(), -1);
  }

  @Test
  void checkDifferentJDKVersionParsing(){
    String[] versions = {
        "1.3.0",
        "1.3.1-beta",
        "1.3.1_05-ea",
        "1.4.0_03",
        "1.4.0_03-ea",
        "1.5.0",
        "1.5.0-b64",
        "1.6.0",
        "1.7.0",
        "1.8.0",
        "9.1.2",
        "9.1.2.1",
        "1.9.0-ea-b19",
        "9-ea",
        "9+100",
        "9.1.2+62",
        "10-ea",
        "11.0.2",
        "10"
    };
    int[] results = {3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 9, 9, 9, 9, 9, 10, 11, 10};

    EnvironmentConfig config = EnvironmentConfig.getInstance();
    for(int x=0;x<versions.length;x++){
      Assertions.assertEquals(results[x], config.getVersion(versions[x]));
    }
  }

}
