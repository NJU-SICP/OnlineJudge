from datetime import datetime
import logging
import requests

from client.protocols.common import models
from client.utils import network
from client.utils.printer import print_warning, print_success, print_error

log = logging.getLogger(__name__)


class QueryProtocol(models.Protocol):
    SHORT_TIMEOUT = 5
    ASSIGNMENT_ENDPOINT = '{server}/assignments'
    SUBMISSION_ENDPOINT = '{server}/submissions'

    def run(self, messages, nointeract=False):
        if not self.args.query:
            return

        if not self.assignment.endpoint:
            log.info('No assignment endpoint, cannot submit')
            return

        if self.args.local:
            print_error("Cannot query with --local option set.")
            return

        if not self.args.insecure:
            network.check_ssl()

        try:
            auth = self.assignment.authenticate(nointeract=nointeract)
            if not auth:
                print_error('Not authenticated. Cannot query submissions.')
                return
            self.query(auth)
        except requests.HTTPError as e:
            print_error('Cannot query: {}'.format(e.response.json()['message']))

    def query(self, auth):
        stop = False
        page, submissions = 1, []
        while not stop:
            address = self.SUBMISSION_ENDPOINT.format(server=self.assignment.server_url)
            headers = {'Authorization': 'Bearer {}'.format(auth['token'])}
            params = {
                'userId': auth['userId'],
                'assignmentId': self.assignment.endpoint,
                'page': page - 1,
                'size': 10
            }
            response = requests.get(address, headers=headers, params=params)
            response.raise_for_status()
            totalElements = response.json()['totalElements']
            totalPages = response.json()['totalPages']
            submissions = []
            for index, item in enumerate(response.json()['content']):
                submissions.append({
                    'index': totalElements - 10 * (page - 1) - index,
                    'id': item['id'],
                    'time': datetime.fromisoformat(item['createdAt']).astimezone(),
                    'score': item['result']['score'] if 'result' in item
                                                        and item['result'] is not None
                                                        and 'score' in item['result']
                                                        and item['result']['score'] is not None
                    else ('Error' if 'result' in item
                                     and item['result'] is not None
                                     and 'error' in item['result']
                                     and item['result']['error'] is not None else '')
                })
            print('Submissions of {assignment} by {username} {fullName} (page {currentPage} of {totalPages})'
                  .format(assignment=self.assignment.endpoint, username=auth['username'], fullName=auth['fullName'],
                          currentPage=page, totalPages=totalPages))
            print('Open in browser: https://sicp.pascal-lab.net/2022/oj/assignments')
            print('---------------------------------------------------------------------')
            print('     ID        Time         Score                                    ')
            print('---------------------------------------------------------------------')
            for item in submissions:
                print("#{index:02d}  {id}  {time:%m-%d %H:%M}  {score}"
                      .format(index=item['index'], id=item['id'][-8:], time=item['time'], score=item['score']))
            print('---------------------------------------------------------------------')
            select = True
            while select:
                select = False
                action = input("Action: #[??]=detail  [p]rev / [n]ext  [r]efresh  [Q]uit? ").strip().lower()
                print()
                if action == '' or action.startswith('q'):
                    stop = True
                elif action.startswith('n'):
                    page = page if page == totalPages else (page + 1)
                elif action.startswith('p'):
                    page = page if page == 1 else (page - 1)
                elif action.startswith('r'):
                    continue
                else:
                    select = True
                    index = None
                    try:
                        index = int(action)
                    except Exception:
                        pass
                    if index is not None:
                        for item in submissions:
                            if item['index'] == index:
                                response = requests.get('{}/{}'.format(address, item['id']), headers=headers)
                                response.raise_for_status()
                                submission = response.json()
                                print('Details of submission #{index}:'.format(index=index))
                                if 'result' in submission and 'details' in submission['result'] \
                                        and submission['result']['details'] is not None:
                                    for detail in submission['result']['details']:
                                        print('  -> {title}: {score}'.format(title=detail['title'],
                                                                             score=detail['score']))
                                elif 'result' in submission:
                                    if 'error' in submission['result'] \
                                            and submission['result']['error'] is not None:
                                        print('  -> cannot grade: {}'.format(submission['result']['error']))
                                    else:
                                        print('  -> not graded yet')
                                break


protocol = QueryProtocol
