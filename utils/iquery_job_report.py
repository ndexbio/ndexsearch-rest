#!/usr/bin/env python

import sys
import os
import io
import re
import json
from json.decoder import JSONDecodeError

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


def _get_jobs_run_from_requests(logfiles_dir):
    """
    Gets count of jobs where at least one network was viewed

    :param logfiles_dir:
    :return:
    """
    job_rundates_hash = {}
    task_id_set = set()
    for entry in os.listdir(logfiles_dir):
        if not entry.endswith('.log'):
            continue
        if not entry.startswith('requests'):
            continue
        fp = os.path.join(logfiles_dir, entry)
        if not os.path.isfile(fp):
            continue
        prefixedremoved = re.sub('^requests_', '', entry)
        suffixremoved = re.sub('\.log', '', prefixedremoved)
        jobdate = datetime.strptime(suffixremoved, '%Y_%m_%d')

        with open(fp, 'r') as f:
            for line in f:
                if '/overlaynetwork]' not in line:
                    continue
                taskidstart = line[0:line.index('/overlaynetwork]')]
                taskid = taskidstart[taskidstart.rindex('/')+1:]

                if taskid in task_id_set:
                    continue
                task_id_set.add(taskid)

                timeraw = line[1:line.index(']')]
                job_timeraw = timeraw[timeraw.index(' ')+1:timeraw.index(',')]
                job_time = datetime.strptime(job_timeraw, '%H:%M:%S')
                jobdate = jobdate.replace(hour=job_time.hour,
                                          minute=job_time.minute,
                                          second=job_time.second)
                if jobdate.year not in job_rundates_hash:
                    job_rundates_hash[jobdate.year] = {}
                if jobdate.month not in job_rundates_hash[jobdate.year]:
                    job_rundates_hash[jobdate.year][jobdate.month] = [0, 0]

                job_rundates_hash[jobdate.year][jobdate.month][0] += 1
    return job_rundates_hash

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
        try:
            with open(fp, 'r') as f:
                qres = json.load(f)
        except JSONDecodeError as je:
            sys.stderr.write('Got decode error on file: ' + str(fp) + ' skipping : ' + str(je))
            continue

        jobdate = datetime.fromtimestamp(qres['startTime'] / 1000.0)
        status = qres['status']
        # walltime = qres['wallTime']
        
        # Example gene lists to compare to query gene list;
        hypoxia = ['ADM', 'ADORA2B', 'AK4', 'AKAP12', 'ALDOA', 'ALDOB', 'ALDOC', 'AMPD3', 'ANGPTL4', 'ANKZF1', 'ANXA2', 'ATF3', 'ATP7A', 'B3GALT6', 'B4GALNT2', 'BCAN', 'BCL2', 'BGN', 'BHLHE40', 'BNIP3L', 'BRS3', 'BTG1', 'CA12', 'CASP6', 'CAV1', 'CCNG2', 'CCRN4L', 'CDKN1A', 'CDKN1B', 'CDKN1C', 'CHST2', 'CHST3', 'CITED2', 'COL5A1', 'CP', 'CSRP2', 'CTGF', 'CXCR4', 'CXCR7', 'CYR61', 'DCN', 'DDIT3', 'DDIT4', 'DPYSL4', 'DTNA', 'DUSP1', 'EDN2', 'EFNA1', 'EFNA3', 'EGFR', 'ENO1', 'ENO2', 'ENO3', 'ERO1L', 'ERRFI1', 'ETS1', 'EXT1', 'F3', 'FAM162A', 'FBP1', 'FOS', 'FOSL2', 'FOXO3', 'GAA', 'GALK1', 'GAPDH', 'GAPDHS', 'GBE1', 'GCK', 'GCNT2', 'GLRX', 'GPC1', 'GPC3', 'GPC4', 'GPI', 'GRHPR', 'GYS1', 'HAS1', 'HDLBP', 'HEXA', 'HK1', 'HK2', 'HMOX1', 'HOXB9', 'HS3ST1', 'HSPA5', 'IDS', 'IER3', 'IGFBP1', 'IGFBP3', 'IL6', 'ILVBL', 'INHA', 'IRS2', 'ISG20', 'JMJD6', 'JUN', 'KDELR3', 'KDM3A', 'KIF5A', 'KLF6', 'KLF7', 'KLHL24', 'LALBA', 'LARGE', 'LDHA', 'LDHC', 'LOX', 'LXN', 'MAFF', 'MAP3K1', 'MIF', 'MT1E', 'MT2A', 'MXI1', 'MYH9', 'NAGK', 'NCAN', 'NDRG1', 'NDST1', 'NDST2', 'NEDD4L', 'NFIL3', 'NR3C1', 'P4HA1', 'P4HA2', 'PAM', 'PCK1', 'PDGFB', 'PDK1', 'PDK3', 'PFKFB3', 'PFKL', 'PFKP', 'PGAM2', 'PGF', 'PGK1', 'PGM1', 'PGM2', 'PHKG1', 'PIM1', 'PKLR', 'PKP1', 'PLAC8', 'PLAUR', 'PLIN2', 'PNRC1', 'PPARGC1A', 'PPFIA4', 'PPP1R15A', 'PPP1R3C', 'PRDX5', 'PRKCA', 'PRKCDBP', 'PTRF', 'PYGM', 'RBPJ', 'RORA', 'RRAGD', 'S100A4', 'SAP30', 'SCARB1', 'SDC2', 'SDC3', 'SDC4', 'SELENBP1', 'SERPINE1', 'SIAH2', 'SLC25A1', 'SLC2A1', 'SLC2A3', 'SLC2A5', 'SLC37A4', 'SLC6A6', 'SRPX', 'STBD1', 'STC1', 'STC2', 'SULT2B1', 'TES', 'TGFB3', 'TGFBI', 'TGM2', 'TIPARP', 'TKTL1', 'TMEM45A', 'TNFAIP3', 'TPBG', 'TPD52', 'TPI1', 'TPST2', 'UGP2', 'VEGFA', 'VHL', 'VLDLR', 'WISP2', 'WSB1', 'XPNPEP1', 'ZFP36', 'ZNF292']
        
        death = ['APAF1', 'BCL2', 'BID', 'BIRC2', 'BIRC3', 'CASP10', 'CASP3', 'CASP6', 'CASP7', 'CFLAR', 'CHUK', 'DFFA', 'DFFB', 'FADD', 'GAS2', 'LMNA', 'MAP3K14', 'NFKB1', 'RELA', 'RIPK1', 'SPTAN1', 'TNFRSF25', 'TNFSF10', 'TRADD', 'TRAF2', 'XIAP']
        
        ros = ['ABCC1', 'ATOX1', 'CAT', 'CDKN2D', 'EGLN2', 'ERCC2', 'FES', 'FTL', 'G6PD', 'GCLC', 'GCLM', 'GLRX', 'GLRX2', 'GPX3', 'GPX4', 'GSR', 'HHEX', 'HMOX2', 'IPCEF1', 'JUNB', 'LAMTOR5', 'LSP1', 'MBP', 'MGST1', 'MPO', 'MSRA', 'NDUFA6', 'NDUFB4', 'NDUFS2', 'NQO1', 'OXSR1', 'PDLIM1', 'PFKP', 'PRDX1', 'PRDX2', 'PRDX4', 'PRDX6', 'PRNP', 'PTPA', 'SBNO2', 'SCAF4', 'SELENOS', 'SOD1', 'SOD2', 'SRXN1', 'STK25', 'TXN', 'TXNRD1', 'TXNRD2']
        
        coxsackie = ['C2AIL', 'CARF', 'CBX1', 'CISY', 'CPSF6', 'CSK21', 'DDX1', 'DPYL2', 'EIF3A', 'EIF3B', 'EIF3C', 'EIF3D', 'EIF3E', 'EIF3F', 'EIF3G', 'EIF3H', 'EIF3I', 'EIF3K', 'EIF3L', 'EIF3M', 'F120A', 'FA98A', 'FBSP1', 'IF4G1', 'IMA5', 'LAR4B', 'MYCB2', 'NCBP3', 'NUP98', 'PAN2', 'PRC2B', 'PTBP1', 'PURB', 'RAE1L', 'RAVR1', 'REL', 'RING1', 'RTRAF', 'SETD3', 'TM225', 'XRN2', 'YTHD3']

        if qres['inputSourceList'] is not None and len(qres['inputSourceList']) == 1 and 'enrichment' in qres['inputSourceList'][0]:
            cdaps_job = True

        if jobdate.year not in job_rundates_hash:
            job_rundates_hash[jobdate.year] = {}
        if jobdate.month not in job_rundates_hash[jobdate.year]:
            job_rundates_hash[jobdate.year][jobdate.month] = [0, 0, 0]

        if qres['query'] is not None:
            # Modify query gene list and make comparison with example list
            query_genes = [item.upper() for item in qres['query']]
            query_genes.sort()
        else:
            # there are tasks that never finish and lack query genes so
            # just set the genes as an empty list
            query_genes = []

        if query_genes != hypoxia and query_genes != death and query_genes != ros and query_genes != coxsackie:
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
            if year >= current_time.year and month > current_time.month:
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
            out_str.write('Report taken by parsing request log files\n')
            job_rundates_hash = _get_jobs_run_from_requests(logfiles_dir)
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
