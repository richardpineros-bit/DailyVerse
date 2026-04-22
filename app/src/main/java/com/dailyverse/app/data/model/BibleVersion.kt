package com.dailyverse.app.data.model

enum class BibleVersion(val displayName: String, val apiCode: String, val isBundled: Boolean) {
    KJV("King James Version", "kjv", true),
    NIV("New International Version", "niv", false),
    ESV("English Standard Version", "esv", false),
    NKJV("New King James Version", "nkjv", false),
    NLT("New Living Translation", "nlt", false),
    WEB("World English Bible", "web", true);

    companion fun fromCode(code: String): BibleVersion {
        return entries.find { it.apiCode.equals(code, ignoreCase = true) } ?: KJV
    }
}
