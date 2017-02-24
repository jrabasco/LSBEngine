var waiting = false;
function doLogin() {
    var form = $('form[name="login"]');
    var loader = $('.loader');
    if (!waiting) {
        hideMessage("login-error");
        waiting = true;
        form.hide();
        loader.show();
        $.ajax
        ({
            type: "POST",
            url: "/api/token",
            dataType: 'json',
            data: '{"username":"' + form[0].username.value + '",' + '"password":"' + form[0].password.value + '"}',
            cache: false,
            xhrFields: {
                withCredentials: true
            },
            contentType: "application/json",
            success: function () {
                waiting = false;
                window.location.reload();
            },
            error: function () {
                waiting = false;
                loader.hide();
                form.show();
                showMessage("login-error", "Password/username combination is incorrect.");
            }
        });
    }
}

function resetLogin() {
    hideMessage("login-error");
    var form = $('form[name="login"]');
    form[0].username.value = "";
    $(form[0].username).removeClass("ok");
    $(form[0].username).addClass("not-ok");
    form[0].password.value = "";
    $(form[0].password).removeClass("ok");
    $(form[0].password).addClass("not-ok");
}

$(document).keypress(function (event) {
    if (event.which == 13) {
        doLogin();
    }
});

$(function () {
    $(":text, :password").keyup(function () {
        var enteredText = $(this).val();
        if (checkStr(enteredText, 1)) {
            $(this).removeClass("not-ok");
            $(this).addClass("ok");
            hideMessage("login-error");
        } else {
            $(this).removeClass("ok");
            $(this).addClass("not-ok");
        }
    });
});
