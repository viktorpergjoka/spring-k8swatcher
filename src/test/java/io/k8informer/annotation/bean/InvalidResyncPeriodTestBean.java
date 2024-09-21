package io.k8informer.annotation.bean;

import io.k8informer.annotation.Informer;

@Informer(
        nsLabels = {"kubernetes.io/metadata.name=foo"},
        resLabels = {"app=foo"},
        resyncPeriod = 10)
public class InvalidResyncPeriodTestBean {}
