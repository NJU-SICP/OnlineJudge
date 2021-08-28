# Modifications copyright (C) 2021 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

import os

CONFIG_DIRECTORY = os.path.join(os.path.expanduser('~'), '.config', 'ok')
AUTH_FILE = os.path.join(CONFIG_DIRECTORY, "sicp-auth")
CERT_FILE = os.path.join(CONFIG_DIRECTORY, "cacert.pem")

def create_config_directory():
    if not os.path.exists(CONFIG_DIRECTORY):
        os.makedirs(CONFIG_DIRECTORY)
    return CONFIG_DIRECTORY
