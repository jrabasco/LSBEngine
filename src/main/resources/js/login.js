var waiting = false;
function doLogin()
{
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
                alert("Password/username combination is wrong!");
            }
        });
    }
}
$(document).keypress(function (event) {
    if (event.which == 13) {
        doLogin();
    }
});