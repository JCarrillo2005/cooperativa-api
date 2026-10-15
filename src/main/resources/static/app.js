/* Presentación web sobre los controllers REST existentes.
 * Los datos se cargan exclusivamente desde la API (que consulta Oracle al
 * abrir la aplicación); no existen datos de ejemplo ni botón de conexión.
 */
(() => {
  'use strict';
  const $ = (selector, base = document) => base.querySelector(selector);
  const $$ = (selector, base = document) => [...base.querySelectorAll(selector)];
  const escapar = valor => String(valor ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const nombres = {panel:'Panel general',personas:'Personas',cuentas:'Cuentas',prestamos:'Préstamos',productos:'Productos',albaranes:'Albaranes',respaldo:'Respaldo'};
  const estado = {vista:'panel',datos:null,sede:'',busqueda:'',filtro:'todos',modo:'desconectado',cargando:false,guardando:false};
  const vacio = {catalogos:{sedes:[],estatus:[],tiposPersona:[],tiposCuenta:[],tiposPrestamo:[],categoriasProducto:[]},personas:[],cuentas:[],cuentasPersonas:[],saldos:[],prestamos:[],productos:[],albaranes:[]};
  const datos = () => estado.datos || vacio;
  const buscar = (lista,id) => lista.find(item => String(item.id) === String(id));
  const catalogo = (tipo,id) => buscar(datos().catalogos[tipo] || [],id)?.nombre || 'Sin catálogo';
  const legible = texto => String(texto || '').toLocaleLowerCase('es').replaceAll('_',' ').replace(/^./,letra => letra.toLocaleUpperCase('es'));
  const persona = id => {const p = buscar(datos().personas,id); return p ? `${p.nombre} ${p.apellido}` : 'Persona no disponible';};
  const sede = id => legible(catalogo('sedes',id));
  const producto = id => buscar(datos().productos,id)?.nombre || 'Producto no disponible';
  const numero = valor => new Intl.NumberFormat('es-GT',{maximumFractionDigits:2}).format(Number(valor));
  // Aritmética decimal para las cifras de presentación; las reglas finales pertenecen a Java.
  function centavos(valor) {
    const texto = String(valor ?? 0);
    if (!/^\d+(\.\d{1,2})?$/.test(texto)) throw new Error('Utiliza números positivos con hasta dos decimales.');
    const [entero,decimal = ''] = texto.split('.');
    return BigInt(entero) * 100n + BigInt(decimal.padEnd(2,'0'));
  }
  const monedaCentavos = valor => `Q ${new Intl.NumberFormat('es-GT',{minimumFractionDigits:2,maximumFractionDigits:2}).format(Number(valor) / 100)}`;
  const moneda = valor => monedaCentavos(centavos(valor));
  const totalAlbaran = a => ((a.detalles || []).reduce((s,d) => s + centavos(d.cantidadAceptada) * centavos(d.precioUnitario),0n) + 50n) / 100n;
  const fecha = valor => valor ? new Intl.DateTimeFormat('es-GT',{day:'2-digit',month:'short',year:'numeric',timeZone:/^\d{4}-\d{2}-\d{2}$/.test(String(valor))?'UTC':'America/Guatemala'}).format(new Date(valor)) : '—';
  function fechaManana() {
    const partes=new Intl.DateTimeFormat('en-US',{year:'numeric',month:'2-digit',day:'2-digit',timeZone:'America/Guatemala'}).formatToParts(new Date());
    const obtener=tipo=>partes.find(p=>p.type===tipo).value;
    const dia=new Date(`${obtener('year')}-${obtener('month')}-${obtener('day')}T00:00:00Z`);
    dia.setUTCDate(dia.getUTCDate()+1);return dia.toISOString().slice(0,10);
  }
  const activo = p => catalogo('estatus',p.estatusId).toUpperCase() === 'ACTIVO';
  const esSocio = p => catalogo('tiposPersona',p.tipoPersonaId).toUpperCase() === 'SOCIO';
  function saldo(cuentaId) {
    const saldos = datos().saldos.filter(s => s.cuentaId === cuentaId).sort((a,b) => new Date(b.fecha)-new Date(a.fecha) || b.id-a.id);
    return saldos.length ? saldos[0].monto : '0';
  }
  function registros(tipo, aplicarBusqueda = true) {
    return datos()[tipo].filter(item => {
      let sedeId = item.sedeId;
      if (tipo === 'prestamos') sedeId = buscar(datos().personas,item.personaId)?.sedeId;
      if (estado.sede && tipo !== 'productos' && String(sedeId) !== estado.sede) return false;
      if (!aplicarBusqueda || !estado.busqueda) return true;
      const texto = [JSON.stringify(item),persona(item.personaId),sede(sedeId),catalogo('tiposPersona',item.tipoPersonaId),catalogo('tiposCuenta',item.tipoCuentaId),catalogo('tiposPrestamo',item.tipoPrestamoId),catalogo('categoriasProducto',item.categoriaId)].join(' ');
      return normalizar(texto).includes(normalizar(estado.busqueda));
    });
  }
  const normalizar = texto => String(texto).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
  function insignia(id) {
    const nombre = catalogo('estatus',id);
    return `<span class="badge ${['ACTIVO','PAGADO'].includes(nombre.toUpperCase()) ? 'badge-success' : 'badge-neutral'}">${escapar(legible(nombre))}</span>`;
  }
  const detalle = (etiqueta,valor) => `<div class="detail-item"><span class="detail-label">${escapar(etiqueta)}</span><span class="detail-value">${escapar(valor)}</span></div>`;
  const botonVer = (tipo,id,texto='Ver detalle') => `<button type="button" class="btn btn-text" data-detalle="${tipo}" data-id="${id}" aria-label="${texto} ${id}">${texto} <span aria-hidden="true">↗</span></button>`;
  const cabecera = (etiqueta,titulo,descripcion,accion='') => `<div class="section-heading"><div><p class="eyebrow">${etiqueta}</p><h1>${titulo}</h1><p class="muted">${descripcion}</p></div>${accion}</div>`;
  const nuevo = (tipo,texto) => `<button class="btn btn-primary" type="button" data-nuevo="${tipo}"><span aria-hidden="true">＋</span> ${texto}</button>`;
  function tabla(columnas,filas,mensaje='No hay registros para esta selección.') {
    if (!filas.length) return `<div class="empty-state"><span class="empty-symbol" aria-hidden="true">◎</span><h3>${estado.datos ? 'Sin resultados' : 'Consultando los registros…'}</h3><p>${estado.datos ? mensaje : 'El sistema se está conectando con la base de datos de la cooperativa.'}</p>${estado.datos ? '<button class="btn btn-secondary" data-limpiar>Limpiar filtros</button>' : ''}</div>`;
    return `<div class="table-wrap" tabindex="0" aria-label="Tabla de registros"><table class="data-table"><thead><tr>${columnas.map(c => `<th scope="col">${c}</th>`).join('')}</tr></thead><tbody>${filas.join('')}</tbody></table></div>`;
  }
  const conteo = (valor,dec=0,pre="") => estado.datos ? ` data-count="${Number(valor)}" data-dec="${dec}" data-pre="${pre}"` : "";
  function metrica(valor,etiqueta,nota,atributos="") {return `<article class="metric"><span class="metric-label">${etiqueta}</span><div class="metric-value"${atributos}>${valor}</div><span class="metric-note">${nota}</span></article>`;}
  function panel() {
    const conectado = estado.modo === 'conectado' && Boolean(estado.datos);
    const socios = registros('personas',false).filter(p => esSocio(p) && activo(p)).length;
    const prestamos = registros('prestamos',false).filter(activo);
    const ahorro = registros('cuentas',false).filter(c => catalogo('tiposCuenta',c.tipoCuentaId).toUpperCase() === 'AHORRO').reduce((s,c) => s + centavos(saldo(c.id)),0n);
    const cartera = prestamos.reduce((s,p) => s + centavos(p.monto),0n);
    const calidad = registros('albaranes',false).flatMap(a => a.detalles).reduce((s,d) => ({aceptado:s.aceptado+Number(d.cantidadAceptada),total:s.total+Number(d.cantidadAceptada)+Number(d.cantidadRechazada)}),{aceptado:0,total:0});
    // Todas las cifras provienen de los registros consultados a la base de datos.
    const resumen = {
      socios, prestamos:prestamos.length, capital:Number(cartera)/100,
      aprobado:calidad.total ? calidad.aceptado/calidad.total*100 : null,
      ahorro:Number(ahorro)/100, produccion:calidad.total
    };
    const atributos = (valor,dec=0,pre='') => valor == null ? '' : ` data-count="${valor}" data-dec="${dec}" data-pre="${pre}"`;
    const fechaPanel = new Intl.DateTimeFormat('es-GT',{weekday:'long',day:'numeric',month:'long',year:'numeric',timeZone:'America/Guatemala'}).format(new Date());
    const sedePanel = conectado ? (estado.sede ? sede(Number(estado.sede)) : 'Todas las sedes') : 'Conectando con el sistema…';
    const nota = conectado ? 'Datos del sistema · selección actual de sede' : 'Conectando con la base de datos del sistema';
    const formatoAhorro = moneda(resumen.ahorro);
    const etapas = [
      ['Socios',numero(resumen.socios),'agricultora.jpg','Agricultora en su comunidad','Nuestra fuerza comienza en las personas. Cada socio aporta una historia, una cosecha y un futuro compartido.'],
      ['Préstamos activos',numero(resumen.prestamos),'cultivo.jpg','Agricultor trabajando entre cultivos','El impulso para dar el siguiente paso. Recursos que acompañan el trabajo, desde la primera semilla.'],
      ['Ahorro',formatoAhorro,'familia-agricultora.jpg','Madre agricultora y su hija','Lo que cultivamos hoy también cuida el mañana. El ahorro une el esfuerzo de las familias con nuevas posibilidades.'],
      ['Producción recibida',`${numero(resumen.produccion)} kg`,'centro-acopio.jpg','Sacos agrícolas en un centro de acopio','La cosecha encuentra su lugar. Recepción y calidad para reconocer el valor de lo que llega del campo.']
    ];
    const modulos = [
      ['personas','01','Las personas, primero.','Socios y empleados, en un mismo lugar.'],
      ['cuentas','02','Un ahorro que crece.','Cuentas, titulares e historial de saldos.'],
      ['prestamos','03','Impulso para el campo.','Efectivo, hipotecas y consumo de productos.'],
      ['productos','04','Cada insumo cuenta.','Catálogo, precios y existencias.'],
      ['albaranes','05','El valor de la cosecha.','Recepción, calidad y detalle de cada entrega.']
    ];
    const documentos = [
      ...registros('prestamos').map(p => ({fecha:p.fecha,tipo:'Préstamo',ref:`PR-${String(p.id).padStart(4,'0')}`,personaId:p.personaId,sedeId:buscar(datos().personas,p.personaId)?.sedeId,valor:moneda(p.monto),id:p.id,modulo:'prestamos'})),
      ...registros('albaranes').map(a => ({...a,tipo:'Albarán',ref:a.numeroTalonario,valor:monedaCentavos(totalAlbaran(a)),modulo:'albaranes'}))
    ].sort((a,b) => new Date(b.fecha)-new Date(a.fecha)).slice(0,6);
    return `<div class="editorial-panel">
      <section class="editorial-hero" id="panel-portada" aria-labelledby="saludo-panel">
        <div class="editorial-hero-media"><img class="editorial-hero-photo" src="assets/images/cafetal.jpg" width="2000" height="1333" alt="Cafetal entre colinas verdes" data-parallax="16" data-hero-photo fetchpriority="high"></div>
        <div class="editorial-hero-copy"><p class="editorial-date">${escapar(fechaPanel)} · ${escapar(sedePanel)}</p><h1 id="saludo-panel">Buenos días.</h1><p class="editorial-lead">La cooperativa está creciendo.</p><p class="editorial-caption">Un mismo origen.<br>Muchas formas de crecer.</p></div>
        <a class="editorial-scroll" href="#panel-metricas"><span>DESCUBRE TU COOPERATIVA</span><span aria-hidden="true">↓</span></a>
      </section>
      <section class="editorial-metrics" id="panel-metricas" aria-labelledby="titulo-metricas">
        <header class="editorial-section-heading"><div><p class="eyebrow">EL VALOR DE ESTAR JUNTOS</p><h2 id="titulo-metricas">El esfuerzo se convierte<br>en oportunidades.</h2><p class="editorial-data-note">${nota}</p></div><figure class="editorial-metric-photo" data-clip><img src="assets/images/aguacate-hass.jpg" width="1200" height="800" alt="Aguacate Hass entero y partido" data-parallax="6" loading="lazy"><figcaption>De la tierra, a nuestra mesa.</figcaption></figure></header>
        <div class="editorial-metric-grid" data-stagger>
          <article class="editorial-metric"><span class="editorial-metric-label">Socios activos</span><div class="editorial-metric-number"${atributos(resumen.socios)}>${numero(resumen.socios)}</div><p>Personas que construyen un futuro compartido.</p></article>
          <article class="editorial-metric"><span class="editorial-metric-label">Cartera activa</span><div class="editorial-metric-number editorial-metric-number--money"${atributos(resumen.capital,2,'Q ')}>${moneda(resumen.capital)}</div><p>Saldo pendiente de los préstamos activos.</p></article>
          <article class="editorial-metric"><span class="editorial-metric-label">Producto aceptado</span><div class="editorial-metric-number"${atributos(resumen.aprobado,2)}>${resumen.aprobado==null?'—':`${numero(resumen.aprobado)}<small>%</small>`}</div><p>Proporción de los kilogramos recibidos en albaranes.</p><div class="editorial-bar-track"><span data-bar="${resumen.aprobado ?? 0}" style="width:${resumen.aprobado ?? 0}%"></span></div></article>
          <article class="editorial-metric"><span class="editorial-metric-label">Ahorro registrado</span><div class="editorial-metric-number editorial-metric-number--money"${atributos(resumen.ahorro,2,'Q ')}>${moneda(resumen.ahorro)}</div><p>El esfuerzo de hoy cuida el mañana.</p></article>
        </div>
        <svg class="editorial-growth-line" viewBox="0 0 1200 180" aria-hidden="true"><path data-draw d="M0 150C130 155 130 95 260 107S435 180 550 92 770 140 860 70 1090 80 1200 15"/></svg>
      </section>
      <section class="editorial-story" id="panel-historias" aria-label="Cuatro formas de crecer" data-parallax-scene>
        <div class="editorial-story-visual"><div class="editorial-story-photo-frame" data-clip>${etapas.map(([titulo,valor,imagen,alt],i)=>`<img class="editorial-story-photo" src="assets/images/${imagen}" width="${i===0?1800:i===2?1000:1200}" height="${i===0?1200:i===2?664:800}" alt="${alt}" data-story-photo="${i}" data-parallax="8">`).join('')}</div><div class="editorial-story-caption"><span data-story-caption>01 / Socios</span><p>LA TIERRA NOS CONECTA</p></div><div class="editorial-story-indicators" aria-hidden="true">${etapas.map((_,i)=>`<span data-story-marker="${i}" class="${i?'':'is-active'}"></span>`).join('')}</div></div>
        <div class="editorial-story-steps">${etapas.map(([titulo,valor,imagen,alt,descripcion],i)=>`<article class="editorial-story-step" data-story-step="${i}"><span class="editorial-step-index">${String(i+1).padStart(2,'0')} / CULTIVAMOS EN COMUNIDAD</span><h2>${titulo}</h2><strong class="${i===2?'editorial-story-money':''}">${valor}</strong><p>${descripcion}</p><span class="editorial-step-note">Datos del sistema</span></article>`).join('')}</div>
      </section>
      <div class="editorial-panel-tail"><p class="editorial-photo-credits"><a href="assets/images/ORIGEN.txt" target="_blank" rel="noopener">Fotografías de referencia · autores y licencias ↗</a></p><div id="panel-aviso"></div><section><div class="section-heading"><div><p class="eyebrow">PRÉSTAMOS Y ENTREGAS</p><h2>Últimos registros.</h2></div><span class="muted">${documentos.length} registros</span></div>${tabla(['Fecha','Documento','Persona','Sede','Monto',''],documentos.map(d=>`<tr><td>${fecha(d.fecha)}</td><td><strong>${escapar(d.ref)}</strong><small>${d.tipo}</small></td><td>${escapar(persona(d.personaId))}</td><td>${escapar(sede(d.sedeId))}</td><td class="amount">${d.valor}</td><td>${botonVer(d.modulo,d.id)}</td></tr>`))}</section><section><div class="section-heading"><div><p class="eyebrow">EL DÍA A DÍA</p><h2>Cada parte, en su lugar.</h2></div></div><div class="module-grid">${modulos.map(([id,n,t,d])=>`<button class="module-card" data-vista="${id}"><span class="module-number">${n} / ${nombres[id]}</span><h3>${t}</h3><p>${d}</p><span class="module-arrow" aria-hidden="true">↗</span></button>`).join('')}</div></section></div>
    </div>`;
  }

  function barraFiltros(opciones,total) {
    return `<div class="toolbar"><div class="tabs" role="group" aria-label="Filtrar registros">${opciones.map(([id,nombre]) => `<button class="tab ${estado.filtro === id ? 'active' : ''}" data-filtro="${id}" aria-pressed="${estado.filtro === id}">${nombre}</button>`).join('')}</div><span class="muted">${total} ${total === 1 ? 'registro' : 'registros'}</span></div>`;
  }
  function personas() {
    const lista = registros('personas').filter(p => estado.filtro === 'todos' || (estado.filtro === 'socios' && esSocio(p)) || (estado.filtro === 'empleados' && catalogo('tiposPersona',p.tipoPersonaId).toUpperCase() === 'EMPLEADO') || (estado.filtro === 'inactivos' && !activo(p)));
    return cabecera('01 / NUESTRA COMUNIDAD','Personas que hacen crecer.','Administra la información de socios y empleados.',nuevo('personas','Registrar persona')) + barraFiltros([['todos','Todas'],['socios','Socios'],['empleados','Empleados'],['inactivos','Inactivos']],lista.length) + tabla(['Persona','DNI / Cédula','Tipo','Sede','Estatus',''],lista.map(p => `<tr><td><div class="person-cell"><span class="person-avatar" aria-hidden="true">${escapar(p.nombre[0]+p.apellido[0])}</span><strong>${escapar(p.nombre+' '+p.apellido)}</strong></div></td><td class="code">${escapar(p.dni)}</td><td>${escapar(legible(catalogo('tiposPersona',p.tipoPersonaId)))}</td><td>${escapar(sede(p.sedeId))}</td><td>${insignia(p.estatusId)}</td><td>${botonVer('personas',p.id)}</td></tr>`));
  }
  function cuentas() {
    const lista = registros('cuentas').filter(c => estado.filtro === 'todos' || catalogo('tiposCuenta',c.tipoCuentaId).toLowerCase() === estado.filtro);
    const total = lista.reduce((s,c) => s + centavos(saldo(c.id)),0n);
    return cabecera('02 / AHORRO Y MOVIMIENTOS','Recursos con un propósito.','Cuentas de ahorro y monetarias, titulares e historial de saldos.',nuevo('cuentas','Abrir cuenta')) + `<div class="summary-card"><span class="eyebrow">SALDO EN LAS CUENTAS MOSTRADAS</span><strong>${estado.datos ? monedaCentavos(total) : 'Q —'}</strong><p>Último saldo registrado de cada cuenta.</p></div>` + barraFiltros([['todos','Todas'],['ahorro','Ahorro'],['monetaria','Monetarias']],lista.length) + tabla(['Cuenta','Tipo','Titulares','Sede','Saldo actual','Estatus',''],lista.map(c => `<tr><td class="code"><strong>${escapar(c.numero)}</strong></td><td>${escapar(legible(catalogo('tiposCuenta',c.tipoCuentaId)))}</td><td>${datos().cuentasPersonas.filter(cp => cp.cuentaId === c.id && cp.titular).map(cp => escapar(persona(cp.personaId))).join(', ') || '—'}</td><td>${escapar(sede(c.sedeId))}</td><td class="amount">${moneda(saldo(c.id))}</td><td>${insignia(c.estatusId)}</td><td>${botonVer('cuentas',c.id)}</td></tr>`));
  }
  function prestamos() {
    const lista = registros('prestamos').filter(p => estado.filtro === 'todos' || catalogo('tiposPrestamo',p.tipoPrestamoId).toLowerCase() === estado.filtro);
    const total = lista.filter(activo).reduce((s,p) => s + centavos(p.monto),0n);
    return cabecera('03 / FINANCIAMIENTO','El impulso para seguir.','Préstamos en efectivo, hipotecarios y de consumo de productos.',nuevo('prestamos','Registrar préstamo')) + `<div class="loan-overview"><span class="eyebrow">CARTERA ACTIVA · SELECCIÓN ACTUAL</span><div class="portfolio-value"${conteo(Number(total)/100,2,"Q ")}>${estado.datos ? monedaCentavos(total) : 'Q —'}</div><p class="muted">Saldo pendiente; los abonos reducen el monto registrado. La tasa es referencial.</p><div class="loan-distribution">${datos().catalogos.tiposPrestamo.map(t => {const valor = registros('prestamos',false).filter(p => p.tipoPrestamoId === t.id && activo(p)).reduce((s,p) => s + centavos(p.monto),0n);return `<button class="loan-type ${estado.filtro===t.nombre.toLowerCase()?'active':''}" data-filtro="${t.nombre.toLowerCase()}"><span>${escapar(legible(t.nombre))}</span><strong>${monedaCentavos(valor)}</strong><span aria-hidden="true">↗</span></button>`;}).join('')}</div></div>` + barraFiltros([['todos','Todos'],['efectivo','Efectivo'],['hipotecario','Hipotecario'],['consumo_productos','Productos']],lista.length) + tabla(['Referencia','Persona','Tipo','Saldo pendiente','Tasa','Vencimiento','Estatus',''],lista.map(p => `<tr><td class="code">PR-${String(p.id).padStart(4,'0')}</td><td>${escapar(persona(p.personaId))}</td><td>${escapar(legible(catalogo('tiposPrestamo',p.tipoPrestamoId)))}</td><td class="amount">${moneda(p.monto)}</td><td>${numero(p.tasaInteres)}%</td><td>${fecha(p.fechaVencimiento)}</td><td>${insignia(p.estatusId)}</td><td>${botonVer('prestamos',p.id)}</td></tr>`));
  }
  function productos() {
    const lista = registros('productos').filter(p => estado.filtro === 'todos' || (estado.filtro === 'bajo' ? p.stock<=5 : String(p.categoriaId) === estado.filtro));
    return cabecera('04 / CATÁLOGO AGRÍCOLA','Lo que la tierra necesita.','Productos, categorías, precios y existencias compartidos entre sedes.',nuevo('productos','Registrar producto')) + barraFiltros([['todos','Todos'],...datos().catalogos.categoriasProducto.map(c => [String(c.id),legible(c.nombre)]),['bajo','Stock ≤ 5']],lista.length) + tabla(['Producto','Categoría','Precio unitario','Existencias',''],lista.map(p => `<tr><td><strong>${escapar(p.nombre)}</strong><small>${escapar(p.descripcion)}</small></td><td>${escapar(legible(catalogo('categoriasProducto',p.categoriaId)))}</td><td class="amount">${moneda(p.precio)}</td><td><span class="badge ${p.stock<=5?'badge-warning':'badge-success'}">${numero(p.stock)} unidades</span></td><td>${botonVer('productos',p.id)}</td></tr>`));
  }
  function albaranes() {
    const lista = registros('albaranes');
    return cabecera('05 / RECEPCIÓN Y CALIDAD','El valor de cada cosecha.','Registra lo aceptado y lo rechazado en el mismo albarán.',nuevo('albaranes','Registrar entrega')) + `<div class="harvest-note"><span class="harvest-mark" aria-hidden="true">✳</span><div><h2>De la entrega al valor justo.</h2><p>El total se calcula únicamente sobre la cantidad aceptada de cada producto.</p></div></div>` + `<div class="toolbar"><span class="eyebrow">TALONARIOS DE ENTREGA</span><span class="muted">${lista.length} registros</span></div>` + tabla(['Talonario','Fecha','Socio','Sede','Aceptado','Rechazado','Total a pagar',''],lista.map(a => `<tr><td class="code"><strong>${escapar(a.numeroTalonario)}</strong></td><td>${fecha(a.fecha)}</td><td>${escapar(persona(a.personaId))}</td><td>${escapar(sede(a.sedeId))}</td><td>${numero(a.detalles.reduce((s,d)=>s+Number(d.cantidadAceptada),0))} kg</td><td>${numero(a.detalles.reduce((s,d)=>s+Number(d.cantidadRechazada),0))} kg</td><td class="amount">${monedaCentavos(totalAlbaran(a))}</td><td>${botonVer('albaranes',a.id,'Ver albarán')}</td></tr>`));
  }
  // --- Respaldo en caliente (Fase IV): estado y disparo desde la interfaz ---
  // Tablas en el mismo orden del contrato Java (EsquemaRespaldo), con etiqueta legible.
  const TABLAS_RESPALDO = [
    ['sedes','Sedes'],['estatus','Estatus'],['tipos_persona','Tipos de persona'],
    ['tipos_cuenta','Tipos de cuenta'],['tipos_prestamo','Tipos de préstamo'],
    ['categorias_producto','Categorías de producto'],['personas','Personas'],
    ['productos','Productos'],['cuentas','Cuentas'],['cuentas_personas','Titulares de cuenta'],
    ['saldos','Saldos'],['prestamos','Préstamos'],['detalles_prestamo','Detalles de préstamo'],
    ['albaranes','Albaranes'],['detalles_albaran','Detalles de albarán']
  ];
  let respaldoEstado = null;
  let respaldoError = '';
  let respaldoConsultando = false;
  let respaldoSondeo = null;
  let respaldoSolicitado = false;   // true solo si el respaldo lo pidió esta sesión (para avisar de su fin)
  let respaldoAvisado = null;       // fecha del último respaldo ya anunciado (evita repetir el aviso)
  let respaldoErrorAvisado = null;  // último error de conexión mostrado (evita repetir el aviso)
  const tieneConteo = (conteos,id) => Object.prototype.hasOwnProperty.call(conteos,id);

  function respaldo() {
    const encabezado = cabecera('06 / MANTENIMIENTO','Respaldo en caliente.','Copia consistente de Oracle a PostgreSQL mientras la cooperativa sigue operando.');
    if (!respaldoEstado) {
      const mensaje = respaldoError || 'El sistema consultará automáticamente el estado del respaldo.';
      const boton = `<button class="btn btn-primary" type="button" data-respaldar ${respaldoConsultando ? 'disabled' : ''}><span aria-hidden="true">↻</span> Respaldar ahora</button>`;
      return encabezado
        + `<div class="empty-state"><span class="empty-symbol" aria-hidden="true">◍</span><h3>Sin información del respaldo</h3><p>${escapar(mensaje)}</p></div>`
        + `<div class="action-row">${boton}</div>`
        + '<p class="muted">Aunque todavía no haya una ejecución registrada, puedes lanzar el respaldo: el sistema avisará al terminar.</p>';
    }
    const e = respaldoEstado;
    const conteos = e.conteos || {};
    const verificadas = TABLAS_RESPALDO.filter(([id]) => tieneConteo(conteos,id)).length;
    const total = TABLAS_RESPALDO.reduce((suma,[id]) => suma + (tieneConteo(conteos,id) ? Number(conteos[id]) : 0),0);
    const aviso = e.enCurso
      ? '<p class="notice">El respaldo está en curso. Puedes seguir trabajando; aquí verás los conteos verificados al terminar.</p>'
      : (e.fecha && !e.exitoso ? `<p class="notice">${escapar(e.mensaje || 'El último respaldo no se completó.')}</p>` : '');
    const insigniaEstado = e.enCurso ? '<span class="badge badge-warning">En curso</span>'
      : (e.fecha ? (e.exitoso ? '<span class="badge badge-success">Completado</span>' : '<span class="badge badge-neutral">Con error</span>')
                 : '<span class="badge badge-neutral">Sin ejecutar</span>');
    const boton = `<button class="btn btn-primary" type="button" data-respaldar ${(e.enCurso || respaldoConsultando) ? 'disabled' : ''}><span aria-hidden="true">↻</span> ${e.enCurso ? 'Respaldando…' : 'Respaldar ahora'}</button>`;
    const filas = TABLAS_RESPALDO.map(([id,etiqueta]) => `<tr><td>${escapar(etiqueta)}</td><td class="code">${escapar(id)}</td><td class="amount">${tieneConteo(conteos,id) ? numero(conteos[id]) : '—'}</td></tr>`);
    return encabezado + aviso
      + `<div class="summary-card"><span class="eyebrow">ÚLTIMA EJECUCIÓN</span><strong>${e.fecha ? escapar(e.fecha) : '—'}</strong>${insigniaEstado}<p>${e.postgresAlcanzable ? 'La base de respaldo en PostgreSQL está disponible.' : 'No se pudo contactar PostgreSQL. Revisa config.properties y que el servicio esté activo.'}</p><p class="muted">${verificadas} de 15 tablas verificadas · ${numero(total)} filas copiadas.</p></div>`
      + tabla(['Tabla','Nombre técnico','Filas copiadas'],filas)
      + `<div class="action-row">${boton}</div>`
      + '<p class="muted">La recuperación (PostgreSQL → Oracle) es una operación de mantenimiento: requiere la API detenida y un esquema Oracle nuevo y vacío, por eso no se ejecuta desde esta pantalla. Ver <code>scripts/README.md</code>.</p>';
  }

  function programarSondeo() {
    clearTimeout(respaldoSondeo);
    if (!respaldoEstado || !respaldoEstado.enCurso) return;
    // Se sondea aunque el usuario cambie de módulo: así el aviso de finalización llega igual.
    respaldoSondeo = setTimeout(cargarRespaldo,2500);
  }

  async function cargarRespaldo() {
    if (respaldoConsultando) return;
    respaldoConsultando = true;
    try { respaldoEstado = await window.ServicioCooperativa.estadoRespaldo(); respaldoError = ''; respaldoErrorAvisado = null; }
    catch (error) { respaldoEstado = null; respaldoError = error.message; }
    finally { respaldoConsultando = false; }
    // Si el estado no se pudo consultar (p. ej. la API está detenida), avísalo una vez.
    if (respaldoError && respaldoError !== respaldoErrorAvisado) {
      respaldoErrorAvisado = respaldoError;
      anunciar(respaldoError,8000);
    }
    avisarRespaldo();
    if (estado.vista === 'respaldo') renderizar({animar:false});
    programarSondeo();
  }

  // Anuncia el fin del respaldo una sola vez, cuando pasa de "en curso" a terminado.
  function avisarRespaldo() {
    const e = respaldoEstado;
    if (!e || e.enCurso || !e.fecha) return;
    if (e.fecha === respaldoAvisado) return;
    respaldoAvisado = e.fecha;                  // el estado ya visto no se vuelve a anunciar
    if (!respaldoSolicitado) return;            // respaldos anteriores a esta sesión no generan aviso
    respaldoSolicitado = false;
    const conteos = e.conteos || {};
    if (e.exitoso) {
      const verificadas = TABLAS_RESPALDO.filter(([id]) => tieneConteo(conteos,id)).length;
      const total = TABLAS_RESPALDO.reduce((suma,[id]) => suma + (tieneConteo(conteos,id) ? Number(conteos[id]) : 0),0);
      anunciar(`Migración completada correctamente: ${verificadas} de 15 tablas y ${numero(total)} filas copiadas en PostgreSQL.`,9000);
    } else {
      anunciar(`La migración no se pudo completar: ${e.mensaje || 'revisa la vista de Respaldo.'}`,9000);
    }
  }

  async function ejecutarRespaldo() {
    if (respaldoConsultando || (respaldoEstado && respaldoEstado.enCurso)) return;
    respaldoConsultando = true;
    try {
      respaldoEstado = await window.ServicioCooperativa.ejecutarRespaldo();
      respaldoSolicitado = true;
      anunciar('Respaldo iniciado. Puedes seguir usando el sistema mientras se copia la base de datos.');
    } catch (error) {
      anunciar(error.status === 409 ? 'Ya hay un respaldo en ejecución.' : error.message);
    } finally { respaldoConsultando = false; }
    await cargarRespaldo();
  }

  const vistas = {panel,personas,cuentas,prestamos,productos,albaranes,respaldo};
  let versionCarga = 0;
  function actualizarAviso(mensaje) {
    const online=estado.modo==='conectado';
    const titulo=$('#titulo-conexion');
    if(titulo) titulo.textContent=estado.cargando?'Conectando…':online?'Sistema conectado':'Sin conexión al sistema';
    const texto=$('#texto-conexion');
    if(texto) texto.textContent=mensaje || (online?'Datos cargados desde la base de datos de la cooperativa.':'No se pudo conectar con la base de datos. Revisa que la API esté iniciada en el puerto 8081.');
    $('#recargar').disabled=estado.cargando;
    $('#aviso-conexion').classList.toggle('conectado',online);
    const envio=$('#dialogo [type="submit"]');
    if(envio && !estado.guardando) envio.disabled=!online||estado.cargando||Boolean(envio.closest('form').dataset.bloqueado);
  }
  async function conectar() {
    if(estado.cargando||estado.guardando) return false;
    const version=++versionCarga;
    estado.cargando=true;
    actualizarAviso('Consultando los registros de la cooperativa…');
    try {
      const cargados=await window.ServicioCooperativa.cargar();
      if(version!==versionCarga)return false;
      estado.datos=cargados;estado.modo='conectado';
      $('#selector-sede').innerHTML='<option value="">Todas las sedes</option>'+cargados.catalogos.sedes.map(s=>`<option value="${s.id}">${escapar(legible(s.nombre))}</option>`).join('');
      if(!cargados.catalogos.sedes.some(s=>String(s.id)===estado.sede))estado.sede='';
      $('#selector-sede').value=estado.sede;
      estado.cargando=false;actualizarAviso();renderizar();return true;
    } catch(error) {
      if(version!==versionCarga)return false;
      estado.cargando=false;
      estado.modo='desconectado';
      actualizarAviso(`${error.message}${estado.datos?' Los datos visibles pertenecen a la última consulta; el guardado está deshabilitado.':''}`);
      renderizar();return false;
    }
  }
  function renderizar({animar=true}={}) {
    document.dispatchEvent(new CustomEvent('tierra:antes-render'));
    const avisoConexion = $('#aviso-conexion');
    // El mismo aviso se conserva al reemplazar el panel; los demás módulos
    // mantienen su ubicación original, antes del contenido administrativo.
    if (avisoConexion) $('#contenido').before(avisoConexion);
    document.body.classList.toggle('panel-editorial',estado.vista === 'panel');
    $('#titulo-seccion').textContent = nombres[estado.vista];
    $('#contenido').innerHTML = vistas[estado.vista]();
    $('#contenido').dataset.vista = estado.vista;
    if (estado.vista === 'panel' && avisoConexion) $('#panel-aviso').append(avisoConexion);
    $$('[data-vista]').forEach(b => {const actual = b.dataset.vista === estado.vista; b.classList.toggle('active',actual); if (actual) b.setAttribute('aria-current','page'); else b.removeAttribute('aria-current');});
    document.title = $('#aplicacion').hidden ? 'Tierra Fértil · Cooperativa Agrícola' : `${nombres[estado.vista]} · Tierra Fértil`;
    document.dispatchEvent(new CustomEvent('tierra:render',{detail:{animar}}));
  }
  function navegar(vista) {
    if (!vistas[vista]) return;
    if (!(respaldoEstado && respaldoEstado.enCurso)) clearTimeout(respaldoSondeo);
    estado.vista = vista; estado.filtro = 'todos'; estado.busqueda = ''; $('#busqueda-global').value = '';
    renderizar();
    if (vista === 'respaldo') cargarRespaldo();
    if (window.MovimientoTierra) window.MovimientoTierra.irAlInicio();
    else window.scrollTo({top:0,behavior:'instant'});
    $('#contenido').focus({preventScroll:true});
  }
  let temporizador;
  function anunciar(mensaje,duracion=6000) {clearTimeout(temporizador); $('#notificacion').textContent=mensaje; $('#notificacion').classList.add('visible'); temporizador=setTimeout(()=>$('#notificacion').classList.remove('visible'),duracion);}
  function abrirDialogo(titulo,contenido) {$('#titulo-dialogo').textContent=titulo; $('#cuerpo-dialogo').innerHTML=contenido; $('#dialogo').showModal();document.dispatchEvent(new CustomEvent('tierra:dialogo'));}
  const avisoFormulario = () => estado.modo === 'conectado' ? '<p class="muted">Los cambios se registrarán en la base de datos de la cooperativa.</p>' : '<p class="notice">El sistema aún no está conectado. Espera a que termine la consulta para guardar.</p>';
  function verDetalle(tipo,id) {
    const item = buscar(datos()[tipo],id); if (!item) return;
    let contenido = '';
    if (tipo === 'personas') contenido = `<div class="detail-grid">${detalle('Nombre',item.nombre+' '+item.apellido)}${detalle('DNI / Cédula',item.dni)}${detalle('Tipo',legible(catalogo('tiposPersona',item.tipoPersonaId)))}${detalle('Sede',sede(item.sedeId))}${detalle('Dirección',item.direccion || 'Sin dirección')}${detalle('Teléfono',item.telefono || 'Sin teléfono')}${detalle('Registro',fecha(item.fechaRegistro))}${detalle('Estatus',legible(catalogo('estatus',item.estatusId)))}</div><div class="action-row"><button class="btn btn-secondary" data-editar="personas" data-id="${item.id}">Ver formulario de edición</button><button class="btn btn-text" data-operacion="estatus-persona" data-id="${item.id}">Cambiar estatus</button></div>`;
    if (tipo === 'productos') contenido = `<div class="detail-grid">${detalle('Producto',item.nombre)}${detalle('Descripción',item.descripcion)}${detalle('Categoría',legible(catalogo('categoriasProducto',item.categoriaId)))}${detalle('Precio',moneda(item.precio))}${detalle('Existencias',numero(item.stock)+' unidades')}</div><div class="action-row"><button class="btn btn-secondary" data-editar="productos" data-id="${item.id}">Ver formulario de edición</button><button class="btn btn-text" data-operacion="stock" data-id="${item.id}">Agregar existencias</button></div>`;
    if (tipo === 'cuentas') {
      const titulares=datos().cuentasPersonas.filter(cp=>cp.cuentaId===item.id);
      const historial=datos().saldos.filter(s=>s.cuentaId===item.id).sort((a,b)=>new Date(b.fecha)-new Date(a.fecha)||b.id-a.id);
      contenido=`<div class="detail-grid">${detalle('Número',item.numero)}${detalle('Saldo actual',moneda(saldo(item.id)))}${detalle('Tipo',legible(catalogo('tiposCuenta',item.tipoCuentaId)))}${detalle('Sede',sede(item.sedeId))}${detalle('Apertura',fecha(item.fechaApertura))}${detalle('Estatus',legible(catalogo('estatus',item.estatusId)))}</div><h3>Personas asociadas</h3>${tabla(['Persona','Relación'],titulares.map(cp=>`<tr><td>${escapar(persona(cp.personaId))}</td><td>${cp.titular?'Titular':'Asociado'}</td></tr>`))}<h3>Historial de saldos</h3><p class="muted">Cada fila muestra el saldo registrado en esa fecha.</p>${tabla(['Fecha','Saldo registrado'],historial.map(s=>`<tr><td>${fecha(s.fecha)}</td><td class="amount">${moneda(s.monto)}</td></tr>`))}<div class="action-row"><button class="btn btn-primary" data-operacion="deposito" data-id="${item.id}">Depositar</button><button class="btn btn-secondary" data-operacion="retiro" data-id="${item.id}">Retirar</button><button class="btn btn-text" data-operacion="asociar" data-id="${item.id}">Asociar persona</button></div>`;
    }
    if (tipo === 'prestamos') contenido=`<div class="detail-grid">${detalle('Persona',persona(item.personaId))}${detalle('Tipo',legible(catalogo('tiposPrestamo',item.tipoPrestamoId)))}${detalle('Saldo pendiente',moneda(item.monto))}${detalle('Tasa de interés (referencial)',numero(item.tasaInteres)+'%')}${detalle('Fecha',fecha(item.fecha))}${detalle('Vencimiento',fecha(item.fechaVencimiento))}${detalle('Estatus',legible(catalogo('estatus',item.estatusId)))}</div>${item.detalles?.length?`<h3>Productos entregados</h3>${tabla(['Producto','Unidades','Precio histórico','Subtotal'],item.detalles.map(d=>`<tr><td>${escapar(producto(d.productoId))}</td><td>${numero(d.cantidad)}</td><td>${moneda(d.precioUnitario)}</td><td class="amount">${monedaCentavos(BigInt(d.cantidad)*centavos(d.precioUnitario))}</td></tr>`))}`:''}<div class="action-row"><button class="btn btn-primary" data-operacion="abono" data-id="${item.id}">Registrar abono</button><button class="btn btn-secondary" data-operacion="estatus-prestamo" data-id="${item.id}">Cambiar estatus</button></div>`;
    if (tipo === 'albaranes') contenido=`<article class="document"><header class="document-header"><div><span class="document-brand">Tierra Fértil</span><p class="eyebrow">COOPERATIVA AGRÍCOLA</p></div><div><span class="eyebrow">ALBARÁN DE ENTREGA</span><h3>${escapar(item.numeroTalonario)}</h3><p>${fecha(item.fecha)}</p></div></header><div class="detail-grid">${detalle('Socio',persona(item.personaId))}${detalle('DNI / Cédula',buscar(datos().personas,item.personaId)?.dni || '—')}${detalle('Sede de recepción',sede(item.sedeId))}${detalle('Estado de liquidación',item.liquidado?'Liquidado':'Pendiente')}</div>${tabla(['Producto','Aceptado (kg)','Rechazado (kg)','Precio / kg','Valor'],item.detalles.map(d=>`<tr><td>${escapar(producto(d.productoId))}</td><td>${numero(d.cantidadAceptada)}</td><td>${numero(d.cantidadRechazada)}</td><td>${moneda(d.precioUnitario)}</td><td class="amount">${monedaCentavos((centavos(d.cantidadAceptada)*centavos(d.precioUnitario)+50n)/100n)}</td></tr>`))}<div class="document-total"><span>Total a pagar por producto aceptado</span><strong>${monedaCentavos(totalAlbaran(item))}</strong></div><p class="document-caption">DETALLE DE LA ENTREGA REGISTRADA</p></article><div class="action-row">${item.liquidado ? '<p class="muted">Esta entrega ya fue liquidada.</p>' : `<button class="btn btn-primary" data-operacion="liquidar" data-id="${item.id}">Abonar a cuenta del socio</button>`}</div>`;
    abrirDialogo(tipo==='albaranes'?'Detalle de la entrega':`Detalle · ${nombres[tipo]}`,contenido);
  }
  function campo(nombre,etiqueta,tipo='text',atributos='',valor='') {return `<label class="field"><span>${etiqueta}</span><input name="${nombre}" type="${tipo}" ${atributos} value="${escapar(valor)}"></label>`;}
  function selector(nombre,etiqueta,opciones,seleccion='',requerido=true) {return `<label class="field"><span>${etiqueta}</span><select name="${nombre}" ${requerido?'required':''}><option value="">Seleccionar…</option>${opciones.map(o=>`<option value="${o.id}" ${String(o.id)===String(seleccion)?'selected':''}>${escapar(o.nombre)}</option>`).join('')}</select></label>`;}
  const opcionesPersonas = (soloSocios=false) => datos().personas.filter(p=>activo(p)&&(!soloSocios||esSocio(p))).map(p=>({id:p.id,nombre:p.nombre+' '+p.apellido}));
  const opcionesCatalogo = tipo => datos().catalogos[tipo].map(c=>({id:c.id,nombre:legible(c.nombre)}));
  const accionesFormulario = () => `<p id="error-formulario" class="field-error" role="alert"></p><div class="form-actions"><button class="btn btn-secondary" type="button" data-cerrar>Cancelar</button><button class="btn btn-primary" type="submit" ${estado.modo==='conectado' && !estado.cargando ? '' : 'disabled'}>Guardar</button></div>`;
  function formulario(tipo,id) {
    const item = buscar(datos()[tipo],id) || {};
    let campos='';
    if (tipo==='personas') campos=campo('nombre','Nombre','text','required maxlength="60"',item.nombre)+campo('apellido','Apellido','text','required maxlength="60"',item.apellido)+campo('dni','DNI / Cédula','text','required maxlength="20"',item.dni)+campo('telefono','Teléfono','tel','maxlength="20"',item.telefono)+campo('direccion','Dirección','text','maxlength="150"',item.direccion)+selector('tipoPersonaId','Tipo de persona',opcionesCatalogo('tiposPersona'),item.tipoPersonaId)+selector('sedeId','Sede',opcionesCatalogo('sedes'),item.sedeId)+selector('estatusId','Estatus',opcionesCatalogo('estatus'),item.estatusId);
    if (tipo==='productos') campos=campo('nombre','Nombre del producto','text','required maxlength="80"',item.nombre)+selector('categoriaId','Categoría',opcionesCatalogo('categoriasProducto'),item.categoriaId)+campo('descripcion','Descripción','text','maxlength="200"',item.descripcion)+campo('precio','Precio unitario (Q)','number','required min="0.01" step="0.01"',item.precio)+(id?'<p class="muted">Existencias actuales: '+numero(item.stock??0)+' unidades. Se administran con “Agregar existencias”; esta edición no las modifica.</p>':campo('stock','Existencias (unidades)','number','required min="0" step="1"',item.stock ?? 0));
    if (tipo==='cuentas') campos=campo('numero','Número de cuenta (opcional)','text','maxlength="30"')+selector('tipoCuentaId','Tipo de cuenta',opcionesCatalogo('tiposCuenta'))+selector('sedeId','Sede',opcionesCatalogo('sedes'))+campo('saldoInicial','Saldo inicial (Q)','number','required min="0" step="0.01"','0.00');
    if (tipo==='prestamos') campos=selector('personaId','Persona activa',opcionesPersonas())+selector('tipoPrestamoId','Tipo de préstamo',opcionesCatalogo('tiposPrestamo'))+campo('monto','Monto (Q)','number','required min="0.01" step="0.01"')+campo('tasaInteres','Tasa de interés (%) — referencial','number','required min="0" step="0.01"','0.00')+campo('fechaVencimiento','Fecha de vencimiento (opcional)','date',`min="${fechaManana()}"`)+'<p class="muted">El monto es la deuda a pagar y los abonos la reducen. La tasa queda registrada como referencia y no se suma sola a la deuda; los préstamos no mueven el saldo de las cuentas.</p>';
    if (tipo==='albaranes') campos=campo('numeroTalonario','Número de talonario','text','required maxlength="30"')+selector('personaId','Socio que entrega',opcionesPersonas(true))+selector('sedeId','Sede de recepción',opcionesCatalogo('sedes'));
    const lineas=['prestamos','albaranes','cuentas'].includes(tipo)?`<section id="seccion-lineas" ${tipo==='prestamos'?'hidden':''}><h3>${tipo==='cuentas'?'Titulares y asociados':'Detalle de productos'}</h3><p class="muted">${tipo==='cuentas'?'La cuenta necesita al menos una persona titular.':tipo==='prestamos'?'El precio de los insumos proviene del catálogo.':'Registra los kilogramos aceptados, rechazados y su precio.'}</p><div id="lineas-formulario"></div><button class="btn btn-secondary" type="button" data-agregar-linea="${tipo}">＋ ${tipo==='cuentas'?'Agregar persona':'Agregar producto'}</button>${tipo==='albaranes'?'<div class="document-total"><span>Total por producto aceptado</span><strong id="total-entrega">Q 0.00</strong></div>':''}</section>`:'';
    abrirDialogo(`${id?'Editar':'Registrar'} · ${nombres[tipo]}`,`${avisoFormulario()}<form id="formulario-registro" data-tipo="${tipo}" data-id="${id || ''}"><div class="form-grid">${campos}</div>${lineas}${accionesFormulario()}</form>`);
    if (tipo==='cuentas'||tipo==='albaranes') agregarLinea(tipo);
  }
  let secuenciaLinea=0;
  function agregarLinea(tipo) {
    const n=++secuenciaLinea;
    let contenido='';
    if (tipo==='cuentas') contenido=selector(`persona-${n}`,'Persona activa',opcionesPersonas())+`<label class="checkbox-field"><input name="titular-${n}" type="checkbox" ${$('#lineas-formulario').children.length?'':'checked'}> Titular de la cuenta</label>`;
    else contenido=selector(`producto-${n}`,'Producto',datos().productos.map(p=>({id:p.id,nombre:p.nombre})))+(tipo==='prestamos'?campo(`cantidad-${n}`,'Cantidad (unidades)','number','required min="1" step="1"','1'):`${campo(`aceptada-${n}`,'Aceptado (kg)','number','required min="0" step="0.01"','0.00')}${campo(`rechazada-${n}`,'Rechazado (kg)','number','required min="0" step="0.01"','0.00')}${campo(`precio-${n}`,'Precio por kg (Q)','number','required min="0.01" step="0.01"','0.00')}`);
    $('#lineas-formulario').insertAdjacentHTML('beforeend',`<div class="form-line" data-linea="${n}"><div class="form-grid">${contenido}</div><button type="button" class="btn btn-text" data-quitar-linea="${n}" aria-label="Quitar línea ${n}">Quitar</button></div>`);
  }
  function actualizarTotal() {
    const salida=$('#total-entrega'); if (!salida) return;
    try {const bruto=$$('[data-linea]').reduce((s,f)=>s+centavos($('input[name^="aceptada-"]',f).value||0)*centavos($('input[name^="precio-"]',f).value||0),0n);salida.textContent=monedaCentavos((bruto+50n)/100n);} catch (_) {salida.textContent='Revisa las cantidades';}
  }
  function operacion(nombre,id) {
    const titulos={deposito:'Depósito en cuenta',retiro:'Retiro de cuenta',abono:'Abono a préstamo',asociar:'Asociar persona',stock:'Agregar existencias','estatus-persona':'Cambiar estatus de persona','estatus-prestamo':'Cambiar estatus de préstamo',liquidar:'Abonar albarán a cuenta'};
    let campos='';
    if (['deposito','retiro','abono'].includes(nombre)) campos=campo('monto','Monto (Q)','number','required min="0.01" step="0.01"');
    if (nombre==='stock') campos=campo('cantidad','Cantidad a agregar','number','required min="1" step="1"');
    if (nombre.startsWith('estatus-')) campos=selector('estatusId','Estatus',opcionesCatalogo('estatus'));
    if (nombre==='asociar') campos=selector('personaId','Persona activa',opcionesPersonas())+'<label class="checkbox-field"><input type="checkbox" name="titular"> Titular</label>';
    if (nombre==='liquidar') {const albaran=buscar(datos().albaranes,id); if(albaran.liquidado){abrirDialogo('Entrega ya liquidada','<p>Esta entrega ya fue abonada. No se volverá a liquidar.</p>');return;} const ids=datos().cuentasPersonas.filter(cp=>cp.personaId===albaran.personaId).map(cp=>cp.cuentaId);campos=`<p>Total de la entrega: <strong>${monedaCentavos(totalAlbaran(albaran))}</strong></p>`+selector('cuentaId','Cuenta del socio',datos().cuentas.filter(c=>ids.includes(c.id)).map(c=>({id:c.id,nombre:c.numero})));}
    const advertencia = nombre === 'liquidar' ? '<label class="checkbox-field"><input type="checkbox" name="confirmarLiquidacion" required> Confirmo abonar esta entrega a la cuenta seleccionada.</label>' : '';
    abrirDialogo(titulos[nombre],`${avisoFormulario()}<form data-operacion="${nombre}" data-id="${id}"><div class="form-grid">${campos}</div>${advertencia}${accionesFormulario()}</form>`);
  }
  function datosFormulario(form) {
    const tipo=form.dataset.tipo;
    const valores=Object.fromEntries(new FormData(form));
    const entero=(v,nombre)=>{const n=Number(v);if(!Number.isSafeInteger(n)||n<0)throw new Error(`Revisa ${nombre}.`);return n;};
    const decimal=(v,nombre,positivo=false)=>{const n=centavos(v);if(positivo&&n<=0n)throw new Error(`${nombre} debe ser mayor que cero.`);return String(v);};
    if(tipo==='personas') return {nombre:valores.nombre.trim(),apellido:valores.apellido.trim(),dni:valores.dni.trim(),telefono:valores.telefono.trim(),direccion:valores.direccion.trim(),tipoPersonaId:entero(valores.tipoPersonaId,'tipo de persona'),sedeId:entero(valores.sedeId,'sede'),...(valores.estatusId?{estatusId:entero(valores.estatusId,'estatus')}:{})};
    if(tipo==='productos'){const producto={nombre:valores.nombre.trim(),descripcion:valores.descripcion.trim(),categoriaId:entero(valores.categoriaId,'categoría'),precio:decimal(valores.precio,'El precio',true)};if(!form.dataset.id)producto.stock=entero(valores.stock,'las existencias');return producto;}
    if(tipo==='cuentas') {
      const personas=$$('[data-linea]',form).map(f=>({personaId:entero($('select',f).value,'persona'),titular:$('input[type="checkbox"]',f).checked}));
      if(!personas.length||!personas.some(p=>p.titular))throw new Error('Agrega al menos una persona titular.');
      if(new Set(personas.map(p=>p.personaId)).size!==personas.length)throw new Error('Una persona no puede repetirse en la cuenta.');
      return {cuenta:{numero:valores.numero.trim(),tipoCuentaId:entero(valores.tipoCuentaId,'tipo de cuenta'),sedeId:entero(valores.sedeId,'sede')},personas,saldoInicial:decimal(valores.saldoInicial,'El saldo inicial')};
    }
    if(tipo==='prestamos') {
      const consumo=catalogo('tiposPrestamo',valores.tipoPrestamoId)==='CONSUMO_PRODUCTOS';
      const detalles=consumo?$$('[data-linea]',form).map(f=>{const cantidad=entero($('input[name^="cantidad-"]',f).value,'la cantidad');if(cantidad<1)throw new Error('La cantidad debe ser mayor que cero.');return {productoId:entero($('select',f).value,'producto'),cantidad};}):[];
      if(consumo&&!detalles.length)throw new Error('Agrega al menos un producto al préstamo.');
      let vencimiento=null;
      if(valores.fechaVencimiento){if(valores.fechaVencimiento < fechaManana())throw new Error('El vencimiento debe ser a partir de mañana.');vencimiento=new Date(`${valores.fechaVencimiento}T12:00:00-06:00`);}
      return {personaId:entero(valores.personaId,'persona'),tipoPrestamoId:entero(valores.tipoPrestamoId,'tipo de préstamo'),monto:consumo?'0':decimal(valores.monto,'El monto',true),tasaInteres:decimal(valores.tasaInteres,'La tasa'),fechaVencimiento:vencimiento?.toISOString() || null,detalles};
    }
    if(tipo==='albaranes') {
      const detalles=$$('[data-linea]',form).map(f=>{const aceptada=$('input[name^="aceptada-"]',f).value,rechazada=$('input[name^="rechazada-"]',f).value;if(centavos(aceptada)+centavos(rechazada)<=0n)throw new Error('Cada producto debe tener cantidad aceptada o rechazada.');return {productoId:entero($('select',f).value,'producto'),cantidadAceptada:decimal(aceptada,'La cantidad aceptada'),cantidadRechazada:decimal(rechazada,'La cantidad rechazada'),precioUnitario:decimal($('input[name^="precio-"]',f).value,'El precio',true)};});
      if(!detalles.length)throw new Error('Agrega al menos un producto a la entrega.');
      return {numeroTalonario:valores.numeroTalonario.trim(),personaId:entero(valores.personaId,'socio'),sedeId:entero(valores.sedeId,'sede'),detalles};
    }
    throw new Error('Formulario no reconocido.');
  }
  document.addEventListener('submit',async evento=>{
    if(!evento.target.closest('#dialogo'))return;
    evento.preventDefault();
    if(estado.modo!=='conectado'||estado.guardando||estado.cargando||evento.target.dataset.bloqueado)return;
    const form=evento.target;
    if(!form.reportValidity())return;
    const errorVisible=$('#error-formulario');errorVisible.textContent='';
    let carga;
    try{carga=form.dataset.tipo?datosFormulario(form):Object.fromEntries(new FormData(form));if(form.dataset.operacion==='asociar')carga.titular=!!form.elements.titular.checked;}
    catch(error){errorVisible.textContent=error.message;return;}
    estado.guardando=true;
    const botones=$$('button', $('#dialogo'));botones.forEach(b=>b.disabled=true);
    const enviar=$('[type="submit"]',form);enviar.textContent='Guardando…';
    try {
      if(form.dataset.tipo)await window.ServicioCooperativa.guardar(form.dataset.tipo,carga,form.dataset.id || undefined);
      else await window.ServicioCooperativa.operar(form.dataset.operacion,Number(form.dataset.id),carga);
      estado.guardando=false;$('#cerrar-dialogo').disabled=false;$('#dialogo').close();
      anunciar('Operación confirmada por el sistema.');
      const actualizado=await conectar();
      if(!actualizado)anunciar('La operación se guardó, pero no se pudo actualizar la vista. Recarga antes de continuar.');
    } catch(error) {
      errorVisible.textContent=error.message;
      estado.guardando=false;botones.forEach(b=>b.disabled=false);enviar.textContent='Guardar';
      if(error.estadoIncierto){
        form.dataset.bloqueado='true';estado.modo='desconectado';enviar.disabled=true;
        errorVisible.textContent+=' Cierra este formulario y actualiza los registros antes de iniciar otra operación.';
        actualizarAviso('El resultado de la última operación no se pudo confirmar. Actualiza los registros antes de continuar.');
      }
    }
  });
  document.addEventListener('click',evento=>{
    const b=evento.target.closest('button'); if (!b) return;
    if (estado.guardando) return;
    if(estado.cargando && (b.dataset.nuevo || b.dataset.editar || b.dataset.operacion)){anunciar('Espera a que termine la consulta para abrir el formulario.');return;}
    if (b.dataset.vista) navegar(b.dataset.vista);
    if (b.hasAttribute('data-respaldar')) ejecutarRespaldo();
    if (b.dataset.filtro) {estado.filtro=b.dataset.filtro;renderizar({animar:false});}
    if (b.hasAttribute('data-limpiar')) {estado.filtro='todos';estado.busqueda='';estado.sede='';$('#busqueda-global').value='';$('#selector-sede').value='';renderizar({animar:false});}
    if (b.dataset.detalle) verDetalle(b.dataset.detalle,b.dataset.id);
    if (b.dataset.nuevo) formulario(b.dataset.nuevo);
    if (b.dataset.editar) formulario(b.dataset.editar,b.dataset.id);
    if (b.dataset.operacion) operacion(b.dataset.operacion,b.dataset.id);
    if (b.dataset.agregarLinea) agregarLinea(b.dataset.agregarLinea);
    if (b.dataset.quitarLinea) {b.closest('[data-linea]').remove();actualizarTotal();}
    if (b.hasAttribute('data-cerrar')) $('#dialogo').close();
  });
  document.addEventListener('change',evento=>{
    if (evento.target.name==='tipoPrestamoId') {const consumo=catalogo('tiposPrestamo',evento.target.value)==='CONSUMO_PRODUCTOS';$('#seccion-lineas').hidden=!consumo;const monto=$('[name="monto"]');monto.disabled=consumo;monto.required=!consumo;if (consumo&&!$('#lineas-formulario').children.length) agregarLinea('prestamos');$$('input,select', $('#seccion-lineas')).forEach(c=>c.disabled=!consumo);}
    if (evento.target.name?.startsWith('producto-') && $('#total-entrega')) {const fila=evento.target.closest('[data-linea]');$('input[name^="precio-"]',fila).value=buscar(datos().productos,evento.target.value)?.precio || '0.00';actualizarTotal();}
  });
  document.addEventListener('input',evento=>{if(evento.target.closest('#lineas-formulario')) actualizarTotal();});
  $('#busqueda-global').addEventListener('input',evento=>{estado.busqueda=evento.target.value;renderizar({animar:false});});
  $('#selector-sede').addEventListener('change',evento=>{estado.sede=evento.target.value;renderizar({animar:false});});
  $('#recargar').addEventListener('click',()=>{conectar();if(estado.vista==='respaldo')cargarRespaldo();});
  $('#cerrar-dialogo').addEventListener('click',()=>{if(!estado.guardando)$('#dialogo').close();});
  $('#dialogo').addEventListener('cancel',evento=>{if(estado.guardando)evento.preventDefault();});
  $('#dialogo').addEventListener('click',evento=>{if(!estado.guardando && evento.target===$('#dialogo')){const r=evento.target.getBoundingClientRect();if(evento.clientX<r.left||evento.clientX>r.right||evento.clientY<r.top||evento.clientY>r.bottom) evento.target.close();}});
  let salidaIntro;
  $('#entrar').addEventListener('click',()=>{clearTimeout(salidaIntro);$('#intro').classList.add('saliendo');$('#intro').inert=true;$('#aplicacion').hidden=false;document.body.classList.add('en-aplicacion');renderizar();document.dispatchEvent(new CustomEvent('tierra:pantalla',{detail:{visible:'aplicacion'}}));$('#contenido').focus({preventScroll:true});if(matchMedia('(prefers-reduced-motion: reduce)').matches)$('#intro').hidden=true;else salidaIntro=setTimeout(()=>$('#intro').hidden=true,1300);});
  $('#volver-inicio').addEventListener('click',()=>{clearTimeout(salidaIntro);$('#intro').hidden=false;$('#intro').inert=false;$('#intro').classList.remove('saliendo');$('#aplicacion').hidden=true;document.body.classList.remove('en-aplicacion');document.dispatchEvent(new CustomEvent('tierra:pantalla',{detail:{visible:'intro'}}));$('#entrar').focus({preventScroll:true});document.title='Tierra Fértil · Cooperativa Agrícola';});
  $('#fecha-actual').textContent=new Intl.DateTimeFormat('es-GT',{weekday:'long',day:'numeric',month:'long',year:'numeric',timeZone:'America/Guatemala'}).format(new Date());
  $('#selector-sede').innerHTML='<option value="">Todas las sedes</option>';
  actualizarAviso();
  conectar();
})();
