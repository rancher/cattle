from os.path import dirname
import os, sys

_file = os.path.abspath(__file__)
sys.path.insert(0, dirname(dirname(_file)))
