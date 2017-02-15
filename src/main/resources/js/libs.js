function showError(errDivId, message) {
    var errDiv = $("#"+errDivId);
    errDiv.empty();
    errDiv.text(message);
    errDiv.show();
}

function hideError(errDivId) {
    $("#"+errDivId).hide();
}
