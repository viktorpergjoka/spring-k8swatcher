package io.k8swatcher.annotation.bean;

import io.k8swatcher.annotation.Informer;

@Informer(nsNames = {"ns1", "ns2", "ns1"})
public class DuplicateNsNamesTestBean {}
