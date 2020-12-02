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
      if (mixin == 'basic container') {
        headers.push(['Link', '<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"']);
      } else if (mixin == 'direct container') {
        headers.push(['Link', '<http://www.w3.org/ns/ldp#DirectContainer>; rel=\"type\"']);
      } else if (mixin == 'indirect container') {
        headers.push(['Link', '<http://www.w3.org/ns/ldp#IndirectContainer>; rel=\"type\"']);
      } else if (mixin == 'archival group') {
        headers.push(['Link', '<http://fedora.info/definitions/v4/repository#ArchivalGroup>; rel=\"type\"']);
      } else {
        alert("Unrecognized type: " + mixin);
        return;
      }
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
      const d = new Date();
      const name = 'version.' + d.getFullYear().toString() + (d.getMonth()+1).toString() + d.getDate().toString() + d.getHours() + d.getMinutes() + d.getSeconds();

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

  /*
   * Do the search
   */
  function doSearch(e) {
      const searchUri = document.getElementById('main').getAttribute('page');
      if (searchUri != undefined) {
          const searchTerms = collectSearch();
          const newUri = searchUri + searchTerms;
          http('GET', newUri + (searchTerms.length > 0 ? '&' : '?') + 'max_results=0', function(res) {
              if (res.status == 200) {
                  window.location = newUri;
              } else {
                  ajaxErrorHandler(res);
              }
          });
      }
      e.preventDefault();
  }

  /*
   * Collect all the various query boxes and make a search.
   */
  function collectSearch() {
      const numFields = parseInt(document.getElementById('search_count').getAttribute('value'));
      var condition_string="";
      for (var f = 1; f <= numFields; f += 1) {
          let condition = document.getElementById('condition_' + f);
          let operator = document.getElementById('operator_' + f);
          let svalue = document.getElementById('search_value_' + f);
          if (condition != null && operator != null && svalue != null && svalue.value != '') {
              condition_string += (condition_string.length > 0 ? "&" : "") + "condition=" + encodeSearchCondition(condition.value + operator.value + svalue.value);
          }
      }
      return (condition_string.length > 0 ? '?' : '') + condition_string;
  }

  /*
   * Check for conditions on the URL and parse them if necessary.
   */
  function initializeSearchPage() {
      const searchUri = document.getElementById('main').getAttribute('page');
      if (searchUri != null && searchUri != '') {
          // On the search page
          const actionForm = document.getElementById('action_search');
          const beforeNode = document.getElementById('search_count');
          if (String(window.location).indexOf('?') > -1) {
              const location = String(window.location);
              const querystring = location.substring(location.indexOf('?') + 1);
              var conditions = decodeSearchString(querystring);
              if (conditions['condition'] != null) {
                  for (var foo = 0; foo < conditions["condition"].length; foo += 1) {
                      const c = getConditionParts(conditions['condition'][foo]);
                      buildSearch(c, foo + 1, actionForm, beforeNode);
                  }
              } else {
                  // Build a blank search box.
                  buildSearch({}, 1, actionForm, beforeNode);
              }
          } else {
              // Build a blank search box.
              buildSearch({}, 1, actionForm, beforeNode);
          }
      }
  }

  /**
   * Build a set of search boxes, and see values to match object condition.
   */
  function buildSearch(condition, count, theForm, beforeNode) {
     let wrapper = document.createElement('div');
     wrapper.setAttribute('class', 'form-group');
     let label1 = document.createElement('label');
     label1.setAttribute('for', 'condition_' + count);
     label1.setAttribute('class', 'control-label');
     label1.textContent="Field";
     wrapper.append(label1);
     let localfield = document.createElement('select');
     localfield.setAttribute('id', 'condition_' + count);
     fields.forEach(function(f) {
         let o = document.createElement('option');
         o.setAttribute('value', f);
         if (f == condition['field']) {
             o.setAttribute('selected', 'true');
         }
         o.textContent=f;
         localfield.append(o);
     });
     wrapper.append(localfield);
     let label2 = document.createElement('label');
     label2.setAttribute('for', 'operator_' + count);
     label2.setAttribute('class', 'control-label');
     label2.textContent="Operator";
     wrapper.append(label2);
     let localoperator = document.createElement('select');
     localoperator.setAttribute('id', 'operator_' + count);
     operators.forEach(function(f) {
         let o = document.createElement('option');
         o.setAttribute('value', f);
         if (f == condition['operator']) {
             o.setAttribute('selected', 'true');
         }
         o.textContent=f;
         localoperator.append(o);
     });
     wrapper.append(localoperator);
     let br = document.createElement('br');
     wrapper.append(br);
     let label3 = document.createElement('label');
     label3.setAttribute('for', 'search_value_' + count);
     label3.setAttribute('class', 'control-label');
     label3.textContent="Query term";
     wrapper.append(label3);
     let localvalue = document.createElement('input');
     localvalue.setAttribute('type', 'text');
     localvalue.setAttribute('id', 'search_value_' + count);
     localvalue.setAttribute('class', 'form-control');
     localvalue.setAttribute('placeholder', 'info:fedora/*');
     if (condition['value'] != null && condition['value'] != '') {
         localvalue.setAttribute('value', condition['value']);
     }
     wrapper.append(localvalue);
     theForm.insertBefore(wrapper, beforeNode);
  }

  /*
   * Encode comparison operators in conditions.
   */
  function encodeSearchCondition(condition) {
      return encodeURIComponent(condition).replaceAll('<', '%3C').replaceAll('=', '%3D').replaceAll('>', '%3E')
      .replaceAll('%26', '&');
  }

  /*
   * Decode a search string into an object with search parameter -> parameter value.
   */
  function decodeSearchString(querystring) {
      var searchParts = {};
      if (querystring.indexOf('&') > -1) {
          // Multiple parts
          const parts = querystring.split('&');
          parts.forEach(function(p) {
              searchParts = getSearchPart(p, searchParts);
          });
      } else {
          searchParts = getSearchPart(querystring, searchParts);
      }
      return searchParts;
  }

  /*
   * Parse bits out of the querystring.
   */
  function getSearchPart(searchPart, result) {
      if (searchPart.indexOf("=") > -1) {
           const bits = searchPart.split("=");
           if (bits[0] == "condition") {
               if (result["condition"] == null) {
                   result["condition"] = [];
               }
               result["condition"].push(decodeURIComponent(bits[1]));
           } else if (bits[0] == "fields") {
               if (result["fields"] == null) {
                   result["fields"] = [];
               }
               result["fields"].push(decodeURIComponent(bits[1]));
           }
      }
      return result;
  }

  /*
   * Decode a condition into an object with field, operator and value properties.
   */
  function getConditionParts(conditionString) {
     const operator = /([<>=]+)/;
     if (operator.test(conditionString)) {
        const result = {};
        const bits = conditionString.split(operator);
        result['field'] = bits[0];
        result['operator'] = bits[1];
        result['value'] = bits[2];
        return result;
     }
     return null;
  }

  function ajaxErrorHandler(xhr, errorThrown) {
      document.getElementById('errorLabel').textContent = errorThrown || xhr.statusText;
      document.getElementById('errorText').textContent = xhr.responseText;
      document.getElementById('showErrorModal').click();
      (document.getElementById('btn_action_create') || {}).disabled = false;
      (document.getElementById('binary_update_content') || {}).disabled = false;
  }

  function enableVersioning(e) {
      const url = document.getElementById('main').getAttribute('resource');
      const d = new Date();
      const expires = (new Date(d - 1000000)).toUTCString();
      const get_headers = [
          ['Prefer', 'return=representation; omit="http://fedora.info/definitions/v4/repository#ServerManaged"'],
          ['Accept', 'application/ld+json'],
          ['Cache-Control', 'no-cache, no-store, max-age=0'],
          ['Expires', expires]
      ];
      const put_headers = [
          ['Prefer', 'handling=lenient; received="minimal"'],
          ['Content-Type', 'application/ld+json'],
          ['Link', '<http://mementoweb.org/ns#OriginalResource>; rel="type"']
      ];
      http('GET', url, get_headers, function(res) {
          if (res.status == 200) {
              var body = res.responseText;
              http('PUT', url, put_headers, body, function(res) {
                  if (res.status == 204) {
                      window.location.reload(true);
                  } else {
                      ajaxErrorHandler(res, 'Error');
                  }
              });
          } else {
              ajaxErrorHandler(res, 'Error');
          }
      });
      e.preventDefault();
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
      listen('action_enable_version', 'submit', enableVersioning);
      listen('action_update_file', 'submit', updateFile);
      listen('action_search', 'submit', doSearch);

      const links = document.querySelectorAll('a[property][href*="' + location.host + '"],#childList a,.breadcrumb a,.version_link');
      for (var i = 0; i < links.length; ++i) {
        links[i].addEventListener('click', checkIfNonRdfResource);
      }
      initializeSearchPage();
  });
})();
