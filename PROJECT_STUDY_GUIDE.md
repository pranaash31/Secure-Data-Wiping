# 🛡️ Secure-Data-Wiping (SecureErase Pro Enterprise)
### *Defense-Grade Storage Media Sanitization, Cryptographic Ledger & Hardware Lifecycle Assurance*

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              MILITARY & REGULATORY COMPLIANCE                          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  • NIST SP 800-88 Rev. 1 (Media Sanitization)    • DoD 5220.22-M & DoD ECE (7-Pass)    │
│  • German BSI-VS / BSI TL-03423                  • British HMG Infosec Standard 5      │
│  • DIN 66399 (Physical Media Destruction)        • ISO/IEC 27040:2015 Storage Security │
│  • EU GDPR Article 17 (Right to Erasure)         • HIPAA § 164.310 / FISMA NIST 800-53 │
│  • RSA-2048 Digital Attestation                  • SHA-256 Blockchain Hash Chained DB  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🗺️ Visual Study Index

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 TABLE OF CONTENTS                                      │
├───────────────────────────────────┬────────────────────────────────────────────────────┤
│  1. Executive Summary & Pillars   │  10. Cryptographic Blockchain Ledger               │
│  2. Interactive Navigation Map    │  11. Hardware Quarantine & DIN 66399 Destruction   │
│  3. 7-Tier System Architecture    │  12. PDF Certification & QR Verification Portal    │
│  4. End-to-End Execution Sequence │  13. ESG & ITAD Carbon Mitigation Engine           │
│  5. 3-Layer Host Safety Shield    │  14. RBAC Clearance & Session Governance           │
│  6. S.M.A.R.T. Health Scoring     │  15. Complete Component & File Blueprint           │
│  7. Thermal Throttle Controller   │  16. High-Yield Viva / Defense Masterclass (Q&A)   │
│  8. Multi-Pass Bit Patterns       │  17. Build, Test & CI/CD Pipeline Playbook         │
│  9. Shannon Entropy Verification  │                                                    │
└───────────────────────────────────┴────────────────────────────────────────────────────┘
```

---

## 1. Executive Summary & Pillars

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    THE 6 ARCHITECTURAL PILLARS                                  │
├───────────────────────────────┬───────────────────────────────┬─────────────────────────────────┤
│ 1. PERMANENT DATA ERASURE     │ 2. MATHEMATICAL VERIFICATION  │ 3. CRYPTOGRAPHIC INTEGRITY      │
│ ┌───────────────────────────┐ │ ┌───────────────────────────┐ │ ┌─────────────────────────────┐ │
│ │ Raw direct block I/O via  │ │ │ Real-time Shannon Entropy │ │ │ SHA-256 blockchain hash     │ │
│ │ /dev/rdisk* / \\.\Phys*   │ │ │ H(X) == 0.0000 bits/byte  │ │ │ chaining + RSA-2048 signing │ │
│ └───────────────────────────┘ │ └───────────────────────────┘ │ └─────────────────────────────┘ │
├───────────────────────────────┼───────────────────────────────┼─────────────────────────────────┤
│ 4. HARDWARE QUARANTINE        │ 5. DEFENSIVE SAFETY SHIELDS   │ 6. ESG CIRCULAR ECONOMY         │
│ ┌───────────────────────────┐ │ ┌───────────────────────────┐ │ ┌─────────────────────────────┐ │
│ │ Auto-abort on bad LBAs;   │ │ │ 3-Tier OS boot disk guard;│ │ │ Computes kg e-waste saved   │ │
│ │ DIN 66399 shred/degauss   │ │ │ SIGSTOP/SIGCONT pauses    │ │ │ & Scope 3 CO2 emissions     │ │
│ └───────────────────────────┘ │ └───────────────────────────┘ │ └─────────────────────────────┘ │
└───────────────────────────────┴───────────────────────────────┴─────────────────────────────────┘
```

```mermaid
flowchart LR
    A["Raw Physical Media\n(USB / SSD / HDD)"] --> B["WipeEngine\n(Overwrites LBAs bs=2m)"]
    B --> C["WipeVerifier\n(Shannon Entropy H(X))"]
    C -->|Pass: H(X)=0.0000| D["CryptoSigner\n(RSA-2048 Seal)"]
    C -->|Fail: Bad LBAs| E["QuarantineEngine\n(DIN 66399 Destruction)"]
    D --> F[("AuditDb\n(Blockchain Ledger)")]
    D --> G["PDF Certificate\n(+ QR Verification)"]
    
    style A fill:#f1f5f9,stroke:#64748b,stroke-width:2px
    style B fill:#eff6ff,stroke:#2563eb,stroke-width:2px
    style C fill:#fef3c7,stroke:#d97706,stroke-width:2px
    style D fill:#ecfdf5,stroke:#059669,stroke-width:2px
    style E fill:#fef2f2,stroke:#dc2626,stroke-width:2px
    style F fill:#eff6ff,stroke:#1d4ed8,stroke-width:2px
    style G fill:#f5f3ff,stroke:#7c3aed,stroke-width:2px
```

---

## 2. Interactive Navigation Map

```mermaid
flowchart TD
    START([Application Launch]) --> LOGIN["🔑 LoginView\n(Officer Name, Agency ID, PIN)"]
    
    LOGIN --> GATE{RBAC Clearance Check}
    GATE -->|Inspector / Supervisor / Chief Auditor| HERO["🏠 HeroView / DashboardView\n(System Overview & Quick Launch)"]
    
    HERO --> WIZ["🧙 WipeWizardView\n(5-Step Guided Technician Workflow)"]
    HERO --> BAT["⚡ BatchWipeView\n(Concurrent Multi-Drive Console)"]
    HERO --> WIP["🎯 WipingView\n(Single-Drive Live Oscilloscope)"]
    HERO --> DIA["🩺 DiskDiagnosticView\n(S.M.A.R.T. Health & Interface)"]
    HERO --> AUD["📜 AuditView\n(Ledger History & PDF/CSV/XML Export)"]
    HERO --> VRF["🔍 VerificationPortalView\n(Offline QR & Signature Verifier)"]
    HERO --> CLI["🏢 ClientManagerView\n(ITAD Corporate Client Registry)"]
    HERO --> SET["⚙️ SettingsView\n(Modular System Configuration)"]

    subgraph WIZARD_PIPELINE["5-Step Guided Wizard Pipeline"]
        W1["Step 1: Select Drive"] --> W2["Step 2: S.M.A.R.T. Health"]
        W2 --> W3["Step 3: Sanitization Policy"]
        W3 --> W4["Step 4: Safety Warning Gate"]
        W4 --> W5["Step 5: Live Overwrite & Verification"]
    end
    WIZ -.-> WIZARD_PIPELINE

    subgraph SETTINGS_CARDS["Settings Modular Cards (settings/*)"]
        SC1["A11y / WCAG"] --- SC2["Auto-Lock PIN"]
        SC3["Custom Policies"] --- SC4["Thermal Rules"]
        SC5["Alerts Webhook"] --- SC6["Speech Synthesizer"]
        SC7["Database Backup"] --- SC8["RSA Key Vault"]
    end
    SET -.-> SETTINGS_CARDS

    IDLE([Idle Timeout > 5-15 min]) --> LOCK["🔒 LockScreenOverlay\n(PIN Re-authentication Required)"]
```

---

## 3. 7-Tier System Architecture

```mermaid
graph TB
    subgraph T1["1. PRESENTATION LAYER (JavaFX 21 & CSS)"]
        NAV[NavigationManager Router]
        VIEWS["Views: Wizard | Batch | Wiping | Diagnostics | Audit | Settings"]
        COMPS["Visualizers: Sector Heatmap | I/O Oscilloscope | Thermal Graph"]
        NAV --> VIEWS --> COMPS
    end

    subgraph T2["2. SECURITY & SESSION LAYER"]
        AUTH[LoginView] --> CTX[SessionContext: UserRole] --> ALOCK[SessionAutoLockManager]
    end

    subgraph T3["3. APPLICATION ORCHESTRATION LAYER"]
        SWS[SanitizationWorkflowService]
        AES[AuditExportService]
        SCS[SystemConfigurationService]
    end

    subgraph T4["4. DEFENSIVE HARDWARE SHIELDS"]
        SDS[SystemDiskShield\nBlocks disk0, C:, /dev/sda]
        DLS[DiskLockShield\nUnmounts partitions]
        WPG[WorkerPoolGovernor\nCPU <= 88% & RAM >= 512MB]
    end

    subgraph T5["5. HARDWARE EXECUTION & DIAGNOSTICS"]
        USB[UsbDetector: OSHI Poller]
        SMART[SmartDiagnostics: Health Score 0-100]
        ENG[WipeEngine: dd Process bs=2m]
        TTC[ThermalThrottleController: SIGSTOP/CONT]
        ENG <--> TTC
    end

    subgraph T6["6. VERIFICATION & QUARANTINE"]
        VER[WipeVerifier\nEntropy H(X) & SHA-256]
        QRN[QuarantineEngine\nDIN 66399 Orders]
    end

    subgraph T7["7. CRYPTOGRAPHIC LEDGER & OUTPUT"]
        CRY[CryptoSigner: RSA-2048]
        DB[(AuditDb SQLite WAL\nSHA-256 Hash Chained)]
        PDF[CertificateGenerator + ZXing QR]
        SRV[WebVerificationServer :8080]
        ALT[AlertDispatcher: Webhook/SMTP]
        ESG[EsgCalculator: CO2e / e-Waste]
    end

    T1 --> T2 --> T3 --> T4 --> T5 --> T6
    VER -->|Pass| CRY --> DB & PDF & ALT & ESG
    PDF --> SRV
    VER -->|Fail| QRN
```

---

## 4. End-to-End Execution Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Tech as Technician / Operator
    participant UI as JavaFX View (Wizard/Batch)
    participant Svc as SanitizationWorkflowService
    participant Shield as SystemDiskShield & DiskLockShield
    participant Smart as SmartDiagnostics
    participant Engine as WipeEngine (dd process)
    participant Therm as ThermalThrottleController
    participant Verifier as WipeVerifier
    participant Crypto as CryptoSigner
    participant DB as AuditDb (Ledger)
    participant PDF as CertificateGenerator
    participant Alert as AlertDispatcher

    Tech->>UI: Select Target Drive & Policy (e.g. DoD 5220.22-M)
    UI->>Svc: executeWorkflow(request, callbacks)

    rect rgb(238, 246, 255)
        Note over Svc,Shield: PHASE 1: PRE-FLIGHT SAFETY VALIDATION
        Svc->>Shield: validateTarget(drivePath)
        alt Protected OS Root Disk Detected (disk0, C:, /)
            Shield-->>Svc: THROW SecurityShieldException
            Svc-->>UI: ABORT: Root Disk Protected! Log Alert
        end
        Svc->>Shield: unmountAndLock(drivePath)
        Shield-->>Svc: Drive Unmounted & Locked
        Svc->>Smart: capturePreWipeSnapshot(drivePath)
        Smart-->>Svc: Pre-Wipe Health (Score: 98/100, Temp: 34°C)
    end

    rect rgb(255, 251, 235)
        Note over Svc,Therm: PHASE 2: MULTI-PASS BLOCK OVERWRITING
        Svc->>Engine: wipeDrive(devicePath, policy, callbacks)
        loop For Pass 1 to N (e.g., 0x00 -> 0xFF -> Random)
            Engine->>Engine: Fork OS `dd` command (bs=2m, direct I/O)
            Engine->>UI: Stream live progress, MB/s, ETA, IOPS, Heatmap
            Engine->>Therm: checkTemperature(currentTemp)
            opt Overheating (e.g., HDD >= 52°C)
                Therm->>Engine: Send SIGSTOP (Freeze write process)
                Therm->>UI: Display "Thermal Cooldown Active"
                Therm->>Therm: Poll cooling cycle until Temp <= 42°C
                Therm->>Engine: Send SIGCONT (Resume write execution)
            end
        end
        Engine-->>Svc: Block write passes completed successfully
    end

    rect rgb(240, 253, 244)
        Note over Svc,Verifier: PHASE 3: MATHEMATICAL VERIFICATION
        Svc->>Verifier: verifyDrive(devicePath, mode, callbacks)
        Verifier->>Verifier: Read physical sectors back (bs=2m)
        Verifier->>Verifier: Compute 256-bin Byte Frequencies
        Verifier->>Verifier: Calculate Shannon Entropy H(X) & SHA-256
        alt H(X) == 0.0000 (Pure Zero Residual)
            Verifier-->>Svc: VERIFICATION PASS
        else H(X) > 0.001 OR Bad Sector POSIX EIO Encountered
            Verifier-->>Svc: VERIFICATION FAIL / HARDWARE DEFECT
            Svc->>Svc: Invoke QuarantineEngine (DIN 66399 Destruction Order)
        end
    end

    rect rgb(250, 245, 255)
        Note over Svc,Alert: PHASE 4: CRYPTOGRAPHIC ATTESTATION & EXPORT
        Svc->>Smart: capturePostWipeSnapshot(drivePath)
        Smart-->>Svc: SmartDelta (Post-Health, Wear Delta, Bad LBAs)
        Svc->>Crypto: signAuditPayload(recordData)
        Crypto-->>Svc: RSA-2048 Digital Signature (Base64)
        Svc->>DB: insertRecord(record + SHA-256 Hash Chaining)
        DB-->>Svc: Record ID #N Created
        Svc->>PDF: generateCertificate(record)
        PDF-->>Svc: Sanitization_Certificate_<id>.pdf
        Svc->>Alert: dispatch(WipeSuccessEvent)
        Svc-->>UI: Workflow Complete! Display Certificate & QR Code
    end
```

---

## 5. 3-Layer Host Safety Shield

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        3-LAYER DEFENSE-IN-DEPTH FIREWALL                               │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  LAYER 1: String Signature Filter  ──▶ Blocks disk0, rdisk0, PhysDrive0, C:, /dev/sda  │
│  LAYER 2: Live Mount Point Filter  ──▶ OSHI inspects /, /boot, /System, C:\Windows     │
│  LAYER 3: Geometry Matching Filter ──▶ Blocks partitions sharing parent host geometry  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    REQ([Target Drive Path Received]) --> L1{Layer 1: Hardcoded Root Names}
    L1 -->|Matches: disk0, rdisk0, C:, /dev/sda| BLOCK["🚫 REJECT: Protected System Drive"]
    L1 -->|Pass| L2{Layer 2: Live Mount Points}
    L2 -->|Matches active mount: /, /boot, /etc, C:\Windows| BLOCK
    L2 -->|Pass| L3{Layer 3: Partition Geometry}
    L3 -->|Shares physical disk with boot volume| BLOCK
    L3 -->|Pass| SIZE{Capacity Boundaries Check\n1 GB <= Size <= 128 GB}
    SIZE -->|Out of Range| BLOCK
    SIZE -->|Valid| OK["✅ APPROVED: Pass to DiskLockShield\n(Unmount & Lock Device)"]

    BLOCK --> ALERT["🚨 SecurityAuditLogger: Record Violation\nAlertDispatcher: Notify Administrator"]
```

---

## 6. S.M.A.R.T. Health Scoring

```mermaid
flowchart LR
    subgraph INPUTS["Telemetry Attributes"]
        I1["ID 5: Reallocated Sectors"]
        I2["ID 173/231: Wear Level %"]
        I3["ID 194: Operating Temp °C"]
        I4["ID 199: UDMA CRC Errors"]
        I5["ID 188: Command Timeouts"]
    end

    subgraph MATH["SmartDiagnostics Formula"]
        F["Health Score = 100\n- (ReallocatedSectors * 5)\n- (PendingSectors * 8)\n- (CrcErrors * 2)\n- (WearLevelPenalty)"]
    end

    subgraph TIERS["Drive Classification"]
        T_OK["🟢 HEALTHY (80 - 100)"]
        T_WARN["🟡 WARNING (50 - 79)"]
        T_CRIT["🔴 CRITICAL (0 - 49)"]
    end

    INPUTS --> MATH --> TIERS
```

---

## 7. Thermal Throttle Controller

```mermaid
stateDiagram-v2
    [*] --> NORMAL : Wipe Started

    state NORMAL {
        [*] --> PollingTemp
        PollingTemp --> PollingTemp : Temp < Warning
    }

    NORMAL --> THROTTLED : Temp >= Warning
    state THROTTLED {
        InjectDelay : Inject sleep delays (ms)\nDissipate heat passively
    }

    THROTTLED --> NORMAL : Temp cools below Warning
    THROTTLED --> PAUSED : Temp >= Auto-Pause
    state PAUSED {
        SendSigstop : Send SIGSTOP to dd\nFreeze write head instantly\nSound laboratory warning chime
        WaitCooldown : Poll temperature decay
    }

    PAUSED --> RESUMING : Temp <= Resume Threshold
    state RESUMING {
        SendSigcont : Send SIGCONT to dd\nResume write at exact byte
    }

    RESUMING --> NORMAL : Normal write speeds restored
    NORMAL --> [*] : All Passes Completed
```

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          DEVICE THERMAL SAFETY PROFILES                                │
├─────────────────────┬────────────────────┬────────────────────┬────────────────────────┤
│ Media Physics       │ Warning Threshold  │ Auto-Pause SIGSTOP │ Auto-Resume SIGCONT    │
├─────────────────────┼────────────────────┼────────────────────┼────────────────────────┤
│ 🧲 Magnetic HDD     │ 46°C               │ 52°C               │ 42°C                   │
│ ⚡ SATA SSD         │ 55°C               │ 65°C               │ 50°C                   │
│ 🚀 NVMe SSD         │ 68°C               │ 78°C               │ 60°C                   │
│ 💾 USB Flash Drive  │ 48°C               │ 58°C               │ 44°C                   │
└─────────────────────┴────────────────────┴────────────────────┴────────────────────────┘
```

---

## 8. Multi-Pass Bit Patterns

```mermaid
flowchart TD
    subgraph NIST["NIST SP 800-88 Rev. 1 Clear (1-Pass)"]
        N1["Pass 1: 0x00 (Fixed Zero-Fill)"]
    end

    subgraph DOD["US DoD 5220.22-M NISPOM (3-Pass)"]
        D1["Pass 1: 0x00 (Zero)"] --> D2["Pass 2: 0xFF (One)"] --> D3["Pass 3: PRNG (Cryptographic Random)"]
    end

    subgraph DODECE["US DoD 5220.22-M ECE (7-Pass)"]
        DE1["0x00"] --> DE2["0xFF"] --> DE3["Random"] --> DE4["0x96"] --> DE5["0x00"] --> DE6["0xFF"] --> DE7["Random"]
    end

    subgraph BSI["German BSI-VS / TL-03423 (7-Pass)"]
        B1["0x00"] --> B2["0xFF"] --> B3["0x00"] --> B4["0xFF"] --> B5["0xAA"] --> B6["0x55"] --> B7["Random"]
    end
```

---

## 9. Shannon Entropy Verification

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        SHANNON ENTROPY MATHEMATICAL MODEL                              │
├────────────────────────────────────────────────────────────────────────────────────────┤
│                                  255                                                   │
│                        H(X) = -  ∑   p(x_i) * log2( p(x_i) )                           │
│                                  i=0                                                   │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  • Pure Zero-Fill (Clean):  p(0x00) = 1.0  ──▶  H(X) = 0.0000 bits/byte  (PASS)        │
│  • Data Remanence / Noise:  p(x_i) spread  ──▶  H(X) ~ 7.99 bits/byte   (FAIL)        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    RAW[Read Raw Physical Block Stream via /dev/rdisk*] --> HIST[Build 256-Bin Frequency Histogram\nCounts for 0x00, 0x01, ..., 0xFF]
    HIST --> PROB["Calculate Byte Probabilities:\np(x_i) = Count(x_i) / TotalBytesSampled"]
    PROB --> CALC["Compute Shannon Entropy:\nH(X) = - SUM [ p(x_i) * log2(p(x_i)) ]"]
    CALC --> EVAL{Evaluate H(X)}

    EVAL -->|H(X) == 0.0000\n100% bytes are 0x00| PASS["✅ VERIFICATION PASS\nZero data remanence mathematically attested\nReadback SHA-256 Digest Created"]
    EVAL -->|H(X) > 0.001\nResidual non-zero bits detected| FAIL["❌ VERIFICATION FAIL\nMedia contaminated! Overwrite incomplete!\nTrigger Hardware Quarantine"]
```

---

## 10. Cryptographic Blockchain Ledger

```mermaid
flowchart LR
    subgraph GENESIS["Block #0 (Genesis Root)"]
        G_H["Hash:\n000000000000000000000000..."]
    end

    subgraph B1["Block #1: Record ST2000"]
        P1["Payload: ID=1 | DoD | PASS"]
        PREV1["prev_hash: 0000..."]
        H1["record_hash:\nSHA-256(prev_hash || P1)"]
    end

    subgraph B2["Block #2: Record WD5000"]
        P2["Payload: ID=2 | NIST | PASS"]
        PREV2["prev_hash: H1"]
        H2["record_hash:\nSHA-256(H1 || P2)"]
    end

    subgraph B3["Block #3: Record SAM980"]
        P3["Payload: ID=3 | BSI | PASS"]
        PREV3["prev_hash: H2"]
        H3["record_hash:\nSHA-256(H2 || P3)"]
    end

    GENESIS --> B1 --> B2 --> B3

    style B1 fill:#eff6ff,stroke:#2563eb
    style B2 fill:#eff6ff,stroke:#2563eb
    style B3 fill:#eff6ff,stroke:#2563eb
```

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          TAMPER-DETECTION MECHANISM                                    │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  IF an attacker edits Block #2 (e.g. changing FAIL to PASS in SQLite):                 │
│  1. Block #2's new hash != original H2                                                 │
│  2. Block #3's prev_hash (H2) NO LONGER MATCHES Block #2's new hash                   │
│  3. LedgerIntegrityEngine immediately flags TAMPERED BLOCK at Row ID #2                │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Hardware Quarantine & DIN 66399 Destruction

```mermaid
flowchart TD
    FAIL[Sanitization Failure Detected\nBad LBAs / POSIX EIO / S.M.A.R.T. Failure] --> ASSESS[QuarantineEngine: Assess Media Physics]

    ASSESS --> MEDIA{Storage Media Physics}

    MEDIA -->|Magnetic HDD\nRotational Platters| DEGAUSS[Prescribe MAGNETIC DEGAUSSING\nStandard: NSA/CSS Manual 9-12 / DIN 66399 H-5\nMagnetic Field: >= 10,000 Oersted\nRenders magnetic domains permanently inert]

    MEDIA -->|Solid State / Flash\nNVMe / SATA SSD / USB Flash| SHRED[Prescribe MECHANICAL SHREDDING\nStandard: DIN 66399 E-4 / NIST SP 800-88\nCross-cut Particle Size: <= 2mm\nPhysically disintegrates NAND silicon dies]

    DEGAUSS --> ORDER[Generate DIN 66399 Destruction Work Order PDF]
    SHRED --> ORDER

    ORDER --> OUT["📄 Quarantine_Report_QRN_<timestamp>.pdf\n• Granular Failing LBA Address Table\n• Digital Attestation Hash\n• Custody Transfer & Officer Sign-Off"]
```

---

## 12. PDF Certification & QR Verification Portal

```mermaid
flowchart TD
    REC[Audit Record Sealed] --> SIGN[CryptoSigner: RSA-2048 Digital Sign\nSHA256withRSA -> Base64]
    SIGN --> QR[ZXing Engine: Embed Dynamic QR Code\nhttp://<host>:8080/verify?certId=...&sig=...]
    QR --> PDF[Apache PDFBox: Render Certificate PDF\n- Official Defense Header Banner\n- Drive Serial, Model, Capacity\n- S.M.A.R.T. Pre/Post Health Delta\n- Thermal Telemetry Logs\n- ESG Circular Economy Metrics\n- Embedded ZXing QR Code & RSA Signature]
    PDF --> SERVER[WebVerificationServer: Listening on :8080]

    PHONE([Auditor Scans QR with Phone Camera]) --> SERVER
    SERVER --> PAGE[Mobile Verification Webpage\nDark-mode authenticated certificate view\nLive authentic/tampered badge]
    SERVER --> REST[REST API Endpoint: GET /api/verify\nJSON payload for enterprise compliance]
```

---

## 13. ESG & ITAD Carbon Mitigation Engine

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        CIRCULAR ECONOMY ESG EQUATIONS                                  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  e-Waste Diverted (kg) = N * 1.4 kg * ( Capacity / 500 GB )^0.25                       │
│  Scope 3 GHG Saved (kg CO2e) = N * 12.6 kg * ( Capacity / 500 GB )^0.30                │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart LR
    DRIVE["Sanitized Storage Drive\n(Baseline: 500 GB)"] --> ESG["EsgCalculator Engine"]

    ESG --> M1["♻️ e-Waste Diverted: ~1.4 kg\n(Avoids hazardous landfill toxins)"]
    ESG --> M2["🌱 Scope 3 GHG Saved: ~12.6 kg CO2e\n(Avoids manufacturing new drive)"]
    ESG --> M3["🌲 Tree Seedling Equiv: ~0.21 trees\n(10 years carbon sequestration)"]
    ESG --> M4["⚡ Energy Conserved: ~18.9 kWh\n(Silicon & metals refining saved)"]
```

---

## 14. RBAC Clearance & Session Governance

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          3-TIER RBAC CLEARANCE MATRIX                                  │
├──────────────────────┬──────────────────────┬──────────────────────────────────────────┤
│ Clearance Tier       │ Title                │ Permitted Operations                     │
├──────────────────────┼──────────────────────┼──────────────────────────────────────────┤
│ 🟢 Tier 1            │ INSPECTOR            │ Guided/Batch Wiping, Diagnostics, PDFs   │
│ 🟡 Tier 2            │ SUPERVISOR           │ Custom Policies, Thermal Rules, Alerts   │
│ 🔴 Tier 3            │ CHIEF AUDITOR        │ Key Vault, Ledger Verify, DB Purge/Reset │
└──────────────────────┴──────────────────────┴──────────────────────────────────────────┘
```

```mermaid
flowchart TD
    subgraph T1["Tier 1: INSPECTOR"]
        T1_A[Execute Guided & Batch Wipes]
        T1_B[Run S.M.A.R.T. Diagnostics]
        T1_C[View Telemetry & Oscilloscope]
        T1_D[Generate Single PDF Certificates]
    end

    subgraph T2["Tier 2: SUPERVISOR"]
        T2_A[All Tier 1 Privileges]
        T2_B[Create & Edit Custom Policies]
        T2_C[Adjust Thermal Safety Thresholds]
        T2_D[Configure Alert Webhooks & SMTP]
        T2_E[Export Full Audit Batches]
    end

    subgraph T3["Tier 3: CHIEF AUDITOR"]
        T3_A[All Tier 2 Privileges]
        T3_B[Cryptographic Key Vault Management]
        T3_C[Verify Blockchain Ledger Integrity]
        T3_D[Database Backup & History Purge]
        T3_E[System-Wide SIEM Security Audits]
    end

    T1 --> T2 --> T3
```

---

## 15. Complete Component & File Blueprint

```mermaid
graph TB
    subgraph CORE_SERVICES["Application Service Orchestrators"]
        S1[SanitizationWorkflowService]
        S2[AuditExportService]
        S3[SystemConfigurationService]
    end

    subgraph ENGINE_SHIELDS["Hardware Engine & Safety Shields"]
        E1[WipeEngine]
        E2[WipeVerifier]
        H1[SystemDiskShield]
        H2[DiskLockShield]
        H3[WorkerPoolGovernor]
        D1[UsbDetector]
        D2[SmartDiagnostics]
        D3[ThermalThrottleController]
    end

    subgraph CRYPTO_LEDGER["Cryptography & Audit Ledger"]
        C1[CryptoSigner]
        C2[AuditDb]
        C3[LedgerIntegrityEngine]
        C4[CertificateGenerator]
        C5[WebVerificationServer]
        C6[QuarantineEngine]
    end

    S1 --> H1 & H2 & H3
    S1 --> E1 & E2
    S1 --> D1 & D2 & D3
    S1 --> C1 & C2 & C4 & C6
    S2 --> C2 & C3 & C4
    S3 --> C2 & D3
```

### Complete Source Code File Map:

| Package | Class / File | Architectural Responsibility |
| :--- | :--- | :--- |
| `service` | [SanitizationWorkflowService.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/service/SanitizationWorkflowService.java) | Master orchestrator: coordinates diagnostics, wiping, verification, digital seal, audit, and quarantine |
| `service` | [AuditExportService.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/service/AuditExportService.java) | Asynchronous exporter for CSV, JSON, and Excel SpreadsheetML (XML) reports |
| `service` | [SystemConfigurationService.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/service/SystemConfigurationService.java) | Decoupled configuration orchestrator for database backups, alert webhooks, and thermal rules |
| `engine` | [WipeEngine.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/engine/WipeEngine.java) | Overwrites raw block devices via `dd`, manages worker threads, pauses on thermal events |
| `engine` | [WipeVerifier.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/engine/WipeVerifier.java) | Calculates Shannon Entropy ($H(X)$), verifies 0x00 pattern, produces SHA-256 proof |
| `shield` | [SystemDiskShield.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/shield/SystemDiskShield.java) | Evaluates disk paths, protects OS root partitions (`disk0`, `C:`, `/dev/sda`) |
| `shield` | [DiskLockShield.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/shield/DiskLockShield.java) | Unmounts logical partitions and locks devices against OS automount daemons |
| `shield` | [WorkerPoolGovernor.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/shield/WorkerPoolGovernor.java) | Regulates host CPU load ($\le 88\%$) and memory to throttle parallel wipe dispatching |
| `detector` | [UsbDetector.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/detector/UsbDetector.java) | Background OSHI hardware poller discovering removable pen drives and block devices |
| `detector` | [SmartDiagnostics.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/detector/SmartDiagnostics.java) | Evaluates S.M.A.R.T. attributes, calculates 0-100 health score, tracks wear delta |
| `detector` | [ThermalThrottleController.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/detector/ThermalThrottleController.java) | Computes throttle delays and auto-pauses when media exceeds thermal limits |
| `policy` | [WipePolicyManager.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/policy/WipePolicyManager.java) | Registry for NIST SP 800-88, DoD 5220.22-M, BSI, HMG, RCMP and custom JSON policies |
| `crypto` | [CryptoSigner.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/crypto/CryptoSigner.java) | Generates RSA-2048 keys and digitally signs compliance proofs using SHA256withRSA |
| `db` | [AuditDb.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/db/AuditDb.java) | SQLite WAL database storing complete historical audit records |
| `db` | [LedgerIntegrityEngine.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/db/LedgerIntegrityEngine.java) | Verifies SHA-256 blockchain-style chaining to mathematically detect database tampering |
| `audit` | [SecurityAuditLogger.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/audit/SecurityAuditLogger.java) | Chained security audit trail logging logins, policy changes, and access denials |
| `pdf` | [CertificateGenerator.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/pdf/CertificateGenerator.java) | Creates tamper-proof PDF certificates with Apache PDFBox and embedded ZXing QR code |
| `server` | [WebVerificationServer.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/server/WebVerificationServer.java) | Embedded HTTP server (port 8080) for live mobile QR verification and REST API |
| `quarantine`| [QuarantineEngine.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/quarantine/QuarantineEngine.java) | Assesses hardware write faults and prescribes DIN 66399 degaussing or shredding |
| `quarantine`| [QuarantineReportGenerator.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/quarantine/QuarantineReportGenerator.java) | Generates formal PDF destruction work orders with failing LBA fault logs |
| `session` | [UserRole.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/session/UserRole.java) | Defines Tier 1 Inspector, Tier 2 Supervisor, and Tier 3 Chief Auditor RBAC hierarchy |
| `alert` | [AlertDispatcher.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/alert/AlertDispatcher.java) | Dispatches notifications to Slack/Teams webhooks and SMTP email servers |
| `esg` | [EsgCalculator.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/esg/EsgCalculator.java) | Computes avoided e-waste (kg), CO2e emissions saved, and tree seedling equivalencies |
| `report` | [AuditExporter.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/report/AuditExporter.java) | Exports audit records to CSV, structured JSON, and Excel SpreadsheetML (XML) |
| `gui` | [NavigationManager.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/gui/navigation/NavigationManager.java) | App router, view cache, RBAC enforcer, shortcut dispatcher, and scene manager |
| `gui` | [SettingsView.java](file:///Users/pranaash31/IdeaProjects/DWproject/DataWiping/src/main/java/com/sanitizer/gui/views/SettingsView.java) | Modular settings dashboard container assembling domain cards |

---

## 16. High-Yield Viva, Defense & Interview Masterclass (Q&A)

Each question below includes a **visual flowchart, memory card box, and authoritative model answer**.

---

### Q1: Why write directly to raw block devices (`/dev/rdisk*`) instead of standard file I/O?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: File I/O vs Direct Block I/O                                             │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  Standard File I/O:  App ──▶ VFS ──▶ Page Cache ──▶ Delayed Write ──▶ Remanence Risk!  │
│  Direct Block I/O:   App ──▶ /dev/rdisk* (bs=2m) ──▶ Physical Sectors ──▶ 100% ERASED │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    subgraph WRONG["❌ Standard File I/O (Unsafe)"]
        F1[Application File Write] --> F2[Virtual File System VFS]
        F2 --> F3[OS Kernel Page Cache]
        F3 --> F4[Delayed Lazy Flush to Disk]
        F4 --> F5[RISK: Unwritten blocks, wear-leveling bypass, cached remnants]
    end

    subgraph RIGHT["✅ Direct Block Character Device (SecureErase)"]
        D1[Application dd Process] --> D2["Raw Device: /dev/rdisk* / \\\\.\\PhysicalDrive*"]
        D2 --> D3["Direct Physical Sector Addressing (bs=2m)"]
        D3 --> D4["Bypasses all OS caches & guarantees physical bit-flip on disk"]
    end
```

> **Authoritative Viva Answer**:
> "Standard file I/O operations travel through the OS Virtual File System (VFS), buffer cache, and delayed write queues. The OS kernel may cache writes in RAM or skip blocks via wear-leveling and journaling. In contrast, opening raw character block devices (`/dev/rdisk*` on macOS, `/dev/sd*` on Linux, `\\\\.\\PhysicalDrive*` on Windows) bypasses kernel page caching and sends low-level write commands straight to physical Logical Block Addresses (LBAs). This is an explicit requirement of **NIST SP 800-88 Rev. 1 Section 2.4**."

---

### Q2: How does Shannon Entropy mathematically prove that zero residual data survives?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: Shannon Entropy Range                                                    │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  Clean Zero-Fill  ──▶ All bytes 0x00 ──▶ p(0x00)=1.0 ──▶ H(X) = 0.0000 bits/byte (PASS)│
│  Residual / Noise ──▶ Bytes distributed              ──▶ H(X) ~ 7.99 bits/byte  (FAIL)│
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart LR
    A["Raw Readback Stream"] --> B["Build 256 Bins\n(0x00 to 0xFF)"]
    B --> C["Compute p(x_i)"]
    C --> D["H(X) = -SUM [ p*log2(p) ]"]
    D --> E{"H(X) Score"}
    E -->|0.0000| F["PASS: 100% Zero-Fill"]
    E -->|> 0.001| G["FAIL: Data Remanence Detected"]
```

> **Authoritative Viva Answer**:
> "Shannon Entropy measures average information uncertainty in bits per byte across 256 possible byte values:
> $$H(X) = -\sum_{i=0}^{255} p(x_i) \log_2 p(x_i)$$
> When a drive is cleanly zero-filled, 100% of the bytes read back are `0x00`, so $p(0\text{x}00) = 1.0$ and all other $p(x_i) = 0$. Since $\log_2(1.0) = 0$, the entropy score is **exactly $0.0000\text{ bits/byte}$**. If even a tiny residual data fragment or encryption artifact remains, bytes distribute across bins and entropy jumps to $\approx 7.9-8.0$. Our threshold of $H(X) \le 0.001$ mathematically certifies zero data remanence."

---

### Q3: How do you prevent accidental destruction of the host operating system disk?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: 3-Tier Security Interceptor                                              │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  Filter 1: Path Check    ──▶ Rejects disk0, rdisk0, C:, PhysicalDrive0, /dev/sda       │
│  Filter 2: Mount Check   ──▶ OSHI inspects live mounts: /, /boot, /System, C:\Windows  │
│  Filter 3: Geometry Check──▶ Rejects partitions sharing device geometry with host boot │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    IN[Target Disk Selected] --> T1{Tier 1: Signature Check}
    T1 -->|Matches disk0 / C: / /dev/sda| BLK[BLOCKED IMMEDIATELY]
    T1 -->|Clean| T2{Tier 2: Live Mount Points}
    T2 -->|Mounted as / or C:\Windows| BLK
    T2 -->|Clean| T3{Tier 3: Shared Geometry}
    T3 -->|Shares physical disk with boot disk| BLK
    T3 -->|Clean| ALLOW[APPROVED FOR SANITIZATION]
```

> **Authoritative Viva Answer**:
> "We implement a triple-layer defensive interceptor in `SystemDiskShield.java`:
> 1. **Hardcoded Root Signatures**: Blocks known platform boot names (`disk0`/`rdisk0` on macOS, `PhysicalDrive0`/`C:` on Windows, `/dev/sda`/`/dev/nvme0n1` on Linux).
> 2. **OSHI Live Mount Inspection**: Queries operating system mount tables to detect any partition mounted at `/`, `/boot`, `/System`, `/etc`, or `C:\Windows`.
> 3. **Partition Geometry Correlation**: Rejects secondary partitions that share a parent physical spindle or controller with an active boot volume. Any violation aborts execution and dispatches a security alarm."

---

### Q4: How is your SQLite audit log tamper-evident?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: Cryptographic Blockchain Hash Chain                                     │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  Record N Hash = SHA-256( Record N-1 Hash || Record N Payload Data )                   │
│  Anchored at Genesis Root: 000000000000000000000000000000000000000000000000...         │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart LR
    G["Genesis: 000...000"] --> R1["Record 1\nprev: 000...\nhash: H1"]
    R1 --> R2["Record 2\nprev: H1\nhash: H2"]
    R2 --> R3["Record 3\nprev: H2\nhash: H3"]

    subgraph TAMPER["Attacker Alters Record 2 in DB"]
        T_R2["Record 2 Modified!\nHash changes to H2_BAD"]
        T_R3["Record 3 prev_hash (H2)\n!= H2_BAD\nCHAIN BROKEN!"]
        T_R2 -.-> T_R3
    end

    style T_R2 fill:#fee2e2,stroke:#ef4444
    style T_R3 fill:#fee2e2,stroke:#ef4444
```

> **Authoritative Viva Answer**:
> "We implement a blockchain-style SHA-256 hash chaining architecture in `AuditDb.java` and `LedgerIntegrityEngine.java`. Each row stores:
> $$\text{record\_hash} = \text{SHA-256}(\text{prev\_record\_hash} \parallel \text{record\_payload})$$
> Anchored to a known Genesis hash (`0000...0000`). If an attacker alters any field in SQLite (e.g., turning a `FAIL` into a `PASS` or deleting a record), the cryptographic chain breaks downstream. The `LedgerIntegrityEngine` mathematically traverses the chain, detects tampering, and flags the exact corrupted record ID."

---

### Q5: What happens if a storage drive overheats during wiping?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: Thermal Throttle Auto-Pause & Resume                                     │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  Warning Temp ──▶ Injects sleep delays to dissipate heat passively                    │
│  Pause Temp   ──▶ Sends SIGSTOP to dd (write heads halt, job NOT killed)               │
│  Resume Temp  ──▶ Sends SIGCONT to dd (writing resumes seamlessly from same byte)      │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    T_POLL[Check Drive Temperature] --> T_EVAL{Temperature Level}
    T_EVAL -->|Safe Range| T_NORM[Normal Overwrite Speed]
    T_EVAL -->|>= Warning (e.g. HDD 46°C)| T_THROT[Inject Sleep Delays (Pacing)]
    T_EVAL -->|>= Auto-Pause (e.g. HDD 52°C)| T_PAUSE[Send SIGSTOP to dd\nFreeze Write Process\nSound Audio Alarm]

    T_PAUSE --> T_WAIT[Poll Temperature Decay]
    T_WAIT -->|<= Resume Temp (e.g. HDD 42°C)| T_RESUME[Send SIGCONT to dd\nResume Write Seamlessly]
    T_RESUME --> T_NORM
```

> **Authoritative Viva Answer**:
> "`ThermalThrottleController` evaluates temperature against media-specific thermal policies (HDD pauses at 52°C, SSD at 65°C, NVMe at 78°C). When warning limits are reached, pacing delays dissipate heat passively. If critical limits are reached, the controller sends a **`SIGSTOP`** signal to the underlying `dd` process. This halts disk I/O immediately without aborting the job. Once the drive cools below the resume threshold, it sends **`SIGCONT`** to seamlessly resume block overwriting from the exact byte where it paused."

---

### Q6: Why can't bad sectors simply be overwritten, and what is your Quarantine protocol?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: Defective Media Physics & DIN 66399 Destruction                          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  Bad LBAs reject writes (POSIX EIO) ──▶ Software wipe IMPOSSIBLE                       │
│  Rotational Magnetic HDDs           ──▶ Magnetic Degaussing (>= 10,000 Oersted)        │
│  Silicon Flash / SSDs / USB         ──▶ Mechanical Shredding (<= 2mm particles)        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    EIO[Bad LBA Encountered\nPOSIX EIO Error] --> WHY[Why Software Fails:\nDrive controller reallocates block or rejects write\nSensitive plaintext data remains trapped in bad cells]
    WHY --> DECIDE{Media Physics}
    DECIDE -->|Rotational HDD| DEG[Prescribe Degaussing\nNSA/CSS Manual 9-12\nDIN 66399 H-5\nField >= 10,000 Oe]
    DECIDE -->|SSD / NVMe / Flash| SHRED[Prescribe Mechanical Shredding\nDIN 66399 E-4\nCross-cut particle <= 2mm]
    DEG --> WORK_ORDER[Generate Official DIN 66399 Destruction Order PDF]
    SHRED --> WORK_ORDER
```

> **Authoritative Viva Answer**:
> "Bad sectors are physical media defects (worn NAND flash planes or degraded magnetic oxide). When `dd` attempts to write to a bad LBA, the drive controller returns an unrecoverable `POSIX EIO` hardware error. Because the drive cannot write to these defective sectors, software sanitization cannot guarantee 100% erasure—residual data remains trapped inside. Per **NIST SP 800-88 Rev. 1 § 4.7** and **EU GDPR Article 17**, the drive is quarantined, and the `QuarantineEngine` generates a **DIN 66399 Physical Destruction Work Order** prescribing **Magnetic Degaussing** ($\ge 10,000\text{ Oe}$) for HDDs or **Mechanical Shredding** ($\le 2\text{mm}$) for SSDs and flash."

---

### Q7: How does the offline QR code verification portal work?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: Zero-Dependency Embedded Web Verification                                │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  PDF Generator ──▶ ZXing embeds QR: http://<ip>:8080/verify?certId=...&sig=...         │
│  Smartphone    ──▶ Camera scans QR ──▶ Connects to Sun HttpServer on port 8080         │
│  Server Engine ──▶ Validates RSA-2048 signature ──▶ Displays Authenticity Badge        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart LR
    A["CertificateGenerator\n(Apache PDFBox)"] --> B["ZXing QR Engine\n(Embeds URL in PDF)"]
    B --> C["Client / Auditor\n(Scans with Phone)"]
    C --> D["WebVerificationServer\n(:8080 /verify)"]
    D --> E["CryptoSigner\n(Verifies RSA Sig)"]
    E --> F["Mobile UI Webpage\n(Verified Authentic Badge)"]
```

> **Authoritative Viva Answer**:
> "The application bundles an embedded, zero-dependency HTTP server (`WebVerificationServer.java` utilizing `com.sun.net.httpserver.HttpServer`) listening on port 8080. When a certificate is generated, a dynamic **ZXing QR Code** is embedded in the PDF linking to:
> `http://<host>:8080/verify?certId=<id>&model=<model>&sig=<sig>`
> When an auditor scans the QR code with their mobile phone, the server validates the RSA-2048 digital signature against the audit database and displays a mobile-friendly authenticated certificate preview. It also exposes a REST API (`/api/verify`) for automated enterprise compliance verification."

---

### Q8: What ESG metrics does the application compute?

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│  CONCEPT BOX: ITAD ESG Carbon Avoidance (Per 500GB Drive Reused)                       │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  • e-Waste Diverted:  ~1.4 kg / drive       • Scope 3 CO2e Saved:  ~12.6 kg CO2e / drive│
│  • Energy Conserved:  ~18.9 kWh / drive     • Tree Seedling Equiv: ~0.21 trees / drive  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    INPUT[500GB Storage Device Sanitized & Reused] --> CALC[EsgCalculator Engine]
    CALC --> R1["1.4 kg e-Waste Diverted from Landfills"]
    CALC --> R2["12.6 kg CO2e Emissions Prevented"]
    CALC --> R3["18.9 kWh Manufacturing Energy Conserved"]
    CALC --> R4["0.21 Tree Seedlings Sequestering Carbon for 10 Years"]
```

> **Authoritative Viva Answer**:
> "In IT Asset Disposition (ITAD), physically shredding reusable drives generates e-waste and forces manufacturers to produce replacement hardware. `EsgCalculator.java` models circular economy benefits: For every standard 500GB drive sanitized and reused rather than shredded, it computes:
> * $\approx 1.4\text{ kg}$ of electronic waste diverted from landfills.
> * $\approx 12.6\text{ kg}$ of Scope 3 greenhouse gas ($\text{CO}_2\text{e}$) emissions prevented.
> * $\approx 18.9\text{ kWh}$ of manufacturing energy conserved.
> * $\approx 0.21$ tree seedlings grown for 10 years equivalent.
> These ESG metrics are embedded directly into every PDF compliance certificate and corporate audit report."

---

## 17. Build, Test & CI/CD Pipeline Playbook

```mermaid
flowchart LR
    SRC[Git Source Repo] --> MVN[Maven Compile\nmvn clean compile]
    MVN --> TEST["JUnit 5 Test Suite\n(34 Test Suites / 100% Pass)"]
    TEST --> COV["JaCoCo Code Coverage\n(target/site/jacoco)"]
    COV --> SHADE["Maven Shade Plugin\n(Executable Uber-JAR)"]
    SHADE --> PACK["jpackage Toolchain\n(packaging/*)"]

    PACK --> MAC["🍏 macOS DMG\nUSBSanitizer-1.0.0.dmg"]
    PACK --> WIN["🪟 Windows MSI / EXE\nUSBSanitizer-Setup.exe"]
    PACK --> LNX["🐧 Linux AppImage / DEB\nUSBSanitizer.AppImage"]
```

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          TERMINAL COMMAND QUICK-REFERENCE                              │
├─────────────────────────┬──────────────────────────────────────────────────────────────┤
│ Action                  │ Command                                                      │
├─────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🚀 Run Locally (JavaFX) │ cd DataWiping && mvn javafx:run                              │
│ 🧪 Run All 34 Tests     │ cd DataWiping && mvn test                                    │
│ 📦 Build All-in-One JAR │ cd DataWiping && mvn clean package                           │
│ 🍏 Build macOS DMG      │ ./packaging/package-mac.sh 1.0.0                             │
│ 🐧 Build Linux Packages │ ./packaging/package-linux.sh 1.0.0                           │
│ 🪟 Build Windows MSI    │ packaging\package-win.bat 1.0.0                              │
└─────────────────────────┴──────────────────────────────────────────────────────────────┘
```
