/* Selector de hora en formato de 12 horas (con a. m. / p. m.). Cada campo guarda la hora como HH:mm (24 h)
   en un input oculto, que es lo que recibe el servidor. Solo ofrece horas en punto o y media. */
(function () {
    'use strict';

    function opcion(valor, texto) {
        var o = document.createElement('option');
        o.value = valor;
        o.textContent = texto;
        return o;
    }
    function dos(n) { return String(n).padStart(2, '0'); }

    document.querySelectorAll('.hora12').forEach(function (campo) {
        var oculto = campo.querySelector('input[type=hidden]');
        var hora = campo.querySelector('.h12-hora');
        var ap = campo.querySelector('.h12-ap');

        hora.appendChild(opcion('', 'Hora'));
        for (var i = 0; i < 24; i++) {                       // 12:00, 12:30, 1:00 ... 11:30
            var h12 = i < 2 ? 12 : Math.floor(i / 2);
            var mm = i % 2 ? '30' : '00';
            hora.appendChild(opcion(h12 + ':' + mm, h12 + ':' + mm));
        }
        ap.appendChild(opcion('', '—'));
        ap.appendChild(opcion('AM', 'a. m.'));
        ap.appendChild(opcion('PM', 'p. m.'));

        // valor inicial (editar una reserva o venir desde el calendario): HH:mm -> 12 horas
        var m = /^(\d{1,2}):(\d{2})/.exec(oculto.value || '');
        if (m) {
            var H = parseInt(m[1], 10);
            hora.value = ((H % 12) || 12) + ':' + m[2];       // si no es en punto/y media queda en "Hora"
            ap.value = H >= 12 ? 'PM' : 'AM';
        }

        function actualizar() {
            if (!hora.value || !ap.value) { oculto.value = ''; return; }
            var p = hora.value.split(':');
            var H = (parseInt(p[0], 10) % 12) + (ap.value === 'PM' ? 12 : 0);
            oculto.value = dos(H) + ':' + p[1];
        }
        hora.addEventListener('change', actualizar);
        ap.addEventListener('change', actualizar);
        actualizar();
    });
})();
