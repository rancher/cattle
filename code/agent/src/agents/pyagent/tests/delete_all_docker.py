#!/usr/bin/env python2

from docker import Client


def delete_all():
    client = Client()
    for c in client.containers(trunc=False):
        print("Killing {0}".format(c["Id"]))
        client.kill(c["Id"])

    for c in client.containers(all=True, trunc=False):
        print("Removing {0}".format(c["Id"]))
        client.remove_container(c["Id"])

    #for i in client.images(all=True):
    #    try:
    #        print("Removing Image {0}".format(i["Id"]))
    #        client.remove_image(i["Id"])
    #    except:
    #        logging.exception("Failed to delete %s", i["Id"])


if __name__ == "__main__":
    delete_all()
