var waiting = false;
function changePassword() {
    var form = $('form[name="edit-password"]');
    var loader = $('.loader');
    if (!waiting) {
        hideMessage("edit-password-error");
        waiting = true;
        form.hide();
        loader.show();
        var oldpassword = form[0].oldpassword.value;
        var newpassword = form[0].newpassword.value;
        var confpassword = form[0].confpassword.value;

        if (!checkStr(newpassword, 10) || !checkStr(confpassword, 10)) {
            showMessage("edit-password-error", "New password is too short (minimum 10 characters).");
            form.show();
            loader.hide();
            waiting = false;
            return;
        }

        if (newpassword != confpassword) {
            showMessage("edit-password-error", "The new password and the confirmation should match.");
            form.show();
            loader.hide();
            waiting = false;
            return;
        }

        var newCredentials = {
            username: form[0].username.value,
            oldPassword: oldpassword,
            newPassword: newpassword
        };

        $.ajax
        ({
            type: "POST",
            url: "/api/new_password",
            data: JSON.stringify(newCredentials),
            cache: false,
            contentType: "application/json",
            headers: {
                'X-Csrf-Protection': form[0].csrf.value
            },
            success: function () {
                waiting = false;
                loader.hide();
                form.show();
                resetFields();
                showMessage("edit-password-success", "Update successful.");
                setTimeout(function () {
                    hideMessage("edit-password-success");
                }, 1000);
            },
            error: function () {
                waiting = false;
                loader.hide();
                form.show();
                showMessage("edit-password-error", "Password is incorrect.");
            }
        });
    }
}

function resetFields() {
    hideMessage("edit-password-error");
    var form = $('form[name="edit-password"]');
    form[0].oldpassword.value = "";
    $(form[0].oldpassword).removeClass("ok");
    $(form[0].oldpassword).addClass("not-ok");
    form[0].newpassword.value = "";
    $(form[0].newpassword).removeClass("ok");
    $(form[0].newpassword).addClass("not-ok");
    form[0].confpassword.value = "";
    $(form[0].confpassword).removeClass("ok");
    $(form[0].confpassword).addClass("not-ok");
}

$(document).keypress(function (event) {
    if (event.which == 13) {
        changePassword();
    }
});

$(function () {
    $(":text, :password").keyup(function () {
        var enteredText = $(this).val();
        if (checkStr(enteredText, 1)) {
            $(this).removeClass("not-ok");
            $(this).addClass("ok")
        } else {
            $(this).removeClass("ok");
            $(this).addClass("not-ok")
        }
    });
});