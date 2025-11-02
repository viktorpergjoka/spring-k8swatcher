/*
 * Copyright (C) 2025 Viktor Pergjoka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.k8swatcher.annotation.cfg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InformerConfigurationPropertyTest {

    @Test
    void postConstruct_createsDefaultConfiguration_whenNotPresent() {
        InformerConfigurationProperty property = new InformerConfigurationProperty();

        property.postConstruct();

        assertNotNull(property.getConfig());
        assertTrue(property.getConfig().containsKey("default"));
        assertNotNull(property.getConfig().get("default"));
    }

    @Test
    void postConstruct_doesNotOverwriteExistingDefaultConfiguration() {
        InformerConfigurationProperty property = new InformerConfigurationProperty();
        InformerConfiguration customDefault = new InformerConfiguration(
                Map.of("key", "value"), Map.of("label", "test"), 5000L, "customClient", Set.of("namespace1"));

        property.getConfig().put("default", customDefault);
        property.postConstruct();

        assertSame(customDefault, property.getConfig().get("default"));
        assertEquals("customClient", property.getConfig().get("default").getClientName());
    }

    @Test
    void getConfig_returnsModifiableMap() {
        InformerConfigurationProperty property = new InformerConfigurationProperty();

        Map<String, InformerConfiguration> config = property.getConfig();

        assertNotNull(config);
        config.put("test", new InformerConfiguration());
        assertEquals(1, property.getConfig().size());
    }

    @Test
    void config_canStoreMultipleConfigurations() {
        InformerConfigurationProperty property = new InformerConfigurationProperty();

        InformerConfiguration config1 = new InformerConfiguration();
        InformerConfiguration config2 = new InformerConfiguration();

        property.getConfig().put("config1", config1);
        property.getConfig().put("config2", config2);

        assertEquals(2, property.getConfig().size());
        assertSame(config1, property.getConfig().get("config1"));
        assertSame(config2, property.getConfig().get("config2"));
    }

    @Test
    void postConstruct_preservesExistingConfigurations() {
        InformerConfigurationProperty property = new InformerConfigurationProperty();

        InformerConfiguration custom = new InformerConfiguration();
        property.getConfig().put("custom", custom);

        property.postConstruct();

        assertEquals(2, property.getConfig().size());
        assertTrue(property.getConfig().containsKey("custom"));
        assertTrue(property.getConfig().containsKey("default"));
        assertSame(custom, property.getConfig().get("custom"));
    }
}
