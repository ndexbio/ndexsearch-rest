#!/usr/bin/env python

import sys
import os
import io
import re
import json
from datetime import datetime
import traceback
import argparse
import smtplib
from email.message import EmailMessage


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
    parser.add_argument('logdir',
                        help='Directory containing log files')
    parser.add_argument('--taskdir', help='Directory containing tasks')
    parser.add_argument('--emails',
                        help='If set, emails will be sent to comma delimited'
                             'email addresses set here')
    parser.add_argument('--smtpserver', default='localhost',
                        help='SMTP server')
    parser.add_argument('--label', default='IQuery',
                        help='Label used in subject line for email report')
    return parser.parse_args(args)


def send_report_as_email(theargs, report_str):
    """
    If 'theargs.emails' is set this method will send the 'report_str' via
    email

    :param theargs:
    :param report_str:
    :return:
    """
    msg = EmailMessage()
    msg.set_content(report_str)
    msg['Subject'] = theargs.label + ' report ' +\
                     datetime.today().strftime('%Y-%m-%d %H:%M:%S')
    msg['From'] = 'no_reply@ndexbio-stats.ucsd.edu'
    msg['To'] = re.sub('\\s+', '', theargs.emails).split(',')
    smtp_obj = smtplib.SMTP(theargs.smtpserver)
    smtp_obj.send_message(msg)
    smtp_obj.quit()


def _get_jobs_run_from_logs(logfiles_dir):
    """

    :param logdir:
    :return:
    """
    job_rundates_hash = {}

    # open logfiles directory and get a list of files
    # and build job_rundates_hash[year][month] => # jobs
    #
    for entry in os.listdir(logfiles_dir):
        if not entry.endswith('.log'):
            continue
        if not entry.startswith('ndexsearch'):
            continue
        fp = os.path.join(logfiles_dir, entry)
        if not os.path.isfile(fp):
            continue
        prefixedremoved = re.sub('^ndexsearch_', '', entry)
        suffixremoved = re.sub('\.log', '', prefixedremoved)
        jobdate = datetime.strptime(suffixremoved, '%Y_%m_%d')

        with open(fp, 'r') as f:
            for line in f:
                if '- Query ' not in line:
                    continue
                timeraw = re.sub(' .*', '', line).rstrip()
                job_timeraw = re.sub('\..*', '', timeraw)
                job_time = datetime.strptime(job_timeraw, '%H:%M:%S')
                jobdate = jobdate.replace(hour=job_time.hour,
                                          minute=job_time.minute,
                                          second=job_time.second)
                if jobdate.year not in job_rundates_hash:
                    job_rundates_hash[jobdate.year] = {}
                if jobdate.month not in job_rundates_hash[jobdate.year]:
                    job_rundates_hash[jobdate.year][jobdate.month] = [0, 0]
                if 'failed' in line or 'submitted' in line or 'processing' in line:
                    tuple_index = 1
                else:
                    tuple_index = 0
                job_rundates_hash[jobdate.year][jobdate.month][tuple_index] += 1
    return job_rundates_hash


def _get_jobs_run_from_tasks(tasks_dir):
    """

    :param tasks_dir:
    :return:
    """
    job_rundates_hash = {}
    # open tasks directory and get a list of directories
    for entry in os.listdir(tasks_dir):
        task_fp = os.path.join(tasks_dir, entry)
        if not os.path.isdir(task_fp):
            continue
        fp = os.path.join(task_fp, 'queryresults.json')
        if not os.path.isfile(fp):
            continue
        cdaps_job = False
        with open(fp, 'r') as f:
            qres = json.load(f)
        jobdate = datetime.fromtimestamp(qres['startTime'] / 1000.0)
        status = qres['status']
        # walltime = qres['wallTime']
        if 'inputSourceList' in qres:
            if len(qres['inputSourceList']) == 1 and 'enrichment' in qres['inputSourceList'][0]:
                cdaps_job = True

        if jobdate.year not in job_rundates_hash:
            job_rundates_hash[jobdate.year] = {}
        if jobdate.month not in job_rundates_hash[jobdate.year]:
            job_rundates_hash[jobdate.year][jobdate.month] = [0, 0, 0]
        if 'complete' not in status:
            tuple_index = 2
        else:
            tuple_index = 0
        job_rundates_hash[jobdate.year][jobdate.month][tuple_index] += 1
        if cdaps_job is True:
            job_rundates_hash[jobdate.year][jobdate.month][1] += 1
    return job_rundates_hash


def _write_monthly_report(job_rundates_hash, out_str,
                          header_list=None):
    """

    :param job_rundates_hash:
    :type job_rundates_hash: dict
    :param out_str: Stream to write str report to
    :type out_str: bytes like io
    :return:
    """
    sorted_months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]

    sorted_years = list(job_rundates_hash.keys())
    sorted_years.sort()
    current_time = datetime.now()
    out_str.write('Month-Year,' + ','.join(header_list) + '\n')

    zero_summary_list = ['0' for x in range(0, len(header_list))]

    for year in sorted_years:
        for month in sorted_months:
            if year >= current_time.year and month >= current_time.month:
                continue
            out_str.write(str(month) + '-' + str(year) + ',')
            if month not in job_rundates_hash[year]:
                month_summary_list = zero_summary_list
            else:
                month_summary_list = [str(entry) for entry in job_rundates_hash[year][month]]
            out_str.write(','.join(month_summary_list) + '\n')


def main(args):
    """
    Main entry point for program

    :param args:
    :return:
    """
    desc = """
    Parses IQuery log files to generate jobs report
    
    """
    theargs = _parse_arguments(desc, args[1:])

    logfiles_dir = os.path.abspath(theargs.logdir)


    try:

        out_str = sys.stdout
        if theargs.emails is not None:
            out_str = io.StringIO()

        # if log files path is a directory do analysis
        if os.path.isdir(logfiles_dir):
            out_str.write('Report taken by parsing log files\n')
            job_rundates_hash = _get_jobs_run_from_logs(logfiles_dir)
            _write_monthly_report(job_rundates_hash, out_str=out_str,
                                  header_list=['# Jobs',
                                               '# Failed Jobs (does not count '
                                               'jobs that never finished)'])

        # if task path is a directory do analysis
        if theargs.taskdir:
            tasks_dir = os.path.abspath(theargs.taskdir)
            if os.path.isdir(tasks_dir):
                out_str.write('\nReport taken by directly parsing tasks\n')
                job_rundates_hash = _get_jobs_run_from_tasks(tasks_dir)
                _write_monthly_report(job_rundates_hash, out_str=out_str,
                                      header_list=['# Jobs',
                                                   '# Enrichment only jobs (CDAPS)',
                                                   '# Jobs failed or never finished'])

        if theargs.emails is not None:
            send_report_as_email(theargs, out_str.getvalue())
        return 0
    except Exception as e:
        sys.stderr.write('\n\nCaught Exception: ' + str(e))
        traceback.print_exc()
        return 2


if __name__ == '__main__':  # pragma: no cover
    sys.exit(main(sys.argv))
