History
========

0.3.5 (2020-11-24)
------------------

* Fixed a bug where if `/status` endpoint raised an exception (unlikely to happen), it
  would return a 500 code, but not output json describing the error.

