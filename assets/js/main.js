/* =========================================================
   Yeamu — логика сайта
   Пиксельные иконки и кнопки, фоны из блоков, плавная прокрутка, статус сервера,
   список модов из manifest.json, звуки, плавные анимации.
   ========================================================= */
(() => {
  "use strict";

  // ===== Настройки =====
  const SERVER_ADDRESS = "yeamu.my.id";
  const MANIFEST_URL = "manifest.json";
  const REPO_DOWNLOADS = "https://github.com/NOTyeamu/Mod-Plugin-Download-for-Server/tree/main/downloads";
  const SPLASHES = [
    "Версия 1.21.11!", "Моды ставятся сами!", "Нужен мод ModSync!", "Заходи: yeamu.my.id!",
    "Без ModSync не пустит!", "Автообновление модов!", "Сначала поставь Fabric!",
    "Сборка проверена SHA-256!", "Чужие моды — кик!", "Честное выживание!", "Скачай мод — и в игру!",
    "Играй с друзьями!"
  ];
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

  // ===== Генератор случайных чисел с зерном =====
  function rng(seed) {
    let a = seed >>> 0;
    return () => {
      a = (a + 0x6D2B79F5) >>> 0;
      let t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }
  const hashStr = (s) => { let h = 2166136261; for (const c of s) h = Math.imul(h ^ c.charCodeAt(0), 16777619); return h >>> 0; };

  // ===== Процедурные текстуры 16×16 =====
  function texture(draw, seed = 1, size = 16) {
    const c = document.createElement("canvas");
    c.width = c.height = size;
    const g = c.getContext("2d");
    draw(g, rng(seed), size);
    return c;
  }
  const noise = (palette) => (g, r) => {
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      g.fillStyle = palette[Math.floor(r() * palette.length)];
      g.fillRect(x, y, 1, 1);
    }
  };
  const P = {
    dirt: ["#8d6545", "#7b5739", "#96704e", "#6c4b31", "#86603f", "#a07a57"],
    grass: ["#7cbd4f", "#6fae45", "#86c95a", "#68a540", "#79b84c"],
    stone: ["#7f7f7f", "#747474", "#8a8a8a", "#6b6b6b", "#959595", "#7f7f7f"],
    obsidian: ["#14101c", "#1d1529", "#2a1f3d", "#0f0b15", "#3b2a57"],
    deepslate: ["#4d4d55", "#3f3f46", "#56565e", "#36363c", "#5e5e66"],
    bedrock: ["#575757", "#333333", "#7a7a7a", "#1f1f1f", "#4a4a4a"],
  };
  const T = {};
  T.dirt = texture(noise(P.dirt), 11);
  T.grass = texture(noise(P.grass), 12);
  T.stone = texture(noise(P.stone), 13);
  T.obsidian = texture(noise(P.obsidian), 14);
  T.deepslate = texture((g, r) => {
    noise(P.deepslate)(g, r);
    g.fillStyle = "rgba(0,0,0,.25)";
    for (let y = 3; y < 16; y += 5) g.fillRect(0, y, 16, 1);
  }, 15);
  T.bedrock = texture(noise(P.bedrock), 16);
  T["grass-side"] = texture((g, r) => {
    noise(P.dirt)(g, r);
    for (let x = 0; x < 16; x++) {
      const d = 3 + Math.floor(r() * 3);
      for (let y = 0; y < d; y++) { g.fillStyle = P.grass[Math.floor(r() * P.grass.length)]; g.fillRect(x, y, 1, 1); }
    }
  }, 17);
  T.planks = texture((g, r) => {
    const pal = ["#a2824e", "#9c7c49", "#b4905a", "#8f7243", "#ab8853"];
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      g.fillStyle = (y % 4 === 3) ? "#6b5434" : pal[Math.floor(r() * pal.length)];
      g.fillRect(x, y, 1, 1);
    }
    g.fillStyle = "#6b5434";
    [[3, 0], [11, 4], [6, 8], [14, 12]].forEach(([x, y]) => g.fillRect(x, y, 1, 3));
  }, 18);
  T.cobble = texture((g, r) => {
    noise(P.stone)(g, r);
    g.fillStyle = "#4f4f4f";
    for (let i = 0; i < 26; i++) g.fillRect(Math.floor(r() * 16), Math.floor(r() * 16), 1 + Math.floor(r() * 2), 1);
  }, 19);
  // Иконки предметов
  T.crafting = texture((g, r) => {
    T.planks && g.drawImage(T.planks, 0, 0);
    g.fillStyle = "#5c4426"; g.fillRect(0, 0, 16, 3);
    g.fillStyle = "#c0c0c0"; g.fillRect(3, 6, 3, 1); g.fillRect(4, 5, 1, 6);
    g.fillStyle = "#8f6a3c"; g.fillRect(10, 5, 3, 7);
  }, 20);
  T.chest = texture((g) => {
    g.fillStyle = "#2b1d10"; g.fillRect(0, 1, 16, 15);
    g.fillStyle = "#a0702f"; g.fillRect(1, 2, 14, 13);
    g.fillStyle = "#7c5422"; g.fillRect(1, 7, 14, 1); g.fillRect(1, 14, 14, 1);
    g.fillStyle = "#d8d8d8"; g.fillRect(7, 6, 2, 3);
    g.fillStyle = "#3a3a3a"; g.fillRect(7, 8, 2, 1);
  }, 21);
  T.diamond = texture((g) => {
    const px = (x, y, c) => { g.fillStyle = c; g.fillRect(x, y, 1, 1); };
    const shape = ["....aaaaaa....", "...abbbbbba...", "..abbccccbba..", ".abbccddccbba.", "abbccddddccbba", ".abbccddccbba.", "..abbccccbba..", "...abbccbba...", "....abbbba....", ".....abba.....", "......aa......"];
    const col = { a: "#0e3f3a", b: "#1fb5a4", c: "#4aedd9", d: "#d5fff6" };
    shape.forEach((row, y) => [...row].forEach((ch, x) => ch !== "." && px(x + 1, y + 2, col[ch])));
  }, 22);
  T.paper = texture((g) => {
    g.fillStyle = "#e6e6d8"; g.fillRect(3, 1, 10, 14);
    g.fillStyle = "#a6a690"; for (let y = 4; y < 14; y += 2) g.fillRect(5, y, 6, 1);
    g.fillStyle = "#7d7d6a"; g.fillRect(3, 1, 10, 1);
  }, 23);

  // Текстуры в CSS-переменные
  const root = document.documentElement.style;
  for (const k of ["dirt", "grass", "stone", "obsidian", "deepslate", "bedrock", "grass-side", "planks", "cobble"]) {
    root.setProperty("--tex-" + k, "url(" + T[k].toDataURL() + ")");
  }
  $$("[data-tex]").forEach((el) => {
    const t = T[el.dataset.tex];
    if (t) { el.style.backgroundImage = "url(" + t.toDataURL() + ")"; el.style.backgroundSize = "cover"; }
  });

  // ===== Пиксельные иконки 16×16 (яркие, с чёрной обводкой) =====
  // Рисуем форму цветными пикселями, обводка добавляется автоматически.
  const ICONS = {
    sword(p, R) {
      for (let k = 0; k <= 7; k++) p(13 - k, 2 + k, k < 2 ? "#ffffff" : "#d6f4ff");
      for (let k = 0; k <= 6; k++) { p(12 - k, 2 + k, "#8fd8ff"); p(13 - k, 3 + k, "#3aa0ff"); }
      [[3, 8], [4, 9], [5, 10], [6, 11], [7, 12]].forEach(([x, y]) => p(x, y, "#ffc21a"));
      [[4, 8], [5, 9], [6, 10], [7, 11]].forEach(([x, y]) => p(x, y, "#ff9d00"));
      p(4, 11, "#c0652a"); p(3, 12, "#c0652a"); p(4, 12, "#8b4a1f"); p(2, 13, "#ffd84a"); p(3, 13, "#ffc21a"); p(2, 14, "#ffc21a");
    },
    box(p, R, hue) {
      const c = hue == null ? null : (l, s = 95) => "hsl(" + hue + "," + s + "%," + l + "%)";
      R(2, 6, 13, 13, c ? c(52) : "#ff9f1a");
      R(11, 6, 13, 13, c ? c(40) : "#e07800");
      R(1, 3, 14, 5, c ? c(64) : "#ffc94d");
      R(1, 3, 14, 3, c ? c(82) : "#ffe9a8");
      R(7, 3, 8, 13, c ? c(50, 20) : "#ff2d55");
      R(7, 3, 7, 13, c ? c(85, 30) : "#ff7a95");
    },
    shield(p, R) {
      const rows = { 1: [2, 13], 2: [2, 13], 3: [2, 13], 4: [2, 13], 5: [2, 13], 6: [2, 13], 7: [2, 13], 8: [2, 13], 9: [3, 12], 10: [3, 12], 11: [4, 11], 12: [5, 10], 13: [6, 9], 14: [7, 8] };
      Object.entries(rows).forEach(([y, [a, b]]) => R(a, +y, b, +y, "#2b6cff"));
      R(3, 2, 12, 2, "#8cc0ff"); R(3, 2, 3, 9, "#8cc0ff");
      R(12, 3, 12, 9, "#1a45c9");
      [[4, 7], [5, 8], [6, 9], [7, 8], [8, 7], [9, 6], [10, 5], [11, 4]].forEach(([x, y]) => { p(x, y, "#ffffff"); p(x, y + 1, "#ffffff"); });
    },
    hammer(p, R) {
      R(2, 2, 13, 5, "#d3dde4"); R(2, 2, 13, 2, "#ffffff"); R(2, 5, 13, 5, "#7f8c96"); R(12, 3, 13, 5, "#9aa7b1");
      R(7, 6, 8, 14, "#e0862d"); R(7, 6, 7, 14, "#ffb85c");
    },
    folder(p, R) {
      R(1, 3, 6, 4, "#ffb300"); R(1, 5, 14, 13, "#ffcc1a"); R(1, 5, 14, 6, "#ffe680"); R(1, 13, 14, 13, "#e09a00");
    },
    play(p, R) {
      for (let y = 2; y <= 13; y++) { const half = Math.min(y - 2, 13 - y); const wd = Math.round(half * 1.65) + 1; R(4, y, 3 + wd, y, "#2ee65a"); }
      R(4, 3, 4, 12, "#b3ffc6");
    },
    trophy(p, R) {
      R(3, 2, 12, 6, "#ffd21f"); R(4, 7, 11, 7, "#ffd21f"); R(5, 8, 10, 8, "#ffc000");
      R(7, 9, 8, 11, "#e0a000"); R(4, 12, 11, 13, "#c98a00"); R(4, 12, 11, 12, "#ffcf40");
      R(4, 3, 4, 6, "#fff6b0"); R(11, 3, 12, 6, "#f0b400");
      p(1, 3, "#ffd21f"); p(1, 4, "#ffd21f"); p(2, 5, "#ffd21f"); p(14, 3, "#ffd21f"); p(14, 4, "#ffd21f"); p(13, 5, "#ffd21f");
    },
    check(p) {
      [[2, 8], [3, 9], [4, 10], [5, 11], [6, 10], [7, 9], [8, 8], [9, 7], [10, 6], [11, 5], [12, 4], [13, 3]].forEach(([x, y]) => { p(x, y, "#3dff6e"); p(x, y + 1, "#16c947"); });
    },
    server(p, R) {
      [1, 6, 11].forEach((y) => {
        R(2, y, 13, y + 3, "#5a6e88"); R(2, y, 13, y, "#9fb4d0"); R(9, y + 2, 12, y + 2, "#2b3646");
        p(4, y + 2, "#3dff5c"); p(6, y + 2, "#ffd21f");
      });
    },
    soundOn(p, R) {
      R(2, 6, 4, 9, "#ffe14d"); R(5, 5, 5, 10, "#ffd000"); R(6, 4, 6, 11, "#ffd000"); R(7, 3, 7, 12, "#ffd000");
      R(9, 6, 9, 9, "#3fe0ff"); R(11, 4, 11, 11, "#3fe0ff"); p(10, 5, "#3fe0ff"); p(10, 10, "#3fe0ff"); R(13, 3, 13, 12, "#3fe0ff"); p(12, 3, "#3fe0ff"); p(12, 12, "#3fe0ff");
    },
    soundOff(p, R) {
      R(2, 6, 4, 9, "#ffe14d"); R(5, 5, 5, 10, "#ffd000"); R(6, 4, 6, 11, "#ffd000"); R(7, 3, 7, 12, "#ffd000");
      for (let k = 0; k <= 4; k++) { p(9 + k, 5 + k, "#ff3b3b"); p(13 - k, 5 + k, "#ff3b3b"); }
    },
    burger(p, R) { R(2, 3, 13, 4, "#ffffff"); R(2, 7, 13, 8, "#ffffff"); R(2, 11, 13, 12, "#ffffff"); },
    close(p) { for (let k = 0; k <= 9; k++) { p(3 + k, 3 + k, "#ffffff"); p(12 - k, 3 + k, "#ffffff"); p(4 + k > 12 ? 12 : 4 + k, 3 + k, "#ffffff"); p(11 - k < 3 ? 3 : 11 - k, 3 + k, "#ffffff"); } },
    download(p, R) {
      R(6, 1, 9, 7, "#3dff6a"); R(6, 1, 6, 7, "#b0ffc4");
      [[8, 3, 12], [9, 4, 11], [10, 5, 10], [11, 6, 9], [12, 7, 8]].forEach(([y, a, b]) => R(a, y, b, y, "#3dff6a"));
      R(2, 14, 13, 14, "#ffffff");
    },
    logo(p) {
      // Стилизованный изометрический кубик: зелёный верх, тёплые бока
      for (let y = 1; y <= 14; y++) for (let x = 1; x <= 14; x++) {
        const dx = Math.abs(x - 7.5), rh = 1 - dx / 6.6;
        if (rh < 0) continue;
        const yTop = 4.6 - 3.3 * rh, yLow = 4.6 + 3.3 * rh;
        if (y + 0.5 >= yTop && y + 0.5 <= yLow) p(x, y, y + 0.5 < 4.6 - 1.8 * rh ? "#b8ff7a" : "#5ee04a");
        else if (y + 0.5 > yLow && y + 0.5 <= yLow + 6.6) {
          const left = x < 7.5, grass = y + 0.5 <= yLow + 1.6;
          p(x, y, grass ? (left ? "#46c23a" : "#2f9a2a") : (left ? "#d08a4e" : "#9c5f30"));
        }
      }
    },
  };
  function pixelIcon(name, hue) {
    const c = document.createElement("canvas");
    c.width = c.height = 16;
    const g = c.getContext("2d");
    const p = (x, y, col) => { g.fillStyle = col; g.fillRect(x, y, 1, 1); };
    const R = (x0, y0, x1, y1, col) => { g.fillStyle = col; g.fillRect(x0, y0, x1 - x0 + 1, y1 - y0 + 1); };
    ICONS[name](p, R, hue);
    // Чёрная обводка вокруг формы
    const img = g.getImageData(0, 0, 16, 16), d = img.data, out = new Uint8ClampedArray(d);
    const solid = (x, y) => x >= 0 && y >= 0 && x < 16 && y < 16 && d[(y * 16 + x) * 4 + 3] > 0;
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      if (solid(x, y)) continue;
      if (solid(x - 1, y) || solid(x + 1, y) || solid(x, y - 1) || solid(x, y + 1)) { const i = (y * 16 + x) * 4; out[i] = 17; out[i + 1] = 17; out[i + 2] = 17; out[i + 3] = 255; }
    }
    g.putImageData(new ImageData(out, 16, 16), 0, 0);
    return c;
  }
  const iconURL = {};
  const iconSrc = (name) => iconURL[name] || (iconURL[name] = pixelIcon(name).toDataURL());
  function paintIcons(scope = document) {
    $$("[data-icon]", scope).forEach((el) => { el.style.backgroundImage = "url(" + iconSrc(el.dataset.icon) + ")"; });
  }
  paintIcons();
  root.setProperty("--icon-server", "url(" + iconSrc("server") + ")");

  // ===== Настоящие кнопки Minecraft: текстура кнопки рисуется попиксельно =====
  // Как в игре: чёрная рамка, светлая фаска сверху и слева, тёмная снизу и справа,
  // шумная серая заливка. Рисуется 9-частной рамкой (border-image) с чёткими пикселями.
  function buttonTex(fill, light, dark, darker, edge, seed) {
    const W = 24, H = 20;
    const c = document.createElement("canvas"); c.width = W; c.height = H;
    const g = c.getContext("2d"), r = rng(seed);
    const p = (x, y, col) => { g.fillStyle = col; g.fillRect(x, y, 1, 1); };
    for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
      let col = fill[Math.floor(r() * fill.length)];
      if (x === 0 || y === 0 || x === W - 1 || y === H - 1) col = edge;
      else if (y === H - 2 || y === H - 3) col = darker;
      else if (x === W - 2) col = dark;
      else if (y === 1 || x === 1) col = light;
      p(x, y, col);
    }
    return "url(" + c.toDataURL() + ")";
  }
  const BTN = {
    "--btn": buttonTex(["#7a7a7a", "#727272", "#808080", "#6c6c6c"], "#b0b0b0", "#575757", "#454545", "#000", 101),
    "--btn-hover": buttonTex(["#8c8c8c", "#848484", "#939393", "#7e7e7e"], "#d6d6d6", "#636363", "#4f4f4f", "#fff", 102),
    "--btn-green": buttonTex(["#3fa52b", "#389c26", "#45ae30", "#348f23"], "#7be35f", "#25701a", "#1b5413", "#000", 103),
    "--btn-green-hover": buttonTex(["#4cbd35", "#45b330", "#53c63b", "#3fa82c"], "#a2ff86", "#2c8420", "#206218", "#fff", 104),
    "--btn-off": buttonTex(["#2c2c2c", "#282828", "#303030"], "#3a3a3a", "#1e1e1e", "#161616", "#000", 105),
  };
  Object.entries(BTN).forEach(([k, v]) => root.setProperty(k, v));

  // ===== Экран загрузки =====
  document.body.classList.add("loading");
  const loader = $("#loader"), bar = $("#loader-bar"), sub = $("#loader-sub");
  const steps = ["Ставим блоки", "Соединяем чанки", "Проверяем сборку", "Прогружаем мир", "Готово!"];
  let prog = 0;
  const loadTimer = setInterval(() => {
    prog = Math.min(prog + 8 + Math.random() * 18, 92);
    bar.style.width = prog + "%";
    sub.textContent = steps[Math.min(steps.length - 2, Math.floor(prog / 25))];
  }, 120);
  function finishLoading() {
    clearInterval(loadTimer);
    bar.style.width = "100%";
    sub.textContent = steps[steps.length - 1];
    setTimeout(() => { loader.classList.add("done"); document.body.classList.remove("loading"); }, reduceMotion ? 0 : 350);
  }
  const started = performance.now();
  window.addEventListener("load", () => setTimeout(finishLoading, Math.max(0, 900 - (performance.now() - started))));
  setTimeout(finishLoading, 4000); // на случай медленных шрифтов

  // ===== Фон из блоков (assets/js/blocks-bg.js) =====
  const world = $("#world");
  const hero = $(".hero-content");
  const mouse = { x: 0, y: 0, tx: 0, ty: 0 };
  let scrollY = 0;
  const panorama = window.YeamuWorld ? window.YeamuWorld.start(world, { reduceMotion, mouse, getScroll: () => scrollY }) : null;
  if (!panorama) $(".hero").classList.add("no-webgl");
  if (!reduceMotion) {
    window.addEventListener("pointermove", (e) => { mouse.tx = e.clientX / innerWidth - 0.5; mouse.ty = e.clientY / innerHeight - 0.5; }, { passive: true });
  }
  // Плавное следование за мышью и лёгкий параллакс заголовка
  (function heroLoop() {
    mouse.x += (mouse.tx - mouse.x) * 0.05;
    mouse.y += (mouse.ty - mouse.y) * 0.05;
    if (hero && scrollY < innerHeight * 1.2) {
      hero.style.transform = "translate3d(" + (mouse.x * -10) + "px," + (scrollY * 0.3 + mouse.y * -8) + "px,0)";
      hero.style.opacity = String(Math.max(0, 1 - scrollY / (innerHeight * 0.8)));
    }
    requestAnimationFrame(heroLoop);
  })();

  // ===== Прокрутка: навигация, активный пункт =====
  const nav = $("#nav");
  window.addEventListener("scroll", () => {
    scrollY = window.scrollY;
    nav.classList.toggle("scrolled", scrollY > 40);
  }, { passive: true });
  const links = $$(".nav-links a");
  const sections = links.map((a) => $(a.getAttribute("href"))).filter(Boolean);
  const navObs = new IntersectionObserver((entries) => {
    entries.forEach((e) => {
      if (e.isIntersecting) links.forEach((a) => a.classList.toggle("active", a.getAttribute("href") === "#" + e.target.id));
    });
  }, { rootMargin: "-45% 0px -50% 0px" });
  sections.forEach((s) => navObs.observe(s));
  const burger = $("#nav-burger"), navLinks = $("#nav-links");
  burger.addEventListener("click", () => navLinks.classList.toggle("open"));
  links.forEach((a) => a.addEventListener("click", () => navLinks.classList.remove("open")));

  // ===== Плавная прокрутка (колёсико мыши с инерцией + плавные переходы по якорям) =====
  const smooth = (() => {
    const maxY = () => Math.max(0, document.documentElement.scrollHeight - innerHeight);
    const coarse = window.matchMedia("(pointer: coarse)").matches;
    let cur = window.scrollY, target = cur, raf = 0, anim = null, selfScroll = false;
    const ease = (x) => (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2);
    function loop(now) {
      raf = 0;
      if (anim) {
        const p = Math.min(1, (now - anim.t0) / anim.dur);
        cur = anim.from + (anim.to - anim.from) * ease(p);
        if (p >= 1) { cur = target = anim.to; anim = null; }
      } else {
        cur += (target - cur) * 0.11;
        if (Math.abs(target - cur) < 0.5) cur = target;
      }
      selfScroll = true;
      window.scrollTo(0, cur);
      if (anim || cur !== target) raf = requestAnimationFrame(loop);
    }
    if (!reduceMotion && !coarse) {
      window.addEventListener("wheel", (e) => {
        if (e.ctrlKey || e.defaultPrevented) return;
        // Внутри прокручиваемых блоков оставляем обычную прокрутку
        for (let el = e.target; el && el !== document.body; el = el.parentElement) {
          if (el.scrollHeight > el.clientHeight + 2 && /(auto|scroll)/.test(getComputedStyle(el).overflowY)) return;
        }
        e.preventDefault();
        if (!raf) cur = window.scrollY;
        anim = null;
        const unit = e.deltaMode === 1 ? 40 : e.deltaMode === 2 ? innerHeight : 1;
        target = Math.max(0, Math.min(maxY(), (raf ? target : cur) + e.deltaY * unit));
        if (!raf) raf = requestAnimationFrame(loop);
      }, { passive: false });
    }
    // Если прокрутили клавиатурой или ползунком — синхронизируемся
    window.addEventListener("scroll", () => {
      if (selfScroll) { selfScroll = false; return; }
      if (!raf) cur = target = window.scrollY;
    }, { passive: true });
    function to(y) {
      y = Math.max(0, Math.min(maxY(), Math.round(y)));
      if (reduceMotion) { window.scrollTo(0, y); cur = target = y; return; }
      const from = window.scrollY, dist = Math.abs(y - from);
      if (dist < 2) return;
      anim = { from, to: y, t0: performance.now(), dur: Math.min(1400, 520 + dist * 0.35) };
      target = y;
      if (!raf) raf = requestAnimationFrame(loop);
    }
    return { to };
  })();
  // Все ссылки внутри страницы — через плавную прокрутку
  document.addEventListener("click", (e) => {
    const a = e.target.closest('a[href^="#"]');
    if (!a || a.classList.contains("nav-logo")) return;
    const id = a.getAttribute("href");
    const el = id.length > 1 && document.querySelector(id);
    if (!el) return;
    e.preventDefault();
    smooth.to(el.getBoundingClientRect().top + window.scrollY - 64);
    history.replaceState(null, "", id);
  });

  // ===== Логотип слева сверху: иконка собирается из пикселей =====
  const logoIcon = pixelIcon("logo");
  $$(".nav-block").forEach((el) => {
    const c = document.createElement("canvas");
    c.width = c.height = 16;
    c.getContext("2d").drawImage(logoIcon, 0, 0);
    el.append(c);
  });
  function pixelReveal(c, dur = 650) {
    const g = c.getContext("2d");
    const src = logoIcon.getContext("2d").getImageData(0, 0, 16, 16);
    const px = [];
    for (let i = 0; i < 256; i++) if (src.data[i * 4 + 3] > 0) px.push(i);
    for (let i = px.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [px[i], px[j]] = [px[j], px[i]]; }
    const t0 = performance.now();
    const step = (now) => {
      const p = Math.min(1, (now - t0) / dur);
      const n = Math.floor(px.length * (1 - Math.pow(1 - p, 2)));
      const img = g.createImageData(16, 16);
      for (let k = 0; k < n; k++) { const i = px[k] * 4; for (let q = 0; q < 4; q++) img.data[i + q] = src.data[i + q]; }
      g.putImageData(img, 0, 0);
      if (p < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }
  const navLogo = $("a.nav-logo");
  navLogo.addEventListener("click", (e) => {
    e.preventDefault();
    if (window.scrollY <= 2) return; // уже наверху — ничего не делаем
    if (!reduceMotion) pixelReveal($("canvas", navLogo));
    smooth.to(0);
    history.replaceState(null, "", location.pathname + location.search);
  });

  // ===== Фоны секций строятся из блоков =====
  if (window.YeamuWorld && window.YeamuWorld.wall) {
    $$(".section").forEach((s) => {
      const theme = ["dirt", "stone", "planks", "deepslate"].find((k) => s.classList.contains("section--" + k));
      if (theme) window.YeamuWorld.wall(s, theme, { reduceMotion });
    });
  }

  // ===== Появление элементов =====
  const revealObs = new IntersectionObserver((entries) => {
    entries.forEach((e) => { if (e.isIntersecting) { e.target.classList.add("in"); revealObs.unobserve(e.target); } });
  }, { threshold: 0.12 });
  $$(".reveal").forEach((el) => {
    const sib = Array.from(el.parentElement.children).filter((c) => c.classList.contains("reveal"));
    el.style.setProperty("--d", (Math.min(sib.indexOf(el), 6) * 0.08) + "s");
    revealObs.observe(el);
  });

  // ===== Сплэш =====
  // Каждую загрузку — новая надпись (не повторяем прошлую)
  let lastSplash = null;
  try { lastSplash = localStorage.getItem("yeamu-splash"); } catch (e) {}
  const splashPool = SPLASHES.filter((x) => x !== lastSplash);
  const splashText = splashPool[Math.floor(Math.random() * splashPool.length)];
  try { localStorage.setItem("yeamu-splash", splashText); } catch (e) {}
  $("#splash").textContent = splashText;
  $("#year").textContent = new Date().getFullYear();

  // ===== Звук кнопок (WebAudio, без файлов) =====
  let audio = null;
  let soundOn = localStorage.getItem("yeamu-sound") === "1";
  const soundBtn = $("#sound-toggle");
  const paintSound = () => { const ic = $(".pix-icon", soundBtn); ic.dataset.icon = soundOn ? "soundOn" : "soundOff"; ic.style.backgroundImage = "url(" + iconSrc(ic.dataset.icon) + ")"; };
  paintSound();
  soundBtn.addEventListener("click", () => { soundOn = !soundOn; localStorage.setItem("yeamu-sound", soundOn ? "1" : "0"); paintSound(); click(); });
  function click() {
    if (!soundOn) return;
    audio = audio || new (window.AudioContext || window.webkitAudioContext)();
    const t = audio.currentTime, o = audio.createOscillator(), g = audio.createGain();
    o.type = "square"; o.frequency.setValueAtTime(820, t); o.frequency.exponentialRampToValueAtTime(380, t + 0.06);
    g.gain.setValueAtTime(0.08, t); g.gain.exponentialRampToValueAtTime(0.0001, t + 0.09);
    o.connect(g).connect(audio.destination); o.start(t); o.stop(t + 0.1);
  }
  document.addEventListener("click", (e) => { if (e.target.closest(".mc-btn, summary, .slot:not(.slot--empty)")) click(); });

  // ===== Тост =====
  const toast = $("#toast");
  let toastT;
  function showToast(text, label = "Готово!") {
    $("#toast-text").textContent = text;
    $(".adv-label", toast).textContent = label;
    toast.classList.add("show");
    clearTimeout(toastT);
    toastT = setTimeout(() => toast.classList.remove("show"), 2600);
  }

  // ===== Адрес сервера =====
  $("#server-address").textContent = SERVER_ADDRESS;
  $$("[data-copy-address]").forEach((b) => b.addEventListener("click", async () => {
    try { await navigator.clipboard.writeText(SERVER_ADDRESS); }
    catch { const ta = document.createElement("textarea"); ta.value = SERVER_ADDRESS; document.body.append(ta); ta.select(); document.execCommand("copy"); ta.remove(); }
    showToast("Адрес " + SERVER_ADDRESS + " скопирован");
  }));

  // ===== Статус сервера =====
  const ping = $("#server-ping");
  ping.classList.add("wait");
  async function loadStatus() {
    try {
      const r = await fetch("https://api.mcsrvstat.us/3/" + encodeURIComponent(SERVER_ADDRESS), { cache: "no-store" });
      const s = await r.json();
      ping.classList.remove("wait", "online", "offline");
      if (s.online) {
        ping.classList.add("online");
        ping.title = "Онлайн";
        $("#server-motd").textContent = (s.motd && s.motd.clean && s.motd.clean.join(" ").trim()) || "Сервер онлайн";
        const pl = $("#server-players");
        pl.textContent = (s.players ? s.players.online : 0) + "/" + (s.players ? s.players.max : 0);
        pl.classList.add("on");
        if (s.version) $("#server-version").textContent = String(s.version).replace(/§./g, "").slice(0, 40);
        if (s.icon) { const ic = $("#server-icon"); ic.style.backgroundImage = "url(" + s.icon + ")"; ic.style.backgroundSize = "cover"; }
      } else {
        ping.classList.add("offline");
        ping.title = "Офлайн";
        $("#server-motd").textContent = "Сервер сейчас офлайн";
      }
    } catch {
      ping.classList.remove("wait");
      $("#server-motd").textContent = "Не удалось узнать статус";
    }
  }
  loadStatus();
  setInterval(loadStatus, 60000);

  // ===== Манифест: моды и ссылки на скачивание =====
  const fmtSize = (b) => b > 1048576 ? (b / 1048576).toFixed(1) + " МБ" : Math.max(1, Math.round(b / 1024)) + " КБ";
  function itemIcon(seed) {
    // Яркая пиксельная коробка, цвет зависит от имени мода
    return pixelIcon("box", Math.floor(rng(seed)() * 360));
  }
  // Пиксельные иконки известных модов (assets/img/mods/<id>.svg). Можно также указать "icon" в манифесте.
  const MOD_ICONS = { "modsync": "assets/img/mods/modsync.svg", "fabric-api": "assets/img/mods/fabric-api.svg" };
  function modIcon(mod) {
    const src = mod.icon || MOD_ICONS[mod.id];
    if (!src) return itemIcon(hashStr(mod.id || mod.name));
    const img = new Image();
    img.alt = ""; img.draggable = false; img.decoding = "async"; img.src = src;
    // Нет файла — показываем сгенерированную коробку
    img.onerror = () => img.replaceWith(itemIcon(hashStr(mod.id || mod.name)));
    return img;
  }
  const tip = $("#tooltip");
  function showTip(html, e) { tip.innerHTML = html; tip.classList.add("show"); moveTip(e); }
  function moveTip(e) {
    const pad = 16, w = tip.offsetWidth, h = tip.offsetHeight;
    let x = e.clientX + pad, y = e.clientY - h - 8;
    if (x + w > innerWidth - 8) x = e.clientX - w - pad;
    if (y < 8) y = e.clientY + pad;
    tip.style.left = x + "px"; tip.style.top = y + "px";
  }
  const hideTip = () => tip.classList.remove("show");
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  async function loadManifest() {
    const slots = $("#slots");
    let m;
    try {
      const r = await fetch(MANIFEST_URL, { cache: "no-store" });
      if (!r.ok) throw new Error(r.status);
      m = await r.json();
    } catch {
      $("#inv-note").textContent = "Список модов ещё не опубликован — загляните позже.";
      const dl = $("#dl-modsync");
      dl.href = REPO_DOWNLOADS; dl.removeAttribute("aria-disabled");
      $("#dl-version").textContent = "—"; $("#dl-size").textContent = "—"; $("#dl-manifest").textContent = "—";
      fillEmpty(slots, 0);
      return;
    }
    const mods = Array.isArray(m.mods) ? m.mods : [];
    const ms = mods.find((x) => x.id === "modsync");
    const fa = mods.find((x) => x.id === "fabric-api");
    const dl = $("#dl-modsync");
    if (ms) {
      dl.href = ms.url; dl.setAttribute("download", ms.file); dl.removeAttribute("aria-disabled");
      $("#dl-version").textContent = ms.version;
      $("#dl-size").textContent = fmtSize(ms.size);
      $("#dl-sha").textContent = "SHA-256: " + ms.sha256;
    } else { dl.href = REPO_DOWNLOADS; dl.removeAttribute("aria-disabled"); }
    if (fa) { $("#dl-fabricapi").href = fa.url; $("#dl-fabricapi").textContent = "Fabric API " + fa.version.split("+")[0]; }
    $("#dl-manifest").textContent = "v" + m.version + " · MC " + (m.minecraft || "?");

    slots.innerHTML = "";
    mods.forEach((mod, i) => {
      const s = document.createElement("div");
      s.className = "slot pop"; s.style.animationDelay = (i * 0.05) + "s";
      s.tabIndex = 0;
      s.append(modIcon(mod));
      const html = '<div class="t-name">' + esc(mod.name) + '</div><div class="t-ver">Версия ' + esc(mod.version) + " · " + fmtSize(mod.size) + "</div>"
        + (mod.description ? '<div class="t-desc">' + esc(mod.description) + "</div>" : "")
        + '<div class="t-mod">' + esc(mod.file) + "</div>";
      s.addEventListener("pointerenter", (e) => showTip(html, e));
      s.addEventListener("pointermove", moveTip);
      s.addEventListener("pointerleave", hideTip);
      s.addEventListener("click", () => { location.href = mod.url; });
      slots.append(s);
    });
    fillEmpty(slots, mods.length);
    $("#inv-note").textContent = "Модов в сборке: " + mods.length + ". ModSync установит их сам.";
  }
  function fillEmpty(slots, n) {
    const total = Math.max(18, Math.ceil(n / 9) * 9);
    for (let i = n; i < total; i++) { const e = document.createElement("div"); e.className = "slot slot--empty"; slots.append(e); }
  }
  loadManifest();

  // ===== Плавное раскрытие FAQ =====
  $$(".faq details").forEach((d) => {
    const body = $(".faq-body", d);
    $("summary", d).addEventListener("click", (e) => {
      if (reduceMotion) return;
      e.preventDefault();
      if (d.open) {
        const h = body.scrollHeight;
        body.animate([{ height: h + "px", opacity: 1 }, { height: "0px", opacity: 0 }], { duration: 260, easing: "ease-in" })
          .onfinish = () => { d.open = false; };
      } else {
        d.open = true;
        const h = body.scrollHeight;
        body.animate([{ height: "0px", opacity: 0 }, { height: h + "px", opacity: 1 }], { duration: 320, easing: "cubic-bezier(.22,1,.36,1)" });
      }
    });
  });

  // ===== Баннер причины (?reason=no_mod|outdated|foreign) =====
  const REASONS = {
    no_mod: ["Нужен мод ModSync", "Сервер не получил ответ от мода. Скачайте ModSync ниже и положите в папку mods."],
    outdated: ["Мод или сборка устарели", "Перезапустите игру или нажмите «Проверить обновления» в главном меню."],
    foreign: ["Найдены чужие моды", "Уберите из папки mods моды, которых нет в сборке сервера."],
  };
  const reason = new URLSearchParams(location.search).get("reason");
  if (reason && REASONS[reason]) {
    $("#reason-title").textContent = REASONS[reason][0];
    $("#reason-text").textContent = REASONS[reason][1];
    $("#reason-banner").hidden = false;
    $("#reason-close").addEventListener("click", () => { $("#reason-banner").hidden = true; });
  }
})();
