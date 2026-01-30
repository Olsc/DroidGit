# DroidGit 📱💻

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DroidGit** is a powerful Git server for Android. It turns your mobile device into a fully functional Git hosting server, allowing you to manage code repositories, users, and permissions directly from your phone or tablet.

Accessible via **HTTP**, DroidGit makes it easy to share code and collaborate on the go. (Note: Currently only HTTP protocol is supported, SSH support may be added in future versions)


[English](https://github.com/Olsc/DroidGit/blob/main/README.md) | [中文](https://github.com/Olsc/DroidGit/blob/main/docs/README_zh.md) | [Español](https://github.com/Olsc/DroidGit/blob/main/docs/README_es.md) | [Français](https://github.com/Olsc/DroidGit/blob/main/docs/README_fr.md) | [日本語](https://github.com/Olsc/DroidGit/blob/main/docs/README_ja.md) | [한국어](https://github.com/Olsc/DroidGit/blob/main/docs/README_ko.md) | [Русский](https://github.com/Olsc/DroidGit/blob/main/docs/README_ru.md)

![DroidGit Preview](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_1.jpg)

![DroidGit Screenshot](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_2.jpg)

---

## ✨ Key Features

### 🚀 Core Functionality
- **HTTP Protocol Support**: Host Git repositories using **Smart HTTP** protocol.
- **Git Operations**: Full support for standard Git commands: `clone`, `push`, `pull`, `fetch`.
- **Repository Management**: Create, delete, and edit repositories via the Android App or Web Console.

### 🌐 Web Management Console
- **Web Interface**: Manage your server from any browser (PC/Mobile) at `http://<device-ip>:<port>/`.
- **Repository Browser**: Browse files, directories, and commit history visually.
- **Markdown Rendering**: Automatically renders `README.md` files for project documentation.
- **Syntax Highlighting**: View code with beautiful syntax highlighting.
- **File Preview**: Preview images, videos, audio, and text files directly in the browser.
- **Quick Edits**: Update repository descriptions and settings from the web UI.

### 🛠 System Integration
- **Service Management**: Run as a background service.
- **WiFi Awareness**: Auto-start/stop based on WiFi connectivity.

---

## 📖 Quick Start Guide

### 1. Start the Server
1.  Open DroidGit on your Android device.
2.  Tap the power button icon to **Start** the server.
3.  Note the IP address and port displayed (e.g., `192.168.1.5:8080`).

### 2. Access Web Console
1.  On your computer, open a browser.
2.  Navigate to `http://<device-ip>:<port>/` (e.g., `http://192.168.1.5:8080/`).
3.  You will see the **DroidGit Web Console**.

### 3. Create a Repository
1.  In the Web Console, click **New Repository**.
2.  Enter a name (e.g., `my-project`) and description.
3.  Click **Create**.

### 4. Clone & Push
You can now use Git on your computer to interact with the repository.

**Using HTTP:**
```bash
# Clone the empty repository
git clone http://<device-ip>:<port>/my-project.git

# Or add as remote to existing project
cd my-project
git init
git remote add origin http://<device-ip>:<port>/my-project.git
git add .
git commit -m "Initial commit"
git push -u origin master
```

---

## 🛡️ License & EULA

### Open Source License
This project is licensed under the **Apache License, Version 2.0**. You are free to use, modify, and distribute this software under the terms of the Apache License. See [LICENSE](LICENSE) for details.

### End-User License Agreement (EULA)
DroidGit adheres to international principles of **Peace, Respect, and Equality**.
By using this software, you agree to:
- Use it in compliance with local laws and international internet conduct standards.
- **NOT** use it for hate speech, discrimination, violence, or promoting illegal content (CSAM, terrorism, etc.).
- Respect privacy and intellectual property rights.

---

## 🏗 Architecture
- **Server Core**: NanoHTTPD (HTTP)
- **Git Engine**: Eclipse JGit.
- **Database**: ORMLite (SQLite).

---
<br>
<br>

# ♥ Contributors

[![Contributors](https://contrib.rocks/image?repo=Olsc/DroidGit)](https://github.com/Olsc/DroidGit/graphs/contributors)
