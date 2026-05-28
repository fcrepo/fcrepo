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
                    ajaxErrorHandler(res, `Error creating ${mixin}`);
                }
            });
        }

        if (mixin == 'binary') {
            const update_file = document.getElementById('binary_payload').files[0];
            const reader = new FileReader();

            const mime_type = document.getElementById('mime_type').value
                || update_file.type
                || 'application/octet-stream';

            headers.push(['Content-Disposition', 'attachment; filename=\"' + update_file.name + '\"']);
            headers.push(['Link', '<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"']);
            headers.push(['Content-Type', mime_type]);
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
                if(!url.match(/.*fcr:(metadata|tx|fixity|versions)/)){
                    newLocation = url + "/fcr:metadata";
                }
                location.href = newLocation;
                return;
            }

            // Note: this Link header parsing works for the reference implementation, but it is not particularly robust
            const headers = res.getResponseHeader('Link').split(', ');
            const isMemento = headers.filter((h) => h.match(/rel=["']?type["']?/))
                .some((h) => h.match(/Memento/));

            const isNonRdfSource = headers.filter((h) => h.match(/rel=["']?type["']?/))
                .some((h) => h.match(/NonRDFSource/));

            if (isNonRdfSource) {
                const description = headers.filter((h) => h.match(/rel="describedby"/));
                if (description.length > 0 && isMemento) {
                    const last = description.length - 1;
                    location.href = description[last].substr(1, description[last].indexOf('>') - 1);
                    return;
                } else if (description.length > 0) {
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

        const mime_type = document.getElementById('update_mime_type').value
            || update_file.type
            || 'application/octet-stream';

        const headers = [
            ['Content-Disposition', 'attachment; filename=\"' + update_file.name + '\"'],
            ['Content-Type', mime_type]];

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
     * Updates the mime type based on the file selected, or clears the type if the type is unknown
     */
    function updateMimeType(target, mimeInputId) {
        const mime_input = document.getElementById(mimeInputId);
        const file = target.files[0];

        if (file != null && file.type != null) {
            mime_input.value = file.type;
        } else {
            mime_input.value = '';
        }
    }

    /*
     * Do the search
     */
    function doSearch(e) {
        const searchUri = document.getElementById('main').getAttribute('page');
        if (searchUri != undefined) {
            const searchTerms = collectSearch();
            const newUri = searchUri + searchTerms;
            http('GET', newUri, function(res) {
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
        const params = [];
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
        params.push(condition_string);

        var fields_string = "";
        const options = document.getElementById('search_fields');
        const selected = options.selectedOptions || [];
        Array.from(selected).forEach((field, index) => {
            fields_string += (index === 0 ? 'fields=' : ',' ) + field.value;
        });
        params.push(fields_string);

        const max = document.getElementById('max_results_value').value;
        const pageNum = document.getElementById('page_num_value').value;
        const offset = `offset=${pageNum * max}`;
        const maxResults = `max_results=${max}`;
        params.push(offset, maxResults);

        const reducer = (prev, curr) => prev.length === 1 ? `${prev}${curr}` : `${prev}&${curr}`;
        return params.filter(param => param !== '').reduce(reducer, '?');
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

            const buttonGroup = document.createElement('div');
            buttonGroup.setAttribute('class', 'form-group')
            const addButton = document.createElement('button');
            addButton.setAttribute('type', 'button');
            addButton.setAttribute('class', 'btn btn-secondary btn-sm');
            addButton.addEventListener('click', addSearchCondition);
            addButton.appendChild(document.createTextNode('Add Condition'));
            buttonGroup.appendChild(addButton);
            actionForm.insertBefore(buttonGroup, beforeNode);

            const location = String(window.location);
            const querystring = location.substring(location.indexOf('?') + 1);
            var query = decodeSearchString(querystring);
            addFieldSelect(query);
            addPagination(query);

            createConditionContainer();
            const condition = query.condition || [];
            if (condition.length > 0) {
                condition.map(getConditionParts).forEach(addSearchCondition);
            } else {
                addSearchCondition();
            }
        }
    }

    function createConditionContainer() {
        const form = document.getElementById('action_search');
        const paginationNode = document.getElementById('search_pagination');
        let wrapper = document.createElement('ul');
        wrapper.setAttribute('class', 'list-group form-group');
        wrapper.setAttribute('id', 'search_conditions');
        form.insertBefore(wrapper, paginationNode);
    }

    /**
     * Add the search inputs needed for the given search condition.
     * If the condition is empty, create the default search inputs.
     */
    function addSearchCondition(condition) {
        const {
            field,
            operator,
            value,
        } = condition || {};

        const list = document.getElementById('search_conditions');
        const countNode = document.getElementById('search_count');
        const count = Number(countNode.value);

        let wrapper = document.createElement('li');
        wrapper.setAttribute('class', 'list-group-item');
        wrapper.setAttribute('id', 'condition_group_' + count);

        let label1 = document.createElement('label');
        label1.setAttribute('for', 'condition_' + count);
        label1.setAttribute('class', 'control-label');
        label1.textContent="Field";
        wrapper.appendChild(label1);
        let localfield = document.createElement('select');
        localfield.setAttribute('id', 'condition_' + count);

        fields.forEach(function(f) {
            let o = document.createElement('option');
            o.setAttribute('value', f);
            if (f === field) {
                o.setAttribute('selected', 'true');
            }
            o.textContent=f;
            localfield.appendChild(o);
        });

        wrapper.appendChild(localfield);
        let label2 = document.createElement('label');
        label2.setAttribute('for', 'operator_' + count);
        label2.setAttribute('class', 'control-label');
        label2.textContent="Operator";
        wrapper.appendChild(label2);
        let localoperator = document.createElement('select');
        localoperator.setAttribute('id', 'operator_' + count);

        operators.forEach(function(f) {
            let o = document.createElement('option');
            o.setAttribute('value', f);
            if (f === operator) {
                o.setAttribute('selected', 'true');
            }
            o.textContent=f;
            localoperator.appendChild(o);
        });

        wrapper.appendChild(localoperator);

        const badge = document.createElement('button');
        badge.setAttribute('type', 'button');
        badge.setAttribute('class', 'badge btn btn-danger');
        badge.setAttribute('aria-label', 'Remove Condition');
        badge.innerHTML = '&times;'; // shows a visible ×

        const glyph = document.createElement('span');
        glyph.setAttribute('class', 'glyphicon glyphicon-remove');
        glyph.setAttribute('aria-hidden', 'true');
        badge.appendChild(glyph);
        badge.addEventListener('click', () => {removeSearchCondition(count)});
        wrapper.appendChild(badge);

        wrapper.appendChild(document.createElement('br'));
        let label3 = document.createElement('label');
        label3.setAttribute('for', 'search_value_' + count);
        label3.setAttribute('class', 'control-label');
        label3.textContent="Query term";
        wrapper.appendChild(label3);
        let localvalue = document.createElement('input');
        localvalue.setAttribute('type', 'text');
        localvalue.setAttribute('id', 'search_value_' + count);
        localvalue.setAttribute('class', 'form-control');
        localvalue.setAttribute('placeholder', 'info:fedora/*');
        if (value !== undefined && value !== '') {
            localvalue.setAttribute('value', value);
        }
        wrapper.appendChild(localvalue);

        list.appendChild(wrapper);
        countNode.value++;
    }

    function removeSearchCondition(condition) {
        const group = document.getElementById('condition_group_' + condition);
        group.remove();
    }

    function addFieldSelect(query) {
        const selectedFields = query.fields || [];

        const form = document.getElementById('action_search');
        const countNode = document.getElementById('search_count');

        const div = document.createElement('div');
        div.setAttribute('id', 'field_group');
        div.setAttribute('class', 'form-group');

        const label = document.createElement('label');
        label.setAttribute('for', 'search_fields');
        label.setAttribute('class', 'control-label');
        label.textContent = 'Display Fields'
        div.appendChild(label);

        const select = document.createElement('select');
        select.setAttribute('multiple', null);
        select.setAttribute('class', 'form-control');
        select.setAttribute('id', 'search_fields');

        fields.forEach(field => {
            const option = document.createElement('option');
            option.setAttribute('value', field);
            option.setAttribute('class', 'field_option');
            option.textContent = field;
            if (selectedFields.includes(field)) {
                option.selected = true;
            }
            select.appendChild(option);
        });
        div.appendChild(select);

        form.insertBefore(div, countNode);
    }

    function addPagination(query) {
        const offset = query.offset || 0;
        const max_results = query.max_results || 10;

        const pageNum = Math.floor(offset / max_results);

        const form = document.getElementById('action_search');
        const fieldSelect = document.getElementById('field_group');

        const pageDiv = document.createElement('div');
        pageDiv.setAttribute('id', 'search_pagination');
        pageDiv.setAttribute('class', 'form-group form-inline');

        const pageNumLabel = document.createElement('label');
        pageNumLabel.setAttribute('for', 'max_results_value');
        pageNumLabel.setAttribute('class', 'control-label');
        pageNumLabel.textContent = "Page";
        pageDiv.appendChild(pageNumLabel);
        const pageNumValue = document.createElement('input');
        pageNumValue.setAttribute('type', 'number');
        pageNumValue.setAttribute('id', 'page_num_value');
        pageNumValue.setAttribute('class', 'form-control');
        pageNumValue.setAttribute('style', 'width: 30%');
        pageNumValue.setAttribute('min', '0');
        pageNumValue.setAttribute('value', pageNum);

        pageDiv.appendChild(pageNumLabel);
        pageDiv.appendChild(pageNumValue);

        const pageSizeLabel = document.createElement('label');
        pageSizeLabel.setAttribute('for', 'max_results_value');
        pageSizeLabel.setAttribute('class', 'control-label');
        pageSizeLabel.textContent = "Page size";
        pageDiv.appendChild(pageSizeLabel);
        const pageSizeValue = document.createElement('input');
        pageSizeValue.setAttribute('type', 'number');
        pageSizeValue.setAttribute('id', 'max_results_value');
        pageSizeValue.setAttribute('class', 'form-control');
        pageSizeValue.setAttribute('style', 'width: 30%');
        pageSizeValue.setAttribute('min', '1');
        pageSizeValue.setAttribute('max', '100');

        if (max_results != null) {
            pageSizeValue.setAttribute('value', max_results);
        } else {
            pageSizeValue.setAttribute('value', '10');
        }

        pageDiv.appendChild(pageSizeValue);
        form.insertBefore(pageDiv, fieldSelect);
    }

    /*
     * Encode comparison operators in conditions.
     */
    function encodeSearchCondition(condition) {
        return encodeURIComponent(condition).replace('/</g', '%3C').replace('/=/g', '%3D').replace('/>/g', '%3E')
            .replace('/%26/g', '&');
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
                result["fields"] = decodeURIComponent(bits[1]).split(',');
            } else if (bits[0] == "max_results") {
                result["max_results"] = decodeURIComponent(bits[1]);
            } else if (bits[0] == "offset") {
                result["offset"] = decodeURIComponent(bits[1]);
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
        // Trigger modal directly using Bootstrap 5 API
        const errorModal = new bootstrap.Modal(document.getElementById('errorModal'));
        errorModal.show();
        (document.getElementById('btn_action_create') || {}).disabled = false;
        (document.getElementById('binary_update_content') || {}).disabled = false;
    }

    ready(function() {
        listen('new_mixin', 'change', function(e) {
            document.getElementById('binary_payload_container').style.display = e.target.value == 'binary' ? 'block' : 'none';
            document.getElementById('binary_mime_type_container').style.display = e.target.value == 'binary' ? 'block' : 'none';
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
        listen('action_create_version', 'submit', createVersionSnapshot);
        listen('action_update_file', 'submit', updateFile);
        listen('action_search', 'submit', doSearch);

        listen('update_file', 'change', function(e) {
            updateMimeType(e.target, 'update_mime_type');
        });
        listen('binary_payload', 'change', function(e) {
            updateMimeType(e.target, 'mime_type');
        });

        const links = document.querySelectorAll('a[property][href*="' + location.host + '"],#childList a,.breadcrumb a,.version_link');
        for (var i = 0; i < links.length; ++i) {
            links[i].addEventListener('click', checkIfNonRdfResource);
        }
        initializeSearchPage();
    });
})();
