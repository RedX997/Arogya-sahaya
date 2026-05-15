# 🚀 Getting Started with Arogya Sahaya

Follow these instructions to set up and run the Arogya Sahaya project on your local machine for development and testing.

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:
- **Java Development Kit (JDK) 11 or higher**
- **Node.js (v18.x or later)** & npm
- **Android Studio (Giraffe or newer)**
- **Git**
- A **PostgreSQL** instance (Local or Cloud like [Neon.tech](https://neon.tech))

---

## 🛠️ Backend Setup (Node.js Server)

1. **Navigate to the server directory:**
   ```bash
   cd server
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Configure Environment Variables:**
   - Copy `.env.example` to `.env`:
     ```bash
     cp .env.example .env
     ```
   - Update `.env` with your database credentials and a secret key:
     ```env
     DATABASE_URL="postgresql://user:password@localhost:5432/arogyasahaya?schema=public"
     JWT_SECRET="your_secure_random_string_here"
     PORT=3000
     ```

4. **Initialize the Database:**
   - Generate Prisma Client:
     ```bash
     npx prisma generate
     ```
   - Push the schema to your database:
     ```bash
     npx prisma db push
     ```

5. **Start the server:**
   - For development (with auto-reload):
     ```bash
     npm run dev
     ```
   - The server will be running at `http://localhost:3000`.

---

## 📱 Mobile App Setup (Android)

1. **Open the project in Android Studio:**
   - Launch Android Studio and select `Open`.
   - Navigate to the root directory of the repository and click `OK`.

2. **Sync Project with Gradle Files:**
   - Wait for Android Studio to finish indexing and syncing the Gradle dependencies.

3. **Configure the API Base URL:**
   - Locate the network configuration (usually in `ApiService.kt` or `Constants.kt`).
   - If running on an emulator and connecting to a local server, use:
     `http://10.0.2.2:3000/`
   - If connecting to a deployed server, use the actual URL.

4. **Run the App:**
   - Select an Android Virtual Device (AVD) or connect a physical device.
   - Click the **Run** button (green play icon) or press `Shift + F10`.

---

## 🧪 Common Commands

| Task | Command |
| :--- | :--- |
| **Backend Lint** | `npm run lint` (if configured) |
| **Prisma Studio** | `npx prisma studio` (to view DB data) |
| **Build Android APK**| `./gradlew assembleDebug` |
| **Run Android Tests**| `./gradlew test` |

---

## 🆘 Troubleshooting

- **Server Connection Failed**: Ensure your server is running and the Android emulator is pointing to `10.0.2.2` instead of `localhost`.
- **Database Errors**: Double-check your `DATABASE_URL` in the `.env` file and ensure the PostgreSQL service is active.
- **Gradle Sync Issues**: Go to `File > Invalidate Caches / Restart` in Android Studio.

---
*For more technical details, refer to [BACKEND_INTEGRATION.md](./BACKEND_INTEGRATION.md)*
