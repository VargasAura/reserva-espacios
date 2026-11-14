/* Calendario de disponibilidad (REQ-10 a REQ-14). JavaScript puro, sin librerías. */
(function () {
    'use strict';

    var app = document.getElementById('app-calendario');
    if (!app) return;

    var URL_DATOS = app.dataset.urlDatos;
    var URL_RESERVA = app.dataset.urlReserva;

    var DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
    var DIAS_LARGOS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];
    var MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto',
        'septiembre', 'octubre', 'noviembre', 'diciembre'];
    var ESTADOS = { L: 'Libre', R: 'Reservado', B: 'Bloqueado (horario pasado)' };

    var contenedor = document.getElementById('calendario');
    var periodo = document.getElementById('periodo');
    var sinDisponibilidad = document.getElementById('sin-disponibilidad');
    var errorConexion = document.getElementById('error-conexion');
    var detalle = document.getElementById('detalle-contenido');
    var inputFecha = document.getElementById('fecha');
    var selectTipo = document.getElementById('tipo');
    var selectFranja = document.getElementById('franja');
    var botonesVista = document.querySelectorAll('#vistas button');

    /* ---------- Utilidades de texto y fechas ---------- */
    function dos(n) { return String(n).padStart(2, '0'); }
    function hhmm(minutos) { return dos(Math.floor(minutos / 60)) + ':' + dos(minutos % 60); }
    function aFecha(iso) { var p = iso.split('-').map(Number); return new Date(p[0], p[1] - 1, p[2]); }
    function aIso(f) { return f.getFullYear() + '-' + dos(f.getMonth() + 1) + '-' + dos(f.getDate()); }
    function indiceSemana(f) { return (f.getDay() + 6) % 7; } // lunes = 0
    function mayuscula(t) { return t.charAt(0).toUpperCase() + t.slice(1); }
    function horasTexto(n) { return String(Math.round(n * 10) / 10).replace('.', ','); }
    function fechaLarga(iso) {
        var f = aFecha(iso);
        return f.getDate() + ' de ' + MESES[f.getMonth()] + ' de ' + f.getFullYear();
    }
    function el(etiqueta, clase, texto) {
        var nodo = document.createElement(etiqueta);
        if (clase) nodo.className = clase;
        if (texto !== undefined && texto !== null) nodo.textContent = texto;
        return nodo;
    }

    /* ---------- Estado de los filtros ---------- */
    var estado = { vista: 'DIA', fecha: aIso(new Date()), tipo: '', franja: 'DIA_COMPLETO', espacioId: null };
    var ultimo = null;          // { vista, datos } de la última respuesta válida
    var ultimaFirma = '';       // para no redibujar si nada cambió (evita parpadeos)
    var ultimaClave = '';       // para saber cuándo volver el scroll al inicio
    var ultimaPeticion = 0;
    var refrescoSegundos = 3;
    var temporizador = null;

    /* ---------- Carga de datos y refresco automático (REQ-14) ---------- */
    // REQ-14: pide los datos y se vuelve a llamar sola cada refrescoSegundos
    async function cargar() {
        clearTimeout(temporizador);
        if (document.hidden) { // pestaña oculta: no gasta peticiones
            temporizador = setTimeout(cargar, refrescoSegundos * 1000);
            return;
        }
        var mia = ++ultimaPeticion;
        var params = new URLSearchParams({ vista: estado.vista, fecha: estado.fecha, franja: estado.franja });
        if (estado.tipo) params.set('tipo', estado.tipo);
        var vistaPedida = estado.vista;

        try {
            var resp = await fetch(URL_DATOS + '?' + params.toString(), { cache: 'no-store', headers: { Accept: 'application/json' } });
            if (resp.redirected) { window.location.reload(); return; } // sesión vencida: vuelve al inicio de sesión
            if (!resp.ok) throw new Error('Respuesta ' + resp.status);
            var texto = await resp.text();
            if (mia !== ultimaPeticion) return; // llegó tarde: ya hay una petición más nueva
            var datos = JSON.parse(texto);
            refrescoSegundos = (datos.config && datos.config.refrescoSegundos) || 3;
            errorConexion.hidden = true;
            contenedor.classList.remove('cargando');

            var firma = JSON.stringify([estado.vista, estado.fecha, estado.tipo, estado.franja]) + '|' + texto;
            if (firma !== ultimaFirma) {
                ultimaFirma = firma;
                ultimo = { vista: vistaPedida, datos: datos };
                dibujar();
            }
        } catch (e) {
            if (mia !== ultimaPeticion) return;
            errorConexion.hidden = false; // se conserva lo que ya estaba dibujado
        }
        temporizador = setTimeout(cargar, refrescoSegundos * 1000);
    }

    // REQ-11: se cambió algún filtro: actualiza los controles y pide datos nuevos de inmediato
    function cambiar() {
        sincronizarControles();
        contenedor.classList.add('cargando');
        cargar();
    }

    /* ---------- Controles ---------- */
    // REQ-10, REQ-11: refleja el estado en la vista, la fecha, el tipo y la franja
    function sincronizarControles() {
        botonesVista.forEach(function (b) {
            b.classList.toggle('activo', b.dataset.vista === estado.vista);
            b.setAttribute('aria-pressed', b.dataset.vista === estado.vista ? 'true' : 'false');
        });
        inputFecha.value = estado.fecha;
        selectTipo.value = estado.tipo;
        selectFranja.value = estado.franja;
    }

    // REQ-10: botones anterior / siguiente según la vista (1 día, 7 días o 1 mes)
    function mover(delta) {
        var f = aFecha(estado.fecha);
        if (estado.vista === 'DIA') f.setDate(f.getDate() + delta);
        else if (estado.vista === 'SEMANA') f.setDate(f.getDate() + 7 * delta);
        else { f.setDate(1); f.setMonth(f.getMonth() + delta); }
        estado.fecha = aIso(f);
        cambiar();
    }

    // REQ-10: desde la vista semana o mes se pasa a la vista día
    function irADia(iso) {
        estado.vista = 'DIA';
        estado.fecha = iso;
        cambiar();
    }

    /* ---------- Dibujo ---------- */
    // REQ-10, REQ-13: dibuja según la última respuesta (o muestra el mensaje de sin disponibilidad)
    function dibujar() {
        if (!ultimo) return;
        var datos = ultimo.datos;
        periodo.textContent = textoPeriodo(ultimo.vista, datos.dias);
        dibujarDetalle(datos);

        var hayLibres = datos.espacios.some(function (e) {
            return Object.keys(e.dias).some(function (d) { return e.dias[d].indexOf('L') >= 0; });
        });
        sinDisponibilidad.hidden = hayLibres;
        if (!hayLibres) {
            contenedor.replaceChildren();
            contenedor.hidden = true;
            return;
        }
        contenedor.hidden = false;

        var nodos;
        if (ultimo.vista === 'MES') nodos = dibujarMes(datos);
        else if (ultimo.vista === 'SEMANA') nodos = dibujarSemana(datos);
        else nodos = [dibujarDia(datos)];

        var clave = ultimo.vista + estado.fecha + estado.tipo + estado.franja;
        var scroll = contenedor.scrollLeft;
        contenedor.replaceChildren.apply(contenedor, nodos);
        contenedor.scrollLeft = (clave === ultimaClave) ? scroll : 0; // mantiene el scroll al refrescar
        ultimaClave = clave;
    }

    // REQ-10: texto del periodo mostrado
    function textoPeriodo(vista, dias) {
        if (!dias.length) return '';
        var primero = aFecha(dias[0]);
        if (vista === 'MES') return mayuscula(MESES[primero.getMonth()]) + ' de ' + primero.getFullYear();
        if (vista === 'SEMANA') return 'Semana del ' + fechaLarga(dias[0]) + ' al ' + fechaLarga(dias[dias.length - 1]);
        return mayuscula(DIAS_LARGOS[indiceSemana(primero)]) + ' ' + fechaLarga(dias[0]);
    }

    // REQ-10, REQ-12: vista día = una columna por espacio, una fila por casilla
    function dibujarDia(datos) {
        var cfg = datos.config;
        var dia = datos.dias[0];
        var rejilla = el('div', 'rejilla-dia');
        rejilla.style.gridTemplateColumns = 'var(--ancho-hora) repeat(' + datos.espacios.length + ', minmax(7rem, 1fr))';
        rejilla.append(el('div', 'celda-esquina'));
        datos.espacios.forEach(function (e) { rejilla.append(botonEspacio(e, 'col-espacio')); });
        var n = totalCasillas(datos.espacios[0], dia);
        for (var i = 0; i < n; i++) {
            rejilla.append(etiquetaHora(cfg, i));
            datos.espacios.forEach(function (e) { rejilla.append(crearCasilla(e, dia, e.dias[dia], i, cfg)); });
        }
        return rejilla;
    }

    // REQ-10: vista semana = por cada espacio, 7 columnas (una por día)
    function dibujarSemana(datos) {
        var cfg = datos.config;
        return datos.espacios.map(function (e) {
            var bloque = el('section', 'bloque-espacio');
            bloque.append(botonEspacio(e, 'col-espacio'));
            var rejilla = el('div', 'rejilla-semana');
            rejilla.style.gridTemplateColumns = 'var(--ancho-hora) repeat(' + datos.dias.length + ', minmax(3.4rem, 1fr))';
            rejilla.append(el('div', 'celda-esquina'));
            datos.dias.forEach(function (d) { rejilla.append(encabezadoDia(d)); });
            var n = totalCasillas(e, datos.dias[0]);
            for (var i = 0; i < n; i++) {
                rejilla.append(etiquetaHora(cfg, i));
                datos.dias.forEach(function (d) { rejilla.append(crearCasilla(e, d, e.dias[d], i, cfg)); });
            }
            bloque.append(rejilla);
            return bloque;
        });
    }

    // REQ-10: vista mes = por cada espacio, un resumen por día con las horas libres
    function dibujarMes(datos) {
        var cfg = datos.config;
        var desfase = indiceSemana(aFecha(datos.dias[0]));
        return datos.espacios.map(function (e) {
            var bloque = el('section', 'bloque-espacio');
            bloque.append(botonEspacio(e, 'col-espacio'));
            var contenedorMes = el('div', 'cal-contenedor');
            var rejilla = el('div', 'rejilla-mes');
            DIAS_CORTOS.forEach(function (d) { rejilla.append(el('div', 'mes-titulo', d)); });
            for (var v = 0; v < desfase; v++) rejilla.append(el('div'));
            datos.dias.forEach(function (d) {
                var cadena = e.dias[d] || '';
                var libres = (cadena.match(/L/g) || []).length;
                var clase = libres > 0 ? 'libre' : (cadena.indexOf('R') >= 0 ? 'lleno' : 'pasado');
                var texto = libres > 0 ? horasTexto(libres * cfg.franjaMinutos / 60) + ' h libres'
                    : (clase === 'lleno' ? 'Completo' : 'Pasado');
                var boton = el('button', 'mes-dia ' + clase);
                boton.type = 'button';
                boton.title = e.nombre + ' · ' + fechaLarga(d) + ' · ' + texto + ' (clic para ver el día)';
                boton.append(el('span', 'num', String(aFecha(d).getDate())), el('span', 'txt', texto));
                boton.addEventListener('click', function () { irADia(d); });
                rejilla.append(boton);
            });
            contenedorMes.append(rejilla);
            bloque.append(contenedorMes);
            return bloque;
        });
    }

    // REQ-12: botón con el nombre del espacio; al hacer clic muestra su detalle
    function botonEspacio(e, clase) {
        var b = el('button', clase + (e.id === estado.espacioId ? ' seleccionado' : ''));
        b.type = 'button';
        b.title = 'Ver detalle de ' + e.nombre;
        b.append(document.createTextNode(e.nombre), el('small', null, '👥 ' + e.capacidad + ' · ' + e.tipoEtiqueta));
        b.addEventListener('click', function () {
            estado.espacioId = e.id;
            dibujar();
            document.getElementById('detalle').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        });
        return b;
    }

    // REQ-10: encabezado de un día (en la vista semana lleva a la vista día)
    function encabezadoDia(iso) {
        var f = aFecha(iso);
        var b = el('button', 'col-dia', DIAS_CORTOS[indiceSemana(f)] + ' ' + f.getDate());
        b.type = 'button';
        b.title = 'Ver ' + fechaLarga(iso);
        b.addEventListener('click', function () { irADia(iso); });
        return b;
    }

    // REQ-10: la hora solo se escribe al comienzo de cada hora
    function etiquetaHora(cfg, i) {
        var minuto = cfg.inicioMinutos + i * cfg.franjaMinutos;
        return el('div', 'etiqueta-hora', minuto % 60 === 0 ? hhmm(minuto) : '');
    }

    function totalCasillas(espacio, dia) {
        return espacio ? (espacio.dias[dia] || '').length : 0;
    }

    // REQ-10: casilla L (libre, clicable), R (reservada) o B (bloqueada); las dos últimas no se pueden elegir
    function crearCasilla(e, dia, cadena, i, cfg) {
        var letra = (cadena || '').charAt(i) || 'B';
        var inicio = cfg.inicioMinutos + i * cfg.franjaMinutos;
        var fin = inicio + cfg.franjaMinutos;
        var texto = e.nombre + ' · ' + dia + ' · ' + hhmm(inicio) + '–' + hhmm(fin) + ' · ' + ESTADOS[letra];
        var nodo;
        if (letra === 'L') {
            nodo = el('a', 'casilla L');
            nodo.href = urlReserva(e.id, dia, inicio, fin);
            nodo.title = texto + ' (clic para reservar)';
        } else {
            nodo = el('div', 'casilla ' + letra);
            nodo.title = texto;
        }
        nodo.setAttribute('aria-label', texto);
        return nodo;
    }

    // REQ-15: enlace a la pantalla de reservas con los datos precargados (la hora 24:00 se envía como 23:59)
    function urlReserva(espacioId, dia, inicio, fin) {
        var p = new URLSearchParams({
            espacioId: espacioId, fecha: dia,
            horaInicio: hhmm(inicio), horaFin: hhmm(Math.min(fin, 1439))
        });
        return URL_RESERVA + '?' + p.toString();
    }

    // REQ-12: nombre, tipo, capacidad y ubicación del espacio elegido
    function dibujarDetalle(datos) {
        var e = datos.espacios.filter(function (x) { return x.id === estado.espacioId; })[0];
        detalle.replaceChildren();
        if (!e) {
            estado.espacioId = null;
            detalle.append(el('p', 'nota', 'Haz clic en el nombre de un espacio para ver su detalle.'));
            return;
        }
        var lista = el('dl');
        [['Nombre', e.nombre], ['Tipo', e.tipoEtiqueta], ['Capacidad', e.capacidad + ' personas'],
            ['Ubicación', e.ubicacion]].forEach(function (par) {
            lista.append(el('dt', null, par[0]), el('dd', null, par[1]));
        });
        var reservar = el('a', 'btn', 'Reservar este espacio');
        reservar.href = URL_RESERVA + '?' + new URLSearchParams({ espacioId: e.id, fecha: estado.fecha }).toString();
        detalle.append(lista, reservar);
    }

    /* ---------- Eventos ---------- */
    botonesVista.forEach(function (b) {
        b.addEventListener('click', function () { estado.vista = b.dataset.vista; cambiar(); });
    });
    document.getElementById('btn-anterior').addEventListener('click', function () { mover(-1); });
    document.getElementById('btn-siguiente').addEventListener('click', function () { mover(1); });
    document.getElementById('btn-hoy').addEventListener('click', function () { estado.fecha = aIso(new Date()); cambiar(); });
    inputFecha.addEventListener('change', function () {
        if (inputFecha.value) { estado.fecha = inputFecha.value; cambiar(); }
    });
    selectTipo.addEventListener('change', function () { estado.tipo = selectTipo.value; cambiar(); });
    selectFranja.addEventListener('change', function () { estado.franja = selectFranja.value; cambiar(); });
    document.addEventListener('visibilitychange', function () { if (!document.hidden) cargar(); });

    sincronizarControles();
    cargar();
})();