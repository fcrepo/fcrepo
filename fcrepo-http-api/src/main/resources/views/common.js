
function addChild()
{
    var id = $("#new_id").val().trim();

    var mixin = $("#new_mixin").val();

    var newURI = $('#main').attr('resource') + "/" + id;

    if ( mixin != '' ) {
        var postURI = newURI + "?mixin=" + mixin;
    } else {
        var postURI = newURI;
    }

    if (mixin == "fedora:datastream") {
        var update_file = document.getElementById("datastream_payload").files[0];var reader = new FileReader();
        var xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function() {
            if (xhr.readyState == 4) {
                var loc = xhr.getResponseHeader('Location').replace("/fcr:content", "");

                if (loc != null) {
                    window.location = loc;
                } else {
                    window.location.reload();
                }
            }
        }

        if (id == "") {
            xhr.open( "POST", newURI + "/fcr:content" );
        } else {
            xhr.open( "PUT", newURI + "/fcr:content" );
        }

        xhr.setRequestHeader("Content-type", update_file.type || "application/octet-stream");
        reader.onload = function(e) {
            var result = e.target.result;
            var data = new Uint8Array(result.length);
            for (var i = 0; i < result.length; i++) {
                data[i] = (result.charCodeAt(i) & 0xff);
            }
            xhr.send(data.buffer);
        };
        reader.readAsBinaryString(update_file);
    } else {
        $.post(postURI, function(data, textStatus, request) {
            window.location = request.getResponseHeader('Location');
        });
    }

    return false;
}

function sendImport() {
    var mixin = $("#import_format").val();
    var postURI = $('#main').attr('resource') + "/fcr:import?format=" + mixin;

    var update_file = document.getElementById("import_file").files[0];
    var reader = new FileReader();
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            window.location.reload();
        }
    }

    xhr.open( "POST", postURI );

    xhr.setRequestHeader("Content-type", update_file.type || "application/octet-stream");
    reader.onload = function(e) {
        var result = e.target.result;
        var data = new Uint8Array(result.length);
        for (var i = 0; i < result.length; i++) {
            data[i] = (result.charCodeAt(i) & 0xff);
        }
        xhr.send(data.buffer);
    };
    reader.readAsBinaryString(update_file);

    return false;

}

$(function() {
    $('#new_mixin').change(function() {
        if($('#new_mixin').val() == "fedora:datastream") {
            $('#datastream_payload_container').show();
        } else {
            $('#datastream_payload_container').hide();
        }
    });

    $('#action_create').submit(addChild);
    $('#action_sparql_update').submit(sendSparqlUpdate);
    $('#action_register_namespace').submit(registerNamespace);
    $('#action_delete').submit(deleteItem);
    $('#action_create_transaction').submit(submitAndFollowLocation);
    $('#action_rollback_transaction').submit(submitAndRedirectToBase);
    $('#action_commit_transaction').submit(submitAndRedirectToBase);
    $('#action_import').submit(sendImport);
    $('#action_cnd_update').submit(sendCndUpdate);

});

function submitAndFollowLocation() {
    var $form = $(this);

    var postURI = $form.attr('action');

    $.post(postURI, "some-data-to-make-chrome-happy", function(data, textStatus, request) {
        window.location = request.getResponseHeader('Location');
    });

    return false;
}

function submitAndRedirectToBase() {
    var $form = $(this);


    var postURI = $form.attr('action');

    $.post(postURI, "some-data-to-make-chrome-happy", function(data, textStatus, request) {
        window.location = $form.attr('data-redirect-after-submit');
    });

    return false;
}


function registerNamespace() {
    var postURI = $('#main').attr('resource');


    var query = "INSERT { <" + $('#namespace_uri').val() + "> <http://fedora.info/definitions/v4/repository#hasNamespace> \"" + $('#namespace_prefix').val() + "\"} WHERE {}";


    $.ajax({url: postURI, type: "POST", contentType: "application/sparql-update", data: query, success: function(data, textStatus, request) {
        window.location.reload(true);
    }});

    return false;
}

function sendSparqlUpdate() {
    var postURI = $('#main').attr('resource');


    $.ajax({url: postURI, type: "PATCH", contentType: "application/sparql-update", data: $("#sparql_update_query").val(), success: function(data, textStatus, request) {
        window.location.reload(true);
    }, error: ajaxErrorHandler});

    return false;
}

function sendCndUpdate() {
    var postURI = $('#main').attr('resource');


    $.ajax({url: postURI, type: "POST", contentType: "text/cnd", data: $("#cnd_update_query").val(), success: function(data, textStatus, request) {
        window.location.reload(true);
    }, error: ajaxErrorHandler});

    return false;
}

function deleteItem()
{
    var uri = $('#main').attr('resource');
    var arr = uri.toString().split("/");
    arr.pop();
    var newURI = arr.join("/");

    $.ajax({url: uri, type: "DELETE", success: function() {
        window.location = newURI;
    }});
    return false;
}

function updateFile()
{
    var update_file = document.getElementById("update_file").files[0];
    var url = window.location + "/fcr:content";
    var reader = new FileReader();
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            window.location = url;
        }
    }
    xhr.open( "PUT", url );
    xhr.setRequestHeader("Content-type", update_file.type);
    reader.onload = function(e) {
        var result = e.target.result;
        var data = new Uint8Array(result.length);
        for (var i = 0; i < result.length; i++) {
            data[i] = (result.charCodeAt(i) & 0xff);
        }
        xhr.send(data.buffer);
    };
    reader.readAsBinaryString(update_file);
}

function ajaxErrorHandler(xhr, textStatus, errorThrown) {
    $('#errorLabel').text(errorThrown);
    $('#errorText').text(xhr.responseText);
    $('#errorModal').modal('show');

}

function addProperty() {
    var sub = $('#main').attr('resource');
    var pre = document.getElementById("add-pre-custom").value;
    if ( pre == '' ) {
        var preMenu = document.getElementById("add-pre-menu");
        pre = preMenu.options[preMenu.selectedIndex].value;
    }
    var obj = quoteObject( document.getElementById("add-val").value );
    var update = "insert data { <" + sub + "> <" + pre + "> " + obj + " }";

    // perform sparql update
    $.ajax({url: sub, type: "PATCH", contentType: "application/sparql-update", data: update, success: function(data, textStatus, request) {
        window.location.reload(true);
    }, error: ajaxErrorHandler});
    return false;
}

function deleteProperty( id ) {
    var elem = document.getElementById(id);
    var sub = $('#main').attr('resource');
    var pre = elem.getAttribute('property');
    var obj = quoteObject( elem.firstChild.data );
    if ( obj.startsWith('"') && obj.indexOf("^^") > -1 ) {
        obj = obj.substring( 0, obj.indexOf("^^") );
    }
    var update = "delete data { <" + sub + "> <" + pre + "> " + obj + " }";

    // perform sparql update
    $.ajax({url: sub, type: "PATCH", contentType: "application/sparql-update", data: update, success: function(data, textStatus, request) {
        window.location.reload(true);
    }, error: ajaxErrorHandler});
    return false;
}

function quoteObject( obj ) {
    if (obj.charAt(0) != '"' && obj.charAt(0) != "'" && obj.charAt(0) != '<') {
        if (obj.substring(0,4) == "http") {
            obj = "<" + obj + ">";
        } else {
            obj = "\"" + obj.replace(/\"/g,"\\\"") + "\"";
        }
    }
  return obj;
}
