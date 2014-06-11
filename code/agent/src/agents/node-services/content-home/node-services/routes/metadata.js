"use strict"; 

var fs      = require('fs'),
    express = require('express');

var VERSIONS = [
  "1.0",
  "2007-01-19",
  "2007-03-01",
  "2007-08-29",
  "2007-10-10",
  "2007-12-15",
  "2008-02-01",
  "2008-09-01",
  "2009-04-04",
  "2011-01-01",
  "2011-05-01",
  "2012-01-12",
  "2014-02-25",
  "latest"
];

var CLIENT_IP = process.env.CLIENT_IP;
var META_DATA_FILE = 'metadata.json';
var started = false;
var data = {};

function load(cb) {
  fs.exists(META_DATA_FILE, function(exists) {
    if ( exists ) {
      fs.readFile(META_DATA_FILE, "utf-8", function(err, text) {
        if ( err ) {
          console.log("Error reading", META_DATA_FILE, err);
          return cb(err);
        }

        try {
          console.log("Loading meta-data from", META_DATA_FILE);
          data = JSON.parse(text);
          return cb(null, data);
        } catch (e) {
          console.log("Failed to load metadata from", META_DATA_FILE, e);
          return cb(e, null);
        }
      });
    } else {
      data = {}
      return cb(null, data);
    }
  });
}

function getData(cb) {
  start();

  if ( Object.keys(data).length == 0 ) {
    load(cb);
  } else {
    process.nextTick(function() {
      return cb(null, data);
    });
  }
}

function start() {
  if ( ! started ) {
    fs.watchFile(META_DATA_FILE, { interval : 1000 }, function() {
      load();
    });

    started = true;
  }
}

function lookup(ip, path, data) {
  data = data[ip]

  if ( data == null ) {
    return null;
  }

  path.split('/').forEach(function(val) {
    if ( val.length > 0 ) {
      if ( data != null ) {
        data = data[val];
      }
    }
  })

  return data;
}

function getResult(ip, path, data, cb) {
  if ( path == null ) {
    path = '';
  }

  var node = lookup(ip, path, data);
  var directoryRequest = (path.lastIndexOf('/') == (path.length-1))
  var redirect = false;
  var content = null;

  if ( node == null || (directoryRequest && typeof(node) == 'string') ) {
    return cb();
  } else if ( directoryRequest ) {
    var entries = [];
    for ( var key in node ) {
      if ( path == '' || typeof(node[key]) == 'string' ) {
        entries.push(key)
      } else {
        entries.push(key + '/')
      }
    }
    content = entries.join('\n');
  } else if ( typeof(node) == 'string' ) {
    content = node;
  } else {
    redirect = true;
  }

  return cb(null, {
    'content' : content,
    'redirect' : redirect
  });
}

function handle(req, res, next) {
  var path = req.params[0];
  var ip = CLIENT_IP || req.ip;

  getData(function(err, data) {
    if ( err ) {
      return cb(err);
    }

    getResult(ip, path, data, function(err, result) {
      if ( err ) {
        return next(err);
      }

      if ( result == null ) {
        return next();
      }

      res.set('Server', 'EC2ws');
      res.type('text/plain');

      if ( result.redirect ) {
        res.redirect(301, req.path + '/')
      } else {
        res.send(result.content);
      }
    });
  });
}

var router = express.Router({ 'strict' : true });

router.get('/', function(req, res) {
  res.set('Content-Type', 'text/plain');
  res.send(VERSIONS.join('\n'));
})

VERSIONS.forEach(function(version) {
  var path = '/' + version;
  router.get(path, function(req, res) {
    // Technically EC2 sends the full http://169.254.169.254 for the Location header
    res.redirect(301, path + '/');
  })

  router.get(path + '/*', handle);
})

module.exports = function(config) {
  start();
  return router;
}
