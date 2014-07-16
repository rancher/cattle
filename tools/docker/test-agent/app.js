var os      = require("os"),
    request = require('request'),
    express = require('express');

var app = express();

app.get('/ping', function(req, res){
  res.send('pong');
});

app.get('/hostname', function(req, res){
  res.send(os.hostname());
});

app.get('/env', function(req, res){
  val = process.env[req.param('var')];
  res.send(val);
});

app.get('/get', function(req, res){
  url = req.param('url');
  link = req.param('link');
  port = req.param('port');
  path = req.param('path');

  if ( link != null && port != null && path != null ) {
    dest_port = process.env[link.toUpperCase() + "_PORT_" + port + "_TCP_PORT"]
    dest_host = process.env[link.toUpperCase() + "_PORT_" + port + "_TCP_ADDR"]
    url = 'http://' + dest_host + ':' + dest_port + '/' + path
  }

  if ( url == null ) {
    res.send(404);
  } else {
    request(url, function(err, response, body) {
      if ( err ) {
        res.send(500, err.message);
      } else if ( response.statusCode != 200 ) {
        res.send(response.statusCode, body);
      } else {
        res.send(body);
      }
    });
  }
});

var server = app.listen(3000, function() {
  console.log('Listening on port %d', server.address().port);
});
