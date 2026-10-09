// Exercise the actual dialog renderer without HTTP or writes to Oracle.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = path.resolve(__dirname, '../../main/resources/static');
class Element {
  constructor() {
    this.innerHTML = ''; this.textContent = ''; this.dataset = {}; this.value = '';
    this.hidden = false; this.classes = new Set();
    this.classList = {add: x => this.classes.add(x), remove: x => this.classes.delete(x),
      toggle: (x, enabled) => enabled ? this.classes.add(x) : this.classes.delete(x)};
  }
  addEventListener() {} before() {} append() {} focus() {} showModal() {}
  setAttribute() {} removeAttribute() {} querySelector() {return null;} querySelectorAll() {return [];}
}
const elements = new Map();
const document = {body: new Element(), title: '', addEventListener() {}, dispatchEvent() {},
  querySelector(selector) {
    if (selector.includes('[type="submit"]')) return null;
    if (!elements.has(selector)) elements.set(selector, new Element());
    return elements.get(selector);
  }, querySelectorAll() {return [];}};
const window = {ServicioCooperativa: new Proxy({}, {get() {throw new Error('No HTTP in renderer tests');}})};
const context = {window, document, location: {protocol: 'file:', port: ''},
  CustomEvent: class {constructor(type) {this.type = type;}}, matchMedia: () => ({matches:false}),
  setTimeout: () => 1, clearTimeout() {}, console};
vm.runInNewContext(fs.readFileSync(path.join(source, 'datos-ejemplo.js'), 'utf8'), context);
const app = fs.readFileSync(path.join(source, 'app.js'), 'utf8');
assert.match(app, /\}\)\(\);\s*$/);
vm.runInNewContext(app.replace(/\}\)\(\);\s*$/,
  'window.__test = {estado, verDetalle, operacion};\n})();'), context);
const state = window.__test;
state.estado.modo = 'conectado';
state.estado.datos = JSON.parse(JSON.stringify(window.DATOS_EJEMPLO));
const delivery = state.estado.datos.albaranes[0];
const html = () => document.querySelector('#cuerpo-dialogo').innerHTML;
delivery.liquidado = false;
state.verDetalle('albaranes', delivery.id);
assert.match(html(), /Estado de liquidación/);
assert.match(html(), /Pendiente/);
assert.match(html(), /data-operacion="liquidar"/);
state.operacion('liquidar', delivery.id);
assert.match(html(), /<form data-operacion="liquidar"/);
assert.match(html(), /Confirmo abonar esta entrega a la cuenta seleccionada/);
assert.doesNotMatch(html(), /no conserva un estado/);
delivery.liquidado = true;
state.verDetalle('albaranes', delivery.id);
assert.match(html(), /Liquidado/);
assert.doesNotMatch(html(), /data-operacion="liquidar"/);
state.operacion('liquidar', delivery.id);
assert.match(html(), /ya fue abonada/);
assert.doesNotMatch(html(), /<form|name="cuentaId"|data-operacion="liquidar"/);
console.log('PASS: pending deliveries can be paid; settled deliveries show persistent status and cannot reopen the payment form.');
