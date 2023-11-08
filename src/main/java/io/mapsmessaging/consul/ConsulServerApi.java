/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.consul;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.rest.RestApiServerManager;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_CLIENT_EXCEPTION;
import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_INVALID_KEY;

public abstract class ConsulServerApi implements Runnable {
  private static final Pattern VALID_KEY_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-._~/]+$");


  private final Logger logger = LoggerFactory.getLogger(ConsulServerApi.class);

  protected final List<String> serviceIds;
  protected final String uniqueName;
  protected final ConsulConfiguration consulConfiguration;

  protected Future<?> scheduledTask;

  protected ConsulServerApi(String name) {
    serviceIds = new ArrayList<>();
    uniqueName = name;
    serviceIds.add(name);
    consulConfiguration = new ConsulConfiguration();
  }

  public String scanForDefaultConfig(String namespace) {
    if (!namespace.endsWith("/")) {
      namespace = namespace + "/";
    }
    try {
      while (namespace.contains("/")) { // we have a depth
        String lookup = namespace + "default";
        List<String> keys = getKeys(lookup);
        if (keys != null && !keys.isEmpty()) {
          return lookup;
        }
        namespace = namespace.substring(0, namespace.length() - 1); // Remove the "/"
        int idx = namespace.lastIndexOf("/");
        if (idx >= 0 && namespace.length() > 1) {
          namespace = namespace.substring(0, idx + 1); // Include the /
        } else {
          break;
        }
      }
    } catch (Throwable exceptionInInitializerError) {
      logger.log(CONSUL_CLIENT_EXCEPTION, exceptionInInitializerError);
    }
    return "";
  }

  public void stop() {
    if (scheduledTask != null) {
      logger.log(ServerLogMessages.CONSUL_SHUTDOWN);
      scheduledTask.cancel(false);
    }
  }

  protected void registerPingTask() {
    scheduledTask = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, Constants.HEALTH_TIME, Constants.HEALTH_TIME, TimeUnit.SECONDS);
  }

  public void run() {
    if (consulConfiguration.registerAgent()) {
      pingService();
    }
  }

  protected abstract void pingService();

  public abstract void register(Map<String, String> meta);

  public abstract void register(EndPointServer endPointServer);

  public abstract void register(RestApiServerManager restApiServerManager);

  public String getUrlPath() {
    return consulConfiguration.getUrlPath();
  }

  public abstract List<String> getKeys(String key) throws IOException;

  public abstract String getValue(String key) throws IOException;

  public abstract void putValue(String key, String value);

  public abstract void deleteKey(String key);

  protected String validateKey(String keyName) {
    if (VALID_KEY_NAME_PATTERN.matcher(keyName).matches()) {
      return keyName;
    }
    String fixed = keyName.replaceAll("[^a-zA-Z0-9-._~/]", "");
    logger.log(CONSUL_INVALID_KEY, keyName, fixed);
    return fixed;
  }
}
