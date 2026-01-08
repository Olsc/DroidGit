# DroidGit 📱💻

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DroidGit** es un potente servidor Git para Android. Convierte tu dispositivo móvil en un servidor de alojamiento Git totalmente funcional, permitiéndote gestionar repositorios de código, usuarios y permisos directamente desde tu teléfono o tablet.

Accesible a través de **HTTP**, DroidGit facilita compartir código y colaborar en cualquier lugar. (Nota: Actualmente solo se admite el protocolo HTTP; el soporte para SSH podría añadirse en futuras versiones)

[English](https://github.com/Olsc/DroidGit/blob/main/README.md) | [中文](https://github.com/Olsc/DroidGit/blob/main/docs/README_zh.md) | [Español](https://github.com/Olsc/DroidGit/blob/main/docs/README_es.md) | [Français](https://github.com/Olsc/DroidGit/blob/main/docs/README_fr.md) | [日本語](https://github.com/Olsc/DroidGit/blob/main/docs/README_ja.md) | [한국어](https://github.com/Olsc/DroidGit/blob/main/docs/README_ko.md) | [Русский](https://github.com/Olsc/DroidGit/blob/main/docs/README_ru.md)

![DroidGit Preview](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_1.jpg)

![DroidGit Screenshot](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_2.jpg)

---

## ✨ Características Principales

### 🚀 Funcionalidad Principal
- **Soporte del Protocolo HTTP**: Aloja repositorios Git utilizando el protocolo **Smart HTTP**.
- **Operaciones Git**: Soporte completo para comandos Git estándar: `clone`, `push`, `pull`, `fetch`.
- **Gestión de Usuarios**: Crea múltiples usuarios con contraseñas y permisos dedicados.
- **Gestión de Repositorios**: Crea, elimina y edita repositorios a través de la aplicación Android o la Consola Web.

### 🌐 Consola de Gestión Web
- **Interfaz Web**: Gestiona tu servidor desde cualquier navegador (PC/Móvil) en `http://<ip-del-dispositivo>:<puerto>/`.
- **Explorador de Repositorios**: Visualiza archivos, directorios e historial de commits.
- **Renderizado de Markdown**: Renderiza automáticamente archivos `README.md` para la documentación del proyecto.
- **Resaltado de Sintaxis**: Visualiza el código con un hermoso resaltado de sintaxis.
- **Vista Previa de Archivos**: Previsualiza imágenes, vídeos, audio y archivos de texto directamente en el navegador.
- **Ediciones Rápidas**: Actualiza descripciones y configuraciones de los repositorios desde la interfaz web.

### 🛠 Integración del Sistema
- **Gestión de Servicios**: Se ejecuta como un servicio en segundo plano.
- **Conciencia de WiFi**: Inicio/parada automático basado en la conectividad WiFi.

---

## 📖 Guía de Inicio Rápido

### 1. Iniciar el Servidor
1.  Abre DroidGit en tu dispositivo Android.
2.  Toca el icono del botón de encendido para **Iniciar** el servidor.
3.  Anota la dirección IP y el puerto mostrados (ej., `192.168.1.5:8080`).

### 2. Acceder a la Consola Web
1.  En tu ordenador, abre un navegador.
2.  Navega a `http://<ip-del-dispositivo>:<puerto>/` (ej., `http://192.168.1.5:8080/`).
3.  Verás la **Consola Web de DroidGit**.

### 3. Crear un Repositorio
1.  En la Consola Web, haz clic en **New Repository**.
2.  Introduce un nombre (ej., `mi-proyecto`) y una descripción.
3.  Haz clic en **Create**.

### 4. Clonar y Empujar (Clone & Push)
Ahora puedes usar Git en tu ordenador para interactuar con el repositorio.

**Usando HTTP:**
```bash
# Clonar el repositorio vacío
git clone http://<ip-del-dispositivo>:<puerto>/mi-proyecto.git

# O añadir como remoto a un proyecto existente
cd mi-proyecto
git init
git remote add origin http://<ip-del-dispositivo>:<puerto>/mi-proyecto.git
git add .
git commit -m "Commit inicial"
git push -u origin master
```

---

## 🛡️ Licencia y EULA

### Licencia de Código Abierto
Este proyecto está bajo la licencia **Apache License, Version 2.0**. Eres libre de usar, modificar y distribuir este software bajo los términos de la Licencia Apache. Consulta el archivo [LICENSE](../LICENSE) para más detalles.

### Acuerdo de Licencia de Usuario Final (EULA)
DroidGit se adhiere a los principios internacionales de **Paz, Respeto e Igualdad**.
Al usar este software, aceptas:
- Usarlo de acuerdo con las leyes locales y los estándares internacionales de conducta en internet.
- **NO** usarlo para discursos de odio, discriminación, violencia o promoción de contenido ilegal (CSAM, terrorismo, etc.).
- Respetar la privacidad y los derechos de propiedad intelectual.

---

## 🏗 Arquitectura
- **Núcleo del Servidor**: NanoHTTPD (HTTP)
- **Motor Git**: Eclipse JGit.
- **Base de Datos**: ORMLite (SQLite).

---
<br>
<br>

# ♥ Lista de Contribuidores

[![Lista de Contribuidores](https://contrib.rocks/image?repo=Olsc/DroidGit)](https://github.com/Olsc/DroidGit/graphs/contributors)
