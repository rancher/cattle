// NoVNC
var NoVNC = (function(window) {
  var Websock_native;

  // util
  /*
  * noVNC: HTML5 VNC client
  * Copyright (C) 2012 Joel Martin
  * Licensed under MPL 2.0 (see LICENSE.txt)
  *
  * See README.md for usage and integration instructions.
  */

  "use strict";
  /*jslint bitwise: false, white: false */
  /*global window, console, document, navigator, ActiveXObject */

  // Globals defined here
  var Util = {};


  /*
  * Make arrays quack
  */

  Array.prototype.push8 = function (num) {
      this.push(num & 0xFF);
  };

  Array.prototype.push16 = function (num) {
      this.push((num >> 8) & 0xFF,
                (num     ) & 0xFF  );
  };
  Array.prototype.push32 = function (num) {
      this.push((num >> 24) & 0xFF,
                (num >> 16) & 0xFF,
                (num >>  8) & 0xFF,
                (num      ) & 0xFF  );
  };

  // IE does not support map (even in IE9)
  //This prototype is provided by the Mozilla foundation and
  //is distributed under the MIT license.
  //http://www.ibiblio.org/pub/Linux/LICENSES/mit.license
  if (!Array.prototype.map)
  {
    Array.prototype.map = function(fun /*, thisp*/)
    {
      var len = this.length;
      if (typeof fun != "function")
        throw new TypeError();

      var res = new Array(len);
      var thisp = arguments[1];
      for (var i = 0; i < len; i++)
      {
        if (i in this)
          res[i] = fun.call(thisp, this[i], i, this);
      }

      return res;
    };
  }

  // IE <9 does not support indexOf
  //This prototype is provided by the Mozilla foundation and
  //is distributed under the MIT license.
  //http://www.ibiblio.org/pub/Linux/LICENSES/mit.license
  if (!Array.prototype.indexOf)
  {
    Array.prototype.indexOf = function(elt /*, from*/)
    {
      var len = this.length >>> 0;

      var from = Number(arguments[1]) || 0;
      from = (from < 0)
          ? Math.ceil(from)
          : Math.floor(from);
      if (from < 0)
        from += len;

      for (; from < len; from++)
      {
        if (from in this &&
            this[from] === elt)
          return from;
      }
      return -1;
    };
  }


  // 
  // requestAnimationFrame shim with setTimeout fallback
  //

  window.requestAnimFrame = (function(){
      return  window.requestAnimationFrame       || 
              window.webkitRequestAnimationFrame || 
              window.mozRequestAnimationFrame    || 
              window.oRequestAnimationFrame      || 
              window.msRequestAnimationFrame     || 
              function(callback){
                  window.setTimeout(callback, 1000 / 60);
              };
  })();

  /* 
  * ------------------------------------------------------
  * Namespaced in Util
  * ------------------------------------------------------
  */

  /*
  * Logging/debug routines
  */

  Util._log_level = 'warn';
  Util.init_logging = function (level) {
      if (typeof level === 'undefined') {
          level = Util._log_level;
      } else {
          Util._log_level = level;
      }
      if (typeof window.console === "undefined") {
          if (typeof window.opera !== "undefined") {
              window.console = {
                  'log'  : window.opera.postError,
                  'warn' : window.opera.postError,
                  'error': window.opera.postError };
          } else {
              window.console = {
                  'log'  : function(m) {},
                  'warn' : function(m) {},
                  'error': function(m) {}};
          }
      }

      Util.Debug = Util.Info = Util.Warn = Util.Error = function (msg) {};
      switch (level) {
          case 'debug': Util.Debug = function (msg) { console.log(msg); };
          case 'info':  Util.Info  = function (msg) { console.log(msg); };
          case 'warn':  Util.Warn  = function (msg) { console.warn(msg); };
          case 'error': Util.Error = function (msg) { console.error(msg); };
          case 'none':
              break;
          default:
              throw("invalid logging type '" + level + "'");
      }
  };
  Util.get_logging = function () {
      return Util._log_level;
  };
  // Initialize logging level
  Util.init_logging();


  // Set configuration default for Crockford style function namespaces
  Util.conf_default = function(cfg, api, defaults, v, mode, type, defval, desc) {
      var getter, setter;

      // Default getter function
      getter = function (idx) {
          if ((type in {'arr':1, 'array':1}) &&
              (typeof idx !== 'undefined')) {
              return cfg[v][idx];
          } else {
              return cfg[v];
          }
      };

      // Default setter function
      setter = function (val, idx) {
          if (type in {'boolean':1, 'bool':1}) {
              if ((!val) || (val in {'0':1, 'no':1, 'false':1})) {
                  val = false;
              } else {
                  val = true;
              }
          } else if (type in {'integer':1, 'int':1}) {
              val = parseInt(val, 10);
          } else if (type === 'str') {
              val = String(val);
          } else if (type === 'func') {
              if (!val) {
                  val = function () {};
              }
          }
          if (typeof idx !== 'undefined') {
              cfg[v][idx] = val;
          } else {
              cfg[v] = val;
          }
      };

      // Set the description
      api[v + '_description'] = desc;

      // Set the getter function
      if (typeof api['get_' + v] === 'undefined') {
          api['get_' + v] = getter;
      }

      // Set the setter function with extra sanity checks
      if (typeof api['set_' + v] === 'undefined') {
          api['set_' + v] = function (val, idx) {
              if (mode in {'RO':1, 'ro':1}) {
                  throw(v + " is read-only");
              } else if ((mode in {'WO':1, 'wo':1}) &&
                        (typeof cfg[v] !== 'undefined')) {
                  throw(v + " can only be set once");
              }
              setter(val, idx);
          };
      }

      // Set the default value
      if (typeof defaults[v] !== 'undefined') {
          defval = defaults[v];
      } else if ((type in {'arr':1, 'array':1}) &&
              (! (defval instanceof Array))) {
          defval = [];
      }
      // Coerce existing setting to the right type
      //Util.Debug("v: " + v + ", defval: " + defval + ", defaults[v]: " + defaults[v]);
      setter(defval);
  };

  // Set group of configuration defaults
  Util.conf_defaults = function(cfg, api, defaults, arr) {
      var i;
      for (i = 0; i < arr.length; i++) {
          Util.conf_default(cfg, api, defaults, arr[i][0], arr[i][1],
                  arr[i][2], arr[i][3], arr[i][4]);
      }
  };

  /*
  * Decode from UTF-8
  */
  Util.decodeUTF8 = function(utf8string) {
      return decodeURIComponent(escape(utf8string));
  }



  /*
  * Cross-browser routines
  */


  // Dynamically load scripts without using document.write()
  // Reference: http://unixpapa.com/js/dyna.html
  //
  // Handles the case where load_scripts is invoked from a script that
  // itself is loaded via load_scripts. Once all scripts are loaded the
  // window.onscriptsloaded handler is called (if set).
  Util.get_include_uri = function() {
      return (typeof INCLUDE_URI !== "undefined") ? INCLUDE_URI : "include/";
  }
  Util._loading_scripts = [];
  Util._pending_scripts = [];
  Util.load_scripts = function(files) {
      var head = document.getElementsByTagName('head')[0], script,
          ls = Util._loading_scripts, ps = Util._pending_scripts;
      for (var f=0; f<files.length; f++) {
          script = document.createElement('script');
          script.type = 'text/javascript';
          script.src = Util.get_include_uri() + files[f];
          //console.log("loading script: " + script.src);
          script.onload = script.onreadystatechange = function (e) {
              while (ls.length > 0 && (ls[0].readyState === 'loaded' ||
                                      ls[0].readyState === 'complete')) {
                  // For IE, append the script to trigger execution
                  var s = ls.shift();
                  //console.log("loaded script: " + s.src);
                  head.appendChild(s);
              }
              if (!this.readyState ||
                  (Util.Engine.presto && this.readyState === 'loaded') ||
                  this.readyState === 'complete') {
                  if (ps.indexOf(this) >= 0) {
                      this.onload = this.onreadystatechange = null;
                      //console.log("completed script: " + this.src);
                      ps.splice(ps.indexOf(this), 1);

                      // Call window.onscriptsload after last script loads
                      if (ps.length === 0 && window.onscriptsload) {
                          window.onscriptsload();
                      }
                  }
              }
          };
          // In-order script execution tricks
          if (Util.Engine.trident) {
              // For IE wait until readyState is 'loaded' before
              // appending it which will trigger execution
              // http://wiki.whatwg.org/wiki/Dynamic_Script_Execution_Order
              ls.push(script);
          } else {
              // For webkit and firefox set async=false and append now
              // https://developer.mozilla.org/en-US/docs/HTML/Element/script
              script.async = false;
              head.appendChild(script);
          }
          ps.push(script);
      }
  }


  // Get DOM element position on page
  //  This solution is based based on http://www.greywyvern.com/?post=331
  //  Thanks to Brian Huisman AKA GreyWyvern!
  Util.getPosition = (function() {
      function getStyle(obj, styleProp) {
          if (obj.currentStyle) {
              var y = obj.currentStyle[styleProp];
          } else if (window.getComputedStyle)
              var y = window.getComputedStyle(obj, null)[styleProp];
          return y;
      };

      function scrollDist() {
          var myScrollTop = 0, myScrollLeft = 0;
          var html = document.getElementsByTagName('html')[0];

          // get the scrollTop part
          if (html.scrollTop && document.documentElement.scrollTop) {
              myScrollTop = html.scrollTop;
          } else if (html.scrollTop || document.documentElement.scrollTop) {
              myScrollTop = html.scrollTop + document.documentElement.scrollTop;
          } else if (document.body.scrollTop) {
              myScrollTop = document.body.scrollTop;
          } else {
              myScrollTop = 0;
          }

          // get the scrollLeft part
          if (html.scrollLeft && document.documentElement.scrollLeft) {
              myScrollLeft = html.scrollLeft;
          } else if (html.scrollLeft || document.documentElement.scrollLeft) {
              myScrollLeft = html.scrollLeft + document.documentElement.scrollLeft;
          } else if (document.body.scrollLeft) {
              myScrollLeft = document.body.scrollLeft;
          } else {
              myScrollLeft = 0;
          }

          return [myScrollLeft, myScrollTop];
      };

      return function (obj) {
          var curleft = 0, curtop = 0, scr = obj, fixed = false;
          while ((scr = scr.parentNode) && scr != document.body) {
              curleft -= scr.scrollLeft || 0;
              curtop -= scr.scrollTop || 0;
              if (getStyle(scr, "position") == "fixed") {
                  fixed = true;
              }
          }
          if (fixed && !window.opera) {
              var scrDist = scrollDist();
              curleft += scrDist[0];
              curtop += scrDist[1];
          }

          do {
              curleft += obj.offsetLeft;
              curtop += obj.offsetTop;
          } while (obj = obj.offsetParent);

          return {'x': curleft, 'y': curtop};
      };
  })();


  // Get mouse event position in DOM element
  Util.getEventPosition = function (e, obj, scale) {
      var evt, docX, docY, pos;
      //if (!e) evt = window.event;
      evt = (e ? e : window.event);
      evt = (evt.changedTouches ? evt.changedTouches[0] : evt.touches ? evt.touches[0] : evt);
      if (evt.pageX || evt.pageY) {
          docX = evt.pageX;
          docY = evt.pageY;
      } else if (evt.clientX || evt.clientY) {
          docX = evt.clientX + document.body.scrollLeft +
              document.documentElement.scrollLeft;
          docY = evt.clientY + document.body.scrollTop +
              document.documentElement.scrollTop;
      }
      pos = Util.getPosition(obj);
      if (typeof scale === "undefined") {
          scale = 1;
      }
      var realx = docX - pos.x;
      var realy = docY - pos.y;
      var x = Math.max(Math.min(realx, obj.width-1), 0);
      var y = Math.max(Math.min(realy, obj.height-1), 0);
      return {'x': x / scale, 'y': y / scale, 'realx': realx / scale, 'realy': realy / scale};
  };


  // Event registration. Based on: http://www.scottandrew.com/weblog/articles/cbs-events
  Util.addEvent = function (obj, evType, fn){
      if (obj.attachEvent){
          var r = obj.attachEvent("on"+evType, fn);
          return r;
      } else if (obj.addEventListener){
          obj.addEventListener(evType, fn, false); 
          return true;
      } else {
          throw("Handler could not be attached");
      }
  };

  Util.removeEvent = function(obj, evType, fn){
      if (obj.detachEvent){
          var r = obj.detachEvent("on"+evType, fn);
          return r;
      } else if (obj.removeEventListener){
          obj.removeEventListener(evType, fn, false);
          return true;
      } else {
          throw("Handler could not be removed");
      }
  };

  Util.stopEvent = function(e) {
      if (e.stopPropagation) { e.stopPropagation(); }
      else                   { e.cancelBubble = true; }

      if (e.preventDefault)  { e.preventDefault(); }
      else                   { e.returnValue = false; }
  };


  // Set browser engine versions. Based on mootools.
  Util.Features = {xpath: !!(document.evaluate), air: !!(window.runtime), query: !!(document.querySelector)};

  Util.Engine = {
      // Version detection break in Opera 11.60 (errors on arguments.callee.caller reference)
      //'presto': (function() {
      //         return (!window.opera) ? false : ((arguments.callee.caller) ? 960 : ((document.getElementsByClassName) ? 950 : 925)); }()),
      'presto': (function() { return (!window.opera) ? false : true; }()),

      'trident': (function() {
              return (!window.ActiveXObject) ? false : ((window.XMLHttpRequest) ? ((document.querySelectorAll) ? 6 : 5) : 4); }()),
      'webkit': (function() {
              try { return (navigator.taintEnabled) ? false : ((Util.Features.xpath) ? ((Util.Features.query) ? 525 : 420) : 419); } catch (e) { return false; } }()),
      //'webkit': (function() {
      //        return ((typeof navigator.taintEnabled !== "unknown") && navigator.taintEnabled) ? false : ((Util.Features.xpath) ? ((Util.Features.query) ? 525 : 420) : 419); }()),
      'gecko': (function() {
              return (!document.getBoxObjectFor && window.mozInnerScreenX == null) ? false : ((document.getElementsByClassName) ? 19 : 18); }())
  };
  if (Util.Engine.webkit) {
      // Extract actual webkit version if available
      Util.Engine.webkit = (function(v) {
              var re = new RegExp('WebKit/([0-9\.]*) ');
              v = (navigator.userAgent.match(re) || ['', v])[1];
              return parseFloat(v, 10);
          })(Util.Engine.webkit);
  }

  Util.Flash = (function(){
      var v, version;
      try {
          v = navigator.plugins['Shockwave Flash'].description;
      } catch(err1) {
          try {
              v = new ActiveXObject('ShockwaveFlash.ShockwaveFlash').GetVariable('$version');
          } catch(err2) {
              v = '0 r0';
          }
      }
      version = v.match(/\d+/g);
      return {version: parseInt(version[0] || 0 + '.' + version[1], 10) || 0, build: parseInt(version[2], 10) || 0};
  }()); 
  ;

  // webutil
  /*
  * noVNC: HTML5 VNC client
  * Copyright (C) 2012 Joel Martin
  * Copyright (C) 2013 NTT corp.
  * Licensed under MPL 2.0 (see LICENSE.txt)
  *
  * See README.md for usage and integration instructions.
  */

  "use strict";
  /*jslint bitwise: false, white: false */
  /*global Util, window, document */

  // Globals defined here
  var WebUtil = {}, $D;

  /*
  * Simple DOM selector by ID
  */
  if (!window.$D) {
      window.$D = function (id) {
          if (document.getElementById) {
              return document.getElementById(id);
          } else if (document.all) {
              return document.all[id];
          } else if (document.layers) {
              return document.layers[id];
          }
          return undefined;
      };
  }


  /* 
  * ------------------------------------------------------
  * Namespaced in WebUtil
  * ------------------------------------------------------
  */

  // init log level reading the logging HTTP param
  WebUtil.init_logging = function(level) {
      if (typeof level !== "undefined") {
          Util._log_level = level;
      } else {
          Util._log_level = (document.location.href.match(
              /logging=([A-Za-z0-9\._\-]*)/) ||
              ['', Util._log_level])[1];
      }
      Util.init_logging();
  };


  WebUtil.dirObj = function (obj, depth, parent) {
      var i, msg = "", val = "";
      if (! depth) { depth=2; }
      if (! parent) { parent= ""; }

      // Print the properties of the passed-in object 
      for (i in obj) {
          if ((depth > 1) && (typeof obj[i] === "object")) { 
              // Recurse attributes that are objects
              msg += WebUtil.dirObj(obj[i], depth-1, parent + "." + i);
          } else {
              //val = new String(obj[i]).replace("\n", " ");
              if (typeof(obj[i]) === "undefined") {
                  val = "undefined";
              } else {
                  val = obj[i].toString().replace("\n", " ");
              }
              if (val.length > 30) {
                  val = val.substr(0,30) + "...";
              } 
              msg += parent + "." + i + ": " + val + "\n";
          }
      }
      return msg;
  };

  // Read a query string variable
  WebUtil.getQueryVar = function(name, defVal) {
      var re = new RegExp('.*[?&]' + name + '=([^&#]*)'),
          match = document.location.href.match(re);
      if (typeof defVal === 'undefined') { defVal = null; }
      if (match) {
          return decodeURIComponent(match[1]);
      } else {
          return defVal;
      }
  };


  /*
  * Cookie handling. Dervied from: http://www.quirksmode.org/js/cookies.html
  */

  // No days means only for this browser session
  WebUtil.createCookie = function(name,value,days) {
      var date, expires, secure;
      if (days) {
          date = new Date();
          date.setTime(date.getTime()+(days*24*60*60*1000));
          expires = "; expires="+date.toGMTString();
      } else {
          expires = "";
      }
      if (document.location.protocol === "https:") {
          secure = "; secure";
      } else {
          secure = "";
      }
      document.cookie = name+"="+value+expires+"; path=/"+secure;
  };

  WebUtil.readCookie = function(name, defaultValue) {
      var i, c, nameEQ = name + "=", ca = document.cookie.split(';');
      for(i=0; i < ca.length; i += 1) {
          c = ca[i];
          while (c.charAt(0) === ' ') { c = c.substring(1,c.length); }
          if (c.indexOf(nameEQ) === 0) { return c.substring(nameEQ.length,c.length); }
      }
      return (typeof defaultValue !== 'undefined') ? defaultValue : null;
  };

  WebUtil.eraseCookie = function(name) {
      WebUtil.createCookie(name,"",-1);
  };

  /*
  * Setting handling.
  */

  WebUtil.initSettings = function(callback) {
      var callbackArgs = Array.prototype.slice.call(arguments, 1);
      if (window.chrome && window.chrome.storage) {
          window.chrome.storage.sync.get(function (cfg) {
              WebUtil.settings = cfg;
              console.log(WebUtil.settings);
              if (callback) {
                  callback.apply(this, callbackArgs);
              }
          });
      } else {
          // No-op
          if (callback) {
              callback.apply(this, callbackArgs);
          }
      }
  };

  // No days means only for this browser session
  WebUtil.writeSetting = function(name, value) {
      if (window.chrome && window.chrome.storage) {
          //console.log("writeSetting:", name, value);
          if (WebUtil.settings[name] !== value) {
              WebUtil.settings[name] = value;
              window.chrome.storage.sync.set(WebUtil.settings);
          }
      } else {
          localStorage.setItem(name, value);
      }
  };

  WebUtil.readSetting = function(name, defaultValue) {
      var value;
      if (window.chrome && window.chrome.storage) {
          value = WebUtil.settings[name];
      } else {
          value = localStorage.getItem(name);
      }
      if (typeof value === "undefined") {
          value = null;
      }
      if (value === null && typeof defaultValue !== undefined) {
          return defaultValue;
      } else {
          return value;
      }
  };

  WebUtil.eraseSetting = function(name) {
      if (window.chrome && window.chrome.storage) {
          window.chrome.storage.sync.remove(name);
          delete WebUtil.settings[name];
      } else {
          localStorage.removeItem(name);
      }
  };

  /*
  * Alternate stylesheet selection
  */
  WebUtil.getStylesheets = function() { var i, links, sheets = [];
      links = document.getElementsByTagName("link");
      for (i = 0; i < links.length; i += 1) {
          if (links[i].title &&
              links[i].rel.toUpperCase().indexOf("STYLESHEET") > -1) {
              sheets.push(links[i]);
          }
      }
      return sheets;
  };

  // No sheet means try and use value from cookie, null sheet used to
  // clear all alternates.
  WebUtil.selectStylesheet = function(sheet) {
      var i, link, sheets = WebUtil.getStylesheets();
      if (typeof sheet === 'undefined') {
          sheet = 'default';
      }
      for (i=0; i < sheets.length; i += 1) {
          link = sheets[i];
          if (link.title === sheet) {    
              Util.Debug("Using stylesheet " + sheet);
              link.disabled = false;
          } else {
              //Util.Debug("Skipping stylesheet " + link.title);
              link.disabled = true;
          }
      }
      return sheet;
  };
  ;

  // base64
  /* This Source Code Form is subject to the terms of the Mozilla Public
  * License, v. 2.0. If a copy of the MPL was not distributed with this
  * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

  // From: http://hg.mozilla.org/mozilla-central/raw-file/ec10630b1a54/js/src/devtools/jint/sunspider/string-base64.js

  /*jslint white: false, bitwise: false, plusplus: false */
  /*global console */

  var Base64 = {

  /* Convert data (an array of integers) to a Base64 string. */
  toBase64Table : 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/='.split(''),
  base64Pad     : '=',

  encode: function (data) {
      "use strict";
      var result = '';
      var toBase64Table = Base64.toBase64Table;
      var length = data.length
      var lengthpad = (length%3);
      var i = 0, j = 0;
      // Convert every three bytes to 4 ascii characters.
    /* BEGIN LOOP */
      for (i = 0; i < (length - 2); i += 3) {
          result += toBase64Table[data[i] >> 2];
          result += toBase64Table[((data[i] & 0x03) << 4) + (data[i+1] >> 4)];
          result += toBase64Table[((data[i+1] & 0x0f) << 2) + (data[i+2] >> 6)];
          result += toBase64Table[data[i+2] & 0x3f];
      }
    /* END LOOP */

      // Convert the remaining 1 or 2 bytes, pad out to 4 characters.
      if (lengthpad === 2) {
          j = length - lengthpad;
          result += toBase64Table[data[j] >> 2];
          result += toBase64Table[((data[j] & 0x03) << 4) + (data[j+1] >> 4)];
          result += toBase64Table[(data[j+1] & 0x0f) << 2];
          result += toBase64Table[64];
      } else if (lengthpad === 1) {
          j = length - lengthpad;
          result += toBase64Table[data[j] >> 2];
          result += toBase64Table[(data[j] & 0x03) << 4];
          result += toBase64Table[64];
          result += toBase64Table[64];
      }

      return result;
  },

  /* Convert Base64 data to a string */
  toBinaryTable : [
      -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
      -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
      -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,62, -1,-1,-1,63,
      52,53,54,55, 56,57,58,59, 60,61,-1,-1, -1, 0,-1,-1,
      -1, 0, 1, 2,  3, 4, 5, 6,  7, 8, 9,10, 11,12,13,14,
      15,16,17,18, 19,20,21,22, 23,24,25,-1, -1,-1,-1,-1,
      -1,26,27,28, 29,30,31,32, 33,34,35,36, 37,38,39,40,
      41,42,43,44, 45,46,47,48, 49,50,51,-1, -1,-1,-1,-1
  ],

  decode: function (data, offset) {
      "use strict";
      offset = typeof(offset) !== 'undefined' ? offset : 0;
      var toBinaryTable = Base64.toBinaryTable;
      var base64Pad = Base64.base64Pad;
      var result, result_length, idx, i, c, padding;
      var leftbits = 0; // number of bits decoded, but yet to be appended
      var leftdata = 0; // bits decoded, but yet to be appended
      var data_length = data.indexOf('=') - offset;

      if (data_length < 0) { data_length = data.length - offset; }

      /* Every four characters is 3 resulting numbers */
      result_length = (data_length >> 2) * 3 + Math.floor((data_length%4)/1.5);
      result = new Array(result_length);

      // Convert one by one.
    /* BEGIN LOOP */
      for (idx = 0, i = offset; i < data.length; i++) {
          c = toBinaryTable[data.charCodeAt(i) & 0x7f];
          padding = (data.charAt(i) === base64Pad);
          // Skip illegal characters and whitespace
          if (c === -1) {
              console.error("Illegal character code " + data.charCodeAt(i) + " at position " + i);
              continue;
          }
          
          // Collect data into leftdata, update bitcount
          leftdata = (leftdata << 6) | c;
          leftbits += 6;

          // If we have 8 or more bits, append 8 bits to the result
          if (leftbits >= 8) {
              leftbits -= 8;
              // Append if not padding.
              if (!padding) {
                  result[idx++] = (leftdata >> leftbits) & 0xff;
              }
              leftdata &= (1 << leftbits) - 1;
          }
      }
    /* END LOOP */

      // If there are any bits left, the base64 string was corrupted
      if (leftbits) {
          throw {name: 'Base64-Error', 
                message: 'Corrupted base64 string'};
      }

      return result;
  }

  }; /* End of Base64 namespace */
  ;

  // websock
  /*
  * Websock: high-performance binary WebSockets
  * Copyright (C) 2012 Joel Martin
  * Licensed under MPL 2.0 (see LICENSE.txt)
  *
  * Websock is similar to the standard WebSocket object but Websock
  * enables communication with raw TCP sockets (i.e. the binary stream)
  * via websockify. This is accomplished by base64 encoding the data
  * stream between Websock and websockify.
  *
  * Websock has built-in receive queue buffering; the message event
  * does not contain actual data but is simply a notification that
  * there is new data available. Several rQ* methods are available to
  * read binary data off of the receive queue.
  */

  /*jslint browser: true, bitwise: false, plusplus: false */
  /*global Util, Base64 */


  // Load Flash WebSocket emulator if needed

  // To force WebSocket emulator even when native WebSocket available
  //window.WEB_SOCKET_FORCE_FLASH = true;
  // To enable WebSocket emulator debug:
  //window.WEB_SOCKET_DEBUG=1;

  if (window.WebSocket && !window.WEB_SOCKET_FORCE_FLASH) {
      Websock_native = true;
  } else if (window.MozWebSocket && !window.WEB_SOCKET_FORCE_FLASH) {
      Websock_native = true;
      window.WebSocket = window.MozWebSocket;
  } else {
      /* no builtin WebSocket so load web_socket.js */

      Websock_native = false;
      (function () {
          window.WEB_SOCKET_SWF_LOCATION = Util.get_include_uri() +
                      "web-socket-js/WebSocketMain.swf";
          if (Util.Engine.trident) {
              Util.Debug("Forcing uncached load of WebSocketMain.swf");
              window.WEB_SOCKET_SWF_LOCATION += "?" + Math.random();
          }
          Util.load_scripts(["web-socket-js/swfobject.js",
                            "web-socket-js/web_socket.js"]);
      }());
  }


  function Websock() {
  "use strict";

  var api = {},         // Public API
      websocket = null, // WebSocket object
      mode = 'base64',  // Current WebSocket mode: 'binary', 'base64'
      rQ = [],          // Receive queue
      rQi = 0,          // Receive queue index
      rQmax = 10000,    // Max receive queue size before compacting
      sQ = [],          // Send queue

      eventHandlers = {
          'message' : function() {},
          'open'    : function() {},
          'close'   : function() {},
          'error'   : function() {}
      },

      test_mode = false;


  //
  // Queue public functions
  //

  function get_sQ() {
      return sQ;
  }

  function get_rQ() {
      return rQ;
  }
  function get_rQi() {
      return rQi;
  }
  function set_rQi(val) {
      rQi = val;
  }

  function rQlen() {
      return rQ.length - rQi;
  }

  function rQpeek8() {
      return (rQ[rQi]      );
  }
  function rQshift8() {
      return (rQ[rQi++]      );
  }
  function rQunshift8(num) {
      if (rQi === 0) {
          rQ.unshift(num);
      } else {
          rQi -= 1;
          rQ[rQi] = num;
      }

  }
  function rQshift16() {
      return (rQ[rQi++] <<  8) +
            (rQ[rQi++]      );
  }
  function rQshift32() {
      return (rQ[rQi++] << 24) +
            (rQ[rQi++] << 16) +
            (rQ[rQi++] <<  8) +
            (rQ[rQi++]      );
  }
  function rQshiftStr(len) {
      if (typeof(len) === 'undefined') { len = rQlen(); }
      var arr = rQ.slice(rQi, rQi + len);
      rQi += len;
      return String.fromCharCode.apply(null, arr);
  }
  function rQshiftBytes(len) {
      if (typeof(len) === 'undefined') { len = rQlen(); }
      rQi += len;
      return rQ.slice(rQi-len, rQi);
  }

  function rQslice(start, end) {
      if (end) {
          return rQ.slice(rQi + start, rQi + end);
      } else {
          return rQ.slice(rQi + start);
      }
  }

  // Check to see if we must wait for 'num' bytes (default to FBU.bytes)
  // to be available in the receive queue. Return true if we need to
  // wait (and possibly print a debug message), otherwise false.
  function rQwait(msg, num, goback) {
      var rQlen = rQ.length - rQi; // Skip rQlen() function call
      if (rQlen < num) {
          if (goback) {
              if (rQi < goback) {
                  throw("rQwait cannot backup " + goback + " bytes");
              }
              rQi -= goback;
          }
          //Util.Debug("   waiting for " + (num-rQlen) +
          //           " " + msg + " byte(s)");
          return true;  // true means need more data
      }
      return false;
  }

  //
  // Private utility routines
  //

  function encode_message() {
      if (mode === 'binary') {
          // Put in a binary arraybuffer
          return (new Uint8Array(sQ)).buffer;
      } else {
          // base64 encode
          return Base64.encode(sQ);
      }
  }

  function decode_message(data) {
      //Util.Debug(">> decode_message: " + data);
      if (mode === 'binary') {
          // push arraybuffer values onto the end
          var u8 = new Uint8Array(data);
          for (var i = 0; i < u8.length; i++) {
              rQ.push(u8[i]);
          }
      } else {
          // base64 decode and concat to the end
          rQ = rQ.concat(Base64.decode(data, 0));
      }
      //Util.Debug(">> decode_message, rQ: " + rQ);
  }


  //
  // Public Send functions
  //

  function flush() {
      if (websocket.bufferedAmount !== 0) {
          Util.Debug("bufferedAmount: " + websocket.bufferedAmount);
      }
      if (websocket.bufferedAmount < api.maxBufferedAmount) {
          //Util.Debug("arr: " + arr);
          //Util.Debug("sQ: " + sQ);
          if (sQ.length > 0) {
              websocket.send(encode_message(sQ));
              sQ = [];
          }
          return true;
      } else {
          Util.Info("Delaying send, bufferedAmount: " +
                  websocket.bufferedAmount);
          return false;
      }
  }

  // overridable for testing
  function send(arr) {
      //Util.Debug(">> send_array: " + arr);
      sQ = sQ.concat(arr);
      return flush();
  }

  function send_string(str) {
      //Util.Debug(">> send_string: " + str);
      api.send(str.split('').map(
          function (chr) { return chr.charCodeAt(0); } ) );
  }

  //
  // Other public functions

  function recv_message(e) {
      //Util.Debug(">> recv_message: " + e.data.length);

      try {
          decode_message(e.data);
          if (rQlen() > 0) {
              eventHandlers.message();
              // Compact the receive queue
              if (rQ.length > rQmax) {
                  //Util.Debug("Compacting receive queue");
                  rQ = rQ.slice(rQi);
                  rQi = 0;
              }
          } else {
              Util.Debug("Ignoring empty message");
          }
      } catch (exc) {
          if (typeof exc.stack !== 'undefined') {
              Util.Warn("recv_message, caught exception: " + exc.stack);
          } else if (typeof exc.description !== 'undefined') {
              Util.Warn("recv_message, caught exception: " + exc.description);
          } else {
              Util.Warn("recv_message, caught exception:" + exc);
          }
          if (typeof exc.name !== 'undefined') {
              eventHandlers.error(exc.name + ": " + exc.message);
          } else {
              eventHandlers.error(exc);
          }
      }
      //Util.Debug("<< recv_message");
  }


  // Set event handlers
  function on(evt, handler) { 
      eventHandlers[evt] = handler;
  }

  function init(protocols) {
      rQ         = [];
      rQi        = 0;
      sQ         = [];
      websocket  = null;

      var bt = false,
          wsbt = false,
          try_binary = false;

      // Check for full typed array support
      if (('Uint8Array' in window) &&
          ('set' in Uint8Array.prototype)) {
          bt = true;
      }

      // Check for full binary type support in WebSockets
      // TODO: this sucks, the property should exist on the prototype
      // but it does not.
      try {
          if (bt && ('binaryType' in (new WebSocket("ws://localhost:17523")))) {
              Util.Info("Detected binaryType support in WebSockets");
              wsbt = true;
          }
      } catch (exc) {
          // Just ignore failed test localhost connections
      }

      // Default protocols if not specified
      if (typeof(protocols) === "undefined") {
          if (wsbt) {
              protocols = ['binary', 'base64'];
          } else {
              protocols = 'base64';
          }
      }

      // If no binary support, make sure it was not requested
      if (!wsbt) {
          if (protocols === 'binary') {
              throw("WebSocket binary sub-protocol requested but not supported");
          }
          if (typeof(protocols) === "object") {
              var new_protocols = [];
              for (var i = 0; i < protocols.length; i++) {
                  if (protocols[i] === 'binary') {
                      Util.Error("Skipping unsupported WebSocket binary sub-protocol");
                  } else {
                      new_protocols.push(protocols[i]);
                  }
              }
              if (new_protocols.length > 0) {
                  protocols = new_protocols;
              } else {
                  throw("Only WebSocket binary sub-protocol was requested and not supported.");
              }
          }
      }

      return protocols;
  }

  function open(uri, protocols) {
      protocols = init(protocols);

      if (test_mode) {
          websocket = {};
      } else {
          websocket = new WebSocket(uri, protocols);
          if (protocols.indexOf('binary') >= 0) {
              websocket.binaryType = 'arraybuffer';
          }
      }

      websocket.onmessage = recv_message;
      websocket.onopen = function() {
          Util.Debug(">> WebSock.onopen");
          if (websocket.protocol) {
              mode = websocket.protocol;
              Util.Info("Server chose sub-protocol: " + websocket.protocol);
          } else {
              mode = 'base64';
              Util.Error("Server select no sub-protocol!: " + websocket.protocol);
          }
          eventHandlers.open();
          Util.Debug("<< WebSock.onopen");
      };
      websocket.onclose = function(e) {
          Util.Debug(">> WebSock.onclose");
          eventHandlers.close(e);
          Util.Debug("<< WebSock.onclose");
      };
      websocket.onerror = function(e) {
          Util.Debug(">> WebSock.onerror: " + e);
          eventHandlers.error(e);
          Util.Debug("<< WebSock.onerror");
      };
  }

  function close() {
      if (websocket) {
          if ((websocket.readyState === WebSocket.OPEN) ||
              (websocket.readyState === WebSocket.CONNECTING)) {
              Util.Info("Closing WebSocket connection");
              websocket.close();
          }
          websocket.onmessage = function (e) { return; };
      }
  }

  // Override internal functions for testing
  // Takes a send function, returns reference to recv function
  function testMode(override_send, data_mode) {
      test_mode = true;
      mode = data_mode;
      api.send = override_send;
      api.close = function () {};
      return recv_message;
  }

  function constructor() {
      // Configuration settings
      api.maxBufferedAmount = 200;

      // Direct access to send and receive queues
      api.get_sQ       = get_sQ;
      api.get_rQ       = get_rQ;
      api.get_rQi      = get_rQi;
      api.set_rQi      = set_rQi;

      // Routines to read from the receive queue
      api.rQlen        = rQlen;
      api.rQpeek8      = rQpeek8;
      api.rQshift8     = rQshift8;
      api.rQunshift8   = rQunshift8;
      api.rQshift16    = rQshift16;
      api.rQshift32    = rQshift32;
      api.rQshiftStr   = rQshiftStr;
      api.rQshiftBytes = rQshiftBytes;
      api.rQslice      = rQslice;
      api.rQwait       = rQwait;

      api.flush        = flush;
      api.send         = send;
      api.send_string  = send_string;

      api.on           = on;
      api.init         = init;
      api.open         = open;
      api.close        = close;
      api.testMode     = testMode;

      return api;
  }

  return constructor();

  }
  ;

  // des
  /*
  * Ported from Flashlight VNC ActionScript implementation:
  *     http://www.wizhelp.com/flashlight-vnc/
  *
  * Full attribution follows:
  *
  * -------------------------------------------------------------------------
  *
  * This DES class has been extracted from package Acme.Crypto for use in VNC.
  * The unnecessary odd parity code has been removed.
  *
  * These changes are:
  *  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *

  * DesCipher - the DES encryption method
  *
  * The meat of this code is by Dave Zimmerman <dzimm@widget.com>, and is:
  *
  * Copyright (c) 1996 Widget Workshop, Inc. All Rights Reserved.
  *
  * Permission to use, copy, modify, and distribute this software
  * and its documentation for NON-COMMERCIAL or COMMERCIAL purposes and
  * without fee is hereby granted, provided that this copyright notice is kept 
  * intact. 
  * 
  * WIDGET WORKSHOP MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
  * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
  * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
  * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. WIDGET WORKSHOP SHALL NOT BE LIABLE
  * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
  * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
  * 
  * THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
  * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
  * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
  * NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
  * SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
  * SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
  * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES").  WIDGET WORKSHOP
  * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
  * HIGH RISK ACTIVITIES.
  *
  *
  * The rest is:
  *
  * Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in the
  *    documentation and/or other materials provided with the distribution.
  *
  * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
  * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
  * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
  * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  *
  * Visit the ACME Labs Java page for up-to-date versions of this and other
  * fine Java utilities: http://www.acme.com/java/
  */

  "use strict";
  /*jslint white: false, bitwise: false, plusplus: false */

  function DES(passwd) {

  // Tables, permutations, S-boxes, etc.
  var PC2 = [13,16,10,23, 0, 4, 2,27,14, 5,20, 9,22,18,11, 3,
            25, 7,15, 6,26,19,12, 1,40,51,30,36,46,54,29,39,
            50,44,32,47,43,48,38,55,33,52,45,41,49,35,28,31 ],
      totrot = [ 1, 2, 4, 6, 8,10,12,14,15,17,19,21,23,25,27,28],
      z = 0x0, a,b,c,d,e,f, SP1,SP2,SP3,SP4,SP5,SP6,SP7,SP8,
      keys = [];

  a=1<<16; b=1<<24; c=a|b; d=1<<2; e=1<<10; f=d|e;
  SP1 = [c|e,z|z,a|z,c|f,c|d,a|f,z|d,a|z,z|e,c|e,c|f,z|e,b|f,c|d,b|z,z|d,
        z|f,b|e,b|e,a|e,a|e,c|z,c|z,b|f,a|d,b|d,b|d,a|d,z|z,z|f,a|f,b|z,
        a|z,c|f,z|d,c|z,c|e,b|z,b|z,z|e,c|d,a|z,a|e,b|d,z|e,z|d,b|f,a|f,
        c|f,a|d,c|z,b|f,b|d,z|f,a|f,c|e,z|f,b|e,b|e,z|z,a|d,a|e,z|z,c|d];
  a=1<<20; b=1<<31; c=a|b; d=1<<5; e=1<<15; f=d|e;
  SP2 = [c|f,b|e,z|e,a|f,a|z,z|d,c|d,b|f,b|d,c|f,c|e,b|z,b|e,a|z,z|d,c|d,
        a|e,a|d,b|f,z|z,b|z,z|e,a|f,c|z,a|d,b|d,z|z,a|e,z|f,c|e,c|z,z|f,
        z|z,a|f,c|d,a|z,b|f,c|z,c|e,z|e,c|z,b|e,z|d,c|f,a|f,z|d,z|e,b|z,
        z|f,c|e,a|z,b|d,a|d,b|f,b|d,a|d,a|e,z|z,b|e,z|f,b|z,c|d,c|f,a|e];
  a=1<<17; b=1<<27; c=a|b; d=1<<3; e=1<<9; f=d|e;
  SP3 = [z|f,c|e,z|z,c|d,b|e,z|z,a|f,b|e,a|d,b|d,b|d,a|z,c|f,a|d,c|z,z|f,
        b|z,z|d,c|e,z|e,a|e,c|z,c|d,a|f,b|f,a|e,a|z,b|f,z|d,c|f,z|e,b|z,
        c|e,b|z,a|d,z|f,a|z,c|e,b|e,z|z,z|e,a|d,c|f,b|e,b|d,z|e,z|z,c|d,
        b|f,a|z,b|z,c|f,z|d,a|f,a|e,b|d,c|z,b|f,z|f,c|z,a|f,z|d,c|d,a|e];
  a=1<<13; b=1<<23; c=a|b; d=1<<0; e=1<<7; f=d|e;
  SP4 = [c|d,a|f,a|f,z|e,c|e,b|f,b|d,a|d,z|z,c|z,c|z,c|f,z|f,z|z,b|e,b|d,
        z|d,a|z,b|z,c|d,z|e,b|z,a|d,a|e,b|f,z|d,a|e,b|e,a|z,c|e,c|f,z|f,
        b|e,b|d,c|z,c|f,z|f,z|z,z|z,c|z,a|e,b|e,b|f,z|d,c|d,a|f,a|f,z|e,
        c|f,z|f,z|d,a|z,b|d,a|d,c|e,b|f,a|d,a|e,b|z,c|d,z|e,b|z,a|z,c|e];
  a=1<<25; b=1<<30; c=a|b; d=1<<8; e=1<<19; f=d|e;
  SP5 = [z|d,a|f,a|e,c|d,z|e,z|d,b|z,a|e,b|f,z|e,a|d,b|f,c|d,c|e,z|f,b|z,
        a|z,b|e,b|e,z|z,b|d,c|f,c|f,a|d,c|e,b|d,z|z,c|z,a|f,a|z,c|z,z|f,
        z|e,c|d,z|d,a|z,b|z,a|e,c|d,b|f,a|d,b|z,c|e,a|f,b|f,z|d,a|z,c|e,
        c|f,z|f,c|z,c|f,a|e,z|z,b|e,c|z,z|f,a|d,b|d,z|e,z|z,b|e,a|f,b|d];
  a=1<<22; b=1<<29; c=a|b; d=1<<4; e=1<<14; f=d|e;
  SP6 = [b|d,c|z,z|e,c|f,c|z,z|d,c|f,a|z,b|e,a|f,a|z,b|d,a|d,b|e,b|z,z|f,
        z|z,a|d,b|f,z|e,a|e,b|f,z|d,c|d,c|d,z|z,a|f,c|e,z|f,a|e,c|e,b|z,
        b|e,z|d,c|d,a|e,c|f,a|z,z|f,b|d,a|z,b|e,b|z,z|f,b|d,c|f,a|e,c|z,
        a|f,c|e,z|z,c|d,z|d,z|e,c|z,a|f,z|e,a|d,b|f,z|z,c|e,b|z,a|d,b|f];
  a=1<<21; b=1<<26; c=a|b; d=1<<1; e=1<<11; f=d|e;
  SP7 = [a|z,c|d,b|f,z|z,z|e,b|f,a|f,c|e,c|f,a|z,z|z,b|d,z|d,b|z,c|d,z|f,
        b|e,a|f,a|d,b|e,b|d,c|z,c|e,a|d,c|z,z|e,z|f,c|f,a|e,z|d,b|z,a|e,
        b|z,a|e,a|z,b|f,b|f,c|d,c|d,z|d,a|d,b|z,b|e,a|z,c|e,z|f,a|f,c|e,
        z|f,b|d,c|f,c|z,a|e,z|z,z|d,c|f,z|z,a|f,c|z,z|e,b|d,b|e,z|e,a|d];
  a=1<<18; b=1<<28; c=a|b; d=1<<6; e=1<<12; f=d|e;
  SP8 = [b|f,z|e,a|z,c|f,b|z,b|f,z|d,b|z,a|d,c|z,c|f,a|e,c|e,a|f,z|e,z|d,
        c|z,b|d,b|e,z|f,a|e,a|d,c|d,c|e,z|f,z|z,z|z,c|d,b|d,b|e,a|f,a|z,
        a|f,a|z,c|e,z|e,z|d,c|d,z|e,a|f,b|e,z|d,b|d,c|z,c|d,b|z,a|z,b|f,
        z|z,c|f,a|d,b|d,c|z,b|e,b|f,z|z,c|f,a|e,a|e,z|f,z|f,a|d,b|z,c|e];

  // Set the key.
  function setKeys(keyBlock) {
      var i, j, l, m, n, o, pc1m = [], pcr = [], kn = [],
          raw0, raw1, rawi, KnLi;

      for (j = 0, l = 56; j < 56; ++j, l-=8) {
          l += l<-5 ? 65 : l<-3 ? 31 : l<-1 ? 63 : l===27 ? 35 : 0; // PC1
          m = l & 0x7;
          pc1m[j] = ((keyBlock[l >>> 3] & (1<<m)) !== 0) ? 1: 0;
      }

      for (i = 0; i < 16; ++i) {
          m = i << 1;
          n = m + 1;
          kn[m] = kn[n] = 0;
          for (o=28; o<59; o+=28) {
              for (j = o-28; j < o; ++j) {
                  l = j + totrot[i];
                  if (l < o) {
                      pcr[j] = pc1m[l];
                  } else {
                      pcr[j] = pc1m[l - 28];
                  }
              }
          }
          for (j = 0; j < 24; ++j) {
              if (pcr[PC2[j]] !== 0) {
                  kn[m] |= 1<<(23-j);
              }
              if (pcr[PC2[j + 24]] !== 0) {
                  kn[n] |= 1<<(23-j);
              }
          }
      }

      // cookey
      for (i = 0, rawi = 0, KnLi = 0; i < 16; ++i) {
          raw0 = kn[rawi++];
          raw1 = kn[rawi++];
          keys[KnLi] = (raw0 & 0x00fc0000) << 6;
          keys[KnLi] |= (raw0 & 0x00000fc0) << 10;
          keys[KnLi] |= (raw1 & 0x00fc0000) >>> 10;
          keys[KnLi] |= (raw1 & 0x00000fc0) >>> 6;
          ++KnLi;
          keys[KnLi] = (raw0 & 0x0003f000) << 12;
          keys[KnLi] |= (raw0 & 0x0000003f) << 16;
          keys[KnLi] |= (raw1 & 0x0003f000) >>> 4;
          keys[KnLi] |= (raw1 & 0x0000003f);
          ++KnLi;
      }
  }

  // Encrypt 8 bytes of text
  function enc8(text) {
      var i = 0, b = text.slice(), fval, keysi = 0,
          l, r, x; // left, right, accumulator

      // Squash 8 bytes to 2 ints
      l = b[i++]<<24 | b[i++]<<16 | b[i++]<<8 | b[i++];
      r = b[i++]<<24 | b[i++]<<16 | b[i++]<<8 | b[i++];

      x = ((l >>> 4) ^ r) & 0x0f0f0f0f;
      r ^= x;
      l ^= (x << 4);
      x = ((l >>> 16) ^ r) & 0x0000ffff;
      r ^= x;
      l ^= (x << 16);
      x = ((r >>> 2) ^ l) & 0x33333333;
      l ^= x;
      r ^= (x << 2);
      x = ((r >>> 8) ^ l) & 0x00ff00ff;
      l ^= x;
      r ^= (x << 8);
      r = (r << 1) | ((r >>> 31) & 1);
      x = (l ^ r) & 0xaaaaaaaa;
      l ^= x;
      r ^= x;
      l = (l << 1) | ((l >>> 31) & 1);

      for (i = 0; i < 8; ++i) {
          x = (r << 28) | (r >>> 4);
          x ^= keys[keysi++];
          fval =  SP7[x & 0x3f];
          fval |= SP5[(x >>> 8) & 0x3f];
          fval |= SP3[(x >>> 16) & 0x3f];
          fval |= SP1[(x >>> 24) & 0x3f];
          x = r ^ keys[keysi++];
          fval |= SP8[x & 0x3f];
          fval |= SP6[(x >>> 8) & 0x3f];
          fval |= SP4[(x >>> 16) & 0x3f];
          fval |= SP2[(x >>> 24) & 0x3f];
          l ^= fval;
          x = (l << 28) | (l >>> 4);
          x ^= keys[keysi++];
          fval =  SP7[x & 0x3f];
          fval |= SP5[(x >>> 8) & 0x3f];
          fval |= SP3[(x >>> 16) & 0x3f];
          fval |= SP1[(x >>> 24) & 0x3f];
          x = l ^ keys[keysi++];
          fval |= SP8[x & 0x0000003f];
          fval |= SP6[(x >>> 8) & 0x3f];
          fval |= SP4[(x >>> 16) & 0x3f];
          fval |= SP2[(x >>> 24) & 0x3f];
          r ^= fval;
      }

      r = (r << 31) | (r >>> 1);
      x = (l ^ r) & 0xaaaaaaaa;
      l ^= x;
      r ^= x;
      l = (l << 31) | (l >>> 1);
      x = ((l >>> 8) ^ r) & 0x00ff00ff;
      r ^= x;
      l ^= (x << 8);
      x = ((l >>> 2) ^ r) & 0x33333333;
      r ^= x;
      l ^= (x << 2);
      x = ((r >>> 16) ^ l) & 0x0000ffff;
      l ^= x;
      r ^= (x << 16);
      x = ((r >>> 4) ^ l) & 0x0f0f0f0f;
      l ^= x;
      r ^= (x << 4);

      // Spread ints to bytes
      x = [r, l];
      for (i = 0; i < 8; i++) {
          b[i] = (x[i>>>2] >>> (8*(3 - (i%4)))) % 256;
          if (b[i] < 0) { b[i] += 256; } // unsigned
      }
      return b;
  }

  // Encrypt 16 bytes of text using passwd as key
  function encrypt(t) {
      return enc8(t.slice(0,8)).concat(enc8(t.slice(8,16)));
  }

  setKeys(passwd);             // Setup keys
  return {'encrypt': encrypt}; // Public interface

  } // function DES
  ;

  // keysymdef
  // This file describes mappings from Unicode codepoints to the keysym values
  // (and optionally, key names) expected by the RFB protocol
  // How this file was generated:
  // node /Users/jalf/dev/mi/novnc/utils/parse.js /opt/X11/include/X11/keysymdef.h
  var keysyms = (function(){
      "use strict";
      var keynames = null;
      var codepoints = {"32":32,"33":33,"34":34,"35":35,"36":36,"37":37,"38":38,"39":39,"40":40,"41":41,"42":42,"43":43,"44":44,"45":45,"46":46,"47":47,"48":48,"49":49,"50":50,"51":51,"52":52,"53":53,"54":54,"55":55,"56":56,"57":57,"58":58,"59":59,"60":60,"61":61,"62":62,"63":63,"64":64,"65":65,"66":66,"67":67,"68":68,"69":69,"70":70,"71":71,"72":72,"73":73,"74":74,"75":75,"76":76,"77":77,"78":78,"79":79,"80":80,"81":81,"82":82,"83":83,"84":84,"85":85,"86":86,"87":87,"88":88,"89":89,"90":90,"91":91,"92":92,"93":93,"94":94,"95":95,"96":96,"97":97,"98":98,"99":99,"100":100,"101":101,"102":102,"103":103,"104":104,"105":105,"106":106,"107":107,"108":108,"109":109,"110":110,"111":111,"112":112,"113":113,"114":114,"115":115,"116":116,"117":117,"118":118,"119":119,"120":120,"121":121,"122":122,"123":123,"124":124,"125":125,"126":126,"160":160,"161":161,"162":162,"163":163,"164":164,"165":165,"166":166,"167":167,"168":168,"169":169,"170":170,"171":171,"172":172,"173":173,"174":174,"175":175,"176":176,"177":177,"178":178,"179":179,"180":180,"181":181,"182":182,"183":183,"184":184,"185":185,"186":186,"187":187,"188":188,"189":189,"190":190,"191":191,"192":192,"193":193,"194":194,"195":195,"196":196,"197":197,"198":198,"199":199,"200":200,"201":201,"202":202,"203":203,"204":204,"205":205,"206":206,"207":207,"208":208,"209":209,"210":210,"211":211,"212":212,"213":213,"214":214,"215":215,"216":216,"217":217,"218":218,"219":219,"220":220,"221":221,"222":222,"223":223,"224":224,"225":225,"226":226,"227":227,"228":228,"229":229,"230":230,"231":231,"232":232,"233":233,"234":234,"235":235,"236":236,"237":237,"238":238,"239":239,"240":240,"241":241,"242":242,"243":243,"244":244,"245":245,"246":246,"247":247,"248":248,"249":249,"250":250,"251":251,"252":252,"253":253,"254":254,"255":255,"256":960,"257":992,"258":451,"259":483,"260":417,"261":433,"262":454,"263":486,"264":710,"265":742,"266":709,"267":741,"268":456,"269":488,"270":463,"271":495,"272":464,"273":496,"274":938,"275":954,"278":972,"279":1004,"280":458,"281":490,"282":460,"283":492,"284":728,"285":760,"286":683,"287":699,"288":725,"289":757,"290":939,"291":955,"292":678,"293":694,"294":673,"295":689,"296":933,"297":949,"298":975,"299":1007,"300":16777516,"301":16777517,"302":967,"303":999,"304":681,"305":697,"308":684,"309":700,"310":979,"311":1011,"312":930,"313":453,"314":485,"315":934,"316":950,"317":421,"318":437,"321":419,"322":435,"323":465,"324":497,"325":977,"326":1009,"327":466,"328":498,"330":957,"331":959,"332":978,"333":1010,"336":469,"337":501,"338":5052,"339":5053,"340":448,"341":480,"342":931,"343":947,"344":472,"345":504,"346":422,"347":438,"348":734,"349":766,"350":426,"351":442,"352":425,"353":441,"354":478,"355":510,"356":427,"357":443,"358":940,"359":956,"360":989,"361":1021,"362":990,"363":1022,"364":733,"365":765,"366":473,"367":505,"368":475,"369":507,"370":985,"371":1017,"372":16777588,"373":16777589,"374":16777590,"375":16777591,"376":5054,"377":428,"378":444,"379":431,"380":447,"381":430,"382":446,"399":16777615,"402":2294,"415":16777631,"416":16777632,"417":16777633,"431":16777647,"432":16777648,"437":16777653,"438":16777654,"439":16777655,"466":16777681,"486":16777702,"487":16777703,"601":16777817,"629":16777845,"658":16777874,"711":439,"728":418,"729":511,"731":434,"733":445,"901":1966,"902":1953,"904":1954,"905":1955,"906":1956,"908":1959,"910":1960,"911":1963,"912":1974,"913":1985,"914":1986,"915":1987,"916":1988,"917":1989,"918":1990,"919":1991,"920":1992,"921":1993,"922":1994,"923":1995,"924":1996,"925":1997,"926":1998,"927":1999,"928":2000,"929":2001,"931":2002,"932":2004,"933":2005,"934":2006,"935":2007,"936":2008,"937":2009,"938":1957,"939":1961,"940":1969,"941":1970,"942":1971,"943":1972,"944":1978,"945":2017,"946":2018,"947":2019,"948":2020,"949":2021,"950":2022,"951":2023,"952":2024,"953":2025,"954":2026,"955":2027,"956":2028,"957":2029,"958":2030,"959":2031,"960":2032,"961":2033,"962":2035,"963":2034,"964":2036,"965":2037,"966":2038,"967":2039,"968":2040,"969":2041,"970":1973,"971":1977,"972":1975,"973":1976,"974":1979,"1025":1715,"1026":1713,"1027":1714,"1028":1716,"1029":1717,"1030":1718,"1031":1719,"1032":1720,"1033":1721,"1034":1722,"1035":1723,"1036":1724,"1038":1726,"1039":1727,"1040":1761,"1041":1762,"1042":1783,"1043":1767,"1044":1764,"1045":1765,"1046":1782,"1047":1786,"1048":1769,"1049":1770,"1050":1771,"1051":1772,"1052":1773,"1053":1774,"1054":1775,"1055":1776,"1056":1778,"1057":1779,"1058":1780,"1059":1781,"1060":1766,"1061":1768,"1062":1763,"1063":1790,"1064":1787,"1065":1789,"1066":1791,"1067":1785,"1068":1784,"1069":1788,"1070":1760,"1071":1777,"1072":1729,"1073":1730,"1074":1751,"1075":1735,"1076":1732,"1077":1733,"1078":1750,"1079":1754,"1080":1737,"1081":1738,"1082":1739,"1083":1740,"1084":1741,"1085":1742,"1086":1743,"1087":1744,"1088":1746,"1089":1747,"1090":1748,"1091":1749,"1092":1734,"1093":1736,"1094":1731,"1095":1758,"1096":1755,"1097":1757,"1098":1759,"1099":1753,"1100":1752,"1101":1756,"1102":1728,"1103":1745,"1105":1699,"1106":1697,"1107":1698,"1108":1700,"1109":1701,"1110":1702,"1111":1703,"1112":1704,"1113":1705,"1114":1706,"1115":1707,"1116":1708,"1118":1710,"1119":1711,"1168":1725,"1169":1709,"1170":16778386,"1171":16778387,"1174":16778390,"1175":16778391,"1178":16778394,"1179":16778395,"1180":16778396,"1181":16778397,"1186":16778402,"1187":16778403,"1198":16778414,"1199":16778415,"1200":16778416,"1201":16778417,"1202":16778418,"1203":16778419,"1206":16778422,"1207":16778423,"1208":16778424,"1209":16778425,"1210":16778426,"1211":16778427,"1240":16778456,"1241":16778457,"1250":16778466,"1251":16778467,"1256":16778472,"1257":16778473,"1262":16778478,"1263":16778479,"1329":16778545,"1330":16778546,"1331":16778547,"1332":16778548,"1333":16778549,"1334":16778550,"1335":16778551,"1336":16778552,"1337":16778553,"1338":16778554,"1339":16778555,"1340":16778556,"1341":16778557,"1342":16778558,"1343":16778559,"1344":16778560,"1345":16778561,"1346":16778562,"1347":16778563,"1348":16778564,"1349":16778565,"1350":16778566,"1351":16778567,"1352":16778568,"1353":16778569,"1354":16778570,"1355":16778571,"1356":16778572,"1357":16778573,"1358":16778574,"1359":16778575,"1360":16778576,"1361":16778577,"1362":16778578,"1363":16778579,"1364":16778580,"1365":16778581,"1366":16778582,"1370":16778586,"1371":16778587,"1372":16778588,"1373":16778589,"1374":16778590,"1377":16778593,"1378":16778594,"1379":16778595,"1380":16778596,"1381":16778597,"1382":16778598,"1383":16778599,"1384":16778600,"1385":16778601,"1386":16778602,"1387":16778603,"1388":16778604,"1389":16778605,"1390":16778606,"1391":16778607,"1392":16778608,"1393":16778609,"1394":16778610,"1395":16778611,"1396":16778612,"1397":16778613,"1398":16778614,"1399":16778615,"1400":16778616,"1401":16778617,"1402":16778618,"1403":16778619,"1404":16778620,"1405":16778621,"1406":16778622,"1407":16778623,"1408":16778624,"1409":16778625,"1410":16778626,"1411":16778627,"1412":16778628,"1413":16778629,"1414":16778630,"1415":16778631,"1417":16778633,"1418":16778634,"1488":3296,"1489":3297,"1490":3298,"1491":3299,"1492":3300,"1493":3301,"1494":3302,"1495":3303,"1496":3304,"1497":3305,"1498":3306,"1499":3307,"1500":3308,"1501":3309,"1502":3310,"1503":3311,"1504":3312,"1505":3313,"1506":3314,"1507":3315,"1508":3316,"1509":3317,"1510":3318,"1511":3319,"1512":3320,"1513":3321,"1514":3322,"1548":1452,"1563":1467,"1567":1471,"1569":1473,"1570":1474,"1571":1475,"1572":1476,"1573":1477,"1574":1478,"1575":1479,"1576":1480,"1577":1481,"1578":1482,"1579":1483,"1580":1484,"1581":1485,"1582":1486,"1583":1487,"1584":1488,"1585":1489,"1586":1490,"1587":1491,"1588":1492,"1589":1493,"1590":1494,"1591":1495,"1592":1496,"1593":1497,"1594":1498,"1600":1504,"1601":1505,"1602":1506,"1603":1507,"1604":1508,"1605":1509,"1606":1510,"1607":1511,"1608":1512,"1609":1513,"1610":1514,"1611":1515,"1612":1516,"1613":1517,"1614":1518,"1615":1519,"1616":1520,"1617":1521,"1618":1522,"1619":16778835,"1620":16778836,"1621":16778837,"1632":16778848,"1633":16778849,"1634":16778850,"1635":16778851,"1636":16778852,"1637":16778853,"1638":16778854,"1639":16778855,"1640":16778856,"1641":16778857,"1642":16778858,"1648":16778864,"1657":16778873,"1662":16778878,"1670":16778886,"1672":16778888,"1681":16778897,"1688":16778904,"1700":16778916,"1705":16778921,"1711":16778927,"1722":16778938,"1726":16778942,"1729":16778945,"1740":16778956,"1746":16778962,"1748":16778964,"1776":16778992,"1777":16778993,"1778":16778994,"1779":16778995,"1780":16778996,"1781":16778997,"1782":16778998,"1783":16778999,"1784":16779000,"1785":16779001,"3458":16780674,"3459":16780675,"3461":16780677,"3462":16780678,"3463":16780679,"3464":16780680,"3465":16780681,"3466":16780682,"3467":16780683,"3468":16780684,"3469":16780685,"3470":16780686,"3471":16780687,"3472":16780688,"3473":16780689,"3474":16780690,"3475":16780691,"3476":16780692,"3477":16780693,"3478":16780694,"3482":16780698,"3483":16780699,"3484":16780700,"3485":16780701,"3486":16780702,"3487":16780703,"3488":16780704,"3489":16780705,"3490":16780706,"3491":16780707,"3492":16780708,"3493":16780709,"3494":16780710,"3495":16780711,"3496":16780712,"3497":16780713,"3498":16780714,"3499":16780715,"3500":16780716,"3501":16780717,"3502":16780718,"3503":16780719,"3504":16780720,"3505":16780721,"3507":16780723,"3508":16780724,"3509":16780725,"3510":16780726,"3511":16780727,"3512":16780728,"3513":16780729,"3514":16780730,"3515":16780731,"3517":16780733,"3520":16780736,"3521":16780737,"3522":16780738,"3523":16780739,"3524":16780740,"3525":16780741,"3526":16780742,"3530":16780746,"3535":16780751,"3536":16780752,"3537":16780753,"3538":16780754,"3539":16780755,"3540":16780756,"3542":16780758,"3544":16780760,"3545":16780761,"3546":16780762,"3547":16780763,"3548":16780764,"3549":16780765,"3550":16780766,"3551":16780767,"3570":16780786,"3571":16780787,"3572":16780788,"3585":3489,"3586":3490,"3587":3491,"3588":3492,"3589":3493,"3590":3494,"3591":3495,"3592":3496,"3593":3497,"3594":3498,"3595":3499,"3596":3500,"3597":3501,"3598":3502,"3599":3503,"3600":3504,"3601":3505,"3602":3506,"3603":3507,"3604":3508,"3605":3509,"3606":3510,"3607":3511,"3608":3512,"3609":3513,"3610":3514,"3611":3515,"3612":3516,"3613":3517,"3614":3518,"3615":3519,"3616":3520,"3617":3521,"3618":3522,"3619":3523,"3620":3524,"3621":3525,"3622":3526,"3623":3527,"3624":3528,"3625":3529,"3626":3530,"3627":3531,"3628":3532,"3629":3533,"3630":3534,"3631":3535,"3632":3536,"3633":3537,"3634":3538,"3635":3539,"3636":3540,"3637":3541,"3638":3542,"3639":3543,"3640":3544,"3641":3545,"3642":3546,"3647":3551,"3648":3552,"3649":3553,"3650":3554,"3651":3555,"3652":3556,"3653":3557,"3654":3558,"3655":3559,"3656":3560,"3657":3561,"3658":3562,"3659":3563,"3660":3564,"3661":3565,"3664":3568,"3665":3569,"3666":3570,"3667":3571,"3668":3572,"3669":3573,"3670":3574,"3671":3575,"3672":3576,"3673":3577,"4304":16781520,"4305":16781521,"4306":16781522,"4307":16781523,"4308":16781524,"4309":16781525,"4310":16781526,"4311":16781527,"4312":16781528,"4313":16781529,"4314":16781530,"4315":16781531,"4316":16781532,"4317":16781533,"4318":16781534,"4319":16781535,"4320":16781536,"4321":16781537,"4322":16781538,"4323":16781539,"4324":16781540,"4325":16781541,"4326":16781542,"4327":16781543,"4328":16781544,"4329":16781545,"4330":16781546,"4331":16781547,"4332":16781548,"4333":16781549,"4334":16781550,"4335":16781551,"4336":16781552,"4337":16781553,"4338":16781554,"4339":16781555,"4340":16781556,"4341":16781557,"4342":16781558,"7682":16784898,"7683":16784899,"7690":16784906,"7691":16784907,"7710":16784926,"7711":16784927,"7734":16784950,"7735":16784951,"7744":16784960,"7745":16784961,"7766":16784982,"7767":16784983,"7776":16784992,"7777":16784993,"7786":16785002,"7787":16785003,"7808":16785024,"7809":16785025,"7810":16785026,"7811":16785027,"7812":16785028,"7813":16785029,"7818":16785034,"7819":16785035,"7840":16785056,"7841":16785057,"7842":16785058,"7843":16785059,"7844":16785060,"7845":16785061,"7846":16785062,"7847":16785063,"7848":16785064,"7849":16785065,"7850":16785066,"7851":16785067,"7852":16785068,"7853":16785069,"7854":16785070,"7855":16785071,"7856":16785072,"7857":16785073,"7858":16785074,"7859":16785075,"7860":16785076,"7861":16785077,"7862":16785078,"7863":16785079,"7864":16785080,"7865":16785081,"7866":16785082,"7867":16785083,"7868":16785084,"7869":16785085,"7870":16785086,"7871":16785087,"7872":16785088,"7873":16785089,"7874":16785090,"7875":16785091,"7876":16785092,"7877":16785093,"7878":16785094,"7879":16785095,"7880":16785096,"7881":16785097,"7882":16785098,"7883":16785099,"7884":16785100,"7885":16785101,"7886":16785102,"7887":16785103,"7888":16785104,"7889":16785105,"7890":16785106,"7891":16785107,"7892":16785108,"7893":16785109,"7894":16785110,"7895":16785111,"7896":16785112,"7897":16785113,"7898":16785114,"7899":16785115,"7900":16785116,"7901":16785117,"7902":16785118,"7903":16785119,"7904":16785120,"7905":16785121,"7906":16785122,"7907":16785123,"7908":16785124,"7909":16785125,"7910":16785126,"7911":16785127,"7912":16785128,"7913":16785129,"7914":16785130,"7915":16785131,"7916":16785132,"7917":16785133,"7918":16785134,"7919":16785135,"7920":16785136,"7921":16785137,"7922":16785138,"7923":16785139,"7924":16785140,"7925":16785141,"7926":16785142,"7927":16785143,"7928":16785144,"7929":16785145,"8194":2722,"8195":2721,"8196":2723,"8197":2724,"8199":2725,"8200":2726,"8201":2727,"8202":2728,"8210":2747,"8211":2730,"8212":2729,"8213":1967,"8215":3295,"8216":2768,"8217":2769,"8218":2813,"8220":2770,"8221":2771,"8222":2814,"8224":2801,"8225":2802,"8226":2790,"8229":2735,"8230":2734,"8240":2773,"8242":2774,"8243":2775,"8248":2812,"8254":1150,"8304":16785520,"8308":16785524,"8309":16785525,"8310":16785526,"8311":16785527,"8312":16785528,"8313":16785529,"8320":16785536,"8321":16785537,"8322":16785538,"8323":16785539,"8324":16785540,"8325":16785541,"8326":16785542,"8327":16785543,"8328":16785544,"8329":16785545,"8352":16785568,"8353":16785569,"8354":16785570,"8355":16785571,"8356":16785572,"8357":16785573,"8358":16785574,"8359":16785575,"8360":16785576,"8361":3839,"8362":16785578,"8363":16785579,"8364":8364,"8453":2744,"8470":1712,"8471":2811,"8478":2772,"8482":2761,"8531":2736,"8532":2737,"8533":2738,"8534":2739,"8535":2740,"8536":2741,"8537":2742,"8538":2743,"8539":2755,"8540":2756,"8541":2757,"8542":2758,"8592":2299,"8593":2300,"8594":2301,"8595":2302,"8658":2254,"8660":2253,"8706":2287,"8709":16785925,"8711":2245,"8712":16785928,"8713":16785929,"8715":16785931,"8728":3018,"8730":2262,"8731":16785947,"8732":16785948,"8733":2241,"8734":2242,"8743":2270,"8744":2271,"8745":2268,"8746":2269,"8747":2239,"8748":16785964,"8749":16785965,"8756":2240,"8757":16785973,"8764":2248,"8771":2249,"8773":16785992,"8775":16785991,"8800":2237,"8801":2255,"8802":16786018,"8803":16786019,"8804":2236,"8805":2238,"8834":2266,"8835":2267,"8866":3068,"8867":3036,"8868":3010,"8869":3022,"8968":3027,"8970":3012,"8981":2810,"8992":2212,"8993":2213,"9109":3020,"9115":2219,"9117":2220,"9118":2221,"9120":2222,"9121":2215,"9123":2216,"9124":2217,"9126":2218,"9128":2223,"9132":2224,"9143":2209,"9146":2543,"9147":2544,"9148":2546,"9149":2547,"9225":2530,"9226":2533,"9227":2537,"9228":2531,"9229":2532,"9251":2732,"9252":2536,"9472":2211,"9474":2214,"9484":2210,"9488":2539,"9492":2541,"9496":2538,"9500":2548,"9508":2549,"9516":2551,"9524":2550,"9532":2542,"9618":2529,"9642":2791,"9643":2785,"9644":2779,"9645":2786,"9646":2783,"9647":2767,"9650":2792,"9651":2787,"9654":2781,"9655":2765,"9660":2793,"9661":2788,"9664":2780,"9665":2764,"9670":2528,"9675":2766,"9679":2782,"9702":2784,"9734":2789,"9742":2809,"9747":2762,"9756":2794,"9758":2795,"9792":2808,"9794":2807,"9827":2796,"9829":2798,"9830":2797,"9837":2806,"9839":2805,"10003":2803,"10007":2804,"10013":2777,"10016":2800,"10216":2748,"10217":2750,"10240":16787456,"10241":16787457,"10242":16787458,"10243":16787459,"10244":16787460,"10245":16787461,"10246":16787462,"10247":16787463,"10248":16787464,"10249":16787465,"10250":16787466,"10251":16787467,"10252":16787468,"10253":16787469,"10254":16787470,"10255":16787471,"10256":16787472,"10257":16787473,"10258":16787474,"10259":16787475,"10260":16787476,"10261":16787477,"10262":16787478,"10263":16787479,"10264":16787480,"10265":16787481,"10266":16787482,"10267":16787483,"10268":16787484,"10269":16787485,"10270":16787486,"10271":16787487,"10272":16787488,"10273":16787489,"10274":16787490,"10275":16787491,"10276":16787492,"10277":16787493,"10278":16787494,"10279":16787495,"10280":16787496,"10281":16787497,"10282":16787498,"10283":16787499,"10284":16787500,"10285":16787501,"10286":16787502,"10287":16787503,"10288":16787504,"10289":16787505,"10290":16787506,"10291":16787507,"10292":16787508,"10293":16787509,"10294":16787510,"10295":16787511,"10296":16787512,"10297":16787513,"10298":16787514,"10299":16787515,"10300":16787516,"10301":16787517,"10302":16787518,"10303":16787519,"10304":16787520,"10305":16787521,"10306":16787522,"10307":16787523,"10308":16787524,"10309":16787525,"10310":16787526,"10311":16787527,"10312":16787528,"10313":16787529,"10314":16787530,"10315":16787531,"10316":16787532,"10317":16787533,"10318":16787534,"10319":16787535,"10320":16787536,"10321":16787537,"10322":16787538,"10323":16787539,"10324":16787540,"10325":16787541,"10326":16787542,"10327":16787543,"10328":16787544,"10329":16787545,"10330":16787546,"10331":16787547,"10332":16787548,"10333":16787549,"10334":16787550,"10335":16787551,"10336":16787552,"10337":16787553,"10338":16787554,"10339":16787555,"10340":16787556,"10341":16787557,"10342":16787558,"10343":16787559,"10344":16787560,"10345":16787561,"10346":16787562,"10347":16787563,"10348":16787564,"10349":16787565,"10350":16787566,"10351":16787567,"10352":16787568,"10353":16787569,"10354":16787570,"10355":16787571,"10356":16787572,"10357":16787573,"10358":16787574,"10359":16787575,"10360":16787576,"10361":16787577,"10362":16787578,"10363":16787579,"10364":16787580,"10365":16787581,"10366":16787582,"10367":16787583,"10368":16787584,"10369":16787585,"10370":16787586,"10371":16787587,"10372":16787588,"10373":16787589,"10374":16787590,"10375":16787591,"10376":16787592,"10377":16787593,"10378":16787594,"10379":16787595,"10380":16787596,"10381":16787597,"10382":16787598,"10383":16787599,"10384":16787600,"10385":16787601,"10386":16787602,"10387":16787603,"10388":16787604,"10389":16787605,"10390":16787606,"10391":16787607,"10392":16787608,"10393":16787609,"10394":16787610,"10395":16787611,"10396":16787612,"10397":16787613,"10398":16787614,"10399":16787615,"10400":16787616,"10401":16787617,"10402":16787618,"10403":16787619,"10404":16787620,"10405":16787621,"10406":16787622,"10407":16787623,"10408":16787624,"10409":16787625,"10410":16787626,"10411":16787627,"10412":16787628,"10413":16787629,"10414":16787630,"10415":16787631,"10416":16787632,"10417":16787633,"10418":16787634,"10419":16787635,"10420":16787636,"10421":16787637,"10422":16787638,"10423":16787639,"10424":16787640,"10425":16787641,"10426":16787642,"10427":16787643,"10428":16787644,"10429":16787645,"10430":16787646,"10431":16787647,"10432":16787648,"10433":16787649,"10434":16787650,"10435":16787651,"10436":16787652,"10437":16787653,"10438":16787654,"10439":16787655,"10440":16787656,"10441":16787657,"10442":16787658,"10443":16787659,"10444":16787660,"10445":16787661,"10446":16787662,"10447":16787663,"10448":16787664,"10449":16787665,"10450":16787666,"10451":16787667,"10452":16787668,"10453":16787669,"10454":16787670,"10455":16787671,"10456":16787672,"10457":16787673,"10458":16787674,"10459":16787675,"10460":16787676,"10461":16787677,"10462":16787678,"10463":16787679,"10464":16787680,"10465":16787681,"10466":16787682,"10467":16787683,"10468":16787684,"10469":16787685,"10470":16787686,"10471":16787687,"10472":16787688,"10473":16787689,"10474":16787690,"10475":16787691,"10476":16787692,"10477":16787693,"10478":16787694,"10479":16787695,"10480":16787696,"10481":16787697,"10482":16787698,"10483":16787699,"10484":16787700,"10485":16787701,"10486":16787702,"10487":16787703,"10488":16787704,"10489":16787705,"10490":16787706,"10491":16787707,"10492":16787708,"10493":16787709,"10494":16787710,"10495":16787711,"12289":1188,"12290":1185,"12300":1186,"12301":1187,"12443":1246,"12444":1247,"12449":1191,"12450":1201,"12451":1192,"12452":1202,"12453":1193,"12454":1203,"12455":1194,"12456":1204,"12457":1195,"12458":1205,"12459":1206,"12461":1207,"12463":1208,"12465":1209,"12467":1210,"12469":1211,"12471":1212,"12473":1213,"12475":1214,"12477":1215,"12479":1216,"12481":1217,"12483":1199,"12484":1218,"12486":1219,"12488":1220,"12490":1221,"12491":1222,"12492":1223,"12493":1224,"12494":1225,"12495":1226,"12498":1227,"12501":1228,"12504":1229,"12507":1230,"12510":1231,"12511":1232,"12512":1233,"12513":1234,"12514":1235,"12515":1196,"12516":1236,"12517":1197,"12518":1237,"12519":1198,"12520":1238,"12521":1239,"12522":1240,"12523":1241,"12524":1242,"12525":1243,"12527":1244,"12530":1190,"12531":1245,"12539":1189,"12540":1200};

      function lookup(k) { return k ? {keysym: k, keyname: keynames ? keynames[k] : k} : undefined; }
      return {
          fromUnicode : function(u) { return lookup(codepoints[u]); },
          lookup : lookup
      };
  })();
  ;

  // keyboard
  var kbdUtil = (function() {
      "use strict";

      function substituteCodepoint(cp) {
          // Any Unicode code points which do not have corresponding keysym entries
          // can be swapped out for another code point by adding them to this table
          var substitutions = {
              // {S,s} with comma below -> {S,s} with cedilla
              0x218 : 0x15e,
              0x219 : 0x15f,
              // {T,t} with comma below -> {T,t} with cedilla
              0x21a : 0x162,
              0x21b : 0x163
          };

          var sub = substitutions[cp];
          return sub ? sub : cp;
      };

      function isMac() {
          return navigator && !!(/mac/i).exec(navigator.platform);
      }
      function isWindows() {
          return navigator && !!(/win/i).exec(navigator.platform);
      }
      function isLinux() {
          return navigator && !!(/linux/i).exec(navigator.platform);
      }

      // Return true if a modifier which is not the specified char modifier (and is not shift) is down
      function hasShortcutModifier(charModifier, currentModifiers) {
          var mods = {};
          for (var key in currentModifiers) {
              if (parseInt(key) !== 0xffe1) {
                  mods[key] = currentModifiers[key];
              }
          }

          var sum = 0;
          for (var k in currentModifiers) {
              if (mods[k]) {
                  ++sum;
              }
          }
          if (hasCharModifier(charModifier, mods)) {
              return sum > charModifier.length;
          }
          else {
              return sum > 0;
          }
      }

      // Return true if the specified char modifier is currently down
      function hasCharModifier(charModifier, currentModifiers) {
          if (charModifier.length === 0) { return false; }

          for (var i = 0; i < charModifier.length; ++i) {
              if (!currentModifiers[charModifier[i]]) {
                  return false;
              }
          }
          return true;
      }

      // Helper object tracking modifier key state
      // and generates fake key events to compensate if it gets out of sync
      function ModifierSync(charModifier) {
          var ctrl = 0xffe3;
          var alt = 0xffe9;
          var altGr = 0xfe03;
          var shift = 0xffe1;
          var meta = 0xffe7;

          if (!charModifier) {
              if (isMac()) {
                  // on Mac, Option (AKA Alt) is used as a char modifier
                  charModifier = [alt];
              }
              else if (isWindows()) {
                  // on Windows, Ctrl+Alt is used as a char modifier
                  charModifier = [alt, ctrl];
              }
              else if (isLinux()) {
                  // on Linux, AltGr is used as a char modifier
                  charModifier = [altGr];
              }
              else {
                  charModifier = [];
              }
          }

          var state = {};
          state[ctrl] = false;
          state[alt] = false;
          state[altGr] = false;
          state[shift] = false;
          state[meta] = false;

          function sync(evt, keysym) {
              var result = [];
              function syncKey(keysym) {
                  return {keysym: keysyms.lookup(keysym), type: state[keysym] ? 'keydown' : 'keyup'};
              }

              if (evt.ctrlKey !== undefined && evt.ctrlKey !== state[ctrl] && keysym !== ctrl) {
                  state[ctrl] = evt.ctrlKey;
                  result.push(syncKey(ctrl));
              }
              if (evt.altKey !== undefined && evt.altKey !== state[alt] && keysym !== alt) {
                  state[alt] = evt.altKey;
                  result.push(syncKey(alt));
              }
              if (evt.altGraphKey !== undefined && evt.altGraphKey !== state[altGr] && keysym !== altGr) {
                  state[altGr] = evt.altGraphKey;
                  result.push(syncKey(altGr));
              }
              if (evt.shiftKey !== undefined && evt.shiftKey !== state[shift] && keysym !== shift) {
                  state[shift] = evt.shiftKey;
                  result.push(syncKey(shift));
              }
              if (evt.metaKey !== undefined && evt.metaKey !== state[meta] && keysym !== meta) {
                  state[meta] = evt.metaKey;
                  result.push(syncKey(meta));
              }
              return result;
          }
          function syncKeyEvent(evt, down) {
              var obj = getKeysym(evt);
              var keysym = obj ? obj.keysym : null;

              // first, apply the event itself, if relevant
              if (keysym !== null && state[keysym] !== undefined) {
                  state[keysym] = down;
              }
              return sync(evt, keysym);
          }

          return {
              // sync on the appropriate keyboard event
              keydown: function(evt) { return syncKeyEvent(evt, true);},
              keyup: function(evt) { return syncKeyEvent(evt, false);},
              // Call this with a non-keyboard event (such as mouse events) to use its modifier state to synchronize anyway
              syncAny: function(evt) { return sync(evt);},

              // is a shortcut modifier down?
              hasShortcutModifier: function() { return hasShortcutModifier(charModifier, state); },
              // if a char modifier is down, return the keys it consists of, otherwise return null
              activeCharModifier: function() { return hasCharModifier(charModifier, state) ? charModifier : null; }
          };
      }

      // Get a key ID from a keyboard event
      // May be a string or an integer depending on the available properties
      function getKey(evt){
          if ('keyCode' in evt && 'key' in evt) {
              return evt.key + ':' + evt.keyCode;
          }
          else if ('keyCode' in evt) {
              return evt.keyCode;
          }
          else {
              return evt.key;
          }
      }

      // Get the most reliable keysym value we can get from a key event
      // if char/charCode is available, prefer those, otherwise fall back to key/keyCode/which
      function getKeysym(evt){
          var codepoint;
          if (evt.char && evt.char.length === 1) {
              codepoint = evt.char.charCodeAt();
          }
          else if (evt.charCode) {
              codepoint = evt.charCode;
          }
          else if (evt.keyCode && evt.type === 'keypress') {
              // IE10 stores the char code as keyCode, and has no other useful properties
              codepoint = evt.keyCode;
          }
          if (codepoint) {
              var res = keysyms.fromUnicode(substituteCodepoint(codepoint));
              if (res) {
                  return res;
              }
          }
          // we could check evt.key here.
          // Legal values are defined in http://www.w3.org/TR/DOM-Level-3-Events/#key-values-list,
          // so we "just" need to map them to keysym, but AFAIK this is only available in IE10, which also provides evt.key
          // so we don't *need* it yet
          if (evt.keyCode) {
              return keysyms.lookup(keysymFromKeyCode(evt.keyCode, evt.shiftKey));
          }
          if (evt.which) {
              return keysyms.lookup(keysymFromKeyCode(evt.which, evt.shiftKey));
          }
          return null;
      }

      // Given a keycode, try to predict which keysym it might be.
      // If the keycode is unknown, null is returned.
      function keysymFromKeyCode(keycode, shiftPressed) {
          if (typeof(keycode) !== 'number') {
              return null;
          }
          // won't be accurate for azerty
          if (keycode >= 0x30 && keycode <= 0x39) {
              return keycode; // digit
          }
          if (keycode >= 0x41 && keycode <= 0x5a) {
              // remap to lowercase unless shift is down
              return shiftPressed ? keycode : keycode + 32; // A-Z
          }
          if (keycode >= 0x60 && keycode <= 0x69) {
              return 0xffb0 + (keycode - 0x60); // numpad 0-9
          }

          switch(keycode) {
              case 0x20: return 0x20; // space
              case 0x6a: return 0xffaa; // multiply
              case 0x6b: return 0xffab; // add
              case 0x6c: return 0xffac; // separator
              case 0x6d: return 0xffad; // subtract
              case 0x6e: return 0xffae; // decimal
              case 0x6f: return 0xffaf; // divide
              case 0xbb: return 0x2b; // +
              case 0xbc: return 0x2c; // ,
              case 0xbd: return 0x2d; // -
              case 0xbe: return 0x2e; // .
          }

          return nonCharacterKey({keyCode: keycode});
      }

      // if the key is a known non-character key (any key which doesn't generate character data)
      // return its keysym value. Otherwise return null
      function nonCharacterKey(evt) {
          // evt.key not implemented yet
          if (!evt.keyCode) { return null; }
          var keycode = evt.keyCode;

          if (keycode >= 0x70 && keycode <= 0x87) {
              return 0xffbe + keycode - 0x70; // F1-F24
          }
          switch (keycode) {

              case 8 : return 0xFF08; // BACKSPACE
              case 13 : return 0xFF0D; // ENTER

              case 9 : return 0xFF09; // TAB

              case 27 : return 0xFF1B; // ESCAPE
              case 46 : return 0xFFFF; // DELETE

              case 36 : return 0xFF50; // HOME
              case 35 : return 0xFF57; // END
              case 33 : return 0xFF55; // PAGE_UP
              case 34 : return 0xFF56; // PAGE_DOWN
              case 45 : return 0xFF63; // INSERT

              case 37 : return 0xFF51; // LEFT
              case 38 : return 0xFF52; // UP
              case 39 : return 0xFF53; // RIGHT
              case 40 : return 0xFF54; // DOWN
              case 16 : return 0xFFE1; // SHIFT
              case 17 : return 0xFFE3; // CONTROL
              case 18 : return 0xFFE9; // Left ALT (Mac Option)

              case 224 : return 0xFE07; // Meta
              case 225 : return 0xFE03; // AltGr
              case 91 : return 0xFFEC; // Super_L (Win Key)
              case 92 : return 0xFFED; // Super_R (Win Key)
              case 93 : return 0xFF67; // Menu (Win Menu), Mac Command
              default: return null;
          }
      }
      return {
          hasShortcutModifier : hasShortcutModifier,
          hasCharModifier :  hasCharModifier,
          ModifierSync : ModifierSync,
          getKey : getKey,
          getKeysym : getKeysym,
          keysymFromKeyCode : keysymFromKeyCode,
          nonCharacterKey : nonCharacterKey,
          substituteCodepoint : substituteCodepoint
      };
  })();

  // Takes a DOM keyboard event and:
  // - determines which keysym it represents
  // - determines a keyId  identifying the key that was pressed (corresponding to the key/keyCode properties on the DOM event)
  // - synthesizes events to synchronize modifier key state between which modifiers are actually down, and which we thought were down
  // - marks each event with an 'escape' property if a modifier was down which should be "escaped"
  // - generates a "stall" event in cases where it might be necessary to wait and see if a keypress event follows a keydown
  // This information is collected into an object which is passed to the next() function. (one call per event)
  function KeyEventDecoder(modifierState, next) {
      "use strict";
      function sendAll(evts) {
          for (var i = 0; i < evts.length; ++i) {
              next(evts[i]);
          }
      }
      function process(evt, type) {
          var result = {type: type};
          var keyId = kbdUtil.getKey(evt);
          if (keyId) {
              result.keyId = keyId;
          }

          var keysym = kbdUtil.getKeysym(evt);

          var hasModifier = modifierState.hasShortcutModifier() || !!modifierState.activeCharModifier();
          // Is this a case where we have to decide on the keysym right away, rather than waiting for the keypress?
          // "special" keys like enter, tab or backspace don't send keypress events,
          // and some browsers don't send keypresses at all if a modifier is down
          if (keysym && (type !== 'keydown' || kbdUtil.nonCharacterKey(evt) || hasModifier)) {
              result.keysym = keysym;
          }

          var isShift = evt.keyCode === 0x10 || evt.key === 'Shift';

          // Should we prevent the browser from handling the event?
          // Doing so on a keydown (in most browsers) prevents keypress from being generated
          // so only do that if we have to.
          var suppress = !isShift && (type !== 'keydown' || modifierState.hasShortcutModifier() || !!kbdUtil.nonCharacterKey(evt));

          // If a char modifier is down on a keydown, we need to insert a stall,
          // so VerifyCharModifier knows to wait and see if a keypress is comnig
          var stall = type === 'keydown' && modifierState.activeCharModifier() && !kbdUtil.nonCharacterKey(evt);

          // if a char modifier is pressed, get the keys it consists of (on Windows, AltGr is equivalent to Ctrl+Alt)
          var active = modifierState.activeCharModifier();

          // If we have a char modifier down, and we're able to determine a keysym reliably
          // then (a) we know to treat the modifier as a char modifier,
          // and (b) we'll have to "escape" the modifier to undo the modifier when sending the char.
          if (active && keysym) {
              var isCharModifier = false;
              for (var i  = 0; i < active.length; ++i) {
                  if (active[i] === keysym.keysym) {
                      isCharModifier = true;
                  }
              }
              if (type === 'keypress' && !isCharModifier) {
                  result.escape = modifierState.activeCharModifier();
              }
          }

          if (stall) {
              // insert a fake "stall" event
              next({type: 'stall'});
          }
          next(result);

          return suppress;
      }

      return {
          keydown: function(evt) {
              sendAll(modifierState.keydown(evt));
              return process(evt, 'keydown');
          },
          keypress: function(evt) {
              return process(evt, 'keypress');
          },
          keyup: function(evt) {
              sendAll(modifierState.keyup(evt));
              return process(evt, 'keyup');
          },
          syncModifiers: function(evt) {
              sendAll(modifierState.syncAny(evt));
          },
          releaseAll: function() { next({type: 'releaseall'}); }
      };
  }

  // Combines keydown and keypress events where necessary to handle char modifiers.
  // On some OS'es, a char modifier is sometimes used as a shortcut modifier.
  // For example, on Windows, AltGr is synonymous with Ctrl-Alt. On a Danish keyboard layout, AltGr-2 yields a @, but Ctrl-Alt-D does nothing
  // so when used with the '2' key, Ctrl-Alt counts as a char modifier (and should be escaped), but when used with 'D', it does not.
  // The only way we can distinguish these cases is to wait and see if a keypress event arrives
  // When we receive a "stall" event, wait a few ms before processing the next keydown. If a keypress has also arrived, merge the two
  function VerifyCharModifier(next) {
      "use strict";
      var queue = [];
      var timer = null;
      function process() {
          if (timer) {
              return;
          }
          while (queue.length !== 0) {
              var cur = queue[0];
              queue = queue.splice(1);
              switch (cur.type) {
              case 'stall':
                  // insert a delay before processing available events.
                  timer = setTimeout(function() {
                      clearTimeout(timer);
                      timer = null;
                      process();
                  }, 5);
                  return;
              case 'keydown':
                  // is the next element a keypress? Then we should merge the two
                  if (queue.length !== 0 && queue[0].type === 'keypress') {
                      // Firefox sends keypress even when no char is generated.
                      // so, if keypress keysym is the same as we'd have guessed from keydown,
                      // the modifier didn't have any effect, and should not be escaped
                      if (queue[0].escape && (!cur.keysym || cur.keysym.keysym !== queue[0].keysym.keysym)) {
                          cur.escape = queue[0].escape;
                      }
                      cur.keysym = queue[0].keysym;
                      queue = queue.splice(1);
                  }
                  break;
              }

              // swallow stall events, and pass all others to the next stage
              if (cur.type !== 'stall') {
                  next(cur);
              }
          }
      }
      return function(evt) {
          queue.push(evt);
          process();
      };
  }

  // Keeps track of which keys we (and the server) believe are down
  // When a keyup is received, match it against this list, to determine the corresponding keysym(s)
  // in some cases, a single key may produce multiple keysyms, so the corresponding keyup event must release all of these chars
  // key repeat events should be merged into a single entry.
  // Because we can't always identify which entry a keydown or keyup event corresponds to, we sometimes have to guess
  function TrackKeyState(next) {
      "use strict";
      var state = [];

      return function (evt) {
          var last = state.length !== 0 ? state[state.length-1] : null;

          switch (evt.type) {
          case 'keydown':
              // insert a new entry if last seen key was different.
              if (!last || !evt.keyId || last.keyId !== evt.keyId) {
                  last = {keyId: evt.keyId, keysyms: {}};
                  state.push(last);
              }
              if (evt.keysym) {
                  // make sure last event contains this keysym (a single "logical" keyevent
                  // can cause multiple key events to be sent to the VNC server)
                  last.keysyms[evt.keysym.keysym] = evt.keysym;
                  last.ignoreKeyPress = true;
                  next(evt);
              }
              break;
          case 'keypress':
              if (!last) {
                  last = {keyId: evt.keyId, keysyms: {}};
                  state.push(last);
              }
              if (!evt.keysym) {
                  console.log('keypress with no keysym:', evt);
              }

              // If we didn't expect a keypress, and already sent a keydown to the VNC server
              // based on the keydown, make sure to skip this event.
              if (evt.keysym && !last.ignoreKeyPress) {
                  last.keysyms[evt.keysym.keysym] = evt.keysym;
                  evt.type = 'keydown';
                  next(evt);
              }
              break;
          case 'keyup':
              if (state.length === 0) {
                  return;
              }
              var idx = null;
              // do we have a matching key tracked as being down?
              for (var i = 0; i !== state.length; ++i) {
                  if (state[i].keyId === evt.keyId) {
                      idx = i;
                      break;
                  }
              }
              // if we couldn't find a match (it happens), assume it was the last key pressed
              if (idx === null) {
                  idx = state.length - 1;
              }

              var item = state.splice(idx, 1)[0];
              // for each keysym tracked by this key entry, clone the current event and override the keysym
              for (var key in item.keysyms) {
                  var clone = (function(){
                      function Clone(){}
                      return function (obj) { Clone.prototype=obj; return new Clone(); };
                  }());
                  var out = clone(evt);
                  out.keysym = item.keysyms[key];
                  next(out);
              }
              break;
          case 'releaseall':
              for (var i = 0; i < state.length; ++i) {
                  for (var key in state[i].keysyms) {
                      var keysym = state[i].keysyms[key];
                      next({keyId: 0, keysym: keysym, type: 'keyup'});
                  }
              }
              state = [];
          }
      };
  }

  // Handles "escaping" of modifiers: if a char modifier is used to produce a keysym (such as AltGr-2 to generate an @),
  // then the modifier must be "undone" before sending the @, and "redone" afterwards.
  function EscapeModifiers(next) {
      "use strict";
      return function(evt) {
          if (evt.type !== 'keydown' || evt.escape === undefined) {
              next(evt);
              return;
          }
          // undo modifiers
          for (var i = 0; i < evt.escape.length; ++i) {
              next({type: 'keyup', keyId: 0, keysym: keysyms.lookup(evt.escape[i])});
          }
          // send the character event
          next(evt);
          // redo modifiers
          for (var i = 0; i < evt.escape.length; ++i) {
              next({type: 'keydown', keyId: 0, keysym: keysyms.lookup(evt.escape[i])});
          }
      };
  }
  ;

  // input
  /*
  * noVNC: HTML5 VNC client
  * Copyright (C) 2012 Joel Martin
  * Copyright (C) 2013 Samuel Mannehed for Cendio AB
  * Licensed under MPL 2.0 or any later version (see LICENSE.txt)
  */

  /*jslint browser: true, white: false, bitwise: false */
  /*global window, Util */


  //
  // Keyboard event handler
  //

  function Keyboard(defaults) {
  "use strict";

  var that           = {},  // Public API methods
      conf           = {},  // Configuration attributes

      keyDownList    = [];         // List of depressed keys 
                                  // (even if they are happy)

  // Configuration attributes
  Util.conf_defaults(conf, that, defaults, [
      ['target',      'wo', 'dom',  document, 'DOM element that captures keyboard input'],
      ['focused',     'rw', 'bool', true, 'Capture and send key events'],

      ['onKeyPress',  'rw', 'func', null, 'Handler for key press/release']
      ]);


  // 
  // Private functions
  //

  /////// setup

  function onRfbEvent(evt) {
      if (conf.onKeyPress) {
          Util.Debug("onKeyPress " + (evt.type == 'keydown' ? "down" : "up")
          + ", keysym: " + evt.keysym.keysym + "(" + evt.keysym.keyname + ")");
          conf.onKeyPress(evt.keysym.keysym, evt.type == 'keydown');
      }
  }

  // create the keyboard handler
  var k = KeyEventDecoder(kbdUtil.ModifierSync(),
      VerifyCharModifier(
          TrackKeyState(
              EscapeModifiers(onRfbEvent)
          )
      )
  );

  function onKeyDown(e) {
      if (! conf.focused) {
          return true;
      }
      if (k.keydown(e)) {
          // Suppress bubbling/default actions
          Util.stopEvent(e);
          return false;
      } else {
          // Allow the event to bubble and become a keyPress event which
          // will have the character code translated
          return true;
      }
  }
  function onKeyPress(e) {
      if (! conf.focused) {
          return true;
      }
      if (k.keypress(e)) {
          // Suppress bubbling/default actions
          Util.stopEvent(e);
          return false;
      } else {
          // Allow the event to bubble and become a keyPress event which
          // will have the character code translated
          return true;
      }
  }

  function onKeyUp(e) {
      if (! conf.focused) {
          return true;
      }
      if (k.keyup(e)) {
          // Suppress bubbling/default actions
          Util.stopEvent(e);
          return false;
      } else {
          // Allow the event to bubble and become a keyPress event which
          // will have the character code translated
          return true;
      }
  }

  function onOther(e) {
      k.syncModifiers(e);
  }

  function allKeysUp() {
      Util.Debug(">> Keyboard.allKeysUp");

      k.releaseAll();
      Util.Debug("<< Keyboard.allKeysUp");
  }

  //
  // Public API interface functions
  //

  that.grab = function() {
      //Util.Debug(">> Keyboard.grab");
      var c = conf.target;

      Util.addEvent(c, 'keydown', onKeyDown);
      Util.addEvent(c, 'keyup', onKeyUp);
      Util.addEvent(c, 'keypress', onKeyPress);

      // Release (key up) if window loses focus
      Util.addEvent(window, 'blur', allKeysUp);

      //Util.Debug("<< Keyboard.grab");
  };

  that.ungrab = function() {
      //Util.Debug(">> Keyboard.ungrab");
      var c = conf.target;

      Util.removeEvent(c, 'keydown', onKeyDown);
      Util.removeEvent(c, 'keyup', onKeyUp);
      Util.removeEvent(c, 'keypress', onKeyPress);
      Util.removeEvent(window, 'blur', allKeysUp);

      // Release (key up) all keys that are in a down state
      allKeysUp();

      //Util.Debug(">> Keyboard.ungrab");
  };

  that.sync = function(e) {
      k.syncModifiers(e);
  }

  return that;  // Return the public API interface

  }  // End of Keyboard()


  //
  // Mouse event handler
  //

  function Mouse(defaults) {
  "use strict";

  var that           = {},  // Public API methods
      conf           = {},  // Configuration attributes
      mouseCaptured  = false;

  var doubleClickTimer = null,
      lastTouchPos = null;

  // Configuration attributes
  Util.conf_defaults(conf, that, defaults, [
      ['target',         'ro', 'dom',  document, 'DOM element that captures mouse input'],
      ['notify',         'ro', 'func',  null, 'Function to call to notify whenever a mouse event is received'],
      ['focused',        'rw', 'bool', true, 'Capture and send mouse clicks/movement'],
      ['scale',          'rw', 'float', 1.0, 'Viewport scale factor 0.0 - 1.0'],

      ['onMouseButton',  'rw', 'func', null, 'Handler for mouse button click/release'],
      ['onMouseMove',    'rw', 'func', null, 'Handler for mouse movement'],
      ['touchButton',    'rw', 'int', 1, 'Button mask (1, 2, 4) for touch devices (0 means ignore clicks)']
      ]);

  function captureMouse() {
      // capturing the mouse ensures we get the mouseup event
      if (conf.target.setCapture) {
          conf.target.setCapture();
      }

      // some browsers give us mouseup events regardless,
      // so if we never captured the mouse, we can disregard the event
      mouseCaptured = true;
  }

  function releaseMouse() {
      if (conf.target.releaseCapture) {
          conf.target.releaseCapture();
      }
      mouseCaptured = false;
  }
  // 
  // Private functions
  //

  function resetDoubleClickTimer() {
      doubleClickTimer = null;
  }

  function onMouseButton(e, down) {
      var evt, pos, bmask;
      if (! conf.focused) {
          return true;
      }

      if (conf.notify) {
          conf.notify(e);
      }

      evt = (e ? e : window.event);
      pos = Util.getEventPosition(e, conf.target, conf.scale);

      if (e.touches || e.changedTouches) {
          // Touch device

          // When two touches occur within 500 ms of each other and are
          // closer than 20 pixels together a double click is triggered.
          if (down == 1) {
              if (doubleClickTimer == null) {
                  lastTouchPos = pos;
              } else {
                  clearTimeout(doubleClickTimer); 

                  // When the distance between the two touches is small enough
                  // force the position of the latter touch to the position of
                  // the first.

                  var xs = lastTouchPos.x - pos.x;
                  var ys = lastTouchPos.y - pos.y;
                  var d = Math.sqrt((xs * xs) + (ys * ys));

                  // The goal is to trigger on a certain physical width, the
                  // devicePixelRatio brings us a bit closer but is not optimal.
                  if (d < 20 * window.devicePixelRatio) {
                      pos = lastTouchPos;
                  }
              }
              doubleClickTimer = setTimeout(resetDoubleClickTimer, 500);
          }
          bmask = conf.touchButton;
          // If bmask is set
      } else if (evt.which) {
          /* everything except IE */
          bmask = 1 << evt.button;
      } else {
          /* IE including 9 */
          bmask = (evt.button & 0x1) +      // Left
                  (evt.button & 0x2) * 2 +  // Right
                  (evt.button & 0x4) / 2;   // Middle
      }
      //Util.Debug("mouse " + pos.x + "," + pos.y + " down: " + down +
      //           " bmask: " + bmask + "(evt.button: " + evt.button + ")");
      if (conf.onMouseButton) {
          Util.Debug("onMouseButton " + (down ? "down" : "up") +
                    ", x: " + pos.x + ", y: " + pos.y + ", bmask: " + bmask);
          conf.onMouseButton(pos.x, pos.y, down, bmask);
      }
      Util.stopEvent(e);
      return false;
  }

  function onMouseDown(e) {
      captureMouse();
      onMouseButton(e, 1);
  }

  function onMouseUp(e) {
      if (!mouseCaptured) {
          return;
      }

      onMouseButton(e, 0);
      releaseMouse();
  }

  function onMouseWheel(e) {
      var evt, pos, bmask, wheelData;
      if (! conf.focused) {
          return true;
      }
      if (conf.notify) {
          conf.notify(e);
      }

      evt = (e ? e : window.event);
      pos = Util.getEventPosition(e, conf.target, conf.scale);
      wheelData = evt.detail ? evt.detail * -1 : evt.wheelDelta / 40;
      if (wheelData > 0) {
          bmask = 1 << 3;
      } else {
          bmask = 1 << 4;
      }
      //Util.Debug('mouse scroll by ' + wheelData + ':' + pos.x + "," + pos.y);
      if (conf.onMouseButton) {
          conf.onMouseButton(pos.x, pos.y, 1, bmask);
          conf.onMouseButton(pos.x, pos.y, 0, bmask);
      }
      Util.stopEvent(e);
      return false;
  }

  function onMouseMove(e) {
      var evt, pos;
      if (! conf.focused) {
          return true;
      }
      if (conf.notify) {
          conf.notify(e);
      }

      evt = (e ? e : window.event);
      pos = Util.getEventPosition(e, conf.target, conf.scale);
      //Util.Debug('mouse ' + evt.which + '/' + evt.button + ' up:' + pos.x + "," + pos.y);
      if (conf.onMouseMove) {
          conf.onMouseMove(pos.x, pos.y);
      }
      Util.stopEvent(e);
      return false;
  }

  function onMouseDisable(e) {
      var evt, pos;
      if (! conf.focused) {
          return true;
      }
      evt = (e ? e : window.event);
      pos = Util.getEventPosition(e, conf.target, conf.scale);
      /* Stop propagation if inside canvas area */
      if ((pos.realx >= 0) && (pos.realy >= 0) &&
          (pos.realx < conf.target.offsetWidth) &&
          (pos.realy < conf.target.offsetHeight)) {
          //Util.Debug("mouse event disabled");
          Util.stopEvent(e);
          return false;
      }
      //Util.Debug("mouse event not disabled");
      return true;
  }

  //
  // Public API interface functions
  //

  that.grab = function() {
      //Util.Debug(">> Mouse.grab");
      var c = conf.target;

      if ('ontouchstart' in document.documentElement) {
          Util.addEvent(c, 'touchstart', onMouseDown);
          Util.addEvent(window, 'touchend', onMouseUp);
          Util.addEvent(c, 'touchend', onMouseUp);
          Util.addEvent(c, 'touchmove', onMouseMove);
      } else {
          Util.addEvent(c, 'mousedown', onMouseDown);
          Util.addEvent(window, 'mouseup', onMouseUp);
          Util.addEvent(c, 'mouseup', onMouseUp);
          Util.addEvent(c, 'mousemove', onMouseMove);
          Util.addEvent(c, (Util.Engine.gecko) ? 'DOMMouseScroll' : 'mousewheel',
                  onMouseWheel);
      }

      /* Work around right and middle click browser behaviors */
      Util.addEvent(document, 'click', onMouseDisable);
      Util.addEvent(document.body, 'contextmenu', onMouseDisable);

      //Util.Debug("<< Mouse.grab");
  };

  that.ungrab = function() {
      //Util.Debug(">> Mouse.ungrab");
      var c = conf.target;

      if ('ontouchstart' in document.documentElement) {
          Util.removeEvent(c, 'touchstart', onMouseDown);
          Util.removeEvent(window, 'touchend', onMouseUp);
          Util.removeEvent(c, 'touchend', onMouseUp);
          Util.removeEvent(c, 'touchmove', onMouseMove);
      } else {
          Util.removeEvent(c, 'mousedown', onMouseDown);
          Util.removeEvent(window, 'mouseup', onMouseUp);
          Util.removeEvent(c, 'mouseup', onMouseUp);
          Util.removeEvent(c, 'mousemove', onMouseMove);
          Util.removeEvent(c, (Util.Engine.gecko) ? 'DOMMouseScroll' : 'mousewheel',
                  onMouseWheel);
      }

      /* Work around right and middle click browser behaviors */
      Util.removeEvent(document, 'click', onMouseDisable);
      Util.removeEvent(document.body, 'contextmenu', onMouseDisable);

      //Util.Debug(">> Mouse.ungrab");
  };

  return that;  // Return the public API interface

  }  // End of Mouse()
  ;

  // display
  /*
  * noVNC: HTML5 VNC client
  * Copyright (C) 2012 Joel Martin
  * Licensed under MPL 2.0 (see LICENSE.txt)
  *
  * See README.md for usage and integration instructions.
  */

  /*jslint browser: true, white: false, bitwise: false */
  /*global Util, Base64, changeCursor */

  function Display(defaults) {
  "use strict";

  var that           = {},  // Public API methods
      conf           = {},  // Configuration attributes

      // Private Display namespace variables
      c_ctx          = null,
      c_forceCanvas  = false,

      // Queued drawing actions for in-order rendering
      renderQ        = [],

      // Predefine function variables (jslint)
      imageDataGet, rgbImageData, bgrxImageData, cmapImageData,
      setFillColor, rescale, scan_renderQ,

      // The full frame buffer (logical canvas) size
      fb_width        = 0,
      fb_height       = 0,
      // The visible "physical canvas" viewport
      viewport       = {'x': 0, 'y': 0, 'w' : 0, 'h' : 0 },
      cleanRect      = {'x1': 0, 'y1': 0, 'x2': -1, 'y2': -1},

      c_prevStyle    = "",
      tile           = null,
      tile16x16      = null,
      tile_x         = 0,
      tile_y         = 0;


  // Configuration attributes
  Util.conf_defaults(conf, that, defaults, [
      ['target',      'wo', 'dom',  null, 'Canvas element for rendering'],
      ['context',     'ro', 'raw',  null, 'Canvas 2D context for rendering (read-only)'],
      ['logo',        'rw', 'raw',  null, 'Logo to display when cleared: {"width": width, "height": height, "data": data}'],
      ['true_color',  'rw', 'bool', true, 'Use true-color pixel data'],
      ['colourMap',   'rw', 'arr',  [], 'Colour map array (when not true-color)'],
      ['scale',       'rw', 'float', 1.0, 'Display area scale factor 0.0 - 1.0'],
      ['viewport',    'rw', 'bool', false, 'Use a viewport set with viewportChange()'],
      ['width',       'rw', 'int', null, 'Display area width'],
      ['height',      'rw', 'int', null, 'Display area height'],

      ['render_mode', 'ro', 'str', '', 'Canvas rendering mode (read-only)'],

      ['prefer_js',   'rw', 'str', null, 'Prefer Javascript over canvas methods'],
      ['cursor_uri',  'rw', 'raw', null, 'Can we render cursor using data URI']
      ]);

  // Override some specific getters/setters
  that.get_context = function () { return c_ctx; };

  that.set_scale = function(scale) { rescale(scale); };

  that.set_width = function (val) { that.resize(val, fb_height); };
  that.get_width = function() { return fb_width; };

  that.set_height = function (val) { that.resize(fb_width, val); };
  that.get_height = function() { return fb_height; };



  //
  // Private functions
  //

  // Create the public API interface
  function constructor() {
      Util.Debug(">> Display.constructor");

      var c, func, i, curDat, curSave,
          has_imageData = false, UE = Util.Engine;

      if (! conf.target) { throw("target must be set"); }

      if (typeof conf.target === 'string') {
          throw("target must be a DOM element");
      }

      c = conf.target;

      if (! c.getContext) { throw("no getContext method"); }

      if (! c_ctx) { c_ctx = c.getContext('2d'); }

      Util.Debug("User Agent: " + navigator.userAgent);
      if (UE.gecko) { Util.Debug("Browser: gecko " + UE.gecko); }
      if (UE.webkit) { Util.Debug("Browser: webkit " + UE.webkit); }
      if (UE.trident) { Util.Debug("Browser: trident " + UE.trident); }
      if (UE.presto) { Util.Debug("Browser: presto " + UE.presto); }

      that.clear();

      // Check canvas features
      if ('createImageData' in c_ctx) {
          conf.render_mode = "canvas rendering";
      } else {
          throw("Canvas does not support createImageData");
      }
      if (conf.prefer_js === null) {
          Util.Info("Prefering javascript operations");
          conf.prefer_js = true;
      }

      // Initialize cached tile imageData
      tile16x16 = c_ctx.createImageData(16, 16);

      /*
      * Determine browser support for setting the cursor via data URI
      * scheme
      */
      curDat = [];
      for (i=0; i < 8 * 8 * 4; i += 1) {
          curDat.push(255);
      }
      try {
          curSave = c.style.cursor;
          changeCursor(conf.target, curDat, curDat, 2, 2, 8, 8);
          if (c.style.cursor) {
              if (conf.cursor_uri === null) {
                  conf.cursor_uri = true;
              }
              Util.Info("Data URI scheme cursor supported");
          } else {
              if (conf.cursor_uri === null) {
                  conf.cursor_uri = false;
              }
              Util.Warn("Data URI scheme cursor not supported");
          }
          c.style.cursor = curSave;
      } catch (exc2) { 
          Util.Error("Data URI scheme cursor test exception: " + exc2);
          conf.cursor_uri = false;
      }

      Util.Debug("<< Display.constructor");
      return that ;
  }

  rescale = function(factor) {
      var c, tp, x, y, 
          properties = ['transform', 'WebkitTransform', 'MozTransform', null];
      c = conf.target;
      tp = properties.shift();
      while (tp) {
          if (typeof c.style[tp] !== 'undefined') {
              break;
          }
          tp = properties.shift();
      }

      if (tp === null) {
          Util.Debug("No scaling support");
          return;
      }


      if (typeof(factor) === "undefined") {
          factor = conf.scale;
      } else if (factor > 1.0) {
          factor = 1.0;
      } else if (factor < 0.1) {
          factor = 0.1;
      }

      if (conf.scale === factor) {
          //Util.Debug("Display already scaled to '" + factor + "'");
          return;
      }

      conf.scale = factor;
      x = c.width - c.width * factor;
      y = c.height - c.height * factor;
      c.style[tp] = "scale(" + conf.scale + ") translate(-" + x + "px, -" + y + "px)";
  };

  setFillColor = function(color) {
      var bgr, newStyle;
      if (conf.true_color) {
          bgr = color;
      } else {
          bgr = conf.colourMap[color[0]];
      }
      newStyle = "rgb(" + bgr[2] + "," + bgr[1] + "," + bgr[0] + ")";
      if (newStyle !== c_prevStyle) {
          c_ctx.fillStyle = newStyle;
          c_prevStyle = newStyle;
      }
  };


  //
  // Public API interface functions
  //

  // Shift and/or resize the visible viewport
  that.viewportChange = function(deltaX, deltaY, width, height) {
      var c = conf.target, v = viewport, cr = cleanRect,
          saveImg = null, saveStyle, x1, y1, vx2, vy2, w, h;

      if (!conf.viewport) {
          Util.Debug("Setting viewport to full display region");
          deltaX = -v.w; // Clamped later if out of bounds
          deltaY = -v.h; // Clamped later if out of bounds
          width = fb_width;
          height = fb_height;
      }

      if (typeof(deltaX) === "undefined") { deltaX = 0; }
      if (typeof(deltaY) === "undefined") { deltaY = 0; }
      if (typeof(width) === "undefined") { width = v.w; }
      if (typeof(height) === "undefined") { height = v.h; }

      // Size change

      if (width > fb_width) { width = fb_width; }
      if (height > fb_height) { height = fb_height; }

      if ((v.w !== width) || (v.h !== height)) {
          // Change width
          if ((width < v.w) && (cr.x2 > v.x + width -1)) {
              cr.x2 = v.x + width - 1;
          }
          v.w = width;

          // Change height
          if ((height < v.h) && (cr.y2 > v.y + height -1)) {
              cr.y2 = v.y + height - 1;
          }
          v.h = height;


          if (v.w > 0 && v.h > 0 && c.width > 0 && c.height > 0) {
              saveImg = c_ctx.getImageData(0, 0,
                      (c.width < v.w) ? c.width : v.w,
                      (c.height < v.h) ? c.height : v.h);
          }

          c.width = v.w;
          c.height = v.h;

          if (saveImg) {
              c_ctx.putImageData(saveImg, 0, 0);
          }
      }

      vx2 = v.x + v.w - 1;
      vy2 = v.y + v.h - 1;


      // Position change

      if ((deltaX < 0) && ((v.x + deltaX) < 0)) {
          deltaX = - v.x;
      }
      if ((vx2 + deltaX) >= fb_width) {
          deltaX -= ((vx2 + deltaX) - fb_width + 1);
      }

      if ((v.y + deltaY) < 0) {
          deltaY = - v.y;
      }
      if ((vy2 + deltaY) >= fb_height) {
          deltaY -= ((vy2 + deltaY) - fb_height + 1);
      }

      if ((deltaX === 0) && (deltaY === 0)) {
          //Util.Debug("skipping viewport change");
          return;
      }
      Util.Debug("viewportChange deltaX: " + deltaX + ", deltaY: " + deltaY);

      v.x += deltaX;
      vx2 += deltaX;
      v.y += deltaY;
      vy2 += deltaY;

      // Update the clean rectangle
      if (v.x > cr.x1) {
          cr.x1 = v.x;
      }
      if (vx2 < cr.x2) {
          cr.x2 = vx2;
      }
      if (v.y > cr.y1) {
          cr.y1 = v.y;
      }
      if (vy2 < cr.y2) {
          cr.y2 = vy2;
      }

      if (deltaX < 0) {
          // Shift viewport left, redraw left section
          x1 = 0;
          w = - deltaX;
      } else {
          // Shift viewport right, redraw right section
          x1 = v.w - deltaX;
          w = deltaX;
      }
      if (deltaY < 0) {
          // Shift viewport up, redraw top section
          y1 = 0;
          h = - deltaY;
      } else {
          // Shift viewport down, redraw bottom section
          y1 = v.h - deltaY;
          h = deltaY;
      }

      // Copy the valid part of the viewport to the shifted location
      saveStyle = c_ctx.fillStyle;
      c_ctx.fillStyle = "rgb(255,255,255)";
      if (deltaX !== 0) {
          //that.copyImage(0, 0, -deltaX, 0, v.w, v.h);
          //that.fillRect(x1, 0, w, v.h, [255,255,255]);
          c_ctx.drawImage(c, 0, 0, v.w, v.h, -deltaX, 0, v.w, v.h);
          c_ctx.fillRect(x1, 0, w, v.h);
      }
      if (deltaY !== 0) {
          //that.copyImage(0, 0, 0, -deltaY, v.w, v.h);
          //that.fillRect(0, y1, v.w, h, [255,255,255]);
          c_ctx.drawImage(c, 0, 0, v.w, v.h, 0, -deltaY, v.w, v.h);
          c_ctx.fillRect(0, y1, v.w, h);
      }
      c_ctx.fillStyle = saveStyle;
  };


  // Return a map of clean and dirty areas of the viewport and reset the
  // tracking of clean and dirty areas.
  //
  // Returns: {'cleanBox':   {'x': x, 'y': y, 'w': w, 'h': h},
  //           'dirtyBoxes': [{'x': x, 'y': y, 'w': w, 'h': h}, ...]}
  that.getCleanDirtyReset = function() {
      var v = viewport, c = cleanRect, cleanBox, dirtyBoxes = [],
          vx2 = v.x + v.w - 1, vy2 = v.y + v.h - 1;


      // Copy the cleanRect
      cleanBox = {'x': c.x1, 'y': c.y1,
                  'w': c.x2 - c.x1 + 1, 'h': c.y2 - c.y1 + 1};

      if ((c.x1 >= c.x2) || (c.y1 >= c.y2)) {
          // Whole viewport is dirty
          dirtyBoxes.push({'x': v.x, 'y': v.y, 'w': v.w, 'h': v.h});
      } else {
          // Redraw dirty regions
          if (v.x < c.x1) {
              // left side dirty region
              dirtyBoxes.push({'x': v.x, 'y': v.y,
                              'w': c.x1 - v.x + 1, 'h': v.h});
          }
          if (vx2 > c.x2) {
              // right side dirty region
              dirtyBoxes.push({'x': c.x2 + 1, 'y': v.y,
                              'w': vx2 - c.x2, 'h': v.h});
          }
          if (v.y < c.y1) {
              // top/middle dirty region
              dirtyBoxes.push({'x': c.x1, 'y': v.y,
                              'w': c.x2 - c.x1 + 1, 'h': c.y1 - v.y});
          }
          if (vy2 > c.y2) {
              // bottom/middle dirty region
              dirtyBoxes.push({'x': c.x1, 'y': c.y2 + 1,
                              'w': c.x2 - c.x1 + 1, 'h': vy2 - c.y2});
          }
      }

      // Reset the cleanRect to the whole viewport
      cleanRect = {'x1': v.x, 'y1': v.y,
                  'x2': v.x + v.w - 1, 'y2': v.y + v.h - 1};

      return {'cleanBox': cleanBox, 'dirtyBoxes': dirtyBoxes};
  };

  // Translate viewport coordinates to absolute coordinates
  that.absX = function(x) {
      return x + viewport.x;
  };
  that.absY = function(y) {
      return y + viewport.y;
  };


  that.resize = function(width, height) {
      c_prevStyle    = "";

      fb_width = width;
      fb_height = height;

      rescale(conf.scale);
      that.viewportChange();
  };

  that.clear = function() {

      if (conf.logo) {
          that.resize(conf.logo.width, conf.logo.height);
          that.blitStringImage(conf.logo.data, 0, 0);
      } else {
          that.resize(640, 20);
          c_ctx.clearRect(0, 0, viewport.w, viewport.h);
      }

      renderQ = [];

      // No benefit over default ("source-over") in Chrome and firefox
      //c_ctx.globalCompositeOperation = "copy";
  };

  that.fillRect = function(x, y, width, height, color) {
      setFillColor(color);
      c_ctx.fillRect(x - viewport.x, y - viewport.y, width, height);
  };

  that.copyImage = function(old_x, old_y, new_x, new_y, w, h) {
      var x1 = old_x - viewport.x, y1 = old_y - viewport.y,
          x2 = new_x - viewport.x, y2 = new_y  - viewport.y;
      c_ctx.drawImage(conf.target, x1, y1, w, h, x2, y2, w, h);
  };


  // Start updating a tile
  that.startTile = function(x, y, width, height, color) {
      var data, bgr, red, green, blue, i;
      tile_x = x;
      tile_y = y;
      if ((width === 16) && (height === 16)) {
          tile = tile16x16;
      } else {
          tile = c_ctx.createImageData(width, height);
      }
      data = tile.data;
      if (conf.prefer_js) {
          if (conf.true_color) {
              bgr = color;
          } else {
              bgr = conf.colourMap[color[0]];
          }
          red = bgr[2];
          green = bgr[1];
          blue = bgr[0];
          for (i = 0; i < (width * height * 4); i+=4) {
              data[i    ] = red;
              data[i + 1] = green;
              data[i + 2] = blue;
              data[i + 3] = 255;
          }
      } else {
          that.fillRect(x, y, width, height, color);
      }
  };

  // Update sub-rectangle of the current tile
  that.subTile = function(x, y, w, h, color) {
      var data, p, bgr, red, green, blue, width, j, i, xend, yend;
      if (conf.prefer_js) {
          data = tile.data;
          width = tile.width;
          if (conf.true_color) {
              bgr = color;
          } else {
              bgr = conf.colourMap[color[0]];
          }
          red = bgr[2];
          green = bgr[1];
          blue = bgr[0];
          xend = x + w;
          yend = y + h;
          for (j = y; j < yend; j += 1) {
              for (i = x; i < xend; i += 1) {
                  p = (i + (j * width) ) * 4;
                  data[p    ] = red;
                  data[p + 1] = green;
                  data[p + 2] = blue;
                  data[p + 3] = 255;
              }   
          } 
      } else {
          that.fillRect(tile_x + x, tile_y + y, w, h, color);
      }
  };

  // Draw the current tile to the screen
  that.finishTile = function() {
      if (conf.prefer_js) {
          c_ctx.putImageData(tile, tile_x - viewport.x, tile_y - viewport.y);
      }
      // else: No-op, if not prefer_js then already done by setSubTile
  };

  rgbImageData = function(x, y, vx, vy, width, height, arr, offset) {
      var img, i, j, data;
      /*
      if ((x - v.x >= v.w) || (y - v.y >= v.h) ||
          (x - v.x + width < 0) || (y - v.y + height < 0)) {
          // Skipping because outside of viewport
          return;
      }
      */
      img = c_ctx.createImageData(width, height);
      data = img.data;
      for (i=0, j=offset; i < (width * height * 4); i=i+4, j=j+3) {
          data[i    ] = arr[j    ];
          data[i + 1] = arr[j + 1];
          data[i + 2] = arr[j + 2];
          data[i + 3] = 255; // Set Alpha
      }
      c_ctx.putImageData(img, x - vx, y - vy);
  };

  bgrxImageData = function(x, y, vx, vy, width, height, arr, offset) {
      var img, i, j, data;
      /*
      if ((x - v.x >= v.w) || (y - v.y >= v.h) ||
          (x - v.x + width < 0) || (y - v.y + height < 0)) {
          // Skipping because outside of viewport
          return;
      }
      */
      img = c_ctx.createImageData(width, height);
      data = img.data;
      for (i=0, j=offset; i < (width * height * 4); i=i+4, j=j+4) {
          data[i    ] = arr[j + 2];
          data[i + 1] = arr[j + 1];
          data[i + 2] = arr[j    ];
          data[i + 3] = 255; // Set Alpha
      }
      c_ctx.putImageData(img, x - vx, y - vy);
  };

  cmapImageData = function(x, y, vx, vy, width, height, arr, offset) {
      var img, i, j, data, bgr, cmap;
      img = c_ctx.createImageData(width, height);
      data = img.data;
      cmap = conf.colourMap;
      for (i=0, j=offset; i < (width * height * 4); i+=4, j+=1) {
          bgr = cmap[arr[j]];
          data[i    ] = bgr[2];
          data[i + 1] = bgr[1];
          data[i + 2] = bgr[0];
          data[i + 3] = 255; // Set Alpha
      }
      c_ctx.putImageData(img, x - vx, y - vy);
  };

  that.blitImage = function(x, y, width, height, arr, offset) {
      if (conf.true_color) {
          bgrxImageData(x, y, viewport.x, viewport.y, width, height, arr, offset);
      } else {
          cmapImageData(x, y, viewport.x, viewport.y, width, height, arr, offset);
      }
  };

  that.blitRgbImage = function(x, y, width, height, arr, offset) {
      if (conf.true_color) {
          rgbImageData(x, y, viewport.x, viewport.y, width, height, arr, offset);
      } else {
          // prolly wrong...
          cmapImageData(x, y, viewport.x, viewport.y, width, height, arr, offset);
      }
  };

  that.blitStringImage = function(str, x, y) {
      var img = new Image();
      img.onload = function () {
          c_ctx.drawImage(img, x - viewport.x, y - viewport.y);
      };
      img.src = str;
  };

  // Wrap ctx.drawImage but relative to viewport
  that.drawImage = function(img, x, y) {
      c_ctx.drawImage(img, x - viewport.x, y - viewport.y);
  };

  that.renderQ_push = function(action) {
      renderQ.push(action);
      if (renderQ.length === 1) {
          // If this can be rendered immediately it will be, otherwise
          // the scanner will start polling the queue (every
          // requestAnimationFrame interval)
          scan_renderQ();
      }
  };

  scan_renderQ = function() {
      var a, ready = true;
      while (ready && renderQ.length > 0) {
          a = renderQ[0];
          switch (a.type) {
              case 'copy':
                  that.copyImage(a.old_x, a.old_y, a.x, a.y, a.width, a.height);
                  break;
              case 'fill':
                  that.fillRect(a.x, a.y, a.width, a.height, a.color);
                  break;
              case 'blit':
                  that.blitImage(a.x, a.y, a.width, a.height, a.data, 0);
                  break;
              case 'blitRgb':
                  that.blitRgbImage(a.x, a.y, a.width, a.height, a.data, 0);
                  break;
              case 'img':    
                  if (a.img.complete) {
                      that.drawImage(a.img, a.x, a.y);
                  } else {
                      // We need to wait for this image to 'load'
                      // to keep things in-order
                      ready = false;
                  }
                  break;
          }
          if (ready) {
              a = renderQ.shift();
          }
      }
      if (renderQ.length > 0) {
          requestAnimFrame(scan_renderQ);
      }
  };


  that.changeCursor = function(pixels, mask, hotx, hoty, w, h) {
      if (conf.cursor_uri === false) {
          Util.Warn("changeCursor called but no cursor data URI support");
          return;
      }

      if (conf.true_color) {
          changeCursor(conf.target, pixels, mask, hotx, hoty, w, h);
      } else {
          changeCursor(conf.target, pixels, mask, hotx, hoty, w, h, conf.colourMap);
      }
  };

  that.defaultCursor = function() {
      conf.target.style.cursor = "default";
  };

  return constructor();  // Return the public API interface

  }  // End of Display()


  /* Set CSS cursor property using data URI encoded cursor file */
  function changeCursor(target, pixels, mask, hotx, hoty, w0, h0, cmap) {
      "use strict";
      var cur = [], rgb, IHDRsz, RGBsz, ANDsz, XORsz, url, idx, alpha, x, y;
      //Util.Debug(">> changeCursor, x: " + hotx + ", y: " + hoty + ", w0: " + w0 + ", h0: " + h0);

      var w = w0;
      var h = h0;
      if (h < w)
          h = w;                 // increase h to make it square
      else
          w = h;                 // increace w to make it square

      // Push multi-byte little-endian values
      cur.push16le = function (num) {
          this.push((num     ) & 0xFF,
                    (num >> 8) & 0xFF  );
      };
      cur.push32le = function (num) {
          this.push((num      ) & 0xFF,
                    (num >>  8) & 0xFF,
                    (num >> 16) & 0xFF,
                    (num >> 24) & 0xFF  );
      };

      IHDRsz = 40;
      RGBsz = w * h * 4;
      XORsz = Math.ceil( (w * h) / 8.0 );
      ANDsz = Math.ceil( (w * h) / 8.0 );

      // Main header
      cur.push16le(0);      // 0: Reserved
      cur.push16le(2);      // 2: .CUR type
      cur.push16le(1);      // 4: Number of images, 1 for non-animated ico

      // Cursor #1 header (ICONDIRENTRY)
      cur.push(w);          // 6: width
      cur.push(h);          // 7: height
      cur.push(0);          // 8: colors, 0 -> true-color
      cur.push(0);          // 9: reserved
      cur.push16le(hotx);   // 10: hotspot x coordinate
      cur.push16le(hoty);   // 12: hotspot y coordinate
      cur.push32le(IHDRsz + RGBsz + XORsz + ANDsz);
                            // 14: cursor data byte size
      cur.push32le(22);     // 18: offset of cursor data in the file


      // Cursor #1 InfoHeader (ICONIMAGE/BITMAPINFO)
      cur.push32le(IHDRsz); // 22: Infoheader size
      cur.push32le(w);      // 26: Cursor width
      cur.push32le(h*2);    // 30: XOR+AND height
      cur.push16le(1);      // 34: number of planes
      cur.push16le(32);     // 36: bits per pixel
      cur.push32le(0);      // 38: Type of compression

      cur.push32le(XORsz + ANDsz); // 43: Size of Image
                                  // Gimp leaves this as 0

      cur.push32le(0);      // 46: reserved
      cur.push32le(0);      // 50: reserved
      cur.push32le(0);      // 54: reserved
      cur.push32le(0);      // 58: reserved

      // 62: color data (RGBQUAD icColors[])
      for (y = h-1; y >= 0; y -= 1) {
          for (x = 0; x < w; x += 1) {
              if (x >= w0 || y >= h0) {
                  cur.push(0);          // blue
                  cur.push(0);          // green
                  cur.push(0);          // red
                  cur.push(0);          // alpha
              } else {
                  idx = y * Math.ceil(w0 / 8) + Math.floor(x/8);
                  alpha = (mask[idx] << (x % 8)) & 0x80 ? 255 : 0;
                  if (cmap) {
                      idx = (w0 * y) + x;
                      rgb = cmap[pixels[idx]];
                      cur.push(rgb[2]);          // blue
                      cur.push(rgb[1]);          // green
                      cur.push(rgb[0]);          // red
                      cur.push(alpha);           // alpha
                  } else {
                      idx = ((w0 * y) + x) * 4;
                      cur.push(pixels[idx + 2]); // blue
                      cur.push(pixels[idx + 1]); // green
                      cur.push(pixels[idx    ]); // red
                      cur.push(alpha);           // alpha
                  }
              }
          }
      }

      // XOR/bitmask data (BYTE icXOR[])
      // (ignored, just needs to be right size)
      for (y = 0; y < h; y += 1) {
          for (x = 0; x < Math.ceil(w / 8); x += 1) {
              cur.push(0x00);
          }
      }

      // AND/bitmask data (BYTE icAND[])
      // (ignored, just needs to be right size)
      for (y = 0; y < h; y += 1) {
          for (x = 0; x < Math.ceil(w / 8); x += 1) {
              cur.push(0x00);
          }
      }

      url = "data:image/x-icon;base64," + Base64.encode(cur);
      target.style.cursor = "url(" + url + ") " + hotx + " " + hoty + ", default";
      //Util.Debug("<< changeCursor, cur.length: " + cur.length);
  }
  ;

  // jsunzip
  /*
  * JSUnzip
  *
  * Copyright (c) 2011 by Erik Moller
  * All Rights Reserved
  *
  * This software is provided 'as-is', without any express
  * or implied warranty.  In no event will the authors be
  * held liable for any damages arising from the use of
  * this software.
  *
  * Permission is granted to anyone to use this software
  * for any purpose, including commercial applications,
  * and to alter it and redistribute it freely, subject to
  * the following restrictions:
  *
  * 1. The origin of this software must not be
  *    misrepresented; you must not claim that you
  *    wrote the original software. If you use this
  *    software in a product, an acknowledgment in
  *    the product documentation would be appreciated
  *    but is not required.
  *
  * 2. Altered source versions must be plainly marked
  *    as such, and must not be misrepresented as
  *    being the original software.
  *
  * 3. This notice may not be removed or altered from
  *    any source distribution.
  */
  
  var tinf;

  function JSUnzip() {

      this.getInt = function(offset, size) {
          switch (size) {
          case 4:
              return  (this.data.charCodeAt(offset + 3) & 0xff) << 24 | 
                      (this.data.charCodeAt(offset + 2) & 0xff) << 16 | 
                      (this.data.charCodeAt(offset + 1) & 0xff) << 8 | 
                      (this.data.charCodeAt(offset + 0) & 0xff);
              break;
          case 2:
              return  (this.data.charCodeAt(offset + 1) & 0xff) << 8 | 
                      (this.data.charCodeAt(offset + 0) & 0xff);
              break;
          default:
              return this.data.charCodeAt(offset) & 0xff;
              break;
          }
      };

      this.getDOSDate = function(dosdate, dostime) {
          var day = dosdate & 0x1f;
          var month = ((dosdate >> 5) & 0xf) - 1;
          var year = 1980 + ((dosdate >> 9) & 0x7f)
          var second = (dostime & 0x1f) * 2;
          var minute = (dostime >> 5) & 0x3f;
          hour = (dostime >> 11) & 0x1f;
          return new Date(year, month, day, hour, minute, second);
      }

      this.open = function(data) {
          this.data = data;
          this.files = [];

          if (this.data.length < 22)
              return { 'status' : false, 'error' : 'Invalid data' };
          var endOfCentralDirectory = this.data.length - 22;
          while (endOfCentralDirectory >= 0 && this.getInt(endOfCentralDirectory, 4) != 0x06054b50)
              --endOfCentralDirectory;
          if (endOfCentralDirectory < 0)
              return { 'status' : false, 'error' : 'Invalid data' };
          if (this.getInt(endOfCentralDirectory + 4, 2) != 0 || this.getInt(endOfCentralDirectory + 6, 2) != 0)
              return { 'status' : false, 'error' : 'No multidisk support' };

          var entriesInThisDisk = this.getInt(endOfCentralDirectory + 8, 2);
          var centralDirectoryOffset = this.getInt(endOfCentralDirectory + 16, 4);
          var globalCommentLength = this.getInt(endOfCentralDirectory + 20, 2);
          this.comment = this.data.slice(endOfCentralDirectory + 22, endOfCentralDirectory + 22 + globalCommentLength);

          var fileOffset = centralDirectoryOffset;

          for (var i = 0; i < entriesInThisDisk; ++i) {
              if (this.getInt(fileOffset + 0, 4) != 0x02014b50)
                  return { 'status' : false, 'error' : 'Invalid data' };
              if (this.getInt(fileOffset + 6, 2) > 20)
                  return { 'status' : false, 'error' : 'Unsupported version' };
              if (this.getInt(fileOffset + 8, 2) & 1)
                  return { 'status' : false, 'error' : 'Encryption not implemented' };

              var compressionMethod = this.getInt(fileOffset + 10, 2);
              if (compressionMethod != 0 && compressionMethod != 8)
                  return { 'status' : false, 'error' : 'Unsupported compression method' };

              var lastModFileTime = this.getInt(fileOffset + 12, 2);
              var lastModFileDate = this.getInt(fileOffset + 14, 2);
              var lastModifiedDate = this.getDOSDate(lastModFileDate, lastModFileTime);

              var crc = this.getInt(fileOffset + 16, 4);
              // TODO: crc

              var compressedSize = this.getInt(fileOffset + 20, 4);
              var uncompressedSize = this.getInt(fileOffset + 24, 4);

              var fileNameLength = this.getInt(fileOffset + 28, 2);
              var extraFieldLength = this.getInt(fileOffset + 30, 2);
              var fileCommentLength = this.getInt(fileOffset + 32, 2);

              var relativeOffsetOfLocalHeader = this.getInt(fileOffset + 42, 4);

              var fileName = this.data.slice(fileOffset + 46, fileOffset + 46 + fileNameLength);
              var fileComment = this.data.slice(fileOffset + 46 + fileNameLength + extraFieldLength, fileOffset + 46 + fileNameLength + extraFieldLength + fileCommentLength);

              if (this.getInt(relativeOffsetOfLocalHeader + 0, 4) != 0x04034b50)
                  return { 'status' : false, 'error' : 'Invalid data' };
              var localFileNameLength = this.getInt(relativeOffsetOfLocalHeader + 26, 2);
              var localExtraFieldLength = this.getInt(relativeOffsetOfLocalHeader + 28, 2);
              var localFileContent = relativeOffsetOfLocalHeader + 30 + localFileNameLength + localExtraFieldLength;

              this.files[fileName] = 
              {
                  'fileComment' : fileComment,
                  'compressionMethod' : compressionMethod,
                  'compressedSize' : compressedSize,
                  'uncompressedSize' : uncompressedSize,
                  'localFileContent' : localFileContent,
                  'lastModifiedDate' : lastModifiedDate
              };

              fileOffset += 46 + fileNameLength + extraFieldLength + fileCommentLength;
          }
          return { 'status' : true }
      };     
      

      this.read = function(fileName) {
          var fileInfo = this.files[fileName];
          if (fileInfo) {
              if (fileInfo.compressionMethod == 8) {
                  if (!tinf) {
                      tinf = new TINF();
                      tinf.init();
                  }
                  var result = tinf.uncompress(this.data, fileInfo.localFileContent);
                  if (result.status == tinf.OK)
                      return { 'status' : true, 'data' : result.data };
                  else
                      return { 'status' : false, 'error' : result.error };
              } else {
                  return { 'status' : true, 'data' : this.data.slice(fileInfo.localFileContent, fileInfo.localFileContent + fileInfo.uncompressedSize) };
              }
          }
          return { 'status' : false, 'error' : "File '" + fileName + "' doesn't exist in zip" };
      };
      
  };



  /*
  * tinflate  -  tiny inflate
  *
  * Copyright (c) 2003 by Joergen Ibsen / Jibz
  * All Rights Reserved
  *
  * http://www.ibsensoftware.com/
  *
  * This software is provided 'as-is', without any express
  * or implied warranty.  In no event will the authors be
  * held liable for any damages arising from the use of
  * this software.
  *
  * Permission is granted to anyone to use this software
  * for any purpose, including commercial applications,
  * and to alter it and redistribute it freely, subject to
  * the following restrictions:
  *
  * 1. The origin of this software must not be
  *    misrepresented; you must not claim that you
  *    wrote the original software. If you use this
  *    software in a product, an acknowledgment in
  *    the product documentation would be appreciated
  *    but is not required.
  *
  * 2. Altered source versions must be plainly marked
  *    as such, and must not be misrepresented as
  *    being the original software.
  *
  * 3. This notice may not be removed or altered from
  *    any source distribution.
  */

  /*
  * tinflate javascript port by Erik Moller in May 2011.
  * emoller@opera.com
  * 
  * read_bits() patched by mike@imidio.com to allow
  * reading more then 8 bits (needed in some zlib streams)
  */

  "use strict";

  function TINF() {
      
  this.OK = 0;
  this.DATA_ERROR = (-3);
  this.WINDOW_SIZE = 32768;

  /* ------------------------------ *
  * -- internal data structures -- *
  * ------------------------------ */

  this.TREE = function() {
    this.table = new Array(16);  /* table of code length counts */
    this.trans = new Array(288); /* code -> symbol translation table */
  };

  this.DATA = function(that) {
    this.source = '';
    this.sourceIndex = 0;
    this.tag = 0;
    this.bitcount = 0;

    this.dest = [];
    
    this.history = [];

    this.ltree = new that.TREE(); /* dynamic length/symbol tree */
    this.dtree = new that.TREE(); /* dynamic distance tree */
  };

  /* --------------------------------------------------- *
  * -- uninitialized global data (static structures) -- *
  * --------------------------------------------------- */

  this.sltree = new this.TREE(); /* fixed length/symbol tree */
  this.sdtree = new this.TREE(); /* fixed distance tree */

  /* extra bits and base tables for length codes */
  this.length_bits = new Array(30);
  this.length_base = new Array(30);

  /* extra bits and base tables for distance codes */
  this.dist_bits = new Array(30);
  this.dist_base = new Array(30);

  /* special ordering of code length codes */
  this.clcidx = [
    16, 17, 18, 0, 8, 7, 9, 6,
    10, 5, 11, 4, 12, 3, 13, 2,
    14, 1, 15
  ];

  /* ----------------------- *
  * -- utility functions -- *
  * ----------------------- */

  /* build extra bits and base tables */
  this.build_bits_base = function(bits, base, delta, first)
  {
    var i, sum;

    /* build bits table */
    for (i = 0; i < delta; ++i) bits[i] = 0;
    for (i = 0; i < 30 - delta; ++i) bits[i + delta] = Math.floor(i / delta);

    /* build base table */
    for (sum = first, i = 0; i < 30; ++i)
    {
        base[i] = sum;
        sum += 1 << bits[i];
    }
  }

  /* build the fixed huffman trees */
  this.build_fixed_trees = function(lt, dt)
  {
    var i;

    /* build fixed length tree */
    for (i = 0; i < 7; ++i) lt.table[i] = 0;

    lt.table[7] = 24;
    lt.table[8] = 152;
    lt.table[9] = 112;

    for (i = 0; i < 24; ++i) lt.trans[i] = 256 + i;
    for (i = 0; i < 144; ++i) lt.trans[24 + i] = i;
    for (i = 0; i < 8; ++i) lt.trans[24 + 144 + i] = 280 + i;
    for (i = 0; i < 112; ++i) lt.trans[24 + 144 + 8 + i] = 144 + i;

    /* build fixed distance tree */
    for (i = 0; i < 5; ++i) dt.table[i] = 0;

    dt.table[5] = 32;

    for (i = 0; i < 32; ++i) dt.trans[i] = i;
  }

  /* given an array of code lengths, build a tree */
  this.build_tree = function(t, lengths, loffset, num)
  {
    var offs = new Array(16);
    var i, sum;

    /* clear code length count table */
    for (i = 0; i < 16; ++i) t.table[i] = 0;

    /* scan symbol lengths, and sum code length counts */
    for (i = 0; i < num; ++i) t.table[lengths[loffset + i]]++;

    t.table[0] = 0;

    /* compute offset table for distribution sort */
    for (sum = 0, i = 0; i < 16; ++i)
    {
        offs[i] = sum;
        sum += t.table[i];
    }

    /* create code->symbol translation table (symbols sorted by code) */
    for (i = 0; i < num; ++i)
    {
        if (lengths[loffset + i]) t.trans[offs[lengths[loffset + i]]++] = i;
    }
  }

  /* ---------------------- *
  * -- decode functions -- *
  * ---------------------- */

  /* get one bit from source stream */
  this.getbit = function(d)
  {
    var bit;

    /* check if tag is empty */
    if (!d.bitcount--)
    {
        /* load next tag */
        d.tag = d.source[d.sourceIndex++] & 0xff;
        d.bitcount = 7;
    }

    /* shift bit out of tag */
    bit = d.tag & 0x01;
    d.tag >>= 1;

    return bit;
  }

  /* read a num bit value from a stream and add base */
  function read_bits_direct(source, bitcount, tag, idx, num)
  {
      var val = 0;
      while (bitcount < 24) {
          tag = tag | (source[idx++] & 0xff) << bitcount;
          bitcount += 8;
      }
      val = tag & (0xffff >> (16 - num));
      tag >>= num;
      bitcount -= num;
      return [bitcount, tag, idx, val];
  }
  this.read_bits = function(d, num, base)
  {
      if (!num)
          return base;

      var ret = read_bits_direct(d.source, d.bitcount, d.tag, d.sourceIndex, num);
      d.bitcount = ret[0];
      d.tag = ret[1];
      d.sourceIndex = ret[2];
      return ret[3] + base;
  }

  /* given a data stream and a tree, decode a symbol */
  this.decode_symbol = function(d, t)
  {
      while (d.bitcount < 16) {
          d.tag = d.tag | (d.source[d.sourceIndex++] & 0xff) << d.bitcount;
          d.bitcount += 8;
      }
      
      var sum = 0, cur = 0, len = 0;
      do {
          cur = 2 * cur + ((d.tag & (1 << len)) >> len);

          ++len;

          sum += t.table[len];
          cur -= t.table[len];

      } while (cur >= 0);

      d.tag >>= len;
      d.bitcount -= len;

      return t.trans[sum + cur];
  }

  /* given a data stream, decode dynamic trees from it */
  this.decode_trees = function(d, lt, dt)
  {
    var code_tree = new this.TREE();
    var lengths = new Array(288+32);
    var hlit, hdist, hclen;
    var i, num, length;

    /* get 5 bits HLIT (257-286) */
    hlit = this.read_bits(d, 5, 257);

    /* get 5 bits HDIST (1-32) */
    hdist = this.read_bits(d, 5, 1);

    /* get 4 bits HCLEN (4-19) */
    hclen = this.read_bits(d, 4, 4);

    for (i = 0; i < 19; ++i) lengths[i] = 0;

    /* read code lengths for code length alphabet */
    for (i = 0; i < hclen; ++i)
    {
        /* get 3 bits code length (0-7) */
        var clen = this.read_bits(d, 3, 0);

        lengths[this.clcidx[i]] = clen;
    }

    /* build code length tree */
    this.build_tree(code_tree, lengths, 0, 19);

    /* decode code lengths for the dynamic trees */
    for (num = 0; num < hlit + hdist; )
    {
        var sym = this.decode_symbol(d, code_tree);

        switch (sym)
        {
        case 16:
          /* copy previous code length 3-6 times (read 2 bits) */
          {
              var prev = lengths[num - 1];
              for (length = this.read_bits(d, 2, 3); length; --length)
              {
                lengths[num++] = prev;
              }
          }
          break;
        case 17:
          /* repeat code length 0 for 3-10 times (read 3 bits) */
          for (length = this.read_bits(d, 3, 3); length; --length)
          {
              lengths[num++] = 0;
          }
          break;
        case 18:
          /* repeat code length 0 for 11-138 times (read 7 bits) */
          for (length = this.read_bits(d, 7, 11); length; --length)
          {
              lengths[num++] = 0;
          }
          break;
        default:
          /* values 0-15 represent the actual code lengths */
          lengths[num++] = sym;
          break;
        }
    }

    /* build dynamic trees */
    this.build_tree(lt, lengths, 0, hlit);
    this.build_tree(dt, lengths, hlit, hdist);
  }

  /* ----------------------------- *
  * -- block inflate functions -- *
  * ----------------------------- */

  /* given a stream and two trees, inflate a block of data */
  this.inflate_block_data = function(d, lt, dt)
  {
    // js optimization.
    var ddest = d.dest;
    var ddestlength = ddest.length;

    while (1)
    {
        var sym = this.decode_symbol(d, lt);

        /* check for end of block */
        if (sym == 256)
        {
          return this.OK;
        }

        if (sym < 256)
        {
          ddest[ddestlength++] = sym; // ? String.fromCharCode(sym);
          d.history.push(sym);
        } else {

          var length, dist, offs;
          var i;

          sym -= 257;

          /* possibly get more bits from length code */
          length = this.read_bits(d, this.length_bits[sym], this.length_base[sym]);

          dist = this.decode_symbol(d, dt);

          /* possibly get more bits from distance code */
          offs = d.history.length - this.read_bits(d, this.dist_bits[dist], this.dist_base[dist]);

          if (offs < 0)
              throw ("Invalid zlib offset " + offs);
          
          /* copy match */
          for (i = offs; i < offs + length; ++i) {
              //ddest[ddestlength++] = ddest[i];
              ddest[ddestlength++] = d.history[i];
              d.history.push(d.history[i]);
          }
        }
    }
  }

  /* inflate an uncompressed block of data */
  this.inflate_uncompressed_block = function(d)
  {
    var length, invlength;
    var i;

    if (d.bitcount > 7) {
        var overflow = Math.floor(d.bitcount / 8);
        d.sourceIndex -= overflow;
        d.bitcount = 0;
        d.tag = 0;
    }
    
    /* get length */
    length = d.source[d.sourceIndex+1];
    length = 256*length + d.source[d.sourceIndex];

    /* get one's complement of length */
    invlength = d.source[d.sourceIndex+3];
    invlength = 256*invlength + d.source[d.sourceIndex+2];

    /* check length */
    if (length != (~invlength & 0x0000ffff)) return this.DATA_ERROR;

    d.sourceIndex += 4;

    /* copy block */
    for (i = length; i; --i) {
        d.history.push(d.source[d.sourceIndex]);
        d.dest[d.dest.length] = d.source[d.sourceIndex++];
    }

    /* make sure we start next block on a byte boundary */
    d.bitcount = 0;

    return this.OK;
  }

  /* inflate a block of data compressed with fixed huffman trees */
  this.inflate_fixed_block = function(d)
  {
    /* decode block using fixed trees */
    return this.inflate_block_data(d, this.sltree, this.sdtree);
  }

  /* inflate a block of data compressed with dynamic huffman trees */
  this.inflate_dynamic_block = function(d)
  {
    /* decode trees from stream */
    this.decode_trees(d, d.ltree, d.dtree);

    /* decode block using decoded trees */
    return this.inflate_block_data(d, d.ltree, d.dtree);
  }

  /* ---------------------- *
  * -- public functions -- *
  * ---------------------- */

  /* initialize global (static) data */
  this.init = function()
  {
    /* build fixed huffman trees */
    this.build_fixed_trees(this.sltree, this.sdtree);

    /* build extra bits and base tables */
    this.build_bits_base(this.length_bits, this.length_base, 4, 3);
    this.build_bits_base(this.dist_bits, this.dist_base, 2, 1);

    /* fix a special case */
    this.length_bits[28] = 0;
    this.length_base[28] = 258;

    this.reset();   
  }

  this.reset = function()
  {
    this.d = new this.DATA(this);
    delete this.header;
  }

  /* inflate stream from source to dest */
  this.uncompress = function(source, offset)
  {

    var d = this.d;
    var bfinal;

    /* initialise data */
    d.source = source;
    d.sourceIndex = offset;
    d.bitcount = 0;

    d.dest = [];

    // Skip zlib header at start of stream
    if (typeof this.header == 'undefined') {
        this.header = this.read_bits(d, 16, 0);
        /* byte 0: 0x78, 7 = 32k window size, 8 = deflate */
        /* byte 1: check bits for header and other flags */
    }

    var blocks = 0;
    
    do {

        var btype;
        var res;

        /* read final block flag */
        bfinal = this.getbit(d);

        /* read block type (2 bits) */
        btype = this.read_bits(d, 2, 0);

        /* decompress block */
        switch (btype)
        {
        case 0:
          /* decompress uncompressed block */
          res = this.inflate_uncompressed_block(d);
          break;
        case 1:
          /* decompress block with fixed huffman trees */
          res = this.inflate_fixed_block(d);
          break;
        case 2:
          /* decompress block with dynamic huffman trees */
          res = this.inflate_dynamic_block(d);
          break;
        default:
          return { 'status' : this.DATA_ERROR };
        }

        if (res != this.OK) return { 'status' : this.DATA_ERROR };
        blocks++;
        
    } while (!bfinal && d.sourceIndex < d.source.length);

    d.history = d.history.slice(-this.WINDOW_SIZE);
    
    return { 'status' : this.OK, 'data' : d.dest };
  }

  };
  ;

  // rfb
  /*
  * noVNC: HTML5 VNC client
  * Copyright (C) 2012 Joel Martin
  * Copyright (C) 2013 Samuel Mannehed for Cendio AB
  * Licensed under MPL 2.0 (see LICENSE.txt)
  *
  * See README.md for usage and integration instructions.
  *
  * TIGHT decoder portion:
  * (c) 2012 Michael Tinglof, Joe Balaz, Les Piech (Mercuri.ca)
  */

  /*jslint white: false, browser: true, bitwise: false, plusplus: false */
  /*global window, Util, Display, Keyboard, Mouse, Websock, Websock_native, Base64, DES */


  function RFB(defaults) {
  "use strict";

  var that           = {},  // Public API methods
      conf           = {},  // Configuration attributes

      // Pre-declare private functions used before definitions (jslint)
      init_vars, updateState, fail, handle_message,
      init_msg, normal_msg, framebufferUpdate, print_stats,

      pixelFormat, clientEncodings, fbUpdateRequest, fbUpdateRequests,
      keyEvent, pointerEvent, clientCutText,

      getTightCLength, extract_data_uri,
      keyPress, mouseButton, mouseMove,

      checkEvents,  // Overridable for testing


      //
      // Private RFB namespace variables
      //
      rfb_host       = '',
      rfb_port       = 5900,
      rfb_password   = '',
      rfb_path       = '',

      rfb_state      = 'disconnected',
      rfb_version    = 0,
      rfb_max_version= 3.8,
      rfb_auth_scheme= '',
      rfb_tightvnc   = false,

      rfb_xvp_ver    = 0,


      // In preference order
      encodings      = [
          ['COPYRECT',         0x01 ],
          ['TIGHT',            0x07 ],
          ['TIGHT_PNG',        -260 ],
          ['HEXTILE',          0x05 ],
          ['RRE',              0x02 ],
          ['RAW',              0x00 ],
          ['DesktopSize',      -223 ],
          ['Cursor',           -239 ],

          // Psuedo-encoding settings
          //['JPEG_quality_lo',   -32 ],
          ['JPEG_quality_med',    -26 ],
          //['JPEG_quality_hi',   -23 ],
          //['compress_lo',      -255 ],
          ['compress_hi',        -247 ],
          ['last_rect',          -224 ],
          ['xvp',                -309 ]
          ],

      encHandlers    = {},
      encNames       = {}, 
      encStats       = {},     // [rectCnt, rectCntTot]

      ws             = null,   // Websock object
      display        = null,   // Display object
      keyboard       = null,   // Keyboard input handler object
      mouse          = null,   // Mouse input handler object
      sendTimer      = null,   // Send Queue check timer
      disconnTimer   = null,   // disconnection timer
      msgTimer       = null,   // queued handle_message timer

      // Frame buffer update state
      FBU            = {
          rects          : 0,
          subrects       : 0,  // RRE
          lines          : 0,  // RAW
          tiles          : 0,  // HEXTILE
          bytes          : 0,
          x              : 0,
          y              : 0,
          width          : 0, 
          height         : 0,
          encoding       : 0,
          subencoding    : -1,
          background     : null,
          zlibs          : []   // TIGHT zlib streams
      },

      fb_Bpp         = 4,
      fb_depth       = 3,
      fb_width       = 0,
      fb_height      = 0,
      fb_name        = "",

      rre_chunk_sz   = 100,

      timing         = {
          last_fbu       : 0,
          fbu_total      : 0,
          fbu_total_cnt  : 0,
          full_fbu_total : 0,
          full_fbu_cnt   : 0,

          fbu_rt_start   : 0,
          fbu_rt_total   : 0,
          fbu_rt_cnt     : 0,
          pixels         : 0
      },

      test_mode        = false,

      /* Mouse state */
      mouse_buttonMask = 0,
      mouse_arr        = [],
      viewportDragging = false,
      viewportDragPos  = {};

  // Configuration attributes
  Util.conf_defaults(conf, that, defaults, [
      ['target',             'wo', 'dom', null, 'VNC display rendering Canvas object'],
      ['focusContainer',     'wo', 'dom', document, 'DOM element that captures keyboard input'],

      ['encrypt',            'rw', 'bool', false, 'Use TLS/SSL/wss encryption'],
      ['true_color',         'rw', 'bool', true,  'Request true color pixel data'],
      ['local_cursor',       'rw', 'bool', false, 'Request locally rendered cursor'],
      ['shared',             'rw', 'bool', true,  'Request shared mode'],
      ['view_only',          'rw', 'bool', false, 'Disable client mouse/keyboard'],
      ['xvp_password_sep',   'rw', 'str',  '@',   'Separator for XVP password fields'],
      ['disconnectTimeout',  'rw', 'int', 3,    'Time (s) to wait for disconnection'],

      ['wsProtocols',        'rw', 'arr', ['binary', 'base64'],
          'Protocols to use in the WebSocket connection'],

      // UltraVNC repeater ID to connect to
      ['repeaterID',         'rw', 'str',  '',    'RepeaterID to connect to'],

      ['viewportDrag',       'rw', 'bool', false, 'Move the viewport on mouse drags'],

      // Callback functions
      ['onUpdateState',      'rw', 'func', function() { },
          'onUpdateState(rfb, state, oldstate, statusMsg): RFB state update/change '],
      ['onPasswordRequired', 'rw', 'func', function() { },
          'onPasswordRequired(rfb): VNC password is required '],
      ['onClipboard',        'rw', 'func', function() { },
          'onClipboard(rfb, text): RFB clipboard contents received'],
      ['onBell',             'rw', 'func', function() { },
          'onBell(rfb): RFB Bell message received '],
      ['onFBUReceive',       'rw', 'func', function() { },
          'onFBUReceive(rfb, fbu): RFB FBU received but not yet processed '],
      ['onFBUComplete',      'rw', 'func', function() { },
          'onFBUComplete(rfb, fbu): RFB FBU received and processed '],
      ['onFBResize',         'rw', 'func', function() { },
          'onFBResize(rfb, width, height): frame buffer resized'],
      ['onDesktopName',      'rw', 'func', function() { },
          'onDesktopName(rfb, name): desktop name received'],
      ['onXvpInit',          'rw', 'func', function() { },
          'onXvpInit(version): XVP extensions active for this connection'],

      // These callback names are deprecated
      ['updateState',        'rw', 'func', function() { },
          'obsolete, use onUpdateState'],
      ['clipboardReceive',   'rw', 'func', function() { },
          'obsolete, use onClipboard']
      ]);


  // Override/add some specific configuration getters/setters
  that.set_local_cursor = function(cursor) {
      if ((!cursor) || (cursor in {'0':1, 'no':1, 'false':1})) {
          conf.local_cursor = false;
      } else {
          if (display.get_cursor_uri()) {
              conf.local_cursor = true;
          } else {
              Util.Warn("Browser does not support local cursor");
          }
      }
  };

  // These are fake configuration getters
  that.get_display = function() { return display; };

  that.get_keyboard = function() { return keyboard; };

  that.get_mouse = function() { return mouse; };



  //
  // Setup routines
  //

  // Create the public API interface and initialize values that stay
  // constant across connect/disconnect
  function constructor() {
      var i, rmode;
      Util.Debug(">> RFB.constructor");

      // Create lookup tables based encoding number
      for (i=0; i < encodings.length; i+=1) {
          encHandlers[encodings[i][1]] = encHandlers[encodings[i][0]];
          encNames[encodings[i][1]] = encodings[i][0];
          encStats[encodings[i][1]] = [0, 0];
      }
      // Initialize display, mouse, keyboard, and websock
      try {
          display   = new Display({'target': conf.target});
      } catch (exc) {
          Util.Error("Display exception: " + exc);
          updateState('fatal', "No working Display");
      }
      keyboard = new Keyboard({'target': conf.focusContainer,
                                  'onKeyPress': keyPress});
      mouse    = new Mouse({'target': conf.target,
                              'onMouseButton': mouseButton,
                              'onMouseMove': mouseMove,
                              'notify': keyboard.sync});

      rmode = display.get_render_mode();

      ws = new Websock();
      ws.on('message', handle_message);
      ws.on('open', function() {
          if (rfb_state === "connect") {
              updateState('ProtocolVersion', "Starting VNC handshake");
          } else {
              fail("Got unexpected WebSockets connection");
          }
      });
      ws.on('close', function(e) {
          Util.Warn("WebSocket on-close event");
          var msg = "";
          if (e.code) {
              msg = " (code: " + e.code;
              if (e.reason) {
                  msg += ", reason: " + e.reason;
              }
              msg += ")";
          }
          if (rfb_state === 'disconnect') {
              updateState('disconnected', 'VNC disconnected' + msg);
          } else if (rfb_state === 'ProtocolVersion') {
              fail('Failed to connect to server' + msg);
          } else if (rfb_state in {'failed':1, 'disconnected':1}) {
              Util.Error("Received onclose while disconnected" + msg);
          } else  {
              fail('Server disconnected' + msg);
          }
      });
      ws.on('error', function(e) {
          Util.Warn("WebSocket on-error event");
          //fail("WebSock reported an error");
      });


      init_vars();

      /* Check web-socket-js if no builtin WebSocket support */
      if (Websock_native) {
          Util.Info("Using native WebSockets");
          updateState('loaded', 'noVNC ready: native WebSockets, ' + rmode);
      } else {
          Util.Warn("Using web-socket-js bridge. Flash version: " +
                    Util.Flash.version);
          if ((! Util.Flash) ||
              (Util.Flash.version < 9)) {
              updateState('fatal', "WebSockets or <a href='http://get.adobe.com/flashplayer'>Adobe Flash<\/a> is required");
          } else if (document.location.href.substr(0, 7) === "file://") {
              updateState('fatal',
                      "'file://' URL is incompatible with Adobe Flash");
          } else {
              updateState('loaded', 'noVNC ready: WebSockets emulation, ' + rmode);
          }
      }

      Util.Debug("<< RFB.constructor");
      return that;  // Return the public API interface
  }

  function connect() {
      Util.Debug(">> RFB.connect");
      var uri;
      
      if (typeof UsingSocketIO !== "undefined") {
          uri = "http";
      } else {
          uri = conf.encrypt ? "wss" : "ws";
      }
      uri += "://" + rfb_host + ":" + rfb_port + "/" + rfb_path;
      Util.Info("connecting to " + uri);

      ws.open(uri, conf.wsProtocols);

      Util.Debug("<< RFB.connect");
  }

  // Initialize variables that are reset before each connection
  init_vars = function() {
      var i;

      /* Reset state */
      ws.init();

      FBU.rects        = 0;
      FBU.subrects     = 0;  // RRE and HEXTILE
      FBU.lines        = 0;  // RAW
      FBU.tiles        = 0;  // HEXTILE
      FBU.zlibs        = []; // TIGHT zlib encoders
      mouse_buttonMask = 0;
      mouse_arr        = [];

      // Clear the per connection encoding stats
      for (i=0; i < encodings.length; i+=1) {
          encStats[encodings[i][1]][0] = 0;
      }
      
      for (i=0; i < 4; i++) {
          //FBU.zlibs[i] = new InflateStream();
          FBU.zlibs[i] = new TINF();
          FBU.zlibs[i].init();
      }
  };

  // Print statistics
  print_stats = function() {
      var i, s;
      Util.Info("Encoding stats for this connection:");
      for (i=0; i < encodings.length; i+=1) {
          s = encStats[encodings[i][1]];
          if ((s[0] + s[1]) > 0) {
              Util.Info("    " + encodings[i][0] + ": " +
                        s[0] + " rects");
          }
      }
      Util.Info("Encoding stats since page load:");
      for (i=0; i < encodings.length; i+=1) {
          s = encStats[encodings[i][1]];
          if ((s[0] + s[1]) > 0) {
              Util.Info("    " + encodings[i][0] + ": " +
                        s[1] + " rects");
          }
      }
  };

  //
  // Utility routines
  //


  /*
  * Page states:
  *   loaded       - page load, equivalent to disconnected
  *   disconnected - idle state
  *   connect      - starting to connect (to ProtocolVersion)
  *   normal       - connected
  *   disconnect   - starting to disconnect
  *   failed       - abnormal disconnect
  *   fatal        - failed to load page, or fatal error
  *
  * RFB protocol initialization states:
  *   ProtocolVersion 
  *   Security
  *   Authentication
  *   password     - waiting for password, not part of RFB
  *   SecurityResult
  *   ClientInitialization - not triggered by server message
  *   ServerInitialization (to normal)
  */
  updateState = function(state, statusMsg) {
      var func, cmsg, oldstate = rfb_state;

      if (state === oldstate) {
          /* Already here, ignore */
          Util.Debug("Already in state '" + state + "', ignoring.");
          return;
      }

      /* 
      * These are disconnected states. A previous connect may
      * asynchronously cause a connection so make sure we are closed.
      */
      if (state in {'disconnected':1, 'loaded':1, 'connect':1,
                    'disconnect':1, 'failed':1, 'fatal':1}) {
          if (sendTimer) {
              clearInterval(sendTimer);
              sendTimer = null;
          }

          if (msgTimer) {
              clearTimeout(msgTimer);
              msgTimer = null;
          }

          if (display && display.get_context()) {
              keyboard.ungrab();
              mouse.ungrab();
              display.defaultCursor();
              if ((Util.get_logging() !== 'debug') ||
                  (state === 'loaded')) {
                  // Show noVNC logo on load and when disconnected if
                  // debug is off
                  display.clear();
              }
          }

          ws.close();
      }

      if (oldstate === 'fatal') {
          Util.Error("Fatal error, cannot continue");
      }

      if ((state === 'failed') || (state === 'fatal')) {
          func = Util.Error;
      } else {
          func = Util.Warn;
      }

      cmsg = typeof(statusMsg) !== 'undefined' ? (" Msg: " + statusMsg) : "";
      func("New state '" + state + "', was '" + oldstate + "'." + cmsg);

      if ((oldstate === 'failed') && (state === 'disconnected')) {
          // Do disconnect action, but stay in failed state
          rfb_state = 'failed';
      } else {
          rfb_state = state;
      }

      if (disconnTimer && (rfb_state !== 'disconnect')) {
          Util.Debug("Clearing disconnect timer");
          clearTimeout(disconnTimer);
          disconnTimer = null;
      }

      switch (state) {
      case 'normal':
          if ((oldstate === 'disconnected') || (oldstate === 'failed')) {
              Util.Error("Invalid transition from 'disconnected' or 'failed' to 'normal'");
          }

          break;


      case 'connect':

          init_vars();
          connect();

          // WebSocket.onopen transitions to 'ProtocolVersion'
          break;


      case 'disconnect':

          if (! test_mode) {
              disconnTimer = setTimeout(function () {
                      fail("Disconnect timeout");
                  }, conf.disconnectTimeout * 1000);
          }

          print_stats();

          // WebSocket.onclose transitions to 'disconnected'
          break;


      case 'failed':
          if (oldstate === 'disconnected') {
              Util.Error("Invalid transition from 'disconnected' to 'failed'");
          }
          if (oldstate === 'normal') {
              Util.Error("Error while connected.");
          }
          if (oldstate === 'init') {
              Util.Error("Error while initializing.");
          }

          // Make sure we transition to disconnected
          setTimeout(function() { updateState('disconnected'); }, 50);

          break;


      default:
          // No state change action to take

      }

      if ((oldstate === 'failed') && (state === 'disconnected')) {
          // Leave the failed message
          conf.updateState(that, state, oldstate); // Obsolete
          conf.onUpdateState(that, state, oldstate);
      } else {
          conf.updateState(that, state, oldstate, statusMsg); // Obsolete
          conf.onUpdateState(that, state, oldstate, statusMsg);
      }
  };

  fail = function(msg) {
      updateState('failed', msg);
      return false;
  };

  handle_message = function() {
      //Util.Debug(">> handle_message ws.rQlen(): " + ws.rQlen());
      //Util.Debug("ws.rQslice(0,20): " + ws.rQslice(0,20) + " (" + ws.rQlen() + ")");
      if (ws.rQlen() === 0) {
          Util.Warn("handle_message called on empty receive queue");
          return;
      }
      switch (rfb_state) {
      case 'disconnected':
      case 'failed':
          Util.Error("Got data while disconnected");
          break;
      case 'normal':
          if (normal_msg() && ws.rQlen() > 0) {
              // true means we can continue processing
              // Give other events a chance to run
              if (msgTimer === null) {
                  Util.Debug("More data to process, creating timer");
                  msgTimer = setTimeout(function () {
                              msgTimer = null;
                              handle_message();
                          }, 10);
              } else {
                  Util.Debug("More data to process, existing timer");
              }
          }
          break;
      default:
          init_msg();
          break;
      }
  };


  function genDES(password, challenge) {
      var i, passwd = [];
      for (i=0; i < password.length; i += 1) {
          passwd.push(password.charCodeAt(i));
      }
      return (new DES(passwd)).encrypt(challenge);
  }

  // overridable for testing
  checkEvents = function() {
      if (rfb_state === 'normal' && !viewportDragging && mouse_arr.length > 0) {
          ws.send(mouse_arr);
          mouse_arr = [];
      }
  };

  keyPress = function(keysym, down) {
      if (conf.view_only) { return; } // View only, skip keyboard events

      ws.send(keyEvent(keysym, down));
  };

  mouseButton = function(x, y, down, bmask) {
      if (down) {
          mouse_buttonMask |= bmask;
      } else {
          mouse_buttonMask ^= bmask;
      }

      if (conf.viewportDrag) {
          if (down && !viewportDragging) {
              viewportDragging = true;
              viewportDragPos = {'x': x, 'y': y};

              // Skip sending mouse events
              return;
          } else {
              viewportDragging = false;
          }
      }

      if (conf.view_only) { return; } // View only, skip mouse events

      mouse_arr = mouse_arr.concat(
              pointerEvent(display.absX(x), display.absY(y)) );
      ws.send(mouse_arr);
      mouse_arr = [];
  };

  mouseMove = function(x, y) {
      //Util.Debug('>> mouseMove ' + x + "," + y);
      var deltaX, deltaY;

      if (viewportDragging) {
          //deltaX = x - viewportDragPos.x; // drag viewport
          deltaX = viewportDragPos.x - x; // drag frame buffer
          //deltaY = y - viewportDragPos.y; // drag viewport
          deltaY = viewportDragPos.y - y; // drag frame buffer
          viewportDragPos = {'x': x, 'y': y};

          display.viewportChange(deltaX, deltaY);

          // Skip sending mouse events
          return;
      }

      if (conf.view_only) { return; } // View only, skip mouse events

      mouse_arr = mouse_arr.concat(
              pointerEvent(display.absX(x), display.absY(y)));
      
      checkEvents();
  };


  //
  // Server message handlers
  //

  // RFB/VNC initialisation message handler
  init_msg = function() {
      //Util.Debug(">> init_msg [rfb_state '" + rfb_state + "']");

      var strlen, reason, length, sversion, cversion, repeaterID,
          i, types, num_types, challenge, response, bpp, depth,
          big_endian, red_max, green_max, blue_max, red_shift,
          green_shift, blue_shift, true_color, name_length, is_repeater,
          xvp_sep, xvp_auth, xvp_auth_str;

      //Util.Debug("ws.rQ (" + ws.rQlen() + ") " + ws.rQslice(0));
      switch (rfb_state) {

      case 'ProtocolVersion' :
          if (ws.rQlen() < 12) {
              return fail("Incomplete protocol version");
          }
          sversion = ws.rQshiftStr(12).substr(4,7);
          Util.Info("Server ProtocolVersion: " + sversion);
          is_repeater = 0;
          switch (sversion) {
              case "000.000": is_repeater = 1; break; // UltraVNC repeater
              case "003.003": rfb_version = 3.3; break;
              case "003.006": rfb_version = 3.3; break;  // UltraVNC
              case "003.889": rfb_version = 3.3; break;  // Apple Remote Desktop
              case "003.007": rfb_version = 3.7; break;
              case "003.008": rfb_version = 3.8; break;
              case "004.000": rfb_version = 3.8; break;  // Intel AMT KVM
              case "004.001": rfb_version = 3.8; break;  // RealVNC 4.6
              default:
                  return fail("Invalid server version " + sversion);
          }
          if (is_repeater) { 
              repeaterID = conf.repeaterID;
              while (repeaterID.length < 250) {
                  repeaterID += "\0";
              }
              ws.send_string(repeaterID);
              break;
          }
          if (rfb_version > rfb_max_version) { 
              rfb_version = rfb_max_version;
          }

          if (! test_mode) {
              sendTimer = setInterval(function() {
                      // Send updates either at a rate of one update
                      // every 50ms, or whatever slower rate the network
                      // can handle.
                      ws.flush();
                  }, 50);
          }

          cversion = "00" + parseInt(rfb_version,10) +
                    ".00" + ((rfb_version * 10) % 10);
          ws.send_string("RFB " + cversion + "\n");
          updateState('Security', "Sent ProtocolVersion: " + cversion);
          break;

      case 'Security' :
          if (rfb_version >= 3.7) {
              // Server sends supported list, client decides 
              num_types = ws.rQshift8();
              if (ws.rQwait("security type", num_types, 1)) { return false; }
              if (num_types === 0) {
                  strlen = ws.rQshift32();
                  reason = ws.rQshiftStr(strlen);
                  return fail("Security failure: " + reason);
              }
              rfb_auth_scheme = 0;
              types = ws.rQshiftBytes(num_types);
              Util.Debug("Server security types: " + types);
              for (i=0; i < types.length; i+=1) {
                  if ((types[i] > rfb_auth_scheme) && (types[i] <= 16 || types[i] == 22)) {
                      rfb_auth_scheme = types[i];
                  }
              }
              if (rfb_auth_scheme === 0) {
                  return fail("Unsupported security types: " + types);
              }
              
              ws.send([rfb_auth_scheme]);
          } else {
              // Server decides
              if (ws.rQwait("security scheme", 4)) { return false; }
              rfb_auth_scheme = ws.rQshift32();
          }
          updateState('Authentication',
                  "Authenticating using scheme: " + rfb_auth_scheme);
          init_msg();  // Recursive fallthrough (workaround JSLint complaint)
          break;

      // Triggered by fallthough, not by server message
      case 'Authentication' :
          //Util.Debug("Security auth scheme: " + rfb_auth_scheme);
          switch (rfb_auth_scheme) {
              case 0:  // connection failed
                  if (ws.rQwait("auth reason", 4)) { return false; }
                  strlen = ws.rQshift32();
                  reason = ws.rQshiftStr(strlen);
                  return fail("Auth failure: " + reason);
              case 1:  // no authentication
                  if (rfb_version >= 3.8) {
                      updateState('SecurityResult');
                      return;
                  }
                  // Fall through to ClientInitialisation
                  break;
              case 22:  // XVP authentication
                  xvp_sep = conf.xvp_password_sep;
                  xvp_auth = rfb_password.split(xvp_sep);
                  if (xvp_auth.length < 3) {
                      updateState('password', "XVP credentials required (user" + xvp_sep +
                                  "target" + xvp_sep + "password) -- got only " + rfb_password);
                      conf.onPasswordRequired(that);
                      return;
                  }
                  xvp_auth_str = String.fromCharCode(xvp_auth[0].length) +
                                String.fromCharCode(xvp_auth[1].length) +
                                xvp_auth[0] +
                                xvp_auth[1];
                  ws.send_string(xvp_auth_str);
                  rfb_password = xvp_auth.slice(2).join(xvp_sep);
                  rfb_auth_scheme = 2;
                  // Fall through to standard VNC authentication with remaining part of password
              case 2:  // VNC authentication
                  if (rfb_password.length === 0) {
                      // Notify via both callbacks since it is kind of
                      // a RFB state change and a UI interface issue.
                      updateState('password', "Password Required");
                      conf.onPasswordRequired(that);
                      return;
                  }
                  if (ws.rQwait("auth challenge", 16)) { return false; }
                  challenge = ws.rQshiftBytes(16);
                  //Util.Debug("Password: " + rfb_password);
                  //Util.Debug("Challenge: " + challenge +
                  //           " (" + challenge.length + ")");
                  response = genDES(rfb_password, challenge);
                  //Util.Debug("Response: " + response +
                  //           " (" + response.length + ")");
                  
                  //Util.Debug("Sending DES encrypted auth response");
                  ws.send(response);
                  updateState('SecurityResult');
                  return;
              case 16: // TightVNC Security Type
                  if (ws.rQwait("num tunnels", 4)) { return false; }
                  var numTunnels = ws.rQshift32();
                  //console.log("Number of tunnels: "+numTunnels);

                  rfb_tightvnc = true;

                  if (numTunnels != 0)
                  {
                      fail("Protocol requested tunnels, not currently supported. numTunnels: " + numTunnels);
                      return;
                  }

                  var clientSupportedTypes = {
                      'STDVNOAUTH__': 1,
                      'STDVVNCAUTH_': 2
                  };

                  var serverSupportedTypes = [];

                  if (ws.rQwait("sub auth count", 4)) { return false; }
                  var subAuthCount = ws.rQshift32();
                  //console.log("Sub auth count: "+subAuthCount);
                  for (var i=0;i<subAuthCount;i++)
                  {

                      if (ws.rQwait("sub auth capabilities "+i, 16)) { return false; }
                      var capNum = ws.rQshift32();
                      var capabilities = ws.rQshiftStr(12);
                      //console.log("queue: "+ws.rQlen());
                      //console.log("auth type: "+capNum+": "+capabilities);

                      serverSupportedTypes.push(capabilities);
                  }

                  for (var authType in clientSupportedTypes)
                  {
                      if (serverSupportedTypes.indexOf(authType) != -1)
                      {
                          //console.log("selected authType "+authType);
                          ws.send([0,0,0,clientSupportedTypes[authType]]);

                          switch (authType)
                          {
                              case 'STDVNOAUTH__':
                                  // No authentication
                                  updateState('SecurityResult');
                                  return;
                              case 'STDVVNCAUTH_':
                                  // VNC Authentication.  Reenter auth handler to complete auth
                                  rfb_auth_scheme = 2;
                                  init_msg();
                                  return;
                              default:
                                  fail("Unsupported tiny auth scheme: " + authType);
                                  return;
                          }
                      }
                  }


                  return;
              default:
                  fail("Unsupported auth scheme: " + rfb_auth_scheme);
                  return;
          }
          updateState('ClientInitialisation', "No auth required");
          init_msg();  // Recursive fallthrough (workaround JSLint complaint)
          break;

      case 'SecurityResult' :
          if (ws.rQwait("VNC auth response ", 4)) { return false; }
          switch (ws.rQshift32()) {
              case 0:  // OK
                  // Fall through to ClientInitialisation
                  break;
              case 1:  // failed
                  if (rfb_version >= 3.8) {
                      length = ws.rQshift32();
                      if (ws.rQwait("SecurityResult reason", length, 8)) {
                          return false;
                      }
                      reason = ws.rQshiftStr(length);
                      fail(reason);
                  } else {
                      fail("Authentication failed");
                  }
                  return;
              case 2:  // too-many
                  return fail("Too many auth attempts");
          }
          updateState('ClientInitialisation', "Authentication OK");
          init_msg();  // Recursive fallthrough (workaround JSLint complaint)
          break;

      // Triggered by fallthough, not by server message
      case 'ClientInitialisation' :
          ws.send([conf.shared ? 1 : 0]); // ClientInitialisation
          updateState('ServerInitialisation', "Authentication OK");
          break;

      case 'ServerInitialisation' :
          if (ws.rQwait("server initialization", 24)) { return false; }

          /* Screen size */
          fb_width  = ws.rQshift16();
          fb_height = ws.rQshift16();

          /* PIXEL_FORMAT */
          bpp            = ws.rQshift8();
          depth          = ws.rQshift8();
          big_endian     = ws.rQshift8();
          true_color     = ws.rQshift8();

          red_max        = ws.rQshift16();
          green_max      = ws.rQshift16();
          blue_max       = ws.rQshift16();
          red_shift      = ws.rQshift8();
          green_shift    = ws.rQshift8();
          blue_shift     = ws.rQshift8();
          ws.rQshiftStr(3); // padding

          Util.Info("Screen: " + fb_width + "x" + fb_height + 
                    ", bpp: " + bpp + ", depth: " + depth +
                    ", big_endian: " + big_endian +
                    ", true_color: " + true_color +
                    ", red_max: " + red_max +
                    ", green_max: " + green_max +
                    ", blue_max: " + blue_max +
                    ", red_shift: " + red_shift +
                    ", green_shift: " + green_shift +
                    ", blue_shift: " + blue_shift);

          if (big_endian !== 0) {
              Util.Warn("Server native endian is not little endian");
          }
          if (red_shift !== 16) {
              Util.Warn("Server native red-shift is not 16");
          }
          if (blue_shift !== 0) {
              Util.Warn("Server native blue-shift is not 0");
          }

          /* Connection name/title */
          name_length   = ws.rQshift32();
          fb_name = Util.decodeUTF8(ws.rQshiftStr(name_length));
          conf.onDesktopName(that, fb_name);
          
          if (conf.true_color && fb_name === "Intel(r) AMT KVM")
          {
              Util.Warn("Intel AMT KVM only support 8/16 bit depths. Disabling true color");
              conf.true_color = false;
          }

          if (rfb_tightvnc)
          {
              // In TightVNC mode, ServerInit message is extended
              var numServerMessages = ws.rQshift16();
              var numClientMessages = ws.rQshift16();
              var numEncodings = ws.rQshift16();
              ws.rQshift16(); // padding
              //console.log("numServerMessages "+numServerMessages);
              //console.log("numClientMessages "+numClientMessages);
              //console.log("numEncodings "+numEncodings);

              for (var i=0;i<numServerMessages;i++)
              {
                  var srvMsg = ws.rQshiftStr(16);
                  //console.log("server message: "+srvMsg);
              }
              for (var i=0;i<numClientMessages;i++)
              {
                  var clientMsg = ws.rQshiftStr(16);
                  //console.log("client message: "+clientMsg);
              }
              for (var i=0;i<numEncodings;i++)
              {
                  var encoding = ws.rQshiftStr(16);
                  //console.log("encoding: "+encoding);
              }
          }

          display.set_true_color(conf.true_color);
          conf.onFBResize(that, fb_width, fb_height);
          display.resize(fb_width, fb_height);
          keyboard.grab();
          mouse.grab();

          if (conf.true_color) {
              fb_Bpp           = 4;
              fb_depth         = 3;
          } else {
              fb_Bpp           = 1;
              fb_depth         = 1;
          }

          response = pixelFormat();
          response = response.concat(clientEncodings());
          response = response.concat(fbUpdateRequests()); // initial fbu-request
          timing.fbu_rt_start = (new Date()).getTime();
          timing.pixels = 0;
          ws.send(response);
          
          checkEvents();

          if (conf.encrypt) {
              updateState('normal', "Connected (encrypted) to: " + fb_name);
          } else {
              updateState('normal', "Connected (unencrypted) to: " + fb_name);
          }
          break;
      }
      //Util.Debug("<< init_msg");
  };


  /* Normal RFB/VNC server message handler */
  normal_msg = function() {
      //Util.Debug(">> normal_msg");

      var ret = true, msg_type, length, text,
          c, first_colour, num_colours, red, green, blue,
          xvp_ver, xvp_msg;

      if (FBU.rects > 0) {
          msg_type = 0;
      } else {
          msg_type = ws.rQshift8();
      }
      switch (msg_type) {
      case 0:  // FramebufferUpdate
          ret = framebufferUpdate(); // false means need more data
          if (ret) {
              // only allow one outstanding fbu-request at a time
              ws.send(fbUpdateRequests());
          }
          break;
      case 1:  // SetColourMapEntries
          Util.Debug("SetColourMapEntries");
          ws.rQshift8();  // Padding
          first_colour = ws.rQshift16(); // First colour
          num_colours = ws.rQshift16();
          if (ws.rQwait("SetColourMapEntries", num_colours*6, 6)) { return false; }
          
          for (c=0; c < num_colours; c+=1) { 
              red = ws.rQshift16();
              //Util.Debug("red before: " + red);
              red = parseInt(red / 256, 10);
              //Util.Debug("red after: " + red);
              green = parseInt(ws.rQshift16() / 256, 10);
              blue = parseInt(ws.rQshift16() / 256, 10);
              display.set_colourMap([blue, green, red], first_colour + c);
          }
          Util.Debug("colourMap: " + display.get_colourMap());
          Util.Info("Registered " + num_colours + " colourMap entries");
          //Util.Debug("colourMap: " + display.get_colourMap());
          break;
      case 2:  // Bell
          Util.Debug("Bell");
          conf.onBell(that);
          break;
      case 3:  // ServerCutText
          Util.Debug("ServerCutText");
          if (ws.rQwait("ServerCutText header", 7, 1)) { return false; }
          ws.rQshiftBytes(3);  // Padding
          length = ws.rQshift32();
          if (ws.rQwait("ServerCutText", length, 8)) { return false; }

          text = ws.rQshiftStr(length);
          conf.clipboardReceive(that, text); // Obsolete
          conf.onClipboard(that, text);
          break;
      case 250:  // XVP
          ws.rQshift8();  // Padding
          xvp_ver = ws.rQshift8();
          xvp_msg = ws.rQshift8();
          switch (xvp_msg) {
          case 0:  // XVP_FAIL
              updateState(rfb_state, "Operation failed");
              break;
          case 1:  // XVP_INIT
              rfb_xvp_ver = xvp_ver;
              Util.Info("XVP extensions enabled (version " + rfb_xvp_ver + ")");
              conf.onXvpInit(rfb_xvp_ver);
              break;
          default:
              fail("Disconnected: illegal server XVP message " + xvp_msg);
              break;
          }
          break;
      default:
          fail("Disconnected: illegal server message type " + msg_type);
          Util.Debug("ws.rQslice(0,30):" + ws.rQslice(0,30));
          break;
      }
      //Util.Debug("<< normal_msg");
      return ret;
  };

  framebufferUpdate = function() {
      var now, hdr, fbu_rt_diff, ret = true;

      if (FBU.rects === 0) {
          //Util.Debug("New FBU: ws.rQslice(0,20): " + ws.rQslice(0,20));
          if (ws.rQwait("FBU header", 3)) {
              ws.rQunshift8(0);  // FBU msg_type
              return false;
          }
          ws.rQshift8();  // padding
          FBU.rects = ws.rQshift16();
          //Util.Debug("FramebufferUpdate, rects:" + FBU.rects);
          FBU.bytes = 0;
          timing.cur_fbu = 0;
          if (timing.fbu_rt_start > 0) {
              now = (new Date()).getTime();
              Util.Info("First FBU latency: " + (now - timing.fbu_rt_start));
          }
      }

      while (FBU.rects > 0) {
          if (rfb_state !== "normal") {
              return false;
          }
          if (ws.rQwait("FBU", FBU.bytes)) { return false; }
          if (FBU.bytes === 0) {
              if (ws.rQwait("rect header", 12)) { return false; }
              /* New FramebufferUpdate */

              hdr = ws.rQshiftBytes(12);
              FBU.x      = (hdr[0] << 8) + hdr[1];
              FBU.y      = (hdr[2] << 8) + hdr[3];
              FBU.width  = (hdr[4] << 8) + hdr[5];
              FBU.height = (hdr[6] << 8) + hdr[7];
              FBU.encoding = parseInt((hdr[8] << 24) + (hdr[9] << 16) +
                                      (hdr[10] << 8) +  hdr[11], 10);

              conf.onFBUReceive(that,
                      {'x': FBU.x, 'y': FBU.y,
                      'width': FBU.width, 'height': FBU.height,
                      'encoding': FBU.encoding,
                      'encodingName': encNames[FBU.encoding]});

              if (encNames[FBU.encoding]) {
                  // Debug:
                  /*
                  var msg =  "FramebufferUpdate rects:" + FBU.rects;
                  msg += " x: " + FBU.x + " y: " + FBU.y;
                  msg += " width: " + FBU.width + " height: " + FBU.height;
                  msg += " encoding:" + FBU.encoding;
                  msg += "(" + encNames[FBU.encoding] + ")";
                  msg += ", ws.rQlen(): " + ws.rQlen();
                  Util.Debug(msg);
                  */
              } else {
                  fail("Disconnected: unsupported encoding " +
                      FBU.encoding);
                  return false;
              }
          }

          timing.last_fbu = (new Date()).getTime();

          ret = encHandlers[FBU.encoding]();

          now = (new Date()).getTime();
          timing.cur_fbu += (now - timing.last_fbu);

          if (ret) {
              encStats[FBU.encoding][0] += 1;
              encStats[FBU.encoding][1] += 1;
              timing.pixels += FBU.width * FBU.height;
          }

          if (timing.pixels >= (fb_width * fb_height)) {
              if (((FBU.width === fb_width) &&
                          (FBU.height === fb_height)) ||
                      (timing.fbu_rt_start > 0)) {
                  timing.full_fbu_total += timing.cur_fbu;
                  timing.full_fbu_cnt += 1;
                  Util.Info("Timing of full FBU, cur: " +
                            timing.cur_fbu + ", total: " +
                            timing.full_fbu_total + ", cnt: " +
                            timing.full_fbu_cnt + ", avg: " +
                            (timing.full_fbu_total /
                                timing.full_fbu_cnt));
              }
              if (timing.fbu_rt_start > 0) {
                  fbu_rt_diff = now - timing.fbu_rt_start;
                  timing.fbu_rt_total += fbu_rt_diff;
                  timing.fbu_rt_cnt += 1;
                  Util.Info("full FBU round-trip, cur: " +
                            fbu_rt_diff + ", total: " +
                            timing.fbu_rt_total + ", cnt: " +
                            timing.fbu_rt_cnt + ", avg: " +
                            (timing.fbu_rt_total /
                                timing.fbu_rt_cnt));
                  timing.fbu_rt_start = 0;
              }
          }
          if (! ret) {
              return ret; // false ret means need more data
          }
      }

      conf.onFBUComplete(that,
              {'x': FBU.x, 'y': FBU.y,
                  'width': FBU.width, 'height': FBU.height,
                  'encoding': FBU.encoding,
                  'encodingName': encNames[FBU.encoding]});

      return true; // We finished this FBU
  };

  //
  // FramebufferUpdate encodings
  //

  encHandlers.RAW = function display_raw() {
      //Util.Debug(">> display_raw (" + ws.rQlen() + " bytes)");

      var cur_y, cur_height;

      if (FBU.lines === 0) {
          FBU.lines = FBU.height;
      }
      FBU.bytes = FBU.width * fb_Bpp; // At least a line
      if (ws.rQwait("RAW", FBU.bytes)) { return false; }
      cur_y = FBU.y + (FBU.height - FBU.lines);
      cur_height = Math.min(FBU.lines,
                            Math.floor(ws.rQlen()/(FBU.width * fb_Bpp)));
      display.blitImage(FBU.x, cur_y, FBU.width, cur_height,
              ws.get_rQ(), ws.get_rQi());
      ws.rQshiftBytes(FBU.width * cur_height * fb_Bpp);
      FBU.lines -= cur_height;

      if (FBU.lines > 0) {
          FBU.bytes = FBU.width * fb_Bpp; // At least another line
      } else {
          FBU.rects -= 1;
          FBU.bytes = 0;
      }
      //Util.Debug("<< display_raw (" + ws.rQlen() + " bytes)");
      return true;
  };

  encHandlers.COPYRECT = function display_copy_rect() {
      //Util.Debug(">> display_copy_rect");

      var old_x, old_y;

      FBU.bytes = 4;
      if (ws.rQwait("COPYRECT", 4)) { return false; }
      display.renderQ_push({
              'type': 'copy',
              'old_x': ws.rQshift16(),
              'old_y': ws.rQshift16(),
              'x': FBU.x,
              'y': FBU.y,
              'width': FBU.width,
              'height': FBU.height});
      FBU.rects -= 1;
      FBU.bytes = 0;
      return true;
  };

  encHandlers.RRE = function display_rre() {
      //Util.Debug(">> display_rre (" + ws.rQlen() + " bytes)");
      var color, x, y, width, height, chunk;

      if (FBU.subrects === 0) {
          FBU.bytes = 4+fb_Bpp;
          if (ws.rQwait("RRE", 4+fb_Bpp)) { return false; }
          FBU.subrects = ws.rQshift32();
          color = ws.rQshiftBytes(fb_Bpp); // Background
          display.fillRect(FBU.x, FBU.y, FBU.width, FBU.height, color);
      }
      while ((FBU.subrects > 0) && (ws.rQlen() >= (fb_Bpp + 8))) {
          color = ws.rQshiftBytes(fb_Bpp);
          x = ws.rQshift16();
          y = ws.rQshift16();
          width = ws.rQshift16();
          height = ws.rQshift16();
          display.fillRect(FBU.x + x, FBU.y + y, width, height, color);
          FBU.subrects -= 1;
      }
      //Util.Debug("   display_rre: rects: " + FBU.rects +
      //           ", FBU.subrects: " + FBU.subrects);

      if (FBU.subrects > 0) {
          chunk = Math.min(rre_chunk_sz, FBU.subrects);
          FBU.bytes = (fb_Bpp + 8) * chunk;
      } else {
          FBU.rects -= 1;
          FBU.bytes = 0;
      }
      //Util.Debug("<< display_rre, FBU.bytes: " + FBU.bytes);
      return true;
  };

  encHandlers.HEXTILE = function display_hextile() {
      //Util.Debug(">> display_hextile");
      var subencoding, subrects, color, cur_tile,
          tile_x, x, w, tile_y, y, h, xy, s, sx, sy, wh, sw, sh,
          rQ = ws.get_rQ(), rQi = ws.get_rQi(); 

      if (FBU.tiles === 0) {
          FBU.tiles_x = Math.ceil(FBU.width/16);
          FBU.tiles_y = Math.ceil(FBU.height/16);
          FBU.total_tiles = FBU.tiles_x * FBU.tiles_y;
          FBU.tiles = FBU.total_tiles;
      }

      /* FBU.bytes comes in as 1, ws.rQlen() at least 1 */
      while (FBU.tiles > 0) {
          FBU.bytes = 1;
          if (ws.rQwait("HEXTILE subencoding", FBU.bytes)) { return false; }
          subencoding = rQ[rQi];  // Peek
          if (subencoding > 30) { // Raw
              fail("Disconnected: illegal hextile subencoding " + subencoding);
              //Util.Debug("ws.rQslice(0,30):" + ws.rQslice(0,30));
              return false;
          }
          subrects = 0;
          cur_tile = FBU.total_tiles - FBU.tiles;
          tile_x = cur_tile % FBU.tiles_x;
          tile_y = Math.floor(cur_tile / FBU.tiles_x);
          x = FBU.x + tile_x * 16;
          y = FBU.y + tile_y * 16;
          w = Math.min(16, (FBU.x + FBU.width) - x);
          h = Math.min(16, (FBU.y + FBU.height) - y);

          /* Figure out how much we are expecting */
          if (subencoding & 0x01) { // Raw
              //Util.Debug("   Raw subencoding");
              FBU.bytes += w * h * fb_Bpp;
          } else {
              if (subencoding & 0x02) { // Background
                  FBU.bytes += fb_Bpp;
              }
              if (subencoding & 0x04) { // Foreground
                  FBU.bytes += fb_Bpp;
              }
              if (subencoding & 0x08) { // AnySubrects
                  FBU.bytes += 1;   // Since we aren't shifting it off
                  if (ws.rQwait("hextile subrects header", FBU.bytes)) { return false; }
                  subrects = rQ[rQi + FBU.bytes-1]; // Peek
                  if (subencoding & 0x10) { // SubrectsColoured
                      FBU.bytes += subrects * (fb_Bpp + 2);
                  } else {
                      FBU.bytes += subrects * 2;
                  }
              }
          }

          /*
          Util.Debug("   tile:" + cur_tile + "/" + (FBU.total_tiles - 1) +
                " (" + tile_x + "," + tile_y + ")" +
                " [" + x + "," + y + "]@" + w + "x" + h +
                ", subenc:" + subencoding +
                "(last: " + FBU.lastsubencoding + "), subrects:" +
                subrects +
                ", ws.rQlen():" + ws.rQlen() + ", FBU.bytes:" + FBU.bytes +
                " last:" + ws.rQslice(FBU.bytes-10, FBU.bytes) +
                " next:" + ws.rQslice(FBU.bytes-1, FBU.bytes+10));
          */
          if (ws.rQwait("hextile", FBU.bytes)) { return false; }

          /* We know the encoding and have a whole tile */
          FBU.subencoding = rQ[rQi];
          rQi += 1;
          if (FBU.subencoding === 0) {
              if (FBU.lastsubencoding & 0x01) {
                  /* Weird: ignore blanks after RAW */
                  Util.Debug("     Ignoring blank after RAW");
              } else {
                  display.fillRect(x, y, w, h, FBU.background);
              }
          } else if (FBU.subencoding & 0x01) { // Raw
              display.blitImage(x, y, w, h, rQ, rQi);
              rQi += FBU.bytes - 1;
          } else {
              if (FBU.subencoding & 0x02) { // Background
                  FBU.background = rQ.slice(rQi, rQi + fb_Bpp);
                  rQi += fb_Bpp;
              }
              if (FBU.subencoding & 0x04) { // Foreground
                  FBU.foreground = rQ.slice(rQi, rQi + fb_Bpp);
                  rQi += fb_Bpp;
              }

              display.startTile(x, y, w, h, FBU.background);
              if (FBU.subencoding & 0x08) { // AnySubrects
                  subrects = rQ[rQi];
                  rQi += 1;
                  for (s = 0; s < subrects; s += 1) {
                      if (FBU.subencoding & 0x10) { // SubrectsColoured
                          color = rQ.slice(rQi, rQi + fb_Bpp);
                          rQi += fb_Bpp;
                      } else {
                          color = FBU.foreground;
                      }
                      xy = rQ[rQi];
                      rQi += 1;
                      sx = (xy >> 4);
                      sy = (xy & 0x0f);

                      wh = rQ[rQi];
                      rQi += 1;
                      sw = (wh >> 4)   + 1;
                      sh = (wh & 0x0f) + 1;

                      display.subTile(sx, sy, sw, sh, color);
                  }
              }
              display.finishTile();
          }
          ws.set_rQi(rQi);
          FBU.lastsubencoding = FBU.subencoding;
          FBU.bytes = 0;
          FBU.tiles -= 1;
      }

      if (FBU.tiles === 0) {
          FBU.rects -= 1;
      }

      //Util.Debug("<< display_hextile");
      return true;
  };


  // Get 'compact length' header and data size
  getTightCLength = function (arr) {
      var header = 1, data = 0;
      data += arr[0] & 0x7f;
      if (arr[0] & 0x80) {
          header += 1;
          data += (arr[1] & 0x7f) << 7;
          if (arr[1] & 0x80) {
              header += 1;
              data += arr[2] << 14;
          }
      }
      return [header, data];
  };

  function display_tight(isTightPNG) {
      //Util.Debug(">> display_tight");

      if (fb_depth === 1) {
          fail("Tight protocol handler only implements true color mode");
      }

      var ctl, cmode, clength, color, img, data;
      var filterId = -1, resetStreams = 0, streamId = -1;
      var rQ = ws.get_rQ(), rQi = ws.get_rQi(); 

      FBU.bytes = 1; // compression-control byte
      if (ws.rQwait("TIGHT compression-control", FBU.bytes)) { return false; }

      var checksum = function(data) {
          var sum=0, i;
          for (i=0; i<data.length;i++) {
              sum += data[i];
              if (sum > 65536) sum -= 65536;
          }
          return sum;
      }

      var decompress = function(data) {
          for (var i=0; i<4; i++) {
              if ((resetStreams >> i) & 1) {
                  FBU.zlibs[i].reset();
                  Util.Info("Reset zlib stream " + i);
              }
          }
          var uncompressed = FBU.zlibs[streamId].uncompress(data, 0);
          if (uncompressed.status !== 0) {
              Util.Error("Invalid data in zlib stream");
          }
          //Util.Warn("Decompressed " + data.length + " to " +
          //    uncompressed.data.length + " checksums " +
          //    checksum(data) + ":" + checksum(uncompressed.data));

          return uncompressed.data;
      }

      var indexedToRGB = function (data, numColors, palette, width, height) {
          // Convert indexed (palette based) image data to RGB
          // TODO: reduce number of calculations inside loop
          var dest = [];
          var x, y, b, w, w1, dp, sp;
          if (numColors === 2) {
              w = Math.floor((width + 7) / 8);
              w1 = Math.floor(width / 8);
              for (y = 0; y < height; y++) {
                  for (x = 0; x < w1; x++) {
                      for (b = 7; b >= 0; b--) {
                          dp = (y*width + x*8 + 7-b) * 3;
                          sp = (data[y*w + x] >> b & 1) * 3;
                          dest[dp  ] = palette[sp  ];
                          dest[dp+1] = palette[sp+1];
                          dest[dp+2] = palette[sp+2];
                      }
                  }
                  for (b = 7; b >= 8 - width % 8; b--) {
                      dp = (y*width + x*8 + 7-b) * 3;
                      sp = (data[y*w + x] >> b & 1) * 3;
                      dest[dp  ] = palette[sp  ];
                      dest[dp+1] = palette[sp+1];
                      dest[dp+2] = palette[sp+2];
                  }
              }
          } else {
              for (y = 0; y < height; y++) {
                  for (x = 0; x < width; x++) {
                      dp = (y*width + x) * 3;
                      sp = data[y*width + x] * 3;
                      dest[dp  ] = palette[sp  ];
                      dest[dp+1] = palette[sp+1];
                      dest[dp+2] = palette[sp+2];
                  }
              }
          }
          return dest;
      };
      var handlePalette = function() {
          var numColors = rQ[rQi + 2] + 1;
          var paletteSize = numColors * fb_depth; 
          FBU.bytes += paletteSize;
          if (ws.rQwait("TIGHT palette " + cmode, FBU.bytes)) { return false; }

          var bpp = (numColors <= 2) ? 1 : 8;
          var rowSize = Math.floor((FBU.width * bpp + 7) / 8);
          var raw = false;
          if (rowSize * FBU.height < 12) {
              raw = true;
              clength = [0, rowSize * FBU.height];
          } else {
              clength = getTightCLength(ws.rQslice(3 + paletteSize,
                                                  3 + paletteSize + 3));
          }
          FBU.bytes += clength[0] + clength[1];
          if (ws.rQwait("TIGHT " + cmode, FBU.bytes)) { return false; }

          // Shift ctl, filter id, num colors, palette entries, and clength off
          ws.rQshiftBytes(3); 
          var palette = ws.rQshiftBytes(paletteSize);
          ws.rQshiftBytes(clength[0]);

          if (raw) {
              data = ws.rQshiftBytes(clength[1]);
          } else {
              data = decompress(ws.rQshiftBytes(clength[1]));
          }

          // Convert indexed (palette based) image data to RGB
          var rgb = indexedToRGB(data, numColors, palette, FBU.width, FBU.height);

          // Add it to the render queue
          display.renderQ_push({
                  'type': 'blitRgb',
                  'data': rgb,
                  'x': FBU.x,
                  'y': FBU.y,
                  'width': FBU.width,
                  'height': FBU.height});
          return true;
      }

      var handleCopy = function() {
          var raw = false;
          var uncompressedSize = FBU.width * FBU.height * fb_depth;
          if (uncompressedSize < 12) {
              raw = true;
              clength = [0, uncompressedSize];
          } else {
              clength = getTightCLength(ws.rQslice(1, 4));
          }
          FBU.bytes = 1 + clength[0] + clength[1];
          if (ws.rQwait("TIGHT " + cmode, FBU.bytes)) { return false; }

          // Shift ctl, clength off
          ws.rQshiftBytes(1 + clength[0]);

          if (raw) {
              data = ws.rQshiftBytes(clength[1]);
          } else {
              data = decompress(ws.rQshiftBytes(clength[1]));
          }

          display.renderQ_push({
                  'type': 'blitRgb',
                  'data': data,
                  'x': FBU.x,
                  'y': FBU.y,
                  'width': FBU.width,
                  'height': FBU.height});
          return true;
      }

      ctl = ws.rQpeek8();

      // Keep tight reset bits
      resetStreams = ctl & 0xF;

      // Figure out filter
      ctl = ctl >> 4; 
      streamId = ctl & 0x3;

      if (ctl === 0x08)      cmode = "fill";
      else if (ctl === 0x09) cmode = "jpeg";
      else if (ctl === 0x0A) cmode = "png";
      else if (ctl & 0x04)   cmode = "filter";
      else if (ctl < 0x04)   cmode = "copy";
      else return fail("Illegal tight compression received, ctl: " + ctl);

      if (isTightPNG && (cmode === "filter" || cmode === "copy")) {
          return fail("filter/copy received in tightPNG mode");
      }

      switch (cmode) {
          // fill uses fb_depth because TPIXELs drop the padding byte
          case "fill":   FBU.bytes += fb_depth; break; // TPIXEL
          case "jpeg":   FBU.bytes += 3;        break; // max clength
          case "png":    FBU.bytes += 3;        break; // max clength
          case "filter": FBU.bytes += 2;        break; // filter id + num colors if palette
          case "copy":                          break;
      }

      if (ws.rQwait("TIGHT " + cmode, FBU.bytes)) { return false; }

      //Util.Debug("   ws.rQslice(0,20): " + ws.rQslice(0,20) + " (" + ws.rQlen() + ")");
      //Util.Debug("   cmode: " + cmode);

      // Determine FBU.bytes
      switch (cmode) {
      case "fill":
          ws.rQshift8(); // shift off ctl
          color = ws.rQshiftBytes(fb_depth);
          display.renderQ_push({
                  'type': 'fill',
                  'x': FBU.x,
                  'y': FBU.y,
                  'width': FBU.width,
                  'height': FBU.height,
                  'color': [color[2], color[1], color[0]] });
          break;
      case "png":
      case "jpeg":
          clength = getTightCLength(ws.rQslice(1, 4));
          FBU.bytes = 1 + clength[0] + clength[1]; // ctl + clength size + jpeg-data
          if (ws.rQwait("TIGHT " + cmode, FBU.bytes)) { return false; }

          // We have everything, render it
          //Util.Debug("   jpeg, ws.rQlen(): " + ws.rQlen() + ", clength[0]: " +
          //           clength[0] + ", clength[1]: " + clength[1]);
          ws.rQshiftBytes(1 + clength[0]); // shift off ctl + compact length
          img = new Image();
          img.src = "data:image/" + cmode +
              extract_data_uri(ws.rQshiftBytes(clength[1]));
          display.renderQ_push({
                  'type': 'img',
                  'img': img,
                  'x': FBU.x,
                  'y': FBU.y});
          img = null;
          break;
      case "filter":
          filterId = rQ[rQi + 1];
          if (filterId === 1) {
              if (!handlePalette()) { return false; }
          } else {
              // Filter 0, Copy could be valid here, but servers don't send it as an explicit filter
              // Filter 2, Gradient is valid but not used if jpeg is enabled
              throw("Unsupported tight subencoding received, filter: " + filterId);
          }
          break;
      case "copy":
          if (!handleCopy()) { return false; }
          break;
      }

      FBU.bytes = 0;
      FBU.rects -= 1;
      //Util.Debug("   ending ws.rQslice(0,20): " + ws.rQslice(0,20) + " (" + ws.rQlen() + ")");
      //Util.Debug("<< display_tight_png");
      return true;
  }

  extract_data_uri = function(arr) {
      //var i, stra = [];
      //for (i=0; i< arr.length; i += 1) {
      //    stra.push(String.fromCharCode(arr[i]));
      //}
      //return "," + escape(stra.join(''));
      return ";base64," + Base64.encode(arr);
  };

  encHandlers.TIGHT = function () { return display_tight(false); };
  encHandlers.TIGHT_PNG = function () { return display_tight(true); };

  encHandlers.last_rect = function last_rect() {
      //Util.Debug(">> last_rect");
      FBU.rects = 0;
      //Util.Debug("<< last_rect");
      return true;
  };

  encHandlers.DesktopSize = function set_desktopsize() {
      Util.Debug(">> set_desktopsize");
      fb_width = FBU.width;
      fb_height = FBU.height;
      conf.onFBResize(that, fb_width, fb_height);
      display.resize(fb_width, fb_height);
      timing.fbu_rt_start = (new Date()).getTime();

      FBU.bytes = 0;
      FBU.rects -= 1;

      Util.Debug("<< set_desktopsize");
      return true;
  };

  encHandlers.Cursor = function set_cursor() {
      var x, y, w, h, pixelslength, masklength;
      Util.Debug(">> set_cursor");
      x = FBU.x;  // hotspot-x
      y = FBU.y;  // hotspot-y
      w = FBU.width;
      h = FBU.height;

      pixelslength = w * h * fb_Bpp;
      masklength = Math.floor((w + 7) / 8) * h;

      FBU.bytes = pixelslength + masklength;
      if (ws.rQwait("cursor encoding", FBU.bytes)) { return false; }

      //Util.Debug("   set_cursor, x: " + x + ", y: " + y + ", w: " + w + ", h: " + h);

      display.changeCursor(ws.rQshiftBytes(pixelslength),
                              ws.rQshiftBytes(masklength),
                              x, y, w, h);

      FBU.bytes = 0;
      FBU.rects -= 1;

      Util.Debug("<< set_cursor");
      return true;
  };

  encHandlers.JPEG_quality_lo = function set_jpeg_quality() {
      Util.Error("Server sent jpeg_quality pseudo-encoding");
  };

  encHandlers.compress_lo = function set_compress_level() {
      Util.Error("Server sent compress level pseudo-encoding");
  };

  /*
  * Client message routines
  */

  pixelFormat = function() {
      //Util.Debug(">> pixelFormat");
      var arr;
      arr = [0];     // msg-type
      arr.push8(0);  // padding
      arr.push8(0);  // padding
      arr.push8(0);  // padding

      arr.push8(fb_Bpp * 8); // bits-per-pixel
      arr.push8(fb_depth * 8); // depth
      arr.push8(0);  // little-endian
      arr.push8(conf.true_color ? 1 : 0);  // true-color

      arr.push16(255);  // red-max
      arr.push16(255);  // green-max
      arr.push16(255);  // blue-max
      arr.push8(16);    // red-shift
      arr.push8(8);     // green-shift
      arr.push8(0);     // blue-shift

      arr.push8(0);     // padding
      arr.push8(0);     // padding
      arr.push8(0);     // padding
      //Util.Debug("<< pixelFormat");
      return arr;
  };

  clientEncodings = function() {
      //Util.Debug(">> clientEncodings");
      var arr, i, encList = [];

      for (i=0; i<encodings.length; i += 1) {
          if ((encodings[i][0] === "Cursor") &&
              (! conf.local_cursor)) {
              Util.Debug("Skipping Cursor pseudo-encoding");

          // TODO: remove this when we have tight+non-true-color
          } else if ((encodings[i][0] === "TIGHT") && 
                    (! conf.true_color)) {
              Util.Warn("Skipping tight, only support with true color");
          } else {
              //Util.Debug("Adding encoding: " + encodings[i][0]);
              encList.push(encodings[i][1]);
          }
      }

      arr = [2];     // msg-type
      arr.push8(0);  // padding

      arr.push16(encList.length); // encoding count
      for (i=0; i < encList.length; i += 1) {
          arr.push32(encList[i]);
      }
      //Util.Debug("<< clientEncodings: " + arr);
      return arr;
  };

  fbUpdateRequest = function(incremental, x, y, xw, yw) {
      //Util.Debug(">> fbUpdateRequest");
      if (typeof(x) === "undefined") { x = 0; }
      if (typeof(y) === "undefined") { y = 0; }
      if (typeof(xw) === "undefined") { xw = fb_width; }
      if (typeof(yw) === "undefined") { yw = fb_height; }
      var arr;
      arr = [3];  // msg-type
      arr.push8(incremental);
      arr.push16(x);
      arr.push16(y);
      arr.push16(xw);
      arr.push16(yw);
      //Util.Debug("<< fbUpdateRequest");
      return arr;
  };

  // Based on clean/dirty areas, generate requests to send
  fbUpdateRequests = function() {
      var cleanDirty = display.getCleanDirtyReset(),
          arr = [], i, cb, db;

      cb = cleanDirty.cleanBox;
      if (cb.w > 0 && cb.h > 0) {
          // Request incremental for clean box
          arr = arr.concat(fbUpdateRequest(1, cb.x, cb.y, cb.w, cb.h));
      }
      for (i = 0; i < cleanDirty.dirtyBoxes.length; i++) {
          db = cleanDirty.dirtyBoxes[i];
          // Force all (non-incremental for dirty box
          arr = arr.concat(fbUpdateRequest(0, db.x, db.y, db.w, db.h));
      }
      return arr;
  };



  keyEvent = function(keysym, down) {
      //Util.Debug(">> keyEvent, keysym: " + keysym + ", down: " + down);
      var arr;
      arr = [4];  // msg-type
      arr.push8(down);
      arr.push16(0);
      arr.push32(keysym);
      //Util.Debug("<< keyEvent");
      return arr;
  };

  pointerEvent = function(x, y) {
      //Util.Debug(">> pointerEvent, x,y: " + x + "," + y +
      //           " , mask: " + mouse_buttonMask);
      var arr;
      arr = [5];  // msg-type
      arr.push8(mouse_buttonMask);
      arr.push16(x);
      arr.push16(y);
      //Util.Debug("<< pointerEvent");
      return arr;
  };

  clientCutText = function(text) {
      //Util.Debug(">> clientCutText");
      var arr, i, n;
      arr = [6];     // msg-type
      arr.push8(0);  // padding
      arr.push8(0);  // padding
      arr.push8(0);  // padding
      arr.push32(text.length);
      n = text.length;
      for (i=0; i < n; i+=1) {
          arr.push(text.charCodeAt(i));
      }
      //Util.Debug("<< clientCutText:" + arr);
      return arr;
  };



  //
  // Public API interface functions
  //

  that.connect = function(host, port, password, path) {
      //Util.Debug(">> connect");

      rfb_host       = host;
      rfb_port       = port;
      rfb_password   = (password !== undefined)   ? password : "";
      rfb_path       = (path !== undefined) ? path : "";

      if ((!rfb_host) || (!rfb_port)) {
          return fail("Must set host and port");
      }

      updateState('connect');
      //Util.Debug("<< connect");

  };

  that.disconnect = function() {
      //Util.Debug(">> disconnect");
      updateState('disconnect', 'Disconnecting');
      //Util.Debug("<< disconnect");
  };

  that.sendPassword = function(passwd) {
      rfb_password = passwd;
      rfb_state = "Authentication";
      setTimeout(init_msg, 1);
  };

  that.sendCtrlAltDel = function() {
      if (rfb_state !== "normal" || conf.view_only) { return false; }
      Util.Info("Sending Ctrl-Alt-Del");
      var arr = [];
      arr = arr.concat(keyEvent(0xFFE3, 1)); // Control
      arr = arr.concat(keyEvent(0xFFE9, 1)); // Alt
      arr = arr.concat(keyEvent(0xFFFF, 1)); // Delete
      arr = arr.concat(keyEvent(0xFFFF, 0)); // Delete
      arr = arr.concat(keyEvent(0xFFE9, 0)); // Alt
      arr = arr.concat(keyEvent(0xFFE3, 0)); // Control
      ws.send(arr);
  };

  that.xvpOp = function(ver, op) {
      if (rfb_xvp_ver < ver) { return false; }
      Util.Info("Sending XVP operation " + op + " (version " + ver + ")")
      ws.send_string("\xFA\x00" + String.fromCharCode(ver) + String.fromCharCode(op));
      return true;
  };

  that.xvpShutdown = function() {
      return that.xvpOp(1, 2);
  };

  that.xvpReboot = function() {
      return that.xvpOp(1, 3);
  };

  that.xvpReset = function() {
      return that.xvpOp(1, 4);
  };

  // Send a key press. If 'down' is not specified then send a down key
  // followed by an up key.
  that.sendKey = function(code, down) {
      if (rfb_state !== "normal" || conf.view_only) { return false; }
      var arr = [];
      if (typeof down !== 'undefined') {
          Util.Info("Sending key code (" + (down ? "down" : "up") + "): " + code);
          arr = arr.concat(keyEvent(code, down ? 1 : 0));
      } else {
          Util.Info("Sending key code (down + up): " + code);
          arr = arr.concat(keyEvent(code, 1));
          arr = arr.concat(keyEvent(code, 0));
      }
      ws.send(arr);
  };

  that.clipboardPasteFrom = function(text) {
      if (rfb_state !== "normal") { return; }
      //Util.Debug(">> clipboardPasteFrom: " + text.substr(0,40) + "...");
      ws.send(clientCutText(text));
      //Util.Debug("<< clipboardPasteFrom");
  };

  // Override internal functions for testing
  that.testMode = function(override_send, data_mode) {
      test_mode = true;
      that.recv_message = ws.testMode(override_send, data_mode);

      checkEvents = function () { /* Stub Out */ };
      that.connect = function(host, port, password) {
              rfb_host = host;
              rfb_port = port;
              rfb_password = password;
              init_vars();
              updateState('ProtocolVersion', "Starting VNC handshake");
          };
  };


  return constructor();  // Return the public API interface

  }  // End of RFB()
  ;

  return {
    RFB: RFB,
    WebUtil: WebUtil
  }
})(window);
;

