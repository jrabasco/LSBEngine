var waiting = false;

function doLogout() {
    $.ajax({
        type: "GET",
        url: "/api/logout",
        cache: false,
        xhrFields: {
            withCredentials: true
        },
        success: function () {
            window.location.href = "/";
        },
        error: function () {
            alert("Failed logout: retry or contact an administrator.");
        }
    });
}

function deletePost(id, statusId, toHide) {
    var csrfInput = $('input[name="csrf"]');
    var toHideObject = $(toHide);
    var loader = $('.loader');
    if (!waiting && confirm("This will delete the post, are you sure you want to continue?")) {
        hideMessage(statusId+"-error");
        waiting = true;
        toHideObject.hide();
        loader.show();
        $.ajax({
            type: "DELETE",
            url: "/api/posts/" + id,
            cache: false,
            contentType: "application/json",
            headers: {
                'X-Csrf-Protection': csrfInput.val()
            },
            success: function () {
                waiting = false;
                loader.hide();
                showMessage(statusId+"-success", "Deletion successful.");
                setTimeout(function () {
                    postsHome()
                }, 500);
            },
            error: function (resp) {
                waiting = false;
                loader.hide();
                toHideObject.show();
                showMessage(statusId+"-error", "Could not delete post for the following reason: " + resp.responseText);
            }
        });
    }
}

function downloadTrash() {
    if (!waiting) {
        hideMessage("index-error");
        waiting = true;
        $.ajax({
            type: "GET",
            url: "/api/trash",
            cache: false,
            contentType: "application/json",
            success: function (resp) {
                waiting = false;
                var dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(resp, null, 2));
                var dlAnchorElem = $('#downloadAnchorElem')[0];
                dlAnchorElem.setAttribute("href", dataStr);
                dlAnchorElem.setAttribute("download", "deleted.json");
                dlAnchorElem.click();
                dlAnchorElem.removeAttribute("href");
                dlAnchorElem.removeAttribute("download");
            },
            error: function (resp) {
                waiting = false;
                showMessage("index-error", "Could not download deleted posts for the following reason: " + resp.responseText);
            }
        });
    }
}

function purgeTrash() {
    var csrfInput = $('input[name="csrf"]');
    if (!waiting) {
        hideMessage("index-error");
        waiting = true;
        $.ajax({
            type: "DELETE",
            url: "/api/trash",
            cache: false,
            contentType: "application/json",
            headers: {
                'X-Csrf-Protection': csrfInput.val()
            },
            success: function () {
                waiting = false;
                showMessage("posts-index-success", "Deleted posts purged.");
                setTimeout(function () {
                    hideMessage("posts-index-success");
                }, 1000);
            },
            error: function (resp) {
                waiting = false;
                showMessage("posts-index-error", "Could not purge deleted posts for the following reason: " + resp.responseText);
            }
        });
    }
}

function edit(id) {
    window.location.href = "/posts/edit/" + id;
}

function addPost() {
    window.location.href = "/posts/add";
}

function backHome() {
    window.location.href = "/";
}

function postsHome() {
    window.location.href = "/posts"
}

function showMessage(divId, message) {
    var errDiv = $("#" + divId);
    errDiv.empty();
    errDiv.text(message);
    errDiv.show();
}

function hideMessage(divId) {
    $("#" + divId).hide();
}

function checkStr(text, minLength) {
    return ((typeof text != "undefined") &&
        (typeof text.valueOf() == "string") &&
        (text.length >= minLength));
}
