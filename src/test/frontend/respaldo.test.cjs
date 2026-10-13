// Verifica que el botón "Respaldar ahora" dispare el respaldo al hacer clic.
//
// El atributo data-respaldar se emite SIN valor. En el DOM eso equivale a
// element.dataset.respaldar === '' (cadena vacía, que es FALSA en JavaScript).
// Por eso la manejadora de clics debe comprobar la PRESENCIA del atributo con
// hasAttribute('data-respaldar'): si se comprueba con dataset.respaldar, el botón
// no hace nada y no queda ningún rastro en consola. Este test fija ese contrato.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = path.resolve(__dirname, '../../main/resources/static');

// Elemento mínimo que imita el DOM real para atributos data-*:
// - hasAttribute() informa si el atributo está presente, con o sin valor.
// - dataset incluye los data-* presentes; los que no tienen valor quedan en ''.
class Element {
  constructor(attrs = {}) {
    this.attrs = attrs;
    this.dataset = {};
    for (const [nombre, valor] of Object.entries(attrs)) {
      if (!nombre.startsWith('data-')) continue;
      const clave = nombre.slice('data-'.length).replace(/-([a-z])/g, (_, c) => c.toUpperCase());
      this.dataset[clave] = valor;
    }
    this.innerHTML = ''; this.textContent = ''; this.value = '';
    this.hidden = false; this.disabled = false; this.classes = new Set();
    this.classList = {
      add: x => this.classes.add(x), remove: x => this.classes.delete(x),
      toggle: (x, activo) => (activo ? this.classes.add(x) : this.classes.delete(x))
    };
  }
  hasAttribute(nombre) { return Object.prototype.hasOwnProperty.call(this.attrs, nombre); }
  addEventListener() {} before() {} append() {} focus() {} showModal() {}
  setAttribute() {} removeAttribute() {} querySelector() { return null; } querySelectorAll() { return []; }
}

// Construye el elemento a partir del HTML REAL que produce la vista, para no
// asumir cómo se escribió el atributo: se lee la etiqueta tal cual se genera.
function botonDesdeHtml(markup) {
  const inicio = markup.indexOf('<button');
  assert.notEqual(inicio, -1, 'la vista de respaldo no generó ningún botón');
  const fin = markup.indexOf('>', inicio);
  const interior = markup.slice(inicio + '<button'.length, fin);
  const attrs = {};
  const re = /([a-zA-Z][\w-]*)(?:\s*=\s*"([^"]*)")?/g;
  let coincidencia;
  while ((coincidencia = re.exec(interior))) {
    attrs[coincidencia[1]] = coincidencia[2] === undefined ? '' : coincidencia[2];
  }
  return new Element(attrs);
}

const oyentes = { click: [] };
const elements = new Map();
const document = {
  body: new Element(), title: '',
  addEventListener(tipo, manejador) { (oyentes[tipo] = oyentes[tipo] || []).push(manejador); },
  dispatchEvent() {},
  querySelector(selector) {
    if (selector.includes('[type="submit"]')) return null;
    if (!elements.has(selector)) elements.set(selector, new Element());
    return elements.get(selector);
  },
  querySelectorAll() { return []; }
};

let llamadas = 0;
const window = {
  ServicioCooperativa: {
    ejecutarRespaldo() {
      llamadas += 1;
      return Promise.resolve({ enCurso: true, exitoso: false, fecha: null, mensaje: 'Respaldo en curso...', conteos: {}, postgresAlcanzable: true });
    },
    estadoRespaldo() {
      return Promise.resolve({ enCurso: false, exitoso: true, fecha: '2026-01-01 00:00:00', mensaje: 'ok', conteos: {}, postgresAlcanzable: true });
    }
  }
};
const context = { window, document, location: { protocol: 'file:', port: '' },
  CustomEvent: class { constructor(type) { this.type = type; } }, matchMedia: () => ({ matches: false }),
  setTimeout: () => 1, clearTimeout() {}, console };

const app = fs.readFileSync(path.join(source, 'app.js'), 'utf8');
assert.match(app, /conectar\(\);\s*\}\)\(\);\s*$/, 'app.js cambió de forma y el test necesita revisarse');
vm.runInNewContext(app.replace(/conectar\(\);\s*\}\)\(\);\s*$/, 'window.__test = {estado, respaldo};\n})();'), context);

async function asentar() { await new Promise(resolver => setImmediate(resolver)); await new Promise(resolver => setImmediate(resolver)); await new Promise(resolver => setImmediate(resolver)); }
function clicEn(boton) {
  const evento = { target: { closest: selector => (selector === 'button' ? boton : null) } };
  oyentes.click.forEach(manejador => manejador(evento));
}

(async () => {
  const html = window.__test.respaldo();
  assert.match(html, /Respaldar ahora/, 'la vista debe ofrecer el botón de respaldo');

  // Caso real: el atributo va presente y sin valor.
  const boton = botonDesdeHtml(html);
  assert.ok(boton.hasAttribute('data-respaldar'), 'el botón debe llevar data-respaldar');
  assert.equal(boton.dataset.respaldar, '', 'data-respaldar va sin valor: dataset.respaldar es cadena vacía (falsa)');
  clicEn(boton);
  await asentar();
  assert.equal(llamadas, 1, 'el clic en "Respaldar ahora" debe disparar exactamente un respaldo');

  // Control negativo: sin el atributo, el clic no debe disparar nada.
  const ajeno = botonDesdeHtml(html);
  delete ajeno.attrs['data-respaldar'];
  clicEn(ajeno);
  await asentar();
  assert.equal(llamadas, 1, 'un botón sin data-respaldar no debe disparar el respaldo');

  console.log('PASS: el botón "Respaldar ahora" (data-respaldar sin valor) dispara el respaldo una sola vez y nada más lo hace.');
})().catch(error => { console.error(error.message); process.exitCode = 1; });
