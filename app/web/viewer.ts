const img = document.getElementById("frame") as HTMLImageElement;

function load() {
    // Dummy processed frame
    fetch("sample_frame.png")
        .then(r => r.blob())
        .then(b => img.src = URL.createObjectURL(b));
}

load();
