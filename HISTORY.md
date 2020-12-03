History
========

0.3.5 (2020-11-24)
------------------

* Fixed a bug where if `/status` endpoint raised an exception (unlikely to happen), it
  would return a 500 code, but not output json describing the error.

* Bumped jetty version to `9.4.34.v20201102` and jackson to `2.9.10` databind `2.9.10.6`

