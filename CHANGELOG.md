## CHANGELOG

### 1.0.5 (2025-05-06)

#### Dependency Upgrade
* Fix #21: Dependency Upgrade

### 1.0.4 (2025-03-14)

#### Bugs
* Fix #14: informers are shutting down immediately when vert.x is used as http client 

### 1.0.3 (2025-01-19)

####  Bugs
* Fix #16: informers shutdown with no error message when no namespace with nsLabel exists

#### Enhancement
* Fix #17: improve informer creation when same resources are watched

### 1.0.2 (2025-01-08)

#### Dependency Downgrade
* Fix #14: downgrade fabric8 from 7.0.1 to 6.13.4

### 1.0.1 (2025-01-08)

* Fix #11: implement cache for the events

#### Dependency Upgrade
* Fix #12: Dependency Upgrade

####  Bugs
* Fix #9: Remove Exception when resyncPeriod

### 0.0.4 (2024-11-15)

#### Dependency Upgrade
* Fix #7: Dependency Upgrade

####  Bugs
* Fix #5: all informer should be started after all are constructed

### 0.0.3 (2024-10-15)

####  Bugs
* Fix #2: remove namespaces.list() with users with restricted roles

####  New Features
* Fix #2 Add nsNames to explicitly name the namespaces on which the watch should be done
