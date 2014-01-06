from distutils.core import setup

setup(
    name='dStackIntegrationTests',
    version='0.1',
    packages=[
      'dstacktest',
      'dstacktest.core',
    ],
    license='ASL 2.0',
    long_description=open('README.txt').read(),
)
