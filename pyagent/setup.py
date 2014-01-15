from setuptools import setup, find_packages
from distutils.core import setup

setup(
    name='dStackAgent',
    version='0.1',
    packages=find_packages(),
    license='ASL 2.0',
    long_description=open('README.txt').read(),
)
