
function addChild()
{
    var id = $("#new_id").val().trim();

    if ( id == "") {
        id = "fcr:new";
    }

    var mixin = $("#new_mixin").val();

    var newURI = $('#main').attr('resource') + "/" + id;

    if ( mixin != '' ) {
        var postURI = newURI + "?mixin=" + mixin;
    } else {
        var postURI = newURI;
    }

    $.post(postURI, function(data, textStatus, request) {
        window.location = request.getResponseHeader('Location');
    });

    return false;
}

$(function() {
$('#action_create').submit(addChild);

});

function deleteItem()
{
    var uri = window.location;
    var arr = uri.toString().split("/");
    arr.pop();
    var newURI = arr.join("/");

    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            window.location = newURI;
        }
    }
    xhr.open('DELETE',uri,true);
    xhr.send(null);
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
