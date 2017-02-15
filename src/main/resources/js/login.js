var waiting = false;
function doLogin() {
    hideError("login-error");
    var form = $('form[name="login"]');
    var loader = $('.loader');
    if (!waiting) {
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
                showError("login-error", "Password/username combination is incorrect.");
            }
        });
    }
}
$(document).keypress(function (event) {
    if (event.which == 13) {
        doLogin();
    }
});

$(function () {
    $(":text, :password").keyup(function () {
        var enteredText = $(this).val();
        if ((typeof enteredText != "undefined") &&
            (typeof enteredText.valueOf() == "string") &&
            (enteredText.length > 0)) {
            $(this).removeClass("not-ok");
            $(this).addClass("ok")
        } else {
            $(this).removeClass("ok");
            $(this).addClass("not-ok")
        }
    });
});
