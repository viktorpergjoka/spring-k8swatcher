## CHANGELOG

### 1.0.3 (Pending)

####  Bugs
* Fix #16: informers shutdown with no error message when no namespace with nsLabel exists

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
