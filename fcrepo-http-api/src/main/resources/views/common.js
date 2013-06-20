
function addChild()
{
    var id = document.getElementById("new_id").value;
    var mixin = document.getElementById("new_mixin").value;
    var newURI = window.location + "/" + id;
    if ( mixin != '' ) {
        var postURI = newURI + "?mixin=" + mixin;
    } else {
        var postURI = newURI;
    }

    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            window.location = newURI;
        }
    }
    xhr.open('POST',postURI,true);
    xhr.send(null);
}
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
