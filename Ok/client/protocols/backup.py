# Modifications copyright (C) 2021 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

import json
import shutil
import tempfile
import zipfile

from display_timedelta import display_timedelta

from client import exceptions
from client.protocols.common import models
from client.utils import network
import datetime
import logging
import time
import os
import sys
from subprocess import Popen
import pickle
import requests
from zipfile import ZipFile

from filelock import Timeout, FileLock

log = logging.getLogger(__name__)

from client.utils.printer import print_warning, print_success, print_error
from client.utils.output import DisableLog


class BackupProtocol(models.Protocol):
    # Timeouts are specified in seconds.
    SHORT_TIMEOUT = 2

    RETRY_LIMIT = 5
    BACKUP_FILE = ".ok_messages"
    BACKUP_ENDPOINT = '{server}/backups'
    ASSIGNMENT_ENDPOINT = '{server}/assignments'

    def run(self, messages, nointeract=False):
        if not self.assignment.endpoint:
            log.info('No assignment endpoint, skipping backup')
            return

        if self.args.local:
            return

        if not self.args.insecure:
            network.check_ssl()

        message_list = self.load_unsent_messages()

        auth = self.assignment.authenticate(nointeract=nointeract)
        if not auth:
            print_error("Not authenticated. Cannot backup to server.")
            self.dump_unsent_messages(message_list)
            return

        message_list.append(messages)
        response = self.send_all_messages(auth, message_list, current=False)

        if isinstance(response, dict):
            print_success('Backup successful for user: {} {}'.format(auth['username'], auth['fullName']))
            print('NOTE: this is only a backup. To submit your assignment, use:\n'
                  '\tpython3 ok --submit')

        self.dump_unsent_messages(message_list)
        print()

    def _update_last_autobackup_time(self, between):
        """
        Updates the last autobackup time in TIME_PATH
        """
        LOCK_PATH = ".ok_backup_time.lock"
        TIME_PATH = ".ok_backup_time"
        try:
            with FileLock(LOCK_PATH, timeout=1):
                try:
                    with open(TIME_PATH) as f:
                        last_time = datetime.datetime.fromtimestamp(int(f.read()))
                    if datetime.datetime.now() - last_time < between:
                        return False
                except FileNotFoundError:
                    pass
                with open(TIME_PATH, "w") as f:
                    f.write(str(int(datetime.datetime.now().timestamp())))
                return True
        except Timeout:
            return False

    def _safe_run(self, messages, between):
        """
        Run a backup, if and only if an autobackup has not been attempted more than `between` time ago.

            between: a timedelta of how long to wait between backups
        """

        if not self._update_last_autobackup_time(between):
            return

        with DisableLog():
            try:
                self.run(messages, nointeract=True)
            except Exception as e:
                return

    def _get_end_time(self):
        access_token = self.assignment.authenticate(nointeract=False)
        due_date = self.get_due_date(access_token, 5)
        if due_date is None:
            due_date = datetime.datetime.min.replace(tzinfo=datetime.timezone.utc)

        return max(due_date, datetime.datetime.now(tz=datetime.timezone.utc)) + datetime.timedelta(hours=1)

    def run_in_loop(self, messages_fn, period, synchronous):
        if not synchronous:
            self.run(messages_fn())
            args = sys.argv[:]
            args.append("--autobackup-actual-run-sync")
            Popen([sys.executable, *args])
            return
        end_time = self._get_end_time()
        self._run_sync(messages_fn, period, end_time)

    def _run_sync(self, messages_fn, period, end_time):
        while datetime.datetime.now(tz=datetime.timezone.utc) < end_time:
            self._safe_run(messages_fn(), between=period)
            time.sleep(5)

    @classmethod
    def load_unsent_messages(cls):
        message_list = []
        try:
            with open(cls.BACKUP_FILE, 'rb') as fp:
                message_list = pickle.load(fp)
            log.info('Loaded %d backed up messages from %s',
                     len(message_list), cls.BACKUP_FILE)
        except (IOError, EOFError) as e:
            log.info('Error reading from ' + cls.BACKUP_FILE + \
                     ', assume nothing backed up')
        return message_list

    @classmethod
    def dump_unsent_messages(cls, message_list):
        with open(cls.BACKUP_FILE, 'wb') as f:
            log.info('Save %d unsent messages to %s', len(message_list),
                     cls.BACKUP_FILE)

            pickle.dump(message_list, f)
            os.fsync(f)

    def send_all_messages(self, auth, message_list, current=False):
        if not self.args.insecure:
            ssl = network.check_ssl()
        else:
            ssl = None

        num_messages = len(message_list)
        send_all = self.args.submit or self.args.backup
        retries = self.RETRY_LIMIT

        if send_all:
            timeout = None
            stop_time = datetime.datetime.max
            retries = self.RETRY_LIMIT * 2
        else:
            timeout = self.SHORT_TIMEOUT
            stop_time = datetime.datetime.now() + datetime.timedelta(seconds=timeout)
            log.info('Setting timeout to %d seconds', timeout)

        first_response = None
        error_msg = ''
        log.info("Sending {0} messages".format(num_messages))

        while retries > 0 and message_list and datetime.datetime.now() < stop_time:
            log.info('Sending messages...%d left', len(message_list))

            print('Backup... {percent}% complete'.format(percent=100 - round(len(message_list) * 100 / num_messages,
                                                                             2)),
                  end='\r')

            # message_list is assumed to be ordered in chronological order.
            # We want to send the most recent message first, and send older
            # messages after.
            message = message_list[-1]

            try:
                response = self.send_messages(auth, message, timeout, current)
            except requests.exceptions.Timeout as ex:
                log.warning("HTTP request timeout: %s", str(ex))
                retries -= 1
                error_msg = 'Connection timed out after {} seconds. '.format(timeout) + \
                            'Please check your network connection.'
            except (requests.exceptions.RequestException, requests.exceptions.BaseHTTPError) as ex:
                log.warning('%s: %s', ex.__class__.__name__, str(ex))
                retries -= 1
                if getattr(ex, 'response', None) is None:
                    error_msg = 'Please check your network connection.'
                    continue
                try:
                    response_json = ex.response.json()
                except ValueError as ex:
                    log.warning("Invalid JSON Response", exc_info=True)
                    retries -= 1
                    error_msg = 'The server did not provide a valid response. Try again soon.'
                    continue

                log.warning('%s error message: %s', ex.__class__.__name__,
                            response_json['message'])

                if ex.response.status_code == 401:  # UNAUTHORIZED (technically authorization != authentication, but oh well)
                    raise exceptions.AuthenticationException(response_json.get('message'))  # raise this for the caller
                elif ex.response.status_code == 403:
                    retries = 0
                    error_msg = response_json['message']
                else:
                    retries -= 1
                    error_msg = response_json['message']
            except Exception as ex:
                if ssl and isinstance(ex, ssl.CertificateError):
                    retries = 0
                    log.warning("SSL Error: %s", str(ex))
                    error_msg = 'SSL Verification Error: {}\n'.format(ex) + \
                                'Please check your network connection and SSL configuration.'
                else:
                    retries -= 1
                    log.warning(error_msg, exc_info=True)
                    error_msg = "Unknown Error: {}".format(ex)
            else:
                if not first_response:
                    first_response = response
                message_list.pop()

        if current and error_msg:
            print()  # Preserve progress bar.
            print_error('Could not backup:', error_msg)
        elif not message_list:
            print('Backup... 100% complete')
            due_date = self.get_due_date(auth, timeout)
            if due_date is not None:
                now = datetime.datetime.now(tz=datetime.timezone.utc)
                time_to_deadline = due_date - now
                if time_to_deadline < datetime.timedelta(0):
                    print_error("Backup past deadline by", display_timedelta(-time_to_deadline))
                elif time_to_deadline < datetime.timedelta(hours=10):
                    print_warning("Assignment is due in", display_timedelta(time_to_deadline))
            return first_response
        elif not send_all:
            # Do not display any error messages if --backup or --submit are not
            # used.
            print()
        elif not error_msg:
            # No errors occurred, but could not complete request within TIMEOUT.
            print()  # Preserve progress bar.
            print_error('Could not backup within {} seconds.'.format(timeout))
        else:
            # If not all messages could be backed up successfully.
            print()  # Preserve progress bar.
            print_error('Could not backup:', error_msg)

    def send_messages(self, auth, messages, timeout, current):
        """Send messages to server, along with user authentication."""
        with tempfile.NamedTemporaryFile(mode='wb+', prefix='sicp-backup-', suffix='.zip') as temp:
            with ZipFile(temp, mode='w', compression=zipfile.ZIP_DEFLATED) as z:
                for f in messages['file_contents']:
                    if type(messages['file_contents'][f]) == str:
                        z.writestr(f, messages['file_contents'][f])
                z.close()
            temp.flush()
            temp.seek(0, os.SEEK_SET)
            address = self.BACKUP_ENDPOINT.format(server=self.assignment.server_url)
            headers = {'Authorization': 'Bearer {}'.format(auth['token'])}
            files = {'file': temp}
            data = {
                'assignmentId': self.assignment.endpoint,
                'analytics': json.dumps(messages['analytics'])
            }

            log.info('Sending messages to %s', address)
            response = requests.post(address, headers=headers, files=files, data=data, timeout=timeout)
            response.raise_for_status()
            return response.json()

    def get_due_date(self, auth, timeout):
        address = '{}/{}'.format(self.ASSIGNMENT_ENDPOINT.format(server=self.assignment.server_url),
                                 self.assignment.endpoint)
        response = requests.get(address,
                                headers={'Authorization': 'Bearer {}'.format(auth['token'])},
                                timeout=timeout)
        response_data = response.json()
        if 'end_time' not in response_data:
            return
        return datetime.datetime.fromisoformat(response_data['end_time'])


protocol = BackupProtocol
