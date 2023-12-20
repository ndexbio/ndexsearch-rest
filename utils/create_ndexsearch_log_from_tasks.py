#!/usr/bin/env python

import sys
import os
import io
import re
import json
from datetime import datetime
import traceback
import argparse


class Formatter(argparse.ArgumentDefaultsHelpFormatter,
                argparse.RawDescriptionHelpFormatter):
    pass


def _parse_arguments(desc, args):
    """
    Parses command line arguments
    :param desc:
    :param args:
    :return:
    """
    parser = argparse.ArgumentParser(description=desc,
                                     formatter_class=Formatter)
    parser.add_argument('taskdir', help='Directory containing tasks')
    parser.add_argument('logdir',
                        help='Directory containing existing log files')
    parser.add_argument('destdir', help='Directory to write mock log files')
    return parser.parse_args(args)


def _get_tasks_to_ignore(logdir):
    """
    Get a set of all task ids in log files where task completed

    :param logdir:
    :return:
    """
    task_set = set()
    date_set = set()
    for entry in os.listdir(logdir):
        if not entry.startswith('ndexsearch'):
            continue
        if not entry.endswith('.log'):
            continue

        fp = os.path.join(logdir, entry)
        if not os.path.isfile(fp):
            continue
        datestr = re.sub('\.log', '', re.sub('ndexsearch_', '', entry))
        date_set.add(datestr)

        with open(fp, 'r') as f:
            for line in f:
                if 'SearchEngineImpl - Query ' not in line:
                    continue
                taskid = re.sub(' .*', '', re.sub('^.* - Query ', '', line.rstrip()))
                task_set.add(taskid)

    return task_set, date_set


def main(args):
    """
    Main entry point for program

    :param args:
    :return:
    """
    desc = """
    Reads IQuery task files and creates mock ndexsearch log files
    so that reporting tools can work
    
    """
    theargs = _parse_arguments(desc, args[1:])

    tasks_dir = os.path.abspath(theargs.taskdir)
    task_set, date_set = _get_tasks_to_ignore(theargs.logdir)
    task_hash = dict()
    try:
        if not os.path.isdir(tasks_dir):
            raise Exception(str(tasks_dir) + ' is not a directory')

        # open tasks directory and get a list of directories
        for entry in os.listdir(tasks_dir):
            if entry in task_set:
                #print('task exists in logdir: ' + str(entry))
                continue

            task_fp = os.path.join(tasks_dir, entry)
            if not os.path.isdir(task_fp):
                continue
            fp = os.path.join(task_fp, 'queryresults.json')
            if not os.path.isfile(fp):
                continue

            with open(fp, 'r') as f:
                qres = json.load(f)
            job_time = datetime.fromtimestamp(qres['startTime']/1000.0)
            datestr = job_time.strftime('%Y_%m_%d')
            if datestr in date_set:
                continue
            status = qres['status']
            walltime = qres['wallTime']
            if datestr not in task_hash:
                task_hash[datestr] = dict()
            task_hash[datestr] = {'time': job_time,
                                  'status': status,
                                  'walltime': walltime}

            # print(job_time)


        sys.exit(1)
        return 0
    except Exception as e:
        sys.stderr.write('\n\nCaught Exception: ' + str(e))
        traceback.print_exc()
        return 2


if __name__ == '__main__':  # pragma: no cover
    sys.exit(main(sys.argv))
