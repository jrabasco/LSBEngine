function showError(errDivId, message) {
    var errDiv = $("#"+errDivId);
    errDiv.empty();
    errDiv.text(message);
    errDiv.show();
}

function hideError(errDivId) {
    $("#"+errDivId).hide();
}
/*$(function () {
    $(".myBox").click(function() {
        window.location = $(this).find("a").attr("href");
        return false;
    });
});
*/
