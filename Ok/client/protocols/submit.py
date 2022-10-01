# Copyright (C) 2021 Tianyun Zhang

import logging
import os
from time import sleep
import requests
import tempfile
from zipfile import ZipFile

from client.protocols.common import models
from client.utils import network
from client.utils.printer import print_warning, print_success, print_error

log = logging.getLogger(__name__)


class SubmitProtocol(models.Protocol):
    SHORT_TIMEOUT = 5
    ASSIGNMENT_ENDPOINT = '{server}/assignments'
    SUBMISSION_ENDPOINT = '{server}/submissions'

    def run(self, messages, nointeract=False):
        if not self.args.submit:
            return

        if not self.assignment.endpoint:
            log.info('No assignment endpoint, cannot submit')
            return

        if self.args.local:
            print_error('Cannot submit with --local option set.')
            return

        if not self.args.insecure:
            network.check_ssl()

        try:
            auth = self.assignment.authenticate(nointeract=nointeract)
            if not auth:
                print_error('Not authenticated. Cannot submit assignment.')
                return
            self.submit(auth)
        except requests.HTTPError as e:
            if e.response.status_code == 404:
                message = "assignment does not exist"
            else:
                message = e.response.json()['message']
            print_error('Cannot submit: {}'.format(message))
        except Exception as e:
            print_error('Error in submit: {}'.format(e))

    def submit(self, auth):
        print("Submitting {assignment} by {username} {fullName}."
              .format(assignment=self.assignment.endpoint, username=auth['username'], fullName=auth['fullName']))
        if not self.args.token:
            limit = self.get_submit_limit(auth)
            count = self.get_submit_count(auth)
            if limit == 0:
                print_error('This assignment does not allow submission.')
                return
            elif limit > 0:
                print_warning(
                    'This assignment has submit limit of {} times.'.format(limit))
                print_warning(
                    'You have already submitted {} / {} times.'.format(count, limit))
                action = input(
                    'Submit your code? [y]es / [N]o ').strip().lower()
                if not action.startswith('y'):
                    print('Aborted.')
                    return

        if len(self.assignment.src) == 1:
            if '.' in self.assignment.src[0]:
                suffix = '.' + self.assignment.src[0].split('.')[-1]
            else:
                suffix = '.py'  # python source file by default
        else:
            suffix = '.zip'
        log.info("submit file type will be {}".format(suffix))
        with tempfile.NamedTemporaryFile(mode='wb+', prefix='sicp-submit-', suffix=suffix) as temp:
            log.info("create temp file {}".format(temp.name))
            if len(self.assignment.src) == 1:
                with open(self.assignment.src[0], mode='rb') as f:
                    temp.write(f.read())
            else:
                with ZipFile(temp, mode='w') as z:
                    for f in self.assignment.src:
                        z.write(f)
                    z.close()
            temp.flush()
            temp.seek(0, os.SEEK_SET)
            address = self.SUBMISSION_ENDPOINT.format(
                server=self.assignment.server_url)
            headers = {'Authorization': 'Bearer {}'.format(auth['token'])}
            files = {'file': temp}
            data = {'assignmentId': self.assignment.endpoint,
                    'token': self.args.token}
            request = requests.post(
                address, headers=headers, files=files, data=data)
            request.raise_for_status()
            result = request.json()
            print_success(
                'Online Judge received submission ID {}'.format(result['id'][-8:]))
            print_success(
                'Open in browser: https://sicp.pascal-lab.net/2022/oj/assignments')
            if not self.args.no_wait:
                self.wait_for_grade(auth, result['id'])

    def wait_for_grade(self, auth, id):
        log.info("wait for grade")
        print("\nWaiting for online judge to grade, press Ctrl+C to exit\n")
        address = '{}/{}'.format(self.SUBMISSION_ENDPOINT.format(
            server=self.assignment.server_url), id)
        headers = {'Authorization': 'Bearer {}'.format(auth['token'])}
        for time_spent in range(180):
            sleep(1.0)
            print("... {}s".format(time_spent), end='\r')
            response = requests.get(address, headers=headers)
            response.raise_for_status()
            submission = response.json()
            if 'result' in submission and submission['result'] is not None:
                if 'details' in submission['result'] and submission['result']['details'] is not None:
                    print()
                    print(
                        '---------------------------------------------------------------------')
                    print_success(
                        f"Submission graded: score={submission['result']['score']}")
                    for detail in submission['result']['details']:
                        print(
                            f" -> {detail['title']}: {detail['score']}", end='')
                        if detail['message']:
                            print(f" ({detail['message']})", end='')
                        print()
                    return
                elif 'error' in submission['result'] and submission['result']['error'] is not None:
                    print()
                    print(
                        '---------------------------------------------------------------------')
                    print_error(
                        f"Submission failed to grade: {submission['result']['error']}")
                    return
        print("Timeout (180s) waiting for grade")

    def get_submit_limit(self, auth):
        log.info("fetching assignment")
        address = '{}/{}'.format(self.ASSIGNMENT_ENDPOINT.format(server=self.assignment.server_url),
                                 self.assignment.endpoint)
        headers = {'Authorization': 'Bearer {}'.format(auth['token'])}
        response = requests.get(address, headers=headers)
        response.raise_for_status()
        log.info("fetched assignment {}".format(response.json()))
        return response.json()['submitCountLimit']

    def get_submit_count(self, auth):
        log.info("fetch submission count")
        address = '{}/count'.format(self.SUBMISSION_ENDPOINT.format(
            server=self.assignment.server_url))
        headers = {'Authorization': 'Bearer {}'.format(auth['token'])}
        params = {
            'userId': auth['userId'],
            'assignmentId': self.assignment.endpoint
        }
        response = requests.get(address, headers=headers, params=params)
        response.raise_for_status()
        log.info("fetched submission count {}".format(response.json()))
        return response.json()


protocol = SubmitProtocol
