# Secure-Data-Wiping (SecureErase Pro Enterprise)

Open-source Java application delivering defense-grade storage sanitization, zero-residual mathematical verification, and regulatory compliance attestation.

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)]()
[![Standards](https://img.shields.io/badge/Standards-NIST%20800--88%20%7C%20DoD%205220.22--M-red.svg)]()

> 📘 **Looking for the exhaustive architecture and file-by-file study manual?**  
> Check out the complete [PROJECT_STUDY_GUIDE.md](file:///Users/pranaash31/IdeaProjects/DWproject/PROJECT_STUDY_GUIDE.md).

---

## 🚀 Key Features

* **Defense-Standard Wiping**: Implements NIST SP 800-88 Rev. 1 (Clear/Purge), US DoD 5220.22-M (3-Pass), US DoD 5220.22-M ECE (7-Pass), British HMG IS5, German BSI-VS, and Canadian RCMP TSSIT OPS-II.
* **Mathematical Zero-Residual Verification**: Computes real-time Shannon Entropy ($H(X)$) across raw sectors to prove zero residual bits remain ($H(X) = 0.0000\text{ bits/byte}$).
* **Multi-OS Safety Shield**: Real-time interceptor blocking root/boot drives (`disk0`, `C:`, `/dev/sda`, active OS mount points).
* **Cryptographic Tamper-Evident Ledger**: SQLite WAL database chained with sequential SHA-256 block hashes (`prev_record_hash` -> `record_hash`), anchored to a Genesis root.
* **Tamper-Proof PDF Certificates**: RSA-2048 digitally signed certificates generated via Apache PDFBox with live ZXing QR codes linking to an embedded verification web portal.
* **Device-Aware Thermal Throttling**: Real-time temperature tracking with automatic cooldown pauses (`kill -STOP`/`kill -CONT`) to preserve NAND flash and magnetic media.
* **Hardware Quarantine & DIN 66399 Orders**: Flags bad sectors/failing LBAs, aborts unsafe operations, and generates formal physical destruction orders (magnetic degaussing or cross-cut shredding).
* **ESG Sustainability Accounting**: Calculates Scope 3 greenhouse gas avoidance, e-waste diverted in kg, and tree seedling equivalents.
* **Multi-Channel Telemetry UI**: Real-time I/O Oscilloscope (MB/s, IOPS, latency, bus saturation), colorblind-friendly Sector Heatmap matrix, and spotlight command palette (`Ctrl/Cmd + K`).

---

## 🏗️ Quick Start

### 1. Prerequisites
* JDK 17+
* Apache Maven 3.8+

### 2. Run Locally
```bash
cd DataWiping
mvn javafx:run
```

### 3. Run Test Suites
```bash
cd DataWiping
mvn test
```

### 4. Build Standalone Uber-JAR
```bash
cd DataWiping
mvn clean package
# Binary saved to: DataWiping/target/usb-sanitizer-1.0-SNAPSHOT-all.jar
```

### 5. Build Native Installers
```bash
# macOS DMG:
./packaging/package-mac.sh 1.0.0

# Linux AppImage / DEB:
./packaging/package-linux.sh 1.0.0

# Windows:
packaging\package-win.bat 1.0.0
```

---

## 📂 Core Package Structure

| Package | Purpose |
| :--- | :--- |
| `com.sanitizer.engine` | Core raw block overwriting (`dd`), metrics telemetry, and Shannon Entropy verification |
| `com.sanitizer.shield` | Safety shield for OS boot disk protection, partition unmounting, and worker concurrency |
| `com.sanitizer.detector` | OSHI device discovery, S.M.A.R.T. health scoring, and dynamic thermal policies |
| `com.sanitizer.policy` | International wiping standards registry and JSON profile import/export |
| `com.sanitizer.crypto` | RSA-2048 keypair generation and SHA256withRSA digital signing |
| `com.sanitizer.db` | Transactional SQLite audit database with SHA-256 blockchain ledger integrity engine |
| `com.sanitizer.pdf` | Compliance certificate PDF generator with embedded ZXing QR code |
| `com.sanitizer.server` | Embedded zero-dependency HTTP server (port 8080) for QR certificate verification |
| `com.sanitizer.quarantine`| Hardware fault assessment and DIN 66399 physical destruction reporting |
| `com.sanitizer.session` | 3-tier clearance RBAC (Inspector, Supervisor, Chief Auditor) & idle session auto-lock |
| `com.sanitizer.alert` | Asynchronous webhook (Slack/Teams) and SMTP email alert notifications |
| `com.sanitizer.esg` | Scope 3 GHG carbon emissions saved and e-waste prevention calculator |
| `com.sanitizer.report` | Enterprise audit export to CSV, JSON, and Excel SpreadsheetML (XML) |
| `com.sanitizer.gui` | JavaFX MVVM presentation layer with modular views, heatmap, and oscilloscope |

---

## 📖 In-Depth Project Study Guide

For a thorough breakdown of every single file, algorithm, equation, data model, and study plan:  
👉 **[Read the Full Project Study Guide](file:///Users/pranaash31/IdeaProjects/DWproject/PROJECT_STUDY_GUIDE.md)**
