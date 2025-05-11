# Bundl Native App

A native Kotlin Android application for Bundl, implementing the same functionality as the React Native version but with native Android components.

## Features

- Phone number authentication with OTP verification
- API key management (create, delete, test)
- Device registration for Firebase Cloud Messaging
- Dark mode support
- Clean Architecture implementation
- Jetpack Compose UI

## Technologies Used

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI toolkit
- **Material 3**: Design system
- **Hilt**: Dependency injection
- **Retrofit**: Network requests
- **DataStore**: Local storage
- **Firebase Messaging**: Push notifications
- **Coroutines**: Asynchronous programming
- **Flows**: Reactive state management

## Architecture

The app follows Clean Architecture principles with the following layers:

1. **Presentation Layer**: UI components, ViewModels
2. **Domain Layer**: Use cases, models, repository interfaces
3. **Data Layer**: Repository implementations, data sources (remote and local)
4. **DI Layer**: Dependency injection modules

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/bundl/app/
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   ├── remote/
│   │   │   │   └── repository/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   └── repository/
│   │   │   ├── di/
│   │   │   ├── presentation/
│   │   │   │   ├── auth/
│   │   │   │   ├── dashboard/
│   │   │   │   ├── navigation/
│   │   │   │   └── theme/
│   │   │   └── utils/
│   │   └── res/
│   │       ├── drawable/
│   │       └── values/
│   └── androidTest/
└── build.gradle
```

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Connect a device or emulator
4. Run the app

## API Connection

The app connects to the Bundl backend API at `https://backend-bundl.1110777.xyz` for authentication, API key management, and device registration. 

# GitHub Actions Workflow Setup

To enable automatic APK builds and uploads to the APK Manager, you need to set the following GitHub secrets:

1. Go to your GitHub repository settings
2. Navigate to Secrets and Variables > Actions
3. Add the following secrets:
   - `APK_MANAGER_SECRET_KEY`: The secret key for authenticating with the APK Manager (default: "bundl-secret-key-change-in-production")

The workflow will:
1. Build a release APK on every push
2. Create a GitHub release with the APK
3. Upload the APK to the APK Manager at https://apkmanager.1110777.xyz

## Download the latest APK

Users can always download the latest version of the app from:
https://apkmanager.1110777.xyz/bundl/latest.apk

This link is already configured in the website's download buttons. 