var img = document.getElementById("frame");
function load() {
    // Dummy processed frame
    fetch("sample_frame.png")
        .then(function (r) { return r.blob(); })
        .then(function (b) { return img.src = URL.createObjectURL(b); });
}
load();
