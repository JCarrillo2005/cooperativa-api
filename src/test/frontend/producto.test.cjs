// Las existencias de un producto solo se capturan al registrarlo. En la edicion se
// administran con movimientos ("Agregar existencias"), para que guardar el nombre o
// el precio no revierta el inventario que cambio mientras el formulario estaba abierto.
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
const app = fs.readFileSync(path.join(source, 'app.js'), 'utf8');
assert.match(app, /conectar\(\);\s*\}\)\(\);\s*$/);
vm.runInNewContext(app.replace(/conectar\(\);\s*\}\)\(\);\s*$/,
  'window.__test = {estado, formulario};\n})();'), context);
const state = window.__test;
state.estado.modo = 'conectado';
state.estado.datos = {
  catalogos: {
    sedes: [{id: 1, nombre: 'SEDE CENTRAL'}],
    estatus: [{id: 1, nombre: 'ACTIVO'}],
    tiposPersona: [{id: 2, nombre: 'SOCIO'}],
    tiposCuenta: [{id: 1, nombre: 'AHORRO'}],
    tiposPrestamo: [], categoriasProducto: [{id: 2, nombre: 'INSUMO_SOCIOS'}]
  },
  personas: [], cuentas: [], cuentasPersonas: [], saldos: [], prestamos: [], albaranes: [],
  productos: [{id: 5, nombre: 'Semilla maíz', descripcion: '', categoriaId: 2, precio: '25.50', stock: 96}]
};
const html = () => document.querySelector('#cuerpo-dialogo').innerHTML;
// Alta: se capturan las existencias iniciales.
state.formulario('productos');
assert.match(html(), /name="stock"/);
assert.match(html(), /Existencias \(unidades\)/);
// Edición: no hay campo de existencias, pero sí el precio y el stock informativo.
state.formulario('productos', 5);
assert.doesNotMatch(html(), /name="stock"/);
assert.match(html(), /name="precio"/);
assert.match(html(), /Existencias actuales: 96 unidades/);
assert.match(html(), /no las modifica/);
console.log('PASS: la edicion del producto no expone el stock y el alta si lo captura.');
