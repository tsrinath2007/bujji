# Bujji — AI Active Recall Study Companion (Android)

> **Creator**: [@sesbuilds](https://x.com/sesbuilds)  
> **Platform**: Android 10+ (API 29+)  
> **Architecture**: Single-Activity MVVM + Jetpack Compose + Material 3 + Supabase Cloud + Google Identity.

---

## 1. Overview & Concept
Bujji is an intelligent study companion where students study material and **teach** a responsive, animated creature by voice or text using the Feynman Technique. 

- **Feynman Technique Engine**: Bujji evaluates students' spoken and written explanations, diagnosing mental gaps and analogies rather than simple keyword memorization.
- **Dynamic Knowledge Graph**: Visualized with Canvas with pinch-zoom, pan, and live mastery tracking.
- **Creature Evolution**: A procedural companion with responsive animations, mood states, XP leveling, and daily streaks.
- **Supabase Cloud Sync**: Effortless cloud sync for profiles, XP, and mastered topics.

---

## 2. Hardware & System Requirements
- **OS**: Android 10 (API 29) to Android 15 (Target SDK 35)
- **RAM**: Minimum 6 GB (8 GB+ recommended)
- **Storage**: ~200 MB free storage

---

## 3. Quickstart & Development

### Via Gradle & ADB (Recommended)
1. Connect your Android device via USB and enable **USB Debugging** in Developer Options.
2. Verify device connection:
   ```bash
   adb devices
   ```
3. Build and run:
   ```powershell
   .\gradlew.bat installDebug
   ```

### Manual APK Installation
The compiled debug APK is located at:
```
app/build/outputs/apk/debug/app-debug.apk
```
You can sideload it manually:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Where to Put the Gemma LLM Model File

Pupil uses the **MediaPipe GenAI Task API** with quantized weights (such as `gemma-2b-it-gpu-int4.bin` or `gemma-2b-it-cpu-int4.bin`).

### Primary Model Location (App External Files Dir)
No root or system permissions required:
```bash
# 1. Create the models directory:
adb shell mkdir -p /sdcard/Android/data/com.pupil.app/files/models/

# 2. Push your quantized Gemma 2B model file:
adb push gemma-2b-it-gpu-int4.bin /sdcard/Android/data/com.pupil.app/files/models/gemma-2b-it-gpu-int4.bin
```

### Alternative Location
```bash
adb push gemma-2b-it-gpu-int4.bin /data/local/tmp/model.bin
```

> **MOCK MODE Graceful Fallback**: If the model weights are not present on the device, Pupil automatically boots in **Mock Mode** with a prominent orange banner. Every feature (OCR, speech-to-text, knowledge graph, creature animations, spaced revision, vault export) remains fully operational.

---

## 5. Step-by-Step Demo Flow

Follow these steps to demonstrate every capability of Pupil:

### Step 1: Onboarding & First Launch
- Launch **Pupil**.
- Experience the 3-slide onboarding introduction (*Teach to Master*, *100% On-Device AI*, *Growth & Knowledge Vault*).
- Tap **"Get Started"** (or **"Skip"**).

### Step 2: Instant Demo Chapter (No PDF required)
- Navigate to the **Library** tab (4th tab).
- Tap **"Load Demo Chapter (15 Concepts)"**.
- This instantly populates a curated chapter on *Photosynthesis & Cellular Respiration* with 15 interconnected concepts, pre-populating 3 Mastered, 2 Partial, 1 Gap, and 9 Unstudied concepts, generating `.md` notes in `/vault` and awarding initial creature XP.

### Step 3: Explore the Knowledge Graph
- Tap the **Graph** tab (3rd tab).
- Test **pinch-to-zoom** and **drag-to-pan** across the 15 connected concept nodes.
- Notice status colors:
  - 🟢 **Green**: Mastered (e.g. Chlorophyll, Glycolysis, Mitochondria)
  - 🟡 **Yellow**: Due for Spaced Revision
  - 🟠 **Amber**: Partial understanding (e.g. Light Reactions)
  - 🔴 **Red**: Misconception / Gap (e.g. Krebs Cycle)
- Tap any node to open the **Concept Bottom Sheet**, previewing its meaning, student quotes, and the generated Obsidian Markdown note (`.md`).

### Step 4: Home & Animated Creature
- Switch to the **Home** tab (1st tab).
- See your animated procedural creature blinking, breathing, and floating.
- View your **Level**, **XP Bar**, **Gentle Streak** counter, and the **"Due for Revision"** card.
- Use the **"+7 Days (Debug)"** button to simulate time passing and observe nodes turning yellow as revision intervals (7/14/30 days) become due.

### Step 5: Teach & Conceptual Grading
- Tap **"Teach Now"** or select a section in the **Teach** tab.
- Speak your explanation using the **Microphone** button (runs offline Android `SpeechRecognizer`), or tap **"Sample Explanation"** / type in the text box.
- Tap **"Grade Understanding"**.
- The on-device LLM evaluates the explanation. In the **Result Screen**:
  - Review the conceptual status and strict student quote evidence.
  - Test the **"I Disagree"** re-grade button.
  - Review the **Gap Report** ("You covered X of Y concepts") and tap **"Teach the Gaps"** to practice missed concepts.

### Step 6: Settings, Offline Proof & Vault Export
- Tap the **Settings icon** (top-right of Home).
- Inspect the **MediaPipe LLM Status**: model file path, file size, active engine, latency (last call & 10-call average), and peak RAM footprint.
- Inspect the **Offline & Privacy Proof**: confirms runtime verification that `android.permission.INTERNET` is not requested, and displays Airplane Mode status.
- Tap **"Export Vault (.zip)"** to share the complete Obsidian markdown vault via the Android share sheet.
