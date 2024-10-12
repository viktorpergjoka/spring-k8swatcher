Spring-K8sWatcher
=========


Spring-K8sWatcher is an easy way to use Kubernetes Informer with Spring Boot only with few annotations. It uses the fabric8 Kubernetes Client.

An Informer is a mechanism where you can watch on any Kubernetes Resource for changes (ADD, UPDATE, DELETE) and react to them, e.g. when a Pod is added, or a ConfigMap is modified or even for Custom Resource Definitions.
Therefore you could build Kubernetes Controllers and Operators with it.

## Prerequisites

Add dependency:

Maven:

```
<dependency>
  <groupId>io.k8swatcher</groupId>
  <artifactId>spring-k8swatcher</artifactId>
  <version>0.0.2</version>
</dependency>

```

Gradle:

```
implementation 'io.k8swatcher:spring-k8swatcher:0.0.2'

```

k8swatcher is already shipped with a fabric8 kubernetes client dependency. If you want to provide your own you have to exclude it:

Maven:

```
<dependency>
  <groupId>io.k8swatcher</groupId>
  <artifactId>spring-k8swatcher</artifactId>
  <version>0.0.2</version>
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
    implementation ('io.k8swatcher:spring-k8swatcher:0.0.2'){
        exclude group: 'io.fabric8', module: 'kubernetes-client'
    }

```

**Note**: its clear that to watch resources on a namespace, you need access with the user to that namespace with ["get", "watch", "list"] as verbs on the resources

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
This a valid example although it is not recommended because this Informer will watch in ALL namespaces for every Pod, every ConfigMap and every Service resources. It needs an user with all the permissions to watch on any namespace for any resource.

The name of the method is not important, whereas the parameter definitions is important.
The Parameter signature must be the following:
* Add: (Resource resource)
* Update: (Resource oldResource, Resource new Resource)
* Delete: (Resource resource, (Optionally) boolean deletedFinalStateUnknown)

Examples:
```
* @Watch(event = EventType.ADD, resource = Pod.class)
  public void myMethod(Pod pod){}


* @Watch(event = EventType.UPDATE, resource = RoleBinding.class)
  public void rolebindingUpdated(RoleBinding oldBinding, RoleBinding newBinding){}


* @Watch(event = EventType.DELETE, resource = Pod.class)
    public void delete(Pod pod){}


* @Watch(event = EventType.DELETE, resource = Pod.class)
  public void delete(Pod pod, boolean deletedFinalStateUnknown){}
```

## Configuration

There are 2 ways to configure the informers:
- in the annotation values
- via application.yml

The order will be the following:
* application.yml
* Annotation value

| Property     | Description                                                                                                                                                                                                                       | Default value                         |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| name         | Name of the referenced configuration in the application.yml                                                                                                                                                                       | "default"                             |
| nsLabels     | a comma separated list of key=value for defining the namespace labels. When used in application.yml it should be wrapped inside "[]". If no value will be provided "all" will be used.                                            | ""                                    |
| resLabels    | a comma separated list of key=value for defining the resource labels. When used in application.yml and "/" is part of the label e.g. myApp/xy it should be wrapped inside "[]" . If no value will be provided "all" will be used. | ""                                    |
| resyncPeriod | The resync with the Kubernetes API Server for updating the informer cache. Must be greater than 1000 or an Exception is thrown                                                                                                    | 1000                                  |
| clientName   | The name of the Kubernetes Client bean which should be used. Must be a of type io.fabric8.kubernetes.client.KubernetesClient                                                                                                      | new KubernetesClientBuilder().build() |

### Configure via annotation values:

```
@Informer(nsLabels = {"kubernetes.io/metadata.name=foo"}, resLabels = {"app=foo"}, resyncPeriod = 2000)
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
This will create an informer which watches for resources with the label app=foo in the namespace with the name foo

### Configuring via application.yml

The same can be configured in the application.yml:

```
@Informer(name = "myConfig")
public class Test2 {

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
        nsLabels:
          "[istio-injection]": enabled
        resLabels:
          app: spike
          foo: bar

```

This will watch for all Pods with label app=spike **and** foo=bar in all namespaces which have the label istio-injection=enabled.

Note that you should write the label keys inside [] in yaml because '/' will be parsed in the application.yaml
so something like

```
...
        nsLabels:
          kubernetes.io/metadata.name: foo: enabled
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
          "[kubernetes.io/metadata.name]": foo
        resLabels:
          app: myApp


```
This will create two Informers which will watch for Secrets and Pods with label app=myApp in the namespace with the name *foo* 


## Configuring the Kubernetes Client

By default the default Kubernetes Client will be created (see https://github.com/fabric8io/kubernetes-client?tab=readme-ov-file#creating-a-client)

If you want to provide your own Client you can define a Bean:

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

