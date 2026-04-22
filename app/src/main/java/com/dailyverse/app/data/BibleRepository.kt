package com.dailyverse.app.data

import android.content.Context
import com.dailyverse.app.data.local.BibleDatabase
import com.dailyverse.app.data.local.BookOrderTuple
import com.dailyverse.app.data.model.BibleVerse
import com.dailyverse.app.data.model.BibleVersion
import com.dailyverse.app.data.model.BookInfo
import com.dailyverse.app.data.model.ChapterInfo
import com.dailyverse.app.data.model.MemorizationProgress
import com.dailyverse.app.data.model.Testament
import com.dailyverse.app.data.remote.BibleApi
import com.dailyverse.app.data.remote.NetworkModule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BibleDatabase,
    private val networkModule: NetworkModule
) {
    private val dao = database.bibleVerseDao()
    private val bibleApi: BibleApi by lazy { networkModule.bibleApi }

    // ============================================================
    // VERSE RETRIEVAL
    // ============================================================

    /**
     * Get a random verse. Prefers local database; falls back to API if empty.
     */
    suspend fun getRandomVerse(version: BibleVersion): BibleVerse? = withContext(Dispatchers.IO) {
        // Try local first
        dao.getRandomVerse(version.apiCode)?.let { return@withContext it }

        // If local is empty, load bundled KJV data
        if (dao.getVerseCountForVersion("kjv") == 0) {
            loadBundledKjvData()
        }

        // Try again with loaded data
        dao.getRandomVerse(version.apiCode)
            ?: dao.getRandomVerse("kjv") // fallback to KJV
    }

    /**
     * Get a specific verse by book, chapter, verse
     */
    suspend fun getVerse(
        book: String,
        chapter: Int,
        verse: Int,
        version: BibleVersion
    ): BibleVerse? = withContext(Dispatchers.IO) {
        dao.getVerse(book, chapter, verse, version.apiCode)
            ?: dao.getVerse(book, chapter, verse, "kjv")
    }

    /**
     * Get multiple verses for memorization mode
     */
    suspend fun getVersesForMemorization(
        book: String,
        chapter: Int,
        startVerse: Int,
        count: Int,
        version: BibleVersion
    ): List<BibleVerse> = withContext(Dispatchers.IO) {
        val endVerse = startVerse + count - 1
        dao.getVersesInRange(book, chapter, startVerse, endVerse, version.apiCode)
            .ifEmpty {
                dao.getVersesInRange(book, chapter, startVerse, endVerse, "kjv")
            }
    }

    /**
     * Get all verses in a chapter
     */
    suspend fun getChapterVerses(
        book: String,
        chapter: Int,
        version: BibleVersion
    ): List<BibleVerse> = withContext(Dispatchers.IO) {
        dao.getChapterVerses(book, chapter, version.apiCode)
            .ifEmpty { dao.getChapterVerses(book, chapter, "kjv") }
    }

    // ============================================================
    // MEMORIZATION PROGRESS
    // ============================================================

    suspend fun getMemorizationProgress(
        book: String,
        chapter: Int,
        lastShownVerse: Int,
        versesPerDay: Int
    ): MemorizationProgress = withContext(Dispatchers.IO) {
        val totalVerses = getVerseCount(book, chapter)
        val currentStart = lastShownVerse + 1
        val versesLearned = if (lastShownVerse > totalVerses) totalVerses else lastShownVerse
        val percentage = if (totalVerses > 0) versesLearned.toFloat() / totalVerses else 0f

        MemorizationProgress(
            book = book,
            chapter = chapter,
            currentVerse = currentStart.coerceAtMost(totalVerses + 1),
            totalVersesInChapter = totalVerses,
            versesPerDay = versesPerDay,
            versesLearned = versesLearned,
            percentageComplete = percentage,
            isComplete = lastShownVerse >= totalVerses
        )
    }

    // ============================================================
    // BOOK/CHAPTER INFO
    // ============================================================

    suspend fun getAllBooks(version: BibleVersion): List<BookInfo> = withContext(Dispatchers.IO) {
        dao.getAllBooks(version.apiCode)
            .ifEmpty { dao.getAllBooks("kjv") }
            .map { it.toBookInfo() }
    }

    suspend fun getChaptersForBook(book: String, version: BibleVersion): List<Int> = withContext(Dispatchers.IO) {
        dao.getChaptersForBook(book, version.apiCode)
            .ifEmpty { dao.getChaptersForBook(book, "kjv") }
    }

    suspend fun getVerseCount(book: String, chapter: Int): Int = withContext(Dispatchers.IO) {
        dao.getVerseCount(book, chapter, "kjv")
    }

    fun getFavoriteVerses(): Flow<List<BibleVerse>> = dao.getFavoriteVerses()

    suspend fun toggleFavorite(verseId: Int, isFavorite: Boolean) {
        dao.setFavorite(verseId, isFavorite)
    }

    // ============================================================
    // DATA INITIALIZATION
    // ============================================================

    /**
     * Load bundled KJV data into the database.
     * This is called on first run to populate the local database.
     */
    suspend fun loadBundledKjvData() = withContext(Dispatchers.IO) {
        if (dao.getVerseCountForVersion("kjv") > 0) return@withContext

        val verses = loadBundledKjvVerses()
        dao.insertVerses(verses)
    }

    suspend fun isDataLoaded(): Boolean = withContext(Dispatchers.IO) {
        dao.getVerseCountForVersion("kjv") > 0
    }

    // ============================================================
    // BIBLE API HELPER
    // ============================================================

    fun getBibleIdForVersion(version: BibleVersion): String {
        return when (version) {
            BibleVersion.KJV -> BibleApi.BIBLE_ID_KJV
            BibleVersion.NIV -> BibleApi.BIBLE_ID_NIV
            BibleVersion.ESV -> BibleApi.BIBLE_ID_ESV
            BibleVersion.NLT -> BibleApi.BIBLE_ID_NLT
            BibleVersion.NKJV -> BibleApi.BIBLE_ID_NKJV
            BibleVersion.WEB -> BibleApi.BIBLE_ID_WEB
        }
    }
}

// ============================================================
// BUNDLED KJV DATA - Essential verses for offline functionality
// ============================================================

private fun BookOrderTuple.toBookInfo(): BookInfo {
    val testament = if (bookOrder <= 39) Testament.OLD else Testament.NEW
    val totalChapters = BIBLE_BOOK_CHAPTERS[book] ?: 1
    return BookInfo(name = book, order = bookOrder, testament = testament, totalChapters = totalChapters)
}

// Chapter counts for each book
private val BIBLE_BOOK_CHAPTERS = mapOf(
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

/**
 * Loads essential KJV verses bundled with the app.
 * Covers key verses from every book for offline functionality.
 */
private fun loadBundledKjvVerses(): List<BibleVerse> {
    val verses = mutableListOf<BibleVerse>()
    val bookOrderMap = mapOf(
        "Genesis" to 1, "Exodus" to 2, "Leviticus" to 3, "Numbers" to 4, "Deuteronomy" to 5,
        "Joshua" to 6, "Judges" to 7, "Ruth" to 8, "1 Samuel" to 9, "2 Samuel" to 10,
        "1 Kings" to 11, "2 Kings" to 12, "1 Chronicles" to 13, "2 Chronicles" to 14,
        "Ezra" to 15, "Nehemiah" to 16, "Esther" to 17, "Job" to 18, "Psalms" to 19,
        "Proverbs" to 20, "Ecclesiastes" to 21, "Song of Solomon" to 22, "Isaiah" to 23,
        "Jeremiah" to 24, "Lamentations" to 25, "Ezekiel" to 26, "Daniel" to 27,
        "Hosea" to 28, "Joel" to 29, "Amos" to 30, "Obadiah" to 31, "Jonah" to 32,
        "Micah" to 33, "Nahum" to 34, "Habakkuk" to 35, "Zephaniah" to 36,
        "Haggai" to 37, "Zechariah" to 38, "Malachi" to 39,
        "Matthew" to 40, "Mark" to 41, "Luke" to 42, "John" to 43, "Acts" to 44,
        "Romans" to 45, "1 Corinthians" to 46, "2 Corinthians" to 47, "Galatians" to 48,
        "Ephesians" to 49, "Philippians" to 50, "Colossians" to 51, "1 Thessalonians" to 52,
        "2 Thessalonians" to 53, "1 Timothy" to 54, "2 Timothy" to 55, "Titus" to 56,
        "Philemon" to 57, "Hebrews" to 58, "James" to 59, "1 Peter" to 60,
        "2 Peter" to 61, "1 John" to 62, "2 John" to 63, "3 John" to 64,
        "Jude" to 65, "Revelation" to 66
    )

    var id = 1
    fun add(book: String, chapter: Int, verse: Int, text: String) {
        verses.add(BibleVerse(
            id = id++,
            book = book,
            bookOrder = bookOrderMap[book] ?: 0,
            chapter = chapter,
            verse = verse,
            text = text,
            version = "kjv"
        ))
    }

    // Genesis
    add("Genesis", 1, 1, "In the beginning God created the heaven and the earth.")
    add("Genesis", 1, 27, "So God created man in his own image, in the image of God created he him; male and female created he them.")
    add("Genesis", 12, 2, "And I will make of thee a great nation, and I will bless thee, and make thy name great; and thou shalt be a blessing.")
    add("Genesis", 28, 15, "And, behold, I am with thee, and will keep thee in all places whither thou goest, and will bring thee again into this land; for I will not leave thee, until I have done that which I have spoken to thee of.")
    add("Genesis", 50, 20, "But as for you, ye thought evil against me; but God meant it unto good, to bring to pass, as it is this day, to save much people alive.")

    // Exodus
    add("Exodus", 14, 14, "The LORD shall fight for you, and ye shall hold your peace.")
    add("Exodus", 15, 2, "The LORD is my strength and song, and he is become my salvation: he is my God, and I will prepare him an habitation; my father's God, and I will exalt him.")
    add("Exodus", 20, 3, "Thou shalt have no other gods before me.")
    add("Exodus", 33, 14, "And he said, My presence shall go with thee, and I will give thee rest.")
    add("Exodus", 34, 6, "And the LORD passed by before him, and proclaimed, The LORD, The LORD God, merciful and gracious, longsuffering, and abundant in goodness and truth.")

    // Leviticus
    add("Leviticus", 19, 18, "Thou shalt not avenge, nor bear any grudge against the children of thy people, but thou shalt love thy neighbour as thyself: I am the LORD.")
    add("Leviticus", 20, 26, "And ye shall be holy unto me: for I the LORD am holy, and have severed you from other people, that ye should be mine.")
    add("Leviticus", 26, 12, "And I will walk among you, and will be your God, and ye shall be my people.")

    // Numbers
    add("Numbers", 6, 24, "The LORD bless thee, and keep thee.")
    add("Numbers", 6, 25, "The LORD make his face shine upon thee, and be gracious unto thee.")
    add("Numbers", 6, 26, "The LORD lift up his countenance upon thee, and give thee peace.")

    // Deuteronomy
    add("Deuteronomy", 6, 5, "And thou shalt love the LORD thy God with all thine heart, and with all thy soul, and with all thy might.")
    add("Deuteronomy", 31, 6, "Be strong and of a good courage, fear not, nor be afraid of them: for the LORD thy God, he it is that doth go with thee; he will not fail thee, nor forsake thee.")
    add("Deuteronomy", 31, 8, "And the LORD, he it is that doth go before thee; he will be with thee, he will not fail thee, neither forsake thee: fear not, neither be dismayed.")

    // Joshua
    add("Joshua", 1, 9, "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee whithersoever thou goest.")
    add("Joshua", 24, 15, "And if it seem evil unto you to serve the LORD, choose you this day whom ye will serve; but as for me and my house, we will serve the LORD.")

    // Judges
    add("Judges", 5, 31, "So let all thine enemies perish, O LORD: but let them that love him be as the sun when he goeth forth in his might.")

    // Ruth
    add("Ruth", 1, 16, "And Ruth said, Intreat me not to leave thee, or to return from following after thee: for whither thou goest, I will go; and where thou lodgest, I will lodge: thy people shall be my people, and thy God my God.")

    // 1 Samuel
    add("1 Samuel", 12, 24, "Only fear the LORD, and serve him in truth with all your heart: for consider how great things he hath done for you.")
    add("1 Samuel", 16, 7, "But the LORD said unto Samuel, Look not on his countenance, or on the height of his stature; because I have refused him: for the LORD seeth not as man seeth; for man looketh on the outward appearance, but the LORD looketh on the heart.")

    // 2 Samuel
    add("2 Samuel", 7, 22, "Wherefore thou art great, O LORD God: for there is none like thee, neither is there any God beside thee, according to all that we have heard with our ears.")
    add("2 Samuel", 22, 31, "As for God, his way is perfect; the word of the LORD is tried: he is a buckler to all them that trust in him.")

    // 1 Kings
    add("1 Kings", 8, 23, "And he said, LORD God of Israel, there is no God like thee, in heaven above, or on earth beneath, who keepest covenant and mercy with thy servants that walk before thee with all their heart.")

    // 2 Kings
    add("2 Kings", 17, 39, "But the LORD your God ye shall fear; and he shall deliver you out of the hand of all your enemies.")

    // 1 Chronicles
    add("1 Chronicles", 16, 11, "Seek the LORD and his strength, seek his face continually.")
    add("1 Chronicles", 29, 11, "Thine, O LORD, is the greatness, and the power, and the glory, and the victory, and the majesty: for all that is in the heaven and in the earth is thine; thine is the kingdom, O LORD, and thou art exalted as head above all.")

    // 2 Chronicles
    add("2 Chronicles", 7, 14, "If my people, which are called by my name, shall humble themselves, and pray, and seek my face, and turn from their wicked ways; then will I hear from heaven, and will forgive their sin, and will heal their land.")
    add("2 Chronicles", 20, 15, "And he said, Hearken ye, all Judah, and ye inhabitants of Jerusalem, and thou king Jehoshaphat, Thus saith the LORD unto you, Be not afraid nor dismayed by reason of this great multitude; for the battle is not yours, but God's.")

    // Ezra
    add("Ezra", 10, 4, "Arise; for this matter belongeth unto thee: we also will be with thee: be of good courage, and do it.")

    // Nehemiah
    add("Nehemiah", 8, 10, "Then he said unto them, Go your way, eat the fat, and drink the sweet, and send portions unto them for whom nothing is prepared: for this day is holy unto our Lord: neither be ye sorry; for the joy of the LORD is your strength.")

    // Esther
    add("Esther", 4, 14, "For if thou altogether holdest thy peace at this time, then shall there enlargement and deliverance arise to the Jews from another place; but thou and thy father's house shall be destroyed: and who knoweth whether thou art come to the kingdom for such a time as this?")

    // Job
    add("Job", 1, 21, "And said, Naked came I out of my mother's womb, and naked shall I return thither: the LORD gave, and the LORD hath taken away; blessed be the name of the LORD.")
    add("Job", 19, 25, "For I know that my redeemer liveth, and that he shall stand at the latter day upon the earth.")
    add("Job", 37, 5, "God thundereth marvellously with his voice; great things doeth he, which we cannot comprehend.")
    add("Job", 42, 2, "I know that thou canst do every thing, and that no thought can be withholden from thee.")

    // Psalms
    add("Psalms", 1, 1, "Blessed is the man that walketh not in the counsel of the ungodly, nor standeth in the way of sinners, nor sitteth in the seat of the scornful.")
    add("Psalms", 1, 2, "But his delight is in the law of the LORD; and in his law doth he meditate day and night.")
    add("Psalms", 1, 3, "And he shall be like a tree planted by the rivers of water, that bringeth forth his fruit in his season; his leaf also shall not wither; and whatsoever he doeth shall prosper.")
    add("Psalms", 19, 1, "The heavens declare the glory of God; and the firmament sheweth his handywork.")
    add("Psalms", 23, 1, "The LORD is my shepherd; I shall not want.")
    add("Psalms", 23, 2, "He maketh me to lie down in green pastures: he leadeth me beside the still waters.")
    add("Psalms", 23, 3, "He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake.")
    add("Psalms", 23, 4, "Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me; thy rod and thy staff they comfort me.")
    add("Psalms", 23, 5, "Thou preparest a table before me in the presence of mine enemies: thou anointest my head with oil; my cup runneth over.")
    add("Psalms", 23, 6, "Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the LORD for ever.")
    add("Psalms", 27, 1, "The LORD is my light and my salvation; whom shall I fear? the LORD is the strength of my life; of whom shall I be afraid?")
    add("Psalms", 28, 7, "The LORD is my strength and my shield; my heart trusted in him, and I am helped: therefore my heart greatly rejoiceth; and with my song will I praise him.")
    add("Psalms", 31, 24, "Be of good courage, and he shall strengthen your heart, all ye that hope in the LORD.")
    add("Psalms", 34, 8, "O taste and see that the LORD is good: blessed is the man that trusteth in him.")
    add("Psalms", 37, 4, "Delight thyself also in the LORD; and he shall give thee the desires of thine heart.")
    add("Psalms", 37, 5, "Commit thy way unto the LORD; trust also in him; and he shall bring it to pass.")
    add("Psalms", 46, 1, "God is our refuge and strength, a very present help in trouble.")
    add("Psalms", 46, 10, "Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth.")
    add("Psalms", 51, 10, "Create in me a clean heart, O God; and renew a right spirit within me.")
    add("Psalms", 55, 22, "Cast thy burden upon the LORD, and he shall sustain thee: he shall never suffer the righteous to be moved.")
    add("Psalms", 61, 2, "From the end of the earth will I cry unto thee, when my heart is overwhelmed: lead me to the rock that is higher than I.")
    add("Psalms", 62, 5, "My soul, wait thou only upon God; for my expectation is from him.")
    add("Psalms", 63, 3, "Because thy lovingkindness is better than life, my lips shall praise thee.")
    add("Psalms", 73, 26, "My flesh and my heart faileth: but God is the strength of my heart, and my portion for ever.")
    add("Psalms", 91, 1, "He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty.")
    add("Psalms", 91, 2, "I will say of the LORD, He is my refuge and my fortress: my God; in him will I trust.")
    add("Psalms", 103, 1, "Bless the LORD, O my soul: and all that is within me, bless his holy name.")
    add("Psalms", 103, 2, "Bless the LORD, O my soul, and forget not all his benefits.")
    add("Psalms", 103, 13, "Like as a father pitieth his children, so the LORD pitieth them that fear him.")
    add("Psalms", 107, 1, "O give thanks unto the LORD, for he is good: for his mercy endureth for ever.")
    add("Psalms", 118, 24, "This is the day which the LORD hath made; we will rejoice and be glad in it.")
    add("Psalms", 119, 11, "Thy word have I hid in mine heart, that I might not sin against thee.")
    add("Psalms", 119, 105, "Thy word is a lamp unto my feet, and a light unto my path.")
    add("Psalms", 121, 1, "I will lift up mine eyes unto the hills, from whence cometh my help.")
    add("Psalms", 121, 2, "My help cometh from the LORD, which made heaven and earth.")
    add("Psalms", 126, 3, "The LORD hath done great things for us; whereof we are glad.")
    add("Psalms", 130, 5, "I wait for the LORD, my soul doth wait, and in his word do I hope.")
    add("Psalms", 139, 14, "I will praise thee; for I am fearfully and wonderfully made: marvellous are thy works; and that my soul knoweth right well.")
    add("Psalms", 139, 23, "Search me, O God, and know my heart: try me, and know my thoughts.")
    add("Psalms", 147, 3, "He healeth the broken in heart, and bindeth up their wounds.")
    add("Psalms", 150, 6, "Let every thing that hath breath praise the LORD. Praise ye the LORD.")

    // Proverbs
    add("Proverbs", 3, 5, "Trust in the LORD with all thine heart; and lean not unto thine own understanding.")
    add("Proverbs", 3, 6, "In all thy ways acknowledge him, and he shall direct thy paths.")
    add("Proverbs", 3, 9, "Honour the LORD with thy substance, and with the firstfruits of all thine increase.")
    add("Proverbs", 3, 27, "Withhold not good from them to whom it is due, when it is in the power of thine hand to do it.")
    add("Proverbs", 4, 23, "Keep thy heart with all diligence; for out of it are the issues of life.")
    add("Proverbs", 9, 10, "The fear of the LORD is the beginning of wisdom: and the knowledge of the holy is understanding.")
    add("Proverbs", 10, 12, "Hatred stirreth up strifes: but love covereth all sins.")
    add("Proverbs", 16, 3, "Commit thy works unto the LORD, and thy thoughts shall be established.")
    add("Proverbs", 16, 9, "A man's heart deviseth his way: but the LORD directeth his steps.")
    add("Proverbs", 17, 17, "A friend loveth at all times, and a brother is born for adversity.")
    add("Proverbs", 18, 10, "The name of the LORD is a strong tower: the righteous runneth into it, and is safe.")
    add("Proverbs", 19, 21, "There are many devices in a man's heart; nevertheless the counsel of the LORD, that shall stand.")
    add("Proverbs", 22, 6, "Train up a child in the way he should go: and when he is old, he will not depart from it.")
    add("Proverbs", 27, 17, "Iron sharpeneth iron; so a man sharpeneth the countenance of his friend.")
    add("Proverbs", 30, 5, "Every word of God is pure: he is a shield unto them that put their trust in him.")
    add("Proverbs", 31, 25, "Strength and honour are her clothing; and she shall rejoice in time to come.")
    add("Proverbs", 31, 30, "Favour is deceitful, and beauty is vain: but a woman that feareth the LORD, she shall be praised.")

    // Ecclesiastes
    add("Ecclesiastes", 3, 1, "To every thing there is a season, and a time to every purpose under the heaven.")
    add("Ecclesiastes", 3, 11, "He hath made every thing beautiful in his time: also he hath set the world in their heart, so that no man can find out the work that God maketh from the beginning to the end.")
    add("Ecclesiastes", 4, 9, "Two are better than one; because they have a good reward for their labour.")
    add("Ecclesiastes", 12, 13, "Let us hear the conclusion of the whole matter: Fear God, and keep his commandments: for this is the whole duty of man.")

    // Song of Solomon
    add("Song of Solomon", 2, 4, "He brought me to the banqueting house, and his banner over me was love.")
    add("Song of Solomon", 8, 7, "Many waters cannot quench love, neither can the floods drown it: if a man would give all the substance of his house for love, it would utterly be contemned.")

    // Isaiah
    add("Isaiah", 9, 6, "For unto us a child is born, unto us a son is given: and the government shall be upon his shoulder: and his name shall be called Wonderful, Counsellor, The mighty God, The everlasting Father, The Prince of Peace.")
    add("Isaiah", 26, 3, "Thou wilt keep him in perfect peace, whose mind is stayed on thee: because he trusteth in thee.")
    add("Isaiah", 40, 28, "Hast thou not known? hast thou not heard, that the everlasting God, the LORD, the Creator of the ends of the earth, fainteth not, neither is weary? there is no searching of his understanding.")
    add("Isaiah", 40, 29, "He giveth power to the faint; and to them that have no might he increaseth strength.")
    add("Isaiah", 40, 31, "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.")
    add("Isaiah", 41, 10, "Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee; yea, I will help thee; yea, I will uphold thee with the right hand of my righteousness.")
    add("Isaiah", 43, 2, "When thou passest through the waters, I will be with thee; and through the rivers, they shall not overflow thee: when thou walkest through the fire, thou shalt not be burned; neither shall the flame kindle upon thee.")
    add("Isaiah", 43, 19, "Behold, I will do a new thing; now it shall spring forth; shall ye not know it? I will even make a way in the wilderness, and rivers in the desert.")
    add("Isaiah", 53, 5, "But he was wounded for our transgressions, he was bruised for our iniquities: the chastisement of our peace was upon him; and with his stripes we are healed.")
    add("Isaiah", 54, 17, "No weapon that is formed against thee shall prosper; and every tongue that shall rise against thee in judgment thou shalt condemn. This is the heritage of the servants of the LORD, and their righteousness is of me, saith the LORD.")
    add("Isaiah", 55, 6, "Seek ye the LORD while he may be found, call ye upon him while he is near.")
    add("Isaiah", 55, 7, "Let the wicked forsake his way, and the unrighteous man his thoughts: and let him return unto the LORD, and he will have mercy upon him; and to our God, for he will abundantly pardon.")
    add("Isaiah", 55, 8, "For my thoughts are not your thoughts, neither are your ways my ways, saith the LORD.")
    add("Isaiah", 55, 9, "For as the heavens are higher than the earth, so are my ways higher than your ways, and my thoughts than your thoughts.")

    // Jeremiah
    add("Jeremiah", 17, 7, "Blessed is the man that trusteth in the LORD, and whose hope the LORD is.")
    add("Jeremiah", 17, 10, "I the LORD search the heart, I try the reins, even to give every man according to his ways, and according to the fruit of his doings.")
    add("Jeremiah", 29, 11, "For I know the thoughts that I think toward you, saith the LORD, thoughts of peace, and not of evil, to give you an expected end.")
    add("Jeremiah", 29, 12, "Then shall ye call upon me, and ye shall go and pray unto me, and I will hearken unto you.")
    add("Jeremiah", 29, 13, "And ye shall seek me, and find me, when ye shall search for me with all your heart.")
    add("Jeremiah", 32, 27, "Behold, I am the LORD, the God of all flesh: is there any thing too hard for me?")

    // Lamentations
    add("Lamentations", 3, 22, "It is of the LORD'S mercies that we are not consumed, because his compassions fail not.")
    add("Lamentations", 3, 23, "They are new every morning: great is thy faithfulness.")

    // Ezekiel
    add("Ezekiel", 36, 26, "A new heart also will I give you, and a new spirit will I put within you: and I will take away the stony heart out of your flesh, and I will give you an heart of flesh.")

    // Daniel
    add("Daniel", 2, 20, "Daniel answered and said, Blessed be the name of God for ever and ever: for wisdom and might are his.")
    add("Daniel", 3, 17, "If it be so, our God whom we serve is able to deliver us from the burning fiery furnace, and he will deliver us out of thine hand, O king.")
    add("Daniel", 6, 22, "My God hath sent his angel, and hath shut the lions' mouths, that they have not hurt me: forasmuch as before him innocency was found in me; and also before thee, O king, have I done no hurt.")

    // Hosea
    add("Hosea", 6, 3, "Then shall we know, if we follow on to know the LORD: his going forth is prepared as the morning; and he shall come unto us as the rain, as the latter and former rain unto the earth.")

    // Joel
    add("Joel", 2, 13, "And rend your heart, and not your garments, and turn unto the LORD your God: for he is gracious and merciful, slow to anger, and of great kindness, and repenteth him of the evil.")

    // Amos
    add("Amos", 5, 4, "For thus saith the LORD unto the house of Israel, Seek ye me, and ye shall live.")
    add("Amos", 5, 24, "But let judgment run down as waters, and righteousness as a mighty stream.")

    // Micah
    add("Micah", 6, 8, "He hath shewed thee, O man, what is good; and what doth the LORD require of thee, but to do justly, and to love mercy, and to walk humbly with thy God?")

    // Nahum
    add("Nahum", 1, 7, "The LORD is good, a strong hold in the day of trouble; and he knoweth them that trust in him.")

    // Habakkuk
    add("Habakkuk", 3, 17, "Although the fig tree shall not blossom, neither shall fruit be in the vines; the labour of the olive shall fail, and the fields shall yield no meat; the flock shall be cut off from the fold, and there shall be no herd in the stalls.")
    add("Habakkuk", 3, 18, "Yet I will rejoice in the LORD, I will joy in the God of my salvation.")
    add("Habakkuk", 3, 19, "The LORD God is my strength, and he will make my feet like hinds' feet, and he will make me to walk upon mine high places.")

    // Zephaniah
    add("Zephaniah", 3, 17, "The LORD thy God in the midst of thee is mighty; he will save, he will rejoice over thee with joy; he will rest in his love, he will joy over thee with singing.")

    // Haggai
    add("Haggai", 2, 9, "The glory of this latter house shall be greater than of the former, saith the LORD of hosts: and in this place will I give peace, saith the LORD of hosts.")

    // Zechariah
    add("Zechariah", 4, 6, "Then he answered and spake unto me, saying, This is the word of the LORD unto Zerubbabel, saying, Not by might, nor by power, but by my spirit, saith the LORD of hosts.")
    add("Zechariah", 9, 12, "Turn you to the strong hold, ye prisoners of hope: even to day do I declare that I will render double unto thee.")

    // Malachi
    add("Malachi", 3, 10, "Bring ye all the tithes into the storehouse, that there may be meat in mine house, and prove me now herewith, saith the LORD of hosts, if I will not open you the windows of heaven, and pour you out a blessing, that there shall not be room enough to receive it.")

    // Matthew
    add("Matthew", 5, 14, "Ye are the light of the world. A city that is set on an hill cannot be hid.")
    add("Matthew", 5, 16, "Let your light so shine before men, that they may see your good works, and glorify your Father which is in heaven.")
    add("Matthew", 6, 33, "But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you.")
    add("Matthew", 7, 7, "Ask, and it shall be given you; seek, and ye shall find; knock, and it shall be opened unto you.")
    add("Matthew", 11, 28, "Come unto me, all ye that labour and are heavy laden, and I will give you rest.")
    add("Matthew", 11, 29, "Take my yoke upon you, and learn of me; for I am meek and lowly in heart: and ye shall find rest unto your souls.")
    add("Matthew", 11, 30, "For my yoke is easy, and my burden is light.")
    add("Matthew", 16, 24, "Then said Jesus unto his disciples, If any man will come after me, let him deny himself, and take up his cross, and follow me.")
    add("Matthew", 19, 26, "But Jesus beheld them, and said unto them, With men this is impossible; but with God all things are possible.")
    add("Matthew", 22, 37, "Jesus said unto him, Thou shalt love the Lord thy God with all thy heart, and with all thy soul, and with all thy mind.")
    add("Matthew", 22, 39, "And the second is like unto it, Thou shalt love thy neighbour as thyself.")
    add("Matthew", 28, 19, "Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost.")
    add("Matthew", 28, 20, "Teaching them to observe all things whatsoever I have commanded you: and, lo, I am with you alway, even unto the end of the world. Amen.")

    // Mark
    add("Mark", 5, 36, "As soon as Jesus heard the word that was spoken, he saith unto the ruler of the synagogue, Be not afraid, only believe.")
    add("Mark", 9, 23, "Jesus said unto him, If thou canst believe, all things are possible to him that believeth.")
    add("Mark", 10, 27, "And Jesus looking upon them saith, With men it is impossible, but not with God: for with God all things are possible.")
    add("Mark", 11, 24, "Therefore I say unto you, What things soever ye desire, when ye pray, believe that ye receive them, and ye shall have them.")
    add("Mark", 12, 30, "And thou shalt love the Lord thy God with all thy heart, and with all thy soul, and with all thy mind, and with all thy strength: this is the first commandment.")

    // Luke
    add("Luke", 1, 37, "For with God nothing shall be impossible.")
    add("Luke", 6, 31, "And as ye would that men should do to you, do ye also to them likewise.")
    add("Luke", 10, 27, "And he answering said, Thou shalt love the Lord thy God with all thy heart, and with all thy soul, and with all thy strength, and with all thy mind; and thy neighbour as thyself.")
    add("Luke", 12, 34, "For where your treasure is, there will your heart be also.")
    add("Luke", 18, 27, "And he said, The things which are impossible with men are possible with God.")

    // John
    add("John", 1, 1, "In the beginning was the Word, and the Word was with God, and the Word was God.")
    add("John", 3, 16, "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.")
    add("John", 3, 17, "For God sent not his Son into the world to condemn the world; but that the world through him might be saved.")
    add("John", 4, 24, "God is a Spirit: and they that worship him must worship him in spirit and in truth.")
    add("John", 7, 38, "He that believeth on me, as the scripture hath said, out of his belly shall flow rivers of living water.")
    add("John", 8, 12, "Then spake Jesus again unto them, saying, I am the light of the world: he that followeth me shall not walk in darkness, but shall have the light of life.")
    add("John", 8, 32, "And ye shall know the truth, and the truth shall make you free.")
    add("John", 10, 10, "The thief cometh not, but for to steal, and to kill, and to destroy: I am come that they might have life, and that they might have it more abundantly.")
    add("John", 10, 11, "I am the good shepherd: the good shepherd giveth his life for the sheep.")
    add("John", 11, 25, "Jesus said unto her, I am the resurrection, and the life: he that believeth in me, though he were dead, yet shall he live.")
    add("John", 13, 34, "A new commandment I give unto you, That ye love one another; as I have loved you, that ye also love one another.")
    add("John", 14, 1, "Let not your heart be troubled: ye believe in God, believe also in me.")
    add("John", 14, 6, "Jesus saith unto him, I am the way, the truth, and the life: no man cometh unto the Father, but by me.")
    add("John", 14, 27, "Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid.")
    add("John", 15, 4, "Abide in me, and I in you. As the branch cannot bear fruit of itself, except it abide in the vine; no more can ye, except ye abide in me.")
    add("John", 15, 5, "I am the vine, ye are the branches: He that abideth in me, and I in him, the same bringeth forth much fruit: for without me ye can do nothing.")
    add("John", 15, 13, "Greater love hath no man than this, that a man lay down his life for his friends.")

    // Acts
    add("Acts", 1, 8, "But ye shall receive power, after that the Holy Ghost is come upon you: and ye shall be witnesses unto me both in Jerusalem, and in all Judaea, and in Samaria, and unto the uttermost part of the earth.")
    add("Acts", 2, 38, "Then Peter said unto them, Repent, and be baptized every one of you in the name of Jesus Christ for the remission of sins, and ye shall receive the gift of the Holy Ghost.")
    add("Acts", 4, 12, "Neither is there salvation in any other: for there is none other name under heaven given among men, whereby we must be saved.")

    // Romans
    add("Romans", 1, 16, "For I am not ashamed of the gospel of Christ: for it is the power of God unto salvation to every one that believeth; to the Jew first, and also to the Greek.")
    add("Romans", 3, 23, "For all have sinned, and come short of the glory of God.")
    add("Romans", 5, 1, "Therefore being justified by faith, we have peace with God through our Lord Jesus Christ.")
    add("Romans", 5, 8, "But God commendeth his love toward us, in that, while we were yet sinners, Christ died for us.")
    add("Romans", 6, 23, "For the wages of sin is death; but the gift of God is eternal life through Jesus Christ our Lord.")
    add("Romans", 8, 1, "There is therefore now no condemnation to them which are in Christ Jesus, who walk not after the flesh, but after the Spirit.")
    add("Romans", 8, 6, "For to be carnally minded is death; but to be spiritually minded is life and peace.")
    add("Romans", 8, 14, "For as many as are led by the Spirit of God, they are the sons of God.")
    add("Romans", 8, 18, "For I reckon that the sufferings of this present time are not worthy to be compared with the glory which shall be revealed in us.")
    add("Romans", 8, 28, "And we know that all things work together for good to them that love God, to them who are the called according to his purpose.")
    add("Romans", 8, 31, "What shall we then say to these things? If God be for us, who can be against us?")
    add("Romans", 8, 38, "For I am persuaded, that neither death, nor life, nor angels, nor principalities, nor powers, nor things present, nor things to come,")
    add("Romans", 8, 39, "Nor height, nor depth, nor any other creature, shall be able to separate us from the love of God, which is in Christ Jesus our Lord.")
    add("Romans", 10, 9, "That if thou shalt confess with thy mouth the Lord Jesus, and shalt believe in thine heart that God hath raised him from the dead, thou shalt be saved.")
    add("Romans", 10, 13, "For whosoever shall call upon the name of the Lord shall be saved.")
    add("Romans", 12, 1, "I beseech you therefore, brethren, by the mercies of God, that ye present your bodies a living sacrifice, holy, acceptable unto God, which is your reasonable service.")
    add("Romans", 12, 2, "And be not conformed to this world: but be ye transformed by the renewing of your mind, that ye may prove what is that good, and acceptable, and perfect, will of God.")
    add("Romans", 12, 12, "Rejoicing in hope; patient in tribulation; continuing instant in prayer.")

    // 1 Corinthians
    add("1 Corinthians", 3, 16, "Know ye not that ye are the temple of God, and that the Spirit of God dwelleth in you?")
    add("1 Corinthians", 6, 19, "What? know ye not that your body is the temple of the Holy Ghost which is in you, which ye have of God, and ye are not your own?")
    add("1 Corinthians", 6, 20, "For ye are bought with a price: therefore glorify God in your body, and in your spirit, which are God's.")
    add("1 Corinthians", 10, 13, "There hath no temptation taken you but such as is common to man: but God is faithful, who will not suffer you to be tempted above that ye are able; but will with the temptation also make a way to escape, that ye may be able to bear it.")
    add("1 Corinthians", 13, 4, "Charity suffereth long, and is kind; charity envieth not; charity vaunteth not itself, is not puffed up.")
    add("1 Corinthians", 13, 13, "And now abideth faith, hope, charity, these three; but the greatest of these is charity.")
    add("1 Corinthians", 15, 58, "Therefore, my beloved brethren, be ye stedfast, unmoveable, always abounding in the work of the Lord, forasmuch as ye know that your labour is not in vain in the Lord.")
    add("1 Corinthians", 16, 14, "Let all your things be done with charity.")

    // 2 Corinthians
    add("2 Corinthians", 3, 17, "Now the Lord is that Spirit: and where the Spirit of the Lord is, there is liberty.")
    add("2 Corinthians", 4, 16, "For which cause we faint not; but though our outward man perish, yet the inward man is renewed day by day.")
    add("2 Corinthians", 4, 18, "While we look not at the things which are seen, but at the things which are not seen: for the things which are seen are temporal; but the things which are not seen are eternal.")
    add("2 Corinthians", 5, 7, "For we walk by faith, not by sight.")
    add("2 Corinthians", 5, 17, "Therefore if any man be in Christ, he is a new creature: old things are passed away; behold, all things are become new.")
    add("2 Corinthians", 9, 7, "Every man according as he purposeth in his heart, so let him give; not grudgingly, or of necessity: for God loveth a cheerful giver.")
    add("2 Corinthians", 12, 9, "And he said unto me, My grace is sufficient for thee: for my strength is made perfect in weakness. Most gladly therefore will I rather glory in my infirmities, that the power of Christ may rest upon me.")

    // Galatians
    add("Galatians", 2, 20, "I am crucified with Christ: nevertheless I live; yet not I, but Christ liveth in me: and the life which I now live in the flesh I live by the faith of the Son of God, who loved me, and gave himself for me.")
    add("Galatians", 5, 1, "Stand fast therefore in the liberty wherewith Christ hath made us free, and be not entangled again with the yoke of bondage.")
    add("Galatians", 5, 22, "But the fruit of the Spirit is love, joy, peace, longsuffering, gentleness, goodness, faith,")
    add("Galatians", 5, 23, "Meekness, temperance: against such there is no law.")
    add("Galatians", 6, 2, "Bear ye one another's burdens, and so fulfil the law of Christ.")
    add("Galatians", 6, 9, "And let us not be weary in well doing: for in due season we shall reap, if we faint not.")

    // Ephesians
    add("Ephesians", 1, 3, "Blessed be the God and Father of our Lord Jesus Christ, who hath blessed us with all spiritual blessings in heavenly places in Christ.")
    add("Ephesians", 2, 8, "For by grace are ye saved through faith; and that not of yourselves: it is the gift of God.")
    add("Ephesians", 2, 9, "Not of works, lest any man should boast.")
    add("Ephesians", 2, 10, "For we are his workmanship, created in Christ Jesus unto good works, which God hath before ordained that we should walk in them.")
    add("Ephesians", 3, 20, "Now unto him that is able to do exceeding abundantly above all that we ask or think, according to the power that worketh in us,")
    add("Ephesians", 4, 2, "With all lowliness and meekness, with longsuffering, forbearing one another in love;")
    add("Ephesians", 4, 32, "And be ye kind one to another, tenderhearted, forgiving one another, even as God for Christ's sake hath forgiven you.")
    add("Ephesians", 5, 2, "And walk in love, as Christ also hath loved us, and hath given himself for us an offering and a sacrifice to God for a sweetsmelling savour.")
    add("Ephesians", 6, 10, "Finally, my brethren, be strong in the Lord, and in the power of his might.")
    add("Ephesians", 6, 11, "Put on the whole armour of God, that ye may be able to stand against the wiles of the devil.")

    // Philippians
    add("Philippians", 1, 6, "Being confident of this very thing, that he which hath begun a good work in you will perform it until the day of Jesus Christ.")
    add("Philippians", 2, 3, "Let nothing be done through strife or vainglory; but in lowliness of mind let each esteem other better than themselves.")
    add("Philippians", 2, 5, "Let this mind be in you, which was also in Christ Jesus.")
    add("Philippians", 3, 13, "Brethren, I count not myself to have apprehended: but this one thing I do, forgetting those things which are behind, and reaching forth unto those things which are before,")
    add("Philippians", 3, 14, "I press toward the mark for the prize of the high calling of God in Christ Jesus.")
    add("Philippians", 4, 4, "Rejoice in the Lord alway: and again I say, Rejoice.")
    add("Philippians", 4, 6, "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God.")
    add("Philippians", 4, 7, "And the peace of God, which passeth all understanding, shall keep your hearts and minds through Christ Jesus.")
    add("Philippians", 4, 8, "Finally, brethren, whatsoever things are true, whatsoever things are honest, whatsoever things are just, whatsoever things are pure, whatsoever things are lovely, whatsoever things are of good report; if there be any virtue, and if there be any praise, think on these things.")
    add("Philippians", 4, 13, "I can do all things through Christ which strengtheneth me.")
    add("Philippians", 4, 19, "But my God shall supply all your need according to his riches in glory by Christ Jesus.")

    // Colossians
    add("Colossians", 1, 16, "For by him were all things created, that are in heaven, and that are in earth, visible and invisible, whether they be thrones, or dominions, or principalities, or powers: all things were created by him, and for him.")
    add("Colossians", 3, 2, "Set your affection on things above, not on things on the earth.")
    add("Colossians", 3, 12, "Put on therefore, as the elect of God, holy and beloved, bowels of mercies, kindness, humbleness of mind, meekness, longsuffering.")
    add("Colossians", 3, 14, "And above all these things put on charity, which is the bond of perfectness.")
    add("Colossians", 3, 17, "And whatsoever ye do in word or deed, do all in the name of the Lord Jesus, giving thanks to God and the Father by him.")
    add("Colossians", 3, 23, "And whatsoever ye do, do it heartily, as to the Lord, and not unto men.")

    // 1 Thessalonians
    add("1 Thessalonians", 5, 11, "Wherefore comfort yourselves together, and edify one another, even as also ye do.")
    add("1 Thessalonians", 5, 16, "Rejoice evermore.")
    add("1 Thessalonians", 5, 17, "Pray without ceasing.")
    add("1 Thessalonians", 5, 18, "In every thing give thanks: for this is the will of God in Christ Jesus concerning you.")
    add("1 Thessalonians", 5, 24, "Faithful is he that calleth you, who also will do it.")

    // 2 Thessalonians
    add("2 Thessalonians", 3, 3, "But the Lord is faithful, who shall stablish you, and keep you from evil.")

    // 1 Timothy
    add("1 Timothy", 1, 15, "This is a faithful saying, and worthy of all acceptation, that Christ Jesus came into the world to save sinners; of whom I am chief.")
    add("1 Timothy", 4, 12, "Let no man despise thy youth; but be thou an example of the believers, in word, in conversation, in charity, in spirit, in faith, in purity.")
    add("1 Timothy", 6, 12, "Fight the good fight of faith, lay hold on eternal life, whereunto thou art also called, and hast professed a good profession before many witnesses.")

    // 2 Timothy
    add("2 Timothy", 1, 7, "For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.")
    add("2 Timothy", 2, 15, "Study to shew thyself approved unto God, a workman that needeth not to be ashamed, rightly dividing the word of truth.")
    add("2 Timothy", 3, 16, "All scripture is given by inspiration of God, and is profitable for doctrine, for reproof, for correction, for instruction in righteousness.")
    add("2 Timothy", 3, 17, "That the man of God may be perfect, throughly furnished unto all good works.")
    add("2 Timothy", 4, 7, "I have fought a good fight, I have finished my course, I have kept the faith.")

    // Titus
    add("Titus", 2, 11, "For the grace of God that bringeth salvation hath appeared to all men,")
    add("Titus", 2, 12, "Teaching us that, denying ungodliness and worldly lusts, we should live soberly, righteously, and godly, in this present world.")

    // Hebrews
    add("Hebrews", 4, 12, "For the word of God is quick, and powerful, and sharper than any twoedged sword, piercing even to the dividing asunder of soul and spirit, and of the joints and marrow, and is a discerner of the thoughts and intents of the heart.")
    add("Hebrews", 4, 16, "Let us therefore come boldly unto the throne of grace, that we may obtain mercy, and find grace to help in time of need.")
    add("Hebrews", 11, 1, "Now faith is the substance of things hoped for, the evidence of things not seen.")
    add("Hebrews", 11, 6, "But without faith it is impossible to please him: for he that cometh to God must believe that he is, and that he is a rewarder of them that diligently seek him.")
    add("Hebrews", 12, 1, "Wherefore seeing we also are compassed about with so great a cloud of witnesses, let us lay aside every weight, and the sin which doth so easily beset us, and let us run with patience the race that is set before us,")
    add("Hebrews", 12, 2, "Looking unto Jesus the author and finisher of our faith; who for the joy that was set before him endured the cross, despising the shame, and is set down at the right hand of the throne of God.")
    add("Hebrews", 13, 5, "Let your conversation be without covetousness; and be content with such things as ye have: for he hath said, I will never leave thee, nor forsake thee.")
    add("Hebrews", 13, 8, "Jesus Christ the same yesterday, and to day, and for ever.")

    // James
    add("James", 1, 2, "My brethren, count it all joy when ye fall into divers temptations.")
    add("James", 1, 3, "Knowing this, that the trying of your faith worketh patience.")
    add("James", 1, 5, "If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not; and it shall be given him.")
    add("James", 1, 17, "Every good gift and every perfect gift is from above, and cometh down from the Father of lights, with whom is no variableness, neither shadow of turning.")
    add("James", 1, 22, "But be ye doers of the word, and not hearers only, deceiving your own selves.")
    add("James", 4, 7, "Submit yourselves therefore to God. Resist the devil, and he will flee from you.")
    add("James", 4, 8, "Draw nigh to God, and he will draw nigh to you.")
    add("James", 4, 10, "Humble yourselves in the sight of the Lord, and he shall lift you up.")

    // 1 Peter
    add("1 Peter", 1, 15, "But as he which hath called you is holy, so be ye holy in all manner of conversation.")
    add("1 Peter", 2, 9, "But ye are a chosen generation, a royal priesthood, an holy nation, a peculiar people; that ye should shew forth the praises of him who hath called you out of darkness into his marvellous light.")
    add("1 Peter", 3, 15, "But sanctify the Lord God in your hearts: and be ready always to give an answer to every man that asketh you a reason of the hope that is in you with meekness and fear.")
    add("1 Peter", 4, 8, "And above all things have fervent charity among yourselves: for charity shall cover the multitude of sins.")
    add("1 Peter", 5, 6, "Humble yourselves therefore under the mighty hand of God, that he may exalt you in due time.")
    add("1 Peter", 5, 7, "Casting all your care upon him; for he careth for you.")

    // 2 Peter
    add("2 Peter", 1, 3, "According as his divine power hath given unto us all things that pertain unto life and godliness, through the knowledge of him that hath called us to glory and virtue.")
    add("2 Peter", 1, 4, "Whereby are given unto us exceeding great and precious promises: that by these ye might be partakers of the divine nature, having escaped the corruption that is in the world through lust.")
    add("2 Peter", 3, 9, "The Lord is not slack concerning his promise, as some men count slackness; but is longsuffering to us-ward, not willing that any should perish, but that all should come to repentance.")
    add("2 Peter", 3, 18, "But grow in grace, and in the knowledge of our Lord and Saviour Jesus Christ. To him be glory both now and for ever. Amen.")

    // 1 John
    add("1 John", 1, 9, "If we confess our sins, he is faithful and just to forgive us our sins, and to cleanse us from all unrighteousness.")
    add("1 John", 3, 1, "Behold, what manner of love the Father hath bestowed upon us, that we should be called the sons of God: therefore the world knoweth us not, because it knew him not.")
    add("1 John", 3, 18, "My little children, let us not love in word, neither in tongue; but in deed and in truth.")
    add("1 John", 4, 4, "Ye are of God, little children, and have overcome them: because greater is he that is in you, than he that is in the world.")
    add("1 John", 4, 7, "Beloved, let us love one another: for love is of God; and every one that loveth is born of God, and knoweth God.")
    add("1 John", 4, 8, "He that loveth not knoweth not God; for God is love.")
    add("1 John", 4, 9, "In this was manifested the love of God toward us, because that God sent his only begotten Son into the world, that we might live through him.")
    add("1 John", 4, 18, "There is no fear in love; but perfect love casteth out fear: because fear hath torment. He that feareth is not made perfect in love.")

    // Jude
    add("Jude", 1, 20, "But ye, beloved, building up yourselves on your most holy faith, praying in the Holy Ghost,")
    add("Jude", 1, 21, "Keep yourselves in the love of God, looking for the mercy of our Lord Jesus Christ unto eternal life.")

    // Revelation
    add("Revelation", 1, 8, "I am Alpha and Omega, the beginning and the ending, saith the Lord, which is, and which was, and which is to come, the Almighty.")
    add("Revelation", 3, 20, "Behold, I stand at the door, and knock: if any man hear my voice, and open the door, I will come in to him, and will sup with him, and he with me.")
    add("Revelation", 21, 4, "And God shall wipe away all tears from their eyes; and there shall be no more death, neither sorrow, nor crying, neither shall there be any more pain: for the former things are passed away.")
    add("Revelation", 21, 7, "He that overcometh shall inherit all things; and I will be his God, and he shall be my son.")
    add("Revelation", 22, 13, "I am Alpha and Omega, the beginning and the end, the first and the last.")
    add("Revelation", 22, 20, "He which testifieth these things saith, Surely I come quickly. Amen. Even so, come, Lord Jesus.")

    return verses
}
