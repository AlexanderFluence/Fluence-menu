# Fluence Menu

Menú de aplicaciones para autoradio Android con estilo inspirado en CarPlay:
fondo oscuro, íconos grandes de esquinas redondeadas en páginas que se deslizan
de lado, y una barra lateral con la hora y tus apps favoritas.

## Cómo se usa

- **Tocar un ícono:** abre la app.
- **Deslizar a los lados:** cambia de página.
- **Mantener presionado un ícono:** fijarlo en la barra lateral (hasta 4), ocultarlo del menú o ver su información.
- **Botón de cuadrícula (abajo en la barra):** vuelve a la primera página.
- **Mantener presionado el botón de cuadrícula:** volver a mostrar apps ocultas o elegir esta app como pantalla de inicio.

La primera vez, la barra se llena sola con Spotify, ZLink y Maps o Waze si están instaladas.

## Cómo obtener el APK

1. Crea un repositorio nuevo en GitHub (por ejemplo `fluence-menu`).
2. Sube todos estos archivos, incluida la carpeta `.github`.
3. La compilación empieza sola en la pestaña **Actions** y tarda unos 3 minutos.
4. Al terminar, el APK aparece en **Releases** (columna derecha del repositorio) como `FluenceMenu.apk`.
5. Descárgalo, cópialo a una USB y ábrelo en la radio para instalarlo.

Si la pestaña Actions no muestra nada, la carpeta `.github` no se subió (algunos
teléfonos ocultan las carpetas que empiezan con punto). Créala a mano: **Add file →
Create new file**, nombre `.github/workflows/build.yml`, y pega el contenido de ese archivo.

## Usarlo como pantalla de inicio

Al presionar el botón de inicio de la radio, Android preguntará qué launcher usar.
Elige **Fluence Menu** y luego **Siempre**.
