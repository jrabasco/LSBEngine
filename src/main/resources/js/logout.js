function doLogout()
{
    $.ajax
    ({
        type: "GET",
        url: "/api/logout",
        cache: false,
        xhrFields: {
            withCredentials: true
        },
        success: function (){
            window.location.reload();
        },
        error: function (){
            alert("Failed logout: retry or contact an administrator.");
        }
    });
}