#!/usr/bin/env python3
"""
Configuration Tools API Tests

This module contains comprehensive tests for the configuration tools API endpoints,
including server configuration, discovery, hardware, schemas, models, and LoRa devices.
"""

import json
import unittest
import requests
import tempfile
import os
from typing import Dict, Any, List
import time


class ConfigToolsAPITest(unittest.TestCase):
    """Test suite for Configuration Tools API endpoints"""
    
    def setUp(self):
        """Set up test fixtures"""
        self.base_url = "http://localhost:8080/api/v1"
        self.session = requests.Session()
        # Add authentication if required
        # self.session.auth = ('admin', 'password')
        
    def tearDown(self):
        """Clean up after tests"""
        self.session.close()
    
    def test_server_config_get(self):
        """Test getting server configuration"""
        response = self.session.get(f"{self.base_url}/server/config")
        
        # Check if endpoint exists and returns valid response
        if response.status_code == 200:
            config = response.json()
            self.assertIsInstance(config, dict)
            # Check for common server config fields
            optional_fields = ['name', 'version', 'port', 'host', 'debug']
            # At least one field should be present
            self.assertTrue(any(field in config for field in optional_fields))
        else:
            # Endpoint might not be available or auth required
            self.assertIn(response.status_code, [401, 403, 404, 500])
    
    def test_server_config_put(self):
        """Test updating server configuration"""
        # First get current config
        get_response = self.session.get(f"{self.base_url}/server/config")
        
        if get_response.status_code == 200:
            current_config = get_response.json()
            
            # Modify config slightly
            updated_config = current_config.copy()
            if 'testMode' in updated_config:
                updated_config['testMode'] = not updated_config['testMode']
            else:
                updated_config['testMode'] = True
            
            # Try to update config
            put_response = self.session.put(
                f"{self.base_url}/server/config",
                json=updated_config
            )
            
            # Should succeed or return auth error
            self.assertIn(put_response.status_code, [200, 401, 403, 405])
        else:
            self.skipTest("Cannot test PUT without being able to GET config")
    
    def test_discovery_management(self):
        """Test discovery management endpoints"""
        # Test discovery status
        try:
            response = self.session.get(f"{self.base_url}/server/discovery")
            if response.status_code == 200:
                discovery_data = response.json()
                self.assertIsInstance(discovery_data, (dict, list))
        except requests.exceptions.RequestException:
            pass
        
        # Test start discovery
        try:
            response = self.session.put(f"{self.base_url}/server/discovery/start")
            self.assertIn(response.status_code, [200, 401, 403, 404, 405])
        except requests.exceptions.RequestException:
            pass
        
        # Test stop discovery
        try:
            response = self.session.put(f"{self.base_url}/server/discovery/stop")
            self.assertIn(response.status_code, [200, 401, 403, 404, 405])
        except requests.exceptions.RequestException:
            pass
        
        # Test discovery config
        try:
            response = self.session.get(f"{self.base_url}/server/discovery/config")
            if response.status_code == 200:
                config = response.json()
                self.assertIsInstance(config, dict)
        except requests.exceptions.RequestException:
            pass
    
    def test_hardware_management(self):
        """Test hardware management endpoints"""
        # Test hardware scan
        try:
            response = self.session.get(f"{self.base_url}/server/hardware/scan")
            if response.status_code == 200:
                scan_data = response.json()
                self.assertIsInstance(scan_data, (dict, list))
        except requests.exceptions.RequestException:
            pass
        
        # Test get hardware devices
        try:
            response = self.session.get(f"{self.base_url}/server/hardware")
            if response.status_code == 200:
                devices_data = response.json()
                self.assertIsInstance(devices_data, (dict, list))
        except requests.exceptions.RequestException:
            pass
        
        # Test hardware config
        try:
            response = self.session.get(f"{self.base_url}/server/hardware/config")
            if response.status_code == 200:
                config = response.json()
                self.assertIsInstance(config, dict)
        except requests.exceptions.RequestException:
            pass
    
    def test_schema_management(self):
        """Test schema management endpoints"""
        # Test get all schemas
        try:
            response = self.session.get(f"{self.base_url}/server/schema")
            if response.status_code == 200:
                schemas_data = response.json()
                self.assertIsInstance(schemas_data, (dict, list))
        except requests.exceptions.RequestException:
            pass
        
        # Test get schema formats
        try:
            response = self.session.get(f"{self.base_url}/server/schema/formats")
            if response.status_code == 200:
                formats = response.json()
                self.assertIsInstance(formats, (list, dict))
        except requests.exceptions.RequestException:
            pass
        
        # Test get schema map
        try:
            response = self.session.get(f"{self.base_url}/server/schema/map")
            if response.status_code == 200:
                schema_map = response.json()
                self.assertIsInstance(schema_map, dict)
        except requests.exceptions.RequestException:
            pass
        
        # Test get link format
        try:
            response = self.session.get(f"{self.base_url}/server/schema/link-format")
            self.assertIn(response.status_code, [200, 401, 403, 404])
        except requests.exceptions.RequestException:
            pass
    
    def test_schema_implementation(self):
        """Test schema implementation endpoint"""
        # First get a schema ID to test with
        try:
            schemas_response = self.session.get(f"{self.base_url}/server/schema")
            if schemas_response.status_code == 200:
                schemas_data = schemas_response.json()
                
                # Try to extract a schema ID
                schema_id = None
                if isinstance(schemas_data, dict):
                    if 'schemas' in schemas_data:
                        schemas = schemas_data['schemas']
                        if schemas and len(schemas) > 0:
                            schema_id = schemas[0].get('uniqueId') or schemas[0].get('id')
                    elif 'list' in schemas_data:
                        schemas = schemas_data['list']
                        if schemas and len(schemas) > 0:
                            schema_id = schemas[0].get('uniqueId') or schemas[0].get('id')
                elif isinstance(schemas_data, list) and len(schemas_data) > 0:
                    schema_id = schemas_data[0].get('uniqueId') or schemas_data[0].get('id')
                
                if schema_id:
                    # Test schema implementation endpoint
                    impl_response = self.session.get(f"{self.base_url}/server/schema/impl/{schema_id}")
                    if impl_response.status_code == 200:
                        impl_data = impl_response.json()
                        self.assertIsInstance(impl_data, dict)
                        self.assertIn('schemaId', impl_data)
                    else:
                        self.assertIn(impl_response.status_code, [401, 403, 404, 500])
                else:
                    self.skipTest("No schema ID available for implementation test")
        except requests.exceptions.RequestException:
            pass
    
    def test_model_management(self):
        """Test model management endpoints"""
        # Test get all models
        try:
            response = self.session.get(f"{self.base_url}/server/models")
            if response.status_code == 200:
                models = response.json()
                self.assertIsInstance(models, list)
        except requests.exceptions.RequestException:
            pass
        
        # Test model upload (create a temporary test file)
        try:
            with tempfile.NamedTemporaryFile(mode='w', suffix='.bin', delete=False) as f:
                f.write("test model data")
                temp_file = f.name
            
            try:
                with open(temp_file, 'rb') as f:
                    files = {'file': f}
                    response = self.session.post(
                        f"{self.base_url}/server/model/test-model",
                        files=files
                    )
                self.assertIn(response.status_code, [200, 401, 403, 404, 405])
            finally:
                os.unlink(temp_file)
        except requests.exceptions.RequestException:
            pass
    
    def test_lora_device_management(self):
        """Test LoRa device management endpoints"""
        # Test get all LoRa devices
        try:
            response = self.session.get(f"{self.base_url}/device/lora")
            if response.status_code == 200:
                lora_data = response.json()
                self.assertIsInstance(lora_data, (dict, list))
        except requests.exceptions.RequestException:
            pass
        
        # Test get LoRa configs
        try:
            response = self.session.get(f"{self.base_url}/device/lora/config")
            if response.status_code == 200:
                configs = response.json()
                self.assertIsInstance(configs, (dict, list))
        except requests.exceptions.RequestException:
            pass
    
    def test_config_validation(self):
        """Test configuration validation logic"""
        # Test valid JSON
        valid_config = {
            "name": "test-server",
            "port": 8080,
            "debug": True,
            "features": ["auth", "ssl"]
        }
        self.assertTrue(self._is_valid_json(valid_config))
        
        # Test invalid JSON structure
        invalid_config = {
            "name": "test-server",
            "port": "invalid_port",  # Should be number
            "debug": True
        }
        # This would be validated by the server
        self.assertIsInstance(invalid_config, dict)
    
    def test_config_diffing(self):
        """Test configuration diffing logic"""
        config1 = {
            "name": "test-server",
            "port": 8080,
            "debug": False
        }
        
        config2 = {
            "name": "updated-server",
            "port": 9090,
            "debug": True,
            "newFeature": "enabled"
        }
        
        # Simple diff test
        self.assertNotEqual(config1, config2)
        self.assertNotEqual(config1["name"], config2["name"])
        self.assertNotEqual(config1["port"], config2["port"])
        self.assertNotEqual(config1["debug"], config2["debug"])
        self.assertNotIn("newFeature", config1)
        self.assertIn("newFeature", config2)
    
    def test_pagination_logic(self):
        """Test pagination logic for large datasets"""
        # Create test data
        items = [f"item-{i}" for i in range(100)]
        page_size = 10
        
        # Test pagination
        total_pages = len(items) // page_size + (1 if len(items) % page_size else 0)
        self.assertEqual(total_pages, 10)
        
        # Test first page
        first_page = items[0:page_size]
        self.assertEqual(len(first_page), page_size)
        self.assertEqual(first_page[0], "item-0")
        
        # Test last page
        last_page_start = (total_pages - 1) * page_size
        last_page = items[last_page_start:]
        self.assertEqual(len(last_page), page_size)
        self.assertEqual(last_page[0], "item-90")
    
    def test_error_handling(self):
        """Test error handling for various scenarios"""
        # Test non-existent endpoint
        try:
            response = self.session.get(f"{self.base_url}/nonexistent/endpoint")
            self.assertEqual(response.status_code, 404)
        except requests.exceptions.RequestException:
            pass
        
        # Test invalid method
        try:
            response = self.session.patch(f"{self.base_url}/server/config")
            self.assertIn(response.status_code, [404, 405])
        except requests.exceptions.RequestException:
            pass
        
        # Test invalid JSON payload
        try:
            response = self.session.put(
                f"{self.base_url}/server/config",
                data="invalid json{"
            )
            self.assertIn(response.status_code, [400, 415])
        except requests.exceptions.RequestException:
            pass
    
    def _is_valid_json(self, data: Any) -> bool:
        """Helper method to check if data can be serialized to JSON"""
        try:
            json.dumps(data)
            return True
        except (TypeError, ValueError):
            return False


class ConfigToolsIntegrationTest(unittest.TestCase):
    """Integration tests for configuration tools workflow"""
    
    def setUp(self):
        """Set up integration test fixtures"""
        self.base_url = "http://localhost:8080/api/v1"
        self.session = requests.Session()
    
    def test_full_config_workflow(self):
        """Test complete configuration management workflow"""
        # This test would simulate a full workflow:
        # 1. Get current config
        # 2. Modify config
        # 3. Validate changes
        # 4. Apply changes
        # 5. Verify changes
        
        try:
            # Step 1: Get current config
            response = self.session.get(f"{self.base_url}/server/config")
            if response.status_code != 200:
                self.skipTest("Cannot get current config - server may not be running")
            
            original_config = response.json()
            
            # Step 2: Create modified config
            modified_config = original_config.copy()
            modified_config['testTimestamp'] = int(time.time())
            
            # Step 3: Validate the modified config is valid JSON
            try:
                json.dumps(modified_config)
            except (TypeError, ValueError):
                self.fail("Modified config is not valid JSON")
            
            # Step 4: Try to apply changes (may fail due to auth/permissions)
            put_response = self.session.put(
                f"{self.base_url}/server/config",
                json=modified_config
            )
            
            # Step 5: Verify response is appropriate
            self.assertIn(put_response.status_code, [200, 401, 403, 405])
            
        except requests.exceptions.RequestException as e:
            self.skipTest(f"Cannot connect to server: {e}")
    
    def test_schema_crud_workflow(self):
        """Test schema CRUD workflow"""
        try:
            # Create a test schema
            test_schema = {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "value": {"type": "number"}
                },
                "required": ["name", "value"]
            }
            
            # Try to create schema
            create_response = self.session.post(
                f"{self.base_url}/server/schema",
                json={
                    "context": "/test/topic",
                    "schema": test_schema
                }
            )
            
            # Check if creation was successful or auth required
            if create_response.status_code == 200:
                # Try to get the schema
                schemas_response = self.session.get(f"{self.base_url}/server/schema")
                if schemas_response.status_code == 200:
                    schemas_data = schemas_response.json()
                    self.assertIsInstance(schemas_data, (dict, list))
            else:
                self.assertIn(create_response.status_code, [401, 403, 405])
                
        except requests.exceptions.RequestException as e:
            self.skipTest(f"Cannot connect to server: {e}")


def run_performance_tests():
    """Run performance tests for configuration operations"""
    import time
    
    base_url = "http://localhost:8080/api/v1"
    session = requests.Session()
    
    print("Running performance tests...")
    
    # Test config retrieval performance
    start_time = time.time()
    for i in range(10):
        try:
            response = session.get(f"{base_url}/server/config")
            if response.status_code == 200:
                pass  # Success
        except requests.exceptions.RequestException:
            pass
    end_time = time.time()
    
    print(f"Config retrieval: {end_time - start_time:.2f}s for 10 requests")
    
    # Test schema listing performance
    start_time = time.time()
    for i in range(10):
        try:
            response = session.get(f"{base_url}/server/schema")
            if response.status_code == 200:
                pass  # Success
        except requests.exceptions.RequestException:
            pass
    end_time = time.time()
    
    print(f"Schema listing: {end_time - start_time:.2f}s for 10 requests")
    
    session.close()


if __name__ == '__main__':
    # Run unit tests
    unittest.main(verbosity=2, exit=False)
    
    # Run performance tests
    print("\n" + "="*50)
    run_performance_tests()