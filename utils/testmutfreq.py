#!/usr/bin/env python

import os
import sys
import requests

query = {"genes": ["mtor", "tp53", "a"]}

res = requests.post('https://iquery-cbio-dev.ucsd.edu/integratedsearch/v1/mutationfrequency',
                    headers={'ContentType': 'application/json',
                             'Accept': 'application/json'},
                    json=query, verify=True)
print(res.text)



