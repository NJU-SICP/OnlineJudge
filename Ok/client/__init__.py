# Modifications copyright (C) 2021-2022 Tianyun Zhang
# This file has been modified to adapt to SICP course at Nanjing University.

import os
import sys

FILE_NAME = 'ok'
__version__ = '2022.12.03'

sys.path.insert(0, '')
# Add directory in which the ok.zip is stored to sys.path.
sys.path.append(os.path.dirname(os.path.abspath(os.path.dirname(__file__))))
