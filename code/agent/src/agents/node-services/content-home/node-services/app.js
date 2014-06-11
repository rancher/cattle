var config = require("./config");
var express = require('express');
var path = require('path');
var logger = require('morgan');
var bodyParser = require('body-parser');

var events = require('./routes/events');
var metadata = require('./routes/metadata');

var app = express();
app.disable('x-powered-by');

app.use(bodyParser.json());
app.use(logger('dev'));

app.use('/', metadata(config));
app.use('/events', events(config));

/// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  err.message = '<?xml version="1.0" encoding="iso-8859-1"?>\n <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"\n "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n <head>\n <title>404 - Not Found</title>\n </head>\n <body>\n <h1>404 - Not Found</h1>\n </body>\n </html>';

  next(err);
});

app.use(function(err, req, res, next) {
  if ( err.status == 404 ) {
    res.send(404, err.message);
  } else {
    res.send(err.status || 500);
  }
});

module.exports = app;
