import logging


logging.basicConfig(level=logging.DEBUG)


def info(*args):
    logging.info(*args)


def debug(*args):
    logging.debug(*args)


def error(*args):
    logging.error(*args)


def exception(*args):
    logging.exception(*args)
