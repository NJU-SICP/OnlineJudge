# Modifications copyright (C) 2021 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

from datetime import datetime
from getpass import getpass
import hashlib
import logging
import pickle
import requests
import time

from client import __version__
from client.exceptions import AuthenticationException, OAuthException
from client.utils.config import (AUTH_FILE,
                                 create_config_directory)
from client.utils import format, network

log = logging.getLogger(__name__)

TIMEOUT = 10
LOGIN_ENDPOINT = '/auth/login'
REFRESH_ENDPOINT = '/auth/refresh'


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


def make_code_post(server, username, password):
    json = {
        'username': username,
        'password': password,
        'platform': 'ok-{}'.format(__version__)
    }
    return post(server, LOGIN_ENDPOINT, json, None)


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
        auth = perform_auth(get_code, cmd_args)
    log.debug('Authenticated with {}'.format(auth))

    return auth


def notebook_authenticate(cmd_args, force=False, silent=True, nointeract=False):
    raise NotImplemented


def get_code(cmd_args):
    server = server_url(cmd_args)
    username = input("Please input your NJU ID: ")
    password = getpass("Please input your password: ")
    return make_code_post(server, username, password)


def get_code_via_browser(cmd_args, redirect_uri, host_name, port_number, endpoint):
    raise NotImplemented


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
