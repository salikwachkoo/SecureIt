package com.mohammadsalik.secureit.core.security

data class StrengthResult(
    val score: Int, // 0..100
    val label: String,
    val suggestions: List<String>
)

object PasswordStrength {
    private val commonPasswords = setOf(
        "123456", "password", "123456789", "12345", "qwerty", "abc123", "111111", "123123",
        "password1", "iloveyou", "admin", "welcome"
    )

    private val abusivePasswords = setOf(
        "fcukyou", "fuckyou", "sucks", "bullshit"
    )

    fun evaluate(password: String): StrengthResult {
        if (password.isBlank()) return StrengthResult(0, "Empty", listOf("Enter a password"))

        var score = 0
        val suggestions = mutableListOf<String>()

        val length = password.length
        score += when {
            length >= 20 -> 40
            length >= 16 -> 32
            length >= 12 -> 24
            length >= 8 -> 16
            length >= 6 -> 8
            else -> 0
        }
        if (length < 12) suggestions += "Use at least 12 characters"

        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val varietyCount = listOf(hasLower, hasUpper, hasDigit, hasSpecial).count { it }
        score += varietyCount * 12

        if (!hasUpper) suggestions += "Add uppercase letters"
        if (!hasLower) suggestions += "Add lowercase letters"
        if (!hasDigit) suggestions += "Add numbers"
        if (!hasSpecial) suggestions += "Add special characters"

        if (hasSequentialChars(password)) {
            score -= 10
            suggestions += "Avoid sequential characters"
        }
        if (hasRepeatedGroups(password)) {
            score -= 10
            suggestions += "Avoid repeated patterns"
        }

        if (password.lowercase() in commonPasswords) {
            score = 10
            suggestions += "Avoid common passwords"
        }

        if (password.lowercase() in abusivePasswords) {
            score -= 10
            suggestions += "You cant use abusive words as password, they are too common"
        }

        // Clamp 0..100
        if (score < 0) score = 0
        if (score > 100) score = 100

        val label = when {
            score >= 80 -> "Very strong"
            score >= 60 -> "Strong"
            score >= 40 -> "Fair"
            score >= 20 -> "Weak"
            else -> "Very weak"
        }

        return StrengthResult(score, label, suggestions)
    }

    private fun hasSequentialChars(text: String): Boolean {
        if (text.length < 3) return false
        for (i in 0 until text.length - 2) {
            val a = text[i].code
            val b = text[i + 1].code
            val c = text[i + 2].code
            if ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1)) return true
        }
        return false
    }

    private fun hasRepeatedGroups(text: String): Boolean {
        val regex = Regex("(.{2,})\\1+")
        return regex.containsMatchIn(text)
    }
}
