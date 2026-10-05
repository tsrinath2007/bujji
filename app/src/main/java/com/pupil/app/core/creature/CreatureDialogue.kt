package com.pupil.app.core.creature

/**
 * Centralized dialogues, level thresholds, and progression constants for the Pupil Creature.
 * All text can be customized here.
 */
object CreatureDialogue {

    // Evolution Form Names
    const val FORM_1_NAME = "Sprout"
    const val FORM_2_NAME = "Lumina"
    const val FORM_3_NAME = "Auron"

    // Level Thresholds (Total XP required to reach each level)
    val LEVEL_THRESHOLDS = listOf(
        0,    // Level 1: Sprout
        25,   // Level 2: Sprout
        60,   // Level 3: Lumina (Evolves!)
        110,  // Level 4: Lumina
        180,  // Level 5: Auron (Evolves!)
        260,  // Level 6: Auron
        350,  // Level 7: Auron
        500   // Level 8: Master
    )

    // Dialogue prompts for Creature states
    const val WAITING_PROMPT = "Teach me what you learned!"
    const val WAITING_SUB = "Explain the concepts in your own words, analogies, or examples."

    const val HAPPY_TITLE = "I get it now!"
    const val HAPPY_BODY = "You're an amazing teacher! That made so much sense to me."

    const val CONFUSED_TITLE = "Hmm, I have a doubt..."
    const val CONFUSED_SUB = "Can you help clarify this part for me?"

    const val SLEEPING_TITLE = "Zzz..."
    const val SLEEPING_BODY = "Resting up for our next study session."

    const val EVOLVING_TITLE = "Whoa! I'm evolving!"
    const val EVOLVING_BODY = "Your teaching helped me grow to a new form!"

    // Gentle Streak Messages (No guilt, welcoming tone)
    const val STREAK_NEW = "Welcome to Pupil! Let's learn together."
    const val STREAK_ACTIVE = "Day %d streak! You're making steady progress."
    const val STREAK_WELCOME_BACK = "Welcome back! Ready to pick up right where you left off?"

    // Sleeping tap reactions (waking up progressively)
    val SLEEPING_TAP_REACTIONS = listOf(
        "Zzz... Just 5 more minutes...",
        "*yawns* Is it study time already?",
        "Zzz... I was dreaming about neural pathways...",
        "Huh? Feed me some knowledge to wake me up fully!",
        "*stretches sprout* Okay, okay! I'm listening!"
    )

    // Interactive & playful tap reactions
    val INTERACTIVE_TAP_REACTIONS = listOf(
        "Hi there! What are we conquering today?",
        "*happy bounce* I love learning with you!",
        "Hehe, that tickles! Ready to teach me?",
        "My sprout is tingling with curiosity!",
        "Every time you teach me, my brain grows!",
        "Ask me anything or explain a tricky concept!",
        "I'm all ears! Well... all sprout!",
        "I bet you can explain that hard topic in 2 minutes!",
        "*wiggle* Let's level up together today!",
        "High five! Or high sprout!"
    )

    // Excited tap reactions
    val EXCITED_TAP_REACTIONS = listOf(
        "Yay! Learning mode activated!",
        "Ooh, let's dive into some juicy knowledge!",
        "*spins happily* Teach me, master!",
        "I'm full of energy! Let's make that brain map glow!",
        "Let's crush today's study goals!"
    )

    // Anxious / revision due reactions
    val ANXIOUS_TAP_REACTIONS = listOf(
        "Our memory is fading on a few topics... can we review?",
        "I need a refresher! Don't let the knowledge slip away!",
        "Quick revision now saves hours later!",
        "Let's protect our streak and sharpen our concepts!"
    )

    // Annoyed/funny tap reactions when clicked many times in a row
    val RAPID_TAP_REACTIONS = listOf(
        "Whoa whoa, easy on the pokes! 😂",
        "Boing! Boing! Okay, I'm definitely awake now!",
        "Are you testing my touchscreen reflexes?!",
        "My sprout is getting dizzy! 🌀",
        "Haha okay! Stop poking and let's actually study! 📚"
    )

    fun getFormForLevel(level: Int): String {
        return when {
            level >= 5 -> FORM_3_NAME
            level >= 3 -> FORM_2_NAME
            else -> FORM_1_NAME
        }
    }

    fun getStageForLevel(level: Int): Int {
        return when {
            level >= 5 -> 3
            level >= 3 -> 2
            else -> 1
        }
    }

    fun getLevelForXp(xp: Int): Int {
        var currentLevel = 1
        for (i in LEVEL_THRESHOLDS.indices) {
            if (xp >= LEVEL_THRESHOLDS[i]) {
                currentLevel = i + 1
            } else {
                break
            }
        }
        return currentLevel
    }

    fun getNextLevelXp(level: Int): Int {
        val targetIdx = level // index for level + 1
        return if (targetIdx < LEVEL_THRESHOLDS.size) {
            LEVEL_THRESHOLDS[targetIdx]
        } else {
            LEVEL_THRESHOLDS.last() + (level * 50)
        }
    }

    fun getCurrentLevelBaseXp(level: Int): Int {
        val idx = (level - 1).coerceIn(0, LEVEL_THRESHOLDS.size - 1)
        return LEVEL_THRESHOLDS[idx]
    }
}
