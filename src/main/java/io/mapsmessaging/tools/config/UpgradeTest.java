package io.mapsmessaging.tools.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;

import java.util.ArrayList;

public class UpgradeTest {

  public static void main(String[] args){
    Gson gson = new Gson();
    ConfigurationManager configurationManager = ConfigurationManager.getInstance();
    configurationManager.setFeatureManager(new FeatureManager(new ArrayList<>()));
    configurationManager.initialise("fred");
    configurationManager.loadAll();
    for(String managerName: configurationManager.getKnownManagers()){
      try {
        ConfigManager manager = configurationManager.getManager(managerName);
        manager.load(new FeatureManager(null));
        JsonObject managerJson = gson.toJsonTree(manager).getAsJsonObject();
        ConfigManager managerCopy = gson.fromJson(managerJson, manager.getClass());
        JsonObject copyJson = gson.toJsonTree(managerCopy).getAsJsonObject();
        if(!managerJson.toString().equals(copyJson.toString())) {
          System.err.println("Manager: " + managerName +
              "\n Original: " + managerJson +
              "\n Copy    : " + copyJson +
              "\n Equal   :" + managerJson.toString().equals(copyJson.toString()));
        }
      }
      catch(Exception ex){
        System.err.println("Failed: " + managerName);
        ex.printStackTrace();
      }
    }
  }
}
