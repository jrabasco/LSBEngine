var waiting = false;

function formatForForm(date) {
    var year = date.getFullYear();
    var month = leftPad(date.getMonth() + 1 + '', 2, '0');
    var day = leftPad(date.getDate() + '', 2, '0');
    var hours = leftPad(date.getHours() + '', 2, '0');
    var minutes = leftPad(date.getMinutes() + '', 2, '0');
    return year + '-' + month + '-' + day + 'T' + hours + ':' + minutes;
}

function leftPad(str, goalLength, padChar) {
    if (str.length < goalLength) {
        return new Array(goalLength - str.length + 1).join(padChar) + str;
    } else {
        return str;
    }
}

function toISOString(date) {
    var resDate = new Date(date);
    var offset = date.getTimezoneOffset();
    resDate.setMinutes(date.getMinutes() + offset);
    return resDate.toISOString();
}

function updatePost() {
    var form = $('form[name="post-edit"]');
    var loader = $('.loader');
    if (!waiting) {
        hideMessage("form-error");
        waiting = true;
        form.hide();
        loader.show();
        var id = parseInt(form[0].id.value);
        var title = form[0].title.value;
        var summary = form[0].summary.value;
        var content = form[0].content.value;
        //Assures consistency between firefox and chrome
        var publishedDate = new Date(form[0].publication.value + ":00Z");
        var publishedDateStr = toISOString(publishedDate);

        var post = {
            id: id,
            title: title,
            summary: summary,
            content: content,
            published: publishedDateStr
        };

        $.ajax({
            type: "POST",
            url: "/api/posts/" + id,
            data: JSON.stringify(post),
            cache: false,
            contentType: "application/json",
            headers: {
                'X-Csrf-Protection': form[0].csrf.value
            },
            success: function () {
                waiting = false;
                loader.hide();
                showMessage("form-success", "Update successful.");
                setTimeout(function () {
                    edit(id);
                }, 500);
            },
            error: function (resp) {
                console.log(resp);
                waiting = false;
                loader.hide();
                form.show();
                showMessage("form-error", "Could not update post for the following reason: " + resp.responseText);
            }
        });
    }
}

function publish() {
    if (!waiting && confirm("This will publish the post on the blog.")) {
        var publishDate = new Date();
        $("input[type='datetime-local']").val(formatForForm(publishDate));
        updatePost()
    }
}

$(function () {
    var dateStr = $("#actualdate").val();
    var date = new Date(parseInt(dateStr));
    $("input[type='datetime-local']").val(formatForForm(date));
});
