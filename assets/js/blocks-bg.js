/*
 * Фоны сайта, которые «строятся» из блоков.
 *  - start(canvas, opts): главный экран. Изометрический ландшафт: блоки падают волной
 *    от центра и складываются в цельный рельеф (без щелей, с мягкими тенями у обрывов).
 *  - wall(section, theme): фон секции. Стена из блоков, которая проявляется
 *    попиксельно по диагонали, когда секция появляется на экране.
 * Всё рисуется на canvas 2D, без внешних картинок и библиотек.
 */
(function () {
  "use strict";

  // ---------- Случайные числа и шум ----------
  function rng(seed) {
    let s = (seed >>> 0) || 1;
    return function () {
      s ^= s << 13; s >>>= 0;
      s ^= s >>> 17;
      s ^= s << 5; s >>>= 0;
      return s / 4294967296;
    };
  }
  function hash2(x, y, seed) {
    let h = Math.imul(x, 374761393) + Math.imul(y, 668265263) + Math.imul(seed, 982451653);
    h = Math.imul(h ^ (h >>> 13), 1274126177);
    return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
  }
  function valueNoise(x, y, seed) {
    const xi = Math.floor(x), yi = Math.floor(y);
    const xf = x - xi, yf = y - yi;
    const u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf);
    const a = hash2(xi, yi, seed), b = hash2(xi + 1, yi, seed);
    const c = hash2(xi, yi + 1, seed), d = hash2(xi + 1, yi + 1, seed);
    return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
  }
  function fbm(x, y, seed) {
    let sum = 0, amp = 1, norm = 0, f = 1;
    for (let o = 0; o < 3; o++) { sum += valueNoise(x * f, y * f, seed + o * 17) * amp; norm += amp; amp *= 0.5; f *= 2; }
    return sum / norm;
  }
  const pick = (arr, r) => arr[Math.floor(r() * arr.length)];

  // ---------- Текстуры 16×16 ----------
  function tex(seed, draw) {
    const c = document.createElement("canvas");
    c.width = c.height = 16;
    const g = c.getContext("2d");
    const r = rng(seed);
    const px = (x, y, col) => { g.fillStyle = col; g.fillRect(x, y, 1, 1); };
    draw(px, r, g);
    return c;
  }
  const noise = (pal) => (px, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) px(x, y, pick(pal, r)); };
  // Камни, выложенные «ячейками» (булыжник)
  const cells = (pal, mortar) => (px, r) => {
    const pts = []; for (let i = 0; i < 9; i++) pts.push([r() * 16, r() * 16, pick(pal, r)]);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      let d1 = 1e9, d2 = 1e9, col = pal[0];
      for (const p of pts) for (let ox = -16; ox <= 16; ox += 16) for (let oy = -16; oy <= 16; oy += 16) {
        const d = Math.hypot(x + 0.5 - p[0] - ox, y + 0.5 - p[1] - oy);
        if (d < d1) { d2 = d1; d1 = d; col = p[2]; } else if (d < d2) d2 = d;
      }
      px(x, y, d2 - d1 < 1.1 ? mortar : (r() < 0.15 ? pick(pal, r) : col));
    }
  };
  // Полосы (камень, глубинный сланец)
  const streaky = (pal, dark, light) => (px, r) => {
    noise(pal)(px, r);
    for (let i = 0; i < 7; i++) { const y = Math.floor(r() * 16), x = Math.floor(r() * 16), l = 2 + Math.floor(r() * 4), c = r() < 0.6 ? dark : light; for (let k = 0; k < l; k++) px((x + k) % 16, y, c); }
  };
  // Руда: основа + яркие вкрапления с тёмным краем
  const ore = (base, cols, n) => (px, r, g) => {
    base(px, r, g);
    for (let i = 0; i < n; i++) {
      const cx = 2 + Math.floor(r() * 11), cy = 2 + Math.floor(r() * 11);
      px(cx, cy, cols[0]); px(cx + 1, cy, cols[1]); px(cx, cy + 1, cols[1]); px(cx + 1, cy + 1, cols[2]);
      if (r() < 0.5) px(cx + 2, cy + 1, cols[2]);
    }
  };
  // Доски: 4 доски по 4 пикселя, тёмный шов снизу и вертикальные стыки
  const planks = (pal, line) => (px, r) => {
    for (let b = 0; b < 4; b++) {
      const seam = Math.floor(r() * 16), base = pick(pal, r);
      for (let y = b * 4; y < b * 4 + 4; y++) for (let x = 0; x < 16; x++) {
        let c = r() < 0.25 ? pick(pal, r) : base;
        if (y === b * 4 + 3 || x === seam) c = line;
        px(x, y, c);
      }
    }
  };

  const PAL = {
    grass: ["#7cbd4f", "#6fae45", "#86c95a", "#68a540", "#79b84c", "#8fd162"],
    dirt: ["#8d6545", "#7b5739", "#96704e", "#6c4b31", "#86603f", "#a07a57"],
    stone: ["#8a8a8a", "#949494", "#7f7f7f", "#9e9e9e"],
    sand: ["#e3d59b", "#dccd8f", "#eadfa8", "#d4c487", "#efe2ae"],
  };

  let TEX = null;
  function textures() {
    if (TEX) return TEX;
    const T = {};
    const stoneFn = streaky(PAL.stone, "#747474", "#a6a6a6");
    const deepFn = streaky(["#4f4f54", "#56565c", "#47474c", "#5e5e64"], "#38383c", "#6a6a70");
    T.grassTop = tex(11, noise(PAL.grass));
    T.dirt = tex(12, noise(PAL.dirt));
    const edge = []; { const r = rng(13); for (let x = 0; x < 16; x++) edge.push(3 + Math.floor(r() * 3)); }
    T.grassSide = tex(14, (px, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) px(x, y, y < edge[x] ? pick(PAL.grass, r) : pick(PAL.dirt, r)); });
    T.stone = tex(15, stoneFn);
    T.sand = tex(16, noise(PAL.sand));
    T.coarse = tex(31, (px, r) => { for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) { const q = r(); px(x, y, q < 0.18 ? "#5a3f2a" : q < 0.26 ? "#b08560" : pick(PAL.dirt, r)); } });
    T.gravel = tex(32, cells(["#857f7c", "#9b9592", "#6d6866", "#a8a19d", "#7d6a5a"], "#5b5553"));
    T.dirt2 = tex(33, noise(PAL.dirt));
    T.stone2 = tex(34, stoneFn);
    T.andesite = tex(35, noise(["#8b8b8b", "#9c9c9c", "#7d7d7d", "#adadad", "#888888"]));
    T.cobble = tex(36, cells(["#7a7a7a", "#8e8e8e", "#6a6a6a", "#a2a2a2"], "#4a4a4a"));
    T.coal = tex(37, ore(stoneFn, ["#3a3a3a", "#1e1e1e", "#111111"], 5));
    T.iron = tex(38, ore(stoneFn, ["#f2d2b6", "#d8a07a", "#a8704c"], 4));
    T.diamondOre = tex(39, ore(stoneFn, ["#b6fff8", "#4aedd9", "#1a9c90"], 4));
    T.gold = tex(40, ore(stoneFn, ["#fff58a", "#fcd93a", "#c08a18"], 4));
    T.oak = tex(41, planks(["#b8945f", "#a98754", "#c29d62", "#b08b57"], "#7d6237"));
    T.oak2 = tex(42, planks(["#b8945f", "#a98754", "#c29d62", "#b08b57"], "#7d6237"));
    T.oak3 = tex(43, planks(["#b8945f", "#a98754", "#c29d62", "#b08b57"], "#7d6237"));
    T.spruce = tex(44, planks(["#735432", "#684c2c", "#7e5c38"], "#4d361d"));
    T.deep = tex(45, deepFn);
    T.deep2 = tex(46, deepFn);
    T.cobDeep = tex(47, cells(["#3e3e42", "#4f4f54", "#5b5b60"], "#2a2a2d"));
    T.tuff = tex(48, noise(["#6c6d66", "#5f605a", "#787972", "#63645d", "#4d4e48"]));
    T.deepDiamond = tex(49, ore(deepFn, ["#b6fff8", "#4aedd9", "#1a9c90"], 4));
    T.amethyst = tex(50, noise(["#8d6acc", "#a07be0", "#7a58b8", "#c7a6ff", "#6b4ba3"]));
    TEX = T;
    return T;
  }

  // =====================================================================
  //  Главный экран: изометрический ландшафт
  // =====================================================================
  function start(canvas, opts) {
    opts = opts || {};
    const ctx = canvas.getContext("2d");
    if (!ctx) return null;
    const reduceMotion = !!opts.reduceMotion;
    const T = textures();
    const TYPES = {
      grass: { top: T.grassTop, side: T.grassSide },
      dirt: { top: T.dirt, side: T.dirt },
      stone: { top: T.stone, side: T.stone },
      sand: { top: T.sand, side: T.sand },
    };

    // Все размеры в физических пикселях экрана и только целые — так блоки стыкуются без щелей
    let dpr = 1, vw = 0, vh = 0, W = 64, w = 32, t = 16, hgt = 36;
    let G = 40, cols = [], grid = [], order = [], sprites = {};
    let ox = 0, oy = 0, startTime = 0, built = false;

    // Один куб. Грани чуть заходят друг на друга (на 1px), чтобы не было светлых швов.
    function renderCube(type) {
      const c = document.createElement("canvas");
      c.width = 2 * w + 2; c.height = 2 * t + hgt + 2;
      const g = c.getContext("2d");
      g.imageSmoothingEnabled = false;
      g.translate(1, 1);
      const face = (pts, m, img, shade) => {
        g.save();
        g.beginPath(); g.moveTo(pts[0][0], pts[0][1]);
        for (let i = 1; i < pts.length; i++) g.lineTo(pts[i][0], pts[i][1]);
        g.closePath(); g.clip();
        g.transform(m[0], m[1], m[2], m[3], m[4], m[5]);
        g.drawImage(img, -0.5, -0.5, 17, 17);
        g.restore();
        if (shade) {
          g.save(); g.beginPath(); g.moveTo(pts[0][0], pts[0][1]);
          for (let i = 1; i < pts.length; i++) g.lineTo(pts[i][0], pts[i][1]);
          g.closePath(); g.fillStyle = shade; g.fill(); g.restore();
        }
      };
      face([[-1, t], [w + 1, 2 * t + 1], [w + 1, 2 * t + hgt + 1], [-1, t + hgt + 1]], [w / 16, t / 16, 0, hgt / 16, 0, t], type.side, "rgba(0,0,0,.2)");
      face([[w, 2 * t], [2 * w + 1, t], [2 * w + 1, t + hgt + 1], [w, 2 * t + hgt + 1]], [w / 16, -t / 16, 0, hgt / 16, w, 2 * t], type.side, "rgba(0,0,0,.4)");
      face([[w, -1], [2 * w + 1, t], [w, 2 * t + 1], [-1, t]], [w / 16, -t / 16, w / 16, t / 16, 0, t], type.top, null);
      return c;
    }

    function layerType(col, L) {
      if (L === col.h - 1) return col.top;
      if (col.top === "sand") return "sand";
      if (L >= col.h - 3) return "dirt";
      return "stone";
    }
    const at = (i, j) => (i < 0 || j < 0 || i >= G || j >= G ? null : grid[i * G + j]);

    function generate() {
      cols = []; grid = [];
      const r = rng(20261109);
      const c0 = (G - 1) / 2;
      const H = [];
      for (let i = 0; i < G; i++) for (let j = 0; j < G; j++) {
        const n = fbm(i * 0.075 + 3.1, j * 0.075 + 7.7, 7);
        H.push(Math.max(1, Math.min(5, 1 + Math.floor((n - 0.18) * 6.5))));
      }
      // Сглаживание: убираем одиночные «пеньки» и ямки, чтобы рельеф был цельным
      for (let pass = 0; pass < 2; pass++) {
        for (let i = 0; i < G; i++) for (let j = 0; j < G; j++) {
          const k = i * G + j, n = [];
          if (i > 0) n.push(H[k - G]); if (i < G - 1) n.push(H[k + G]);
          if (j > 0) n.push(H[k - 1]); if (j < G - 1) n.push(H[k + 1]);
          const higher = n.filter((v) => v > H[k]).length, lower = n.filter((v) => v < H[k]).length;
          if (lower >= 3) H[k] = Math.max(...n.filter((v) => v < H[k]));
          else if (higher >= 3) H[k] = Math.min(...n.filter((v) => v > H[k]));
        }
      }
      for (let i = 0; i < G; i++) for (let j = 0; j < G; j++) {
        const h = H[i * G + j];
        const top = h <= 1 ? "sand" : h >= 5 ? "stone" : "grass";
        const dist = Math.hypot(i - c0, j - c0);
        const delay = reduceMotion ? -99 : dist * 0.05 + r() * 0.2;
        const col = { i, j, h, top, delay, lift: 0, layers: [], minVis: 0, ao: [0, 0], edge: [0, 0] };
        cols.push(col); grid.push(col);
      }
      cols.forEach((col) => {
        for (let L = 0; L < col.h; L++) col.layers.push(layerType(col, L));
        // Нижние слои, полностью закрытые соседями спереди, можно не рисовать
        const fl = at(col.i, col.j + 1), fr = at(col.i + 1, col.j), fd = at(col.i + 1, col.j + 1);
        col.minVis = fl && fr && fd ? Math.max(0, Math.min(fl.h, fr.h, fd.h) - 1) : 0;
        // Тени у основания обрыва и светлая кромка у края плато
        const bl = at(col.i - 1, col.j), br = at(col.i, col.j - 1);
        col.ao = [bl && bl.h > col.h ? 1 : 0, br && br.h > col.h ? 1 : 0];
        col.edge = [bl && bl.h < col.h ? 1 : 0, br && br.h < col.h ? 1 : 0];
      });
      order = cols.slice().sort((a, b) => (a.i + a.j) - (b.i + b.j) || a.i - b.i);
      built = reduceMotion;
    }

    function resize() {
      const rect = canvas.getBoundingClientRect();
      dpr = Math.min(2, window.devicePixelRatio || 1);
      vw = Math.max(1, Math.round(rect.width * dpr)); vh = Math.max(1, Math.round(rect.height * dpr));
      canvas.width = vw; canvas.height = vh;
      W = 4 * Math.round(Math.max(48, Math.min(88, rect.width / 16)) * dpr / 4);
      w = W / 2; t = W / 4; hgt = Math.round(W * 0.56);
      const need = Math.ceil((vw + 2 * vh + 8 * hgt) / W) + 4;
      const oldG = G; G = Math.min(70, need);
      sprites = {};
      Object.keys(TYPES).forEach((k) => { sprites[k] = renderCube(TYPES[k]); });
      ox = Math.round(vw / 2 - w);
      oy = Math.round(vh / 2 - G * t + 2.5 * hgt);
      if (!cols.length || oldG !== G) generate();
    }

    const easeOut = (x) => 1 - Math.pow(1 - x, 3);
    let px = -1e5, py = -1e5;
    if (!reduceMotion) {
      window.addEventListener("pointermove", (e) => {
        const rect = canvas.getBoundingClientRect();
        px = (e.clientX - rect.left) * dpr; py = (e.clientY - rect.top) * dpr;
        if (py > vh || py < 0) px = py = -1e5;
      }, { passive: true });
      document.addEventListener("pointerleave", () => { px = py = -1e5; });
    }

    const FALL = 0.5, STEP = 0.09;
    let running = true, visible = true, raf = 0, ready = false;

    function poly(pts, fill) {
      ctx.beginPath(); ctx.moveTo(pts[0][0], pts[0][1]);
      for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i][0], pts[i][1]);
      ctx.closePath(); ctx.fillStyle = fill; ctx.fill();
    }

    function frame(now) {
      raf = 0;
      if (!running || !visible) return;
      const time = (now - startTime) / 1000;
      const m = opts.mouse || { x: 0, y: 0 };
      const sx = reduceMotion ? 0 : Math.round(m.x * -16 * dpr);
      const sy = reduceMotion ? 0 : Math.round(m.y * -10 * dpr);
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, vw, vh);
      ctx.imageSmoothingEnabled = false;

      let hi = -99, hj = -99;
      if (px > -1e4) {
        const X = (px - ox - w - sx) / w, Y = (py - oy - t - sy + 3 * hgt) / t;
        hi = (Y + X) / 2; hj = (Y - X) / 2;
      }
      const strip = Math.max(2, Math.round(t * 0.45));
      let allDone = true;

      for (let k = 0; k < order.length; k++) {
        const col = order[k];
        const bx = ox + (col.i - col.j) * w + sx;
        const by = oy + (col.i + col.j) * t + sy;
        if (bx < -2 * w || bx > vw + 2 || by - (col.h + 1) * hgt > vh + 2 * t || by + 2 * t < 0) continue;

        // Под курсором столбик плавно вырастает (снизу он остаётся сплошным)
        let target = 0;
        if (hi > -90) { const d = Math.hypot(col.i - hi, col.j - hj); if (d < 2.4) target = (1 - d / 2.4) * hgt * 0.5; }
        col.lift += (target - col.lift) * 0.12;
        if (Math.abs(col.lift) < 0.3) col.lift = 0;
        const lift = Math.round(col.lift);

        const from = built && !lift ? col.minVis : 0;
        if (lift) ctx.drawImage(sprites[col.layers[0]], bx - 1, by - hgt + lift - 1);
        let topY = null;
        for (let L = from; L < col.h; L++) {
          let p = built ? 1 : (time - (col.delay + L * STEP)) / FALL;
          if (p <= 0) { allDone = false; break; }
          if (p > 1) p = 1; else allDone = false;
          const y = Math.round(by - (L + 1) * hgt - (1 - easeOut(p)) * hgt * 5 - lift);
          if (p < 1) ctx.globalAlpha = Math.min(1, p * 3);
          ctx.drawImage(sprites[col.layers[L]], bx - 1, y - 1);
          if (p < 1) ctx.globalAlpha = 1;
          if (L === col.h - 1 && p >= 1) topY = y;
        }
        if (topY !== null) {
          // Мягкая тень у подножия более высокого соседа сзади
          if (col.ao[0]) poly([[bx, topY + t], [bx + w, topY], [bx + w + strip * 2, topY + strip], [bx + strip * 2, topY + t + strip]], "rgba(0,0,0,.22)");
          if (col.ao[1]) poly([[bx + w, topY], [bx + 2 * w, topY + t], [bx + 2 * w - strip * 2, topY + t + strip], [bx + w - strip * 2, topY + strip]], "rgba(0,0,0,.16)");
          // Светлая кромка на краю плато
          if (col.edge[0]) poly([[bx, topY + t], [bx + w, topY], [bx + w, topY + Math.max(1, dpr)], [bx, topY + t + Math.max(1, dpr)]], "rgba(255,255,255,.22)");
          if (col.edge[1]) poly([[bx + w, topY], [bx + 2 * w, topY + t], [bx + 2 * w, topY + t + Math.max(1, dpr)], [bx + w, topY + Math.max(1, dpr)]], "rgba(255,255,255,.14)");
          if (lift > 2) poly([[bx + w, topY], [bx + 2 * w, topY + t], [bx + w, topY + 2 * t], [bx, topY + t]], "rgba(255,255,255," + Math.min(0.22, lift / hgt * 0.45).toFixed(3) + ")");
        }
      }
      if (!built && allDone && time > 0.5) built = true;
      if (!ready && time > 0.15) { ready = true; canvas.classList.add("ready"); }
      if (reduceMotion && ready) return;
      raf = requestAnimationFrame(frame);
    }
    function kick() { if (!raf && running && visible) raf = requestAnimationFrame(frame); }

    resize();
    startTime = performance.now();
    let rt = 0;
    window.addEventListener("resize", () => { clearTimeout(rt); rt = setTimeout(() => { resize(); kick(); }, 150); });
    if ("IntersectionObserver" in window) new IntersectionObserver((es) => { visible = es[0].isIntersecting; kick(); }).observe(canvas);
    document.addEventListener("visibilitychange", () => { running = !document.hidden; kick(); });
    kick();
    return { rebuild() { generate(); built = false; startTime = performance.now(); kick(); } };
  }

  // =====================================================================
  //  Фоны секций: стена из блоков, проявляется попиксельно
  // =====================================================================
  const THEMES = {
    dirt: { base: "#3b2a1d", tiles: [["dirt", 50], ["dirt2", 34], ["coarse", 16]] },
    stone: { base: "#3d3d3d", tiles: [["stone", 45], ["stone2", 30], ["andesite", 14], ["coal", 1.6], ["iron", 1.2], ["gold", 0.5], ["diamondOre", 0.5]] },
    planks: { base: "#4a3920", tiles: [["oak", 34], ["oak2", 33], ["oak3", 33], ["spruce", 6]] },
    deepslate: { base: "#202024", tiles: [["deep", 45], ["deep2", 30], ["cobDeep", 8], ["tuff", 6], ["deepDiamond", 0.8], ["amethyst", 0.6]] },
  };
  const STAGES = 8;
  const stageCache = {};
  // Готовим 8 стадий «проявления» для текстуры: пиксели появляются в случайном порядке
  function stagesFor(name) {
    if (stageCache[name]) return stageCache[name];
    const src = textures()[name];
    const data = src.getContext("2d").getImageData(0, 0, 16, 16);
    const r = rng(name.length * 977 + name.charCodeAt(0) * 31);
    const rank = Array.from({ length: 256 }, (_, i) => i);
    for (let i = 255; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [rank[i], rank[j]] = [rank[j], rank[i]]; }
    const out = [];
    for (let s = 1; s <= STAGES; s++) {
      const c = document.createElement("canvas"); c.width = c.height = 16;
      const g = c.getContext("2d"), img = g.createImageData(16, 16);
      const lim = Math.round((s / STAGES) * 256);
      for (let p = 0; p < 256; p++) if (rank[p] < lim) for (let k = 0; k < 4; k++) img.data[p * 4 + k] = data.data[p * 4 + k];
      g.putImageData(img, 0, 0);
      out.push(c);
    }
    return (stageCache[name] = out);
  }

  function wall(section, theme, opts) {
    const th = THEMES[theme];
    if (!th) return null;
    opts = opts || {};
    const reduceMotion = !!opts.reduceMotion;
    const canvas = document.createElement("canvas");
    canvas.className = "section-bg";
    canvas.setAttribute("aria-hidden", "true");
    section.prepend(canvas);
    section.style.backgroundColor = th.base;
    const g = canvas.getContext("2d");
    const total = th.tiles.reduce((s, x) => s + x[1], 0);
    const seed = theme.length * 7919 + theme.charCodeAt(0);
    let S = 4, TS = 64, ncols = 0, nrows = 0, cells = [], t0 = -1, raf = 0, done = reduceMotion;

    function choose(i, j) {
      let q = hash2(i, j, seed) * total;
      for (const [name, wgt] of th.tiles) { q -= wgt; if (q <= 0) return name; }
      return th.tiles[0][0];
    }
    function layout() {
      const rect = section.getBoundingClientRect();
      const dpr = Math.min(2, window.devicePixelRatio || 1);
      canvas.width = Math.max(1, Math.round(rect.width * dpr));
      canvas.height = Math.max(1, Math.round(rect.height * dpr));
      S = Math.max(2, Math.round(4 * dpr)); TS = 16 * S;
      ncols = Math.ceil(canvas.width / TS) + 1; nrows = Math.ceil(canvas.height / TS) + 1;
      cells = [];
      const shift = Math.round((canvas.width - ncols * TS) / 2);
      for (let j = 0; j < nrows; j++) for (let i = 0; i < ncols; i++) {
        cells.push({ x: shift + i * TS, y: j * TS, st: stagesFor(choose(i, j)), delay: (i * 0.9 + j) * 0.03 + hash2(i, j, seed + 5) * 0.18 });
      }
      g.imageSmoothingEnabled = false;
    }
    function draw(now) {
      raf = 0;
      const time = t0 < 0 ? 0 : (now - t0) / 1000;
      g.clearRect(0, 0, canvas.width, canvas.height);
      g.imageSmoothingEnabled = false;
      let pending = false;
      for (const c of cells) {
        let p = done ? 1 : (time - c.delay) / 0.35;
        if (p <= 0) { pending = true; continue; }
        if (p < 1) pending = true; else p = 1;
        const k = Math.min(STAGES - 1, Math.floor(p * STAGES - 1e-6));
        g.drawImage(c.st[k], 0, 0, 16, 16, c.x, c.y, TS, TS);
      }
      if (pending && !done) raf = requestAnimationFrame(draw); else done = true;
    }
    layout();
    if (done) draw(performance.now());
    else if ("IntersectionObserver" in window) {
      const io = new IntersectionObserver((es) => {
        if (es[0].isIntersecting && t0 < 0) { t0 = performance.now(); io.disconnect(); raf = requestAnimationFrame(draw); }
      }, { threshold: 0.08 });
      io.observe(section);
    } else { done = true; draw(performance.now()); }
    // Изменился размер секции (например, раскрыли вопрос) — перерисовать
    let lastW = 0, lastH = 0;
    const ro = "ResizeObserver" in window ? new ResizeObserver(() => {
      const r = section.getBoundingClientRect();
      if (Math.abs(r.width - lastW) < 1 && Math.abs(r.height - lastH) < 1) return;
      lastW = r.width; lastH = r.height;
      layout();
      if (t0 >= 0 || done) { if (!raf) draw(performance.now()); }
    }) : null;
    if (ro) ro.observe(section);
    return { canvas };
  }

  window.YeamuWorld = { start, wall };
})();
