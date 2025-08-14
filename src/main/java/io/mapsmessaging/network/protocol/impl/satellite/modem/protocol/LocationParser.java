/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.Sentence;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.SentenceFactory;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LocationParser {

  private final SentenceFactory sentenceFactory;

  public LocationParser() {
    ConfigurationProperties configurationProperties = ConfigurationManager.getInstance().getProperties("NMEA-0183");
    sentenceFactory = new SentenceFactory((ConfigurationProperties) configurationProperties.get("sentences"));
  }

  public Sentence parseLocation(String location) {
    List<String> values = List.of(location.split(","));
    Iterator<String> itr = new LinkedList<>(values).iterator();
    String id = itr.next().substring(1);
    return sentenceFactory.parse(id, itr);
  }
}
