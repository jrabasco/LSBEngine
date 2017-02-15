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
            goHome();
        },
        error: function (){
            alert("Failed logout: retry or contact an administrator.");
        }
    });
}

function edit(id)
{
    window.location.href = "/editform/" + id;
}

function goHome()
{
    window.location.href = "/";
}