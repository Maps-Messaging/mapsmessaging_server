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

@Getter
public class DestinationPathManager {

  private static final String OPTIONAL_PATH = "{folder}";

  private final long partitionSize;
  private final long idleTime;
  private final int itemCount;
  private final int expiredEventPoll;
  private final boolean enableSync;
  private final boolean remap;
  private final boolean writeThrough;
  private final boolean enableCache;
  private final String name;
  private final String directory;
  private final String namespaceMapping;
  private final String type;
  private final String cacheType;
  private final String archiveName;
  private final long archiveIdleTime;
  private final String digestAlgorithm;
  private final String s3RegionName;
  private final String s3AccessKeyId;
  private final String s3SecretAccessKey;
  private final String s3BucketName;
  private final boolean s3Compression;
  private final boolean debug;


  public DestinationPathManager(ConfigurationProperties properties) {
    name = properties.getProperty("name");
    type = properties.getProperty("type", "File");
    debug = properties.getBooleanProperty("debug", false);

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
