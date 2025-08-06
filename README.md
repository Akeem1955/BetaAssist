# Beta Assist

Beta Assist is an Android application that leverages the power of on-device AI to provide a powerful, privacy-focused voice assistant through the Accessibility Service. It allows users to control their device, interact with on-screen content, and get information using voice commands, all while running locally on your device.

## Features

- **Voice-Activated Control**: Use the wake word "Gemma" to start interacting.
- **Application Management**: Open installed applications by name (e.g., "Open Chrome").
- **Screen Interaction**:
  - **Extract Text**: Reads all text content from the current screen.
  - **Translate Text**: Translates the text currently visible on the screen.
  - **Summarize Text**: Summarizes long articles or text on the screen.
  - **Describe Screen**: Provides a visual description of the current screen, including images.
- **Gesture Control**: Perform basic swipe gestures (up, down, left, right) via voice commands.
- **Conversational AI**: Have a general conversation, ask questions, and get detailed responses streamed back to you.
- **On-Device Processing**: Utilizes on-device models for both wake-word detection (Vosk) and LLM/Vision tasks (MediaPipe with Gemma), ensuring privacy and offline functionality.

## Setup and Installation

### Prerequisites

- Android Studio
- An Android device (or emulator)
- `adb` command-line tool

### 1. Model Placement (Crucial Step)

This service relies on a MediaPipe LLM model. You must place the model file on your device in the correct directory before running the app.

The application expects the model at the following path: `/data/local/tmp/llm/model_version.task`.

Use the `adb` to push your model file:

```sh
# First, create the directory
adb shell mkdir -p /data/local/tmp/llm

# Then, push your model file. 
# Replace 'path/to/your/model_version.task' with the actual path on your computer.
adb push path/to/your/model_version.task /data/local/tmp/llm/model_version.task
```

### 2. Build and Install

1.  Open the project in Android Studio.
2.  Build the project to generate an APK.
3.  Install the APK on your Android device.

### 3. Post-Installation Setup

After installing the app, you must manually enable the following for the service to work:

1.  **Grant Permissions**: The app will prompt you to grant the `RECORD_AUDIO` permission. This is required for voice commands.
2.  **Enable Accessibility Service**:
    - Go to your device's **Settings**.
    - Navigate to **Accessibility**.
    - Find and select **Beta Assist** from the list of installed services.
    - Turn it on.

## Usage

Once the service is running, it will greet you. It will then listen for the wake word.

1.  Say **"Gemma"** to activate the assistant.
2.  After the activation sound, state your command.

### Example Commands

- "Open settings"
- "Describe what you see"
- "Summarize the text on the screen"
- "Swipe right"
- "What is the capital of France?"
