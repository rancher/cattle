"use strict";

var path    = require('path'),
    fs      = require('fs'),
    child   = require('child_process'),
    express = require('express'),
    byline  = require('byline');

var router = express.Router();

var home = process.env.CATTLE_HOME || '/var/lib/cattle';
var scriptDir = path.join(home, 'events');
var token = null;

function run(script, event, cb) {
  var output = '';
  var resultData = null;
  var append = function(data) {
    output += data;
  }

  var p = child.spawn(script, { 'cwd' : path.dirname(script) });

  byline(p.stdout).on('data', function(data) {
    data = data.toString();
    if ( data.indexOf("{") == 0 ) {
      resultData = data;
    } else {
      output += data;
    }
  });

  p.stderr.on('data', function(data) {
    output += data;
  });

  p.stdin.end(JSON.stringify(event));

  p.on('close', function(code) {
    return cb(null, {
      'data' : resultData,
      'output' : output,
      'exitCode' : code
    });
  });
}

function exec(name, event, cb) {
  var script = path.join(scriptDir, name.replace(/;.*/, ''));

  fs.exists(script, function(exist) {
    if ( ! exist ) {
      return cb(null);
    }

    return run(script, event, cb);
  });
}

router.post('/', function(req, res, next) {
  if ( ! req.is('json') || ! req.body.name || token != req.query.token ) {
    return next();
  }

  exec(req.body.name, req.body, function(err, val) {
    if ( err ) {
      return next(err);
    }

    if ( val ) {
      res.type('json');
      res.send(val);
    } else {
      next();
    }
  })
});

module.exports = function(config) {
  token = config.token;

  return router;
}
