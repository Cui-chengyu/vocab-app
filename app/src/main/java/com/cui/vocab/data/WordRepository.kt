package com.cui.vocab.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** 一个单词条目 */
data class Word(
    val id: Long,
    val word: String,
    val translation: String,
    val rightCount: Int = 0,
    val wrongCount: Int = 0,
) {
    /** 首字母，用来做 A-Z 分组 */
    val initial: Char
        get() = word.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
}

enum class AddOutcome { ADDED, EXISTS, INVALID }

data class AddResult(val outcome: AddOutcome, val existing: Word? = null)

/** 单词归一化：去空格 + 转小写，用于判重与答题比对 */
fun normalizeKey(raw: String): String = raw.trim().lowercase()

/**
 * 词库存储：SQLite。不依赖 Room / KSP，构建更快也更不容易出问题。
 * 表结构：
 *   words(id, word, word_key UNIQUE, translation, created_at, right_count, wrong_count, last_practiced)
 *   meta(k, v)
 */
class WordRepository(context: Context) {

    private val appContext: Context = context.applicationContext
    private val helper = object : SQLiteOpenHelper(appContext, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE words (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "word TEXT NOT NULL," +
                    "word_key TEXT NOT NULL UNIQUE," +
                    "translation TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL," +
                    "right_count INTEGER NOT NULL DEFAULT 0," +
                    "wrong_count INTEGER NOT NULL DEFAULT 0," +
                    "last_practiced INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL("CREATE INDEX idx_words_key ON words(word_key)")
            db.execSQL("CREATE TABLE meta (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    /** 首次启动时把 assets/words.tsv 里的 4000 个常用词灌进数据库（只做一次） */
    fun seedIfNeeded(): Boolean {
        val db = helper.writableDatabase
        if (isSeeded(db)) return false
        val lines = try {
            appContext.assets.open(ASSET_WORDS).bufferedReader().readLines()
        } catch (e: Exception) {
            emptyList()
        }
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT OR IGNORE INTO words(word, word_key, translation, created_at) VALUES(?,?,?,?)"
            )
            val now = System.currentTimeMillis()
            for (line in lines) {
                val tab = line.indexOf('\t')
                if (tab <= 0) continue
                val w = line.substring(0, tab).trim()
                val t = line.substring(tab + 1).trim()
                if (w.isEmpty() || t.isEmpty()) continue
                stmt.clearBindings()
                stmt.bindString(1, w)
                stmt.bindString(2, w.lowercase())
                stmt.bindString(3, t)
                stmt.bindLong(4, now)
                stmt.executeInsert()
            }
            db.execSQL("INSERT OR REPLACE INTO meta(k, v) VALUES('seeded', '1')")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return true
    }

    private fun isSeeded(db: SQLiteDatabase): Boolean =
        db.rawQuery("SELECT v FROM meta WHERE k = 'seeded'", null).use { it.moveToFirst() }

    fun count(): Int =
        helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM words", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    /** 全部单词，按 A→Z 排序 */
    fun all(): List<Word> =
        helper.readableDatabase.rawQuery(
            "SELECT id, word, translation, right_count, wrong_count FROM words ORDER BY word_key ASC",
            null
        ).use { c ->
            val list = ArrayList<Word>(c.count)
            while (c.moveToNext()) list.add(c.toWord())
            list
        }

    fun findByWord(word: String): Word? =
        helper.readableDatabase.rawQuery(
            "SELECT id, word, translation, right_count, wrong_count FROM words WHERE word_key = ? LIMIT 1",
            arrayOf(normalizeKey(word))
        ).use { if (it.moveToFirst()) it.toWord() else null }

    fun findById(id: Long): Word? =
        helper.readableDatabase.rawQuery(
            "SELECT id, word, translation, right_count, wrong_count FROM words WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { if (it.moveToFirst()) it.toWord() else null }

    fun add(word: String, translation: String): AddResult {
        val w = word.trim()
        val t = translation.trim()
        if (w.isEmpty() || t.isEmpty()) return AddResult(AddOutcome.INVALID)
        findByWord(w)?.let { return AddResult(AddOutcome.EXISTS, it) }
        val values = ContentValues().apply {
            put("word", w)
            put("word_key", normalizeKey(w))
            put("translation", t)
            put("created_at", System.currentTimeMillis())
        }
        val id = helper.writableDatabase.insert("words", null, values)
        return if (id == -1L) {
            AddResult(AddOutcome.EXISTS, findByWord(w))
        } else {
            AddResult(AddOutcome.ADDED)
        }
    }

    fun update(id: Long, word: String, translation: String): AddResult {
        val w = word.trim()
        val t = translation.trim()
        if (w.isEmpty() || t.isEmpty()) return AddResult(AddOutcome.INVALID)
        val key = normalizeKey(w)
        val clash = findByWord(w)
        if (clash != null && clash.id != id) return AddResult(AddOutcome.EXISTS, clash)
        val values = ContentValues().apply {
            put("word", w)
            put("word_key", key)
            put("translation", t)
        }
        val rows = helper.writableDatabase.update("words", values, "id = ?", arrayOf(id.toString()))
        return if (rows > 0) AddResult(AddOutcome.ADDED) else AddResult(AddOutcome.INVALID)
    }

    fun delete(id: Long) {
        helper.writableDatabase.delete("words", "id = ?", arrayOf(id.toString()))
    }

    /** 随机取一个单词，尽量不和上一题重复 */
    fun randomWord(excludeId: Long?): Word? {
        val db = helper.readableDatabase
        if (excludeId != null) {
            db.rawQuery(
                "SELECT id, word, translation, right_count, wrong_count FROM words WHERE id <> ? ORDER BY RANDOM() LIMIT 1",
                arrayOf(excludeId.toString())
            ).use { if (it.moveToFirst()) return it.toWord() }
        }
        db.rawQuery(
            "SELECT id, word, translation, right_count, wrong_count FROM words ORDER BY RANDOM() LIMIT 1",
            null
        ).use { if (it.moveToFirst()) return it.toWord() else return null }
    }

    fun recordAnswer(id: Long, correct: Boolean) {
        val column = if (correct) "right_count" else "wrong_count"
        helper.writableDatabase.execSQL(
            "UPDATE words SET $column = $column + 1, last_practiced = ? WHERE id = ?",
            arrayOf<Any>(System.currentTimeMillis(), id)
        )
    }

    fun clearAll() {
        helper.writableDatabase.execSQL("DELETE FROM words")
    }

    private fun Cursor.toWord() = Word(
        id = getLong(0),
        word = getString(1),
        translation = getString(2),
        rightCount = getInt(3),
        wrongCount = getInt(4),
    )

    private companion object {
        const val DB_NAME = "vocab.db"
        const val DB_VERSION = 1
        const val ASSET_WORDS = "words.tsv"
    }
}
