# Orvio Native App

A native Kotlin Android application for Orvio, implementing the same functionality as the React Native version but with native Android components.

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
│   │   ├── java/com/orvio/app/
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

The app connects to the Orvio backend API at `https://backend-orvio.pavit.xyz` for authentication, API key management, and device registration. 