/* Adaptador de los RestController existentes. No modifica reglas ni crea rutas. */
(function () {
  'use strict';

  const vistaSeparada = window.location.port === '5173' && ['localhost', '127.0.0.1'].includes(window.location.hostname);
  const base = window.location.protocol === 'file:' || vistaSeparada ? 'http://localhost:8081/api' : '/api';
  const tiempoLimite = 20000;
  const cola = [];
  let activas = 0;
  let cargaEnCurso = null;
  let catalogosActuales = null;
  const rutasCatalogos = {
    sedes: 'sedes', estatus: 'estatus', tiposPersona: 'tipos-persona',
    tiposCuenta: 'tipos-cuenta', tiposPrestamo: 'tipos-prestamo',
    categoriasProducto: 'categorias-producto'
  };
  const campos = {
    personas: ['id', 'nombre', 'apellido', 'dni', 'direccion', 'telefono', 'tipoPersonaId', 'estatusId', 'sedeId', 'fechaRegistro'],
    productos: ['id', 'nombre', 'descripcion', 'categoriaId', 'precio', 'stock'],
    cuentas: ['id', 'numero', 'tipoCuentaId', 'estatusId', 'sedeId', 'fechaApertura'],
    prestamos: ['id', 'personaId', 'tipoPrestamoId', 'estatusId', 'monto', 'tasaInteres', 'fecha', 'fechaVencimiento'],
    albaranes: ['id', 'numeroTalonario', 'personaId', 'sedeId', 'fecha']
  };

  // El pool Oracle es compartido: como máximo cuatro peticiones simultáneas.
  function siguiente() {
    while (activas < 4 && cola.length) {
      const tarea = cola.shift();
      activas++;
      tarea.ejecutar().then(tarea.resolver, tarea.rechazar).finally(() => {
        activas--;
        siguiente();
      });
    }
  }

  function limitar(ejecutar) {
    return new Promise((resolver, rechazar) => {
      cola.push({ ejecutar, resolver, rechazar });
      siguiente();
    });
  }

  function errorServicio(mensaje, status, estadoIncierto) {
    const error = new Error(mensaje);
    error.status = status || 0;
    error.estadoIncierto = Boolean(estadoIncierto);
    return error;
  }

  function mensajeError(texto, status) {
    if (status >= 500) return 'El servicio no pudo completar la solicitud. Revisa que la API y su conexión a la base de datos estén disponibles.';
    let mensaje = texto.trim();
    try {
      const objeto = JSON.parse(mensaje);
      mensaje = typeof objeto === 'string' ? objeto : (typeof objeto.message === 'string' ? objeto.message : '');
    } catch (_) { /* Los errores de negocio normalmente llegan como texto. */ }
    if (!mensaje || mensaje.length > 700 || /<[^>]+>|ORA-\d+|SQLException|jdbc:|\bat [\w.$]+\(/i.test(mensaje)) {
      return 'No se pudo completar la solicitud (HTTP ' + status + '). Revisa los datos o la conexión del servicio.';
    }
    return mensaje;
  }

  function pedir(ruta, metodo = 'GET', cuerpo, esperaTexto = false) {
    return limitar(async () => {
      const control = new AbortController();
      const reloj = setTimeout(() => control.abort(), tiempoLimite);
      const escribe = metodo !== 'GET';
      const opciones = { method: metodo, signal: control.signal, headers: { Accept: 'application/json, text/plain' }, cache: 'no-store' };
      if (cuerpo !== undefined) {
        opciones.headers['Content-Type'] = 'application/json';
        opciones.body = JSON.stringify(cuerpo);
      }
      try {
        const respuesta = await fetch(base + ruta, opciones);
        const texto = await respuesta.text();
        if (!respuesta.ok) throw errorServicio(mensajeError(texto, respuesta.status), respuesta.status, escribe && respuesta.status >= 500);
        if (!texto.trim()) return null;
        const tipo = respuesta.headers.get('content-type') || '';
        if (/\bjson\b/i.test(tipo)) {
          try { return JSON.parse(texto); }
          catch (_) {
            // StringHttpMessageConverter puede negociar application/json para
            // el texto sin comillas que devuelven estos RestController.
            if (esperaTexto && !/<[^>]+>/.test(texto)) return texto;
            throw errorServicio('El servicio devolvió una respuesta que no se pudo interpretar.', respuesta.status, escribe);
          }
        }
        // Los PATCH devuelven mensajes de texto; GET/POST normalmente JSON.
        try { return JSON.parse(texto); }
        catch (_) { return texto; }
      } catch (error) {
        if (error.status !== undefined) throw error;
        const mensaje = error.name === 'AbortError'
          ? 'El servicio tardó demasiado en responder.'
          : 'No se pudo conectar con la API. Comprueba que el servicio esté iniciado en el puerto 8081.';
        throw errorServicio(mensaje + (escribe ? ' No se pudo confirmar el resultado; actualiza los registros antes de repetir la operación.' : ''), 0, escribe);
      } finally {
        clearTimeout(reloj);
      }
    });
  }

  function identificador(valor) {
    const numero = Number(valor);
    if (!Number.isSafeInteger(numero) || numero <= 0) throw new Error('Selecciona un registro válido.');
    return String(numero);
  }

  function lista(valor) {
    if (!Array.isArray(valor)) throw new Error('La dirección consultada no devolvió datos de la API. Abre la página desde el servicio del proyecto.');
    return valor;
  }

  async function cargarDatos() {
    const nombresCatalogos = Object.keys(rutasCatalogos);
    const rutas = nombresCatalogos.map(nombre => '/catalogos/' + rutasCatalogos[nombre])
      .concat(['/personas', '/productos', '/prestamos', '/albaranes']);
    const iniciales = await Promise.all(rutas.map(ruta => pedir(ruta).then(lista)));
    const catalogos = {};
    nombresCatalogos.forEach((nombre, indice) => { catalogos[nombre] = iniciales[indice]; });
    const [personas, productos, prestamosLista, albaranesLista] = iniciales.slice(nombresCatalogos.length);

    // CuentaRestController no publica GET /cuentas: recorrer las relaciones
    // existentes y deduplicar por ID, incluyendo cuentas compartidas.
    const porPersona = await Promise.all(personas.map(persona => pedir('/cuentas/persona/' + identificador(persona.id)).then(lista)));
    const unicas = new Map();
    porPersona.forEach(cuentas => cuentas.forEach(cuenta => unicas.set(cuenta.id, cuenta)));
    const cuentas = Array.from(unicas.values());
    const [relaciones, historiales, prestamos, albaranes] = await Promise.all([
      Promise.all(cuentas.map(cuenta => pedir('/cuentas/' + identificador(cuenta.id) + '/personas').then(lista))),
      Promise.all(cuentas.map(cuenta => pedir('/cuentas/' + identificador(cuenta.id) + '/historial-saldos').then(lista))),
      Promise.all(prestamosLista.map(prestamo => pedir('/prestamos/' + identificador(prestamo.id)))),
      Promise.all(albaranesLista.map(albaran => pedir('/albaranes/' + identificador(albaran.id))))
    ]);
    [...prestamos, ...albaranes].forEach(registro => {
      if (!registro || typeof registro !== 'object' || !Array.isArray(registro.detalles)) {
        throw new Error('El servicio devolvió un registro sin sus detalles. Actualiza la página para volver a consultar.');
      }
    });
    catalogosActuales = catalogos;
    return {
      catalogos, personas, productos, cuentas, prestamos, albaranes,
      cuentasPersonas: relaciones.flat(), saldos: historiales.flat()
    };
  }

  function cargar() {
    if (!cargaEnCurso) cargaEnCurso = cargarDatos().finally(() => { cargaEnCurso = null; });
    return cargaEnCurso;
  }

  function seleccionar(datos, nombres) {
    if (!datos || typeof datos !== 'object' || Array.isArray(datos)) throw new Error('Faltan los datos del formulario.');
    const resultado = {};
    nombres.forEach(nombre => { if (datos[nombre] !== undefined && datos[nombre] !== '') resultado[nombre] = datos[nombre]; });
    return resultado;
  }

  function cuerpoRegistro(tipo, datos) {
    if (tipo === 'cuentas') {
      return {
        cuenta: seleccionar(datos.cuenta, campos.cuentas),
        personas: lista(datos.personas).map(persona => seleccionar(persona, ['cuentaId', 'personaId', 'titular'])),
        saldoInicial: datos.saldoInicial
      };
    }
    const resultado = seleccionar(datos, campos[tipo]);
    if (tipo === 'prestamos' || tipo === 'albaranes') {
      const nombres = tipo === 'prestamos'
        ? ['id', 'prestamoId', 'productoId', 'cantidad', 'precioUnitario']
        : ['id', 'albaranId', 'productoId', 'cantidadAceptada', 'cantidadRechazada', 'precioUnitario'];
      resultado.detalles = lista(datos.detalles || []).map(detalle => seleccionar(detalle, nombres));
    }
    return resultado;
  }

  function guardar(tipo, datos, id) {
    if (!Object.prototype.hasOwnProperty.call(campos, tipo)) throw new Error('Este módulo no admite registros desde la API.');
    const editar = id !== undefined && id !== null && id !== '';
    if (editar && !['personas', 'productos'].includes(tipo)) throw new Error('La API no ofrece edición para este módulo.');
    const ruta = '/' + tipo + (editar ? '/' + identificador(id) : '');
    return pedir(ruta, editar ? 'PUT' : 'POST', cuerpoRegistro(tipo, datos));
  }

  function operar(nombre, id, valores = {}) {
    const registro = identificador(id);
    const operaciones = {
      deposito: ['PATCH', '/cuentas/' + registro + '/depositar', { monto: valores.monto }],
      retiro: ['PATCH', '/cuentas/' + registro + '/retirar', { monto: valores.monto }],
      abono: ['PATCH', '/prestamos/' + registro + '/abono', { montoAbono: valores.montoAbono ?? valores.monto }],
      stock: ['PATCH', '/productos/' + registro + '/stock', { cantidad: valores.cantidad }],
      asociar: ['POST', '/cuentas/' + registro + '/personas', { personaId: valores.personaId, titular: valores.titular ?? false }],
      liquidar: ['PATCH', '/albaranes/' + registro + '/liquidar', { cuentaId: valores.cuentaId }]
    };
    if (nombre === 'estatus-persona' || nombre === 'estatus-prestamo') {
      const estatus = valores.estatus || catalogosActuales?.estatus.find(item => String(item.id) === String(valores.estatusId))?.nombre;
      operaciones[nombre] = ['PATCH', '/' + (nombre === 'estatus-persona' ? 'personas' : 'prestamos') + '/' + registro + '/estatus', { estatus }];
    }
    const operacion = operaciones[nombre];
    if (!operacion) throw new Error('Esta operación no está disponible en la API.');
    const parametros = new URLSearchParams();
    Object.entries(operacion[2]).forEach(([campo, valor]) => {
      if (valor === undefined || valor === null || valor === '') throw new Error('Falta el dato requerido: ' + campo + '.');
      parametros.set(campo, String(valor));
    });
    return pedir(operacion[1] + '?' + parametros.toString(), operacion[0], undefined, true);
  }

  function quitarPersona(cuentaId, personaId) {
    return pedir('/cuentas/' + identificador(cuentaId) + '/personas/' + identificador(personaId), 'DELETE', undefined, true);
  }

  function eliminar(tipo, id) {
    if (!['personas', 'productos'].includes(tipo)) throw new Error('La API no ofrece eliminación para este módulo.');
    return pedir('/' + tipo + '/' + identificador(id), 'DELETE', undefined, true);
  }

  // Importar este archivo nunca inicia consultas ni carga datos de ejemplo.
  window.ServicioCooperativa = Object.freeze({ cargar, guardar, operar, quitarPersona, eliminar });
}());
