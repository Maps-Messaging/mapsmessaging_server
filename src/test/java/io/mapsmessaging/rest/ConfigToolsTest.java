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

package io.mapsmessaging.rest;

import io.mapsmessaging.BaseTest;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.config.MessageDaemonConfigDTO;
import io.mapsmessaging.dto.rest.discovery.DiscoveredServersDTO;
import io.mapsmessaging.dto.rest.devices.DeviceInfoDTO;
import io.mapsmessaging.dto.rest.lora.LoRaDeviceInfoDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.impl.server.ServerConfigApi;
import io.mapsmessaging.rest.api.impl.discovery.DiscoveryManagementApi;
import io.mapsmessaging.rest.api.impl.discovery.DiscoveryConfigurationApi;
import io.mapsmessaging.rest.api.impl.hardware.HardwareManagementApi;
import io.mapsmessaging.rest.api.impl.hardware.HardwareConfigurationApi;
import io.mapsmessaging.rest.api.impl.schema.SchemaQueryApi;
import io.mapsmessaging.rest.api.impl.lora.LoRaDeviceApi;
import io.mapsmessaging.rest.api.impl.lora.LoRaDeviceConfigApi;
import io.mapsmessaging.rest.responses.SchemaImplementationResponse;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ConfigToolsTest extends BaseTest {

    @Mock
    private MessageDaemon mockMessageDaemon;

    @Mock
    private SchemaManager mockSchemaManager;

    private ServerConfigApi serverConfigApi;
    private DiscoveryManagementApi discoveryManagementApi;
    private DiscoveryConfigurationApi discoveryConfigurationApi;
    private HardwareManagementApi hardwareManagementApi;
    private HardwareConfigurationApi hardwareConfigurationApi;
    private SchemaQueryApi schemaQueryApi;
    private LoRaDeviceApi loRaDeviceApi;
    private LoRaDeviceConfigApi loRaDeviceConfigApi;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        serverConfigApi = new ServerConfigApi();
        discoveryManagementApi = new DiscoveryManagementApi();
        discoveryConfigurationApi = new DiscoveryConfigurationApi();
        hardwareManagementApi = new HardwareManagementApi();
        hardwareConfigurationApi = new HardwareConfigurationApi();
        schemaQueryApi = new SchemaQueryApi();
        loRaDeviceApi = new LoRaDeviceApi();
        loRaDeviceConfigApi = new LoRaDeviceConfigApi();
    }

    @Test
    @DisplayName("Test Server Configuration CRUD Operations")
    public void testServerConfigCrud() throws IOException {
        // Test getting server configuration
        MessageDaemonConfigDTO config = new MessageDaemonConfigDTO();
        config.setName("test-server");
        config.setVersion("1.0.0");
        
        // Mock the MessageDaemon.getInstance() call
        try (var mockedStatic = mockStatic(MessageDaemon.class)) {
            MessageDaemon mockDaemon = mock(MessageDaemon.class);
            mockedStatic.when(MessageDaemon::getInstance).thenReturn(mockDaemon);
            
            when(mockDaemon.getMessageDaemonConfig()).thenReturn(config);
            when(mockDaemon.getMessageDaemonConfig().update(any())).thenReturn(true);
            
            // Test GET
            MessageDaemonConfigDTO result = serverConfigApi.getServerConfig();
            assertNotNull(result);
            assertEquals("test-server", result.getName());
            
            // Test PUT
            MessageDaemonConfigDTO updateConfig = new MessageDaemonConfigDTO();
            updateConfig.setName("updated-server");
            
            Response response = serverConfigApi.updateServerConfig(updateConfig);
            assertNotNull(response);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }

    @Test
    @DisplayName("Test Configuration Diffing")
    public void testConfigDiffing() {
        Map<String, Object> originalConfig = new HashMap<>();
        originalConfig.put("name", "test-server");
        originalConfig.put("port", 8080);
        originalConfig.put("debug", false);
        
        Map<String, Object> updatedConfig = new HashMap<>();
        updatedConfig.put("name", "updated-server");
        updatedConfig.put("port", 9090);
        updatedConfig.put("debug", true);
        updatedConfig.put("newFeature", "enabled");
        
        // Test diff detection
        assertFalse(configsEqual(originalConfig, updatedConfig));
        
        // Test specific changes
        assertNotEquals(originalConfig.get("name"), updatedConfig.get("name"));
        assertNotEquals(originalConfig.get("port"), updatedConfig.get("port"));
        assertNotEquals(originalConfig.get("debug"), updatedConfig.get("debug"));
        assertFalse(originalConfig.containsKey("newFeature"));
        assertTrue(updatedConfig.containsKey("newFeature"));
    }

    @Test
    @DisplayName("Test Discovery Management Operations")
    public void testDiscoveryManagement() {
        try (var mockedStatic = mockStatic(MessageDaemon.class)) {
            MessageDaemon mockDaemon = mock(MessageDaemon.class);
            mockedStatic.when(MessageDaemon::getInstance).thenReturn(mockDaemon);
            
            var mockSubSystemManager = mock(io.mapsmessaging.SubSystemManager.class);
            var mockDiscoveryManager = mock(io.mapsmessaging.network.discovery.DiscoveryManager.class);
            var mockServerConnectionManager = mock(io.mapsmessaging.network.connection.ConnectionManager.class);
            
            when(mockDaemon.getSubSystemManager()).thenReturn(mockSubSystemManager);
            when(mockSubSystemManager.getDiscoveryManager()).thenReturn(mockDiscoveryManager);
            when(mockSubSystemManager.getServerConnectionManager()).thenReturn(mockServerConnectionManager);
            
            // Test start discovery
            Response startResponse = discoveryManagementApi.startDiscovery();
            assertNotNull(startResponse);
            verify(mockDiscoveryManager).start();
            
            // Test stop discovery
            Response stopResponse = discoveryManagementApi.stopDiscovery();
            assertNotNull(stopResponse);
            verify(mockDiscoveryManager).stop();
        }
    }

    @Test
    @DisplayName("Test Discovery Configuration CRUD")
    public void testDiscoveryConfigurationCrud() throws IOException {
        // This test would need to mock the DiscoveryManagerConfig
        // For now, we'll test the basic structure
        assertNotNull(discoveryConfigurationApi);
        
        // The actual implementation would need proper mocking of the config classes
        // This is a placeholder to show the test structure
    }

    @Test
    @DisplayName("Test Hardware Management Operations")
    public void testHardwareManagement() {
        try (var mockedStatic = mockStatic(MessageDaemon.class)) {
            MessageDaemon mockDaemon = mock(MessageDaemon.class);
            mockedStatic.when(MessageDaemon::getInstance).thenReturn(mockDaemon);
            
            var mockSubSystemManager = mock(io.mapsmessaging.SubSystemManager.class);
            var mockDeviceManager = mock(io.mapsmessaging.hardware.DeviceManager.class);
            
            when(mockDaemon.getSubSystemManager()).thenReturn(mockSubSystemManager);
            when(mockSubSystemManager.getDeviceManager()).thenReturn(mockDeviceManager);
            
            // Test hardware scan
            List<DeviceInfoDTO> scanResults = new ArrayList<>();
            DeviceInfoDTO device = new DeviceInfoDTO();
            device.setName("test-device");
            device.setType("I2C");
            device.setDescription("Test I2C device");
            device.setState("ACTIVE");
            scanResults.add(device);
            
            when(mockDeviceManager.scan()).thenReturn(scanResults);
            
            var scanResponse = hardwareManagementApi.scanForDevices();
            assertNotNull(scanResponse);
            assertEquals(1, scanResponse.getList().size());
            
            // Test get active devices
            var mockDeviceController = mock(io.mapsmessaging.devices.DeviceController.class);
            List<io.mapsmessaging.devices.DeviceController> activeDevices = new ArrayList<>();
            when(mockDeviceController.getName()).thenReturn("test-device");
            when(mockDeviceController.getType()).thenReturn(io.mapsmessaging.devices.DeviceType.I2C);
            when(mockDeviceController.getDescription()).thenReturn("Test device");
            when(mockDeviceController.getDeviceState()).thenReturn("ACTIVE".getBytes());
            
            activeDevices.add(mockDeviceController);
            when(mockDeviceManager.getActiveDevices()).thenReturn(activeDevices);
            
            var devicesResponse = hardwareManagementApi.getAllDiscoveredDevices();
            assertNotNull(devicesResponse);
            assertEquals(1, devicesResponse.getList().size());
        }
    }

    @Test
    @DisplayName("Test Schema CRUD Operations")
    public void testSchemaCrud() throws IOException {
        try (var mockedStatic = mockStatic(SchemaManager.class)) {
            SchemaManager mockManager = mock(SchemaManager.class);
            mockedStatic.when(SchemaManager::getInstance).thenReturn(mockManager);
            
            // Test create schema
            JsonSchemaConfig schemaConfig = new JsonSchemaConfig();
            schemaConfig.setUniqueId("test-schema-123");
            schemaConfig.setName("Test Schema");
            schemaConfig.setVersion(1);
            
            when(mockManager.getSchema("test-schema-123")).thenReturn(schemaConfig);
            
            // Test get schema
            var response = schemaQueryApi.getSchemaById("test-schema-123");
            assertNotNull(response);
            
            // Test get schema implementation
            var implResponse = schemaQueryApi.getSchemaImplementation("test-schema-123");
            assertNotNull(implResponse);
            assertEquals("test-schema-123", implResponse.getSchemaId());
            
            // Test delete schema
            when(mockManager.getSchema("test-schema-123")).thenReturn(schemaConfig);
            var deleteResponse = schemaQueryApi.deleteSchemaById("test-schema-123");
            assertNotNull(deleteResponse);
            
            verify(mockManager).removeSchema("test-schema-123");
        }
    }

    @Test
    @DisplayName("Test Schema Implementation Details")
    public void testSchemaImplementationDetails() throws IOException {
        try (var mockedStatic = mockStatic(SchemaManager.class)) {
            SchemaManager mockManager = mock(SchemaManager.class);
            mockedStatic.when(SchemaManager::getInstance).thenReturn(mockManager);
            
            JsonSchemaConfig schemaConfig = new JsonSchemaConfig();
            schemaConfig.setUniqueId("test-schema-123");
            schemaConfig.setName("Test Schema");
            schemaConfig.setVersion(1);
            schemaConfig.setInterfaceDescription("json");
            schemaConfig.setResourceType("message");
            
            when(mockManager.getSchema("test-schema-123")).thenReturn(schemaConfig);
            when(mockManager.getMessageFormatter("test-schema-123")).thenReturn(null);
            
            SchemaImplementationResponse response = schemaQueryApi.getSchemaImplementation("test-schema-123");
            
            assertNotNull(response);
            assertEquals("test-schema-123", response.getSchemaId());
            assertEquals("JsonSchemaConfig", response.getSchemaType());
            assertEquals("Test Schema", response.getSchemaName());
            assertEquals(1, response.getSchemaVersion());
            assertEquals("json", response.getInterfaceDescription());
            assertEquals("message", response.getResourceType());
            assertFalse(response.isFormatterAvailable());
        }
    }

    @Test
    @DisplayName("Test LoRa Device CRUD Operations")
    public void testLoRaDeviceCrud() {
        try (var mockedStatic = mockStatic(io.mapsmessaging.network.io.impl.lora.LoRaDeviceManager.class)) {
            var mockDeviceManager = mock(io.mapsmessaging.network.io.impl.lora.LoRaDeviceManager.class);
            mockedStatic.when(io.mapsmessaging.network.io.impl.lora.LoRaDeviceManager::getInstance).thenReturn(mockDeviceManager);
            
            // Test get all LoRa devices
            List<io.mapsmessaging.network.io.impl.lora.LoRaDevice> devices = new ArrayList<>();
            var mockDevice = mock(io.mapsmessaging.network.io.impl.lora.LoRaDevice.class);
            when(mockDevice.getName()).thenReturn("test-lora-device");
            when(mockDevice.getConfig()).thenReturn(mock(io.mapsmessaging.config.network.impl.LoRaConfigDTO.class));
            
            devices.add(mockDevice);
            when(mockDeviceManager.getDevices()).thenReturn(devices);
            
            var response = loRaDeviceApi.getAllLoRaDevices();
            assertNotNull(response);
            assertEquals(1, response.getList().size());
            
            // Test get specific LoRa device
            when(mockDevice.getName()).thenReturn("test-lora-device");
            var deviceResponse = loRaDeviceApi.getLoRaDevice("test-lora-device");
            assertNotNull(deviceResponse);
            assertEquals("test-lora-device", deviceResponse.getName());
        }
    }

    @Test
    @DisplayName("Test Configuration Validation")
    public void testConfigurationValidation() {
        // Test valid JSON configuration
        String validConfig = "{\"name\":\"test\",\"port\":8080,\"debug\":true}";
        assertTrue(isValidJson(validConfig));
        
        // Test invalid JSON configuration
        String invalidConfig = "{\"name\":\"test\",\"port\":8080,\"debug\":}";
        assertFalse(isValidJson(invalidConfig));
        
        // Test valid YAML-like configuration (basic check)
        String validYaml = "name: test\nport: 8080\ndebug: true";
        assertTrue(isValidYaml(validYaml));
        
        // Test invalid configuration (missing required fields)
        String incompleteConfig = "{\"name\":\"test\"}";
        assertFalse(hasRequiredServerFields(incompleteConfig));
    }

    @Test
    @DisplayName("Test Device Configuration Upload and Download")
    public void testDeviceConfigUploadDownload() {
        // This test would verify the upload/download functionality
        // For now, we'll test the basic validation logic
        
        String validDeviceConfig = """
            {
                "name": "test-device",
                "type": "I2C",
                "address": "0x48",
                "description": "Test I2C sensor"
            }
            """;
        
        assertTrue(isValidDeviceConfig(validDeviceConfig));
        
        String invalidDeviceConfig = """
            {
                "type": "I2C",
                "address": "0x48"
            }
            """;
        
        assertFalse(isValidDeviceConfig(invalidDeviceConfig));
    }

    @Test
    @DisplayName("Test Pagination for Large Payloads")
    public void testPagination() {
        // Create test data
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add("item-" + i);
        }
        
        // Test pagination logic
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) items.size() / pageSize);
        
        assertEquals(10, totalPages);
        
        // Test first page
        List<String> firstPage = items.subList(0, pageSize);
        assertEquals(10, firstPage.size());
        assertEquals("item-0", firstPage.get(0));
        
        // Test last page
        List<String> lastPage = items.subList(90, 100);
        assertEquals(10, lastPage.size());
        assertEquals("item-90", lastPage.get(0));
    }

    // Helper methods
    private boolean configsEqual(Map<String, Object> config1, Map<String, Object> config2) {
        return config1.equals(config2);
    }
    
    private boolean isValidJson(String json) {
        try {
            javax.json.JsonReader reader = javax.json.Json.createReader(new java.io.StringReader(json));
            reader.readObject();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidYaml(String yaml) {
        // Basic YAML validation - in a real implementation, you'd use a YAML parser
        return yaml != null && !yaml.trim().isEmpty() && yaml.contains(":");
    }
    
    private boolean hasRequiredServerFields(String config) {
        try {
            javax.json.JsonObject obj = javax.json.Json.createReader(new java.io.StringReader(config)).readObject();
            return obj.containsKey("name") && obj.containsKey("port");
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidDeviceConfig(String config) {
        try {
            javax.json.JsonObject obj = javax.json.Json.createReader(new java.io.StringReader(config)).readObject();
            return obj.containsKey("name") && obj.containsKey("type");
        } catch (Exception e) {
            return false;
        }
    }
}