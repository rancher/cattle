#!/usr/bin/env python
# PYTHON_ARGCOMPLETE_OK

import requests
import collections
import hashlib
import os
import json
import time
import argcomplete


def _prefix(cmd):
    prefix = os.path.basename(cmd.replace("-", "_"))
    for i in [".py", "-cli", "-tool", "-util"]:
        prefix = prefix.replace(i, "")
    return prefix.upper()

PREFIX = _prefix(__file__)
CACHE_DIR = "~/." + PREFIX.lower()
TIME = not os.environ.get("TIME_API") is None

LIST = "list-"
CREATE = "create-"
UPDATE = "update-"
DELETE = "delete-"
ACTION = "action-"
COMMAND_TYPES = [LIST, CREATE, UPDATE, DELETE, ACTION]

GET_METHOD = "GET"
POST_METHOD = "POST"
PUT_METHOD = "PUT"
DELETE_METHOD = "DELETE"

HEADERS = {"Accept": "application/json"}

LIST_METHODS = {"__iter__": True, "__len__": True, "__getitem__": True}


def echo(fn):
    def wrapped(*args, **kw):
        ret = fn(*args, **kw)
        print fn.__name__, repr(ret)
        return ret
    return wrapped


def timed_url(fn):
    def wrapped(*args, **kw):
        if TIME:
            start = time.time()
            ret = fn(*args, **kw)
            delta = time.time() - start
            print delta, args[1], fn.__name__
            return ret
        else:
            return fn(*args, **kw)
    return wrapped


class RestObject:
    def _is_public(self, k, v):
        return k not in ["links", "actions", "id", "type"] and not callable(v)

    def __str__(self):
        return self.__repr__()

    def _as_table(self):
        if not hasattr(self, "type"):
            return str(self.__dict__)
        data = [("Type", "Id", "Name", "Value")]
        for k, v in self.iteritems():
            if self._is_public(k, v):
                if v is None:
                    v = "null"
                if v is True:
                    v = "true"
                if v is False:
                    v = "false"
                data.append((self.type, self.id, str(k), str(v)))

        return indent(data, hasHeader=True, prefix='| ', postfix=' |',
                      wrapfunc=lambda x: str(x))

    def _is_list(self):
        return "data" in self.__dict__ and isinstance(self.data, list)

    def __repr__(self):
        data = {}
        for k, v in self.__dict__.iteritems():
            if self._is_public(k, v):
                data[k] = v
        return repr(data)

    def __getattr__(self, k):
        if self._is_list() and k in LIST_METHODS:
            return getattr(self.data, k)
        return getattr(self.__dict__, k)

    def __iter__(self):
        if self._is_list():
            return iter(self.data)

class Schema:
    def __init__(self, text, obj):
        self.text = text
        self.types = {}
        for t in obj:
            self.types[t.id] = t
            for old_name in ["methods", "actions", "fields"]:
                if hasattr(t, old_name):
                    vars(t)["resource" + old_name.capitalize()] =\
                        vars(t)[old_name]

            t.creatable = False
            try:
                if POST_METHOD in t.collectionMethods:
                    t.creatable = True
            except:
                pass

            t.updatable = False
            try:
                if PUT_METHOD in t.resourceMethods:
                    t.updatable = True
            except:
                pass

            t.deletable = False
            try:
                if DELETE_METHOD in t.resourceMethods:
                    t.deletable = True
            except:
                pass

            t.listable = False
            try:
                if GET_METHOD in t.collectionMethods:
                    t.listable = True
            except:
                pass

            if not hasattr(t, "collectionFilters"):
                t.collectionFilters = {}

    def __str__(self):
        return str(self.text)

    def __repr(self):
        return repr(self.text)


class ApiError(Exception):
    def __init__(self, obj):
        self.error = obj
        try:
            super(ApiError, self).__init__(self, "{} : {}\n{}".format(obj.code, obj.message, obj))
        except:
            super(ApiError, self).__init__(self, "API Error")


class ClientApiError(Exception):
    pass


class Client:
    def __init__(self, access_key=None, secret_key=None, url=None, cache=None,
                 cache_time=86400, strict=False, **kw):
        self._access_key = access_key
        self._secret_key = secret_key
        self._auth = (self._access_key, self._secret_key)
        self._url = url
        self._cache = cache
        self._cache_time = cache_time
        self._strict = strict
        self.schema = None

        if not self._cache_time:
            self._cache_time = 60 * 60 * 24  # 24 Hours

        if self.valid():
            self._load_schemas()

    def valid(self):
        return self._access_key is not None and self._secret_key is not None \
            and self._url is not None

    def object_hook(self, obj):
        if isinstance(obj, list):
            return [self.object_hook(x) for x in obj]

        if isinstance(obj, dict):
            result = RestObject()

            for k, v in obj.iteritems():
                setattr(result, k, self.object_hook(v))

            if hasattr(result, "type") and isinstance(getattr(result, "type"), basestring):
                if hasattr(result, "links"):
                    for link_name, link in result.links.iteritems():
                        cb = lambda link=link: \
                            lambda **kw: self._get(link, data=kw)
                        setattr(result, link_name, cb())

                if hasattr(result, "actions"):
                    for link_name, link in result.actions.iteritems():
                        cb = lambda link_name=link_name, result=result: \
                            lambda *args, **kw: \
                            self.action(result, link_name, *args, **kw)
                        setattr(result, link_name, cb())

            return result

        return obj

    def object_pairs_hook(self, pairs):
        ret = collections.OrderedDict()
        for k, v in pairs:
            ret[k] = v
        return self.object_hook(ret)

    def _get(self, url, data=None):
        return self._unmarshall(self._get_raw(url, data=data))

    def _error(self, text):
        raise ApiError(self._unmarshall(text))

    @timed_url
    def _get_raw(self, url, data=None):
        r = self._get_response(url, data)
        return r.text

    def _get_response(self, url, data=None):
        r = requests.get(url, auth=self._auth, params=data, headers=HEADERS)
        if r.status_code < 200 or r.status_code >= 300:
            self._error(r.text)

        return r

    @timed_url
    def _post(self, url, data=None):
        r = requests.post(url, auth=self._auth, data=self._marshall(data),
                          headers=HEADERS)
        if r.status_code < 200 or r.status_code >= 300:
            self._error(r.text)

        return self._unmarshall(r.text)

    @timed_url
    def _put(self, url, data=None):
        r = requests.put(url, auth=self._auth, data=self._marshall(data),
                         headers=HEADERS)
        if r.status_code < 200 or r.status_code >= 300:
            self._error(r.text)

        return self._unmarshall(r.text)

    @timed_url
    def _delete(self, url):
        r = requests.delete(url, auth=self._auth, headers=HEADERS)
        if r.status_code < 200 or r.status_code >= 300:
            self._error(r.text)

        return self._unmarshall(r.text)

    def _unmarshall(self, text):
        return json.loads(text, object_hook=self.object_hook)

    def _marshall(self, obj):
        if obj is None:
            return None
        return json.dumps(self._to_dict(obj))

    def _load_schemas(self, force=False):
        if self.schema and not force:
            return

        schema_text = self._get_cached_schema()

        if force or not schema_text:
            response = self._get_response(self._url)
            schema_url = response.headers.get("X-API-Schemas")
            if schema_url is not None and self._url != schema_url:
                schema_text = self._get_raw(schema_url)
            else:
                schema_text = response.text
            self._cache_schema(schema_text)

        obj = self._unmarshall(schema_text)
        schema = Schema(schema_text, obj)

        self._bind_methods(schema)
        self.schema = schema

    def reload_schema(self):
        self._load_schemas(force=True)

    def by_id(self, type, id):
        url = self.schema.types[type].links.collection
        if url.endswith("/"):
            url = url + id
        else:
            url = "/".join([url, id])
        try:
            return self._get(url)
        except ApiError, e:
            if e.error.code == "RESOURCE_NOT_FOUND":
                return None
            else:
                raise e

    def update_by_id(self, type, id, *args, **kw):
        url = self.schema.types[type].links.collection
        if url.endswith("/"):
            url = url + id
        else:
            url = "/".join([url, id])

        return self._put(url, data=self._to_dict(*args, **kw))

    def update(self, obj, *args, **kw):
        url = obj.links.self

        for k, v in self._to_dict(*args, **kw).iteritems():
            setattr(obj, k, v)

        return self._put(url, data=obj)

    def _validate_list(self, type, **kw):
        if not self._strict:
            return

        collectionFilters = self.schema.types[type].collectionFilters

        for k in kw:
            if hasattr(collectionFilters, k):
                return

            for filter_name, filter_value in collectionFilters.iteritems():
                for m in filter_value.modifiers:
                    if k == "_".join([filter_name, m]):
                        return

            raise ClientApiError(k + " is not searchable field")

    def list(self, type, **kw):
        if not type in self.schema.types:
            raise ClientApiError(type + " is not a valid type")

        self._validate_list(type, **kw)
        collection_url = self.schema.types[type].links.collection
        return self._get(collection_url, data=kw)

    def reload(self, obj):
        return self.by_id(obj.type, obj.id)

    def create(self, type, *args, **kw):
        collection_url = self.schema.types[type].links.collection
        return self._post(collection_url, data=self._to_dict(*args, **kw))

    def delete(self, *args):
        for i in args:
            if isinstance(i, RestObject):
                self._delete(i.links.self)

    def action(self, obj, action_name, *args, **kw):
        url = obj.actions[action_name]
        return self._post(url, data=self._to_dict(*args, **kw))

    def _to_dict(self, *args, **kw):
        ret = {}

        for i in args:
            if isinstance(i, dict):
                for k, v in i.iteritems():
                    ret[k] = v

            if isinstance(i, RestObject):
                for k, v in vars(i).iteritems():
                    if not k.startswith("_") and \
                            not isinstance(v, RestObject) and not callable(v):
                        ret[k] = v

        for k, v in kw.iteritems():
            ret[k] = v

        return ret

    def _bind_methods(self, schema):
        bindings = [
            ("list", "collectionMethods", GET_METHOD, self.list),
            ("by_id", "collectionMethods", GET_METHOD, self.by_id),
            ("update_by_id", "resourceMethods", PUT_METHOD, self.update_by_id),
            ("create", "collectionMethods", POST_METHOD, self.create)
        ]

        for type_name, type in schema.types.iteritems():
            for method_name, type_collection, test_method, m in bindings:
                # double lambda for lexical binding hack
                cb = lambda type_name=type_name, method=m: \
                    lambda *args, **kw: method(type_name, *args, **kw)
                if hasattr(type, type_collection) and \
                        test_method in type[type_collection]:
                    setattr(self, "_".join([method_name, type_name]), cb())

    def _get_schema_hash(self):
        h = hashlib.new("sha1")
        h.update(self._url)
        h.update(self._access_key)
        return h.hexdigest()

    def _get_cached_schema_file_name(self):
        if not self._cache:
            return None

        h = self._get_schema_hash()

        cachedir = os.path.expanduser(CACHE_DIR)
        if not cachedir:
            return None

        if not os.path.exists(cachedir):
            os.mkdir(cachedir)

        return os.path.join(cachedir, "schema-" + h + ".json")

    def _cache_schema(self, text):
        cached_schema = self._get_cached_schema_file_name()

        if not cached_schema:
            return None

        with open(cached_schema, "w") as f:
            f.write(text)

    def _get_cached_schema(self):
        cached_schema = self._get_cached_schema_file_name()

        if not cached_schema:
            return None

        if os.path.exists(cached_schema):
            mod_time = os.path.getmtime(cached_schema)
            if time.time() - mod_time < self._cache_time:
                with open(cached_schema) as f:
                    data = f.read()
                return data

        return None

    def _all_commands(self, lst=True, update=True, remove=True, create=True):
        for type, schema in self.schema.types.iteritems():
            if schema.listable:
                yield LIST + type
            if schema.creatable:
                yield CREATE + type
            if schema.updatable:
                yield UPDATE + type
            if schema.deletable:
                yield DELETE + type

            if hasattr(schema, "resourceActions"):
                for k in schema.resourceActions:
                    yield ACTION + "-".join([type, k])

    def _possible_args(self, command_type, type_name, action_name):
        result = []
        type_def = self.schema.types[type_name]

        if command_type == ACTION:
            result.append("id")
            action_def = type_def.resourceActions[action_name]
            if hasattr(action_def, "input"):
                type_def = self.schema.types[action_def.input]

        if command_type == LIST and type_def.listable:
            try:
                for name, filter in type_def.collectionFilters.iteritems():
                    result.append(name)
                    for m in filter.modifiers:
                        if m != "eq":
                            result.append(name + "_" + m)
            except:
                pass

        try:
            for name, field in type_def.resourceFields.iteritems():
                if ((command_type == CREATE and type_def.creatable) or
                        command_type == ACTION) and \
                        hasattr(field, "create") and field.create:
                    result.append(name)
                if command_type == UPDATE and type_def.updatable and \
                        hasattr(field, "update") and field.update:
                    result.append(name)
        except:
            pass

        if command_type == DELETE and type_def.deletable:
            result.append("id")

        if command_type == UPDATE and type_def.updatable:
            result.append("id")

        return result

    def _is_list_type(self, type_name, field_name):
        try:
            return self.schema.types[type_name].resourceFields[field_name]\
                .type.startswith("array")
        except:
            return False

    def _run(self, cmd, args):
        if cmd not in self._all_commands():
            return

        command_type, type_name, action_name = self._decompose_command(cmd)
        possible_args_map = {}
        for i in self._possible_args(command_type, type_name, action_name):
            possible_args_map[i.lower()] = i
        new_args = {}

        for k, v in args.iteritems():
            k_l = k.lower()
            if k_l in possible_args_map:
                k = possible_args_map[k_l]

            if self._is_list_type(type_name, k):
                new_args[k] = v
            else:
                if len(v) and v[0] != "null":
                    new_args[k] = v[0]
                else:
                    new_args[k] = None

        if command_type == LIST:
            for i in self.list(type_name, **new_args):
                _print_cli(i)

        if command_type == CREATE:
            _print_cli(self.create(type_name, **new_args))

        if command_type == DELETE:
            obj = self.by_id(type_name, new_args["id"])
            if obj:
                self.delete(obj)
                _print_cli(obj)

        if command_type == UPDATE:
            _print_cli(self.update_by_id(type_name, new_args["id"], new_args))

        if command_type == ACTION:
            obj = self.by_id(type_name, new_args["id"])
            if obj:
                _print_cli(self.action(obj, action_name, new_args))


def _list(client, type, args):
    for i in client.list(type, **args):
        _print_cli(i)


def _print_cli(obj):
    if callable(getattr(obj, "_as_table")):
        print obj._as_table()
    else:
        print obj

## {{{ http://code.activestate.com/recipes/267662/ (r7)
import cStringIO
import operator


def indent(rows, hasHeader=False, headerChar='-', delim=' | ', justify='left',
           separateRows=False, prefix='', postfix='', wrapfunc=lambda x: x):
        """Indents a table by column.
             - rows: A sequence of sequences of items, one sequence per row.
             - hasHeader: True if the first row consists of the columns' names.
             - headerChar: Character to be used for the row separator line
                 (if hasHeader==True or separateRows==True).
             - delim: The column delimiter.
             - justify: Determines how are data justified in their column.
                 Valid values are 'left','right' and 'center'.
             - separateRows: True if rows are to be separated by a line
                 of 'headerChar's.
             - prefix: A string prepended to each printed row.
             - postfix: A string appended to each printed row.
             - wrapfunc: A function f(text) for wrapping text; each element in
                 the table is first wrapped by this function."""
        # closure for breaking logical rows to physical, using wrapfunc
        def rowWrapper(row):
                newRows = [wrapfunc(item).split('\n') for item in row]
                return [[substr or '' for substr in item] for item in map(None, *newRows)]
        # break each logical row into one or more physical ones
        logicalRows = [rowWrapper(row) for row in rows]
        # columns of physical rows
        columns = map(None, *reduce(operator.add, logicalRows))
        # get the maximum of each column by the string length of its items
        maxWidths = [max([len(str(item)) for item in column]) for column in columns]
        rowSeparator = headerChar * (len(prefix) + len(postfix) + sum(maxWidths) + len(delim)*(len(maxWidths)-1))
        # select the appropriate justify method
        justify = {'center': str.center, 'right': str.rjust, 'left': str.ljust}[justify.lower()]
        output = cStringIO.StringIO()
        if separateRows:
            print >> output, rowSeparator
        for physicalRows in logicalRows:
            for row in physicalRows:
                print >> output, prefix \
                    + delim.join([justify(str(item), width) for (item, width) in zip(row, maxWidths)]) + postfix
            if separateRows or hasHeader:
                print >> output, rowSeparator
                hasHeader = False
        return output.getvalue()
#End ## {{{ http://code.activestate.com/recipes/267662/ (r7)


def _env_prefix(cmd):
    return _prefix(cmd) + "_"


def from_env(prefix, **kw):
    args = dict((x, None) for x in ["access_key", "secret_key", "url", "cache", "cache_time", "strict"])
    args.update(kw)
    if not prefix.endswith("_"):
        prefix += "_"
    prefix = prefix.upper()
    return _from_env(prefix=prefix, **args)


def _from_env(prefix=PREFIX + "_", **kw):
    result = dict(kw)
    for k, v in kw.iteritems():
        if not v is None:
            result[k] = v
        else:
            result[k] = os.environ.get(prefix + k.upper())

        if result[k] is None:
            del result[k]

    return Client(**result)


def _general_args(help=True):
    import argparse

    parser = argparse.ArgumentParser(add_help=help)
    parser.add_argument("--access-key")
    parser.add_argument("--secret-key")
    parser.add_argument("--url")
    parser.add_argument("--cache", dest="cache", action="store_true", default=True)
    parser.add_argument("--no-cache", dest="cache", action="store_false")
    parser.add_argument("--cache-time", type=int)
    parser.add_argument("--strict", type=bool)

    return parser


def _list_args(subparsers, client, type, schema):
    subparser = subparsers.add_parser(LIST + type, help=LIST[0:len(LIST)-1].capitalize() + " " + type)
    for name, filter in schema.collectionFilters.iteritems():
        subparser.add_argument("--" + name)
        for m in filter.modifiers:
            if m != "eq":
                subparser.add_argument("--" + name + "_" + m)

    return subparser


def _generic_args(subparsers, operation, field_key, type, schema):
    prefix = operation + type
    subparser = subparsers.add_parser(prefix,
                                      help=operation[0:len(operation)-1].capitalize() + " " + type + " resource")

    for name, field in schema.resourceFields.iteritems():
        if field.get(field_key) is True:
            if field.get("type").startswith("array"):
                subparser.add_argument("--" + name, nargs="*")
            else:
                subparser.add_argument("--" + name)

    return subparser


def log(msg):
    print "HI", msg


def _full_args(client):
    parser = _general_args()
    subparsers = parser.add_subparsers(help="Sub-Command Help")
    for type, schema in client.schema.types.iteritems():
        if schema.listable:
            subparser = _list_args(subparsers, client, type, schema)
            subparser.set_defaults(_action=LIST, _type=type)
        if schema.creatable:
            subparser = _generic_args(subparsers, CREATE, "create", type, schema)
            subparser.set_defaults(_action=CREATE, _type=type)
        if schema.updatable:
            subparser = _generic_args(subparsers, UPDATE, "update", type, schema)
            subparser.add_argument("--id")
            subparser.set_defaults(_action=UPDATE, _type=type)
    argcomplete.autocomplete(parser)
    return parser


def _run_cli(client, namespace):
    args, command_type, type_name = _extract(namespace, "_action", "_type")

    try:
        if command_type == LIST:
            for i in client.list(type_name, **args):
                _print_cli(i)

        if command_type == CREATE:
            _print_cli(client.create(type_name, **args))

        if command_type == DELETE:
            obj = client.by_id(type_name, args["id"])
            if obj:
                client.delete(obj)
                _print_cli(obj)

        if command_type == UPDATE:
            _print_cli(client.update_by_id(type_name, args["id"], args))

        if command_type == ACTION:
            obj = client.by_id(type_name, args["id"])
            if obj:
                _print_cli(client.action(obj, action_name, args))
    except ApiError, e:
        import sys

        sys.stderr.write("Error : {}\n".format(e.error))
        status = int(e.error.status) - 400
        if status > 0 and status < 255:
            sys.exit(status)
        else:
            sys.exit(1)


def _extract(namespace, *args):
    values = vars(namespace)
    result = [values]
    for arg in args:
        value = values.get(arg)
        result.append(value)
        try:
            del values[arg]
        except KeyError:
            pass

    return tuple(result)


def _cli_client(argv):
    args, unknown = _general_args(help=False).parse_known_args()
    args = vars(args)
    prefix = _env_prefix(argv[0])
    return _from_env(prefix, **args)


if __name__ == '__main__':
    import sys
    client = _cli_client(sys.argv)
    if not client.valid():
        _general_args().print_help()
        sys.exit(2)

    args = _full_args(client).parse_args()
    _run_cli(client, args)
