package com.pupil.app.core.json

import kotlinx.serialization.json.Json
import java.util.Stack

object JsonRepair {

    val jsonInstance = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    /**
     * Cleans and repairs common malformations produced by quantized LLMs:
     * - Strips markdown code blocks (```json ... ```)
     * - Locates the outer JSON object { ... }
     * - Cleans trailing commas before closing braces/brackets
     * - Balances unclosed brackets and quotation marks
     */
    fun repair(rawText: String): String {
        var text = rawText.trim()

        // 1. Remove markdown code fences
        val codeBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
        val match = codeBlockRegex.find(text)
        if (match != null) {
            text = match.groupValues[1].trim()
        }

        // 2. Extract outermost JSON object { ... }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1).trim()
        } else if (firstBrace != -1) {
            text = text.substring(firstBrace).trim()
        }

        // 3. Remove trailing commas before closing braces or brackets: , } -> } and , ] -> ]
        text = text.replace(Regex(",\\s*\\}"), "}")
        text = text.replace(Regex(",\\s*\\]"), "]")

        // 4. Balance braces and brackets if model cut off early
        text = balanceJson(text)

        return text
    }

    private fun balanceJson(input: String): String {
        val stack = Stack<Char>()
        var inQuotes = false
        var isEscaped = false

        for (c in input) {
            if (c == '\\' && !isEscaped) {
                isEscaped = true
                continue
            }
            if (c == '"' && !isEscaped) {
                inQuotes = !inQuotes
            }
            if (!inQuotes) {
                when (c) {
                    '{' -> stack.push('}')
                    '[' -> stack.push(']')
                    '}' -> if (stack.isNotEmpty() && stack.peek() == '}') stack.pop()
                    ']' -> if (stack.isNotEmpty() && stack.peek() == ']') stack.pop()
                }
            }
            isEscaped = false
        }

        val repaired = StringBuilder(input)
        if (inQuotes) {
            repaired.append('"')
        }
        while (stack.isNotEmpty()) {
            repaired.append(stack.pop())
        }
        return repaired.toString()
    }
}
