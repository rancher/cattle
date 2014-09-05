var USE_HOSTMAP = true;
var MAX_RETRIES = 5;

initCheckboxes();
updateShowNames();
loadHosts(connect);

$('#show-names').on('click', updateShowNames);
$('#show-messages').on('click', updateShowMessages);

// ------------------------------------

function gotoApi()
{
  window.open('http://'+apiHost()+'/v1','_blank');
}

function initCheckboxes() {
  var c = cookies();
  $('#show-names').attr('checked', c.names == '1');
  $('#show-messages').attr('checked', c.messages == '1');
}

function updateShowNames() {
  var show = $('#show-names')[0].checked;
  $('#hosts').toggleClass('hide-names', !show);
  setCookie('names', (show ? 1 : 0));
};

function updateShowMessages() {
  var show = $('#show-messages')[0].checked;
  setCookie('messages', (show ? 1 : 0));
};

function loadHosts(cb, url) {
  note('Loading hosts');
  ajax('/v1/hosts?limit=1000&removed_null').then(done).fail(fail);
  
  function done(res) {
    var html = [];

    res.data.forEach(function(host) {
      var url = host.links.self;
      url += (url.indexOf('?') >= 0 ? '&' : '?') + 'include=agent';

      html.push(
        '<div id="host-' + host.id + '" class="host-wrapper">' +
          '<div class="host-name"><a href="'+url+'" target="_blank">'+(host.name || host.id)+'</a></div>' +
          '<div class="host-contents clearfix"></div>' +
        '</div>'
      );
    });

    var $hosts = $('#hosts');
    $hosts.empty();
    $hosts.append.call($hosts,html);

    note('Loaded hosts');
    loadInstances(cb);
  }

  function fail(xhr) {
    alert('Error (' + xhr.status + '): ' + xhr.responseText);
  }
}

function loadInstances(cb, url) {
  //note('Loading instances');
  ajax(url||'/v1/instances?limit=1000' + (USE_HOSTMAP ? '&removed_null&include=instanceHostMaps' : '')).then(done).fail(fail);

  function done(res) {
    renderInstances(res.data);

    //note('Rendered instances');
    if ( res.pagination && res.pagination.next )
      loadInstances(cb,res.pagination.next);
    else if ( cb )
      cb();
  }

  function fail(xhr) {
    alert('Error (' + xhr.status + '): ' + xhr.responseText);
  }
}

function renderInstances(data) {
  //note('Render Instances');
  var hosts = {};

  data.forEach(function(inst) {
    var $elem = instanceElem(inst.id);
    if ( $elem )
    {
      if ( $elem.data('transitioning') != inst.transitioning )
        $elem.css({backgroundColor: instanceColor(inst)});

      return;
    }

    var hostId = 'none';
    if ( USE_HOSTMAP && inst.instanceHostMaps && inst.instanceHostMaps.length )
      hostId = inst.instanceHostMaps[0].hostId;
    else if ( !USE_HOSTMAP )
      hostId = inst.requestedHostId;

    if ( !hosts[hostId] )
      hosts[hostId] = [];

    hosts[hostId].push( instanceTpl(inst) );
  });

  var keys = Object.keys(hosts);
  keys.forEach(function(hostId) {
    var $par = $('#host-'+hostId+' .host-contents');
    $par.append.call($par,hosts[hostId]);
  });
}

function instanceElem(id) {
  var $elem = $('#inst-'+id);
  if ( $elem.length > 0 )
    return $elem;
  else
    return null;
}

function instanceColor(inst) {
  if ( inst.transitioning == 'yes' )
    return "#6666ff";
  else if ( inst.transitioning == 'error' )
    return "#ff3333";
  else
    return "#dddddd";
}

function instanceTpl(inst) {
  return '<a href="'+inst.links.self+'" target="_blank" data-transitioning="'+inst.transitioning+'" id="inst-' + inst.id + '" class="inst" style="background-color: '+ instanceColor(inst) + ';">' +
    '<span class="inst-name">'+(inst.name || inst.id)+'</span>' +
  '</a>';
}

// ------------------------------------

function connect() {
  var url = 'ws://'+ apiHost() +'/v1/subscribe?include=instanceHostMaps&eventNames=resource.change';
  note('Connecting to: '+url);
  var sock = new WebSocket(url);

  sock.onmessage = function(event) {
    var d = JSON.parse(event.data);
    var str = d.name;
    if ( d.resourceType )
    {
      str += ' ' + d.resourceType;

      if ( d.resourceId )
        str += ' ' + d.resourceId;
    }

    note(str);
    //console.log('Message:',d);

    if ( d.resourceType == 'host' )
      hostChanged(d);
    else if ( d.resourceType == 'instance' )
      instanceChanged(d);
  };

  sock.onopen = function() {
    note('WebSocket opened');
  };

  sock.onclose = function() {
    note('WebSocket closed');
    sock.close();
    setTimeout(connect, 1000);
  };
}

function hostChanged(change) {
  window.location.href = window.location.href;
  /*
  var $elem = $('#host-'+change.resourceId);
  if ( ! $elem.length )
  {
    note("Heard about host "+ id + " but I don't have a box for that, reloading");
    loadHosts();
  }
  */
}

function instanceChanged(change) {
  renderInstances([change.data.resource]);
}
