# DroidGit 📱💻

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DroidGit** 是一款强大的 Android Git 服务器应用。它能将您的手机或平板变身为功能完备的 Git 托管服务器，让您可以直接在移动设备上管理代码仓库、用户和权限。

DroidGit 支持 **HTTP** 协议，让您随时随地轻松共享代码和协同工作。(注意：目前仅支持 HTTP 协议，SSH 支持可能会在未来版本中添加)


[English](https://github.com/Olsc/DroidGit/blob/main/README.md) | [中文](https://github.com/Olsc/DroidGit/blob/main/docs/README_zh.md) | [Español](https://github.com/Olsc/DroidGit/blob/main/docs/README_es.md) | [Français](https://github.com/Olsc/DroidGit/blob/main/docs/README_fr.md) | [日本語](https://github.com/Olsc/DroidGit/blob/main/docs/README_ja.md) | [한국어](https://github.com/Olsc/DroidGit/blob/main/docs/README_ko.md) | [Русский](https://github.com/Olsc/DroidGit/blob/main/docs/README_ru.md)

![DroidGit 预览](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_1.jpg)

![DroidGit 截图](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_2.jpg)

---

## ✨ 核心功能

### 🚀 基础功能
- **HTTP 协议支持**: 支持通过 **Smart HTTP** 协议托管 Git 仓库。
- **标准 Git 操作**: 完美支持所有标准 Git 命令：`clone`, `push`, `pull`, `fetch`。
- **用户管理**: 创建多个用户，支持独立的密码和权限配置。
- **仓库管理**: 通过 Android App 或网页控制台创建、删除和编辑仓库。

### 🌐 网页管理控制台
- **Web 界面**: 通过浏览器 (PC/手机) 访问管理后台 `http://<设备IP>:<端口>/`。
- **仓库浏览器**: 可视化浏览文件、目录结构和提交历史。
- **Markdown 渲染**: 自动渲染项目中的 `README.md` 文件，展示精美文档。
- **语法高亮**: 浏览代码时支持多种语言的语法高亮显示。
- **文件预览**: 直接在浏览器中预览图片、视频、音频和文本文件。
- **快速编辑**: 支持在 Web 界面直接更新仓库描述和设置。

### 🛠 系统集成
- **后台服务**: 作为后台服务运行，即便切换应用也能保持连接；提供桌面小组件一键开关。
- **WiFi 智能控制**: 支持根据 WiFi 连接状态自动启动或停止服务。

---

## 📖 快速开始指南

### 1. 启动服务器
1.  在 Android 设备上打开 DroidGit。
2.  点击主界面的电源图标 **启动** 服务器。
3.  记下屏幕上显示的 IP 地址和端口（例如：`192.168.1.5:8080`）。

### 2. 访问网页控制台
1.  在电脑或手机浏览器中输入地址 `http://<设备IP>:<端口>/` (如 `http://192.168.1.5:8080/`)。
2.  如果不设置 HTTPS，大多数浏览器会提示不安全，继续访问即可。
3.  您将看到 **DroidGit Web Console** 管理界面。

### 3. 创建仓库
1.  在网页控制台中，点击 **New Repository** (新建仓库)。
2.  输入仓库名称 (如 `my-project`) 和描述。
3.  点击 **Create** (创建)。

### 4. 克隆与推送 (Clone & Push)
现在您可以在电脑上使用 Git 命令与服务器交互。

**使用 HTTP 协议:**
```bash
# 克隆刚刚创建的空仓库
git clone http://<设备IP>:<端口>/my-project.git

# 或者将现有项目推送到服务器
cd my-project
git init
git remote add origin http://<设备IP>:<端口>/my-project.git
git add .
git commit -m "Initial commit"
git push -u origin master
```

---

## 🛡️ 许可协议与最终用户协议 (EULA)

### 开源许可证
本项目采用 **Apache License, Version 2.0** 许可证。您可以根据 Apache 许可证条款自由使用、修改和分发本软件。详情请参阅 [LICENSE](../LICENSE)。

### 最终用户许可协议 (EULA)
DroidGit 遵循 **和平、尊重和平等** 的国际原则。
使用本软件即表示您同意：
- 遵守当地法律法规以及国际公认的互联网行为准则。
- **不得** 利用本软件散布仇恨言论、歧视、暴力或非法内容（如 CSAM、恐怖主义内容等）。
- 尊重他人的隐私和知识产权。

---

## 🏗 技术架构
- **核心服务**: NanoHTTPD (HTTP)
- **Git 引擎**: Eclipse JGit.
- **数据库**: ORMLite (SQLite).

---
<br>
<br>

# ♥ 贡献者列表

[![贡献者列表](https://contrib.rocks/image?repo=Olsc/DroidGit)](https://github.com/Olsc/DroidGit/graphs/contributors)
