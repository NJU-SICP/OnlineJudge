# Modifications copyright (C) 2021 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

import logging
import os
import requests

from client.utils.printer import print_error, print_success

log = logging.getLogger(__name__)

VERSION_ENDPOINT = '{server}/misc/ok-client/version'
DOWNLOAD_ENDPOINT = '{server}/misc/ok-client/{version}'

SHORT_TIMEOUT = 15  # seconds


def check_version(insecure, server, version, filename, timeout=SHORT_TIMEOUT, verbose=False):
    """Check for the latest version of OK and update accordingly."""

    scheme = 'http' if insecure else 'https'
    address = '{}://{}'.format(scheme, VERSION_ENDPOINT.format(server=server))

    log.info('Checking for software updates...')
    log.info('Existing OK version: %s', version)
    log.info('Checking latest version from %s', address)

    try:
        response = requests.get(address, timeout=timeout)
        response.raise_for_status()
    except (requests.exceptions.RequestException, requests.exceptions.BaseHTTPError) as e:
        print_error('Network error when checking for updates.')
        log.warning('Network error when checking version from %s: %s', address,
                    str(e), stack_info=True)
        return False

    current_version = response.text
    if current_version == version:
        if verbose:
            print_success('OK is up to date')
        return True
    else:
        print("Updating OK: {}".format(current_version))

    download_link = '{}://{}'.format(scheme, DOWNLOAD_ENDPOINT.format(server=server, version=current_version))
    log.info('Downloading version %s from %s', current_version, download_link)
    try:
        response = requests.get(download_link, timeout=timeout)
        response.raise_for_status()
    except (requests.exceptions.RequestException, requests.exceptions.BaseHTTPError) as e:
        print_error('Error when downloading new version of OK')
        log.warning('Error when downloading new version of OK: %s', str(e),
                    stack_info=True)
        return False

    log.info('Writing new version to %s', filename)

    zip_binary = response.content
    try:
        _write_zip(filename, zip_binary)
    except IOError as e:
        print_error('Error when downloading new version of OK')
        log.warning('Error writing to %s: %s', filename, str(e))
        return False
    else:
        print_success('Updated to version: {}'.format(current_version))
        log.info('Successfully wrote to %s', filename)
        return True


def _validate_api_response(data):
    return 'current_version' in data and \
           'download_link' in data


def _write_zip(zip_name, zip_contents):
    with open(zip_name, 'wb') as f:
        f.write(zip_contents)
        os.fsync(f)
