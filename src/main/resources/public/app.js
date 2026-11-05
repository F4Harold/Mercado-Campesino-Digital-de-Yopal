'use strict';

/* ---------- Referencias y estado ---------- */
const $ = id => document.getElementById(id);
const ui = {
  lista: $('lista'), buscar: $('buscar'), vereda: $('vereda'), precio: $('precio'), solo: $('solo'),
  chips: $('categorias'), contador: $('contador'), cierre: $('cierre'), mensaje: $('mensaje'),
  cajon: $('cajon'), velo: $('velo'), misReservas: $('mis-reservas'), favoritos: $('favoritos')
};
const estado = { catalogo: [], ofertas: [], categoria: '', segundos: 0, abierto: true, reloj: null };
const META_CATEGORIAS = {
  'Lácteos':               { ico: '🧀', c1: '#3b4a12', c2: '#1b2a0e' },
  'Tubérculos y plátano':  { ico: '🍌', c1: '#4a3b12', c2: '#2a200e' },
  'Verduras y frutas':     { ico: '🥬', c1: '#14492a', c2: '#0d2a1a' },
  'Huevos y carnes':       { ico: '🥚', c1: '#4a2a1c', c2: '#2a170f' },
  'Otros':                 { ico: '🧺', c1: '#2d3a31', c2: '#17211b' }
};
const ICONOS = { queso:'🧀', cuajada:'🧀', mantequilla:'🧈', yuca:'🥔', platano:'🍌', huevos:'🥚', cerdo:'🥩', pollo:'🍗',
  tomate:'🍅', cilantro:'🌿', maracuya:'🍈', papaya:'🍈' };
const HORAS = { 'Madrugada': ['6:00 a. m.', '7:00 a. m.', '8:00 a. m.'], 'Media mañana': ['9:00 a. m.', '10:00 a. m.', '11:00 a. m.', '12:00 m.'] };
const PAGOS = [['Efectivo', 'Pagas al recoger'], ['Nequi', 'Desde tu celular']];
const NIVEL_MAX = 3;

/* ---------- Utilidades ---------- */
const singular = u => /(ales|ades)$/.test(u) ? u.slice(0, -2) : u.replace(/s$/, '');
const pesos = n => '$' + Math.round(n).toLocaleString('es-CO');
const sinTildes = s => s.normalize('NFD').replace(/\p{M}/g, '').toLowerCase();
const metaDe = o => META_CATEGORIAS[o.categoria] || META_CATEGORIAS['Otros'];
const iconoDe = o => ICONOS[Object.keys(ICONOS).find(k => sinTildes(o.producto).includes(k))] || metaDe(o).ico;

function el(tag, clase, texto, attrs) {
  const e = document.createElement(tag);
  if (clase) e.className = clase;
  if (texto !== undefined) e.textContent = texto;
  if (attrs) for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  return e;
}
function boton(clase, texto, onclick, attrs) {
  const b = el('button', clase, texto, { type: 'button', ...attrs });
  b.addEventListener('click', onclick);
  return b;
}
function guardado(clave, defecto) {
  try { return JSON.parse(localStorage.getItem(clave)) ?? defecto; } catch { return defecto; }
}
function guardar(clave, valor) {
  try { localStorage.setItem(clave, JSON.stringify(valor)); } catch { /* modo privado: se ignora */ }
}
let favoritos = new Set(guardado('mc.favoritos', []));
let reservas = guardado('mc.reservas', []);

function avisar(texto, error, codigo) {
  ui.mensaje.className = 'mensaje' + (error ? ' error' : '');
  ui.mensaje.replaceChildren(document.createTextNode(texto));
  if (codigo) ui.mensaje.append(' Código de recogida: ', el('strong', '', codigo));
  ui.mensaje.hidden = false;
  clearTimeout(avisar.t);
  avisar.t = setTimeout(() => { ui.mensaje.hidden = true; }, 6000);
}

function proximoSabado() {
  const d = new Date();
  d.setDate(d.getDate() + (6 - d.getDay() + 7) % 7);
  return d.toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });
}

/* ---------- Cierre de reservas ---------- */
function pintarCierre() {
  const s = estado.segundos;
  if (!estado.abierto || s <= 0) {
    ui.cierre.textContent = 'Reservas cerradas (viernes 6:00 p. m.)';
    ui.cierre.classList.add('cerrado');
    return;
  }
  const d = Math.floor(s / 86400), h = Math.floor(s % 86400 / 3600), m = Math.floor(s % 3600 / 60);
  ui.cierre.textContent = `Cierra el viernes 6:00 p. m. · faltan ${d} d ${h} h ${m} min`;
  ui.cierre.classList.remove('cerrado');
}
async function cargarEstado() {
  try {
    const r = await (await fetch('/api/estado')).json();
    estado.abierto = r.abierto; estado.segundos = r.segundosParaCierre;
  } catch { estado.abierto = true; }
  pintarCierre();
  clearInterval(estado.reloj);
  estado.reloj = setInterval(() => { estado.segundos--; pintarCierre(); }, 1000);
}

/* ---------- Datos ---------- */
async function pedir(q, vereda) {
  const p = new URLSearchParams({ q: q || '', vereda: vereda || '' });
  return (await fetch('/api/ofertas?' + p)).json();
}
async function cargarCatalogo() {
  estado.catalogo = await pedir('', '');
  pintarStats(); pintarVeredas(); pintarChips();
}
async function cargarOfertas() {
  try { estado.ofertas = await pedir(ui.buscar.value, ui.vereda.value); }
  catch { avisar('Sin conexión. Intente de nuevo.', true); return; }
  pintarLista();
}
function filtradas() {
  const [min, max] = ui.precio.value ? ui.precio.value.split('-').map(v => v === '' ? Infinity : +v) : [0, Infinity];
  return estado.ofertas.filter(o =>
    (!estado.categoria || o.categoria === estado.categoria) &&
    o.precio >= min && o.precio <= (max ?? Infinity) &&
    (!ui.solo.checked || o.disponible > 0));
}

/* ---------- Hero, filtros y chips ---------- */
function pintarStats() {
  const c = estado.catalogo;
  $('st-productos').textContent = c.length;
  $('st-productores').textContent = new Set(c.map(o => o.productor)).size;
  $('st-veredas').textContent = new Set(c.map(o => o.vereda)).size;
  $('st-promedio').textContent = c.length ? pesos(c.reduce((a, o) => a + o.precio, 0) / c.length) : '–';
}
function pintarVeredas() {
  const actual = ui.vereda.value;
  const veredas = [...new Set(estado.catalogo.map(o => o.vereda))].sort();
  ui.vereda.replaceChildren(el('option', '', 'Todas las veredas', { value: '' }),
    ...veredas.map(v => el('option', '', v, { value: v })));
  ui.vereda.value = actual;
}
function pintarChips() {
  const cats = [...new Set(estado.catalogo.map(o => o.categoria))];
  const hacer = valor => boton('chip', '', () => {
    estado.categoria = valor; pintarChips(); pintarLista();
  }, { 'aria-pressed': String(estado.categoria === valor) });
  const con = (b, ico, texto) => { b.append(el('span', '', ico, { 'aria-hidden': 'true' }), document.createTextNode(texto)); return b; };
  ui.chips.replaceChildren(con(hacer(''), '✦', 'Todos'),
    ...cats.map(c => con(hacer(c), (META_CATEGORIAS[c] || META_CATEGORIAS['Otros']).ico, c)));
}

/* ---------- Tarjetas ---------- */
function etiquetaDe(o) {
  if (o.disponible === 0) return ['Agotado', 'agotado'];
  if (o.disponible <= 8) return ['Pocas unidades', ''];
  return null;
}
function tarjeta(o) {
  const m = metaDe(o), t = el('article', 'tarjeta');
  const foto = el('div', 'foto');
  foto.style.setProperty('--c1', m.c1); foto.style.setProperty('--c2', m.c2);
  foto.append(el('span', '', iconoDe(o), { 'aria-hidden': 'true' }));
  const et = etiquetaDe(o);
  if (et) foto.append(el('span', 'etiqueta ' + et[1], et[0]));
  const fav = boton('fav', favoritos.has(o.id) ? '♥' : '♡', () => alternarFavorito(o.id),
    { 'aria-pressed': String(favoritos.has(o.id)), 'aria-label': `Favorito: ${o.producto}` });
  foto.append(fav);
  const libre = o.disponible > 0;
  foto.append(el('span', 'estado' + (libre ? '' : ' no'), libre ? 'Disponible' : 'Sin existencias'));

  const cuerpo = el('div', 'cuerpo');
  const precio = el('p', 'precio', pesos(o.precio)); precio.append(el('small', '', ` /${singular(o.unidad)}`));
  const fila = el('div', 'precio-fila'); fila.append(precio, el('span', 'quedan', libre ? `Quedan ${o.disponible}` : ''));
  const acc = el('div', 'acciones');
  const reservar = boton('btn primario', 'Reservar', () => abrirCajon(o));
  reservar.disabled = !libre || !estado.abierto;
  acc.append(reservar, boton('btn', 'Detalles', () => abrirCajon(o)));
  cuerpo.append(el('p', 'cat', o.categoria), el('h3', '', o.producto),
    el('p', 'meta', `${o.productor} · ${o.vereda}`), fila, acc);
  t.append(foto, cuerpo);
  return t;
}
function pintarLista() {
  const datos = filtradas();
  ui.contador.textContent = `${datos.length} ${datos.length === 1 ? 'producto' : 'productos'}`;
  ui.lista.replaceChildren(...(datos.length ? datos.map(tarjeta) : [el('p', 'vacio', 'No hay ofertas con ese filtro.')]));
}
function alternarFavorito(id) {
  favoritos.has(id) ? favoritos.delete(id) : favoritos.add(id);
  guardar('mc.favoritos', [...favoritos]);
  pintarLista(); pintarLateral();
}

/* ---------- Panel lateral ---------- */
function filaMini(ico, titulo, sub, extra) {
  const li = el('li'), i = el('span', 'ico', ico, { 'aria-hidden': 'true' }), txt = el('div', 'txt');
  txt.append(el('b', '', titulo), el('small', '', sub));
  li.append(i, txt); if (extra) li.append(extra);
  return li;
}
function pintarLateral() {
  const nivel = Math.min(NIVEL_MAX, reservas.length + 1), faltan = Math.max(0, NIVEL_MAX - reservas.length);
  $('nivel').textContent = `Nivel ${nivel} de ${NIVEL_MAX}`;
  $('barra').setAttribute('aria-valuenow', nivel);
  $('barra').firstElementChild.style.width = (nivel / NIVEL_MAX * 100) + '%';
  $('nivel-texto').textContent = faltan ? `Cumple ${faltan} reserva${faltan > 1 ? 's' : ''} más y subes de nivel.` : '¡Ya eres cliente de máxima confianza!';

  ui.misReservas.replaceChildren(...(reservas.length
    ? reservas.slice(0, 3).map(r => filaMini(r.ico, `${r.cantidad} ${r.unidad} · ${r.producto}`, `${r.dia} · ${r.hora} · ${r.pago}`, el('span', 'ok', r.codigo)))
    : [el('li', 'mini-vacio', 'Aún no tienes reservas.')]));

  const fav = estado.catalogo.filter(o => favoritos.has(o.id));
  ui.favoritos.replaceChildren(...(fav.length
    ? fav.slice(0, 4).map(o => filaMini(iconoDe(o), o.producto, `${pesos(o.precio)} · ${o.productor}`,
        o.disponible > 0 && estado.abierto ? boton('btn lima', 'Reservar', () => abrirCajon(o)) : undefined))
    : [el('li', 'mini-vacio', 'Marca el ♡ de un producto para guardarlo.')]));
}

/* ---------- Cajón de reserva ---------- */
let ultimoFoco = null;
function abrirCajon(o) {
  ultimoFoco = document.activeElement;
  const sel = { cantidad: 1, hora: '', pago: 'Efectivo' };
  const m = metaDe(o);

  const cab = el('div', 'cajon-cab');
  cab.style.setProperty('--c1', m.c1); cab.style.setProperty('--c2', m.c2);
  const titulo = el('h2', '', o.producto, { id: 'cajon-titulo' });
  cab.append(boton('cerrar', '✕', cerrarCajon, { 'aria-label': 'Cerrar' }), el('p', 'cat', o.categoria), titulo,
    el('p', '', `${o.productor} · ${o.vereda} · ${pesos(o.precio)}/${singular(o.unidad)}`));

  /* paso 1: cantidad */
  const num = el('output', '', '', { 'aria-live': 'polite' });
  const menos = boton('', '−', () => cambiar(-1), { 'aria-label': 'Menos' });
  const mas = boton('', '+', () => cambiar(1), { 'aria-label': 'Más' });
  const p1 = paso(1, '¿Cuánto necesitas?', `Quedan ${o.disponible}`);
  const cant = el('div', 'cantidad'); cant.append(menos, num, mas); p1.append(cant);

  /* paso 2: hora de recogida */
  const p2 = paso(2, '¿A qué hora recoges?', proximoSabado());
  const botonesHora = [];
  for (const [franja, horas] of Object.entries(HORAS)) {
    const g = el('div', 'rejilla');
    horas.forEach(h => { const b = boton('opcion', h, () => { sel.hora = h; marcar(botonesHora, b); actualizar(); }, { 'aria-pressed': 'false' }); botonesHora.push(b); g.append(b); });
    p2.append(el('p', 'franja', franja), g);
  }

  /* paso 3: pago */
  const p3 = paso(3, '¿Cómo pagas?');
  const g3 = el('div', 'rejilla dos'), botonesPago = [];
  PAGOS.forEach(([n, d]) => {
    const b = boton('opcion', n, () => { sel.pago = n; marcar(botonesPago, b); }, { 'aria-pressed': String(n === sel.pago) });
    b.append(el('small', '', d)); botonesPago.push(b); g3.append(b);
  });
  p3.append(g3, el('p', 'confianza', `Nivel de confianza: ${Math.min(NIVEL_MAX, reservas.length + 1)} de ${NIVEL_MAX}. Pagas solo al recoger tu pedido.`));

  const cuerpo = el('div', 'cajon-cuerpo'); cuerpo.append(p1, p2, p3);
  const total = el('strong'), resumen = el('div', 'resumen'); resumen.append(el('span', '', 'Total a pagar al recoger'), total);
  const confirmar = boton('btn primario', '', enviar);
  const pie = el('div', 'cajon-pie'); pie.append(resumen, confirmar);
  ui.cajon.replaceChildren(cab, cuerpo, pie);

  function cambiar(d) { sel.cantidad = Math.min(Math.min(o.disponible, 50), Math.max(1, sel.cantidad + d)); actualizar(); }
  function actualizar() {
    num.textContent = `${sel.cantidad} `; num.append(el('small', '', o.unidad));
    menos.disabled = sel.cantidad <= 1; mas.disabled = sel.cantidad >= Math.min(o.disponible, 50);
    total.textContent = pesos(o.precio * sel.cantidad);
    confirmar.disabled = !sel.hora || !estado.abierto;
    confirmar.textContent = !estado.abierto ? 'Reservas cerradas' : sel.hora ? 'Reservar y pagar al recoger' : 'Elige una hora para continuar';
  }
  async function enviar() {
    confirmar.disabled = true;
    try {
      const r = await fetch('/api/reservas', { method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idOferta: o.id, cantidad: sel.cantidad }) });
      const d = await r.json();
      if (!r.ok) { avisar(d.error, true); confirmar.disabled = false; await refrescar(); return; }
      reservas.unshift({ producto: o.producto, unidad: o.unidad, ico: iconoDe(o), cantidad: sel.cantidad,
        dia: proximoSabado().split(',')[0], hora: sel.hora, pago: sel.pago, codigo: d.codigo });
      reservas = reservas.slice(0, 20); guardar('mc.reservas', reservas);
      exito(d.codigo);
      await refrescar();
    } catch { avisar('Sin conexión. Intente de nuevo.', true); confirmar.disabled = false; }
  }
  function exito(codigo) {
    const e = el('div', 'exito');
    e.append(el('span', 'ok', '✅', { 'aria-hidden': 'true' }), el('h2', '', 'Reserva confirmada'),
      el('p', '', `${sel.cantidad} ${o.unidad} de ${o.producto}. Recoge el ${proximoSabado()} a las ${sel.hora} y paga con ${sel.pago.toLowerCase()}.`),
      el('p', 'codigo', codigo), el('p', '', 'Muestra este código en el puesto.'));
    const cerrar = boton('btn primario', 'Listo', cerrarCajon);
    const pie2 = el('div', 'cajon-pie'); pie2.append(cerrar);
    ui.cajon.replaceChildren(cab, cuerpo, pie2);
    cuerpo.replaceChildren(e); cerrar.focus();
  }
  actualizar();
  ui.cajon.hidden = ui.velo.hidden = false;
  document.body.style.overflow = 'hidden';
  botonesHora[0].focus();
}
function paso(n, texto, extra) {
  const s = el('section', 'paso'), h = el('h3');
  h.append(el('b', '', n), document.createTextNode(texto));
  if (extra) h.append(el('small', '', extra));
  s.append(h); return s;
}
function marcar(grupo, activo) { grupo.forEach(b => b.setAttribute('aria-pressed', String(b === activo))); }
function cerrarCajon() {
  ui.cajon.hidden = ui.velo.hidden = true;
  document.body.style.overflow = '';
  if (ultimoFoco && document.contains(ultimoFoco)) ultimoFoco.focus();
}
async function refrescar() { await cargarCatalogo(); await cargarOfertas(); pintarLateral(); }

/* Teclado: Esc cierra; Tab queda dentro del cajón */
document.addEventListener('keydown', e => {
  if (ui.cajon.hidden) return;
  if (e.key === 'Escape') return cerrarCajon();
  if (e.key !== 'Tab') return;
  const f = [...ui.cajon.querySelectorAll('button:not(:disabled)')];
  if (!f.length) return;
  const a = f[0], z = f[f.length - 1];
  if (e.shiftKey && document.activeElement === a) { e.preventDefault(); z.focus(); }
  else if (!e.shiftKey && document.activeElement === z) { e.preventDefault(); a.focus(); }
});
ui.velo.addEventListener('click', cerrarCajon);

/* ---------- Eventos e inicio ---------- */
let espera;
ui.buscar.addEventListener('input', () => { clearTimeout(espera); espera = setTimeout(cargarOfertas, 250); });
ui.vereda.addEventListener('change', cargarOfertas);
ui.precio.addEventListener('change', pintarLista);
ui.solo.addEventListener('change', pintarLista);

cargarEstado().then(async () => { await cargarCatalogo(); await cargarOfertas(); pintarLateral(); });
