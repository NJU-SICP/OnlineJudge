# Modifications copyright (C) 2021 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

__version__ = 'NJU-SICP-v1.3.0'

FILE_NAME = 'ok'

import os
import sys

sys.path.insert(0, '')
# Add directory in which the ok.zip is stored to sys.path.
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
