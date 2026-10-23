/* SQL Lab lesson: the V1 / V2 tabs show one version block (query, steps, output) and hide the others.
   Each tab has a star: tapping it marks that version as preferred (Slate saves it as user_progress.preferred, the same
   column the web uses); tapping the filled star clears it. The page opens on the preferred version, and a preference
   that arrives later (sync) switches to it only while the user hasn't picked a tab. */
(function () {
  var init = window.__init || {};
  var pref = init.preferred == null ? null : init.preferred;
  var touched = false;

  function show(v) {
    var btns = document.querySelectorAll('.sl-ver');
    for (var i = 0; i < btns.length; i++) {
      var on = btns[i].getAttribute('data-v') === String(v);
      btns[i].classList.toggle('is-on', on);
      btns[i].setAttribute('aria-selected', on ? 'true' : 'false');
    }
    var blocks = document.querySelectorAll('.sl-version');
    for (var j = 0; j < blocks.length; j++) blocks[j].hidden = blocks[j].getAttribute('data-v') !== String(v);
  }

  function paint() {
    var btns = document.querySelectorAll('.sl-ver');
    for (var i = 0; i < btns.length; i++) {
      var on = pref != null && btns[i].getAttribute('data-v') === String(pref);
      btns[i].classList.toggle('is-pref', on);
      var star = btns[i].querySelector('.sl-pref');
      if (star) star.setAttribute('aria-pressed', on ? 'true' : 'false');
    }
  }

  window.setPreferred = function (v) {
    pref = v == null ? null : v;
    paint();
    if (!touched && pref != null && document.querySelector('.sl-ver[data-v="' + pref + '"]')) show(pref);
  };

  document.addEventListener('click', function (ev) {
    var star = ev.target.closest('.sl-pref');
    if (star) {
      ev.stopPropagation();
      touched = true;
      var v = parseInt(star.closest('.sl-ver').getAttribute('data-v'), 10);
      pref = pref === v ? null : v;
      paint();
      if (window.Slate && window.Slate.prefer) window.Slate.prefer(pref == null ? -1 : pref);
      return;
    }
    var b = ev.target.closest('.sl-ver');
    if (!b) return;
    touched = true;
    show(b.getAttribute('data-v'));
  });

  function start() {
    paint();
    if (pref != null && document.querySelector('.sl-ver[data-v="' + pref + '"]')) show(pref);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();

/* Tap a step picture and it opens full screen at 1.25× its natural size, pannable; any tap there (or the ✕) closes it.
   A tap that ends a text selection does nothing. Port of ict-quiz src/components/sql/vizZoom.js — keep them in step. */
(function () {
  var SCALE = 1.25;
  var open = null;
  function tell(v) { if (window.Slate && window.Slate.zoomed) window.Slate.zoomed(v); }
  function close() { if (open) { open.parentNode.removeChild(open); open = null; tell(false); } }
  document.addEventListener('click', function (ev) {
    if (open) return;
    var svg = ev.target.closest && ev.target.closest('.sl-viz');
    if (!svg) return;
    if (String(window.getSelection ? window.getSelection() : '').trim()) return;
    var vb = svg.viewBox && svg.viewBox.baseVal;
    var copy = svg.cloneNode(true);
    var w = Math.max(Math.round(((vb && vb.width) || svg.clientWidth) * SCALE), window.innerWidth - 24);
    copy.removeAttribute('style');
    copy.style.width = w + 'px';
    copy.style.maxWidth = 'none';
    var layer = document.createElement('div');
    layer.className = 'sl-zoom';
    layer.innerHTML = '<button class="sl-zoom-x" aria-label="Close"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg></button>';
    var pane = document.createElement('div');
    pane.className = 'sl-zoom-pane';
    pane.appendChild(copy);
    layer.appendChild(pane);
    layer.addEventListener('click', function (e) { e.stopPropagation(); close(); });
    (document.querySelector('.sl-root') || document.body).appendChild(layer);
    open = layer;
    tell(true);
    var r = svg.getBoundingClientRect();
    var fx = r.width ? (ev.clientX - r.left) / r.width : 0.5;
    pane.scrollLeft = Math.max(0, fx * pane.scrollWidth - pane.clientWidth / 2);
  });
  window.closeVizZoom = function () { if (!open) return false; close(); return true; };
})();
