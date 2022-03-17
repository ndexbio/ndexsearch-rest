#!/usr/bin/env python

import os
import sys
import requests

query = {"geneList": ["mtor", "tp53", "a"],
         "sourceList": ["enrichment"],
         "geneAnnotationServices": {
            "mutation": "https://iquery-cbio-dev.ucsd.edu/integratedsearch/v1/mutationfrequency",
            "alteration": "http://localhost"
         }}

res = requests.post('https://iquery-cbio-dev.ucsd.edu/integratedsearch/v1/',
                    headers={'ContentType': 'application/json',
                             'Accept': 'application/json'},
                    json=query, verify=True)
print(res.text)



