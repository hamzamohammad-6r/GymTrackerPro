# GymTrackerPro

### Your Fitness. Your Progress. Your Journey.

GymTrackerPro is an Android-based fitness management application designed to help users organise workout routines, manage exercises, set fitness goals and monitor their training progress.

The application combines workout planning, exercise management, progress tracking and personal fitness information in one mobile platform.

---

## 📱 Project Overview

GymTrackerPro aims to make fitness management more organised by providing users with tools to plan workouts, record exercise information and review their fitness activities.

**Key objectives:**
- Organise and manage workout routines.
- Browse and manage exercises.
- Record sets, repetitions and workout information.
- Track completed workouts and training history.
- Set and manage personal fitness goals.
- Monitor weekly activity and workout consistency.
- Maintain personal profile information.

---

## ✨ Features

### 🏠 Dashboard
- Central dashboard with application navigation.
- Workout summaries and activity information.
- Weekly progress and workout streak information.

### 💪 Workout Management
- Create, view, edit and delete workouts.
- Organise workouts by date and training day.
- Mark workouts as completed.
- View scheduled workout information.

### 🏋️ Exercise Library
- Browse available exercises.
- View exercise information and instructions.
- Explore different exercise categories.

### 📋 Workout Exercise Tracking
- Assign exercises to individual workouts.
- Record sets and repetitions.
- Store workout-specific exercise information.

### 🎯 Fitness Goals
- Create and manage fitness goals.
- Store personal fitness targets.
- Review saved goal information.

### 📈 Workout History & Progress
- Review completed workouts.
- Monitor weekly workout activity.
- Track workout consistency and streaks.

### 👤 Profile Management
- Save and update personal information.
- Manage profile pictures.
- Store fitness-related profile details.

### 🎵 Workout Music
- Select audio files from the device.
- Associate selected music with workouts.

### 🔐 Authentication
- Login and registration screens.
- Password and email input validation.
- Session-based login state.
- Logout functionality.
- Protected dashboard navigation.

*Backend connectivity and end-to-end authentication are still being completed and tested.*

---

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Android application logic |
| XML | User interface layouts |
| Android Studio | Development environment |
| Android SDK | Android application development |
| Room Persistence Library | Local database management |
| SQLite | Local data storage |
| Retrofit | HTTP API communication |
| Gradle | Build configuration |
| Git & GitHub | Version control and project hosting |

**Design:** Midnight Blue theme with electric-blue accents.

---

## 🗄️ Database Architecture

GymTrackerPro uses the Room Persistence Library over SQLite for local data persistence.

The application includes six main database entities:

| Entity | Purpose |
|---|---|
| User | Stores user profile information |
| Workout | Stores workout information |
| Exercise | Stores exercise details |
| WorkoutExercise | Connects workouts and exercises, including training details |
| Goal | Stores fitness goals |
| WorkoutMusic | Stores workout music associations |

The `WorkoutExercise` entity acts as an association between workouts and exercises.

---

## 🏗️ Application Architecture

The application follows a layered Android architecture:

1. **Presentation Layer:** Java Activities and XML layouts handle screens and user interactions.
2. **Application Logic:** Java classes handle validation, navigation and application operations.
3. **Data Layer:** Room DAOs manage database operations.
4. **Local Database:** SQLite stores application information on the device.
5. **API Communication:** Retrofit provides the Android client with an interface for communicating with the backend.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio.
- Android SDK.
- Java development environment.
- Gradle build tools.
- Android device or emulator.
- Git.

### Installation

**1. Clone the repository**

```bash
git clone (https://github.com/hamzamohammad-6r/GymTrackerPro.git)
```

**2. Open the project**

Open the cloned GymTrackerPro folder in Android Studio.

**3. Sync Gradle**

Allow Android Studio to download dependencies and complete Gradle Sync.

**4. Configure the backend**

If testing API authentication, ensure the backend is running and configure the Retrofit base URL in the Android project.

For a physical Android device, use your development computer's local network IP address instead of `localhost`.

**5. Run the application**

Connect an Android device or start an emulator, then click Run in Android Studio.

---

## 📲 System Requirements

| Requirement | Specification |
|---|---|
| Platform | Android |
| Minimum SDK | API 23 (Android 6.0) |
| Target SDK | API 36 |
| Development IDE | Android Studio |
| Programming language | Java |
| Layout system | XML |
| Local database | SQLite through Room |

---

## 🧪 Testing

The project has undergone functional testing, including:

- Application launch and navigation.
- Workout creation, editing and deletion.
- Exercise selection and assignment.
- Workout completion tracking.
- Workout history.
- Weekly progress and streak information.
- Goal creation and persistence.
- Profile information persistence.
- Workout music assignment.
- Local database persistence.
- Android APK generation and installation.

Authentication and backend connectivity are undergoing further integration and verification.

---

## 🔮 Future Improvements

Planned enhancements include:

- Advanced workout analytics and visual charts.
- Exercise performance tracking and personal records.
- Nutrition and calorie tracking.
- Advanced workout sessions and rest timers.
- Fitness achievements and challenges.
- Cloud-based data synchronisation.
- Improved account security.
- Workout reminders and notifications.
- Exportable fitness progress reports.

---

## 🎨 User Interface

GymTrackerPro uses a Midnight Blue design system featuring:

- Dark navy backgrounds.
- Electric-blue accent colours.
- Modern workout cards.
- Consistent navigation.
- Clear typography and visual hierarchy.

---

## 👨‍💻 Developer

**Hamza Mohammad**

BSc Information Technology  
Richfield College

GitHub: [hamzamohammad-6r](https://github.com/hamzamohammad-6r)

---

## 📄 Project Status

**Active Development**

GymTrackerPro is an ongoing Android application development project. Core fitness-management functionality has been implemented, while authentication integration and additional enhancements continue to be developed.

---

## 📜 License

This project is developed for educational and portfolio purposes. A formal open-source licence can be added when the project is prepared for public distribution.

---

*Built with Java, Android Studio and a passion for technology and fitness.*
