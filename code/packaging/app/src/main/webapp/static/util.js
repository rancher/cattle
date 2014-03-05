function kvSplit(str, itemSep, pairSep)
{
  var out = {};
  var pairs = (str||'').split(itemSep);
  pairs.forEach(function(str) {
    var kv = str.split(pairSep);
    out[kv[0]] = kv[1];
  });
  return out;
}

function queryParams()
{
  return kvSplit((window.location.search||'').substr(1), /&/, /=/);
}

function apiHost() {
  var q = queryParams();
  if ( q.host )
  {
    return q.host + ":" + (q.port ? q.port : 8080);
  }
  else
  {
    return window.location.host;
  }
}

function ajax(url) {
  return $.ajax({
    url: url,
    dataType: 'json',
    headers: {
      'Accept': 'application/json',
      'X-API-Host': apiHost()
    },
  });
}

var MAX_NOTES = 20;
function note(str) {
  if ( ! $('#show-messages')[0].checked )
    return;

  var neu = $('<div/>', {'class': 'note'}).text(str);
  $('#note').prepend(neu);

  function limit() {
    var elems = $('#note .note').slice(Math.max(MAX_NOTES-1,0));
    elems.remove();
  }
}

function cookies() {
  return kvSplit(document.cookie, /\s*;\s*/, /=/);
}

function setCookie(name, val) {
  document.cookie = name + '=' + val;
}
