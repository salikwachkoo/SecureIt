# SecureIt - Android Secure Vault App

A secure password manager and document vault built for Android that keeps your sensitive information protected with strong encryption and biometric authentication.

## What This App Does

SecureIt is designed to be your personal digital safe. It stores passwords, secure notes, and important documents in an encrypted format that only you can access. The app uses your device's fingerprint sensor or PIN to unlock, ensuring that even if someone gets your phone, they can't access your data without your biometric or PIN.

## Key Features

### Password Management
- Store website URLs, usernames, and passwords securely
- Generate strong, random passwords when you need them
- Copy passwords to clipboard with one tap
- Organize passwords by categories

### Secure Notes
- Keep private notes, ideas, or any text information
- Add tags and categories for easy organization
- All content is encrypted and protected

### Document Storage
- Upload and store PDF documents securely
- View documents within the app
- Documents are copied to app-private storage for security

### Security Features
- PIN-based authentication with biometric fallback
- Root detection to prevent compromised devices
- Background app monitoring for additional security
- Screen recording detection (configurable)

## How It Works

The app uses Android's built-in encryption capabilities to secure your data. When you first open the app, you'll set up a PIN and optionally enable fingerprint authentication. This PIN is used to encrypt and decrypt your data locally on your device.

Your passwords, notes, and documents are stored in an encrypted database that's only accessible when you're authenticated. The app also includes several security checks to ensure your device hasn't been compromised.

## Getting Started

1. Download and install the app
2. Set up your PIN during first launch
3. Optionally enable fingerprint authentication
4. Start adding your passwords, notes, and documents

## Technical Details

- Built with Kotlin and Jetpack Compose
- Uses Room database for local storage
- Implements Material Design 3 components
- Supports Android 6.0 (API 23) and above
- Uses Hilt for dependency injection

## Privacy and Security

All your data stays on your device. Nothing is sent to external servers or cloud services. The encryption keys are derived from your PIN and stored securely in Android's Keystore system.

## Development Status

This is currently in development as an MVP (Minimum Viable Product). The core functionality for password management, secure notes, and document storage is implemented. Additional features like cloud backup, sharing, and advanced security options are planned for future versions.

## Building the Project

To build this project:

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on a device or emulator

## Dependencies

The app uses several Android libraries including:
- Jetpack Compose for the UI
- Room for database operations
- Hilt for dependency injection
- Biometric authentication APIs
- PDF rendering capabilities

## Contributing

This is a personal project, but feedback and suggestions are welcome. If you find bugs or have ideas for improvements, feel free to open an issue.

## License

This project is for personal use and educational purposes.
