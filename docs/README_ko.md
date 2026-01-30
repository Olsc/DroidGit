# DroidGit 📱💻

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DroidGit**은 Android용 강력한 Git 서버입니다. 귀하의 모바일 장치를 완전한 기능을 갖춘 Git 호스팅 서버로 전환하여 휴대폰이나 태블릿에서 직접 코드 저장소, 사용자 및 권한을 관리할 수 있습니다.

**HTTP**를 통해 액세스 가능한 DroidGit은 이동 중에도 간편하게 코드를 공유하고 협업할 수 있도록 합니다. (참고: 현재 HTTP 프로토콜만 지원되며, SSH 지원은 향후 버전에서 추가될 수 있습니다.)

[English](https://github.com/Olsc/DroidGit/blob/main/README.md) | [中文](https://github.com/Olsc/DroidGit/blob/main/docs/README_zh.md) | [Español](https://github.com/Olsc/DroidGit/blob/main/docs/README_es.md) | [Français](https://github.com/Olsc/DroidGit/blob/main/docs/README_fr.md) | [日本語](https://github.com/Olsc/DroidGit/blob/main/docs/README_ja.md) | [한국어](https://github.com/Olsc/DroidGit/blob/main/docs/README_ko.md) | [Русский](https://github.com/Olsc/DroidGit/blob/main/docs/README_ru.md)

![DroidGit Preview](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_1.jpg)

![DroidGit Screenshot](https://raw.githubusercontent.com/Olsc/DroidGit/refs/heads/main/docs/image_2.jpg)

---

## ✨ 주요 기능

### 🚀 핵심 기능
- **HTTP 프로토콜 지원**: **Smart HTTP** 프로토콜을 사용하여 Git 저장소를 호스팅합니다.
- **Git 작업**: 표준 Git 명령에 대한 전체 지원 (`clone`, `push`, `pull`, `fetch`).
- **저장소 관리**: Android 앱 또는 웹 콘솔을 통해 저장소를 생성, 삭제 및 편집할 수 있습니다.

### 🌐 웹 관리 콘솔
- **웹 인터페이스**: 임의의 브라우저(PC/모바일)에서 `http://<장치-IP>:<포트>/`를 통해 서버를 관리할 수 있습니다.
- **저장소 브라우저**: 파일, 디렉토리 및 커밋 기록을 시각적으로 탐색할 수 있습니다.
- **Markdown 렌더링**: 프로젝트 문서를 위해 `README.md` 파일을 자동으로 렌더링합니다.
- **구문 강조**: 아름다운 구문 강조와 함께 코드를 볼 수 있습니다.
- **파일 미리보기**: 이미지, 비디오, 오디오 및 텍스트 파일을 브라우저에서 직접 미리 볼 수 있습니다.
- **빠른 편집**: 웹 UI에서 저장소 설명 및 설정을 업데이트할 수 있습니다.

### 🛠 시스템 통합
- **서비스 관리**: 백그라운드 서비스로 실행됩니다.
- **WiFi 인식**: WiFi 연결 상태에 따라 자동으로 시작/중지됩니다.

---

## 📖 빠른 시작 가이드

### 1. 서버 시작
1.  Android 장치에서 DroidGit을 엽니다.
2.  전원 버튼 아이콘을 눌러 서버를 **시작**합니다.
3.  표시된 IP 주소와 포트(예: `192.168.1.5:8080`)를 메모합니다.

### 2. 웹 콘솔 접속
1.  컴퓨터에서 브라우저를 엽니다.
2.  `http://<장치-IP>:<포트>/`(예: `http://192.168.1.5:8080/`)로 이동합니다.
3.  **DroidGit Web Console**이 표시됩니다.

### 3. 저장소 생성
1.  웹 콘솔에서 **New Repository**를 클릭합니다.
2.  이름(예: `my-project`)과 설명을 입력합니다.
3.  **Create**를 클릭합니다.

### 4. 클론 및 푸시 (Clone & Push)
이제 컴퓨터에서 Git을 사용하여 저장소와 상호 작용할 수 있습니다.

**HTTP 사용 시:**
```bash
# 빈 저장소 클론
git clone http://<장치-IP>:<포트>/my-project.git

# 또는 기존 프로젝트에 원격으로 추가
cd my-project
git init
git remote add origin http://<장치-IP>:<포트>/my-project.git
git add .
git commit -m "Initial commit"
git push -u origin master
```

---

## 🛡️ 라이선스 및 EULA

### 오픈 소스 라이선스
이 프로젝트는 **Apache License, Version 2.0**에 따라 라이선스가 부여됩니다. Apache 라이선스 조건에 따라 이 소프트웨어를 자유롭게 사용, 수정 및 배포할 수 있습니다. 자세한 내용은 [LICENSE](../LICENSE)를 참조하십시오.

### 최종 사용자 라이선스 계약 (EULA)
DroidGit은 **평화, 존중, 평등**의 국제 원칙을 준수합니다.
이 소프트웨어를 사용함으로써 귀하는 다음에 동의하게 됩니다:
- 현지 법률 및 국제 인터넷 행동 표준을 준수하여 사용합니다.
- 혐오 표현, 차별, 폭력 또는 불법 콘텐츠(CSAM, 테러 등)를 조장하는 데 사용하지 **않습니다**.
- 개인 정보 및 지적 재산권을 존중합니다.

---

## 🏗 아키텍처
- **서버 코어**: NanoHTTPD (HTTP)
- **Git 엔진**: Eclipse JGit.
- **데이터베이스**: ORMLite (SQLite).

---
<br>
<br>

# ♥ 기여자 목록

[![기여자 목록](https://contrib.rocks/image?repo=Olsc/DroidGit)](https://github.com/Olsc/DroidGit/graphs/contributors)
