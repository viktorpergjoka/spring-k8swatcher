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
package io.k8swatcher.annotation.bean;

import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;

@Informer(
        nsLabels = {"kubernetes.io/metadata.name=foo"},
        resLabels = {"app=foo"})
public class InvalidUpdateParamTestBean {

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void test2(Pod pod1, Pod pod2, Pod pod3) {}
}
