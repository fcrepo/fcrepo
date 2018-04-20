(function() {

  'use strict';

  function removeTrailingSlash(uri) {
    if (uri.lastIndexOf('/') == uri.length - 1) {
      return removeTrailingSlash(uri.substr(0, uri.length - 1));
    }
    return uri;
  }

  // http(String method, String url, Array headers (optional), TypedArray data (optional), Function callback (optional));
  function http(method, url) {
      const args = Array.prototype.slice.call(arguments, http.length);
      const fn = args.pop();
      const headers = args.length > 0 && Array.isArray(args[0]) ? args[0] : [];
      const data = args.length > 0 && !Array.isArray(args[args.length-1]) ? args[args.length-1] : null;
      const xhr = new XMLHttpRequest();
      xhr.open(method, url);
      xhr.onreadystatechange = function() {
        xhr.readyState == 4 && typeof fn === 'function' && fn(xhr);
      }
      headers.filter(function(h) { return Array.isArray(h) && h.length == 2 })
             .forEach(function(h) { xhr.setRequestHeader(h[0], h[1]) });
      xhr.send(data);
  }

  function ready(fn) {
    if (document.readyState != 'loading'){
      fn();
    } else {
      document.addEventListener('DOMContentLoaded', fn);
    }
  }

  function listen(id, event, fn) {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener(event, fn);
    }
  }

  function addChild() {
    document.getElementById('btn_action_create').disabled = true;

    const id = document.getElementById('new_id').value.trim();
    const mixin = document.getElementById('new_mixin').value;
    const uri = removeTrailingSlash(document.getElementById('main').getAttribute('resource'));

    const method = id == '' ? 'POST' : 'PUT';
    const url = id == '' ? uri : uri + '/' + id;
    const headers = [];

    const fn = function (method, url, headers, data) {
      http(method, url, headers, data, function(res) {
        if (res.status == 201) {
          const loc = res.getResponseHeader('Location');
          const linkheaders = (res.getResponseHeader('Link') != null) ? res.getResponseHeader('Link').split(", ") : null;
          const link = (linkheaders != null) ? linkheaders.filter(function(h) { return h.match(/rel="describedby"/)}) : "";
          if (linkheaders != null && link.length > 0) {
            window.location = link[0].substr(1, link[0].indexOf('>') - 1);
          } else if (loc != null) {
            window.location = loc;
          } else {
            window.location.reload();
          }
        } else {
            ajaxErrorHandler(res, 'Error creating binary');
        }
      });
    }

    //both ldp-rs and ldp-nr resources should be created as versionable resources.
    headers.push(['Link', '<http://mementoweb.org/ns#OriginalResource>; rel=\"type\"']);

    if (mixin == 'binary') {
      const update_file = document.getElementById('binary_payload').files[0];
      const reader = new FileReader();
      headers.push(['Content-Disposition', 'attachment; filename=\"' + update_file.name + '\"']);
      headers.push(['Link', '<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"']);
      headers.push(['Content-Type', update_file.type || 'application/octet-stream']);
      reader.onload = function(e) {
          fn(method, url, headers, e.target.result);
      };
      reader.readAsArrayBuffer(update_file);
    } else {
      headers.push(['Link', '<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"']);
      const turtle = document.getElementById('turtle_payload');
      if (turtle && turtle.value) {
        headers.push(['Content-Type', 'text/turtle']);
        fn(method, url, headers, turtle.value);
      } else {
        fn(method, url, headers, null);
      }
    }
  }

  function checkIfNonRdfResource(e) {
      const url = this.href;
      http('HEAD', url, function(res) {
        if (res.status >= 400 || res.getResponseHeader('Link') == null) {
          var newLocation = url;
          // Note: HEADing an external resource returns a temporary redirect to the external resource:
          // therefore there is no Link header. However what we want to see is the metadata 
          // for the external reference rather than  the external object itself.
          // (c.f. https://jira.duraspace.org/browse/FCREPO-2387)
          // WARNING: Fragile code relying on magic suffix '/fcr:metadata' and absence of 'Link' header 
          // on external resource.
          if(!url.match(/.*fcr:(metadata|tx)/)){
            newLocation = url + "/fcr:metadata";
          }
          location.href = newLocation ;
          return;
        }

        // Note: this Link header parsing works for the reference implementation, but it is not particularly robust
        const headers = res.getResponseHeader('Link').split(', ');
        const isNonRdfSource = headers.filter(function(h) { return h.match(/rel=["']?type["']?/) })
                                      .some(function(h) { return h.match(/NonRDFSource/) });

        if (isNonRdfSource) {
            const description = headers.filter(function(h) { return h.match(/rel="describedby"/)});
            if (description.length > 0) {
                location.href = description[0].substr(1, description[0].indexOf('>') - 1);
                return;
            }
        }

        location.href = url;
      });
      e.preventDefault();
  }

  function submitAndFollowLocation(e) {
      http('POST', e.target.getAttribute('action'), 'data', function(res) {
        if (res.status == 201 || res.status == 204) {
          window.location = res.getResponseHeader('Location');
        } else {
          ajaxErrorHandler(res);
        }
      });
      e.preventDefault();
  }

  function removeVersion(e) {
      const redirect = e.target.dataset.redirectAfterSubmit;
      http('DELETE', e.target.getAttribute('action'), function(res) {
        if (res.status == 204) {
          window.location = redirect;
        } else {
          ajaxErrorHandler(res, 'Error removing version');
        }
      });
      e.preventDefault();
  }

  function patchAndReload(e) {
      const redirect = e.target.dataset.redirectAfterSubmit;
      http('PATCH', e.target.getAttribute('action'), function(res) {
        if (res.status >= 400) {
          ajaxErrorHandler(res);
        } else {
          window.location = redirect;
        }
      });
      e.preventDefault();
  }

  function submitAndRedirectToBase(e) {
      const redirect = e.target.dataset.redirectAfterSubmit;
      http('POST', e.target.getAttribute('action'), function(res) {
        if (res.status >= 400) {
          ajaxErrorHandler(res);
        } else {
          window.location = redirect;
        }
      });
      e.preventDefault();
  }

  function sendSparqlUpdate(e) {
      const data = document.getElementById('sparql_update_query').value;
      http('PATCH', window.location, [['Content-Type', 'application/sparql-update']], data, function(res) {
        if (res.status == 204) {
          window.location.reload(true);
        } else {
          ajaxErrorHandler(res);
        }
      });
      e.preventDefault();
  }

  function createVersionSnapshot(e) {
      const uri = document.getElementById('main').getAttribute('resource');

      http('POST', uri + '/fcr:versions', function(res) {
        if (res.status == 201) {
          window.location = uri + '/fcr:versions';
        } else {
          ajaxErrorHandler(res);
        }
      });
      e.preventDefault();
  }

  function deleteItem(e) {
      const uri = document.getElementById('main').getAttribute('resource');
      const arr = uri.toString().split('/');
      arr.pop();

      http('DELETE', uri, function(res) {
        if (res.status == 204) {
          window.location = arr.join('/');
        } else {
          ajaxErrorHandler(res);
        }
      });
      e.preventDefault();
  }

  function updateFile(e) {
      const update_file = document.getElementById('update_file').files[0];
      if (!update_file) {
        return;
      }

      document.getElementById('binary_update_content').disabled = true;
      const url = window.location.href.replace('fcr:metadata', '');
      const reader = new FileReader();

      const headers = [
        ['Content-Disposition', 'attachment; filename=\"' + update_file.name + '\"'],
        ['Content-Type', update_file.type]];

      reader.onload = function(e) {
          http('PUT', url, headers, e.target.result, function(res) {
              if (res.status == 204 || res.status == 201) {
                  window.location.reload(true);
              } else {
                  ajaxErrorHandler(res, 'Error updating binary');
              }
          });
      };
      reader.readAsArrayBuffer(update_file);
      e.preventDefault();
  }

  function updateAccessRoles(e)
  {
      const update_json = document.getElementById('rbacl_json').value;
      const url = window.location + '/fcr:accessroles';
      http('POST', url, [['Content-Type', 'application/json']], update_json, function(res) {
          if (res.status == 204 || res.status == 201) {
              window.location.reload(true);
          } else {
              ajaxErrorHandler(res, 'Error');
          }
      });
      e.preventDefault();
  }

  function ajaxErrorHandler(xhr, errorThrown) {
      document.getElementById('errorLabel').textContent = errorThrown || xhr.statusText;
      document.getElementById('errorText').textContent = xhr.responseText;
      document.getElementById('showErrorModal').click();
      (document.getElementById('btn_action_create') || {}).disabled = false;
      (document.getElementById('binary_update_content') || {}).disabled = false;
  }

  ready(function() {
      listen('new_mixin', 'change', function(e) {
        document.getElementById('binary_payload_container').style.display = e.target.value == 'binary' ? 'block' : 'none';
        document.getElementById('turtle_payload_container').style.display = e.target.value == 'binary' ? 'none' : 'block';

      });

      listen('btn_action_create', 'click', function(e) {
        e.preventDefault();
        if (document.getElementById('new_mixin').value == 'binary') {
          const files = document.getElementById('binary_payload');
          if (files.value.length == 0) {
            files.click();
            return;
          }
        }
        addChild();
      });

      listen('action_sparql_update', 'submit', sendSparqlUpdate);
      listen('action_delete', 'submit', deleteItem);
      listen('action_create_transaction', 'submit', submitAndFollowLocation);
      listen('action_rollback_transaction', 'submit', submitAndRedirectToBase);
      listen('action_commit_transaction', 'submit', submitAndRedirectToBase);
      listen('action_revert', 'submit', patchAndReload);
      listen('action_remove_version', 'submit', removeVersion);
      listen('action_create_version', 'submit', createVersionSnapshot);
      listen('action_update_file', 'submit', updateFile);
      listen('update_rbacl', 'submit', updateAccessRoles);

      const links = document.querySelectorAll('a[property][href*="' + location.host + '"],#childList a,.breadcrumb a,.version_link');
      for (var i = 0; i < links.length; ++i) {
        links[i].addEventListener('click', checkIfNonRdfResource);
      }
  });
})();
