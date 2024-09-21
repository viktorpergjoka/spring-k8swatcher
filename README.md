Spring-K8Informer
=========


# Spring-K8Informer

Spring-K8Informer is an easy way to use Kubernetes Informer with Spring Boot only with few annotations. It uses the fabric8 Kubernetes Client.

### What is an Informer?
An Informer is a mechanism where you watch on any Kubernetes Resource and react to them, e.g. wehn a Pod is added, or a ConfigMap is modified or even with Custom Resource Definitions.


## Prerequisites

```


```

## Quick Start

```
@SpringBootApplication
@EnableInformer
public class WatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatchtestApplication.class, args);
    }
}
```

```

@Informer
public class MyInformer {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod){
       
    }

    @Watch(event = EventType.UPDATE, resource = ConfigMap.class)
    public void cmUpdated(Pod pod1, Pod pod2){
        
    }

    @Watch(event = EventType.DELETE, resource = Service.class)
    public void serviceDeleted(Pod pod){
        
    }
}

```
This a valid example although it is not recomendeed because this Informer will watch in ALL namespaces for every Pod, every ConfigMap and every Service resources. 


## Configuring Informers

There are 2 ways to configure the informers:
- in the annotation values
- via application.yml

### Configure via annotation values:

```
@Informer(nsLabels = {"kubernetes.io/metadata.name=foo"}, resLabels = {"app=foo"})
public class MyInformer {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod){
       
    }

    @Watch(event = EventType.UPDATE, resource = ConfigMap.class)
    public void cmUpdated(Pod pod1, Pod pod2){
        
    }

    @Watch(event = EventType.DELETE, resource = Service.class)
    public void serviceDeleted(Pod pod){
        
    }
}

This will create an informer which watches for resources with the label app=foo in the namespace with the name foo
```
### Configuring via application.yml

The same can be configured via application.yml:

```
@Informer(name = "myConfig")
public class Test2 {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void test(Pod pod){
    }

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void test(Pod pod1, Pod pod2){
    }

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void test2(Pod pod, boolean deletedFinalStateUnknown){
    }
}
```

```
k8watch:
  informer:
    config:
      myConfig:
        nsLabels:
          "[istio-injection]": enabled
        resLabels:
          app: spike
          foo: bar

```

This will watch for all Pods with label app=spike **and** foo=bar in **ALL** namespaces which have the label stio-injection=enabled.

Note that you should write the label keys inside [] in yaml because '/' will be parsed in the application.yaml
so something like

```
...
        nsLabels:
          kubernetes.io/metadata.name: foo: enabled
...

```
wont work and you would need to define it like this:

```
...
        nsLabels:
          "[kubernetes.io/metadata.name]": foo
...

```

If no name is defined, the default configs name is   *default*

```
@Informer
public class PodInformer {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void test(Pod pod){
    }
}

@Informer
public class SecretInformer {

    @Watch(event = EventType.ADD, resource = Secret.class)
    public void test(Secret secret){
    }
}

```

```
k8watch:
  informer:
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
k8watch:
  informer:
    config:
      default:
        clientName: myClient
        nsLabels:
          "[kubernetes.io/metadata.name]": foo
        resLabels:
          app: myApp

```
