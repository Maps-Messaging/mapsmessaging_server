/*
 *
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

package io.mapsmessaging.modules;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Discovers modules and collects their pre-initialization metadata.
 * The owning subsystem manager registers each module in its existing lifecycle.
 */
public class ModuleManager {

  private final Iterable<? extends MapsModule> providers;
  private final List<MapsModule> applications;
  private final List<String> restApiPackages;
  private final List<ManagerConfigProvider> configProviders;
  private boolean loaded;
  private boolean initialized;

  public ModuleManager() {
    this(ServiceLoader.load(MapsModule.class));
  }

  public ModuleManager(Iterable<? extends MapsModule> providers) {
    this.providers = Objects.requireNonNull(providers, "providers");
    applications = new ArrayList<>();
    restApiPackages = new ArrayList<>();
    configProviders = new ArrayList<>();
  }

  /**
   * Discovers module declarations without initializing runtime services.
   * Metadata is available immediately after this call.
   */
  public void discover() {
    if (loaded) {
      throw new IllegalStateException("Modules have already been discovered");
    }
    Set<String> names = new HashSet<>();
    List<MapsModule> discovered = new ArrayList<>();
    List<String> packages = new ArrayList<>();
    List<ManagerConfigProvider> configs = new ArrayList<>();
    Set<String> types = new HashSet<>();
    for (MapsModule application : providers) {
      Objects.requireNonNull(application, "application");
      String name = Objects.requireNonNull(application.getName(), "application name");
      if (!names.add(name)) {
        throw new IllegalStateException("Duplicate application name: " + name);
      }
      List<String> applicationPackages = List.copyOf(application.getRestApiPackages());
      List<ManagerConfigProvider> applicationConfigs = List.copyOf(application.getConfigProviders());
      for (ManagerConfigProvider provider : applicationConfigs) {
        String type = Objects.requireNonNull(provider.getType(), "configuration type");
        Objects.requireNonNull(provider.getConfigClass(), "configuration class");
        if (!types.add(type)) {
          throw new IllegalStateException("Duplicate application configuration type: " + type);
        }
      }
      packages.addAll(applicationPackages);
      configs.addAll(applicationConfigs);
      discovered.add(application);
    }
    applications.addAll(discovered);
    restApiPackages.addAll(packages);
    configProviders.addAll(configs);
    loaded = true;
  }

  /**
   * Initializes discovered modules after their metadata has been made available.
   */
  public void initialize(ModuleContext context) {
    if (!loaded) {
      throw new IllegalStateException("Modules have not been discovered");
    }
    if (initialized) {
      throw new IllegalStateException("Modules have already been initialized");
    }
    Objects.requireNonNull(context, "context");
    for (MapsModule application : applications) {
      application.initialize(context);
    }
    initialized = true;
  }

  /**
   * Convenience operation preserving discovery followed by initialization.
   */
  public void load(ModuleContext context) {
    Objects.requireNonNull(context, "context");
    discover();
    initialize(context);
  }

  public List<MapsModule> getApplications() {
    return List.copyOf(applications);
  }

  public List<String> getRestApiPackages() {
    return List.copyOf(restApiPackages);
  }

  public List<ManagerConfigProvider> getConfigProviders() {
    return List.copyOf(configProviders);
  }
}
