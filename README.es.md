[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · **Español** · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard vigila los enchufes e interruptores inteligentes de tus propias cuentas de **eWeLink / SONOFF** y **Tuya / Smart Life** y hace sonar una alarma real en cuanto algo cambia: una bomba que se detuvo, un congelador desenchufado, un dispositivo que perdió la conexión. Funciona en teléfonos y tabletas Android, Android TV y relojes Wear OS, y también hay una app para Windows.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Dispositivos">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Energía">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Automatizaciones">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Alarma de dispositivo">
</p>

> **Idiomas:** la app de Windows está disponible en 20 idiomas desde la 0.13.0. En Android, la elección de idioma dentro de la app (20 idiomas) llega con la **2.26**; la última versión de Android es la 2.25.0.

## Descargar

### Android (teléfono, tableta, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [última versión de Android](https://github.com/Macerce/switchguard/releases/latest) — descarga `SwitchGuard-x.y.z.apk` y ábrelo en el teléfono. Si SwitchGuard ya está instalado desde Google Play, desinstálalo primero (firma distinta). La versión de GitHub te avisa cuando sale una nueva.

### Windows 10 / 11
- **Instalador**: [versiones de Windows](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — descarga `SwitchGuard-x.y.z-Kurulum.exe` de la más reciente y ejecútalo. La app busca nuevas versiones y se actualiza con un clic; los ajustes se conservan.

### Gratis y Pro
- Gratis: todas las funciones, para 1 dispositivo vigilado. Los dispositivos de demostración permiten probarlo todo sin hardware.
- Android: Pro es una compra única en Google Play (sin suscripción) y vigila todos los dispositivos; te acompaña a un teléfono nuevo con tu cuenta de Google.
- Windows: una clave de licencia Pro (Ajustes → tarjeta del plan) vigila todos los dispositivos. Para obtener una clave: **switchguardapp@gmail.com**

## Requisitos

- Android 8.0+ (API 26); Wear OS para la app del reloj
- eWeLink: una app de desarrollador de eWeLink gratuita (App ID + App Secret); la guía de la app te lleva paso a paso. eWeLink permite una sola sesión por cuenta y App ID, así que cada teléfono, tableta, TV o PC necesita su propia cuenta de eWeLink con los dispositivos compartidos ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: el Access ID y el Access Secret de tu propio proyecto gratuito de Tuya Cloud, vinculado a tu cuenta de la app Smart Life / Tuya ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64 bits) para la app de Windows

## Guías de configuración

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Vídeos

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Funciones

### Vigilancia y alarma

- Actualizaciones en directo por el WebSocket de eWeLink y el servicio de mensajes de Tuya, con resincronización HTTP y comprobaciones periódicas como respaldo.
- La alarma suena hasta que la apagas, incluso en modo silencio, y se abre a pantalla completa sobre la pantalla de bloqueo; cada dispositivo puede tener su propio sonido.
- Reglas por dispositivo: alarma, notificación o nada cuando un dispositivo se apaga, se enciende, se desconecta o vuelve a conectarse.
- Alertas condicionales: suena cuando un dispositivo sigue encendido o apagado un tiempo, o su potencia sube o baja de un límite (p. ej. una bomba por debajo de 50 W).
- Ignorar desconexiones breves, aviso cuando la propia vigilancia se detiene (sin internet / nube), aviso sonoro cuando se pierde la sesión de eWeLink (p. ej. la misma cuenta iniciada en otro teléfono).
- Una tarjeta de preparación muestra y corrige los ajustes del teléfono que podrían impedir la alarma (restricción de batería, permiso de pantalla completa, No molestar, volumen de alarma…).

### Dispositivos

- Mosaicos al estilo hogar inteligente con botones rápidos, un botón por canal en dispositivos multicanal, estado y potencia en directo.
- Página del dispositivo: reglas, sonido de alarma propio, categoría de alerta, historial de energía y coste, temporizadores de eWeLink, señal Wi-Fi y diagnóstico de cortes, consumo en espera y eventos recientes.
- **Organizar dispositivos**: Estrella = mantener arriba · flechas = mover · tamaño de tarjeta = pequeña o grande · etiqueta = categoría. Mantén pulsada una tarjeta para volver aquí.

### Alertas

- Alertas de dispositivo y alertas condicionales en un solo lugar, con filtro por categoría de alerta (Crítico / Importante / Normal).
- Ajustes de alertas: sonido y volumen de alarma, horas de silencio, categorías de alerta, Telegram («Avisar a otra persona») y alertas inteligentes; consulta *Todos los ajustes* más abajo.
- Alertas inteligentes: una sola alerta por corte de luz, consumo inusual, presupuesto mensual, informe semanal, modo vacaciones.

### Automatizaciones

- **Reglas** — acciones automáticas: *Si* pasa algo, *entonces* haz algo, con retraso opcional.
- Disparadores: se enciende, se apaga, permanece encendido durante, permanece apagado durante, se desconecta, vuelve a conectarse, la potencia supera, la potencia cae por debajo de, al amanecer, al atardecer, cuando la temperatura exterior sube por encima de, cuando la temperatura exterior baja por debajo de, al llegar, al salir.
- Acciones: Encender, Apagar, Hacer sonar la alarma, Enviar notificación, Ejecutar escena, Iniciar ciclo, Detener ciclo.
- **Solo cuando**: Solo durante estas horas, Y solo si otro dispositivo…, Solo cuando estoy en casa / fuera.
- **Escenas** — Elige qué ocurre con cada dispositivo cuando se ejecuta la escena. Los dispositivos en “Omitir” no se tocan.
- **Grupos** — Elige los dispositivos del grupo. El grupo aparece en la pantalla Dispositivos como un dispositivo más: su botón los enciende o apaga todos a la vez, y puedes usarlo en automatizaciones.
- **Ciclos y temporizadores** — Cuenta atrás, Encender y luego apagar, Ciclo encendido/apagado, Uno por uno, Pasos personalizados. Inicio con un botón, a una hora fija en los días elegidos o durante una franja horaria; variación aleatoria opcional (aspecto de casa habitada) y «detener si se cambia a mano».
- Las automatizaciones se ejecutan en este teléfono mientras el monitoreo está activo. Para programaciones que deban funcionar incluso con el teléfono apagado, usa los temporizadores de la página del dispositivo.

### Energía

- Vista por día / mes / año con total, coste estimado, potencia media y máxima, hora/día/mes de más consumo y un resumen de lo destacado.
- Tarifa única o de tres periodos (día / punta / noche), precio por dispositivo o precio predeterminado.
- Consumo en espera (la potencia por debajo de la cual nunca baja un dispositivo) y su coste mensual.
- Informe de energía en PDF (para imprimir o compartir) o CSV (Excel, Hojas de cálculo de Google).
- SwitchGuard calcula el consumo a partir de las lecturas de potencia tomadas mientras el monitoreo está activo, por lo que puede diferir ligeramente de la app del fabricante del dispositivo (eWeLink, Smart Life).

### Historial

- Cada evento con fecha y hora, y quién o qué lo cambió (app, automatización); filtro por dispositivo.
- Exportar a CSV (últimas 24 horas, 7 días, 30 días o todo) y borrar el historial.

### Chat

- Hola. Puedes gestionar tus dispositivos escribiendo (en inglés o turco): encenderlos o apagarlos, preguntar su estado, el consumo de potencia o la temperatura exterior. Toca el micrófono para decirlo en voz alta (“Turn off kitchen”); la respuesta se lee en voz alta. Abajo hay plantillas ya preparadas; toca una para enviarla, mantenla pulsada para editarla.
- Los comandos en texto libre funcionan sin conexión y entienden inglés y turco; el micrófono usa el reconocimiento de voz del teléfono y las respuestas pueden leerse en voz alta.
- Plantillas: mensajes de un toque que también pueden ejecutarse solos cada día a una hora o cuando pasa algo.

### Pantalla de espera

- Un panel a pantalla completa para un teléfono o tableta en carga: reloj, tiempo, estado crítico, potencia en vivo, actividad reciente y lo que se ejecuta a continuación. También se muestra sobre la pantalla de bloqueo.
- Abre los ajustes de Android: elige SwitchGuard en Protector de pantalla y configúralo para que se inicie al cargar. El panel sustituirá entonces la pantalla de bloqueo mientras esté en el cargador.

### Widgets, icono de ajustes rápidos, accesos directos

- **SwitchGuard** — Muestra si el monitoreo está activo y cualquier alarma que esté sonando
- **Interruptor de SwitchGuard** — Enciende y apaga un dispositivo o canal con un toque. También puede configurarse como En casa / Fuera o para ejecutar una escena.
- **Grupo de SwitchGuard** — Icono grande: enciende o apaga cada dispositivo de un grupo por separado
- **Comando de voz de SwitchGuard** — Un solo icono: toca, dilo (“Turn off kitchen”) y se ejecuta
- **Icono de ajustes rápidos**: muestra el estado de la vigilancia y la inicia o detiene (primero hay que desbloquear el teléfono); mientras suena una alarma abre la app.
- **Accesos directos**: mantén pulsado el icono de la app para En casa / Fuera y tus escenas.

### En casa / Fuera

- **En casa / Fuera** — Las reglas marcadas “solo cuando estoy fuera” no se ejecutan. Cambia a Fuera cuando salgas (también desde el widget, el reloj o el /away de Telegram).
- **Modo vacaciones** — Cuentas como fuera, las horas de silencio están desactivadas y llega un informe cada mañana a las 09:00. Termina por sí solo.
- **Zonas** — Elige un área en el mapa; las automatizaciones se ejecutan cuando tu teléfono entra o sale de ella (p. ej., enciende la luz del jardín al llegar a casa, apaga todo al salir). Las imágenes del mapa se descargan de OpenStreetMap; tu ubicación solo se usa en este teléfono.

### Wear OS

- La app de reloj SwitchGuard muestra alarmas (apágalas desde la muñeca), dispositivos (enciende/apaga), escenas y En casa / Fuera; se conecta con la app del teléfono.

### Android TV

- Funciona en Android TV con el mando; la alarma aparece encima de lo que se esté reproduciendo («Mostrar la alarma sobre otras apps»).
- «Introducir desde el teléfono»: la TV muestra un código QR; escribes las claves en el teléfono y llegan cifradas a la TV por el Wi-Fi de casa.
- «Recibir desde otro dispositivo»: copia claves de Tuya, reglas y ajustes desde tu teléfono por Wi-Fi.

### Varios dispositivos

Teléfono, tableta, Android TV y PC: **cada dispositivo necesita su propia cuenta de eWeLink con la función de desarrollador aprobada y las claves API propias de esa cuenta (App ID / Secret)**; eWeLink permite un solo dispositivo con sesión iniciada por cuenta. Comparte tus dispositivos con la cuenta adicional desde la app eWeLink. Las claves de Tuya pueden usarse en varios dispositivos. Pro se compra una vez y funciona en todos los dispositivos Android con la misma cuenta de Google. Detalles: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo y Pro

- **Dispositivos de demostración** — Dispositivos virtuales para probar todas las funciones. Actúan como si algo hubiera ocurrido en el propio dispositivo (alguien lo accionó, se fue la luz o el Wi-Fi), así que se aplican tus reglas y la alarma puede sonar. Accionar con los botones de arriba cuenta como si fueras tú y no hace sonar la alarma.
- La versión gratuita monitorea 1 dispositivo. Pro monitorea todos tus dispositivos, con todas las funciones. Pago único, sin suscripción. Vinculado a tu cuenta de Google, así que te acompaña a un teléfono nuevo.

## Todos los ajustes

Todo en el orden de la app. La pestaña **Ajustes** contiene conexiones y opciones de la app; alarma, horas de silencio, Telegram y alertas inteligentes están en la pestaña **Alertas** → Ajustes de alertas; las opciones de cada dispositivo están en su página.

### Ajustes → Cuentas

| Ajuste | Para qué sirve |
|---|---|
| Pro / Obtener Pro | La versión gratuita monitorea 1 dispositivo. Pro monitorea todos tus dispositivos, con todas las funciones. Pago único, sin suscripción. Vinculado a tu cuenta de Google, así que te acompaña a un teléfono nuevo. |
| Restaurar | Vuelve a comprobar tus compras de Google Play (teléfono nuevo, reinstalación). |
| eWeLink — Iniciar sesión con eWeLink | Abre la propia página de inicio de sesión de eWeLink; la app nunca ve tu contraseña. |
| Credenciales de la API | App ID, App Secret y región de tu app de desarrollador de eWeLink. |
| Cerrar sesión | El monitoreo se detendrá. Tus ajustes y tu historial se conservan. |
| Tuya / Smart Life | Introduce el Access ID y el Access Secret de tu proyecto de Tuya Cloud (platform.tuya.com → Cloud → tu proyecto → Overview). La cuenta de tu app Smart Life / Tuya debe estar vinculada al proyecto. Centro de datos: Europa central, Oeste de América, China, India |
| Desconectar | Quita la conexión con Tuya; los dispositivos Tuya dejan de vigilarse. |
| Dispositivos de demostración | Dispositivos virtuales para probar todas las funciones |

### Ajustes → Monitoreo

Cómo vigila SwitchGuard tus dispositivos en segundo plano.

| Ajuste | Para qué sirve |
|---|---|
| Máxima fiabilidad | Mantiene el procesador activo para que ningún evento se retrase. Consume más batería. |
| Ignorar desconexiones breves | Espera este tiempo antes de avisar de que un dispositivo se desconectó. Si vuelve a tiempo, no se genera ninguna alerta. |
| Avisar si el monitoreo se detiene | Si no hay internet o conexión con la nube durante este tiempo, recibirás un aviso |
| Intervalo de comprobación de respaldo | Se usa solo cuando no hay actualizaciones instantáneas. |

### Ajustes → Permisos

Sin estos permisos, Android puede impedir que la alarma llegue hasta ti.

| Ajuste | Para qué sirve |
|---|---|
| Notificaciones | No se pueden mostrar alarmas ni avisos. |
| Actividad en segundo plano | El teléfono puede suspender el monitoreo; las alarmas pueden llegar tarde o perderse. |
| Alarma a pantalla completa | Con el teléfono bloqueado, la alarma no puede abrir su pantalla grande. |
| Alarma en pantalla de bloqueo (Xiaomi) | Con el teléfono bloqueado, la pantalla de alarma no puede abrirse. Toca Corregir, abre Otros permisos y activa “Mostrar en pantalla de bloqueo” y “Abrir ventanas nuevas mientras se ejecuta en segundo plano”. |
| Mostrar la alarma sobre otras apps | Necesario en Android TV para que la alarma aparezca encima de lo que se esté reproduciendo. Toca y permite SwitchGuard. |
| Inicio en segundo plano (Xiaomi, Huawei, OPPO, vivo…) | Aparece en teléfonos que bloquean el inicio en segundo plano (Xiaomi, Huawei, OPPO, vivo…): permite que la vigilancia vuelva a arrancar tras reiniciar o actualizar. Ver Solución de problemas. |

### Ajustes → Ubicación y zonas

| Ajuste | Para qué sirve |
|---|---|
| Ubicación | Necesaria para las reglas de amanecer/atardecer y de temperatura exterior. Permanece en este dispositivo; para la temperatura se envía a Open-Meteo redondeada a unos 100 m. |
| Zonas | Elige un área en el mapa; las automatizaciones se ejecutan cuando tu teléfono entra o sale de ella (p. ej., enciende la luz del jardín al llegar a casa, apaga todo al salir). Las imágenes del mapa se descargan de OpenStreetMap; tu ubicación solo se usa en este teléfono. |

### Ajustes → Pantalla de espera

| Ajuste | Para qué sirve |
|---|---|
| Abrir pantalla de espera | Toca la pantalla para ver “Abrir app” y “Cerrar” |
| Usar como protector de pantalla | Abre los ajustes de Android: elige SwitchGuard en Protector de pantalla y configúralo para que se inicie al cargar. El panel sustituirá entonces la pantalla de bloqueo mientras esté en el cargador. |
| Atenuar de noche | El texto se atenúa entre las 23:00 y las 07:00 |

### Ajustes → Resumen diario

| Ajuste | Para qué sirve |
|---|---|
| Notificación de resumen diario | Cuántas veces se encendió cada dispositivo, cuánto tiempo estuvo encendido y cuánta energía usó hoy |
| Enviar a las | Hora del día a la que se envía el resumen. |
| Mostrar ahora el resumen de hoy | Lo muestra ahora mismo. |

### Ajustes → Copia y transferencia

| Ajuste | Para qué sirve |
|---|---|
| Copiar a un archivo | Las reglas, automatizaciones, ajustes y las claves de Tuya y eWeLink se guardan en un archivo protegido con contraseña |
| Restaurar desde un archivo | ¿Cambias de teléfono? Elige el archivo de copia e introduce su contraseña |
| Enviar a otro dispositivo | Copia las claves de Tuya, las reglas y los ajustes a una TV o tableta en el mismo Wi-Fi |
| Recibir desde otro dispositivo | Obtén la configuración de tu teléfono por Wi-Fi |

La transferencia por Wi-Fi nunca envía las claves API de eWeLink ni la sesión de eWeLink: en el otro dispositivo, introduce sus propias claves API e inicia sesión con su propia cuenta de eWeLink. La copia en archivo sí incluye las claves de eWeLink y Tuya, protegidas con tu contraseña.

### Ajustes → Idioma

| Ajuste | Para qué sirve |
|---|---|
| Idioma | Elige el idioma de la app, las notificaciones y las alarmas. Idioma del teléfono o uno de 20 idiomas (desde la 2.26). |

### Ajustes → Ayuda e información

| Ajuste | Para qué sirve |
|---|---|
| Guía de configuración | Mostrar de nuevo la configuración inicial |
| Guía de configuración de eWeLink | Paso a paso con imágenes (GitHub) |
| Guía de configuración de Tuya / Smart Life | Paso a paso con imágenes (GitHub) |
| Enviar un informe de problema | ¿Algo va mal? Prepara un correo con datos de diagnóstico. No se comparte nada hasta que lo envíes. |
| Eliminar informes de fallos guardados | Borra los informes de fallos guardados en este dispositivo. |
| Contacto y comentarios | Escribe un correo al desarrollador (preguntas, errores, ideas). |
| Política de privacidad | Abre la política de privacidad. |
| Acerca de | Versión y aviso legal. La versión de GitHub comprueba en GitHub si hay una más nueva. |

### Alertas

| Ajuste | Para qué sirve |
|---|---|
| Alertas de dispositivos | Por qué suena o notifica cada dispositivo |
| Alertas condicionales | Suena cuando un dispositivo permanece encendido o apagado un tiempo, o cambia su potencia |
| La alarma está lista / Puede que las alarmas no lleguen hasta ti | La tarjeta de preparación de arriba: enumera los ajustes del teléfono que pueden impedir la alarma y abre la pantalla adecuada para corregirlos; también hay un botón «Probar alarma». |

### Alertas → Alarma

| Ajuste | Para qué sirve |
|---|---|
| Sonido de alarma | Sonido general de la alarma. Cada dispositivo puede tener el suyo (página del dispositivo). |
| Volumen de la alarma | Volumen propio de la alarma de SwitchGuard. |
| Vibrar | Vibrar mientras suena la alarma. |
| Sonar al volumen máximo | Sube el volumen de alarma del teléfono al máximo mientras suena la alarma y pausa la música; se restablece al silenciarla |
| Volver a sonar si no se resuelve | Tras silenciar una alarma, vuelve a sonar con este intervalo mientras el dispositivo siga en el estado que la causó (aún apagado, aún encendido o sin conexión). Se detiene cuando el dispositivo vuelve a la normalidad. |
| Avisar a otra persona | Configuración de Telegram (ver abajo). |
| Probar alarma | Prueba la alarma ahora: con sonido o en silencio, al instante o tras 10 segundos |

### Alertas → Categorías de alerta

Cada dispositivo tiene una categoría de alerta. Si no eliges una, se establece automáticamente: un dispositivo monitoreado con algún evento en Alarma es Crítico, uno que solo envía notificaciones es Importante, y el resto son Normales. Las alertas condicionales y las automatizaciones pueden usar una categoría como disparador (p. ej., “hacer sonar la alarma cuando cualquier dispositivo crítico se desconecte”).

| Ajuste | Para qué sirve |
|---|---|
| Crítico · Importante · Normal · Auto | En automático: Crítico si algún evento hace sonar una alarma, Importante si los eventos solo notifican |
| Sonar también en horas de silencio | Las alarmas suenan también en horas de silencio / En horas de silencio las alarmas se convierten en notificaciones |

### Alertas → Horas de silencio

| Ajuste | Para qué sirve |
|---|---|
| Horas de silencio | Durante estas horas las alarmas se convierten en notificaciones (los dispositivos críticos siguen sonando) |
| Empieza / Termina | Hora de inicio y de fin de las horas de silencio. |

Los dispositivos en categorías de alerta configuradas para sonar en horas de silencio (Crítico por defecto) siguen sonando.

### Alertas → Avisar a otra persona

Si una alarma no se silencia a tiempo, las personas que elijas reciben un mensaje de Telegram, y otro cuando se silencie. Aunque tu teléfono esté en otra habitación por la noche, alguien de casa o un vecino se enterará. Los mensajes los envía un bot de Telegram que creas tú; no interviene ningún otro servidor.

| Ajuste | Para qué sirve |
|---|---|
| 1. Bot de Telegram | En Telegram, abre @BotFather, envía /newbot y ponle nombre a tu bot. Pega abajo el token que te da BotFather. |
| 2. Personas a avisar | Pide a cada persona que abra @your_bot en Telegram y pulse Start (también puedes añadir el bot a un grupo familiar). Después toca Buscar personas. |
| 3. Cuándo | Cuánto puede sonar una alarma antes de avisar a las personas (o al instante). |
| Enviar un mensaje de prueba | Envía un mensaje de prueba a todos los de la lista. |
| Desactivar | Desactiva el aviso por Telegram. |

### Alertas → Alertas inteligentes

Alertas que observan todos tus dispositivos en conjunto: cortes de luz, consumo inusual y un resumen semanal. Con Telegram también puedes consultar el estado, hacerte cargo de una alarma y accionar dispositivos a distancia.

| Ajuste | Para qué sirve |
|---|---|
| Corte de luz: Detectar cortes de luz | Si todos los dispositivos monitoreados se desconectan a la vez, recibes una sola alerta de “corte de luz” en lugar de una por dispositivo, y un mensaje con la duración cuando vuelve la luz. |
| Cargador como sensor de corriente | Si este teléfono/tableta permanece enchufado en el lugar, que lo desenchufen durante un minuto cuenta como un corte de luz; también funciona cuando no hay internet. El mensaje sale por Telegram usando datos móviles. |
| En casa / Fuera: Alertas más suaves en casa | Mientras estás en casa, las alarmas de los dispositivos no críticos se convierten en notificaciones (como en las horas de silencio). Los dispositivos críticos siempre suenan. |
| Modo vacaciones | Cuentas como fuera, las horas de silencio están desactivadas y llega un informe cada mañana a las 09:00. Termina por sí solo. |
| Consumo: Consumo inusual | Cada mañana compara ayer con las dos semanas anteriores. Un uso mucho mayor puede indicar una avería; uno mucho menor (un congelador que apenas funcionó) puede indicar que dejó de funcionar. |
| Tarifa eléctrica | El precio predeterminado para los dispositivos sin precio propio. Con una tarifa de tres franjas, cada hora se cobra en su franja: día 06–17, punta 17–22, noche 22–06. |
| Presupuesto mensual | Se te avisa una vez al mes cuando el costo estimado de los dispositivos con medición de energía alcanza esta cantidad. |
| Informe semanal | Cada lunes a las 09:00: cortes de luz, alarmas y tiempo de respuesta, desconexiones y consumo de la última semana. También se envía a Telegram si está configurado. |
| Mostrar el informe de esta semana | Lo muestra ahora mismo. |
| Telegram: Comandos y botón “Recibido” | Las personas de tu lista pueden enviar /status y hacerse cargo de una alarma que suena. El mensaje de alarma incluye un botón “Recibido”: al pulsarlo se silencia el teléfono. |
| Permitir encender y apagar dispositivos | Comandos /on y /off. Solo pueden usarlos las personas de tu lista. |
| Aviso escalonado | A cada persona se le escribe tras su propia espera. Por ejemplo, a un vecino a los 5 minutos, a un familiar a los 15, al grupo familiar a los 30. Si alguien pulsa “Recibido”, no se avisa a las siguientes personas. |
| Llamada en alarma crítica | Cuando un dispositivo de la categoría Crítico hace sonar la alarma, además se llama a la persona desde la línea de este teléfono cuando termina su espera en los avisos escalonados. La llamada solo sirve para despertarla: lo ocurrido está en el mensaje de Telegram enviado al mismo tiempo (Android no permite que las apps reproduzcan sonido en una llamada). Pídele que guarde este número como “Alarma SwitchGuard”. Las llamadas se repiten cada 2 minutos, hasta 3 veces, hasta que se silencie la alarma o alguien pulse “Recibido” en Telegram. No se puede llamar a grupos. |
| Enviar las alertas inteligentes a Telegram | Los cortes de luz, el consumo inusual, el aviso de presupuesto y el informe semanal también se escriben en Telegram. |
| Vigilante | Dos dispositivos con el mismo bot se vigilan mutuamente (p. ej., el teléfono y un TV box). El dispositivo emisor mantiene actualizado un mensaje fijado en el chat de Telegram; el dispositivo vigilante activa una alarma y avisa a tus contactos si se detiene. No desfijes ese mensaje. |

### Página del dispositivo (toca un dispositivo)

| Ajuste | Para qué sirve |
|---|---|
| Monitorear este dispositivo | Los cambios se registran y se avisan según las reglas de abajo |
| Cuando ocurra esto | Se apaga, Se enciende, Se desconecta, Se conecta → Alarma / Notificar / Ninguna. La alarma suena hasta que la silencies. Notificar muestra una notificación normal. Ninguna solo registra el evento en el historial. |
| Sonido para este dispositivo | Su propio sonido de alarma; si no, el sonido general. |
| Categoría de alerta | En automático: Crítico si algún evento hace sonar una alarma, Importante si los eventos solo notifican |
| Energía | Potencia, tensión, corriente; hoy y este mes; historial por día / mes / año con coste; obtener informe (PDF / CSV). |
| Precio por kWh | Precio por kWh de este dispositivo o el precio predeterminado; también puede fijarse como predeterminado. |
| Temporizadores (en el dispositivo) | No hay temporizadores. Los temporizadores se ejecutan en el propio dispositivo, incluso con el teléfono apagado. (Dispositivos eWeLink: añade o edita temporizadores de encendido/apagado a una hora en los días elegidos.) |
| Conexión | dBm es la intensidad de la señal: cuanto más cerca de 0, mejor. Por encima de -55 excelente, hasta -67 buena, hasta -75 aceptable, por debajo débil. |
| Consumo en espera | La media por hora más baja de los últimos 7 días: la potencia por debajo de la cual este dispositivo (y lo que tenga enchufado) nunca baja, incluso “sin hacer nada”. Los televisores, módems y cargadores suelen consumir esto en espera. |
| Copiar datos del dispositivo | Copia los datos sin procesar del dispositivo al portapapeles, útil para informar de un modelo no compatible |

## Comandos de Telegram

Las personas de tu lista pueden enviar /status y hacerse cargo de una alarma que suena. El mensaje de alarma incluye un botón “Recibido”: al pulsarlo se silencia el teléfono.

```
Comandos de SwitchGuard:
/status – monitoreo, alarma y dispositivos
/ack – hacerte cargo de la alarma que suena
/on nombre [canal] – enciende un dispositivo o grupo (varios: /on kitchen, garden)
/off nombre [canal] – apaga un dispositivo o grupo
/watt [nombre] – potencia ahora, hoy y consumo habitual
/temp – temperatura exterior
/scene nombre – ejecuta una escena
/home · /away – establece en casa / fuera

También puedes escribir libremente (en inglés o turco): “turn off kitchen and garden”, “heater watts”, “is the pump on”.

Encender y apagar dispositivos y las escenas debe permitirse en la app (Alertas → Alertas inteligentes).
```

Funcionan tanto los nombres de comando en inglés como en turco (p. ej. /status = /durum, /on = /ac). Solo se aceptan comandos de los chats que elegiste.

## Permisos y por qué

| Permiso | Por qué |
|---|---|
| `POST_NOTIFICATIONS` | Mostrar alarmas, notificaciones y el estado de la vigilancia. |
| `FOREGROUND_SERVICE` | Mantener la vigilancia en segundo plano (la notificación de estado permanente). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Pedir quedar fuera de la optimización de batería para que las alarmas no se retrasen. |
| `USE_FULL_SCREEN_INTENT` | Abrir la alarma a pantalla completa sobre la pantalla de bloqueo. |
| `SYSTEM_ALERT_WINDOW` | Mostrar la alarma sobre otras apps (Android TV, Xiaomi). |
| `WAKE_LOCK` | Mantener el procesador despierto al procesar eventos («Máxima fiabilidad»). |
| `VIBRATE` | Vibrar durante la alarma. |
| `RECEIVE_BOOT_COMPLETED` | Volver a iniciar la vigilancia cuando el teléfono se reinicia. |
| `INTERNET` | Hablar con la nube de eWeLink / Tuya (y con Telegram / Open-Meteo / OpenStreetMap si los usas). |
| `ACCESS_COARSE_LOCATION` | Solo cuando tocas «Usar la ubicación de este dispositivo» para reglas de amanecer/atardecer y temperatura. |
| `ACCESS_FINE_LOCATION` | Zonas: un área exacta para reglas de llegada / salida. |
| `ACCESS_BACKGROUND_LOCATION` | Zonas: detectar llegadas y salidas con la app cerrada (solo se pide al crear una zona). |
| `CALL_PHONE` | «Llamada en alarma crítica»: llamar al número que introdujiste desde la línea de este teléfono. |

## Privacidad

- Sin servidor propio, sin analíticas, sin anuncios, sin rastreo. No se vende ni se comparte nada.
- Claves, tokens, dispositivos, reglas, ajustes e historial (máx. 2.000 eventos) se guardan solo en tu dispositivo.
- La app solo habla con las nubes que conectas (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) y, solo si los usas, con Telegram, Open-Meteo (temperatura y tiempo; ubicación redondeada a unos 100 m) y los mosaicos de mapa de OpenStreetMap.
- Tu contraseña de eWeLink se escribe en la propia página de eWeLink; la app nunca la ve.
- Los datos de la app están excluidos de la copia en la nube de Android; las copias en archivo están protegidas con contraseña; la transferencia por Wi-Fi va cifrada con un código de un solo uso y no sale de tu red doméstica.
- Las compras de Pro las gestiona Google Play.

Política de privacidad completa: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Solución de problemas y preguntas frecuentes

### La alarma llega tarde o no suena

| Problema | Qué hacer |
|---|---|
| La actividad en segundo plano está restringida | El teléfono puede suspender el monitoreo; las alarmas pueden llegar tarde o perderse. |
| La alarma a pantalla completa no está permitida | Con el teléfono bloqueado, la alarma no puede abrir su pantalla grande. |
| Xiaomi bloquea la pantalla de alarma | Con el teléfono bloqueado, la pantalla de alarma no puede abrirse. Toca Corregir, abre Otros permisos y activa “Mostrar en pantalla de bloqueo” y “Abrir ventanas nuevas mientras se ejecuta en segundo plano”. |
| Las notificaciones de alarma están atenuadas | La categoría de notificaciones “Alarmas” no tiene prioridad alta, así que la alarma no aparecerá de forma emergente ni ocupará la pantalla. |
| No molestar: silencio total | En este modo incluso los sonidos de alarma se silencian. |
| El volumen de alarma es muy bajo | El volumen de alarma del teléfono está a cero o casi, por lo que puede que no oigas SwitchGuard. Corregir activa “Sonar al volumen máximo”. |
| Las notificaciones están desactivadas | No se pueden mostrar alarmas ni avisos. |
| Hay un segundo SwitchGuard en este teléfono | Se creó otra copia de SwitchGuard con la función Aplicaciones duales (clonar app) del teléfono. Dos copias funcionando a la vez hacen sonar cada alarma dos veces y se cierran la sesión mutuamente si usan la misma cuenta de eWeLink; en los teléfonos Xiaomi, además, los permisos de esta copia pueden aparecer desactivados. Elimina la segunda copia: mantén pulsado su icono (el que tiene la pequeña insignia) y toca Desinstalar. |

### Fabricantes que detienen apps en segundo plano

| Teléfono | Qué hacer |
|---|---|
| Xiaomi | Necesario para que el monitoreo se reinicie tras reiniciar o actualizar el teléfono. Toca y activa el inicio automático para SwitchGuard. |
| Samsung | Samsung puede poner la app en suspensión. En Batería, elige "Sin restricciones" y añade SwitchGuard a "Apps que nunca se suspenden" (Batería → Límites de uso en segundo plano). |
| Huawei / Honor | En Inicio de aplicaciones, configura SwitchGuard en "Administrar manualmente" y activa Inicio automático, Inicio secundario y Ejecutar en segundo plano. |
| OPPO / realme / OnePlus | Activa el inicio automático para SwitchGuard y, en sus ajustes de batería, permite la actividad en segundo plano. |
| vivo / iQOO | Permite que SwitchGuard se inicie en segundo plano y permite el alto consumo de energía en segundo plano. |
| Tecno / Infinix | Activa el inicio automático para SwitchGuard en Phone Master para que el monitoreo sobreviva a reinicios y limpiezas. |
| ASUS | Permite SwitchGuard en el Administrador de inicio automático para que el monitoreo se reinicie tras reiniciar el teléfono. |

### Otras preguntas

**La vigilancia se detuvo y dice que se cerró mi sesión.**  
Consejo: eWeLink permite una sesión por cuenta. Da a cada teléfono su propia cuenta de eWeLink y comparte los dispositivos desde la app de eWeLink.

**Falta un dispositivo Tuya que compartieron conmigo.**  
¿Falta un dispositivo que compartieron contigo? Tuya solo lista los dispositivos de tus hogares. Pide al propietario que te añada como miembro del hogar en Smart Life (Yo → Gestión del hogar → Añadir miembro).

**Los dispositivos Tuya dejaron de actualizarse.**  
La prueba de Tuya Cloud ha caducado, por lo que los dispositivos Tuya no se están monitoreando. Amplíala en platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Los cambios llegan tarde / no hay actualizaciones instantáneas.**  
Las actualizaciones instantáneas no están disponibles con este App ID; se usan comprobaciones periódicas (los cambios más breves que el intervalo pueden pasar desapercibidos)

**¿Puedo cambiar entre la versión de Google Play y la de GitHub?**  
Sí, pero desinstala antes la otra: están firmadas de forma distinta. Haz una copia en archivo antes de desinstalar y restáurala después.

**¿Cómo pruebo la alarma sin despertar a nadie?**  
Prueba la alarma ahora: con sonido o en silencio, al instante o tras 10 segundos

**¿Por qué el consumo difiere del de la app eWeLink / Smart Life?**  
SwitchGuard calcula el consumo a partir de las lecturas de potencia tomadas mientras el monitoreo está activo, por lo que puede diferir ligeramente de la app del fabricante del dispositivo (eWeLink, Smart Life).

## App para Windows

SwitchGuard para Windows (10 / 11) hace la misma vigilancia en un PC: se queda en la bandeja del sistema, hace sonar una alarma a pantalla completa en el ordenador y comparte las mismas reglas, automatizaciones y funciones de Telegram. Zonas, widgets, reloj y llamadas telefónicas son solo del teléfono.

### Funciones

- Las mismas pestañas: Dispositivos, Alertas, Automatizaciones, Historial, Chat, Ajustes; los comandos de voz del chat usan el dictado por voz de Windows.
- Icono en la bandeja: abrir, iniciar / detener la vigilancia, apagar la alarma, salir; cerrar la ventana no detiene la vigilancia.
- Ventana de alarma a pantalla completa, sonidos integrados (Sirena, Pitido, Pulso, Campanilla) o tu propio WAV / MP3.
- Pantalla de espera y protector de pantalla de Windows: el panel se abre cuando el PC está inactivo.
- Un portátil enchufado sirve como sensor de cortes de luz.
- Actualizaciones desde GitHub con un clic; se conservan ajustes y licencia. Copia en un archivo protegido con contraseña (no se puede restaurar en un teléfono).

### Ajustes solo de Windows

| Ajuste | Para qué sirve |
|---|---|
| Windows | Para que SwitchGuard vigile en este ordenador sin interrupciones. Si alguno está desactivado, se pueden perder alarmas tras un reinicio o mientras el ordenador está en suspensión. |
| Iniciar con Windows | Se abre en la bandeja al iniciar sesión en Windows |
| Mantener el ordenador despierto | Durante el monitoreo, el ordenador no entra en suspensión |
| Usar como protector de pantalla de Windows | Cuando el ordenador está inactivo se abre la pantalla de espera; mover el ratón o pulsar una tecla la cierra |
| Espera | Cuánto tiempo debe estar inactivo el ordenador antes de que se abra la pantalla de espera |
| Ajustes del protector de pantalla de Windows | P. ej., «Mostrar la pantalla de inicio de sesión al reanudar» |
| Abrir pantalla de espera | Pantalla completa; Esc cierra, clic para «Abrir app». La pantalla se mantiene encendida mientras está abierta. |
| Idioma y apariencia: Tema | Claro, oscuro o igual que Windows. |
| Idioma | Elige el idioma de la app, las notificaciones y las alarmas. |
| Actualizaciones en vivo de Tuya en este ordenador | Desactivado: los dispositivos Tuya se comprueban cada 5 minutos |
| Corte de luz cuando este ordenador pasa a batería | Para un portátil enchufado en casa: si funciona con batería durante un minuto, cuenta como corte de luz, incluso sin internet (el mensaje de Telegram se envía al volver la conexión). |
| Este ordenador está en la misma red eléctrica que los dispositivos | Mientras funciona con la red eléctrica no hay corte de luz, así que un dispositivo que se cae se avisa de inmediato en lugar de tras hasta 90 segundos. |
| Cargar desde archivo… | Usa tu propio WAV o MP3 para la alarma o la notificación (primeros 60 segundos, máx. 20 MB). |
| Copia en un archivo… | Todos los ajustes, claves de cuenta y sonidos en un archivo protegido con contraseña |
| Activar Pro | Sin licencia la app monitorea 1 dispositivo. Introduce tu clave de licencia para monitorear todos tus dispositivos. |

### Instalar en Windows

1. Haz doble clic en el instalador (SwitchGuard-…-Kurulum.exe). No pide permisos de administrador y se instala en unos segundos.
2. Si Windows dice «Windows protegió su PC» (SmartScreen), pulsa Más información → Ejecutar de todas formas. La app aún no está firmada; por eso avisa.
3. SwitchGuard se instala en tu carpeta de usuario (AppData › Local › SwitchGuard) y añade accesos directos al escritorio y al menú Inicio.
4. Se inicia solo en la bandeja (junto al reloj) al iniciar sesión en Windows. Cerrar la ventana no lo detiene; para salir, usa el icono de la bandeja → Salir.
5. Añade tus cuentas en Ajustes → Cuentas (los pasos de Tuya y eWeLink están abajo).
6. Pasar a otro ordenador: en el antiguo, Ajustes → Copia y transferencia → Copia en un archivo; instala en el nuevo, Restaurar desde un archivo y vuelve a iniciar sesión en eWeLink.
7. Para desinstalar: Configuración de Windows → Aplicaciones → Aplicaciones instaladas → SwitchGuard → Desinstalar.
8. Instalar una versión nueva: primero desinstala la anterior (paso 7) y luego ejecuta el nuevo instalador. Tus ajustes, cuentas y sonidos se conservan. (El botón Actualizar de la app lo hace por ti.)

## Compilar

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Las compilaciones de publicación necesitan `keystore.properties` + keystore en la raíz del proyecto (no están en el repositorio).

## Historial de cambios

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Contacto

Preguntas, errores y comentarios (o abre una issue aquí): **switchguardapp@gmail.com**

## Licencia

SwitchGuard es **source-available** (código fuente disponible), no de código abierto. Código fuente: [PolyForm Noncommercial 1.0.0](LICENSE). El uso personal y otros usos no comerciales son libres; vender el código o usarlo comercialmente no está permitido. Las versiones hasta la 2.8.3 inclusive se publicaron con GPL-3.0.

Las versiones oficiales (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) puede usarlas cualquiera, también empresas. Las versiones modificadas no pueden usar el nombre ni el logotipo de SwitchGuard. Licencias comerciales: **switchguardapp@gmail.com**

Sin afiliación, respaldo ni patrocinio de eWeLink, CoolKit o ITEAD. Sonoff y eWeLink son marcas comerciales de sus respectivos propietarios. Tampoco está afiliado a Tuya.
