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

import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.utilities.Agent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Discovers applications and collects their pre-initialization metadata.
 * The owning subsystem manager registers the host in its existing lifecycle.
 */
public class ModuleManager implements Agent {

  private final Iterable<? extends MapsModule> providers;
  private final List<MapsModule> applications;
  private final List<String> restApiPackages;
  private final List<ManagerConfigProvider> configProviders;
  private boolean loaded;
  private boolean started;

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
   * Reads metadata before initializing any application. This method is single-use.
   */
  public void load(ModuleContext context) {
    if (loaded) {
      throw new IllegalStateException("Applications have already been loaded");
    }
    Objects.requireNonNull(context, "context");
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
    for (MapsModule application : discovered) {
      application.initialize(context);
    }
    applications.addAll(discovered);
    restApiPackages.addAll(packages);
    configProviders.addAll(configs);
    loaded = true;
  }

  @Override
  public String getName() {
    return "Application Host";
  }

  @Override
  public String getDescription() {
    return "Manages optional application subsystems";
  }

  @Override
  public void start() {
    if (!loaded) {
      throw new IllegalStateException("Applications have not been loaded");
    }
    List<MapsModule> ordered = new ArrayList<>(applications);
    ordered.sort(Comparator.comparingInt(MapsModule::getStartPriority));
    for (MapsModule application : ordered) {
      application.start();
    }
    started = true;
  }

  @Override
  public void stop() {
    List<MapsModule> ordered = new ArrayList<>(applications);
    ordered.sort(Comparator.comparingInt(MapsModule::getStopPriority));
    for (MapsModule application : ordered) {
      application.stop();
    }
    started = false;
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setStatus(started ? Status.OK : Status.STOPPED);
    for (MapsModule application : applications) {
      SubSystemStatusDTO applicationStatus = application.getStatus();
      if (applicationStatus.getStatus() == Status.ERROR) {
        status.setStatus(Status.ERROR);
        break;
      }
      if (applicationStatus.getStatus() == Status.WARN && status.getStatus() == Status.OK) {
        status.setStatus(Status.WARN);
      }
    }
    return status;
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
