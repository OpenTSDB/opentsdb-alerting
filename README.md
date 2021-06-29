OpenTSDB Horizon Alerting
=========================

Horizon alerting is an alerting engine.

Background
----------

Interfacing with the Horizon config API, the alerting engine will pull 
alert configurations and periodically run queries against OpenTSDB. State is 
tracked for each alert and when an alert changes status, notifications are 
sent to the alerting notification engine.

Configuration
-------------

Configuration is performed via the `EnvironmentConfig` class.

Usage
-----

We have some work to do to get this back up and running. For now it does require
an OpenTSDB endpoint with the Horizon config running to be useful. It also 
requires an Apache Pulsar instance to store state and forward notifications.

Contribute
----------

Please see the [Contributing](contributing.md) file for information on how to
get involved. We welcome issues, questions, and pull requests.

Maintainers
-----------

* Ravi Kiran Chiruvolu
* Sergey Khegay
* Hariharasudhan Soundararajan

License
-------

This project is licensed under the terms of the Apache 2.0 open source license.
Please refer to [LICENSE](LICENSE.md) for the full terms.