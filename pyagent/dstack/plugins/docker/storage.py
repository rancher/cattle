from dstack import log
from dstack import utils
from dstack.storage import BaseStoragePool
from docker import Client
import json

class Template(object):
    def __init__(self, client, image=None):
        self._client = client
        self._image = image


    @staticmethod
    def from_template_or_pool_ref(client, template_or_pool_ref):
        image = utils.get_data(template_or_pool_ref, prefix="docker.image", strip_prefix=True)
        if image.get("Id") is None and image.get("Repository") is None:
            raise Exception("Invalid template, both Id and Repository are null")

        if not image.get("Repository") is None and image.get("Tag") is None:
            image["Tag"] = "latest"

        return Template(client, image)


    def delete(self):
        image = self._get_image()
        if image is None:
            return

        self._client.remove_image(image["Id"])
        return image

    def _parse_status(self, status):
        result = []
        current = 0
        try:
            while current <= len(status):
                i = status.index("}", current)
                part = status[current:i+1]
                result.append(json.loads(part))
                current = i+1
        except ValueError:
            pass

        return result


    def pull(self):
        image = self._get_image()
        if not image is None:
            return image

        id, repo, tag = self._lookup_data(self._image)

        pull_result = self._client.pull(repo, tag=tag)
        for status in self._parse_status(pull_result):
            log.info("%s: %s [%s]" % (status.get("id"), status.get("status"), status.get("progress")))

        image = None
        if status.get("progress") == "complete":
            image = self._get_image(repo=repo, tag=tag)
            if not image is None:
                self._image = (image["Id"], image["Repository"], image["Tag"])

        return image

    def _lookup_data(self, image):
        if image is None:
            return (None, None, None)

        id = image.get("Id")
        repo = image.get("Repository")
        tag = image.get("Tag")
        
        return (id, repo, tag)

    def _get_image(self, id=None, repo=None, tag=None):
        result = []
        
        if repo is None:
            id, repo, tag = self._lookup_data(self._image)

        if id is None:
            if not repo is None:
                for image in self._client.images(name=repo):
                    if image["Tag"] == tag:
                        result.append(image)
        else:
            for image in filter(lambda x: x["Id"] == id, self._client.images()):
                result.append(image)

        if len(result) == 0:
            return None

        if len(result) > 1:
            log.error("Found multiple results for %s" % ref)

        return result[0]



class DockerPool(BaseStoragePool):
    def delete_template(self, storagePool=None, template=None,
                        templateStoragePoolRef=None, **kw):
        template = Template.from_template_or_pool_ref(self._get_client(), templateStoragePoolRef)
        template.delete()


    def stage_template(self, storagePool=None, template=None, **kw):
        template = Template.from_template_or_pool_ref(self._get_client(), template)
        stage_result = template.pull()
        return stage_result

        
    def _get_client(self):
        return Client()
