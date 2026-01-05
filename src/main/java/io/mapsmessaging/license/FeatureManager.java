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

package io.mapsmessaging.license;

import io.mapsmessaging.license.features.Features;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;

import java.lang.reflect.Field;
import java.util.List;

public class FeatureManager {

  private final Logger logger = LoggerFactory.getLogger(FeatureManager.class);
  private final List<FeatureDetails> featuresList;

  public FeatureManager(List<FeatureDetails> featuresList) {
    this.featuresList = featuresList;
  }

  public boolean isEnabled(String featurePath) {
    for (FeatureDetails featureDetails : featuresList) {
      Features feature = featureDetails.getFeature();
      if(feature.isOverrideFeatures() && !featurePath.equalsIgnoreCase("ml")){
        return true;
      }
      Object value = getFieldValue(feature, featurePath);
      if (value instanceof Boolean && ((boolean) value)) {
        return true;
      }
      else{
        logger.log(ServerLogMessages.LICENSE_DISABLED_FEATURE_KEY, featurePath);
      }
    }
    return false;
  }

  public String getLoadedLicenses(){
    StringBuilder loadedLicenses = new StringBuilder();
    for(FeatureDetails features : featuresList){
      loadedLicenses.append(features.getFeature().getName()).append(", ");
    }
    return loadedLicenses.toString();
  }

  public String getLoadedInfo(){
    StringBuilder loadedLicenses = new StringBuilder();
    for(FeatureDetails features : featuresList){
      loadedLicenses.append(features.getInfo()).append(", ");
    }
    return loadedLicenses.toString();
  }

  public int getMaxValue(String featurePath) {
    int maxValue = 0;
    for (FeatureDetails features : featuresList) {
      Object value = getFieldValue(features.getFeature(), featurePath);
      if (value instanceof Integer) {
        maxValue = Math.max(maxValue, (Integer) value);
      }
    }
    return maxValue;
  }

  private Object getFieldValue(Object obj, String featurePath) {
    try {
      String[] parts = featurePath.split("\\.");
      for (String part : parts) {
        if (obj == null) {
          logger.log(ServerLogMessages.LICENSE_UNKNOWN_FEATURE_KEY, featurePath);
          return null;
        }
        Field field = obj.getClass().getDeclaredField(part);
        field.setAccessible(true);
        obj = field.get(obj);
      }
      return obj;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      logger.log(ServerLogMessages.LICENSE_UNKNOWN_FEATURE_KEY, featurePath);
      return null;
    }
  }

}
