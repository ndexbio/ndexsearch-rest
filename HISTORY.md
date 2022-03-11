History
========

0.8.0 (2022-03-11)
-------------

* To be consistent with other IQuery services, version bumped up to `0.8.0`

* Bumped jetty version to `9.4.45.v20220203`

* Bumped jackson-databind to `2.9.10.8`

* Added support for mutation frequency if caller includes `geneAnnotationServices` in query with
  URLs to service endpoints that provide mutation frequency. This includes addition of a mock mutation
  frequency endpoint

* When submitting query the resulting json now includes a `webURL` link that displays the
  results of that query in IQuery

* Fixed possible race conditions during processing of queries by adding `synchronized` wrappers

* Added logging of query genes and input sources. Also added logging of requests ip address and endpoint visited

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

