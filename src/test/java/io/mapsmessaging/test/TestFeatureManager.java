package io.mapsmessaging.test;

import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.license.features.Features;

import java.util.List;

public class TestFeatureManager extends FeatureManager {
  public TestFeatureManager(List<Features> featuresList) {
    super(featuresList);
  }

  public boolean isEnabled(String featurePath){
    return true;
  }

}
