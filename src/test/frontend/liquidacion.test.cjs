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
const app = fs.readFileSync(path.join(source, 'app.js'), 'utf8');
assert.match(app, /conectar\(\);\s*\}\)\(\);\s*$/);
vm.runInNewContext(app.replace(/conectar\(\);\s*\}\)\(\);\s*$/,
  'window.__test = {estado, verDetalle, operacion};\n})();'), context);
const state = window.__test;
state.estado.modo = 'conectado';
state.estado.datos = {
  catalogos: {
    sedes: [{id: 1, nombre: 'SEDE CENTRAL'}],
    estatus: [{id: 1, nombre: 'ACTIVO'}, {id: 3, nombre: 'PAGADO'}],
    tiposPersona: [{id: 2, nombre: 'SOCIO'}],
    tiposCuenta: [{id: 1, nombre: 'AHORRO'}],
    tiposPrestamo: [], categoriasProducto: []
  },
  personas: [{id: 7, nombre: 'Ana', apellido: 'Prueba', dni: 'C-0007', tipoPersonaId: 2, estatusId: 1, sedeId: 1}],
  cuentas: [{id: 11, numero: 'CTA-11', tipoCuentaId: 1, estatusId: 1, sedeId: 1}],
  cuentasPersonas: [{cuentaId: 11, personaId: 7, titular: true}],
  saldos: [{id: 1, cuentaId: 11, monto: '1000.00', fecha: '2026-01-10'}],
  prestamos: [],
  productos: [
    {id: 3, nombre: 'Tomate', descripcion: '', categoriaId: 2, precio: '7.50', stock: 0},
    {id: 4, nombre: 'Cebolla', descripcion: '', categoriaId: 2, precio: '5.00', stock: 0}
  ],
  albaranes: [{
    id: 1, numeroTalonario: 'TAL-0001', personaId: 7, sedeId: 1, fecha: '2026-01-12',
    detalles: [
      {id: 1, albaranId: 1, productoId: 3, cantidadAceptada: '125.50', cantidadRechazada: '4.50', precioUnitario: '7.50'},
      {id: 2, albaranId: 1, productoId: 4, cantidadAceptada: '80.00', cantidadRechazada: '0.00', precioUnitario: '5.00'}
    ]
  }]
};
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
