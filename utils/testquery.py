#!/usr/bin/env python

import os
import sys
import requests

query = {"geneList": ["mtor", "tp53", "a"],
         "sourceList": ["enrichment"],
         "alterationData": [
            {
              "gene": "TP53",
              "altered": 4437,
              "sequenced": 10336,
              "percentAltered": "43%"
            },
            {
              "gene": "MTOR",
              "altered": 329,
              "sequenced": 10336,
              "percentAltered": "3%"
            }
         ]
        }

dev_url = 'https://iquery-cbio-dev.ucsd.edu/integratedsearch/v1/'
local_url = 'http://localhost:8290/v1/'

res = requests.post(local_url,
                    headers={'ContentType': 'application/json',
                             'Accept': 'application/json'},
                    json=query, verify=True)
print(res.text)



