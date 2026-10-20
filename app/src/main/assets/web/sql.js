/* SQL Lab lesson: the V1 / V2 buttons show one version block (query, steps, output) and hide the others. */
(function () {
  document.addEventListener('click', function (ev) {
    var b = ev.target.closest('.sl-ver');
    if (!b) return;
    var v = b.getAttribute('data-v');
    var btns = document.querySelectorAll('.sl-ver');
    for (var i = 0; i < btns.length; i++) btns[i].classList.toggle('is-on', btns[i] === b);
    var blocks = document.querySelectorAll('.sl-version');
    for (var j = 0; j < blocks.length; j++) blocks[j].hidden = blocks[j].getAttribute('data-v') !== v;
  });
})();
