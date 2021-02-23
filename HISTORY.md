History
========

0.3.6 (2021-02-22)
------------------

* Fixed a bug where if a query had no results the source would be omitted from
  the returned JSON which did not adhere to the expected behavior of web UI. UD-1591

0.3.5 (2020-11-24)
------------------

* Fixed a bug where if `/status` endpoint raised an exception (unlikely to happen), it
  would return a 500 code, but not output json describing the error.

* Bumped jetty version to `9.4.34.v20201102` and jackson to `2.9.10` databind `2.9.10.6`

* Fixed bug on getting results with source filter. If one passed a list of sources delimited
  by a comma with no spaces between the terms, none of the sources would be returned.

* Fixed bug where periodic update of sources would clear sources if one of them 
  (in this case, enrichment) failed when update called. UD-1569.

