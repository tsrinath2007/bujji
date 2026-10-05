# UI Specification: Bujji (Clean Notebook Design System)

## 1. Design System Overview
- **Name**: Bujji (Defined in single constant `APP_NAME = "Bujji"`)
- **Theme**: Light theme only (clean, focused, digital notebook feel).
- **Background**: `#FAFAFA`
- **Surface / Cards**: `#FFFFFF` with 24dp rounded corners, subtle border/shadow.
- **Primary Color**: `#2563EB` (Royal Blue)
- **Accent Color**: `#06B6D4` (Cyan)
- **Text Color**: `#111827` (Deep Slate / Dark Charcoal)
- **Status Colors**:
  - Got it (Understood): `#16A34A` (Green)
  - Shaky (Partial): `#EAB308` (Yellow / Amber)
  - Needs work (Misconception / Gap): `#F97316` (Orange / Coral)
  - Not started: `#D1D5DB` (Light Grey)

## 2. Typography
- Font Family: **Inter** bundled in `res/font/inter_*.ttf`.
- Title: 28sp SemiBold
- Subtitle / Headline: 20sp SemiBold
- Body: 16sp Regular
- Caption: 12sp Regular

## 3. Creature: Sprig (Bujji)
- **Evolution Stages**:
  - Seed (Level 1 - 4): Cute round seed with tiny sprout.
  - Sprout (Level 5 - 14): Leafy bulb with expressive eyes.
  - Leafling (Level 15+): Crown of leaves, playful companion.
- **Expressions / Moods**:
  - Sleeping, Curious, Confused, Happy, Proud.
- Vector Canvas drawing with smooth breathing/blinking idle animations.

## 4. Core Navigation & Screens
- **Bottom Navigation**: Home (`/home`), Library (`/library`), Settings (`/settings`) only.
- **Screens**:
  1. **Onboarding** (`/onboarding`): "Teach it. It grows." + Creature illustration + "Get started" button.
  2. **Home** (`/home`): "Good morning. Let's learn something new.", Level & streak chips, Creature center, "Teach me" primary button, active subject card.
  3. **Library** (`/library`): "My Subjects", 2-column grid of subject cards with circular coverage indicator (e.g. "DCN 12 of 40"), "+ New subject" FAB / button, dialog to create subject.
  4. **Subject Hub** (`/subject/{subjectId}`): Tabs for `Sources`, `Topics`, `Brain Map`.
     - *Sources*: List of sources with processing status ("Reading page 12 of 39"), "+ Add source" button (PDF, Camera, Gallery, Paste text).
     - *Topics*: List of topics (3-8 concepts each) with mastery status, tap to teach.
  5. **Teaching** (`/teach/{subjectId}?topicId={topicId}`): Creature top with reactive moods, Conversational dialogue ("Here's a question..."), Voice mic button, "That's all I know" button.
  6. **Summary** (`/summary/{subjectId}?topicId={topicId}`): "Nice work! +120 XP", Level progress bar, Breakdown (Got it, Shaky, Needs work), "Teach the gaps", "Done".
  7. **Brain Map** (`/brain_map/{subjectId}`): Map/List toggle, interactive concept network cards and links. No Obsidian / Markdown mentions anywhere.
  8. **Settings** (`/settings`): On-device model status / Cloud boost info, Offline readiness, Appearance, Storage, Data & Privacy, About, and Build Stamp (`UI v2 · Bujji Clean Notebook`).
