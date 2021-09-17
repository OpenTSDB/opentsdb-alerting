OpenTSDB Horizon Alerting Notification Components
=================================================

This is a library and daemon for consuming alerts and routing them to the proper
endpoints for notifications.

Background
----------

Interfacing with the Horizon config API, the alerting notification engine will
route alerts from a queue to the proper endpoints such as an email address,
Slack, OpsGenie or call a webhook.

Configuration
-------------

Configuration is to be cleaned up.

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

* Sergey Khegay
* Hariharasudhan Soundararajan
* Ravi Kiran Chiruvolu

License
-------

This project is licensed under the terms of the Apache 2.0 open source license.
Please refer to [LICENSE](LICENSE.md) for the full terms.

### Developer Notes
* When using IntelliJ IDE, be sure and install the lombok plugin, which resolves getters and setters.
* To test OpsGenie: return the API Key, in a way of your choosing, in `getOpsGenieApiKey()` located at TestData.