package com.dailyverse.app.util

object BibleBookUtil {

    /**
     * Complete list of Bible books in canonical order.
     */
    val ALL_BOOKS = listOf(
        // Old Testament
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
        "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles",
        "Ezra", "Nehemiah", "Esther", "Job", "Psalms",
        "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah",
        "Jeremiah", "Lamentations", "Ezekiel", "Daniel",
        "Hosea", "Joel", "Amos", "Obadiah", "Jonah",
        "Micah", "Nahum", "Habakkuk", "Zephaniah",
        "Haggai", "Zechariah", "Malachi",
        // New Testament
        "Matthew", "Mark", "Luke", "John", "Acts",
        "Romans", "1 Corinthians", "2 Corinthians", "Galatians",
        "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
        "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus",
        "Philemon", "Hebrews", "James", "1 Peter",
        "2 Peter", "1 John", "2 John", "3 John",
        "Jude", "Revelation"
    )

    val OLD_TESTAMENT = ALL_BOOKS.subList(0, 39)
    val NEW_TESTAMENT = ALL_BOOKS.subList(39, 66)

    /**
     * Chapter counts for each book.
     */
    val CHAPTER_COUNTS = mapOf(
        "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36, "Deuteronomy" to 34,
        "Joshua" to 24, "Judges" to 21, "Ruth" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
        "1 Kings" to 22, "2 Kings" to 25, "1 Chronicles" to 29, "2 Chronicles" to 36,
        "Ezra" to 10, "Nehemiah" to 13, "Esther" to 10, "Job" to 42, "Psalms" to 150,
        "Proverbs" to 31, "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66,
        "Jeremiah" to 52, "Lamentations" to 5, "Ezekiel" to 48, "Daniel" to 12,
        "Hosea" to 14, "Joel" to 3, "Amos" to 9, "Obadiah" to 1, "Jonah" to 4,
        "Micah" to 7, "Nahum" to 3, "Habakkuk" to 3, "Zephaniah" to 3,
        "Haggai" to 2, "Zechariah" to 14, "Malachi" to 4,
        "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21, "Acts" to 28,
        "Romans" to 16, "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6,
        "Ephesians" to 6, "Philippians" to 4, "Colossians" to 4, "1 Thessalonians" to 5,
        "2 Thessalonians" to 3, "1 Timothy" to 6, "2 Timothy" to 4, "Titus" to 3,
        "Philemon" to 1, "Hebrews" to 13, "James" to 5, "1 Peter" to 5,
        "2 Peter" to 3, "1 John" to 5, "2 John" to 1, "3 John" to 1,
        "Jude" to 1, "Revelation" to 22
    )

    fun getChapterCount(book: String): Int {
        return CHAPTER_COUNTS[book] ?: 1
    }

    fun getBookOrder(book: String): Int {
        return ALL_BOOKS.indexOf(book) + 1
    }

    fun isValidReference(book: String, chapter: Int, verse: Int = 1): Boolean {
        val maxChapters = CHAPTER_COUNTS[book] ?: return false
        if (chapter < 1 || chapter > maxChapters) return false
        return true
    }

    fun getAbbreviation(book: String): String {
        return when (book) {
            "Genesis" -> "Gen"
            "Exodus" -> "Exo"
            "Leviticus" -> "Lev"
            "Numbers" -> "Num"
            "Deuteronomy" -> "Deu"
            "Joshua" -> "Jos"
            "Judges" -> "Jdg"
            "Ruth" -> "Rth"
            "1 Samuel" -> "1Sa"
            "2 Samuel" -> "2Sa"
            "1 Kings" -> "1Ki"
            "2 Kings" -> "2Ki"
            "1 Chronicles" -> "1Ch"
            "2 Chronicles" -> "2Ch"
            "Ezra" -> "Ezr"
            "Nehemiah" -> "Neh"
            "Esther" -> "Est"
            "Job" -> "Job"
            "Psalms" -> "Psa"
            "Proverbs" -> "Pro"
            "Ecclesiastes" -> "Ecc"
            "Song of Solomon" -> "Son"
            "Isaiah" -> "Isa"
            "Jeremiah" -> "Jer"
            "Lamentations" -> "Lam"
            "Ezekiel" -> "Eze"
            "Daniel" -> "Dan"
            "Hosea" -> "Hos"
            "Joel" -> "Joe"
            "Amos" -> "Amo"
            "Obadiah" -> "Oba"
            "Jonah" -> "Jon"
            "Micah" -> "Mic"
            "Nahum" -> "Nah"
            "Habakkuk" -> "Hab"
            "Zephaniah" -> "Zep"
            "Haggai" -> "Hag"
            "Zechariah" -> "Zec"
            "Malachi" -> "Mal"
            "Matthew" -> "Mat"
            "Mark" -> "Mar"
            "Luke" -> "Luk"
            "John" -> "Joh"
            "Acts" -> "Act"
            "Romans" -> "Rom"
            "1 Corinthians" -> "1Co"
            "2 Corinthians" -> "2Co"
            "Galatians" -> "Gal"
            "Ephesians" -> "Eph"
            "Philippians" -> "Php"
            "Colossians" -> "Col"
            "1 Thessalonians" -> "1Th"
            "2 Thessalonians" -> "2Th"
            "1 Timothy" -> "1Ti"
            "2 Timothy" -> "2Ti"
            "Titus" -> "Tit"
            "Philemon" -> "Phm"
            "Hebrews" -> "Heb"
            "James" -> "Jas"
            "1 Peter" -> "1Pe"
            "2 Peter" -> "2Pe"
            "1 John" -> "1Jo"
            "2 John" -> "2Jo"
            "3 John" -> "3Jo"
            "Jude" -> "Jud"
            "Revelation" -> "Rev"
            else -> book
        }
    }

    fun formatReference(book: String, chapter: Int, verse: Int): String {
        return "${getAbbreviation(book)} $chapter:$verse"
    }

    fun formatReferenceRange(book: String, chapter: Int, startVerse: Int, endVerse: Int): String {
        return if (startVerse == endVerse) {
            formatReference(book, chapter, startVerse)
        } else {
            "${getAbbreviation(book)} $chapter:$startVerse-$endVerse"
        }
    }
}
