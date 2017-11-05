var waiting = false;
showdown.setOption('headerLevelStart', 2);
//Extension
showdown.extension('header-anchors', function() {

	var ancTpl = '$1<a id="user-content-$3" class="anchor" href="#$3" aria-hidden="true"><img src="/assets/link.svg" /></a>$4';

  return [{
    type: 'html',
    regex: /(<h([1-4]) id="([^"]+?)">)(.*<\/h\2>)/g,
    replace: ancTpl
  }];
});

function getConverter() {
	return new showdown.Converter({
		  extensions: ['header-anchors']
	});
}

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

function delDoc(type, id, statusId, toHide, homeFn) {
    var csrfInput = $('input[name="csrf"]');
    var toHideObject = $(toHide);
    var loader = $('.loader');
    if (!waiting && confirm("This will delete the post, are you sure you want to continue?")) {
        hideMessage(statusId + "-error");
        waiting = true;
        toHideObject.hide();
        loader.show();
        $.ajax({
            type: "DELETE",
            url: "/api/"+type+"/" + id,
            cache: false,
            contentType: "application/json",
            headers: {
                'X-Csrf-Protection': csrfInput.val()
            },
            success: function () {
                waiting = false;
                loader.hide();
                showMessage(statusId + "-success", "Deletion successful.");
                setTimeout(function () {
                    homeFn()
                }, 500);
            },
            error: function (resp) {
                waiting = false;
                loader.hide();
                toHideObject.show();
                showMessage(statusId + "-error", "Could not delete for the following reason: " + resp.responseText);
            }
        });
    }
}

function downloadTrash(type, statusId) {
    if (!waiting) {
        hideMessage(statusId + "-error");
        waiting = true;
        $.ajax({
            type: "GET",
            url: "/api/trash/" + type,
            cache: false,
            contentType: "application/json",
            success: function (resp) {
                waiting = false;
                var dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(resp, null, 2));
                var dlAnchorElem = $('#downloadAnchorElem')[0];
                dlAnchorElem.setAttribute("href", dataStr);
                dlAnchorElem.setAttribute("download", "deleted"+type+".json");
                dlAnchorElem.click();
                dlAnchorElem.removeAttribute("href");
                dlAnchorElem.removeAttribute("download");
            },
            error: function (resp) {
                waiting = false;
                showMessage(statusId + "-error", "Could not download deleted items for the following reason: " + resp.responseText);
            }
        });
    }
}

function purgeTrash(type, statusId) {
    var csrfInput = $('input[name="csrf"]');
    if (!waiting) {
        hideMessage(statusId + "-error");
        waiting = true;
        $.ajax({
            type: "DELETE",
            url: "/api/trash/" + type,
            cache: false,
            contentType: "application/json",
            headers: {
                'X-Csrf-Protection': csrfInput.val()
            },
            success: function () {
                waiting = false;
                showMessage(statusId + "-success", "Deleted items purged.");
                setTimeout(function () {
                    hideMessage(statusId + "-success");
                }, 1000);
            },
            error: function (resp) {
                waiting = false;
                showMessage(statusId + "-error", "Could not purge deleted items for the following reason: " + resp.responseText);
            }
        });
    }
}

function edit(type, id) {
    window.location.href = "/" + type + "/edit/" + id;
}

function add(type) {
    window.location.href = "/" + type + "/add";
}

function backHome() {
    window.location.href = "/";
}

function postsHome() {
    window.location.href = "/posts"
}

function projectsHome() {
    window.location.href = "/projects"
}

function editAbout() {
    window.location.href = "/perso/edit"
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

function updateAbout() {
    var form = $('form[name="aboutme-edit"]');
    var loader = $('.loader');
    if (!waiting) {
        hideMessage("form-error");
        waiting = true;
        form.hide();
        loader.show();

        var converter = getConverter();
        var introMarkdown = form[0].intromarkdown.value;
        var introHtml = converter.makeHtml(introMarkdown);

        var resumeMarkdown = form[0].resumemarkdown.value;
        var resumeHtml = converter.makeHtml(resumeMarkdown);

        var aboutMe = {};

        if (checkStr(introMarkdown, 1)) {
            aboutMe["introduction"] = {
                html: introHtml,
                markdown: introMarkdown
            }
        }

        if (checkStr(resumeMarkdown, 1)) {
            aboutMe["resume"] = {
                html: resumeHtml,
                markdown: resumeMarkdown
            }
        }

        $.ajax({
            type: "PUT",
            url: "/api/perso",
            data: JSON.stringify(aboutMe),
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
                    editAbout();
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

function update(type, add, formName) {
    var form = $('form[name="'+formName+'"]');
    var loader = $('.loader');
    if (!waiting) {
        hideMessage("form-error");
        waiting = true;
        form.hide();
        loader.show();
        var id = parseInt(form[0].id.value);
        var title = form[0].title.value;
        var abstract = form[0].abstract.value;

        var converter = getConverter();
        var contentMarkdown = form[0].contentmarkdown.value;
        var contentHtml = converter.makeHtml(contentMarkdown);
        //Ensures consistency between firefox and chrome
        var publishedDate = new Date(form[0].publication.value + ":00Z");
        var publishedDateStr = toISOString(publishedDate);

        var doc = {
            id: id,
            title: title,
            abstract: abstract,
            content: {
                html: contentHtml,
                markdown: contentMarkdown
            },
            published: publishedDateStr
        };
        
        if (type === "posts") {
        	addPostValues(form, doc);
        }

        var reqType = add ? "POST" : "PUT";
        var urlEnd = add ? "" : "/" + id;

        $.ajax({
            type: reqType,
            url: "/api/" + type + urlEnd,
            data: JSON.stringify(doc),
            cache: false,
            contentType: "application/json",
            headers: {
                'X-Csrf-Protection': form[0].csrf.value
            },
            success: function (resp) {
                waiting = false;
                loader.hide();
                showMessage("form-success", "Update successful.");
                var editId = add ? resp.id : id;
                setTimeout(function () {
                    edit(type, editId);
                }, 500);
            },
            error: function (resp) {
                console.log(resp);
                waiting = false;
                loader.hide();
                form.show();
                showMessage("form-error", "Could not update for the following reason: " + resp.responseText);
            }
        });
    }
}

function addPostValues(form, doc) {
	var category = $("#category option:selected").text();
	if (category !== "None") {
		doc.category = category;
	}
	doc.explicit = form[0].explicit.checked;
}

function submitNavbarConf() {
    var form = $('form[name="edit-navbar"]');
    var formTitle = $('#nav-title');
    var loader = $('.loader');
    if (!waiting) {
        hideMessage("form-error");
        waiting = true;
        form.hide();
        formTitle.hide();
        loader.show();

        var projects = form[0].projects.checked;
        var about = form[0].about.checked;

        var navBarConf = {
            projects: projects,
            about: about
        };

        $.ajax({
            type: "PUT",
            url: "/api/navbar",
            data: JSON.stringify(navBarConf),
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
                    hideMessage("form-success");
                    form.show();
                    formTitle.show();
                }, 500);
            },
            error: function (resp) {
                console.log(resp);
                waiting = false;
                loader.hide();
                form.show();
                formTitle.show();
                showMessage("form-error", "Could not update navigation bar for the following reason: " + resp.responseText);
            }
        });
    }
}

function publish(type, add, formName) {
    if (!waiting && confirm("This will publish the item on the blog.")) {
        var publishDate = new Date();
        $("input[type='datetime-local']").val(formatForForm(publishDate));
        update(type, add, formName)
    }
}

function showPreview(formName, fillPreview) {
    var form = $('form[name="'+ formName+ '"]');
    var preview = $('.preview');
    var loader = $('.loader');
    form.hide();
    loader.show();

    fillDocPreview(formName);

    loader.hide();
    preview.show();
}

function fillDocPreview(formName) {
    var form = $('form[name="' + formName + '"]');
    var preview = $('.preview');
    var title = form[0].title.value;
    var previewDoc = $('.preview > .doc');

    var converter = getConverter();
    var contentMarkdown = form[0].contentmarkdown.value;
    var contentHtml = converter.makeHtml(contentMarkdown);

    previewDoc.html("<h1>" + title + "</h1>" + contentHtml);
    $('pre code').each(function (i, block) {
        hljs.highlightBlock(block);
    });
}

function fillAboutMePreview() {
    var form = $('form[name="aboutme-edit"]');
    var preview = $('.preview');
    var previewIntro = $('.preview > #introduction');
    var previewResume = $('.preview > #resume');

    var converter = getConverter();
    var introMarkdown = form[0].intromarkdown.value;
    var introHtml = converter.makeHtml(introMarkdown);

    var resumeMarkdown = form[0].resumemarkdown.value;
    var resumeHtml = converter.makeHtml(resumeMarkdown);

    previewIntro.html(introHtml);
    previewResume.html(resumeHtml);

    if (checkStr(introMarkdown, 1)) {
        previewIntro.show();
    } else {
        previewIntro.hide();
    }

    if (checkStr(resumeMarkdown, 1)) {
        previewResume.show();
    } else {
        previewResume.hide();
    }

    $('pre code').each(function (i, block) {
        hljs.highlightBlock(block);
    });
}

function hidePreview(formName) {
    var form = $('form[name="'+ formName+ '"]');
    var preview = $('.preview');
    preview.hide();
    form.show();
}

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
                resetFields("edit-password", "edit-password-error");
                showMessage("edit-password-success", "Update successful.");
                setTimeout(function () {
                    hideMessage("edit-password-success");
                }, 1000);
            },
            error: function () {
                waiting = false;
                loader.hide();
                form.show();
                showMessage("edit-password-error", "Old password is incorrect.");
            }
        });
    }
}

function resetFields(formName, errorId) {
    hideMessage(errorId);
    var form = $('form[name="' + formName + '"]');
    form.find('input[type=password], input[type=text]').each(function () {
        var inputField = $(this);
        inputField.val("");
        inputField.removeClass("ok");
        inputField.addClass("not-ok");
    });
}

function removeCategory(id) {
	$('#category'+id).remove();
}

function addCategory() {
	var form = $('form[name="edit-categories"]');
	var nextCat = form[0].nextcat;
	var id = nextCat.value;
	var newCat = form[0].newcategory;
	var newCatName = newCat.value;
	if (newCatName !== "") {
		$('#categories').append(
		  '<div class="listitem" id="category'+id+'">'+
		     '<label>'+newCatName+'</label><div class="remove-icon" style="cursor: pointer;" onclick="removeCategory('+id+')">&#10006;</div>'+
		  '</div>'
		);
		newCat.value = '';
		nextCat.value = id + 1;
	}
}

function submitCategories() {
	var form = $('form[name="edit-categories"]');
	var formTitle = $('#cat-title');
    var loader = $('.loader');
    if (!waiting) {
        hideMessage("form-error");
        waiting = true;
        form.hide();
        formTitle.hide();
        loader.show();

    	var nextCat = form[0].nextcat.value;
    	var categories = {titles: []};
    	var order = 0;
    	for (var i = 0; i < nextCat; ++i) {
    		var cat = $('#category'+i);
    		if (cat.length) {
    			categories.titles.push({
    				title: cat.children('label').text(),
    				order: order
    			});
    			order++;
    		}
    	}


        $.ajax({
            type: "PUT",
            url: "/api/categories",
            data: JSON.stringify(categories),
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
                    hideMessage("form-success");
                    form.show();
                    formTitle.show();
                }, 500);
            },
            error: function (resp) {
                console.log(resp);
                waiting = false;
                loader.hide();
                form.show();
                formTitle.show();
                showMessage("form-error", "Could not update categories for the following reason: " + resp.responseText);
            }
        });
    }
}
