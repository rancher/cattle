// Application
window.App = Em.Application.create({
  rootElement: '#outlet',
  LOG_TRANSITIONS: true,
  LOG_VIEW_LOOKUPS: true,
  LOG_ACTIVE_GENERATION: true
});

// Em.ENV.RAISE_ON_DEPRECATION = true;
// Em.LOG_STACKTRACE_ON_DEPRECATION = true;

/*
Em.onerror = function(error) {
  Em.$.ajax('/error-notification', {
    type: 'POST',
    data: {
      stack: error.stack,
      otherInformation: 'exception message'
    }
  });
}
*/

Em.RSVP.configure('onerror', function(error) {
  Em.Logger.assert(false, error);
});

App.ApplicationRoute = Em.Route.extend({
  actions: {
    error: function(err) {
      Growl.error('Error',err);
      console.log(arguments);
    }
  }
});

App.ApplicationController = Em.Controller.extend({
});

App.ApplicationView = Em.View.extend({
  templateName: 'application'
});

App.IndexView = Em.View.extend({
  templateName: 'index'
});

function l(name) {
  return App.__container__.lookup(name);
}

function lc(name) {
  return App.__container__.lookup('controller:'+name);
}

if ("ontouchstart" in document.documentElement)
{
  $('BODY').addClass('touch');
}
else
{
  $('BODY').addClass('no-touch');
}

;

// HandlebarsHelpers
Em.Handlebars.helper('uppercase', function(value, options) {
  return (value||'').toUpperCase();
});

Em.Handlebars.helper('lowercase', function(value, options) {
  return (value||'').toLowerCase();
});

Em.Handlebars.helper('ucfirst', function(value, options) {
  var val = value||'';
  return val.substr(0,1).toUpperCase() + val.substr(1);
});

Em.Handlebars.helper('nl2br', function(value, options) {
  var val = Em.Handlebars.Utils.escapeExpression(value||'');
  return new Em.Handlebars.SafeString(val.replace(/\n/g,'<br/>\n'));
});

Em.Handlebars.helper('moment', function(date, options) {
  var f = options.hash.format || "MMM DD, YYYY hh:mm:ss A";
  return moment(date).format(f);
});

Em.Handlebars.helper('momentFromNow', function(date, options) {
  return moment(date).fromNow();
});

Em.Handlebars.helper('momentCalendar', function(date, options) {
  return moment(date).calendar();
});

App.DisplayNameSelectOption = Em.SelectOption.extend({
  labelPathDidChange: Ember.observer('parentView.optionLabelPath', function() {
    var labelPath = Em.get(this, 'parentView.optionLabelPath');

    if (!labelPath) { return; }

    Ember.defineProperty(this, 'label', Ember.computed(function() {
      return Em.get(this, labelPath) || Em.get(this,'content.id');
    }).property(labelPath,'content.id'));
  }),
});

App.DisplayNameSelect = Em.Select.extend({
  optionView: App.DisplayNameSelectOption
});
;

// Config
Em.Application.initializer({
  name: "config",
 
  initialize: function(container, application) {
    container.register('config:server', _serverState, {instantiate: false, singleton: true});
  }
});

Em.Application.initializer({
  name: "injectConfig",
 
  initialize: function(container) {
    container.typeInjection('view', 'config', 'config:server');
  }
});
;

// Util
function Util() {
}

Util.arrayDiff = function(a, b) {
  return a.filter(function(i) {return b.indexOf(i) < 0;});
};

Util.arrayIntersect = function(a, b) {
  return a.filter(function(i) {return b.indexOf(i) >= 0;});
};

Util.download = function(url) {
  var id = '__downloadIframe';
  var iframe = document.getElementById(id);
  if ( !iframe )
  {
    iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.id = id;
    document.body.appendChild(iframe);
  }

  iframe.src = url;
}

;

// Growl
function Growl()
{
}

Growl.success = function(title,body)
{
  Growl._jGrowl(title, body, {
    theme: 'success'
  });
}

Growl.message = function(title,body)
{
  Growl._jGrowl(title, body, {
    theme: 'message'
  });
}

Growl.error = function(title,body)
{
  Growl._jGrowl(title, body, {
    sticky: true,
    theme: 'error'
  });
}

Growl._jGrowl = function(title, body, opt)
{
  opt = opt || {};

  if ( title )
    opt.header = title;

  return $.jGrowl(body, opt);
}

$.jGrowl.defaults.pool = 6;
$.jGrowl.defaults.closeTemplate = '<i class="fa fa-times"></i>';
$.jGrowl.defaults.closerTemplate = '<div><button type="button" class="btn btn-info btn-xs btn-block">Hide All Notifications</button></div>';
//$.jGrowl.defaults.glue = 'before';
;

// Error
App.ErrorRoute = Em.Route.extend({
  renderTemplate: function() {
    this.render('error', {into: 'application', outlet: 'error'});
  }
});

App.ErrorView = Em.View.extend({
  classNames: ['error'],
  templateName: 'error',
});
;

// Loading
App.LoadingRoute = Em.Route.extend({
  renderTemplate: function() {
    this.render('loading', {into: 'application', outlet: 'loading'});
  }
});

App.LoadingView = Em.View.extend({
  classNames: ['loading'],
  templateName: 'loading',

  animateIn: function(done) {
    $('#loading-underlay').show();
    this.$().fadeIn({duration: 200, queue: false, easing: 'linear', complete: done});
  },

  animateOut: function(done) {
    $('#loading-underlay').hide();
    this.$().fadeOut({duration: 100, queue: false, easing: 'linear', complete: done});
  },
});
;

// Gdapi
// --------
// Request adapter
// --------
App.GdapiAdapter = Em.Object.extend({
  baseUrl: '/v1/',
  typeifier: App.GdapiTypeifier,

  request: function(opt, cb) {
    var self = this;

    var url = opt.url;
    if ( url.indexOf('http') !== 0 && url.indexOf('/') !== 0 )
      url = this.get('baseUrl')+url;

    opt.url = url;
    opt.headers = opt.headers || {};
    opt.processData = false;

    opt.dataType = opt.dataType || 'json';
    if ( opt.dataType == 'json' )
    {
      opt.headers['Accept'] = 'application/json';
    }

    var csrf = $.cookie('CSRF');
    if ( csrf )
    {
      opt.headers['X-API-CSRF'] = csrf;
    }

    if ( opt.data && typeof opt.data == 'object' )
    {
      opt.data = JSON.stringify(opt.data);
      opt.contentType = 'application/json';
    }

    var promise = new Em.RSVP.Promise(function(resolve,reject) {
      $.ajax(opt).then(success,fail);

      function success(body, textStatus, xhr) {
        resolve(self.typeifier.typeify(body));
      }

      function fail(xhr, textStatus, err) {
        var body;
        if ( xhr.responseJSON )
        {
          body = self.typeifier.typeify(xhr.responseJSON);
        }
        else
        {
          body = xhr.responseText;
        }
        
        reject(body);
      }
    });

    return promise;
  },
});

// --------
// Typeifier
// --------
App.GdapiTypeifier = Em.Object.extend({});
App.GdapiTypeifier.reopenClass({
  metaKeys: ['actions','createDefaults','createTypes','filters','links','pagination','sort','sortLinks'],

  keyName: function(type, key) {
    return key.replace(/\./g,'_');
  },

  typeify: function(input) {
    var self = this;

    if ( !input || typeof input != 'object' )
      return input;

    var type = input.type||'';
    var properType = type.substr(0,1).toUpperCase() + type.substr(1);

    var output;
    if ( type == 'collection' )
    {
      output = App.GdapiCollection.create({content: []});
      input.data.forEach(function(item) {
        output.pushObject(self.typeify(item));
      });

      self.metaKeys.forEach(function(key) {
        if ( input.hasOwnProperty(key) )
        {
          output.set(self.keyName(type, key), input[key]);
        }
      });

      return output;
    }

    if ( type )
    {
      if ( App[properType] && App.GdapiResource.detect(App[properType]) )
      {
        output = App[properType].create();
      }
      else
      {
        output = App.GdapiResource.create();
      }
    }
    else
    {
      output = Em.Object.create();
    }

    self.metaKeys.forEach(function(key) {
      if ( input.hasOwnProperty(key) )
      {
        output.set(self.keyName(type, key),input[key]);
        delete input[key];
      }
    });

    Object.keys(input).forEach(function(key) {
      var value = input[key];
      if ( Em.isArray(value) )
      {
        output.set(self.keyName(type, key), value.map(function(item) { 
          var neu = self.typeify(item);
          return neu;
        }));
      }
      else if ( typeof value === 'object' )
      {
        output.set(self.keyName(type, key), self.typeify(value));
      }
      else
      {
        output.set(self.keyName(type, key),value);
      }
    });

    return output;
  }
});

// --------
// Type mixin
// --------
App.GdapiTypeMixin = Em.Mixin.create({
  id: null,
  type: null,
  links: null,
  actions: null,

  merge: function(data) {
    var self = this;
    var keys = Object.keys(data);
    keys.forEach(function(key) {
      if ( data.hasOwnProperty(key) )
      {
        self.set(key,data.get(key));
      }
    });
  },

  toJSON: function() {
    return this.getProperties(Object.keys(this));
  },

  clone: function() {
    return this.constructor.create(this.toJSON());
  },

  followLink: function(name) {
    var self = this;
    var url = this.get('links.'+name);
    if (!url)
      throw new Error('Unknown link');

    return App.adapter.request({
      method: 'GET',
      url: url
    });
  },

  importLink: function(name) {
    var self = this;
    return new Em.RSVP.Promise(function(resolve,reject) {
      self.followLink(name).then(function(data) {
        self.set(name,data);
        resolve(self);
      }).catch(function(err) {
        reject(err);
      });
    });
  },

  hasAction: function(name) {
    var self = this;
    var url = this.get('actions.'+name);
    return !!url;
  },

  doAction: function(name, data) {
    var self = this;
    var url = this.get('actions.'+name);
    if (!url)
      throw new Error('Unknown action');

    return App.adapter.request({
      method: 'POST',
      url: url,
      json: data
    }).then(function(newData) {
      // An action might return the same resource, or an unrelated one.
      if ( !self.get('id') ||  self.get('id') == newData.get('id') )
      {
        self.merge(newData);
        return self;
      }
      else
      {
        return newData;
      }
    });
  },

  save: function() {
    var self = this;
    var baseUrl = this.constructor.baseUrl.replace(/\/+$/,'');
    var url = this.get('links.self') || baseUrl;
    var json = this.toJSON();

    return App.adapter.request({
      method: (this.get('id') ? 'PUT' : 'POST'),
      url: url,
      data: json,
    }).then(function(newData) {
      self.merge(newData);
      return self;
    });
  },

  delete: function() {
    var self = this;
    return App.adapter.request({
      method: 'DELETE',
      url: this.get('links.self')
    }).then(function(newData) {
      if ( newData )
      {
        self.merge(newData);
      }
    });
  }
});

// --------
// Resource
// --------
App.GdapiResource = Em.Object.extend(App.GdapiTypeMixin, {
});

App.GdapiResource.reopenClass({
  baseUrl: null,

  find: function(id, opt) {
    opt = opt || {};
    var baseUrl = this.baseUrl.replace(/\/+$/,'');
    var url = baseUrl + (id ? '/'+encodeURIComponent(id) : '');

    // @TODO direct support for modifiers
    if ( opt.filter )
    {
      var keys = Object.keys(opt.filter);
      keys.forEach(function(key) {
        var vals = opt.filter[key];
        if ( !Em.isArray(vals) )
        {
          vals = [vals];
        }

        vals.forEach(function(val) {
          url += (url.indexOf('?') >= 0 ? '&' : '?') + encodeURIComponent(key) + '=' + encodeURIComponent(val);
        });
      });
    }

    if ( opt.include )
    {
      if ( !Em.isArray(opt.include) )
        opt.include = [opt.include];

      opt.include.forEach(function(key) {
        url += (url.indexOf('?') >= 0 ? '&' : '?') + 'include=' + encodeURIComponent(key);
      });
    }

    return App.adapter.request({
      url: url
    });
  },
});

// --------
// Error
// --------
App.GdapiError = App.GdapiResource.extend({

})

// --------
// Collection
// --------
App.GdapiCollection = Em.ArrayProxy.extend(Em.SortableMixin,App.GdapiTypeMixin,{
  createDefaults: null,
  createTypes: null,
  filters: null,
  pagination: null,
  sort: null,
  sortLinks: null
});
;

// Cattle
// Every cattle resource has these
App.CattleResource = App.GdapiResource.extend({
  name: null,
  description: null,
  created: null,
});

App.CattleResourceController = Em.ObjectController.extend({
  displayName: function() {
    return this.get('name') || this.get('id')
  }.property('name','id'),

  delete: function() {
    return this.get('model').delete();
  },

  isDeleted: Em.computed.equal('state','removed'),
  isPurged: Em.computed.equal('state','purged')
});

App.CattleCollectionController = Em.ArrayController.extend({
  sortProperties: ['name','id'],
  dataSource: Em.computed.alias('notPurged'),

  notPurged: function() {
    return this.get('arrangedContent').filter(function(item) {
      var state = item.get('state').toLowerCase();
      return state != 'purged'
    });
  }.property('arrangedContent.@each.state')
})


// Cattle resources that transition have these
App.TransitioningResource = App.CattleResource.extend({
  state: null,
  transitioning: null,
  transitioningMessage: null,
  transitioningProgress: null,
})

App.TransitioningResourceController = App.CattleResourceController.extend({
  displayState: function() {
    var state = this.get('state');
    return state.substr(0,1).toUpperCase() + state.substr(1);
  }.property('state'),

  isError: Em.computed.alias('transitioning','error'),

  showTransitioningMessage: function() {
    var trans = this.get('transitioning');
    return (trans == 'yes' || trans == 'error');
  }.property('transitioning'),

  stateIcon: function() {
    var trans = this.get('transitioning');
    if ( trans == 'yes' )
    {
      return 'fa-cog fa-spin';
    }
    else if ( trans == 'error' )
    {
      return 'fa-exclamation-circle text-danger';
    }
    else
    {
      var map = this.constructor.stateMap;
      var key = this.get('state').toLowerCase();
      if ( map && map[key] && map[key].icon !== undefined)
      {
        if ( typeof map[key].icon == 'function' )
          return map[key].icon(this);
        else
          return map[key].icon;
      }

      return this.constructor.defaultStateIcon;
    }
  }.property('state','transitioning'),

  stateColor: function() {
      var map = this.constructor.stateMap;
      var key = this.get('state').toLowerCase();
      if ( map && map[key] && map[key].color !== undefined )
      {
        if ( typeof map[key].color == 'function' )
          return map[key].color(this);
        else
          return map[key].color;
      }

    return this.constructor.defaultStateColor;
  }.property('state','transitioning'),

  isTransitioning: Em.computed.equal('transitioning','yes'),

  doAction: function(/*arguments*/) {
    var model = this.get('model');
    return model.doAction.apply(model,arguments);
  },

  displayProgress: function() {
    var progress = this.get('transitioningProgress');
    if ( isNaN(progress) || !progress )
      progress = 100;

    return Math.max(2,Math.min(progress, 100));
  }.property('transitioningProgress'),

  progressStyle: function() {
    return 'width: '+ this.get('displayProgress') +'%';
  }.property('displayProgress'), 
});

// Override stateMap with a map of state -> icon classes
App.TransitioningResourceController.reopenClass({
  stateMap: null,
  defaultStateIcon: 'fa-question-circle',
  defaultStateColor: ''
})

App.CattleTypeifier = App.GdapiTypeifier.extend();
App.CattleTypeifier.reopenClass({
  keyName: function(type, key) {
    var out = key.replace(/\./g,'_');
    if ( type == 'host' )
    {
      if ( ['state','transitioning','transitioningMessage','transitioningProgress'].indexOf(key) >= 0 )
      {
        out = 'host' + out.substr(0,1).toUpperCase() + out.substr(1);
      }
    }

    return out;
  },
});

App.adapter = App.GdapiAdapter.create({
  typeifier: App.CattleTypeifier
});

;

// Router
App.Router.reopen({
  location: 'auto'
});

App.Router.map(function()
{
  this.resource('hosts', { path: '/hosts'}, function() {
    this.route('new');
    this.resource('host', { path: '/:host_id' }, function() {
      this.route('edit');
      this.route('delete');
    });

    this.resource('containerNew', {path: '/new-container/:host_id'});
    this.resource('containers', { path: '/containers'}, function() {
      this.resource('container', { path: '/:container_id' }, function() {
        this.route('edit');
        this.route('delete');
      });
    });

    this.resource('virtualMachineNew', {path: '/new-virtualmachine/:host_id'});
    this.resource('virtualMachines', { path: '/virtualmachines'}, function() {
      this.resource('virtualMachine', { path: '/:virtualMachine_id' }, function() {
        this.route('console');
        this.route('edit');
        this.route('delete');
      });
    });
  });

  this.resource('apikeys', {path: '/api'}, function() {
    this.route('new');
    this.resource('apikey', {path: '/:apikey_id'}, function() {
      this.route('edit');
      this.route('delete');
    });
  });

  this.resource('sshkeys', {path: '/sshkey'}, function() {
    this.route('new');
    this.route('import');
    this.resource('sshkey', {path: '/:sshkey_id'}, function() {
      this.route('edit');
      this.route('delete');
    });
  });
});

App.IndexRoute = Em.Route.extend({
  beforeModel: function() {
    this.transitionTo('hosts');
  }
});
;

// User
App.User = Em.Object.extend({
  displayName: function() {
    var email = (this.get('data.email')||'').replace(/@gmail.com$/,'');
    if ( email )
      return email;
    else
      return this.get('id')
  }.property('data.email','id')
});

Em.Application.initializer({
  name: "currentUser",
 
  initialize: function(container, application) {
    var user = App.User.create(_serverState.user);
    container.register('user:current', user, {instantiate: false, singleton: true});
  }
});
 
Em.Application.initializer({
  name: "injectCurrentUser",
  after: 'currentUser',
 
  initialize: function(container) {
    container.typeInjection('controller', 'currentUser', 'user:current');
    container.typeInjection('route', 'currentUser', 'user:current');
  }
});
;

// Overlay
App.OverlayView = Em.View.extend({
  classNames: ['overlay'],

  didInsertElement: function() {
    this.$('BUTTON, SPAN, I').tooltip();

    var input = this.$('INPUT')[0];
    if ( input )
    {
      input.focus();
    }
    else
    {
      this.$().attr('tabindex',0);
      this.$().focus();
    }
  },

  willAnimateIn: function() {
    this.$().hide();
  },

  animateIn: function(done) {
    $('#underlay').fadeIn({duration: 200, queue: false});
    this.$().slideDown({duration: 200, queue: false, easing: 'linear', complete: done});
  },

  animateOut: function(done) {
    $('#underlay').fadeOut({duration: 200, queue: false});
    this.$().slideUp({duration: 200, queue: false, easing: 'linear', complete: done});
  },

  keyDown: function(event) {
    if ( event.keyCode == 27 ) // Escape
      this.send('overlayClose');
    else if ( event.keyCode == 13 || event.keyCode == 10 ) // Enter
    {
      if ( event.target.tagName == 'A' || event.target.tagName == 'TEXTAREA' )
        return true;
      else
        this.send('overlayEnter'); 
    }
    else
      return true;
  },

  actions: {
    overlayClose: function() {
      // Override me
    },

    overlayEnter: function() {
      // Override me
    }
  }
});

App.ConfirmDeleteView = App.OverlayView.extend({
  templateName: 'confirm-delete',
  classNames: ['overlay-small'],

  actions: {
    overlayClose: function() {
      this.get('controller').send('cancel');
    },

    overlayEnter: function() {
      this.get('controller').send('confirm');
    }
  }
});

App.OverlayEditView = App.OverlayView.extend({
  actions: {
    overlayClose: function() {
      this.get('controller').send('cancel');
    },

    overlayEnter: function() {
      this.get('controller').send('save');
    },

    showAdvanced: function() {
      this.$('.advanced').slideDown();
      this.$('.advanced-toggle').hide();
    }
  },
});
;

// PageHeader
App.PageHeaderComponent = Em.Component.extend({
  tagName: 'header',
  classNames: ['navbar','navbar-default'],
  layoutName: 'page-header',
  homeUrl: _serverState.homeUrl
});
;

// Socket
App.ApplicationRoute.reopen({
  socket: null,
  connected: false,
  tries: 0,
  disconnectedAt: null,

  connect: function() {
    var self = this;
    var socket = self.get('socket');
    if ( socket )
    {
      try {
        socket.close();
        self.set('socket',null);
      }
      catch (e)
      {
        // Meh..
      }
    }

    var url = "ws://"+window.location.host+_serverState.wsEndpoint;
    socket = new WebSocket(url);
    self.set('socket', socket);

    socket.onmessage = function(event) {
      var d = App.GdapiTypeifier.typeify(JSON.parse(event.data));
      trySend('wsMessage',d);

      var str = d.name;
      if ( d.resourceType )
      {
        str += ' ' + d.resourceType;

        if ( d.resourceId )
          str += ' ' + d.resourceId;
      }

      var action;
      if ( d.name == 'resource.change' )
      {
        action = d.resourceType+'Changed';
      }
      else if ( d.name == 'ping' )
      {
        action = 'wsPing'; 
      }

      if ( action )
      {
        trySend(action,d);
      }
    }

    socket.onopen = function() {
      var now = (new Date()).getTime();
      self.set('connected',true);

      var at = self.get('disconnectedAt');
      var after = null;
      if ( at )
        after = now - at;
      trySend('wsConnected', self.get('tries'), after);
      self.set('tries',0);
      self.set('disconnectedAt', null);
    }

    socket.onclose = function() {
      self.set('connected',false);
      self.incrementProperty('tries');

      if ( self.get('disconnectedAt') == null )
        self.set('disconnectedAt', (new Date()).getTime());

      socket.close();
      var delay = Math.max(1000, Math.min(1000 * self.get('tries'), 30000));
      setTimeout(self.connect.bind(self), delay);

      trySend('wsDisconnected',self.get('tries'));
    }

    function trySend(/* arguments */)
    {
      try 
      {
        self.send.apply(self,arguments);
      }
      catch (err)
      {
        if ( err instanceof Em.Error && err.message.indexOf('Nothing handled the action') === 0 )
        {
          // Don't care
        }
        else
        {
          throw err;
        }
      }
    }
  },

  enter: function() {
    this.connect();
  },
});
;

// SocketActions
App.ApplicationRoute.reopen({

  findInCollection: function(collectionName,id) {
    var collection = this.controllerFor(collectionName);
    var existing = collection.filterProperty('id',id).get('firstObject.content');
    return existing;
  },

  mergeChangeInto: function(collectionName,change) {
    var item = change.data.resource;
    var collection = this.controllerFor(collectionName);
    var existing = collection.filterProperty('id',item.get('id')).get('firstObject.content');

    if ( existing )
    {
      existing.setProperties(item);
      return existing;
    }
    else
    {
      collection.pushObject(item);
      return item;
    }
  },
  
  instanceChanged: function(change) {
    console.log('Instance Changed:',change);
    var self = this;
    var instance = change.data.resource;
    var id = instance.get('id');

    // All the hosts
    var allHosts = self.controllerFor('hosts');

    // Host IDs the instance should be on
    var expectedHostIds = [];
    if ( instance.get('state') != 'purged' )
    {
      expectedHostIds = (instance.get('hosts')||[]).map(function(host) {
        return host.get('id');
      });
    }

    // Host IDs it is currently on
    var curHostIds = [];
    allHosts.forEach(function(host) {
      var existing = (host.get('instances')||[]).filterProperty('id', id);
      if ( existing.length )
        curHostIds.push(host.get('id'));
    });

    // Remove from hosts the instance shouldn't be on
    var remove = Util.arrayDiff(curHostIds, expectedHostIds);
    remove.forEach(function(hostId) {
      var host = self.findInCollection('hosts',hostId);
      if ( host )
      {
        var instances = host.get('instances');
        if ( !instances )
          return;

        instances.removeObjects(instances.filterProperty('id', id));
      }
    });

    // Add or update hosts the instance should be on
    expectedHostIds.forEach(function(hostId) {
      var host = self.findInCollection('hosts',hostId);
      if ( host )
      {
        var instances = host.get('instances');
        if ( !instances )
        {
          instances = [];
          host.set('instances',instances);
        }

        var existing = instances.filterProperty('id', id);
        if ( existing.length )
        {
          existing.forEach(function(item) {
            item.setProperties(instance);
          });
        }
        else
        {
          instances.pushObject(instance);
        }
      }
    });
  },

  actions: {
    wsMessage: function(data) {
      //console.log('wsMessage',data);
    },

    wsConnected: function(tries,sec) { 
      console.log(
        'WebSocket connected ' +
        '(after '+tries+' '+(tries==1?'try':'tries') +
        (sec ? ', '+sec/1000+' sec' : '') +
        ')'
      ); 
    },

    wsDisconnected: function() {
      console.log('WebSocket disconnected');
    },

    wsPing: function() {
    },

    credentialChanged: function(change) {
      var type = change.data.resource.type.toLowerCase();
      if ( type == 'sshkey' )
        this.send('sshKeyChanged',change);
      else if ( type == 'apikey' )
        this.send('apiKeyChanged',change);
    },

    apiKeyChanged: function(change) {
      this.mergeChangeInto('apikeys', change);
    },

    sshKeyChanged: function(change) {
      this.mergeChangeInto('sshkeys', change);
    },

    hostChanged: function(change) {
      console.log('Host Changed:', change);
      this.mergeChangeInto('hosts', change);
    },

    agentChanged: function(change) {
      console.log('Agent Changed:', change);
      var agent = change.data.resource;
      var id = agent.id;
      delete agent.hosts;

      var hosts = this.controllerFor('hosts');
      hosts.forEach(function(host) {
        if ( host.get('agent.id') == id )
        {
          host.get('agent').setProperties(agent);
        }
      });
    },

    containerChanged:       function(change) { this.instanceChanged(change) },
    instanceChanged:        function(change) { this.instanceChanged(change) },
    virtualMachineChanged:  function(change) { this.instanceChanged(change) },
  },
});
;

// RadioButton
App.RadioButton = Ember.Component.extend({
  tagName: 'input',
  type: 'radio',
  disabled: false,
  attributeBindings: ['name', 'type', 'value', 'checked:checked', 'disabled:disabled'],

  click : function() {
    this.set('selection', this.$().val());
  },

  checked : function() {
    console.log(this.get('value'),this.get('selection'),lc('virtualMachineEdit').get('content.imageId'));
    return this.get('value') === this.get('selection');
  }.property('value','selection')
});

Em.Handlebars.helper('radio-button',App.RadioButton);
;

// Host
// Model
App.Host = App.TransitioningResource.extend();
App.Host.reopenClass({
  baseUrl: 'hosts'
})

App.Host.reopen({
  type: 'host',

  displayName: function() {
    return this.get('name') || this.get('id')
  }.property('name','id'),

  instancesUpdated: 0,
  onInstanceChanged: function() {
    this.incrementProperty('instancesUpdated');
  }.observes('instances.@each.{id,name,state}','instances.length'),

  state: function() {
    var host = this.get('hostState');
    if ( host == 'active' )
    {
      return this.get('agent.state');
    }
    else
    {
      return host;
    }
  }.property('hostState','agent.state'),

  transitioning: function() {
    var host = this.get('hostTransitioning');
    if ( host == 'no' )
    {
      return this.get('agent.transitioning');
    }
    else
    {
      return host;
    }
  }.property('hostTransitioning','agent.transitioning'),

  transitioningMessage: function() {
    if ( this.get('hostTransitioning') == 'no' )
    {
      return this.get('agent.transitioningMessage');
    }
    else
    {
      return this.get('hostTransitioningMessage');
    }
  }.property('hostTransitioningMessage','agent.transitioningMessage'),

  transitioningProgress: function() {
    if ( this.get('hostTransitioning') == 'no' )
    {
      return this.get('agent.transitioningProgress');
    }
    else
    {
      return this.get('hostTransitioningProgress');
    }
  }.property('hostTransitioningProgress','agent.transitioningProgress'),

});

// Collection
App.HostsRoute = Em.Route.extend({
  all: null,

  model: function() {
    if ( this.get('all') )
      return this.get('all');
    else
      return App.Host.find(null,{include: ['agent','instances','physicalHosts']});
  },

  afterModel: function(model, transition) {
    this.set('all',model);
  },

  actions: {
    newContainer: function() {
      this.transitionTo('newContainer');
    },
  },
});

App.HostsView = Em.View.extend({
  templateName: 'hosts',

  resizeFn: null,
  didInsertElement: function() {
    console.log('didInsertElement');
    this._super();
    
    this.set('resizeFn', this.onResize.bind(this));
    $(window).on('resize', this.get('resizeFn'));
  },

  willDestroyElement: function() {
    $(window).off('resize', this.get('resizeFn'));
  },

  windowWidth: $(window).width(),
  onResize: function() {
    this.set('windowWidth', $(window).width());
  },

  columnCount: function() {
    var width = this.get('windowWidth');
    return Math.floor((width-42)/310);
  }.property('windowWidth'),

  columns: function() {
    var hosts = this.get('context.byPhysical');
    var total = hosts.get('length');
    var columnCount = this.get('columnCount');
    var perColumn = Math.ceil(total/columnCount);

    var out = [];
    for ( var i = 0 ; i < total ; i++ )
    {
      // If on a column boundary, create a new column
      if ( i % perColumn == 0 )
      {
        out.push([]);
      }

      // Add the host to the last column
      out[out.length-1].push(hosts[i]);
    }

    return out;
  }.property('context.byPhysical.[]','columnCount'),
});

App.HostsController = App.CattleCollectionController.extend({
  itemController: 'host',

  byPhysical: function() {
    var phy = {};
    this.get('model').forEach(function(host) {
      var phyId = host.get('physicalHostId');
      if ( phy[phyId] )
      {
        phy[phyId].get('hosts').pushObject(host);
        phy[phyId].set('multiple',true);
      }
      else
      {
        phy[phyId] = Em.Object.create({
                      id: phyId,
                      name: host.get('physicalHost.name') || host.get('name'),
                      hosts: [host],
                      multiple: false
                     });
      }
    });

    var out = [];
    Object.keys(phy).forEach(function(key) {
      out.push(phy[key]);
    });

    out.sort(function(a,b) {
      var an = a.get('name');
      var bn = b.get('name');
      return (an < bn ? -1 : (an > bn ? 1 : 0));
    });

    return out;
  }.property('model.[]','model.@each.physicalHostId')
});

App.HostsItemView = Em.View.extend({
  classNames: ['host'],
  tagName: 'DIV',
  templateName: 'hosts-item',
  didInsertElement: function() {
    this.$('BUTTON, SPAN, I').tooltip();
  }
});

// Resource
App.HostRoute = Em.Route.extend({
});

function _hostIcon(host)
{
  if ( host.get('isDocker') )
    return 'fa-cubes';
  else
    return 'fa-cube';
}

App.HostController = App.TransitioningResourceController.extend({
  isDocker: Em.computed.equal('kind','docker'),
  isLibVirt: Em.computed.equal('kind','libvirt'),

  actions: {
    activate:   function() { return this.doAction('activate'); },
    deactivate: function() { return this.doAction('deactivate'); },
    delete:     function() { return this.delete(); },
    purge:      function() { return this.doAction('purge'); },
  }
});


App.HostController.reopenClass({
  stateMap: {
    'requested':        {icon: 'fa-ticket',      color: 'text-danger'},
    'registering':      {icon: 'fa-ticket',      color: 'text-danger'},
    'activating':       {icon: 'fa-ticket',      color: 'text-danger'},
    'active':           {icon: _hostIcon,        color: ''},
    'reconnecting':     {icon: 'fa-cog fa-spin', color: 'text-danger'},
    'updating-active':  {icon: _hostIcon,        color: 'text-success'},
    'updating-inactive':{icon: 'fa-stop',        color: 'text-danger'},
    'deactivating':     {icon: 'fa-pause',       color: 'text-danger'},
    'inactive':         {icon: 'fa-stop',        color: 'text-danger'},
    'removing':         {icon: 'fa-trash',       color: 'text-danger'},
    'removed':          {icon: 'fa-trash',       color: 'text-danger'},
    'purging':          {icon: 'fa-fire',        color: 'text-danger'},
    'purged':           {icon: 'fa-fire',        color: 'text-danger'},
    'restoring':        {icon: 'fa-trash',       color: 'text-danger'},
  }
})

App.HostContainerView = Em.View.extend({
  classNames: ['instance'],
  tagName: 'DIV',
  templateName: 'host-container-item',
  didInsertElement: function() {
    this.$('BUTTON, SPAN, I').tooltip();
  }
});

App.HostVirtualMachineView = Em.View.extend({
  classNames: ['instance'],
  tagName: 'DIV',
  templateName: 'host-virtualmachine-item',
  didInsertElement: function() {
    this.$('BUTTON, SPAN, I').tooltip();
  }
});

App.HostView = Em.View.extend();

App.HostEditRoute = Em.Route.extend({
});

App.HostEditView = Em.View.extend({
  templateName: 'host-edit'
});

App.AddHostView = Em.View.extend({
  classNames: ['host','add-host'],
  templateName: 'add-host',
});

// --------
// Delete
// --------
App.HostDeleteRoute = Em.Route.extend({
  renderTemplate: function() {
    this.render('confirmDelete', {into: 'application', outlet: 'overlay', controller: 'host'});
  },

  actions: {
    confirm: function() {
      this.controllerFor('host').send('delete');
      this.transitionTo('hosts');
    },

    cancel: function() {
      this.transitionTo('hosts');
    }
  }
});
;

// Network
App.Network = App.TransitioningResource.extend();
App.Network.reopenClass({
  baseUrl: 'networks'
});

App.Network.reopen({
  kind: null,
  isPublic: null,
  macPrefix: null,
});

App.NetworkController = App.TransitioningResourceController.extend({
});

App.NetworkController.reopenClass({
  stateMap: {
    'active':     {icon: 'fa-code-fork',  color: 'text-success'},
    'inactive':   {icon: 'fa-pause',      color: 'text-muted'},
    'purged':     {icon: 'fa-fire',       color: 'text-danger'},
    'removed':    {icon: 'fa-fire',       color: 'text-danger'},
    'requested':  {icon: 'fa-ticket',     color: 'text-info'},
  }
});

// Collection
App.NetworksController = App.CattleCollectionController.extend({
  itemController: 'network',
});
;

// Ippool
App.Ippool = App.TransitioningResource.extend();
App.Ippool.reopenClass({
  baseUrl: 'ippools'
});

App.Ippool.reopen({
  kind: null,
  isPublic: null,
  macPrefix: null,
});

App.IppoolController = App.TransitioningResourceController.extend({
});

App.IppoolController.reopenClass({
  stateMap: {
    'active':     {icon: 'fa-life-ring',  color: 'text-success'},
    'inactive':   {icon: 'fa-pause',      color: 'text-muted'},
    'purged':     {icon: 'fa-fire',       color: 'text-danger'},
    'removed':    {icon: 'fa-fire',       color: 'text-danger'},
    'requested':  {icon: 'fa-ticket',     color: 'text-info'},
  }
});

// Collection
App.IppoolsController = App.CattleCollectionController.extend({
  itemController: 'ippool',
});
;

// Instance
App.Instance = App.TransitioningResource.extend();
App.Instance.reopenClass({
  baseUrl: 'instances'
});

App.Instance.reopen({
  requestedHostId: null,
  networkIds: null,
});

App.InstanceController = App.TransitioningResourceController.extend({
  actions: {
    restart:  function() { return this.doAction('restart'); },
    start:    function() { return this.doAction('start'); },
    stop:     function() { return this.doAction('stop'); },
    delete:   function() { return this.delete(); },
    purge:    function() { return this.doAction('purge'); },
  },

  isOn: function() {
    return ['running','updating-running','migrating','restarting'].indexOf(this.get('state')) >= 0;
  }.property('state'),
});

App.InstanceController.reopenClass({
  stateMap: {
   'running': {icon: 'fa-bolt',   color: 'text-success'},
   'stopped': {icon: 'fa-stop',   color: 'text-danger'},
   'removed': {icon: 'fa-trash',  color: 'text-danger'},
   'purged':  {icon: 'fa-fire',   color: 'text-danger'}
  },
});

App.InstanceEditRoute = Em.Route.extend({
  controllerName: 'instanceEdit', // Override me
  templateName: 'instanceEdit', // Override me

  model: function() {
    // Override me
    var model = this.modelFor('instance');
    return model;
  },

  afterModel: function() {
    return this.controllerFor(this.get('controllerName')).loadDependencies();
  },

  setupController: function(controller, model) {
    controller.set('originalModel',model);
    controller.set('model', model.clone());
    controller.set('editing',true);
    controller.initFields();
  },

  renderTemplate: function() {
    this.render(this.get('templateName'), {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('hosts');
    },
  }
});

App.InstanceEditController = Em.ObjectController.extend({
  needs: ['networks'],
  editing: false,
  saving: false,
  originalModel: null,

  loadDependencies: function() {
  },

  initFields: function() {
    var networkIds = this.get('networkIds');
    if ( networkIds && networkIds.length > 0 )
      this.set('networkId', networkIds[0]);
    else
      this.set('networkId', null);
  },

  saveDefaults: function() {
    // Override me to remember more defaults for future new creates
    this.set('lastNetworkId', (this.get('networkIds')||[])[0]);
  },

  networkId: null,
  networks: Em.computed.alias('controllers.networks'),
  networkIdDidChange: function() {
    var ary = this.get('networkIds')||[];
    ary.length = 0;
    ary.push(this.get('networkId'));
  }.observes('networkId'),

  validate: function() {
    return true;
  },

  error: null,
  actions: {
    error: function(err) {
      var msg;
      if ( err.get('status') == 422 )
      {
        switch ( err.get('fieldName') )
        {
          case 'imageUuid':
            msg = 'Invalid source image name';
            break;
          default:
            msg = 'Invalid ' + err.get('fieldName');
            break;
        }
      }

      this.set('error', msg);
    },

    save: function() {
      var self = this;

      this.set('error',null);
      var ok = this.validate();
      if ( !ok )
        return;

      if ( self.get('saving') )
      {
        return;
      }

      self.set('saving',true);

      var model = self.get('model');
      model.save().then(function(newData) {
        if ( self.get('editing') )
        {
          var original = self.get('originalModel');
          if ( original )
            original.merge(newData);
        }
        else
        {
          self.saveDefaults();
        }

        self.transitionToRoute('hosts');

      }).catch(function(err) {
        self.send('error', err);
      }).finally(function() {
        self.set('saving',false);
      });
    },
  }
});

App.InstanceEditView = App.OverlayEditView.extend({
  // Overwrite me
  // templateName: 'instance-edit'
});

// --------
// New
// --------
App.InstanceNewRoute = Em.Route.extend({
  controllerName: 'instanceEdit', // Override me
  templateName: 'instanceEdit', // Override me

  model: function(params, transition) {
    // @TODO Fix this hack
    // Return the host as the "model", then setup the real model in setupController
    return App.Host.find(params.host_id);
  },

  afterModel: function() {
    return this.controllerFor(this.get('controllerName')).loadDependencies();
  },


  setupController: function(generatedController, model) {
    // Overwrite me to generate the real model given (generatedController,host)
    // And then call this.super(generatedController, realModel)
    var controller = this.controllerFor(this.get('controllerName'))
    controller.set('model', model);
    controller.set('editing',false);
    controller.initFields();
  },

  renderTemplate: function() {
    this.render(this.get('templateName'), {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('hosts');
    },
  }
});

// --------
// Delete
// --------
App.InstanceDeleteRoute = Em.Route.extend({
  controllerName: 'instance', // Override me

  renderTemplate: function() {
    this.render('confirmDelete', {
      into: 'application',
      outlet: 'overlay',
      controller: this.get('controllerName')
    });
  },

  actions: {
    confirm: function() {
      this.controllerFor(this.get('controllerName')).send('delete');
      this.transitionTo('hosts');
    },

    cancel: function() {
      this.transitionTo('hosts');
    }
  }
});
;

// Container
App.Container = App.Instance.extend({
  imageUuid: null,
  requestedHostId: null,
  command: null,
  commandArgs: null,
  environment: null,
  ports: null,
  instanceLinks: null
});

App.Container.reopenClass({
  baseUrl: 'containers'
});

App.ContainerController = App.InstanceController.extend();

// --------
// Edit
// --------
App.ContainerEditRoute = App.InstanceEditRoute.extend({
  controllerName: 'containerEdit',
  templateName: 'containerEdit',

  model: function() {
    var model = this.modelFor('container');
    return Em.RSVP.all([
      model.importLink('ports'),
      model.importLink('instanceLinks')
    ]).then(function() {
      return model;
    });
  },
});

App.ContainerEditController = App.InstanceEditController.extend({
  needs: ['hosts'],

  initFields: function() {
    this._super();
    this.initEnvironment();
    this.initArgs();
    this.initPorts();
    this.initLinks();
    this.userImageUuidDidChange();
  },

  loadDependencies: function() {
    var self = this;
    return Em.RSVP.all([
      App.Network.find().then(function(data) {
        self.set('controllers.networks.model',data);
      }),
    ]);
  },

  // Environment Vars
  environmentArray: null,
  initEnvironment: function() {
    var obj = this.get('environment')||[];
    var keys = Object.keys(obj);
    var out = [];
    keys.forEach(function(key) {
      out.push({ key: key, value: obj[key] });
    });

    this.set('environmentArray', out);
  },

  environmentChanged: function() {
    var ary = this.get('environmentArray');
    ary.beginPropertyChanges();
    
    // Remove empty rows in the middle
    // and sync with the actual environment
    var row;
    var out = {};
    var len = ary.get('length');
    for ( var i = len-1 ; i >= 0 ; i-- )
    {
      row = ary.objectAt(i);
      if ( row.key && row.value )
      {
        out[row.key] = row.value;
      }
      else if ( i != len-1 )
      {
        // Don't remove the last one
        ary.removeAt(i);
      }
    }

    // Add an empty row at the end
    var last = ary.objectAt(ary.get('length')-1);
    if ( !last || (last.key && last.value) )
      ary.pushObject({key: '', value: ''});

    this.set('environment', out);
    ary.endPropertyChanges();
  }.observes('environmentArray.@each.{key,value}'),

  // Command Arguments
  argsArray: null,
  initArgs: function() {
    var out = [];
    var args = this.get('commandArgs')||[];
    args.forEach(function(value) {
      out.push({value: value});
    });

    this.set('argsArray', out);
  },

  argsChanged: function() {
    var ary = this.get('argsArray');
    ary.beginPropertyChanges();
    
    // Remove empty rows in the middle
    // and sync with the actual environment
    var row;
    var out = [];
    var len = ary.get('length');
    for ( var i = len-1 ; i >= 0 ; i-- )
    {
      row = ary.objectAt(i);
      if ( row.value )
      {
        out.push(row.value);
      }
      else if ( i != len-1 )
      {
        // Don't remove the last one
        ary.removeAt(i);
      }
    }

    // Add an empty row at the end
    var last = ary.objectAt(ary.get('length')-1);
    if ( !last || last.value )
    {
      ary.pushObject({value: ''});
    }

    this.set('commandArgs', out);
    ary.endPropertyChanges();
  }.observes('argsArray.@each.value'),

  // Ports
  protocolOptions: [
    {label: 'TCP', value: 'tcp'}, 
    {label: 'UDP', value: 'udp'}
 ],
  portsArray: null,
  initPorts: function() {
    var out = [];
    var ports = this.get('ports')||[];
    ports.forEach(function(value) {
      // Objects, from edit
      if ( value.id )
      {
        out.push({public: value.publicPort, private: value.privatePort, protocol: value.protocol, existing: true});
      }
      else
      {
        // Strings, from create maybe
        var match = value.match(/^(\d+):(\d+)\/(.*)$/);
        if ( match )
        {
          out.push({public: match[1], private: match[2], protocol: match[3], existing: false});
        }
      }
    });

    this.set('portsArray', out);
  },

  portsChanged: function() {
    var ary = this.get('portsArray');
    ary.beginPropertyChanges();
    
    // Remove empty rows in the middle
    // and sync with the actual environment
    var row;
    var out = [];
    var len = ary.get('length');
    for ( var i = len-1 ; i >= 0 ; i-- )
    {
      row = ary.objectAt(i);
      if ( row.public && row.private && row.protocol )
      {
        out.push(row.public+":"+row.private+'/'+row.protocol);
      }
      else if ( i != len-1 )
      {
        // Don't remove the last one
        ary.removeAt(i);
      }
    }

    // Add an empty row at the end
    if ( !this.get('editing') )
    {
      var last = ary.objectAt(ary.get('length')-1);
      if ( !last || (last.public && last.private && last.protocol) )
      {
        ary.pushObject({public: '', private: '', protocol: 'tcp'});
      }
    }

    this.set('ports', out);
    ary.endPropertyChanges();
  }.observes('portsArray.@each.{public,private,protocol}'),

  // Links
  linksArray: null,
  containers: function() {
    var list = [];
    var id = this.get('id');
    this.get('controllers.hosts').forEach(function(host) {
      list.pushObjects((host.get('instances')||[]).filter(function(instance) {
        // You can't link to yourself
        return instance.get('id') != id && instance.get('kind') == 'container';
      }));
    });


    return list;
  }.property('controllers.hosts.@each.[]','controllers.hosts.@each.instancesUpdated').volatile(),

  initLinks: function() {
    var out = [];
    var links = this.get('instanceLinks')||[];

    links.forEach(function(value) {
      // Objects, from edit
      if ( value.id )
      {
        out.push({linkName: value.linkName, targetInstanceId: value.targetInstanceId, existing: true});
      }
      else
      {
        // Strings, from create maybe
        var match = value.match(/^([^:]+):(.*)$/);
        if ( match )
        {
          out.push({linkName: match[1], targetInstanceId: match[2], existing: false});
        }
      }
    });

    this.set('linksArray', out);
  },

  linksChanged: function() {
    var ary = this.get('linksArray');
    ary.beginPropertyChanges();
    
    // Remove empty rows in the middle
    // and sync with the actual environment
    var row;
    var out = {};
    var len = ary.get('length');
    for ( var i = len-1 ; i >= 0 ; i-- )
    {
      row = ary.objectAt(i);
      if ( row.linkName && row.targetInstanceId )
      {
        out[ row.linkName ] = row.targetInstanceId;
      }
      else if ( i != len-1 )
      {
        // Don't remove the last one
        ary.removeAt(i);
      }
    }

    // Add an empty row at the end
    if ( !this.get('editing') )
    {
      var last = ary.objectAt(ary.get('length')-1);
      if ( !last || (last.linkName && last.targetInstanceId) )
      {
        ary.pushObject({linkName: '', targetInstanceId: null});
      }
    }

    this.set('instanceLinks', out);
    ary.endPropertyChanges();
  }.observes('linksArray.@each.{linkName,targetInstanceId}'),

  userImageUuid: 'stackbrew/ubuntu:14.04',
  userImageUuidDidChange: function() {
    var image = this.get('userImageUuid');
    if ( image.indexOf('docker:') === 0 )
    {
      this.set('userImageUuid', image.replace(/^docker:/,''));
    }
    else
    {
      image = 'docker:' + image;
    }

    this.set('imageUuid', image);
  }.observes('userImageUuid'),

  validate: function() {
    return true;
  },

  actions: {
    removeArg: function(obj) {
      this.get('argsArray').removeObject(obj);
    },

    removeEnvironment: function(obj) {
      this.get('environmentArray').removeObject(obj);
    },

    removePort: function(obj) {
      this.get('portsArray').removeObject(obj);
    },

    removeLink: function(obj) {
      this.get('linksArray').removeObject(obj);
    },
  },
});

App.ContainerEditView = App.InstanceEditView.extend({
  templateName: 'container-edit',
});

// --------
// New
// --------
App.ContainerNewRoute = App.InstanceNewRoute.extend({
  controllerName: 'containerEdit',
  templateName: 'containerEdit',

  setupController: function(generatedController, host) {
    var controller = this.controllerFor('containerEdit');

    var networkId = controller.get('lastNetworkId');
    if ( !networkId )
    {
      networkId = this.controllerFor('networks').get('firstObject.id');
    }

    var model = App.Container.create({
      requestedHostId: host.get('id'),
      commandArgs: [],
      networkIds: [networkId],
      environment: {
      }
    });

    this._super(generatedController, model);
  },
});

// --------
// Delete
// --------
App.ContainerDeleteRoute = App.InstanceDeleteRoute.extend({
  controllerName: 'container'
});

App.ContainerLoadingRoute = Em.Route.extend({
  renderTemplate: function() {
    this.render('loading', {into: 'application', outlet: 'overlay'});
  },
});
;

// VirtualMachine
App.VirtualMachine = App.Instance.extend({
  imageId: null,
  memoryMb: null,
  credentialIds: null,
  userdata: null
});

App.VirtualMachine.reopenClass({
  baseUrl: 'virtualmachines'
});

App.VirtualMachineController = App.InstanceController.extend();

// --------
// Individual View
// --------
/*
App.VirtualMachineIndexRoute = Em.Route.extend({
  renderTemplate: function() {
    this.render('virtualMachineIndex', {into: 'application', outlet: 'overlay'});
  },
});

App.VirtualMachineIndexView = App.OverlayView.extend({
  templateName: 'virtualmachine',

  actions: {
    overlayClose: function() {
      this.get('controller').send('close');
    },
  }
});
*/


// --------
// Console
// --------
App.VirtualMachineConsoleRoute = Em.Route.extend({
  model: function() {
    var vm = this.modelFor('virtualMachine');
    var promise = vm.doAction('console').then(function(console) {
      console.set('virtualMachine', vm);
      return console;
    });
    return promise;
  },

  renderTemplate: function() {
    this.render('virtualMachineConsole', {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('hosts');
    },
  }
});

App.VirtualMachineConsoleController = Em.ObjectController.extend({
  rfb: null,
});

App.VirtualMachineConsoleView = App.OverlayView.extend({
  templateName: 'virtualmachine-console',

  ctrlAltDeleteDisabled: true,
  actions: {
    overlayClose: function() {
      this.get('controller').send('close');
    },

    ctrlAltDelete: function() {
      this.get('rfb').sendCtrlAltDel();
    },
  },

  didInsertElement: function() {
    var self = this;
    var rfb;

    function updateState(rfb, state, oldstate, msg)
    {
      if (typeof msg !== 'undefined')
      {
        // The console goes away during willDestroyElement and can't update status anymore
        var elem = self.$('.console-status');
        if ( elem )
          elem.text(msg);
      }

      self.set('ctrlAltDeleteDisabled',(state !== 'normal'));
    }

    rfb = new NoVNC.RFB({
      'target':       self.$('.console-canvas')[0],
      'encrypt':      false,
      'repeaterID':   '',
      'true_color':   true,
      'local_cursor': true,
      'shared':       true,
      'view_only':    false,
      'updateState':  updateState,
    });

    rfb.connect(self.get('context.host'), self.get('context.port'), self.get('context.password'), self.get('context.path'));

    this.set('rfb',rfb);
  },

  willDestroyElement: function() {
    this.get('rfb').disconnect();
  }
});

// --------
// Edit
// --------
App.VirtualMachineEditRoute = App.InstanceEditRoute.extend({
  controllerName: 'virtualMachineEdit',
  templateName: 'virtualMachineEdit',

  model: function() {
    var model = this.modelFor('virtualMachine');
    return model;
  },
});

App.VirtualMachineEditController = App.InstanceEditController.extend({
  needs: ['ippools','images','sshkeys'],

  publicIppools: Em.computed.alias('controllers.ippools'),

  saveDefaults: function() {
    this._super();
    this.set('lastMemoryMb', this.get('memoryMb'));
    this.set('lastImageId', this.get('imageId'));
    this.set('lastCredentialIds', this.get('credentialIds'));
  },

  loadDependencies: function() {
    var self = this;
    return Em.RSVP.all([
      App.Image.find().then(function(data) {
        self.set('controllers.images.model',data.filter(function(img) {
          return img.get('format') != 'docker'
        }));
      }),
      App.Ippool.find(null, {filter: {isPublic: true}}).then(function(data) {
        self.set('controllers.ippools.model',data);
      }),
      App.Network.find().then(function(data) {
        self.set('controllers.networks.model',data);
      }),
      App.Sshkey.find().then(function(data) {
        self.set('controllers.sshkeys.model',data);
      }),
    ]);
  },

  memoryChoices: [
    {size: 64,   label: '64 MB'},
    {size: 128,  label: '128 MB'},
    {size: 256,  label: '256 MB'},
    {size: 512,  label: '512 MB'},
    {size: 1024, label: '1 GB'},
    {size: 2048, label: '2 GB'},
    {size: 4096, label: '4 GB'},
    {size: 8192, label: '8 GB'},
    {size: 16384, label: '16 GB'},
    {size: 32768, label: '32 GB'},
  ],

  images: Em.computed.alias('controllers.images'),

  sshkeys: Em.computed.alias('controllers.sshkeys'),
  sshkeyObjs: function() {
    var out = [];
    var selected = this.get('credentialIds')||[];
    this.get('sshkeys').forEach(function(key) {
      out.push({
        id: key.get('id'),
        displayName: key.get('displayName'),
        selected: selected.indexOf(key.get('id')) >= 0
      });
    });

    return out;
  }.property('sshkeys.[]'),

  sshkeyDidChange: function() {
    var selected = this.get('sshkeyObjs').filter(function(key) {
      return Em.get(key,'selected');
    }).map(function(key) {
      return Em.get(key,'id');
    });

    this.set('credentialIds', selected);
  }.observes('sshkeyObjs.[]','sshkeyObjs.@each.selected'),
});

App.VirtualMachineEditView = App.InstanceEditView.extend({
  templateName: 'virtualmachine-edit',
});

// --------
// New
// --------
App.VirtualMachineNewRoute = App.InstanceNewRoute.extend({
  controllerName: 'virtualMachineEdit',
  templateName: 'virtualMachineEdit',

  setupController: function(generatedController, host) {
    var controller = this.controllerFor('virtualMachineEdit');

    var imageId = controller.get('lastImageId');
    if ( !imageId )
    {
      this.controllerFor('images').forEach(function(item) {
        if ( (item.get('name')||'').toLowerCase().indexOf('cirros') >= 0 )
          imageId = item.get('id');
      });
    }

    var networkId = controller.get('lastNetworkId');
    if ( !networkId )
    {
      networkId = this.controllerFor('networks').get('firstObject.id');
    }

    var sshKeys = controller.get('lastCredentialIds');
    if ( !sshKeys )
    {
      this.controllerFor('sshkeys').forEach(function(item) {
        if ( (item.get('name')||'').toLowerCase().indexOf('default ssh key') >= 0 )
          sshKeys = [ item.get('id') ];
      });
    }

    var model = App.VirtualMachine.create({
      requestedHostId: host.get('id'),
      memoryMb: controller.get('lastMemoryMb') || 64,
      imageId: imageId,
      credentialIds: sshKeys,
      networkIds: [networkId]
    });

    this._super(generatedController, model);
  },
});

// --------
// Delete
// --------
App.VirtualMachineDeleteRoute = App.InstanceDeleteRoute.extend({
  controllerName: 'virtualMachine'
});
;

// Image
App.Image = App.TransitioningResource.extend();
App.Image.reopenClass({
  baseUrl: 'images'
});

App.Image.reopen({
  isPublic: null,
  physicalSizeMb: null,
  url: null,
  virtualSizeMb: null
});

App.ImageController = App.TransitioningResourceController.extend({
});

App.ImageController.reopenClass({
  stateMap: {
    'active':     {icon: 'fa-code-fork',  color: 'text-success'},
    'inactive':   {icon: 'fa-pause',      color: 'text-muted'},
    'purged':     {icon: 'fa-fire',       color: 'text-danger'},
    'removed':    {icon: 'fa-fire',       color: 'text-danger'},
    'requested':  {icon: 'fa-ticket',     color: 'text-info'},
  }
});

// Collection
App.ImagesController = App.CattleCollectionController.extend({
  itemController: 'image',
});
;

// Apikey
App.Apikey = App.TransitioningResource.extend();
App.Apikey.reopenClass({
  baseUrl: 'apikeys'
});

App.Apikey.reopen({
  publicValue: null,
  secretValue: null,
  kind: 'apiKey'
});

App.ApikeyController = App.TransitioningResourceController.extend({
  actions: {
    deactivate: function() { return this.doAction('deactivate'); },
    activate:   function() { return this.doAction('activate'); },
    edit:       function() { this.transitionToRoute('apikey.edit',this.get('model')); },
    delete:     function() { this.transitionToRoute('apikey.delete',this.get('model')); },
  },

  canEdit: function() {
    var state = this.get('state');
    return state != 'removed' && state != 'purged';
  }.property('state'),

  canDelete: function() {
    var state = this.get('state');
    return state == 'inactive' || state == 'requested';
  }.property('state'),
});

App.ApikeyController.reopenClass({
  stateMap: {
    'active':     {icon: 'fa-code',   color: 'text-success'},
    'inactive':   {icon: 'fa-pause',  color: 'text-muted'},
    'purged':     {icon: 'fa-fire',   color: 'text-danger'},
    'removed':    {icon: 'fa-fire',   color: 'text-danger'},
    'requested':  {icon: 'fa-ticket', color: 'text-info'},
  }
});

App.ApikeyRoute = Em.Route.extend({
  model: function(params, transition) {
    return App.Apikey.find(params.apikey_id);
  }
});

// Collection
App.ApikeysController = App.CattleCollectionController.extend({
  itemController: 'apikey',
});

App.ApikeysRoute = Em.Route.extend({
  all: null,

  model: function() {
    if ( this.get('all') )
      return this.get('all');
    else
      return App.Apikey.find();
  },

  afterModel: function(model, transition) {
    this.set('all',model);
    this.controllerFor('apikeys').set('model',model);
  },

  actions: {
    newApikey: function() {
      this.transitionTo('apikeys.new');
    },
  },
});

App.ApikeysView = Em.View.extend({
  templateName: 'apikeys',
  didInsertElement: function() {
    this.$('BUTTON, SPAN, I').tooltip();
  }
});

// --------
// Edit
// --------
App.ApikeyEditRoute = Em.Route.extend({
  model: function() {
    return this.modelFor('apikey');
  },

  setupController: function(controller, model) {
    controller.set('originalModel',model);
    controller.set('model', model.clone());
    controller.set('editing',true);
  },

  renderTemplate: function() {
    this.render('apikeyEdit', {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('apikeys');
    },
  }
});

App.ApikeyEditController = Em.ObjectController.extend({
  editing: false,
  saving: false,
  originalModel: null,


  actions: {
    save: function() {
      var self = this;
      var model = self.get('model');

      if ( self.get('saving') )
      {
        return;
      }

      self.set('saving',true);
      model.save().then(function(newData) {
        if ( self.get('editing') )
        {
          var original = self.get('originalModel');
          if ( original )
            original.merge(newData);
        }
        self.transitionToRoute('apikeys');
      }).catch(function(err) {
        self.send('error', err);
      }).finally(function() {
        self.set('saving',false);
      });
    },
  }
});

App.ApikeyEditView = App.OverlayView.extend({
  templateName: 'apikey-edit',
});



// --------
// New
// --------
App.ApikeysNewRoute = Em.Route.extend({
  model: function(params, transition) {
    var self = this;
    var cred = App.Apikey.create({
      kind: 'apiKey',
    });

    return cred.save().then(function(newData) {
      self.controllerFor('apikeys').pushObject(cred);
      return cred;
    }).catch(function(err) {
      self.send('error', err);
    });
  },

  setupController: function(generatedController, model) {
    var controller = this.controllerFor('apikeyEdit');
    controller.set('model', model);
    controller.set('editing',false);
  },

  renderTemplate: function() {
    this.render('apikeyEdit', {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('apikeys');
    },
  }
});

// --------
// Delete
// --------
App.ApikeyDeleteRoute = Em.Route.extend({
  renderTemplate: function() {
    this.render('confirmDelete', {into: 'application', outlet: 'overlay', controller: 'apikey'});
  },

  actions: {
    confirm: function() {
      var self = this;
      var controller = this.controllerFor('apikey');
      controller.delete().then(function() {
        self.transitionTo('apikeys');
      }).catch(function(err) {
        controller.send('error',err);
      });
    },

    cancel: function() {
      this.transitionTo('apikeys');
    }
  }
});
;

// Sshkey
App.Sshkey = App.TransitioningResource.extend();
App.Sshkey.reopenClass({
  baseUrl: 'sshkeys'
});

App.Sshkey.reopen({
  publicValue: null,
  secretValue: null,
  kind: 'sshKey'
});

App.SshkeyController = App.TransitioningResourceController.extend({
  actions: {
    deactivate: function() { return this.doAction('deactivate'); },
    activate:   function() { return this.doAction('activate'); },
    edit:       function() { this.transitionToRoute('sshkey.edit',this.get('model')); },
    delete:     function() { this.transitionToRoute('sshkey.delete',this.get('model')); },
    downloadPem: function() {
      var url = this.get('links.pem');
      if ( url )
        Util.download(url);
      else
        Growl.error('Unable to find download link');
    }
  },

  publicParts: function() {
    var str = this.get('publicValue');
    var key, comment, algorithm, matches;

    if ( !str )
      return {};

    var bits = parseInt(str.substr(0,str.indexOf(' ')), 10);
    if ( bits > 0 )
    {
      // rsa 1
      match = str.match(/^\s*(\d+\s+){3}\s*(.*)\s*$/);
      algorithm = 'rsa1';
      key = match[1];
      comment = match[2];
    }
    else
    {
      // Newer algorithm
      match = str.match(/^\s*([^\s]+)\s+([^\s]+)\s*(.*)\s*$/);
      switch ( match[1].toLowerCase() )
      {
        case 'ssh-dss':
          algorithm = 'dsa';
          break;
        case 'ssh-rsa':
          algorithm = 'rsa';
          break;
        case 'ecdsa-sha2-nistp256':
          algorithm = 'ecdsa';
          break;
        default:
          algorithm = match[1];
      }

      key = match[2];
      comment = match[3];
    }

    return {
      algorithm: algorithm,
      key: key,
      comment: comment
    }
  }.property('publicValue'),

  key: Em.computed.alias('publicParts.key'),
  algorithm: Em.computed.alias('publicParts.algorithm'),
  comment: Em.computed.alias('publicParts.comment'),

  unbreakablePublicValue: function() {
    var val = this.get('publicValue')||'';

    var str = '<span class="clip">'+ Em.Handlebars.Utils.escapeExpression(val.substr(0,20))+'</span>';
    str += Em.Handlebars.Utils.escapeExpression(val.substr(21));

    return new Em.Handlebars.SafeString(str);
  }.property('publicValue'),

  canEdit: function() {
    var state = this.get('state');
    return state != 'removed' && state != 'purged';
  }.property('state'),

  canDelete: function() {
    var state = this.get('state');
    return state == 'inactive' || state == 'requested';
  }.property('state'),
});

App.SshkeyController.reopenClass({
  stateMap: {
    'active':     {icon: 'fa-key',    color: 'text-success'},
    'inactive':   {icon: 'fa-pause',  color: 'text-muted'},
    'purged':     {icon: 'fa-fire',   color: 'text-danger'},
    'removed':    {icon: 'fa-fire',   color: 'text-danger'},
    'requested':  {icon: 'fa-ticket', color: 'text-info'},
  }
});

App.SshkeyRoute = Em.Route.extend({
  model: function(params, transition) {
    return App.Sshkey.find(params.sshkey_id);
  }
});

// Collection
App.SshkeysController = App.CattleCollectionController.extend({
  itemController: 'sshkey',
});

App.SshkeysRoute = Em.Route.extend({
  all: null,

  model: function() {
    if ( this.get('all') )
      return this.get('all');
    else
      return App.Sshkey.find();
  },

  afterModel: function(model, transition) {
    this.set('all',model);
    this.controllerFor('sshkeys').set('model',model);
  },

  actions: {
    newSshkey: function() {
      this.transitionTo('sshkeys.new');
    },
    importPublicKey: function() {
      this.transitionTo('sshkeys.import');
    },
  },
});

App.SshkeysView = Em.View.extend({
  templateName: 'sshkeys',
  didInsertElement: function() {
    this.$('BUTTON, SPAN, I').tooltip();
  }
});

// --------
// Edit
// --------
App.SshkeyEditRoute = Em.Route.extend({
  model: function() {
    return this.modelFor('sshkey');
  },

  setupController: function(controller, model) {
    controller.set('originalModel',model);
    controller.set('model', model.clone());
    controller.set('editing',true);
    controller.set('import',false);
  },

  renderTemplate: function() {
    this.render('sshkeyEdit', {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('sshkeys');
    },
  }
});

App.SshkeyEditController = Em.ObjectController.extend({
  editing: false,
  import: false,
  saving: false,
  originalModel: null,


  actions: {
    save: function() {
      var self = this;
      var model = self.get('model');

      if ( self.get('saving') )
      {
        return;
      }

      self.set('saving',true);
      model.save().then(function(newData) {
        if ( self.get('editing') )
        {
          var original = self.get('originalModel');
          if ( original )
            original.merge(newData);
        }
        self.transitionToRoute('sshkeys');
      }).catch(function(err) {
        self.send('error', err);
      }).finally(function() {
        self.set('saving',false);
      });
    },
  }
});

App.SshkeyEditView = App.OverlayView.extend({
  templateName: 'sshkey-edit',
  editPublicKey: Em.computed.or('controller.editing','controller.import'),
  showSecretKey: Em.computed.not('controller.import')
});


// --------
// Import
// --------
App.SshkeysImportRoute = Em.Route.extend({
  model: function(params, transition) {
    var self = this;
    var cred = App.Sshkey.create({
      kind: 'sshKey',
    });

    return cred;
  },

  setupController: function(generatedController, model) {
    var controller = this.controllerFor('sshkeyEdit');
    controller.set('model', model);
    controller.set('editing',false);
    controller.set('import',true);
  },

  renderTemplate: function() {
    this.render('sshkeyEdit', {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('sshkeys');
    },
  }
});

// --------
// New
// --------
App.SshkeysNewRoute = Em.Route.extend({
  model: function(params, transition) {
    var self = this;
    var cred = App.Sshkey.create({
      kind: 'sshKey',
    });

    return cred.save().then(function(newData) {
      self.controllerFor('sshkeys').pushObject(cred);
      return cred;
    }).catch(function(err) {
      self.send('error', err);
    });
  },

  setupController: function(generatedController, model) {
    var controller = this.controllerFor('sshkeyEdit');
    controller.set('model', model);
    controller.set('editing',false);
    controller.set('import',false);
  },

  renderTemplate: function() {
    this.render('sshkeyEdit', {into: 'application', outlet: 'overlay'});
  },

  actions: {
    cancel: function() {
      this.transitionTo('sshkeys');
    },
  }
});

// --------
// Delete
// --------
App.SshkeyDeleteRoute = Em.Route.extend({
  renderTemplate: function() {
    this.render('confirmDelete', {into: 'application', outlet: 'overlay', controller: 'sshkey'});
  },

  actions: {
    confirm: function() {
      var self = this;
      var controller = this.controllerFor('sshkey');
      controller.delete().then(function() {
        self.transitionTo('sshkeys');
      }).catch(function(err) {
        controller.send('error',err);
      });
    },

    cancel: function() {
      this.transitionTo('sshkeys');
    }
  }
});
;

// application
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'application',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  data.buffer.push("<div id=\"underlay\" class=\"underlay\"></div>\n<div id=\"loading-underlay\" class=\"underlay\"></div>\n");
  data.buffer.push(escapeExpression((helper = helpers.outlet || (depth0 && depth0.outlet),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "loading", options) : helperMissing.call(depth0, "outlet", "loading", options))));
  data.buffer.push("\n");
  data.buffer.push(escapeExpression((helper = helpers.outlet || (depth0 && depth0.outlet),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "overlay", options) : helperMissing.call(depth0, "outlet", "overlay", options))));
  data.buffer.push("\n");
  data.buffer.push(escapeExpression((helper = helpers['page-header'] || (depth0 && depth0['page-header']),options={hash:{
    'user': ("currentUser")
  },hashTypes:{'user': "ID"},hashContexts:{'user': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "page-header", options))));
  data.buffer.push("\n");
  data.buffer.push(escapeExpression((helper = helpers.outlet || (depth0 && depth0.outlet),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "error", options) : helperMissing.call(depth0, "outlet", "error", options))));
  data.buffer.push("\n<main>\n  ");
  stack1 = helpers._triageMustache.call(depth0, "outlet", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</main>\n");
  data.buffer.push(escapeExpression((helper = helpers.outlet || (depth0 && depth0.outlet),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "modal", options) : helperMissing.call(depth0, "outlet", "modal", options))));
  data.buffer.push("\n");
  return buffer;
  
}
);;

// error
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'error',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1;


  data.buffer.push("<h5>");
  stack1 = helpers._triageMustache.call(depth0, "message", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h5>\n<h6>");
  stack1 = helpers._triageMustache.call(depth0, "detail", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h6>\n<p>");
  stack1 = helpers._triageMustache.call(depth0, "stack", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</p>\n");
  return buffer;
  
}
);;

// loading
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'loading',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  


  data.buffer.push("<div class=\"loading-background\"></div>\n<div class=\"loading-box\">\n  <h3><i class=\"fa fa-spinner fa-spin\" style=\"width:30px;\"></i> Loading&hellip;</h3>\n</div>\n");
  
}
);;

// index
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'index',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  


  data.buffer.push("Index\n");
  
}
);;

// page-header
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'page-header',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'href': ("view.href")
  },hashTypes:{'href': "STRING"},hashContexts:{'href': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("><i class=\"fa fa-cubes\"></i> Hosts</a>\n    ");
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'href': ("view.href")
  },hashTypes:{'href': "STRING"},hashContexts:{'href': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("><i class=\"fa fa-key\"></i> SSH Keys</a>\n    ");
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'href': ("view.href")
  },hashTypes:{'href': "STRING"},hashContexts:{'href': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("><i class=\"fa fa-code\"></i> API</a>\n    ");
  return buffer;
  }

  data.buffer.push("<div class=\"container-fluid\">\n  <div class=\"navbar-header\">\n    <a class=\"navbar-brand\" href=\"");
  data.buffer.push(escapeExpression(helpers.unbound.call(depth0, "view.homeUrl", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\"><i class=\"fa fa-cloud\"></i> Rancher</a>\n  </div>\n\n  <ul class=\"nav navbar-nav\">\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{
    'tagName': ("li")
  },hashTypes:{'tagName': "STRING"},hashContexts:{'tagName': depth0},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "hosts", options) : helperMissing.call(depth0, "link-to", "hosts", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{
    'tagName': ("li")
  },hashTypes:{'tagName': "STRING"},hashContexts:{'tagName': depth0},inverse:self.noop,fn:self.program(3, program3, data),contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "sshkeys", options) : helperMissing.call(depth0, "link-to", "sshkeys", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{
    'tagName': ("li")
  },hashTypes:{'tagName': "STRING"},hashContexts:{'tagName': depth0},inverse:self.noop,fn:self.program(5, program5, data),contexts:[depth0],types:["STRING"],data:data},helper ? helper.call(depth0, "apikeys", options) : helperMissing.call(depth0, "link-to", "apikeys", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </ul>\n</div>\n");
  return buffer;
  
}
);;

// save-cancel
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'save-cancel',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  
  data.buffer.push("\n    <button class=\"btn btn-primary btn-disabled\"><i class=\"fa fa-spinner fa-spin\"></i> Saving...</button>\n  ");
  }

function program3(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "save", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-primary\">");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(6, program6, data),fn:self.program(4, program4, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</button>\n  ");
  return buffer;
  }
function program4(depth0,data) {
  
  
  data.buffer.push("Save");
  }

function program6(depth0,data) {
  
  
  data.buffer.push("Create");
  }

  data.buffer.push("<div class=\"footer-actions\">\n  ");
  stack1 = helpers['if'].call(depth0, "saving", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "cancel", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-link\">Cancel</button>\n</div>\n");
  return buffer;
  
}
);;

// confirm-delete
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'confirm-delete',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression;


  data.buffer.push("<h4>Are you sure you want to delete:</h4>\n\n<ul>\n  <li>");
  stack1 = helpers._triageMustache.call(depth0, "displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</li>\n</ul>\n\n<div class=\"footer-actions\">\n  <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "confirm", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-danger\">Delete</button>\n  <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "cancel", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-link\">Cancel</button>\n</div>\n");
  return buffer;
  
}
);;

// add-host
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'add-host',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  


  data.buffer.push("<label>\n  <div class=\"host-name\">Add a host</div>\n</label>\n<div class=\"well\">\n  <p>Try it on our hardware:</p>\n  <button class=\"btn btn-primary btn-block\">Try for free</button>\n  <br/>\n  <p>Or run in on your own:</p>\n  <button class=\"btn btn-default btn-block\">Amazon EC2</button>\n  <button class=\"btn btn-default btn-block\">Google Compute</button>\n  <button class=\"btn btn-default btn-block\">Rackspace</button>\n</div>\n");
  
}
);;

// hosts
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'hosts',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <div class=\"host-column\">\n      ");
  stack1 = helpers.each.call(depth0, "physicalHost", "in", "col", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(2, program2, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </div>\n  ");
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n        <div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":physical-host physicalHost.multiple:physical-host-multiple")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n          ");
  data.buffer.push(escapeExpression(helpers.each.call(depth0, "physicalHost.hosts", {hash:{
    'itemViewClass': ("App.HostsItemView"),
    'itemController': ("host")
  },hashTypes:{'itemViewClass': "STRING",'itemController': "STRING"},hashContexts:{'itemViewClass': depth0,'itemController': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n        </div>\n      ");
  return buffer;
  }

  data.buffer.push("<section class=\"hosts clearfix\">\n  ");
  stack1 = helpers.each.call(depth0, "col", "in", "view.columns", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</section>\n");
  return buffer;
  
}
);;

// hosts-item
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'hosts-item',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "activate", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-arrow-up\" title=\"Activate\"></i></a>\n    ");
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "deactivate", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-arrow-down\" title=\"Deactivate\"></i></a>\n    ");
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n      ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(6, program6, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "host.delete", "", options) : helperMissing.call(depth0, "link-to", "host.delete", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  return buffer;
  }
function program6(depth0,data) {
  
  
  data.buffer.push("<i class=\"fa fa-trash-o\" title=\"Delete\"></i>");
  }

function program8(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "purge", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-fire\" title=\"Purge\"></i></a>\n    ");
  return buffer;
  }

function program10(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n    ");
  stack1 = helpers.each.call(depth0, "instances", {hash:{
    'itemController': ("container")
  },hashTypes:{'itemController': "STRING"},hashContexts:{'itemController': depth0},inverse:self.program(13, program13, data),fn:self.program(11, program11, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{
    'tagName': ("div"),
    'classNames': ("add-to-host")
  },hashTypes:{'tagName': "STRING",'classNames': "STRING"},hashContexts:{'tagName': depth0,'classNames': depth0},inverse:self.noop,fn:self.program(15, program15, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "containerNew", "", options) : helperMissing.call(depth0, "link-to", "containerNew", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }
function program11(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "App.HostContainerView", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n    ");
  return buffer;
  }

function program13(depth0,data) {
  
  
  data.buffer.push("\n      <i class=\"text-muted\">No containers yet</i>\n    ");
  }

function program15(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'src': ("view.href")
  },hashTypes:{'src': "STRING"},hashContexts:{'src': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("><i class=\"fa fa-plus\"></i> Add a container</a>\n    ");
  return buffer;
  }

function program17(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n    ");
  stack1 = helpers.each.call(depth0, "instances", {hash:{
    'itemController': ("virtualMachine")
  },hashTypes:{'itemController': "STRING"},hashContexts:{'itemController': depth0},inverse:self.program(20, program20, data),fn:self.program(18, program18, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{
    'tagName': ("div"),
    'classNames': ("add-to-host")
  },hashTypes:{'tagName': "STRING",'classNames': "STRING"},hashContexts:{'tagName': depth0,'classNames': depth0},inverse:self.noop,fn:self.program(22, program22, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "virtualMachineNew", "", options) : helperMissing.call(depth0, "link-to", "virtualMachineNew", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }
function program18(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "App.HostVirtualMachineView", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n    ");
  return buffer;
  }

function program20(depth0,data) {
  
  
  data.buffer.push("\n      <i class=\"text-muted\">No VMs yet</i>\n    ");
  }

function program22(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n      <a ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'src': ("view.href")
  },hashTypes:{'src': "STRING"},hashContexts:{'src': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("><i class=\"fa fa-plus\"></i> Add a VM</a>\n    ");
  return buffer;
  }

  data.buffer.push("<label ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': ("stateColor")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n  <div class=\"host-name\">\n    <i ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":fa stateIcon"),
    'title': ("displayState")
  },hashTypes:{'class': "STRING",'title': "STRING"},hashContexts:{'class': depth0,'title': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("></i>\n    ");
  stack1 = helpers._triageMustache.call(depth0, "displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n  <div class=\"host-actions\">\n    ");
  stack1 = helpers['if'].call(depth0, "actions.activate", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  stack1 = helpers['if'].call(depth0, "actions.deactivate", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(3, program3, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  stack1 = helpers.unless.call(depth0, "isDeleted", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  stack1 = helpers['if'].call(depth0, "actions.purge", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(8, program8, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n</label>\n<div class=\"well\">\n  ");
  stack1 = helpers['if'].call(depth0, "isDocker", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(17, program17, data),fn:self.program(10, program10, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n");
  return buffer;
  
}
);;

// host-container-item
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'host-container-item',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <i class=\"fa fa-tint\"></i> ");
  stack1 = helpers._triageMustache.call(depth0, "displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "restart", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-refresh\" title=\"Restart\"></i></a>\n  ");
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "start", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-arrow-up\" title=\"Start\"></i></a>\n  ");
  return buffer;
  }

function program7(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "stop", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-arrow-down\" title=\"Stop\"></i></a>\n  ");
  return buffer;
  }

function program9(depth0,data) {
  
  
  data.buffer.push("<i class=\"fa fa-edit\" title=\"Edit\"></i>");
  }

function program11(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(12, program12, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "container.delete", "", options) : helperMissing.call(depth0, "link-to", "container.delete", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }
function program12(depth0,data) {
  
  
  data.buffer.push("<i class=\"fa fa-trash-o\" title=\"Delete\"></i>");
  }

function program14(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "purge", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-fire\" title=\"Purge\"></i></a>\n  ");
  return buffer;
  }

function program16(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n  <div class=\"progress progress-striped active\">\n    <div class=\"progress-bar\" role=\"progressbar\" aria-valuemin=\"0\" aria-valuemax=\"100\" ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'aria-valuenow': ("displayProgress"),
    'style': ("progressStyle")
  },hashTypes:{'aria-valuenow': "ID",'style': "ID"},hashContexts:{'aria-valuenow': depth0,'style': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n      <span class=\"sr-only\">");
  stack1 = helpers._triageMustache.call(depth0, "displayProgress", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("% Complete</span>\n    </div>\n  </div>\n");
  return buffer;
  }

  data.buffer.push("<div class=\"instance-name\">\n  ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "container", "", options) : helperMissing.call(depth0, "link-to", "container", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n<div class=\"instance-actions\">\n  ");
  stack1 = helpers['if'].call(depth0, "actions.restart", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(3, program3, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers['if'].call(depth0, "actions.start", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers['if'].call(depth0, "actions.stop", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(7, program7, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(9, program9, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "container.edit", "", options) : helperMissing.call(depth0, "link-to", "container.edit", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers.unless.call(depth0, "isDeleted", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(11, program11, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers['if'].call(depth0, "actions.purge", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(14, program14, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n<div>\n  <div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":instance-status stateColor")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n    <i ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":fa stateIcon")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("></i> ");
  stack1 = helpers._triageMustache.call(depth0, "displayState", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n  <div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":instance-ip isOn::text-muted")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">");
  stack1 = helpers._triageMustache.call(depth0, "primaryIpAddress", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</div>\n</div>\n<div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":force-wrap isError:text-danger:text-muted showTransitioningMessage::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n  ");
  stack1 = helpers._triageMustache.call(depth0, "transitioningMessage", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n");
  stack1 = helpers['if'].call(depth0, "isTransitioning", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(16, program16, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n");
  return buffer;
  
}
);;

// host-virtualmachine-item
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'host-virtualmachine-item',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <i class=\"fa fa-desktop\"></i> ");
  stack1 = helpers._triageMustache.call(depth0, "displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "restart", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-refresh\" title=\"Restart\"></i></a>\n  ");
  return buffer;
  }

function program5(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "start", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-arrow-up\" title=\"Start\"></i></a>\n  ");
  return buffer;
  }

function program7(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "stop", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-arrow-down\" title=\"Stop\"></i></a>\n  ");
  return buffer;
  }

function program9(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(10, program10, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "virtualMachine.console", "", options) : helperMissing.call(depth0, "link-to", "virtualMachine.console", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }
function program10(depth0,data) {
  
  
  data.buffer.push("<i class=\"fa fa-terminal\" title=\"Console\"></i>");
  }

function program12(depth0,data) {
  
  
  data.buffer.push("<i class=\"fa fa-edit\" title=\"Edit\"></i>");
  }

function program14(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n    ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(15, program15, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "virtualMachine.delete", "", options) : helperMissing.call(depth0, "link-to", "virtualMachine.delete", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }
function program15(depth0,data) {
  
  
  data.buffer.push("<i class=\"fa fa-trash-o\" title=\"Delete\"></i>");
  }

function program17(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <a ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "purge", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" href=\"#\"><i class=\"fa fa-fire\" title=\"Purge\"></i></a>\n  ");
  return buffer;
  }

function program19(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n  <div class=\"progress progress-striped active\">\n    <div class=\"progress-bar\" role=\"progressbar\" aria-valuemin=\"0\" aria-valuemax=\"100\" ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'aria-valuenow': ("displayProgress"),
    'style': ("progressStyle")
  },hashTypes:{'aria-valuenow': "ID",'style': "ID"},hashContexts:{'aria-valuenow': depth0,'style': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n      <span class=\"sr-only\">");
  stack1 = helpers._triageMustache.call(depth0, "displayProgress", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("% Complete</span>\n    </div>\n  </div>\n");
  return buffer;
  }

  data.buffer.push("<div class=\"instance-name\">\n  ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "virtualMachine", "", options) : helperMissing.call(depth0, "link-to", "virtualMachine", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n<div class=\"instance-actions\">\n  ");
  stack1 = helpers['if'].call(depth0, "actions.restart", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(3, program3, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers['if'].call(depth0, "actions.start", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers['if'].call(depth0, "actions.stop", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(7, program7, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers['if'].call(depth0, "actions.console", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(9, program9, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = (helper = helpers['link-to'] || (depth0 && depth0['link-to']),options={hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(12, program12, data),contexts:[depth0,depth0],types:["STRING","ID"],data:data},helper ? helper.call(depth0, "virtualMachine.edit", "", options) : helperMissing.call(depth0, "link-to", "virtualMachine.edit", "", options));
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers.unless.call(depth0, "isDeleted", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(14, program14, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  stack1 = helpers['if'].call(depth0, "actions.purge", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(17, program17, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n<div>\n  <div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":instance-status stateColor")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n    <i ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":fa stateIcon")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("></i> ");
  stack1 = helpers._triageMustache.call(depth0, "displayState", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n  <div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":instance-ip isOn::text-muted")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">");
  stack1 = helpers._triageMustache.call(depth0, "primaryIpAddress", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</div>\n</div>\n<div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":force-wrap isError:text-danger:text-muted showTransitioningMessage::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n  ");
  stack1 = helpers._triageMustache.call(depth0, "transitioningMessage", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n");
  stack1 = helpers['if'].call(depth0, "isTransitioning", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(19, program19, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n");
  return buffer;
  
}
);;

// host-edit
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'host-edit',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, self=this;

function program1(depth0,data) {
  
  
  data.buffer.push("Edit");
  }

function program3(depth0,data) {
  
  
  data.buffer.push("Add Host");
  }

  data.buffer.push("<h1>");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h1>\n");
  return buffer;
  
}
);;

// container-edit
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'container-edit',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); partials = this.merge(partials, Ember.Handlebars.partials); data = data || {};
  var buffer = '', stack1, helper, options, self=this, escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  
  data.buffer.push("Edit");
  }

function program3(depth0,data) {
  
  
  data.buffer.push("Add");
  }

function program5(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n  <div class=\"alert alert-danger\">\n    <i style=\"float: left;\" class=\"fa fa-exclamation-circle\"></i>\n    <p style=\"margin-left: 50px\">");
  stack1 = helpers._triageMustache.call(depth0, "error", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</p>\n  </div>\n");
  return buffer;
  }

function program7(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n      ");
  stack1 = self.invokePartial(partials['container-edit-ports'], 'container-edit-ports', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n      ");
  stack1 = self.invokePartial(partials['container-edit-links'], 'container-edit-links', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  return buffer;
  }

function program9(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n      <div class=\"form-group\">\n        <label for=\"networkId\"><i class=\"fa fa-sitemap\"></i> Network</label>\n        ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "Ember.Select", {hash:{
    'class': ("form-control"),
    'disabled': ("editing"),
    'id': ("networkId"),
    'content': ("networks"),
    'optionLabelPath': ("content.displayName"),
    'optionValuePath': ("content.id"),
    'value': ("networkId"),
    'prompt': ("Select a Network to join the Instance to...")
  },hashTypes:{'class': "STRING",'disabled': "ID",'id': "STRING",'content': "ID",'optionLabelPath': "STRING",'optionValuePath': "STRING",'value': "ID",'prompt': "STRING"},hashContexts:{'class': depth0,'disabled': depth0,'id': depth0,'content': depth0,'optionLabelPath': depth0,'optionValuePath': depth0,'value': depth0,'prompt': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n      </div>\n      ");
  stack1 = self.invokePartial(partials['container-edit-image'], 'container-edit-image', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  return buffer;
  }

function program11(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n  <div class=\"row\">\n    <div class=\"col-md-6\">\n      ");
  stack1 = self.invokePartial(partials['container-edit-ports'], 'container-edit-ports', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </div>\n\n    <div class=\"col-md-6\">\n      ");
  stack1 = self.invokePartial(partials['container-edit-links'], 'container-edit-links', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </div>\n  </div>\n\n  <div class=\"advanced-toggle\">\n    <a href=\"#\" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "showAdvanced", {hash:{
    'target': ("view")
  },hashTypes:{'target': "ID"},hashContexts:{'target': depth0},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(">Advanced options</a>\n  </div>\n  <div class=\"advanced\" style=\"display: none\">\n    <div class=\"row\">\n\n      <div class=\"col-md-6\">\n        ");
  stack1 = self.invokePartial(partials['container-edit-command'], 'container-edit-command', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n      </div>\n\n      <div class=\"col-md-6\">\n        ");
  stack1 = self.invokePartial(partials['container-edit-environment'], 'container-edit-environment', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n      </div>\n    </div>\n  </div>\n");
  return buffer;
  }

  data.buffer.push("<h1><i class=\"fa fa-tint\"></i> ");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push(" Container</h1>\n\n");
  stack1 = helpers['if'].call(depth0, "error", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n<div class=\"row\">\n  <div class=\"col-md-6\">\n    <div class=\"form-group\">\n      <label for=\"name\">Name</label>\n      ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'id': ("name"),
    'type': ("text"),
    'value': ("name"),
    'classNames': ("form-control"),
    'placeholder': ("e.g. web01")
  },hashTypes:{'id': "STRING",'type': "STRING",'value': "ID",'classNames': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'type': depth0,'value': depth0,'classNames': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n    </div>\n    <div class=\"form-group\">\n      <label for=\"description\">Description</label>\n      ");
  data.buffer.push(escapeExpression((helper = helpers.textarea || (depth0 && depth0.textarea),options={hash:{
    'id': ("description"),
    'value': ("description"),
    'classNames': ("form-control no-resize"),
    'rows': ("5"),
    'placeholder': ("e.g. It serves the webs")
  },hashTypes:{'id': "STRING",'value': "ID",'classNames': "STRING",'rows': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'value': depth0,'classNames': depth0,'rows': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "textarea", options))));
  data.buffer.push("\n    </div>\n  </div>\n\n  <div class=\"col-md-6\">\n    ");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(9, program9, data),fn:self.program(7, program7, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n</div>\n\n");
  stack1 = helpers.unless.call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(11, program11, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n");
  stack1 = self.invokePartial(partials['save-cancel'], 'save-cancel', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n");
  return buffer;
  
}
);;

// container-edit-ports
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'container-edit-ports',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    ");
  stack1 = helpers['if'].call(depth0, "port.existing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(4, program4, data),fn:self.program(2, program2, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n      <tr>\n        <td>");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'class': ("form-control"),
    'type': ("number"),
    'min': ("1"),
    'max': ("65535"),
    'value': ("port.public"),
    'placeholder': ("e.g. 80")
  },hashTypes:{'class': "STRING",'type': "STRING",'min': "STRING",'max': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'class': depth0,'type': depth0,'min': depth0,'max': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("</td>\n        <td><span class=\"form-control-static text-muted\">");
  stack1 = helpers._triageMustache.call(depth0, "port.private", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</span></td>\n        <td><span class=\"form-control-static text-muted\">");
  data.buffer.push(escapeExpression((helper = helpers.uppercase || (depth0 && depth0.uppercase),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "port.protocol", options) : helperMissing.call(depth0, "uppercase", "port.protocol", options))));
  data.buffer.push("</span></td>\n        <td>&nbsp;</td>\n      </tr>\n    ");
  return buffer;
  }

function program4(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n      <tr>\n        <td>");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'class': ("form-control"),
    'type': ("number"),
    'min': ("1"),
    'max': ("65535"),
    'value': ("port.public"),
    'placeholder': ("e.g. 80")
  },hashTypes:{'class': "STRING",'type': "STRING",'min': "STRING",'max': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'class': depth0,'type': depth0,'min': depth0,'max': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("</td>\n        <td>");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'class': ("form-control"),
    'type': ("number"),
    'min': ("1"),
    'max': ("65535"),
    'value': ("port.private"),
    'placeholder': ("e.g. 8080")
  },hashTypes:{'class': "STRING",'type': "STRING",'min': "STRING",'max': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'class': depth0,'type': depth0,'min': depth0,'max': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("</td>\n        <td>");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "Em.Select", {hash:{
    'class': ("form-control"),
    'content': ("protocolOptions"),
    'value': ("port.protocol"),
    'optionValuePath': ("content.value"),
    'optionLabelPath': ("content.label")
  },hashTypes:{'class': "STRING",'content': "ID",'value': "ID",'optionValuePath': "STRING",'optionLabelPath': "STRING"},hashContexts:{'class': depth0,'content': depth0,'value': depth0,'optionValuePath': depth0,'optionLabelPath': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("</td>\n        <td style=\"text-align: right;\"><button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "removePort", "port", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0,depth0],types:["STRING","ID"],data:data})));
  data.buffer.push(" class=\"btn btn-default\" type=\"button\" title=\"Remove this port\" tabindex=\"-1\"><i class=\"fa fa-times\"></i></button></td>\n      </tr>\n    ");
  return buffer;
  }

  data.buffer.push("<label>Ports</label>\n<table class=\"table\">\n  <tr>\n    <th>Public</th>\n    <th>Private</th>\n    <th>Protocol</th>\n    <th>&nbsp;</th>\n  </tr>\n  ");
  stack1 = helpers.each.call(depth0, "port", "in", "portsArray", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</table>\n");
  return buffer;
  
}
);;

// container-edit-environment
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'container-edit-environment',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n    <div class=\"row input-list\">\n      <div class=\"col-xs-4\">\n        ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'class': ("form-control"),
    'type': ("text"),
    'value': ("env.key"),
    'placeholder': ("e.g. SECRET")
  },hashTypes:{'class': "STRING",'type': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'class': depth0,'type': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n      </div>\n      <div class=\"col-xs-1\"><div class=\"form-group\"><p class=\"form-control-static\">=</p></div></div>\n      <div class=\"col-xs-7\">\n        <div class=\"input-group\">\n          ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'class': ("form-control"),
    'type': ("text"),
    'value': ("env.value"),
    'placeholder': ("e.g. quijibo")
  },hashTypes:{'class': "STRING",'type': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'class': depth0,'type': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n          <span class=\"input-group-btn\" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "removeEnvironment", "env", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0,depth0],types:["STRING","ID"],data:data})));
  data.buffer.push(">\n            <button class=\"btn btn-default\" type=\"button\" title=\"Remove this variable\" tabindex=\"-1\"><i class=\"fa fa-times\"></i></button>\n          </span>\n        </div>\n      </div>\n    </div>\n  ");
  return buffer;
  }

  data.buffer.push("<label>Environment Variables</label>\n<div class=\"well\">\n  <div class=\"row\">\n    <div class=\"col-xs-6\"><label>Variable Name</label></div>\n    <div class=\"col-xs-6\"><label>Value</label></div>\n  </div>\n  ");
  stack1 = helpers.each.call(depth0, "env", "in", "environmentArray", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n");
  return buffer;
  
}
);;

// container-edit-command
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'container-edit-command',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n    <div class=\"input-group input-list\">\n      ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'type': ("text"),
    'class': ("form-control"),
    'value': ("arg.value"),
    'placeholder': ("e.g. -f httpd.conf")
  },hashTypes:{'type': "STRING",'class': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'type': depth0,'class': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n      <span class=\"input-group-btn\" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "removeArg", "arg", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0,depth0],types:["STRING","ID"],data:data})));
  data.buffer.push(">\n        <button class=\"btn btn-default\" type=\"button\" title=\"Remove this argument\" tabindex=\"-1\"><i class=\"fa fa-times\"></i></button>\n      </span>\n    </div>\n  ");
  return buffer;
  }

  data.buffer.push("<label>Command</label>\n<div class=\"well\">\n  <div class=\"form-group\">\n    ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'class': ("form-control"),
    'type': ("text"),
    'name': ("command"),
    'value': ("command"),
    'placeholder': ("e.g. /usr/sbin/httpd")
  },hashTypes:{'class': "STRING",'type': "STRING",'name': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'class': depth0,'type': depth0,'name': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n  </div>\n  <label>Arguments</label>\n  ");
  stack1 = helpers.each.call(depth0, "arg", "in", "argsArray", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n");
  return buffer;
  
}
);;

// container-edit-image
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'container-edit-image',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;


  data.buffer.push("<div class=\"form-group\">\n  <label>Source Image</label>\n  <div class=\"input-group\">\n    <span class=\"input-group-addon\">docker:</span>\n    ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'disabled': ("isRancher"),
    'type': ("text"),
    'class': ("form-control"),
    'value': ("userImageUuid"),
    'placeholder': ("e.g. stackbrew/ubuntu:14.04")
  },hashTypes:{'disabled': "ID",'type': "STRING",'class': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'disabled': depth0,'type': depth0,'class': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n  </div>\n</div>\n");
  return buffer;
  
}
);;

// container-edit-links
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'container-edit-links',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <tr>\n      <td>\n        ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "App.DisplayNameSelect", {hash:{
    'classNames': ("form-control"),
    'prompt': ("Select a container..."),
    'value': ("link.targetInstanceId"),
    'content': ("containers"),
    'optionValuePath': ("content.id"),
    'optionLabelPath': ("content.name"),
    'optionViewBinding': ("App.DisplayNameSelectOption")
  },hashTypes:{'classNames': "STRING",'prompt': "STRING",'value': "ID",'content': "ID",'optionValuePath': "STRING",'optionLabelPath': "STRING",'optionViewBinding': "STRING"},hashContexts:{'classNames': depth0,'prompt': depth0,'value': depth0,'content': depth0,'optionValuePath': depth0,'optionLabelPath': depth0,'optionViewBinding': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n      </td>\n      <td style=\"text-align: center\">\n        <div class=\"form-group\">\n          <p class=\"form-control-static\"><i class=\"fa fa-arrow-right\"></i></p>\n        </div>\n      </td>\n      <td>\n        <div class=\"input-group\">\n          ");
  stack1 = helpers['if'].call(depth0, "link.existing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(4, program4, data),fn:self.program(2, program2, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n        </div>\n      </td>\n    </tr>\n  ");
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n            <p class=\"form-control-static text-muted\">");
  stack1 = helpers._triageMustache.call(depth0, "link.linkName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</p>\n          ");
  return buffer;
  }

function program4(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n            ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'class': ("form-control"),
    'type': ("text"),
    'value': ("link.linkName"),
    'placeholder': ("e.g. database")
  },hashTypes:{'class': "STRING",'type': "STRING",'value': "ID",'placeholder': "STRING"},hashContexts:{'class': depth0,'type': depth0,'value': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n            <span class=\"input-group-btn\" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "removeLink", "link", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0,depth0],types:["STRING","ID"],data:data})));
  data.buffer.push(">\n              <button class=\"btn btn-default\" type=\"button\" title=\"Remove this link\" tabindex=\"-1\"><i class=\"fa fa-times\"></i></button>\n            </span>\n          ");
  return buffer;
  }

function program6(depth0,data) {
  
  
  data.buffer.push("\n    <tr>\n      <td colspan=\"3\">\n        <i>None</i>\n      </td>\n    </tr>\n  ");
  }

  data.buffer.push("<label>Links</label>\n<table class=\"table\">\n  <tr>\n    <th width=\"45%\">Destination Container</th>\n    <th>&nbsp;</th>\n    <th width=\"45%\">As Name</th>\n  </tr>\n  ");
  stack1 = helpers.each.call(depth0, "link", "in", "linksArray", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(6, program6, data),fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</table>\n");
  return buffer;
  
}
);;

// virtualmachine
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'virtualmachine',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression;


  data.buffer.push("<h1><i ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":fa stateIcon")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("> Virtual Machine: <span class=\"text-muted\">");
  stack1 = helpers._triageMustache.call(depth0, "name", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</span></h1>\n");
  return buffer;
  
}
);;

// virtualmachine-edit
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'virtualmachine-edit',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); partials = this.merge(partials, Ember.Handlebars.partials); data = data || {};
  var buffer = '', stack1, helper, options, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression, self=this;

function program1(depth0,data) {
  
  
  data.buffer.push("Edit");
  }

function program3(depth0,data) {
  
  
  data.buffer.push("Add");
  }

function program5(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n  <div class=\"alert alert-danger\">\n    <i style=\"float: left;\" class=\"fa fa-exclamation-circle\"></i>\n    <p style=\"margin-left: 50px\">");
  stack1 = helpers._triageMustache.call(depth0, "error", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</p>\n  </div>\n");
  return buffer;
  }

function program7(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n        ");
  stack1 = helpers.each.call(depth0, "img", "in", "images", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(8, program8, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    ");
  return buffer;
  }
function program8(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n          <label>\n            ");
  data.buffer.push(escapeExpression((helper = helpers['radio-button'] || (depth0 && depth0['radio-button']),options={hash:{
    'name': ("image"),
    'value': ("img.id"),
    'selection': ("ctl.imageId"),
    'disabled': ("ctl.editing")
  },hashTypes:{'name': "STRING",'value': "ID",'selection': "ID",'disabled': "ID"},hashContexts:{'name': depth0,'value': depth0,'selection': depth0,'disabled': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "radio-button", options))));
  data.buffer.push(" ");
  stack1 = helpers._triageMustache.call(depth0, "img.displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n          </label>\n        ");
  return buffer;
  }

function program10(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n        ");
  stack1 = helpers.each.call(depth0, "key", "in", "sshkeyObjs", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(11, program11, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n      ");
  return buffer;
  }
function program11(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n          <label>\n            ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'type': ("checkbox"),
    'checked': ("key.selected"),
    'disabled': ("controller.editing")
  },hashTypes:{'type': "STRING",'checked': "ID",'disabled': "ID"},hashContexts:{'type': depth0,'checked': depth0,'disabled': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push(" ");
  stack1 = helpers._triageMustache.call(depth0, "key.displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n          </label>\n        ");
  return buffer;
  }

  data.buffer.push("<h1><i class=\"fa fa-desktop\"></i> ");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push(" Virtual Machine</h1>\n\n");
  stack1 = helpers['if'].call(depth0, "error", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n<div class=\"row\">\n  <div class=\"col-md-4\">\n    <div class=\"form-group\">\n      <label for=\"name\">Name</label>\n      ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'id': ("name"),
    'type': ("text"),
    'value': ("name"),
    'classNames': ("form-control"),
    'placeholder': ("e.g. web01")
  },hashTypes:{'id': "STRING",'type': "STRING",'value': "ID",'classNames': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'type': depth0,'value': depth0,'classNames': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n    </div>\n  </div>\n  <div class=\"col-md-2\">\n    <div class=\"form-group\">\n      <label for=\"memoryMb\"><i class=\"fa fa-signal\"></i> Memory:</label>\n      ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "Ember.Select", {hash:{
    'class': ("form-control"),
    'disabled': ("editing"),
    'id': ("memoryMb"),
    'content': ("memoryChoices"),
    'optionLabelPath': ("content.label"),
    'optionValuePath': ("content.size"),
    'value': ("memoryMb"),
    'prompt': ("Select a VM size...")
  },hashTypes:{'class': "STRING",'disabled': "ID",'id': "STRING",'content': "ID",'optionLabelPath': "STRING",'optionValuePath': "STRING",'value': "ID",'prompt': "STRING"},hashContexts:{'class': depth0,'disabled': depth0,'id': depth0,'content': depth0,'optionLabelPath': depth0,'optionValuePath': depth0,'value': depth0,'prompt': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n    </div>\n  </div>\n  <div class=\"col-md-3\">\n    <div class=\"form-group\">\n      <label for=\"networkId\"><i class=\"fa fa-sitemap\"></i> Network</label>\n      ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "Ember.Select", {hash:{
    'class': ("form-control"),
    'disabled': ("editing"),
    'id': ("networkId"),
    'content': ("networks"),
    'optionLabelPath': ("content.displayName"),
    'optionValuePath': ("content.id"),
    'value': ("networkId"),
    'prompt': ("Select a Network to join the VM to...")
  },hashTypes:{'class': "STRING",'disabled': "ID",'id': "STRING",'content': "ID",'optionLabelPath': "STRING",'optionValuePath': "STRING",'value': "ID",'prompt': "STRING"},hashContexts:{'class': depth0,'disabled': depth0,'id': depth0,'content': depth0,'optionLabelPath': depth0,'optionValuePath': depth0,'value': depth0,'prompt': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n    </div>\n  </div>\n  <div class=\"col-md-3\">\n    <div class=\"form-group\">\n      <label for=\"publicIpAddressPoolId\"><i class=\"fa fa-life-ring\"></i> Public IP Pool</label>\n      ");
  data.buffer.push(escapeExpression(helpers.view.call(depth0, "Ember.Select", {hash:{
    'class': ("form-control"),
    'disabled': ("editing"),
    'id': ("publicIpAddressPoolId"),
    'content': ("publicIppools"),
    'optionLabelPath': ("content.displayName"),
    'optionValuePath': ("content.id"),
    'value': ("publicIpAddressPoolId"),
    'prompt': ("Select an IP Address Pool...")
  },hashTypes:{'class': "STRING",'disabled': "ID",'id': "STRING",'content': "ID",'optionLabelPath': "STRING",'optionValuePath': "STRING",'value': "ID",'prompt': "STRING"},hashContexts:{'class': depth0,'disabled': depth0,'id': depth0,'content': depth0,'optionLabelPath': depth0,'optionValuePath': depth0,'value': depth0,'prompt': depth0},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\n    </div>\n  </div>\n</div>\n\n<div class=\"row\">\n  <div class=\"col-md-6\">\n    <div class=\"form-group\">\n      <label for=\"description\">Description</label>\n      ");
  data.buffer.push(escapeExpression((helper = helpers.textarea || (depth0 && depth0.textarea),options={hash:{
    'id': ("description"),
    'value': ("description"),
    'classNames': ("form-control no-resize"),
    'rows': ("5"),
    'placeholder': ("e.g. It serves the webs")
  },hashTypes:{'id': "STRING",'value': "ID",'classNames': "STRING",'rows': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'value': depth0,'classNames': depth0,'rows': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "textarea", options))));
  data.buffer.push("\n    </div>\n\n    <label><i class=\"fa fa-image\"></i> Image</label>\n    <div class=\"well radio-well\" style=\"max-height: 150px; overflow-y: auto\">\n      ");
  stack1 = helpers['with'].call(depth0, "controller", "as", "ctl", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(7, program7, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </div>\n  </div>\n\n  <div class=\"col-md-6\">\n    <label><i class=\"fa fa-key\"></i> SSH Keys</label>\n    <div class=\"well radio-well\">\n     ");
  stack1 = helpers['with'].call(depth0, "controller", "as", "controller", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(10, program10, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </div>\n\n    <label for=\"userdata\"><i class=\"fa fa-file-code-o\"></i> User Data</label>\n    ");
  data.buffer.push(escapeExpression((helper = helpers.textarea || (depth0 && depth0.textarea),options={hash:{
    'id': ("userdata"),
    'value': ("userdata"),
    'classNames': ("form-control no-resize"),
    'rows': ("5"),
    'placeholder': ("User data will be injected into the VM"),
    'disabled': ("editing")
  },hashTypes:{'id': "STRING",'value': "ID",'classNames': "STRING",'rows': "STRING",'placeholder': "STRING",'disabled': "ID"},hashContexts:{'id': depth0,'value': depth0,'classNames': depth0,'rows': depth0,'placeholder': depth0,'disabled': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "textarea", options))));
  data.buffer.push("\n  </div>\n</div>\n\n");
  stack1 = self.invokePartial(partials['save-cancel'], 'save-cancel', depth0, helpers, partials, data);
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n");
  return buffer;
  
}
);;

// virtualmachine-console
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'virtualmachine-console',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression;


  data.buffer.push("<h1><i class=\"fa fa-desktop\"></i> Console: ");
  stack1 = helpers._triageMustache.call(depth0, "virtualMachine.name", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h1>\n<div id=\"noVNC_screen\">\n  <canvas class=\"console-canvas\" width=\"640\" height=\"20\">\n    Canvas not supported.\n  </canvas>\n</div>\n<div class=\"console-status text-muted\">Loading...</div>\n<div class=\"footer-actions\">\n  <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "ctrlAltDelete", {hash:{
    'target': ("view")
  },hashTypes:{'target': "STRING"},hashContexts:{'target': depth0},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-default\" ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'disabled': ("ctrlAltDeleteDisabled")
  },hashTypes:{'disabled': "ID"},hashContexts:{'disabled': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">Send Ctrl-Alt-Delete</button>\n  <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "cancel", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-primary\">Close</button>\n</div>\n");
  return buffer;
  
}
);;

// apikeys
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'apikeys',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n        <tr>\n          <td ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': ("stateColor")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n            <i ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":fa key.stateIcon")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("></i> ");
  stack1 = helpers._triageMustache.call(depth0, "key.displayState", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n          </td>\n          <td>\n            ");
  stack1 = helpers['if'].call(depth0, "key.name", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(4, program4, data),fn:self.program(2, program2, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n            <p class=\"text-muted\">");
  stack1 = helpers['if'].call(depth0, "key.description", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(8, program8, data),fn:self.program(6, program6, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</p>\n\n          </td>\n          <td>\n            <div class=\"text-muted\">");
  data.buffer.push(escapeExpression((helper = helpers.momentCalendar || (depth0 && depth0.momentCalendar),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "key.created", options) : helperMissing.call(depth0, "momentCalendar", "key.created", options))));
  data.buffer.push("</div>\n            <div>");
  stack1 = helpers['if'].call(depth0, "key.publicValue", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(12, program12, data),fn:self.program(10, program10, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</div>\n          </td>\n          <td align=\"right\">\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.canEdit::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "edit", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Edit\"><i class=\"fa fa-edit\"></i></button>\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.actions.activate::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "activate", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Activate\"><i class=\"fa fa-play\"></i></button>\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.actions.deactivate::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "deactivate", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Deactivate\"><i class=\"fa fa-pause\"></i></button>\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.canDelete::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "delete", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Delete\"><i class=\"fa fa-trash-o\"></i></button>\n          </td>\n        </tr>\n      ");
  return buffer;
  }
function program2(depth0,data) {
  
  var stack1;
  stack1 = helpers._triageMustache.call(depth0, "key.displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  else { data.buffer.push(''); }
  }

function program4(depth0,data) {
  
  
  data.buffer.push("<span class=\"text-muted text-italic\">No name</span>");
  }

function program6(depth0,data) {
  
  var stack1;
  stack1 = helpers._triageMustache.call(depth0, "key.description", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  else { data.buffer.push(''); }
  }

function program8(depth0,data) {
  
  
  data.buffer.push("<span class=\"text-italic\">No description</span>");
  }

function program10(depth0,data) {
  
  var stack1;
  stack1 = helpers._triageMustache.call(depth0, "key.publicValue", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  else { data.buffer.push(''); }
  }

function program12(depth0,data) {
  
  
  data.buffer.push("<span class=\"text-muted text-italic\">No public value</span>");
  }

  data.buffer.push("<section>\n  <div class=\"row\">\n    <div class=\"col-md-6\">\n      <label>Endpoint</label>\n      <div class=\"well\">\n        <a href=\"");
  data.buffer.push(escapeExpression(helpers.unbound.call(depth0, "view.config.apiEndpoint", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("\" target=\"_blank\">");
  data.buffer.push(escapeExpression(helpers.unbound.call(depth0, "view.config.apiEndpoint", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data})));
  data.buffer.push("</a>\n      </div>\n    </div>\n\n    <div class=\"col-md-6\">\n      <label>Documentation</label>\n      <div class=\"well\">\n        <span class=\"text-muted\">Coming Soon&trade;</span>\n      </div>\n    </div>\n  </div>\n\n  <div>\n    <div class=\"clear-fix\">\n      <label><i class=\"fa fa-code\"></i> API Keys</label>\n      <div style=\"float: right\">\n        <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "newApikey", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-primary\"><i class=\"fa fa-plus\"></i> Create an API key</button>\n      </tr>\n      </div>\n    </div>\n    <table class=\"table fixed\" style=\"margin-bottom: 0\">\n      <tr>\n        <th width=\"120\">State</th>\n        <th>Name<br/><span class=\"text-muted\">Description</span></th>\n        <th>Created<br/><span class=\"text-muted\">Username</span></th>\n        <th width=\"140\">&nbsp;</th>\n      </tr>\n      ");
  stack1 = helpers.each.call(depth0, "key", "in", "dataSource", {hash:{
    'itemController': ("apikey")
  },hashTypes:{'itemController': "STRING"},hashContexts:{'itemController': depth0},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </table>\n  </div>\n</section>\n");
  return buffer;
  
}
);;

// apikey-edit
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'apikey-edit',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  
  data.buffer.push("Edit API Key");
  }

function program3(depth0,data) {
  
  
  data.buffer.push("API Key Created");
  }

function program5(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <p class=\"form-control-static text-muted text-italic\">");
  stack1 = helpers._triageMustache.call(depth0, "publicValue", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</p>\n  ");
  return buffer;
  }

function program7(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <h3 class=\"form-control-static\">");
  stack1 = helpers._triageMustache.call(depth0, "publicValue", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h3>\n  ");
  return buffer;
  }

function program9(depth0,data) {
  
  
  data.buffer.push("\n    <p class=\"form-control-static text-muted text-italic\">Not available.</p>\n  ");
  }

function program11(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <h3 class=\"form-control-static\">");
  stack1 = helpers._triageMustache.call(depth0, "secretValue", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h3>\n  ");
  return buffer;
  }

function program13(depth0,data) {
  
  
  data.buffer.push("\n  <div class=\"alert alert-warning\">\n    <i style=\"float: left;\" class=\"fa fa-exclamation-circle fa-3x\"></i>\n    <p style=\"margin-left: 50px\">Save the username and password above!  This is the only time you'll be able to see the password.</p>\n    <p style=\"margin-left: 50px\">If you lose it, you'll have to create a new API key.</p>\n  </div>\n  <p>If you like, you can give this key an optional name &amp; description to help keep yourself organized:</p>\n");
  }

function program15(depth0,data) {
  
  
  data.buffer.push("\n    <button class=\"btn btn-primary btn-disabled\"><i class=\"fa fa-spinner fa-spin\"></i> Saving...</button>\n  ");
  }

function program17(depth0,data) {
  
  var buffer = '';
  data.buffer.push("\n    <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "save", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-primary\">Save</button>\n  ");
  return buffer;
  }

  data.buffer.push("<h1>");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</h1>\n\n<div class=\"form-group\">\n  <label>Username</label>\n  ");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(7, program7, data),fn:self.program(5, program5, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n\n<div class=\"form-group\">\n  <label>Password</label>\n  ");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(11, program11, data),fn:self.program(9, program9, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</div>\n\n");
  stack1 = helpers.unless.call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(13, program13, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n\n<div class=\"form-group\">\n  <label for=\"name\">Name</label>\n  ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'id': ("name"),
    'type': ("text"),
    'value': ("name"),
    'classNames': ("form-control"),
    'placeholder': ("e.g. App servers")
  },hashTypes:{'id': "STRING",'type': "STRING",'value': "ID",'classNames': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'type': depth0,'value': depth0,'classNames': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n</div>\n\n<div class=\"form-group\">\n  <label for=\"description\">Description</label>\n  ");
  data.buffer.push(escapeExpression((helper = helpers.textarea || (depth0 && depth0.textarea),options={hash:{
    'id': ("description"),
    'value': ("description"),
    'classNames': ("form-control no-resize"),
    'rows': ("5"),
    'placeholder': ("e.g. This key is used by the app servers to deploy containers")
  },hashTypes:{'id': "STRING",'value': "ID",'classNames': "STRING",'rows': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'value': depth0,'classNames': depth0,'rows': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "textarea", options))));
  data.buffer.push("\n</div>\n\n<div class=\"footer-actions\">\n  ");
  stack1 = helpers['if'].call(depth0, "saving", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(17, program17, data),fn:self.program(15, program15, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "cancel", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-link\">Close</button>\n</div>\n");
  return buffer;
  
}
);;

// sshkeys
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'sshkeys',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = '', stack1, helper, options;
  data.buffer.push("\n        <tr class=\"info-row\">\n          <td ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': ("stateColor")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n            <i ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":fa key.stateIcon")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push("></i> ");
  stack1 = helpers._triageMustache.call(depth0, "key.displayState", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n          </td>\n          <td>\n            ");
  stack1 = helpers['if'].call(depth0, "key.name", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(4, program4, data),fn:self.program(2, program2, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n            <p class=\"text-muted\">");
  stack1 = helpers['if'].call(depth0, "key.description", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(8, program8, data),fn:self.program(6, program6, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</p>\n          </td>\n          <td>\n            <div>");
  data.buffer.push(escapeExpression((helper = helpers.momentCalendar || (depth0 && depth0.momentCalendar),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "key.created", options) : helperMissing.call(depth0, "momentCalendar", "key.created", options))));
  data.buffer.push("</div>\n            <p class=\"text-muted\">");
  data.buffer.push(escapeExpression((helper = helpers.uppercase || (depth0 && depth0.uppercase),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "algorithm", options) : helperMissing.call(depth0, "uppercase", "algorithm", options))));
  data.buffer.push("</p>\n          </td>\n          <td align=\"right\">\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.actions.activate::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "activate", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Activate\"><i class=\"fa fa-play\"></i></button>\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.actions.deactivate::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "deactivate", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Deactivate\"><i class=\"fa fa-pause\"></i></button>\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.links.pem::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "downloadPem", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Download\"><i class=\"fa fa-download\"></i></button>\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.canEdit::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "edit", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Edit\"><i class=\"fa fa-edit\"></i></button>\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.canDelete::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "delete", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Delete\"><i class=\"fa fa-trash-o\"></i></button>\n          </td>\n        </tr>\n        <tr class=\"key-row\">\n          <td colspan=\"4\">\n            <div class=\"text-muted force-wrap\">");
  stack1 = helpers['if'].call(depth0, "key.publicValue", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(12, program12, data),fn:self.program(10, program10, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</div>\n          </td>\n        </tr>\n      ");
  return buffer;
  }
function program2(depth0,data) {
  
  var stack1;
  stack1 = helpers._triageMustache.call(depth0, "key.displayName", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  else { data.buffer.push(''); }
  }

function program4(depth0,data) {
  
  
  data.buffer.push("<span class=\"text-muted text-italic\">No name</span>");
  }

function program6(depth0,data) {
  
  var stack1;
  stack1 = helpers._triageMustache.call(depth0, "key.description", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  else { data.buffer.push(''); }
  }

function program8(depth0,data) {
  
  
  data.buffer.push("<span class=\"text-italic\">No description</span>");
  }

function program10(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("<small class=\"text-mono\">");
  stack1 = helpers._triageMustache.call(depth0, "key.unbreakablePublicValue", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</small>");
  return buffer;
  }

function program12(depth0,data) {
  
  
  data.buffer.push("<span class=\"text-muted text-italic\">No public key</span>");
  }

  data.buffer.push("<section>\n  <div>\n    <div class=\"clear-fix\">\n      <label><i class=\"fa fa-key\"></i> SSH Keys</label>\n      <div style=\"float: right\">\n        <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "newSshkey", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-primary\"><i class=\"fa fa-plus\"></i> Create a SSH key pair</button>\n        <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "importPublicKey", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-primary\"><i class=\"fa fa-upload\"></i> Import a public key</button>\n      </div>\n    </div>\n    <table class=\"table fixed\" style=\"margin-bottom: 0\">\n      <tr>\n        <th width=\"120\">State</th>\n        <th width=\"300\">Name<br/><span class=\"text-muted\">Description</span></th>\n        <th>Created<br/><span class=\"text-muted\">Algorithm</span></th>\n        <th width=\"170\">&nbsp;</th>\n      </tr>\n      ");
  stack1 = helpers.each.call(depth0, "key", "in", "dataSource", {hash:{
    'itemController': ("sshkey")
  },hashTypes:{'itemController': "STRING"},hashContexts:{'itemController': depth0},inverse:self.noop,fn:self.program(1, program1, data),contexts:[depth0,depth0,depth0],types:["ID","ID","ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </table>\n  </div>\n</section>\n");
  return buffer;
  
}
);;

// sshkey-edit
(function(handlebars, container, name, compiled) {
  container = [].concat(container);
  var tpl = handlebars.template(compiled,{});
  for ( var i = 0 ; i < container.length ; i++ ) {
    container[i][name] = tpl;
  }
  handlebars.registerPartial(name, tpl);
})(
  Ember.Handlebars,
  Ember.TEMPLATES,
  'sshkey-edit',
  function anonymous(Handlebars,depth0,helpers,partials,data) {
this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Ember.Handlebars.helpers); data = data || {};
  var buffer = '', stack1, helper, options, self=this, helperMissing=helpers.helperMissing, escapeExpression=this.escapeExpression;

function program1(depth0,data) {
  
  
  data.buffer.push("\n    Import Public SSH Key\n  ");
  }

function program3(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    ");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(6, program6, data),fn:self.program(4, program4, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  ");
  return buffer;
  }
function program4(depth0,data) {
  
  
  data.buffer.push("\n      Edit SSH Key\n    ");
  }

function program6(depth0,data) {
  
  
  data.buffer.push("\n      SSH Key Created\n    ");
  }

function program8(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n        ");
  data.buffer.push(escapeExpression((helper = helpers.textarea || (depth0 && depth0.textarea),options={hash:{
    'id': ("publicValue"),
    'value': ("publicValue"),
    'classNames': ("form-control no-resize text-mono"),
    'rows': ("7"),
    'placeholder': ("Public Key value, e.g. ssh-rsa AAAAB3NzaC1yc2...xi/r7 you@yourhost")
  },hashTypes:{'id': "STRING",'value': "ID",'classNames': "STRING",'rows': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'value': depth0,'classNames': depth0,'rows': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "textarea", options))));
  data.buffer.push("\n      ");
  return buffer;
  }

function program10(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n        <div class=\"form-control-static text-mono force-wrap\"><small>");
  stack1 = helpers._triageMustache.call(depth0, "publicValue", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</small></div>\n      ");
  return buffer;
  }

function program12(depth0,data) {
  
  
  data.buffer.push("\n        <button class=\"btn btn-primary btn-disabled\"><i class=\"fa fa-spinner fa-spin\"></i> Saving...</button>\n      ");
  }

function program14(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n        <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "save", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-primary\">");
  stack1 = helpers['if'].call(depth0, "import", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(17, program17, data),fn:self.program(15, program15, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</button>\n      ");
  return buffer;
  }
function program15(depth0,data) {
  
  
  data.buffer.push("Import");
  }

function program17(depth0,data) {
  
  
  data.buffer.push("Save");
  }

function program19(depth0,data) {
  
  
  data.buffer.push("Cancel");
  }

function program21(depth0,data) {
  
  
  data.buffer.push("Close");
  }

function program23(depth0,data) {
  
  var buffer = '', stack1;
  data.buffer.push("\n    <div class=\"col-md-6\">\n      <div class=\"form-group\">\n        <div class=\"clear-fix\">\n          <label>Secret Key</label>\n          <div style=\"float: right\">\n            <button ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': (":btn :btn-sm :btn-info key.links.pem::hide")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(" ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "downloadPem", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" title=\"Download\"><i class=\"fa fa-download\"></i></button>\n          </div>\n        </div>\n        ");
  stack1 = helpers['if'].call(depth0, "editing", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(26, program26, data),fn:self.program(24, program24, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n      </div>\n    ");
  return buffer;
  }
function program24(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n          ");
  data.buffer.push(escapeExpression((helper = helpers.textarea || (depth0 && depth0.textarea),options={hash:{
    'id': ("secretValue"),
    'value': ("secretValue"),
    'classNames': ("form-control no-resize text-mono"),
    'rows': ("30"),
    'placeholder': ("Secret Key value, starting with ----BEGIN RSA PRIVATE KEY-----")
  },hashTypes:{'id': "STRING",'value': "ID",'classNames': "STRING",'rows': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'value': depth0,'classNames': depth0,'rows': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "textarea", options))));
  data.buffer.push("\n        ");
  return buffer;
  }

function program26(depth0,data) {
  
  var buffer = '', helper, options;
  data.buffer.push("\n          <div class=\"form-control-static text-mono force-wrap\"><small>");
  data.buffer.push(escapeExpression((helper = helpers.nl2br || (depth0 && depth0.nl2br),options={hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["ID"],data:data},helper ? helper.call(depth0, "secretValue", options) : helperMissing.call(depth0, "nl2br", "secretValue", options))));
  data.buffer.push("</small></div>\n        ");
  return buffer;
  }

  data.buffer.push("<h1>\n  <i class=\"fa fa-key\"></i> \n  ");
  stack1 = helpers['if'].call(depth0, "import", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(3, program3, data),fn:self.program(1, program1, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n</h1>\n\n<div class=\"row\">\n  <div ");
  data.buffer.push(escapeExpression(helpers['bind-attr'].call(depth0, {hash:{
    'class': ("import:col-md-12:col-md-6")
  },hashTypes:{'class': "STRING"},hashContexts:{'class': depth0},contexts:[],types:[],data:data})));
  data.buffer.push(">\n    <div class=\"form-group\">\n      <label for=\"publicValue\">Public Key</label>\n      ");
  stack1 = helpers['if'].call(depth0, "view.editPublicKey", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(10, program10, data),fn:self.program(8, program8, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n    </div>\n\n    <p>If you like, you can give this key a name &amp; description to help keep yourself organized:</p>\n    <div class=\"form-group\">\n      <label for=\"name\">Name</label>\n      ");
  data.buffer.push(escapeExpression((helper = helpers.input || (depth0 && depth0.input),options={hash:{
    'id': ("name"),
    'type': ("text"),
    'value': ("name"),
    'classNames': ("form-control"),
    'placeholder': ("e.g. App servers")
  },hashTypes:{'id': "STRING",'type': "STRING",'value': "ID",'classNames': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'type': depth0,'value': depth0,'classNames': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "input", options))));
  data.buffer.push("\n    </div>\n\n    <div class=\"form-group\">\n      <label for=\"description\">Description</label>\n      ");
  data.buffer.push(escapeExpression((helper = helpers.textarea || (depth0 && depth0.textarea),options={hash:{
    'id': ("description"),
    'value': ("description"),
    'classNames': ("form-control no-resize"),
    'rows': ("5"),
    'placeholder': ("e.g. My SSH key")
  },hashTypes:{'id': "STRING",'value': "ID",'classNames': "STRING",'rows': "STRING",'placeholder': "STRING"},hashContexts:{'id': depth0,'value': depth0,'classNames': depth0,'rows': depth0,'placeholder': depth0},contexts:[],types:[],data:data},helper ? helper.call(depth0, options) : helperMissing.call(depth0, "textarea", options))));
  data.buffer.push("\n    </div>\n\n    <div class=\"footer-actions\">\n      ");
  stack1 = helpers['if'].call(depth0, "saving", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(14, program14, data),fn:self.program(12, program12, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n      <button ");
  data.buffer.push(escapeExpression(helpers.action.call(depth0, "cancel", {hash:{},hashTypes:{},hashContexts:{},contexts:[depth0],types:["STRING"],data:data})));
  data.buffer.push(" class=\"btn btn-link\">");
  stack1 = helpers['if'].call(depth0, "import", {hash:{},hashTypes:{},hashContexts:{},inverse:self.program(21, program21, data),fn:self.program(19, program19, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("</button>\n    </div>\n  </div>\n\n  ");
  stack1 = helpers.unless.call(depth0, "import", {hash:{},hashTypes:{},hashContexts:{},inverse:self.noop,fn:self.program(23, program23, data),contexts:[depth0],types:["ID"],data:data});
  if(stack1 || stack1 === 0) { data.buffer.push(stack1); }
  data.buffer.push("\n  </div>\n</div>\n");
  return buffer;
  
}
);;

