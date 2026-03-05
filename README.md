# Jatra - Passenger App 🗺️🚍

**Jatra** is a smart city bus tracking and navigation ecosystem designed to modernize public transit in Dhaka. This repository contains the source code for the **Passenger Side Android Application**, which allows users to track buses in real-time, find direct routes, and discover multi-leg journeys.

## 🚀 Application Role & Features
The Passenger App is the user interface for the system, responsible for:
* **Real-Time Tracking:** Displaying the live location of buses on an interactive map.
* **Route Discovery:** Identifying buses that connect a user's origin and destination.
* **Navigation:** Visualizing the path, estimated time of arrival (ETA), and nearest boarding points.
* **Trip Planning:** Planning efficient journeys with multi-leg route suggestions.
* **Offline-First Routing:** Calculating paths using pre-loaded datasets to minimize API costs.

## 🧠 Algorithms Used
* **Breadth-First Search (BFS):** Used to find "Connecting Routes" (transfer options) when no single bus connects the start and end points directly.
* **String Normalization Algorithm:** A sanitization layer that standardizes user input (e.g., handling "Mirpur-10" vs. "Mirpur 10") to ensure accurate route matching.
* **Update Throttling:** A mechanism that buffers incoming real-time data and updates the UI in 1000ms batches, preventing map stutter/lag during high-traffic periods.
* **Directional Vector Analysis:** Validates whether a bus is "Incoming" or "Outgoing" relative to the user to prevent suggesting buses moving in the wrong direction.

## ⚡ Challenges Solved
* **High API Costs:** Implemented an "Offline-First" strategy that uses stored route coordinates to draw paths on the map, reducing reliance on the paid Google Directions API to near zero.
* **UI Performance:** Solved severe lag caused by 50+ concurrent bus updates by decoupling the data listener from the UI thread using a buffering system.
* **Duplicate Bus Entries:** Fixed list "flickering" issues by implementing an "Upsert" (Update/Insert) logic based on unique license plates rather than blindly appending new data.

## 🛠 Tech Stack
* **Language:** Java (Android Native) / Kotlin
* **Framework:** Android SDK
* **Map Interface:** Google Maps SDK
* **Data Synchronization:** Firebase Realtime Database
* **UI Optimization:** Android `DiffUtil` for efficient list rendering
* **Build System:** Gradle

## ⚙️ Getting Started

### Prerequisites
* Android Studio Ladybug or newer.
* JDK 17 or compatible version.

### Installation
1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/enamulhasan248/Jatra-Passenger-Side.git](https://github.com/enamulhasan248/Jatra-Passenger-Side.git)
    ```
2.  **Open in Android Studio:**
    * Launch Android Studio and select "Open".
    * Navigate to the cloned project directory.
3.  **Sync & Build:**
    * Allow Gradle to sync and download dependencies.
    * Ensure your `google-services.json` is configured for Firebase (if applicable).
4.  **Run:**
    * Connect an Android device or launch an emulator.
    * Click "Run" to deploy the application.

## 🤝 Contributing
Contributions are welcome! Please follow these steps:
1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/YourFeature`).
3.  Commit your changes (`git commit -m 'Add some feature'`).
4.  Push to the branch (`git push origin feature/YourFeature`).
5.  Open a Pull Request.


## License
[Not licensed yet]
