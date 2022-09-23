# Modifications copyright (C) 2021 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

from datetime import datetime
from getpass import getpass
import hashlib
import http.server
import logging
import pickle
import requests
from urllib.parse import urlencode, urlparse, parse_qsl
import time
import webbrowser

from client import __version__
from client.exceptions import AuthenticationException, OAuthException
from client.utils.config import (AUTH_FILE,
                                 create_config_directory)
from client.utils.printer import print_error
from client.utils import format, network

log = logging.getLogger(__name__)

TIMEOUT = 10
#LOGIN_ENDPOINT = '/auth/login'
REFRESH_ENDPOINT = '/auth/refresh'
GITLAB_ENDPOINT_1 = '/auth/gitlab/login'
GITLAB_ENDPOINT_2 = '/auth/gitlab/login/callback'
GITLAB_ENDPOINT_3 = '/auth/gitlab/login/success'


def post(server, endpoint, json, headers):
    """Try getting an access token from the server. If successful, returns the
    JSON response. If unsuccessful, raises an OAuthException.
    """
    try:
        response = requests.post(server + endpoint, json=json, headers=headers, timeout=TIMEOUT)
        body = response.json()
    except Exception as e:
        log.warning('Other error when exchanging code', exc_info=True)
        raise OAuthException(
            error='Authentication Failed',
            error_description=str(e))
    if 'error' in body:
        log.error(body)
        raise OAuthException(
            error=body.get('error', 'Unknown Error'),
            error_description=body.get('error_description', ''))
    return body


#def make_code_post(server, username, password):
#    json = {
#        'username': username,
#        'password': password,
#        'platform': 'ok-{}'.format(__version__)
#    }
#    return post(server, LOGIN_ENDPOINT, json, None)


def make_code_post_via_gitlab(server, code):
    json = {
        'code': code,
        'state': 'ok',
        'platform': 'ok-{}'.format(__version__)
    }
    return post(server, GITLAB_ENDPOINT_2, json, None)


def make_refresh_post(server, auth):
    headers = {'Authorization': 'Bearer ' + auth['token']}
    json = {'platform': 'ok-{}'.format(__version__)}
    return post(server, REFRESH_ENDPOINT, json, headers)


def get_storage():
    create_config_directory()
    with open(AUTH_FILE, 'rb') as fp:
        storage = pickle.load(fp)
    return storage['sicp-auth']


def update_storage(auth):
    if not auth:
        raise AuthenticationException(
            "Authentication failed and returned an empty token.")

    create_config_directory()
    with open(AUTH_FILE, 'wb') as fp:
        pickle.dump({
            'sicp-auth': auth
        }, fp)


def refresh_local_token(server):
    auth = get_storage()
    cur_time = int(time.time())
    iss_time = int(datetime.fromisoformat(auth['issued']).timestamp())
    if cur_time < iss_time + 3600:
        return auth
    auth = make_refresh_post(server, auth)
    if not auth:
        raise AuthenticationException(
            "Authentication failed and returned an empty token.")
    update_storage(auth)
    return auth


def perform_auth(code_fn, *args, **kwargs):
    try:
        auth = code_fn(*args, **kwargs)
    except OAuthException as e:
        with format.block('-'):
            print("Authentication error: {}".format(e.error.replace('_', ' ')))
            if e.error_description:
                print(e.error_description)
    else:
        update_storage(auth)
        return auth


def server_url(cmd_args):
    scheme = 'http' if cmd_args.insecure else 'https'
    return '{}://{}'.format(scheme, cmd_args.server)


def authenticate(cmd_args, force=False, nointeract=False):
    """Returns an OAuth token that can be passed to the server for
    identification. If FORCE is False, it will attempt to use a cached token
    or refresh the OAuth token. If NOINTERACT is true, it will return None
    rather than prompting the user.
    """
    server = server_url(cmd_args)
    network.check_ssl()
    auth = None

    try:
        assert not force
        auth = refresh_local_token(server)
    except Exception:
        if nointeract:
            return auth
        print('Performing authentication')
        auth = perform_auth(get_code_via_gitlab, cmd_args)
        #if cmd_args.oauth_gitlab:
        #    auth = perform_auth(get_code_via_gitlab, cmd_args)
        #else:
        #    auth = perform_auth(get_code, cmd_args)
    
    if auth is None:
        print_error('Error: null response')
        return None
    elif 'status' in auth:
        print_error('Error: {}'.format(repr(auth)))
        return None
    else:
        log.debug('Authenticated with {}'.format(auth))
        return auth


def notebook_authenticate(cmd_args, force=False, silent=True, nointeract=False):
    raise NotImplemented


#def get_code(cmd_args):
#    server = server_url(cmd_args)
#    username = input("Please input your NJU ID: ")
#    password = getpass("Please input your password: ")
#    return make_code_post(server, username, password)


def get_code_via_gitlab(cmd_args):
    server = server_url(cmd_args)
    code_response = None
    oauth_exception = None

    class CodeHandler(http.server.BaseHTTPRequestHandler):
        def do_GET(self):
            """Respond to the GET request made by the OAuth"""
            nonlocal code_response, oauth_exception
            log.debug('Received GET request for %s', self.path)
            path = urlparse(self.path)
            qs = {k: v for k, v in parse_qsl(path.query)}
            code = qs.get('code')
            if code:
                try:
                    code_response = make_code_post_via_gitlab(server, code)
                except OAuthException as e:
                    oauth_exception = e
            else:
                oauth_exception = OAuthException(
                    error=qs.get('error', 'Unknown Error'),
                    error_description = qs.get('error_description', ''))

            if oauth_exception:
                print_error('{}\n{}'.format(oauth_exception.error, server, ERROR_ENDPOINT, urlencode(params)))
            else:
                self.send_response(302)
                self.send_header("Location", '{}{}'.format(server, GITLAB_ENDPOINT_3))
                self.end_headers()

        def log_message(self, format, *args):
            return

    if cmd_args.code is None:
        host_name = "localhost"
        port_number = 2830  # SICP
        server_address = (host_name, port_number)
        log.info("Authentication server running on {}:{}".format(host_name, port_number))

        assert webbrowser.open_new('{}/auth/gitlab/login?state=ok'.format(server))
        try:
            httpd = http.server.HTTPServer(server_address, CodeHandler)
            httpd.handle_request()
        except OSError as e:
            log.warning("HTTP Server Err {}".format(server_address), exc_info=True)
            raise
    else:
        code = cmd_args.code
        try:
            code_response = make_code_post_via_gitlab(server, code)
        except OAuthException as e:
            oauth_exception = e

    if oauth_exception:
        raise oauth_exception
    return code_response


def display_student_info(cmd_args, auth):
    try:
        username = auth['username']
        full_name = auth['fullName']
        print('Successfully logged in as ', username, full_name)
        return username
    except Exception:  # Do not catch KeyboardInterrupts
        log.debug("Did not obtain student info", exc_info=True)
        return None


def get_student_info(cmd_args):
    """Attempts to get the student's email. Returns the email, or None."""
    log.info("Attempting to get student info")
    if cmd_args.local:
        return None
    auth = authenticate(cmd_args, force=False)
    if not auth:
        return None
    try:
        return auth['username']
    except IOError as e:
        return None


def get_identifier(cmd_args):
    """ Obtain anonmyzied identifier."""
    username = get_student_info(cmd_args)
    if not username:
        return "Unknown"
    return hashlib.md5(username.encode()).hexdigest()
