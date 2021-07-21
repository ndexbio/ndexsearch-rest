#!/usr/bin/env python

import sys
import time
import requests


rest_endpoint = 'http://public.ndexbio.org/integratedsearch/v1'

# for dev
# rest_endpoint = 'http://dev.ndexbio.org/integratedsearch/v1'

# for test
# rest_endpoint = 'http://test.ndexbio.org/integratedsearch/v1'


# check if service is alive
resp = requests.get(rest_endpoint + '/status')
if resp.status_code != 200:
    print('There was an error getting status ' + str(resp.text))
    sys.exit(1)

print('Status output:')
print('\t' + str(resp.json()))

# to see what sources are available
# which needs to be set in the 'sourceList' in
# the query below
resp = requests.get(rest_endpoint + '/source')

if resp.status_code != 200:
    print('There was an error running query: ' + str(resp.text))
    sys.exit(1)

source_list = []
for entry in resp.json()['results']:
    source_list.append(entry['name'])

print('Source list:')
print('\t' + str(source_list))


#
# run a query using the genes mtor and tp53
#
query = {'geneList': ['mtor', 'tp53'],
         'sourceList': source_list}


resp = requests.post(rest_endpoint, json=query)

# upon success 202 is returned (REST convention for accepted task)
if resp.status_code != 202:
    print("there was an error running query: " + str(resp.text))
    sys.exit(1)

# the id of the task corresponding to the query
task_id = resp.json()['id']

# polling loop to wait for task to complete
progress = 0
consecutive_err_cnt = 0
while progress != 100 and consecutive_err_cnt < 5:
    time.sleep(1)
    resp = requests.get(rest_endpoint + '/' + task_id + '/status')
    if resp.status_code != 200:
        consecutive_err_cnt += 1
        print('ran into some error: ' + str(resp.text))
        continue
    consecutive_err_cnt = 0
    progress_list = []
    for src in resp.json()['sources']:
        progress_list.append(src['progress'])

    # set progress to lowest progress level
    # for sources. The task is totally done when
    # progress is 100 for all the sources, but you
    # could always get partial results
    progress = min(progress_list)

if consecutive_err_cnt > 0:
    print('Received 5 consecutive errors, '
          'is your net connection down?')
    sys.exit(1)

# get the full results
# this same call would work in above loop, but returns
# a lot more data
resp = requests.get(rest_endpoint + '/' + task_id)


result = resp.json()
src_uuid = None
net_uuid = None
# The results are under sources as a list
for entry in result['sources']:

    print('\nFor source: ' + str(entry['sourceName']))
    print('\tSourceUUID: ' + str(entry['sourceUUID']))
    print('\tNum hits: ' + str(entry['numberOfHits']))

    # just grabbing first source
    if src_uuid is None:
        src_uuid = entry['sourceUUID']

    for thenet in entry['results']:
        print('\t\tDescription: ' + str(thenet['description']))
        print('\t\tNumNodes: ' + str(thenet['nodes']))
        print('\t\tHitGenes: ' + str(thenet['hitGenes']))
        print('\t\tNetworkUUID: ' + str(thenet['networkUUID']))
        print('\t\tDetails: ' + str(thenet['details']) + '\n')

        # just grabbing first network
        if net_uuid is None:
            net_uuid = thenet['networkUUID']

# to get the CX for a specific network
req_url = rest_endpoint + '/' + task_id +\
          '/overlaynetwork?sourceUUID=' + src_uuid +\
          '&networkUUID=' + net_uuid

resp = requests.get(req_url)

cx = resp.json()
print('For network ' + net_uuid + ' under source' + src_uuid)
print('Got CX with ' + str(len(cx)) + ' aspects')


print('Deleting task\n')
# delete the task
resp = requests.delete(rest_endpoint + '/' + task_id)
if resp.status_code != 200:
    print('got non 200 for delete of task: ' +
          task_id + ' ' + str(resp.text))
    sys.exit(1)
