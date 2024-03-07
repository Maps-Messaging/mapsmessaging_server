/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.configuration.ConfigurationProperties;
import lombok.Getter;

public class DestinationPathManager {

  private static final String OPTIONAL_PATH = "{folder}";

  @Getter
  private final long partitionSize;
  @Getter
  private final long idleTime;

  @Getter
  private final int itemCount;
  @Getter
  private final int expiredEventPoll;

  @Getter
  private final boolean enableSync;
  @Getter
  private final boolean remap;
  @Getter
  private final boolean writeThrough;
  @Getter
  private final boolean enableCache;

  @Getter
  private final String name;
  @Getter
  private final String directory;
  @Getter
  private final String namespaceMapping;
  @Getter
  private final String type;
  @Getter
  private final String cacheType;
  @Getter
  private final String archiveName;
  @Getter
  private final long archiveIdleTime;
  @Getter
  private final String digestAlgorithm;

  @Getter
  private final String s3RegionName;
  @Getter
  private final String s3AccessKeyId;
  @Getter
  private final String s3SecretAccessKey;
  @Getter
  private final String s3BucketName;
  @Getter
  private final boolean s3Compression;


  public DestinationPathManager(ConfigurationProperties properties) {
    name = properties.getProperty("name");
    type = properties.getProperty("type", "File");

    String propertyNamespace = properties.getProperty("namespace");
    String tmp = properties.getProperty("directory");
    if (tmp == null) {
      tmp = "";
    }
    directory = tmp;
    remap = (propertyNamespace.endsWith(OPTIONAL_PATH) && directory.contains(OPTIONAL_PATH));
    if (remap) {
      namespaceMapping = propertyNamespace.substring(0, propertyNamespace.indexOf(OPTIONAL_PATH));
    } else {
      namespaceMapping = propertyNamespace;
    }
    enableSync = properties.getBooleanProperty("sync", false);
    itemCount = properties.getIntProperty("itemCount", 524_288);
    partitionSize = properties.getLongProperty("maxPartitionSize", 4_294_967_296L);
    idleTime = properties.getLongProperty("autoPauseTimeout", 0L);
    expiredEventPoll = properties.getIntProperty("expiredEventPoll", 1);

    if (properties.containsKey("cache")) {
      ConfigurationProperties cacheProps = (ConfigurationProperties) properties.get("cache");
      enableCache = true;
      cacheType = cacheProps.getProperty("type", "WeakReference");
      writeThrough = cacheProps.getBooleanProperty("writeThrough", false);
    } else {
      cacheType = "";
      writeThrough = false;
      enableCache = false;
    }
    if(properties.containsKey("archive")){
      ConfigurationProperties archiveProps = (ConfigurationProperties) properties.get("archive");
      archiveName = archiveProps.getProperty("name");
      archiveIdleTime = archiveProps.getLongProperty("idleTime", -1);
      digestAlgorithm = archiveProps.getProperty("digestAlgorithm", "MD5");
      if(archiveProps.containsKey("S3") && archiveName.equalsIgnoreCase("s3")){
        ConfigurationProperties s3Props = (ConfigurationProperties) properties.get("s3");
        s3AccessKeyId = s3Props.getProperty("accesskeyId");
        s3BucketName = s3Props.getProperty("bucketName");
        s3SecretAccessKey = s3Props.getProperty("secretAccessKey");
        s3Compression =  s3Props.getBooleanProperty("compression", false);
        s3RegionName = s3Props.getProperty("regionName");
      }
      else{
        s3AccessKeyId = "";
        s3BucketName = "";
        s3SecretAccessKey = "";
        s3Compression =  false;
        s3RegionName = "";
      }
    }
    else{
      archiveName = "None";
      archiveIdleTime = -1;
      s3AccessKeyId = "";
      s3BucketName = "";
      s3SecretAccessKey = "";
      s3Compression =  false;
      s3RegionName = "";
      digestAlgorithm = "";
    }
  }

  public String getTrailingPath() {
    if (remap) {
      return directory.substring(directory.indexOf(OPTIONAL_PATH) + OPTIONAL_PATH.length());
    }
    return "";
  }

}
