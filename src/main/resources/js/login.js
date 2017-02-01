function doLogin(form)
{
    $.ajax
    ({
        type: "POST",
        url: "/api/token",
        dataType: 'json',
        data: '{"username":"' + form.username.value + '",' + '"password":"' + form.password.value + '"}',
        cache: false,
        xhrFields: {
            withCredentials: true
        },
        contentType: "application/json",
        success: function (){
            window.location.reload();
        },
        error: function (){
            alert("Password/username combination is wrong!");
        }
    });
}
$(document).keypress(function (event) {
    if (event.which == 13) {
        doLogin($('form[name="login"]')[0])
    }
});