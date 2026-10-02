package com.easycode.ide.data.repository

import com.easycode.ide.data.model.Boilerplate
import com.easycode.ide.data.model.ExternalResource

object BoilerplateRepository {

    val boilerplates: List<Boilerplate> = listOf(
        Boilerplate(
            id = "tailwind_checkboxes",
            title = "Tailwind Checkboxes",
            tag = "Tailwind",
            description = "Custom accessible checkboxes styled with Tailwind CSS utility classes.",
            htmlCode = """<div class="min-h-screen bg-slate-900 flex items-center justify-center p-6">
  <div class="bg-slate-800 border border-slate-700 rounded-2xl p-6 max-w-sm w-full shadow-2xl">
    <h2 class="text-xl font-bold text-white mb-4">Project Tasks</h2>
    <div class="space-y-3">
      <label class="flex items-center space-x-3 cursor-pointer select-none">
        <input type="checkbox" checked class="w-5 h-5 rounded border-slate-600 text-amber-500 focus:ring-amber-400 bg-slate-700" />
        <span class="text-slate-300 text-sm font-medium">Design Responsive UI</span>
      </label>
      <label class="flex items-center space-x-3 cursor-pointer select-none">
        <input type="checkbox" checked class="w-5 h-5 rounded border-slate-600 text-amber-500 focus:ring-amber-400 bg-slate-700" />
        <span class="text-slate-300 text-sm font-medium">Integrate Tailwind CDN</span>
      </label>
      <label class="flex items-center space-x-3 cursor-pointer select-none">
        <input type="checkbox" class="w-5 h-5 rounded border-slate-600 text-amber-500 focus:ring-amber-400 bg-slate-700" />
        <span class="text-slate-300 text-sm font-medium">Add Interactive Animations</span>
      </label>
    </div>
    <button id="btn_status" class="mt-6 w-full py-2.5 px-4 bg-amber-500 hover:bg-amber-600 text-slate-900 font-semibold rounded-xl transition duration-200 shadow-md">
      Check Progress
    </button>
  </div>
</div>""",
            cssCode = """/* Tailwind classes handle styling. Custom overrides can be placed here */
body {
  margin: 0;
  font-family: system-ui, -apple-system, sans-serif;
}""",
            jsCode = """const btn = document.getElementById('btn_status');
const checkboxes = document.querySelectorAll('input[type="checkbox"]');

btn.addEventListener('click', () => {
  const completed = Array.from(checkboxes).filter(c => c.checked).length;
  console.log('Completed tasks: ' + completed + ' of ' + checkboxes.length);
  alert('Completed ' + completed + ' out of ' + checkboxes.length + ' tasks.');
});

console.info('Tailwind Checkboxes boilerplate loaded successfully.');""",
            resources = listOf(
                ExternalResource(
                    name = "Tailwind CSS",
                    url = "https://cdn.tailwindcss.com",
                    isCss = false
                )
            )
        ),

        Boilerplate(
            id = "js_module",
            title = "Import a JS Module",
            tag = "ES6",
            description = "Modern ES6 JavaScript counter with dynamic DOM rendering and state management.",
            htmlCode = """<div class="container">
  <h1>ES6 Module Counter</h1>
  <p class="subtitle">Vanilla JavaScript with modular architecture</p>
  <div class="counter-card">
    <div id="counter-val" class="display">0</div>
    <div class="btn-group">
      <button id="decrement">-</button>
      <button id="reset">Reset</button>
      <button id="increment">+</button>
    </div>
  </div>
</div>""",
            cssCode = """body {
  margin: 0;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #0f172a;
  color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.container {
  text-align: center;
  padding: 24px;
}

h1 {
  font-size: 24px;
  margin-bottom: 4px;
  color: #f59e0b;
}

.subtitle {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 24px;
}

.counter-card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.3);
}

.display {
  font-size: 64px;
  font-weight: 800;
  color: #38bdf8;
  margin-bottom: 24px;
}

.btn-group {
  display: flex;
  gap: 12px;
  justify-content: center;
}

button {
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  background: #334155;
  color: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
}

button:active {
  transform: scale(0.95);
}

#increment {
  background: #f59e0b;
  color: #0f172a;
}""",
            jsCode = """class Counter {
  constructor(initial = 0) {
    this.value = initial;
  }
  inc() { return ++this.value; }
  dec() { return --this.value; }
  reset() { this.value = 0; return this.value; }
}

const counter = new Counter(0);
const display = document.getElementById('counter-val');

document.getElementById('increment').addEventListener('click', () => {
  display.innerText = counter.inc();
  console.log('Counter increased to: ' + counter.value);
});

document.getElementById('decrement').addEventListener('click', () => {
  display.innerText = counter.dec();
  console.log('Counter decreased to: ' + counter.value);
});

document.getElementById('reset').addEventListener('click', () => {
  display.innerText = counter.reset();
  console.info('Counter reset to 0');
});

console.log('ES6 Counter initialized successfully');"""
        ),

        Boilerplate(
            id = "jquery",
            title = "jQuery Playground",
            tag = "jQuery",
            description = "Interactive DOM animations and AJAX handling with jQuery 3.7.",
            htmlCode = """<div class="wrapper">
  <h2>jQuery Interactive Panel</h2>
  <div class="box" id="animated-box">Click Me to Toggle Animation</div>
  <button id="toggle-btn" class="btn">Fade Out / In</button>
  <button id="slide-btn" class="btn">Slide Toggle</button>
</div>""",
            cssCode = """body {
  background: #18181b;
  color: #f4f4f5;
  font-family: sans-serif;
  display: grid;
  place-items: center;
  min-height: 100vh;
  margin: 0;
}

.wrapper {
  text-align: center;
  max-width: 400px;
  padding: 24px;
}

.box {
  background: #f59e0b;
  color: #18181b;
  padding: 32px;
  border-radius: 12px;
  font-weight: bold;
  cursor: pointer;
  margin: 20px 0;
  user-select: none;
}

.btn {
  margin: 6px;
  padding: 10px 18px;
  background: #27272a;
  border: 1px solid #3f3f46;
  color: #fafafa;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}""",
            jsCode = """$(document).ready(function() {
  console.log('jQuery ' + $.fn.jquery + ' ready!');

  $('#animated-box').on('click', function() {
    $(this).animate({
      opacity: 0.7,
      fontSize: '1.2em'
    }, 300).animate({
      opacity: 1.0,
      fontSize: '1em'
    }, 300);
    console.log('Box clicked and animated');
  });

  $('#toggle-btn').on('click', function() {
    $('#animated-box').fadeToggle(400);
    console.log('Toggled fade animation');
  });

  $('#slide-btn').on('click', function() {
    $('#animated-box').slideToggle(400);
    console.log('Toggled slide animation');
  });
});""",
            resources = listOf(
                ExternalResource(
                    name = "jQuery 3.7",
                    url = "https://code.jquery.com/jquery-3.7.1.min.js",
                    isCss = false
                )
            )
        ),

        Boilerplate(
            id = "react",
            title = "React + JSX",
            tag = "React",
            description = "React 18 components rendered with Babel standalone CDN.",
            htmlCode = """<div id="root"></div>""",
            cssCode = """body {
  margin: 0;
  background-color: #0b0f19;
  color: #e2e8f0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}

.card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 16px;
  padding: 28px;
  width: 320px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
}

.tag {
  background: #0284c7;
  color: white;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  text-transform: uppercase;
  font-weight: 700;
}

.count {
  font-size: 54px;
  font-weight: 800;
  color: #38bdf8;
  margin: 16px 0;
}

.btn-react {
  background: #38bdf8;
  color: #0f172a;
  border: none;
  padding: 10px 20px;
  font-weight: 700;
  border-radius: 8px;
  cursor: pointer;
}""",
            jsCode = """const { useState } = React;

function App() {
  const [count, setCount] = useState(0);

  return (
    <div className="card">
      <span className="tag">React 18</span>
      <h2>Interactive State</h2>
      <div className="count">{count}</div>
      <button className="btn-react" onClick={() => {
        setCount(c => c + 1);
        console.log('React state updated: ' + (count + 1));
      }}>
        Increment
      </button>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
console.info('React component mounted successfully.');""",
            resources = listOf(
                ExternalResource(
                    name = "React 18",
                    url = "https://unpkg.com/react@18/umd/react.production.min.js",
                    isCss = false
                ),
                ExternalResource(
                    name = "ReactDOM 18",
                    url = "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js",
                    isCss = false
                ),
                ExternalResource(
                    name = "Babel Standalone",
                    url = "https://unpkg.com/@babel/standalone/babel.min.js",
                    isCss = false
                )
            )
        ),

        Boilerplate(
            id = "css_grid",
            title = "CSS Grid Gallery",
            tag = "CSS Grid",
            description = "Modern responsive auto-fit CSS Grid layout showcase with hover states.",
            htmlCode = """<div class="gallery-container">
  <h1>CSS Grid Showcase</h1>
  <div class="grid">
    <div class="grid-item card-1"><h3>01</h3><p>Flexibility</p></div>
    <div class="grid-item card-2"><h3>02</h3><p>Responsive</p></div>
    <div class="grid-item card-3"><h3>03</h3><p>Auto-Fit</p></div>
    <div class="grid-item card-4"><h3>04</h3><p>Modern CSS</p></div>
    <div class="grid-item card-5"><h3>05</h3><p>MinMax Grid</p></div>
    <div class="grid-item card-6"><h3>06</h3><p>EasyCode</p></div>
  </div>
</div>""",
            cssCode = """body {
  margin: 0;
  padding: 24px;
  background: #111827;
  color: #f9fafb;
  font-family: system-ui, sans-serif;
}

.gallery-container {
  max-width: 800px;
  margin: 0 auto;
}

h1 {
  color: #f59e0b;
  text-align: center;
  margin-bottom: 24px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.grid-item {
  background: #1f2937;
  border: 1px solid #374151;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: transform 0.2s, border-color 0.2s;
  cursor: pointer;
}

.grid-item:hover {
  transform: translateY(-4px);
  border-color: #f59e0b;
}

.grid-item h3 {
  margin: 0;
  font-size: 28px;
  color: #38bdf8;
}

.grid-item p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #9ca3af;
}""",
            jsCode = """document.querySelectorAll('.grid-item').forEach(card => {
  card.addEventListener('click', () => {
    const title = card.querySelector('h3').innerText;
    console.log('Selected Card: ' + title);
  });
});

console.log('CSS Grid gallery rendered with ' + document.querySelectorAll('.grid-item').length + ' items.');"""
        ),

        Boilerplate(
            id = "canvas",
            title = "HTML5 Canvas Demo",
            tag = "Canvas",
            description = "Interactive particle animation system using HTML5 2D Canvas context.",
            htmlCode = """<canvas id="canvas"></canvas>
<div class="overlay">Tap or move cursor to generate particles</div>""",
            cssCode = """body, html {
  margin: 0;
  padding: 0;
  overflow: hidden;
  background: #09090b;
  font-family: sans-serif;
}

#canvas {
  position: absolute;
  width: 100vw;
  height: 100vh;
  display: block;
}

.overlay {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(24, 24, 27, 0.85);
  color: #a1a1aa;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 12px;
  border: 1px solid #3f3f46;
  pointer-events: none;
}""",
            jsCode = """const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');

let w = canvas.width = window.innerWidth;
let h = canvas.height = window.innerHeight;

window.addEventListener('resize', () => {
  w = canvas.width = window.innerWidth;
  h = canvas.height = window.innerHeight;
});

const particles = [];
class Particle {
  constructor(x, y) {
    this.x = x;
    this.y = y;
    this.size = Math.random() * 5 + 2;
    this.speedX = Math.random() * 4 - 2;
    this.speedY = Math.random() * 4 - 2;
    this.color = 'hsl(' + (Math.random() * 60 + 30) + ', 100%, 55%)';
    this.life = 1;
  }
  update() {
    this.x += this.speedX;
    this.y += this.speedY;
    this.life -= 0.02;
  }
  draw() {
    ctx.fillStyle = this.color;
    ctx.globalAlpha = Math.max(0, this.life);
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
    ctx.fill();
  }
}

function addParticles(x, y) {
  for (let i = 0; i < 5; i++) {
    particles.push(new Particle(x, y));
  }
}

window.addEventListener('pointermove', (e) => addParticles(e.clientX, e.clientY));
window.addEventListener('pointerdown', (e) => addParticles(e.clientX, e.clientY));

function animate() {
  ctx.globalAlpha = 0.2;
  ctx.fillStyle = '#09090b';
  ctx.fillRect(0, 0, w, h);

  for (let i = particles.length - 1; i >= 0; i--) {
    particles[i].update();
    particles[i].draw();
    if (particles[i].life <= 0) particles.splice(i, 1);
  }

  requestAnimationFrame(animate);
}

// Initial burst
for (let i = 0; i < 20; i++) addParticles(w/2, h/2);
animate();
console.log('Canvas particle loop started');"""
        )
    )

    fun getById(id: String): Boilerplate? = boilerplates.firstOrNull { it.id == id }
}
