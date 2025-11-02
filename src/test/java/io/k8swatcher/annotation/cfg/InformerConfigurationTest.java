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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InformerConfigurationTest {

    @Test
    void noArgsConstructor_initializesDefaultValues() {
        InformerConfiguration config = new InformerConfiguration();

        assertNotNull(config.getNsLabels());
        assertNotNull(config.getResLabels());
        assertNotNull(config.getNsNames());
        assertEquals("", config.getClientName());
        assertNull(config.getResyncPeriod());
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        Map<String, String> nsLabels = Map.of("env", "prod");
        Map<String, String> resLabels = Map.of("app", "myapp");
        Set<String> nsNames = Set.of("namespace1", "namespace2");
        Long resyncPeriod = 5000L;
        String clientName = "testClient";

        InformerConfiguration config =
                new InformerConfiguration(nsLabels, resLabels, resyncPeriod, clientName, nsNames);

        assertEquals(nsLabels, config.getNsLabels());
        assertEquals(resLabels, config.getResLabels());
        assertEquals(resyncPeriod, config.getResyncPeriod());
        assertEquals(clientName, config.getClientName());
        assertEquals(nsNames, config.getNsNames());
    }

    @Test
    void setters_updateFields() {
        InformerConfiguration config = new InformerConfiguration();

        Map<String, String> nsLabels = new HashMap<>();
        nsLabels.put("key1", "value1");
        config.setNsLabels(nsLabels);

        Map<String, String> resLabels = new HashMap<>();
        resLabels.put("key2", "value2");
        config.setResLabels(resLabels);

        Set<String> nsNames = new HashSet<>();
        nsNames.add("ns1");
        config.setNsNames(nsNames);

        config.setResyncPeriod(3000L);
        config.setClientName("newClient");

        assertEquals(nsLabels, config.getNsLabels());
        assertEquals(resLabels, config.getResLabels());
        assertEquals(nsNames, config.getNsNames());
        assertEquals(3000L, config.getResyncPeriod());
        assertEquals("newClient", config.getClientName());
    }

    @Test
    void equals_returnsTrueForSameContent() {
        InformerConfiguration config1 =
                new InformerConfiguration(Map.of("k", "v"), Map.of("l", "m"), 2000L, "client", Set.of("ns"));
        InformerConfiguration config2 =
                new InformerConfiguration(Map.of("k", "v"), Map.of("l", "m"), 2000L, "client", Set.of("ns"));

        assertEquals(config1, config2);
    }

    @Test
    void hashCode_isSameForEqualObjects() {
        InformerConfiguration config1 =
                new InformerConfiguration(Map.of("k", "v"), Map.of("l", "m"), 2000L, "client", Set.of("ns"));
        InformerConfiguration config2 =
                new InformerConfiguration(Map.of("k", "v"), Map.of("l", "m"), 2000L, "client", Set.of("ns"));

        assertEquals(config1.hashCode(), config2.hashCode());
    }
}
