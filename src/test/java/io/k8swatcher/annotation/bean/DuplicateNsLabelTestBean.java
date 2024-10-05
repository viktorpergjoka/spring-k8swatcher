package io.k8swatcher.annotation.bean;

import io.k8swatcher.annotation.Informer;

@Informer(
        nsLabels = {"kubernetes.io/metadata.name=foo", "kubernetes.io/metadata.name=foo"},
        resLabels = {"app=foo"})
public class DuplicateNsLabelTestBean {}
