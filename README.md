Spring-K8sWatcher
=========
[![Build](https://github.com/viktorpergjoka/spring-k8swatcher/actions/workflows/build.yml/badge.svg)](https://github.com/viktorpergjoka/spring-k8swatcher/actions/workflows/build.yml)


Spring-K8sWatcher is an easy way to use Kubernetes Informer with Spring Boot only with few annotations. It uses the fabric8 Kubernetes Client.

An Informer is a mechanism where you can watch on any Kubernetes Resource for changes (ADD, UPDATE, DELETE) and react to them, e.g. when a Pod is added, or a ConfigMap is modified or even for Custom Resource Definitions.
Therefore you could build Kubernetes Controllers and Kubernetes Operators with it.


## Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
  - [Configure via annotation values](#configure-via-annotation-values)
  - [Configuring via application.yml](#configuring-via-applicationyml)
  - [Configuring the Kubernetes Client](#configuring-the-kubernetes-client)
- [Custom Resource Definitions (CRD)](#custom-resource-definitions-crd)
- [Permissions](#permissions)



## Prerequisites

Add dependency:

You don't need a web context like spring-boot-starter-web. spring-boot-starter is sufficient.

Maven:

```
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
</dependency>

<dependency>
  <groupId>io.k8swatcher</groupId>
  <artifactId>spring-k8swatcher</artifactId>
  <version>0.0.4</version>
</dependency>

```

Gradle:

```
implementation 'org.springframework.boot:spring-boot-starter'
implementation 'io.k8swatcher:spring-k8swatcher:0.0.4'

```

k8swatcher is already shipped with a fabric8 kubernetes client dependency. If you want to provide your own you have to exclude it:

Maven:

```
<dependency>
  <groupId>io.k8swatcher</groupId>
  <artifactId>spring-k8swatcher</artifactId>
  <version>0.0.4</version>
  <exclusions>
     <exclusion>
         <groupId>io.fabric8</groupId>
         <artifactId>kubernetes-client</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```


Gradle:

```
implementation ('io.k8swatcher:spring-k8swatcher:0.0.4'){
    exclude group: 'io.fabric8', module: 'kubernetes-client'
}

```

<br>

## Quick Start

```
@SpringBootApplication
@EnableInformer
public class WatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatchApplication.class, args);
    }
}
```

```

@Informer
public class MyInformer {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod){
       //do your logic here
    }

    @Watch(event = EventType.UPDATE, resource = ConfigMap.class)
    public void cmUpdated(ConfigMap oldCm, ConfigMap newCm){
      //do your logic here
    }

    @Watch(event = EventType.DELETE, resource = Service.class)
    public void serviceDeleted(Service service){
      //do your logic here    
    }
}

```
This a valid example although it is not recommended because this Informer will watch in ALL namespaces for every Pod, every ConfigMap and every Service resources. It needs a user with all the permissions to watch on any namespace for any resource.
<br><br>

The name of the method is not important, whereas the parameter definitions are important.
The Parameter signature must be the following:
* Add: (Resource resource)
* Update: (Resource oldResource, Resource newResource)
* Delete: (Resource resource, (Optionally) boolean deletedFinalStateUnknown)

Examples:
```
 @Watch(event = EventType.ADD, resource = Pod.class)
 public void myMethod(Pod pod){}


 @Watch(event = EventType.UPDATE, resource = RoleBinding.class)
 public void rolebindingUpdated(RoleBinding oldBinding, RoleBinding newBinding){}


 @Watch(event = EventType.DELETE, resource = Pod.class)
 public void deletePod(Pod pod){}


 @Watch(event = EventType.DELETE, resource = Pod.class)
 public void podDeleted(Pod deletedPod, boolean deletedFinalStateUnknown){}
```

## Configuration

There are 2 ways to configure the informers:
- in the annotation values
- via application.yml

The order will be the following:
* application.yml
* Annotation value
* Default value

| Property     | Description                                                                                                                                                                                                                                         | Default value                         |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| name         | Name of the referenced configuration in the application.yml. See [Configuring via application.yml](#configuring-via-applicationyml)                                                                                                                 | "default"                             |
| nsNames      | The name of the namespaces. If there is a namespace foo and foo2 it would be nsNames={"foo", "foo2"}. If nsNames are used, nsLabels will be ignored. See [Permissions](#Permissions)   section for details                                          | ""                                    |
| nsLabels     | a comma separated list of key=value for defining the namespace labels. When used in application.yml and the key has "/"  it should be wrapped inside "[]" e.g. "[kubernetes.io/metadata.name]" . If no value is provided, "all" will be used.       | ""                                    |
| resLabels    | a comma separated list of key=value for defining the resource labels. When used in application.yml and "/" is part of the label e.g. myApp/xy=z it should be wrapped inside "[]" e.g. "[myApp/xy]"   . If no value is provided, "all" will be used. | ""                                    |
| resyncPeriod | The resync with the Kubernetes API Server for updating the informer cache. Minimum 1000. If < 1000, value will be set to 1000                                                                                                                       | 1000                                  |
| clientName   | The name of the Kubernetes Client bean which should be used. Must be a of type io.fabric8.kubernetes.client.KubernetesClient                                                                                                                        | new KubernetesClientBuilder().build() |
### Configure via annotation values:

```
@Informer(nsLabels = {"kubernetes.informer/k8swatcher=enabled"}, resLabels = {"app=foo"}, resyncPeriod = 2000)
public class MyInformer {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod){
       
    }

    @Watch(event = EventType.UPDATE, resource = ConfigMap.class)
    public void cmUpdated(ConfigMap oldCm, ConfigMap newCm){
        
    }

    @Watch(event = EventType.DELETE, resource = Service.class)
    public void serviceDeleted(Service service){
        
    }
}

```
This will create an informer which watches for resources with the label app=foo in the namespaces with the label kubernetes.informer/k8swatcher=enabled


If you want to explicitly name the namespaces, you would use nsNames instead:
```
@Informer(nsNames = {"foo", "bar"}, resLabels = {"app=foo"}, resyncPeriod = 2000)
```
This will watch for resources labeled with app=foo in the namespaces with name "foo" and "bar".



If *nsNames* are used, *nsLabels* will be ignored


### Configuring via application.yml

The same can be configured in the application.yml:

```
@Informer(name = "myConfig")
public class MyInformer {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod){}

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void podUpdated(Pod oldPod, Pod newPod){}

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void podDeleted(Pod pod, boolean deletedFinalStateUnknown){}
}
```

```
k8swatcher:
    config:
      myConfig:
        resyncPeriod: 1000
        nsLabels:
          "[kubernetes.informer/k8swatcher]": enabled
        resLabels:
          app: spike
          foo: bar

```

This will watch for all Pods with label app=spike **and** foo=bar in all namespaces which have the label kubernetes.informer/k8swatcher=enabled.

Note that you should write the label keys inside [] in yaml because '/' will be parsed in the application.yaml
so something like

```
...
        nsLabels:
          kubernetes.io/metadata.name: foo
...

```
won't work and you would need to define it like this:

```
...
        nsLabels:
          "[kubernetes.io/metadata.name]": foo
...

```

If no name is defined, the default configs name is *default*

```
@Informer
public class PodInformer {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod){
    }
}

@Informer
public class SecretInformer {

    @Watch(event = EventType.ADD, resource = Secret.class)
    public void secretAdded(Secret secret){
    }
}

```

```
k8swatcher:
    config:
      default:
        nsLabels:
          "[k8swatcher.io/watched]": true
        resLabels:
          app: myApp


```
This will create two Informers which will watch for Secrets and Pods with label app=myApp in the namespaces with the name label *k8swatcher.io/watched=true* 

If you want to explicitly list the namespaces with their name:
```
k8swatcher:
  config:
    default:
      nsNames:
        - foo
        - foo2
```
If nsNames is used, nsLabels will be ignored.

## Configuring the Kubernetes client

By default the default Kubernetes client will be created (see https://github.com/fabric8io/kubernetes-client?tab=readme-ov-file#creating-a-client)

If you want to provide your own client you can define a Bean:

```
@Bean("myClient")
public KubernetesClient myKubernetesClient(){
    return ....
}
```
You would then reference to your client via annotation value or application.yml:
```
@Informer(clientName = "myClient")
```

```
k8swatcher:
    config:
      default:
        clientName: myClient
        ....

```

## Custom Resource Definitions (CRD)

As an example we take the example from the official Kubernetes Docs https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/

The Java classes would be the following:

```
@Group("stable.example.com")
@Version("v1")
public class CronTab extends CustomResource<CronTabSpec, CronTabStatus> implements Namespaced { }


@JsonDeserialize()
public class CronTabSpec implements KubernetesResource {

    private String cronSpec;
    private String image;
    private int replicas;
    
    //getters and setter

}

@JsonDeserialize()
public class CronTabStatus implements KubernetesResource {

    private String labelSelector;
    private int replicas;
    
    //getters and setters
}

```

Its then used like any other Kubernetes resource:

```
    @Watch(event = EventType.ADD, resource = CronTab.class)
    public void cronTabAdded(CronTab cronTab){
        String cronSpec = cronTab.getSpec().getCronSpec();
    }

```



## Permissions

Depending on which resources you want to watch you have to consider the following:
* You need ["get", "list", "watch"] verbs on that resource. A Pod cannot be watched if you don't have permissions for it. <br><br>
* if you use nsLabels e.g. you want to watch for resources in all namespaces that has the labels nsLabels = {"watcher=true"}  that user needs the permission to list all namespaces (like kubectl get ns). This implies that a ClusterRole is associated with that user. So this only works if the user has the appropriate permissions. If you have a user with limited access, for example when you run it within a container inside the cluster (which is recommended) with a service account associated and the service account can not list all namespaces, because it should only operate on his own namespace, you should use nsNames and list the namespace names explicitly.

