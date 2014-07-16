from distutils.core import setup

setup(
    name='CattleValidationTests',
    version='0.1',
    packages=[
        'cattlevalidationtest',
        'cattlevalidationtest.core',
    ],
    license='ASL 2.0',
    long_description=open('README.txt').read(),
)
