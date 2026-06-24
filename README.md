<h1 align="center">ApnaHisaab</h1>
<h3 align="center">Personal Finance & Bill Tracker</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Android-97C5A0?style=for-the-badge&logo=android&logoColor=1C3B63" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-1C3B63?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=1C3B63" alt="Firebase" />
  <img src="https://img.shields.io/badge/Room_DB-5D5FEF?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room" />
</p>

**ApnaHisaab** is a robust, offline-first personal finance application designed to track daily expenses and manage recurring scheduled bills. Built with modern Android architecture, it provides a seamless edge-to-edge UI, secure cloud authentication, and lightning-fast local data storage.

---

## App Interface

| Authentication | Home Dashboard |
| :---: | :---: |
| <img src="https://github.com/user-attachments/assets/9db4e23c-617f-42f1-a712-7469abcc4d91" width="250"> | <img src="https://github.com/user-attachments/assets/55e2d21f-0467-44b6-b1bc-391634c7f528" width="250"> |
| **Scheduled Reminders** | **Advanced Analytics** |
| <img src="https://github.com/user-attachments/assets/55349d35-265e-4d62-a24e-6f05ed7567fb" width="250"> | <img src="https://github.com/user-attachments/assets/fd0ab23a-8792-485c-8844-5a3e6e51221a" width="250"> |

---

## Core Features (Version 1.0)
* **Secure Authentication:** Integrated Firebase Auth featuring Google Sign-In, Email/Password registration, robust error handling, and auto-login routing.
* **Offline-First Architecture:** Powered by Room Database, ensuring users can log expenses and check budgets instantly without an internet connection.
* **Smart Bill Reminders:** A dedicated scheduling system that tracks exact due dates and custom sorting logic to separate "Paid" vs. "Unpaid" bills.
* **Advanced Analytics:** Interactive graphical representations (Bar & Donut charts) to visualize spending trends across different categories and daily limits.
* **Dynamic UI/UX:** Features full edge-to-edge system bar handling, Material Components, and an intuitive bottom navigation system.
* **Persistent Preferences:** Device-level SharedPreferences for saving user-specific budgets and global currency symbols.

---

## Tech Stack & Architecture
* **Language:** Kotlin
* **UI Toolkit:** XML / Material Design Components
* **Local Database:** Room Database (SQLite abstraction)
* **Backend & Auth:** Firebase Authentication
* **Architecture:** MVVM (Model-View-ViewModel) approach with Repository pattern
* **Concurrency:** Kotlin Coroutines & Flow

---

## Future Scope (Version 2.0 Roadmap)
* **Multi-Profile Management:** Implementing a streaming-app style "Profile Selector" (e.g., Family profiles) on launch to filter local Room queries by User UID.
* **Cloud Syncing:** Firing local Room data to Firebase Firestore for cross-device syncing and backup.

---

## How to Run This Project Locally
*Note: For security purposes, the `google-services.json` file has been removed from this public repository.*

1. Clone this repository:
   ```bash
   git clone [https://github.com/SmeetPandya/ExpenceManagement.git](https://github.com/SmeetPandya/ExpenceManagement.git)
