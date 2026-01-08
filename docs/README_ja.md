# DroidGit 📱💻

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DroidGit** は、Android 用の強力な Git サーバーアプリです。モバイルデバイスを完全に機能する Git ホスティングサーバーに変え、携帯電話やタブレットから直接コードリポジトリ、ユーザー、権限を管理できます。

**HTTP** プロトコル経由でアクセス可能な DroidGit は、外出先でのコード共有とコラボレーションを容易にします。（注：現在 HTTP プロトコルのみがサポートされています。SSH サポートは将来のバージョンで追加される可能性があります）

[English](https://github.com/Olsc/DroidGit/blob/main/README.md) | [中文](https://github.com/Olsc/DroidGit/blob/main/docs/README_zh.md) | [Español](https://github.com/Olsc/DroidGit/blob/main/docs/README_es.md) | [Français](https://github.com/Olsc/DroidGit/blob/main/docs/README_fr.md) | [日本語](https://github.com/Olsc/DroidGit/blob/main/docs/README_ja.md) | [한국어](https://github.com/Olsc/DroidGit/blob/main/docs/README_ko.md) | [Русский](https://github.com/Olsc/DroidGit/blob/main/docs/README_ru.md)

![DroidGit Preview](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_1.jpg)

![DroidGit Screenshot](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_2.jpg)

---

## ✨ 主な機能

### 🚀 コア機能
- **HTTP プロトコルのサポート**: **Smart HTTP** プロトコルを使用して Git リポジトリをホストします。
- **Git 操作**: 標準的な Git コマンドをフルサポート：`clone`, `push`, `pull`, `fetch`。
- **ユーザー管理**: 専用のパスワードと権限を持つ複数のユーザーを作成できます。
- **リポジトリ管理**: Android アプリまたは Web コンソールを介してリポジトリを作成、削除、編集できます。

### 🌐 Web 管理コンソール
- **Web インターフェース**: ブラウザ（PC/モバイル）から `http://<デバイスIP>:<ポート>/` でサーバーを管理できます。
- **リポジトリブラウザ**: ファイル、ディレクトリ、コミット履歴を視覚的に閲覧できます。
- **Markdown レンダリング**: プロジェクト文書として `README.md` ファイルを自動的に表示します。
- **構文ハイライト**: 美しい構文ハイライトでコードを表示します。
- **ファイルプレビュー**: 画像、動画、音声、テキストファイルをブラウザ内で直接プレビューできます。
- **クイック編集**: Web UI からリポジトリの説明や設定を更新できます。

### 🛠 システム統合
- **サービス管理**: バックグラウンドサービスとして実行されます。
- **WiFi 認識**: WiFi 接続状況に基づいて自動的に開始/停止します。

---

## 📖 クイックスタートガイド

### 1. サーバーを起動する
1.  Android デバイスで DroidGit を開きます。
2.  電源ボタンアイコンをタップしてサーバーを **開始** します。
3.  表示されている IP アドレスとポート（例：`192.168.1.5:8080`）をメモします。

### 2. Web コンソールにアクセスする
1.  コンピュータでブラウザを開きます。
2.  `http://<デバイスIP>:<ポート>/`（例：`http://192.168.1.5:8080/`）に移動します。
3.  **DroidGit Web Console** が表示されます。

### 3. リポジトリを作成する
1.  Web コンソールで、**New Repository** をクリックします。
2.  名前（例：`my-project`）と説明を入力します。
3.  **Create** をクリックします。

### 4. クローンとプッシュ
コンピュータの Git を使用してリポジトリとやり取りできるようになります。

**HTTP を使用する場合:**
```bash
# 空のリポジトリをクローンする
git clone http://<デバイスIP>:<ポート>/my-project.git

# または、既存のプロジェクトにリモートとして追加する
cd my-project
git init
git remote add origin http://<デバイスIP>:<ポート>/my-project.git
git add .
git commit -m "Initial commit"
git push -u origin master
```

---

## 🛡️ ライセンスと EULA

### オープンソースライセンス
このプロジェクトは **Apache License, Version 2.0** の下でライセンスされています。Apache ライセンスの条件に従って、このソフトウェアを自由に使用、変更、配布できます。詳細は [LICENSE](../LICENSE) をご覧ください。

### エンドユーザーライセンス契約 (EULA)
DroidGit は、**平和、尊重、平等** の国際原則を遵守します。
このソフトウェアを使用することにより、以下に同意したものとみなされます：
- 現地の法律および国際的なインターネット行動規範を遵守して使用すること。
- ヘイトスピーチ、差別、暴力、または違法コンテンツ（CSAM、テロリズムなど）の助長に使用し**ない**こと。
- プライバシーと知的財産権を尊重すること。

---

## 🏗 アーキテクチャ
- **サーバーコア**: NanoHTTPD (HTTP)
- **Git エンジン**: Eclipse JGit.
- **データベース**: ORMLite (SQLite).

---
<br>
<br>

# ♥ コントリビューターリスト

[![コントリビューターリスト](https://contrib.rocks/image?repo=Olsc/DroidGit)](https://github.com/Olsc/DroidGit/graphs/contributors)
