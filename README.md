# 📱 Madusari Log - Digital Downtime Tracker

Landingpage download : https://madusari-tracker.netlify.app/

Aplikasi Android Native berbasis **Kotlin Jetpack Compose** yang dirancang khusus untuk memantau dan mencatat *downtime* mesin produksi secara real-time di **PT. Madusari Foods**.

## 🚀 Fitur Utama
- **Digital Logging:** Menggantikan pencatatan manual berbasis kertas menjadi digital yang lebih akurat.
- **Real-time Sync:** Terintegrasi langsung dengan **Google Sheets** sebagai database melalui **Google Apps Script**.
- **Dashboard Analytics:** Visualisasi tren downtime menggunakan grafik dari **Vico Charts**.
- **Automated Duration:** Perhitungan otomatis durasi downtime berdasarkan jam mulai dan jam selesai.
- **Categorized Issues:** Pilihan kategori masalah yang lengkap (KLIP, SEAL, IJP, DUC, PM, dll) untuk memudahkan filter data.

## 🛠️ Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Asynchronous:** Kotlin Coroutines
- **Database Backend:** Google Sheets API via Apps Script
- **Charts:** Vico Charts Library

## 📸 Tampilan Aplikasi
<p align="center">
  <img src="https://github.com/user-attachments/assets/ce520c4f-f25f-4f61-912b-e432bfcce224" width="300" title="Dashboard Utama" alt="Dashboard Utama">
  <img src="https://github.com/user-attachments/assets/935a29af-3911-4924-ae9c-b508684652bf" width="300" title="Input Log" alt="Input Log">
  <img src="https://github.com/user-attachments/assets/ca82a75a-a870-4e68-882c-36006bd9d550" width="300" title="Detail Data" alt="Detail Data">
</p>

## ⚙️ Cara Instalasi
1. Clone repository ini.
2. Buka project menggunakan **Android Studio Koala** atau versi terbaru.
3. Pastikan sudah menginsta!
l **Git** di perangkat kamu.
4. Untuk mengaktifkan fitur kirim data, isi variabel `GOOGLE_SCRIPT_URL` pada file `MainActivity.kt` di baris 68 dengan URL Web App dari Google Apps Script kamu.
5. Build dan jalankan aplikasi di perangkat Android (Min SDK 24).

## 👨‍💻 Developer
**Ganendra**
*Informatics Engineering Graduate - Amikom University Yogyakarta*

---
*Project ini dikembangkan sebagai solusi digitalisasi laporan downtime produksi secara mandiri.*
