// src/main.ts
const canvas = document.getElementById('canvas') as HTMLCanvasElement;
const ctx = canvas.getContext('2d')!;
const btnSave = document.getElementById('btnSave') as HTMLButtonElement;
const chkOverlay = document.getElementById('chkOverlay') as HTMLInputElement;
const fallback = document.getElementById('fallback') as HTMLDivElement;

let lastTimestamp = performance.now();
let fps = 0;
let frameCount = 0;
let lastFpsCalc = performance.now();

type FrameInfo = {
  width: number;
  height: number;
  timestamp: number;
};

let currentFrame: HTMLImageElement | null = null;
let frameInfo: FrameInfo | null = null;

// Helper to draw overlay
function drawOverlay() {
  if (!chkOverlay.checked) return;

  const infoLines: string[] = [];
  infoLines.push(`FPS: ${fps.toFixed(1)}`);
  if (frameInfo) {
    infoLines.push(`RES: ${frameInfo.width}×${frameInfo.height}`);
    const t = new Date(frameInfo.timestamp);
    infoLines.push(`TS: ${t.toLocaleTimeString()}`);
  }
  const padding = 8;
  const lineHeight = 18;
  ctx.save();
  ctx.font = '14px monospace';
  ctx.fillStyle = 'rgba(0,0,0,0.45)';
  const w = Math.max(...infoLines.map(s => ctx.measureText(s).width)) + padding * 2;
  const h = infoLines.length * lineHeight + padding * 2;
  ctx.fillRect(12, 12, w, h);
  ctx.fillStyle = '#e6eef6';
  infoLines.forEach((line, i) => {
    ctx.fillText(line, 12 + padding, 12 + padding + (i + 1) * lineHeight - 6);
  });
  ctx.restore();
}

// Try to load sample image from same folder; if not present, generate a placeholder
async function loadSample(): Promise<HTMLImageElement> {
  const img = new Image();
  img.crossOrigin = 'anonymous';

  const tryUrl = 'sample-frame.jpg';
  const loaded = await new Promise<HTMLImageElement | null>((resolve) => {
    let ok = false;
    img.onload = () => { ok = true; resolve(img); };
    img.onerror = () => resolve(null);
    img.src = tryUrl;
  });

  if (loaded) {
    fallback.style.display = 'none';
    return loaded;
  }

  // fallback: generate synthetic processed-looking frame using an offscreen canvas
  const off = document.createElement('canvas');
  off.width = 640;
  off.height = 480;
  const octx = off.getContext('2d')!;
  // draw gradient background
  const grad = octx.createLinearGradient(0, 0, off.width, off.height);
  grad.addColorStop(0, '#001a2e');
  grad.addColorStop(1, '#00364d');
  octx.fillStyle = grad;
  octx.fillRect(0, 0, off.width, off.height);

  // draw synthetic edges (white lines)
  octx.strokeStyle = 'rgba(255,255,255,0.85)';
  octx.lineWidth = 2;
  for (let i = 0; i < 30; i++) {
    octx.beginPath();
    octx.moveTo(Math.random() * off.width, Math.random() * off.height);
    octx.lineTo(Math.random() * off.width, Math.random() * off.height);
    octx.stroke();
  }

  // overlay a processed filter look
  octx.globalCompositeOperation = 'overlay';
  octx.fillStyle = 'rgba(0, 120, 180, 0.06)';
  octx.fillRect(0, 0, off.width, off.height);

  const dataUrl = off.toDataURL('image/png');
  const fallbackImg = new Image();
  fallbackImg.src = dataUrl;
  await new Promise<void>((res) => { fallbackImg.onload = () => res(); });
  fallback.textContent = 'Using generated sample frame (place sample-frame.jpg in this folder to replace it)';
  return fallbackImg;
}

// Draw loop: single static frame, but we keep FPS and overlay updated
function renderLoop(now: number) {
  // FPS calc
  frameCount++;
  if (now - lastFpsCalc >= 500) {
      fps = (frameCount * 1000) / (now - lastFpsCalc);
      frameCount = 0;
      lastFpsCalc = now;
  }


  if (currentFrame) {
    // Fit image to canvas while preserving aspect
    const cw = canvas.width;
    const ch = canvas.height;
    const iw = currentFrame.naturalWidth;
    const ih = currentFrame.naturalHeight;
    const scale = Math.min(cw / iw, ch / ih);
    const w = iw * scale;
    const h = ih * scale;
    const x = (cw - w) / 2;
    const y = (ch - h) / 2;
    ctx.clearRect(0, 0, cw, ch);
    ctx.drawImage(currentFrame, x, y, w, h);
    drawOverlay();
  } else {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#122233';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#9aa5b1';
    ctx.font = '16px sans-serif';
    ctx.fillText('No frame loaded', 20, 40);
  }

  requestAnimationFrame(renderLoop);
}

// Save current canvas to file
btnSave.addEventListener('click', () => {
  const url = canvas.toDataURL('image/png');
  const a = document.createElement('a');
  a.href = url;
  a.download = `frame-${Date.now()}.png`;
  document.body.appendChild(a);
  a.click();
  a.remove();
});

// Toggle overlay quickly updates the canvas immediately
chkOverlay.addEventListener('change', () => {
  // immediate redraw
  if (currentFrame) {
    ctx.drawImage(currentFrame, 0, 0, canvas.width, canvas.height);
    drawOverlay();
  }
});

async function bootstrap() {
  // set canvas resolution (CSS and pixel ratio aware)
  const DPR = Math.max(1, Math.min(2, window.devicePixelRatio || 1));
  canvas.width = 640;
  canvas.height = 480;
  canvas.style.maxWidth = '100%';

  // load sample image
  currentFrame = await loadSample();
  frameInfo = { width: currentFrame.naturalWidth, height: currentFrame.naturalHeight, timestamp: Date.now() };

  // initial draw
  ctx.fillStyle = '#001a2e';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(currentFrame, 0, 0, canvas.width, canvas.height);
  drawOverlay();

  // start loop
  lastFpsCalc = performance.now();
  requestAnimationFrame(renderLoop);
}

bootstrap().catch(err => {
  console.error(err);
  fallback.textContent = 'Failed to load sample frame: ' + err;
});
