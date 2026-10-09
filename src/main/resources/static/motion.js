/* Movimiento de presentación independiente de los datos y de la API.
 * Los contextos se revierten antes de sustituir cada vista del SPA.
 * No oculta contenido si faltan las bibliotecas o se reduce el movimiento.
 */
(() => {
  'use strict';
  const {gsap, ScrollTrigger, Lenis} = window;
  if (!gsap || !ScrollTrigger || !Lenis) return;
  gsap.registerPlugin(ScrollTrigger);
  const preferencia = matchMedia('(prefers-reduced-motion: reduce)');
  const $ = selector => document.querySelector(selector);
  const root = $('#contenido'), app = $('#aplicacion'), intro = $('#intro');
  const dock = $('.dock'), dialogo = $('#dialogo');
  const ease = 'power3.out';
  let vistaCtx, introCtx, marcoCtx, dialogoCtx, lenis, ticker, dockResize;
  let cuadroVista = 0, cuadroRefresh = 0, restaurarContadores = [];
  let activo = false, imprimiendo = false;

  function limpiarVista() {
    cancelAnimationFrame(cuadroVista);
    cuadroVista = 0;
    vistaCtx?.revert(); vistaCtx = null;
    restaurarContadores.forEach(restaurar => restaurar());
    restaurarContadores = [];
    const historia = root.querySelector('.editorial-story');
    if (historia) {
      historia.dataset.activeStep = '0';
      const titulo = historia.querySelector('[data-story-step="0"] h2');
      const pie = historia.querySelector('[data-story-caption]');
      if (pie && titulo) pie.textContent = `01 / ${titulo.textContent}`;
      historia.querySelectorAll('[data-story-marker], [data-story-step]').forEach(el => {
        const indice = el.dataset.storyMarker ?? el.dataset.storyStep;
        el.classList.toggle('is-active',indice === '0');
      });
    }
  }

  function actualizarDock() {
    const boton = dock.querySelector('.dock-item[aria-current="page"]');
    if (!boton || app.hidden) return;
    // offset evita medir un rectángulo transitorio transformado por GSAP.
    dock.style.setProperty('--dock-x', `${boton.offsetLeft}px`);
    dock.style.setProperty('--dock-y', `${boton.offsetTop}px`);
    dock.style.setProperty('--dock-w', `${boton.offsetWidth}px`);
    dock.style.setProperty('--dock-h', `${boton.offsetHeight}px`);
    dock.dataset.motionReady = 'true';
  }

  function refrescar() {
    cancelAnimationFrame(cuadroRefresh);
    cuadroRefresh = requestAnimationFrame(() => {
      cuadroRefresh = 0;
      if (!activo || app.hidden) return;
      lenis?.resize(); actualizarDock(); ScrollTrigger.refresh();
    });
  }

  function pararScroll() {
    if (ticker) gsap.ticker.remove(ticker);
    ticker = null;
    lenis?.destroy(); lenis = null;
  }

  function iniciarScroll() {
    if (lenis || preferencia.matches || app.hidden || imprimiendo) return;
    lenis = new Lenis({
      lerp: .085, wheelMultiplier: .95,
      anchors: {duration: 1.6, easing: t => 1 - Math.pow(1 - t, 4)},
      prevent: nodo => nodo.closest?.('dialog, .table-wrap, textarea, select') != null
    });
    lenis.on('scroll', ScrollTrigger.update);
    ticker = tiempo => lenis?.raf(tiempo * 1000);
    gsap.ticker.add(ticker);
    gsap.ticker.lagSmoothing(0);
    if (dialogo.open) lenis.stop();
  }

  function irAlInicio() {
    if (lenis) lenis.scrollTo(0, {duration: 1.6, easing: t => 1 - Math.pow(1 - t, 4)});
    else window.scrollTo({top: 0, behavior: 'instant'});
  }

  function prepararVista() {
    // Atributos en los componentes existentes, sin reorganizar el documento.
    root.querySelectorAll('.section-heading, .editorial-section-heading, .hero-aside, .empty-state, .harvest-note, .summary-card, .loan-overview > .eyebrow, .loan-overview > .muted').forEach(el => el.dataset.reveal = '');
    root.querySelectorAll('.metrics, .module-grid, .loan-distribution, .data-table tbody').forEach(el => el.dataset.stagger = '');
    const hero = root.querySelector('.hero');
    if (hero && !hero.querySelector('.hero-landscape')) {
      const paisaje = document.createElement('div');
      paisaje.className = 'hero-landscape'; paisaje.setAttribute('aria-hidden', 'true');
      paisaje.dataset.speed = '-.3';
      const arte = document.createElement('div'); arte.className = 'hero-landscape-art';
      paisaje.append(arte); hero.prepend(paisaje);
    }
    root.querySelectorAll('.field-lines').forEach(el => el.dataset.speed = '.2');
    root.querySelectorAll('.table-wrap').forEach(el => el.setAttribute('data-lenis-prevent', ''));
    root.querySelectorAll('img').forEach(img => {
      if (!img.complete && !img.dataset.motionObserved) {
        img.dataset.motionObserved = 'true';
        img.addEventListener('load', refrescar, {once:true});
      }
    });
  }

  function montarEditorial(animar) {
    const hero = root.querySelector('.editorial-hero');
    if (!hero) return;
    const foto = hero.querySelector('[data-hero-photo]');
    const texto = hero.querySelector('.editorial-hero-copy');
    if (animar) gsap.fromTo(texto.children,
      {autoAlpha:0, y:40, filter:'blur(10px)'},
      {autoAlpha:1, y:0, filter:'blur(0px)', duration:1.2, ease, stagger:.12,
        delay:!intro.hidden && intro.inert ? .65 : 0,
        clearProps:'transform,opacity,visibility,filter'});
    // La fotografía responde al recorrido completo de la portada, en ambos
    // sentidos. El texto tiene otra velocidad y conserva su propio contraste.
    gsap.fromTo(foto, {scale:1.16, yPercent:0}, {
      scale:1, yPercent:16, ease:'none',
      scrollTrigger:{trigger:hero,start:'top top',end:'bottom top',scrub:true}
    });
    gsap.to(texto, {y:-90, opacity:.15, ease:'none',
      scrollTrigger:{trigger:hero,start:'top top',end:'bottom top',scrub:true}
    });
    const historia = root.querySelector('.editorial-story');
    if (!historia) return;
    const fotos = [...historia.querySelectorAll('[data-story-photo]')];
    const pasos = [...historia.querySelectorAll('[data-story-step]')];
    const indicadores = [...historia.querySelectorAll('[data-story-marker]')];
    const pie = historia.querySelector('[data-story-caption]');
    let etapaActual = -1;
    const marcar = progreso => {
      const etapa = Math.min(3,Math.floor(progreso * 3 + .28));
      if (etapa === etapaActual) return;
      etapaActual = etapa; historia.dataset.activeStep = String(etapa);
      pie.textContent = `${String(etapa+1).padStart(2,'0')} / ${pasos[etapa].querySelector('h2').textContent}`;
      indicadores.forEach((el,i)=>el.classList.toggle('is-active',i===etapa));
      pasos.forEach((el,i)=>el.classList.toggle('is-active',i===etapa));
    };
    gsap.set(fotos.slice(1),{autoAlpha:0}); gsap.set(fotos[0],{autoAlpha:1});
    // CSS fija la columna izquierda dentro de esta sección. Este timeline
    // controla el zoom y los crossfades según el avance, sin efectos once:true.
    const escena = gsap.timeline({scrollTrigger:{trigger:historia,start:'top top',end:'bottom bottom',scrub:true,
      onUpdate:self=>marcar(self.progress)}});
    escena.fromTo(fotos,{scale:1.06},{scale:1,duration:3,ease:'none'},0);
    fotos.slice(1).forEach((imagen,i)=>{
      escena.to(fotos[i],{autoAlpha:0,duration:.28,ease:'none'},i+.72);
      escena.to(imagen,{autoAlpha:1,duration:.28,ease:'none'},i+.72);
    });
    pasos.forEach(paso=>gsap.fromTo(paso.children,{opacity:.25,y:50},
      {opacity:1,y:0,ease:'none',scrollTrigger:{trigger:paso,start:'top 80%',end:'top 30%',scrub:true}}));
    marcar(0);
  }

  function revelar(el, opciones = {}) {
    return gsap.fromTo(el, {autoAlpha: 0, y: 40, filter: 'blur(8px)'}, {
      autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 1.1, ease,
      clearProps: 'transform,opacity,visibility,filter',
      scrollTrigger: {trigger: el, start: 'top 88%', once: true}, ...opciones
    });
  }

  function contar(el) {
    const destino = Number(el.dataset.count), dec = Number(el.dataset.dec || 0);
    if (!Number.isFinite(destino) || dec < 0 || dec > 2) return;
    const htmlFinal = el.innerHTML;
    const accesible = document.createElement('span');
    accesible.className = 'sr-only'; accesible.textContent = el.textContent;
    const visual = document.createElement('span'); visual.setAttribute('aria-hidden', 'true');
    while (el.firstChild) visual.append(el.firstChild);
    el.append(accesible, visual);
    const texto = [...visual.childNodes].find(n => n.nodeType === Node.TEXT_NODE);
    if (!texto) { el.innerHTML = htmlFinal; return; }
    const textoFinal = texto.textContent;
    const formatear = valor => (el.dataset.pre || '') + valor.toLocaleString('es-GT', {
      minimumFractionDigits: dec, maximumFractionDigits: dec
    }) + (el.dataset.suf || '');
    const objeto = {valor: 0}; texto.textContent = formatear(0);
    // El valor accesible permanece estable, también dentro de aria-live.
    restaurarContadores.push(() => {el.innerHTML = htmlFinal;});
    gsap.to(objeto, {valor: destino, duration: 2.2, ease,
      onUpdate: () => {texto.textContent = formatear(objeto.valor);},
      onComplete: () => {texto.textContent = textoFinal;},
      scrollTrigger: {trigger: el, start: 'top 90%', once: true}
    });
  }

  function montarVista(animar = true) {
    limpiarVista(); actualizarDock();
    if (app.hidden || !activo) return;
    prepararVista();
    if (preferencia.matches || imprimiendo) {refrescar(); return;}
    iniciarScroll();
    vistaCtx = gsap.context(() => {
      if (animar) {
        root.querySelectorAll('[data-reveal]').forEach(el => revelar(el));
        root.querySelectorAll('[data-stagger]').forEach(grupo => {
          // Un único trigger por grupo; listas largas no acumulan cientos de triggers.
          const filas = [...grupo.children];
          revelar(filas, {duration: 1, stagger: {each: .1, amount: Math.min(.4, filas.length * .1)},
            scrollTrigger: {trigger: grupo, start: 'top 85%', once: true}
          });
        });
        root.querySelectorAll('[data-count]').forEach(contar);
        root.querySelectorAll('[data-draw]').forEach((path, i) => {
          if (typeof path.getTotalLength !== 'function') return;
          const longitud = path.getTotalLength();
          gsap.fromTo(path, {strokeDasharray: longitud, strokeDashoffset: longitud}, {
            strokeDashoffset: 0, duration: 2.4, delay: i * .15, ease: 'power2.inOut',
            scrollTrigger: {trigger: path, start: 'top 85%', once: true}
          });
        });
        // Las barras conservan su medida: scaleX anima sin recalcular el layout.
        root.querySelectorAll('[data-bar]').forEach(el => gsap.fromTo(el, {scaleX: 0}, {
          scaleX: 1, transformOrigin: 'left center', duration: 1.6, ease: 'power3.inOut',
          scrollTrigger: {trigger: el, start: 'top 92%', once: true}
        }));
      }
      const hero = root.querySelector('.hero');
      if (hero) {
        const copia = hero.querySelector('.hero-copy');
        if (animar) {
          const titulo = copia.querySelector('h1');
          // Mantiene el salto de línea original y anima sus dos líneas por separado.
          if (titulo.firstChild?.nodeType === Node.TEXT_NODE) {
            const linea = document.createElement('span');
            linea.className = 'hero-title-line'; linea.textContent = titulo.firstChild.textContent;
            titulo.firstChild.replaceWith(linea);
          }
          gsap.fromTo([copia.querySelector('.hero-kicker'), copia.querySelector('.hero-title-line'), copia.querySelector('em'), copia.querySelector('.hero-subtitle'), copia.querySelector('.hero-button')],
            {autoAlpha: 0, y: 40, filter: 'blur(10px)'},
            {autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 1.2, ease, stagger: .12, clearProps: 'transform,opacity,visibility,filter'});
          gsap.fromTo(hero.querySelector('.hero-landscape-art'), {scale: 1.16}, {scale: 1, duration: 2.4, ease: 'expo.out'});
        }
        gsap.to(copia, {y: -90, opacity: .15, ease: 'none',
          scrollTrigger: {trigger: hero, start: 'top top', end: 'bottom top', scrub: true}
        });
      }
      montarEditorial(animar);
      root.querySelectorAll('[data-speed]').forEach(el => gsap.to(el, {
        y: Number(el.dataset.speed) * 160, ease: 'none',
        scrollTrigger: {trigger: el.closest('section') || root, start: 'top top', end: 'bottom top', scrub: true}
      }));
      root.querySelectorAll('[data-parallax]:not([data-hero-photo])').forEach(el => {
        const amplitud = Number(el.dataset.parallax) || 8;
        gsap.fromTo(el, {yPercent: -amplitud}, {yPercent: amplitud, ease: 'none',
          scrollTrigger: {trigger: el.closest('[data-parallax-scene]') || el.parentElement, start: 'top bottom', end: 'bottom top', scrub: true}
        });
      });
      root.querySelectorAll('[data-clip]').forEach(el => gsap.fromTo(el,
        {clipPath: 'inset(12% 10% 12% 10% round 28px)', scale: .92},
        {clipPath: 'inset(0% 0% 0% 0% round 28px)', scale: 1, ease: 'none',
          scrollTrigger: {trigger: el.closest('[data-parallax-scene]') || el, start: 'top 95%', end: 'top 25%', scrub: 1}
        }));
      // Cada sección pertenece al módulo actual. ScrollTrigger no cambia ni
      // inventa rutas: respeta la selección y el título del SPA existente.
      const secciones = root.querySelectorAll(':scope > section');
      (secciones.length ? [...secciones] : [root]).forEach(seccion => ScrollTrigger.create({
        trigger: seccion, start: 'top 50%', end: 'bottom 50%',
        onEnter: actualizarDock, onEnterBack: actualizarDock
      }));
    }, root);
    refrescar();
  }

  function programarVista(animar = true) {
    cancelAnimationFrame(cuadroVista);
    cuadroVista = requestAnimationFrame(() => {cuadroVista = 0; montarVista(animar);});
  }

  function portada() {
    introCtx?.revert(); introCtx = null;
    if (preferencia.matches || imprimiendo) return;
    introCtx = gsap.context(() => {
      gsap.fromTo(intro.querySelectorAll('.intro-content > *'),
        {autoAlpha: 0, y: 40, filter: 'blur(10px)'},
        {autoAlpha: 1, y: 0, filter: 'blur(0px)', duration: 1.2, ease, stagger: .12, clearProps: 'transform,opacity,visibility,filter'});
      gsap.fromTo(intro.querySelector('.intro-ambient'), {scale: 1.16}, {scale: 1, duration: 2.4, ease: 'expo.out'});
    }, intro);
  }

  function pantalla(visible) {
    if (!activo) iniciar();
    if (visible === 'intro') {
      limpiarVista(); marcoCtx?.revert(); marcoCtx = null;
      pararScroll(); window.scrollTo({top: 0, behavior: 'instant'}); portada();
      return;
    }
    introCtx?.revert(); introCtx = null;
    if (!preferencia.matches && !imprimiendo) {
      introCtx = gsap.context(() => gsap.fromTo(intro,
        {clipPath:'inset(0 0 0% 0)'},
        {clipPath:'inset(0 0 100% 0)',duration:1.3,ease:'expo.inOut'}));
      marcoCtx?.revert();
      marcoCtx = gsap.context(() => {
        // No transformar el contenedor .application (tiene hijos fixed).
        gsap.fromTo($('.topbar'), {autoAlpha: 0, y: -12}, {autoAlpha: 1, y: 0, duration: .8, delay: .2, ease, clearProps: 'transform,opacity,visibility'});
        gsap.fromTo(dock, {opacity: 0}, {opacity: 1, duration: .8, delay: .3, ease, clearProps: 'opacity'});
      });
    }
    iniciarScroll(); programarVista();
  }

  function abrirDialogo() {
    lenis?.stop(); dialogoCtx?.revert(); dialogoCtx = null;
    if (preferencia.matches || imprimiendo) return;
    dialogoCtx = gsap.context(() => gsap.fromTo(dialogo,
      {autoAlpha: 0, y: 12, scale: .98}, {autoAlpha: 1, y: 0, scale: 1, duration: .4, ease, clearProps: 'transform,opacity,visibility'}));
  }

  function iniciar() {
    if (activo) return;
    activo = true; document.documentElement.classList.add('motion-ready');
    dockResize = new ResizeObserver(actualizarDock); dockResize.observe(dock);
    if (app.hidden) portada(); else {iniciarScroll(); programarVista(false);}
  }

  function destruir() {
    limpiarVista(); pararScroll();
    introCtx?.revert(); introCtx = null;
    marcoCtx?.revert(); marcoCtx = null;
    dialogoCtx?.revert(); dialogoCtx = null;
    dockResize?.disconnect(); dockResize = null;
    cancelAnimationFrame(cuadroRefresh); cuadroRefresh = 0;
    document.documentElement.classList.remove('motion-ready');
    dock.removeAttribute('data-motion-ready');
    root.querySelectorAll('.hero-landscape').forEach(el => el.remove());
    activo = false;
  }

  document.addEventListener('tierra:antes-render', limpiarVista);
  document.addEventListener('tierra:render', evento => programarVista(evento.detail.animar));
  document.addEventListener('tierra:pantalla', evento => pantalla(evento.detail.visible));
  document.addEventListener('tierra:dialogo', abrirDialogo);
  dialogo.addEventListener('close', () => {
    dialogoCtx?.revert(); dialogoCtx = null;
    if (!app.hidden) {lenis?.start(); refrescar();}
  });
  preferencia.addEventListener('change', () => {
    destruir();
    // Si se cambia la preferencia durante la salida, no restaurar la portada
    // sobre la aplicación que ya está activa.
    if (!app.hidden && intro.inert) intro.hidden = true;
    iniciar();
  });
  window.addEventListener('resize', refrescar);
  window.addEventListener('pagehide', destruir);
  window.addEventListener('pageshow', evento => {if (evento.persisted) iniciar();});
  window.addEventListener('beforeprint', () => {imprimiendo = true; destruir();});
  window.addEventListener('afterprint', () => {imprimiendo = false; iniciar();});
  document.fonts?.ready.then(refrescar);
  window.MovimientoTierra = Object.freeze({irAlInicio});
  iniciar();
})();
