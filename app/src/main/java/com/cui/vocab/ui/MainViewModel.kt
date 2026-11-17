package com.cui.vocab.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cui.vocab.data.AddOutcome
import com.cui.vocab.data.Word
import com.cui.vocab.data.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 出题方向 */
enum class QuizDir { EN_TO_CN, CN_TO_EN, MIX }

/** 本题问的是什么 */
enum class AskKind { EN_TO_CN, CN_TO_EN }

data class QuizState(
    val running: Boolean = false,
    val direction: QuizDir = QuizDir.MIX,
    val question: Word? = null,
    val ask: AskKind = AskKind.EN_TO_CN,
    val input: String = "",
    val submitted: Boolean = false,
    val lastCorrect: Boolean = false,
    val correct: Int = 0,
    val wrong: Int = 0,
    val answered: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** 本题是否已经点过「提示」（直接给出答案，不计对错） */
    val hinted: Boolean = false,
    /** 本次一共看了几次答案 */
    val hintCount: Int = 0,
    /** 本次一共跳过了几题 */
    val skipCount: Int = 0,
    /** 当前是第几题（1 起） */
    val qIndex: Int = 1,
    val summary: String? = null,
) {
    val total: Int get() = correct + wrong
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WordRepository(app)

    /** 词库（已按 A→Z 排序） */
    var words by mutableStateOf<List<Word>>(emptyList())
        private set

    var loading by mutableStateOf(true)
        private set

    /** 顶部提示（snackbar） */
    var message by mutableStateOf<String?>(null)

    /** 需要弹出的「编辑单词」对话框（添加重复单词时也会走到这里） */
    var editing by mutableStateOf<Word?>(null)

    /** 添加成功计数，用于让添加页清空输入框 */
    var addOkCount by mutableIntStateOf(0)
        private set

    var quiz by mutableStateOf(QuizState())
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.seedIfNeeded()
            val list = repo.all()
            withContext(Dispatchers.Main) {
                words = list
                loading = false
            }
        }
    }

    // ---------------- 词库读写 ----------------

    private fun reloadAsync() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.all()
            withContext(Dispatchers.Main) { words = list }
        }
    }

    /** 添加单词：已存在则直接打开编辑页 */
    fun addWord(rawWord: String, rawTranslation: String) {
        val w = rawWord.trim()
        val t = rawTranslation.trim()
        if (w.isEmpty()) {
            message = "请先输入单词"
            return
        }
        if (t.isEmpty()) {
            message = "请先输入中文释义"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.add(w, t)
            val list = if (result.outcome == AddOutcome.ADDED) repo.all() else emptyList()
            withContext(Dispatchers.Main) {
                when (result.outcome) {
                    AddOutcome.ADDED -> {
                        words = list
                        addOkCount += 1
                        message = "已添加：$w"
                    }
                    AddOutcome.EXISTS -> {
                        message = "「${result.existing?.word ?: w}」已在词库中，已为你打开编辑页"
                        editing = result.existing
                    }
                    AddOutcome.INVALID -> message = "单词或释义不能为空"
                }
            }
        }
    }

    fun saveEdit(id: Long, word: String, translation: String) {
        val w = word.trim()
        val t = translation.trim()
        if (w.isEmpty() || t.isEmpty()) {
            message = "单词和释义都不能为空"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.update(id, w, t)
            val list = if (result.outcome == AddOutcome.ADDED) repo.all() else emptyList()
            withContext(Dispatchers.Main) {
                when (result.outcome) {
                    AddOutcome.ADDED -> {
                        words = list
                        editing = null
                        message = "已保存：$w"
                    }
                    AddOutcome.EXISTS -> message = "词库里已经有「${result.existing?.word ?: w}」了"
                    AddOutcome.INVALID -> message = "单词和释义都不能为空"
                }
            }
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(word.id)
            val list = repo.all()
            withContext(Dispatchers.Main) {
                words = list
                editing = null
                message = "已删除：${word.word}"
            }
        }
    }

    fun dismissEdit() {
        editing = null
    }

    // ---------------- 检测 ----------------

    fun startQuiz(dir: QuizDir) {
        if (words.isEmpty()) {
            message = "词库还是空的，先添加几个单词吧"
            return
        }
        val first = words.random()
        quiz = QuizState(
            running = true,
            direction = dir,
            question = first,
            ask = pickAsk(dir),
            correct = 0,
            wrong = 0,
            answered = 0,
            streak = 0,
            bestStreak = 0,
            qIndex = 1,
        )
    }

    fun stopQuiz() {
        val q = quiz
        val extra = buildString {
            if (q.hintCount > 0) append("，看答案 ${q.hintCount} 次")
            if (q.skipCount > 0) append("，跳过 ${q.skipCount} 题")
        }
        val text = if (q.answered == 0) {
            if (q.hintCount == 0 && q.skipCount == 0) "已停止，本次还没答题"
            else "已停止，本次还没答题（看答案 ${q.hintCount} 次，跳过 ${q.skipCount} 题）"
        } else {
            "本次共答 ${q.answered} 题：对 ${q.correct} · 错 ${q.wrong}（最长连对 ${q.bestStreak}）$extra"
        }
        quiz = q.copy(
            running = false,
            question = null,
            submitted = false,
            hinted = false,
            input = "",
            summary = text,
        )
        message = text
    }

    fun onQuizInput(text: String) {
        quiz = quiz.copy(input = text)
    }

    fun submitAnswer() {
        val q = quiz
        val word = q.question ?: return
        if (q.submitted) return
        if (q.input.isBlank()) {
            message = "先写下你的答案"
            return
        }
        val ok = checkAnswer(word, q.ask, q.input)
        val streak = if (ok) q.streak + 1 else 0
        quiz = q.copy(
            submitted = true,
            lastCorrect = ok,
            correct = q.correct + if (ok) 1 else 0,
            wrong = q.wrong + if (ok) 0 else 1,
            answered = q.answered + 1,
            streak = streak,
            bestStreak = maxOf(q.bestStreak, streak),
        )
        viewModelScope.launch(Dispatchers.IO) { repo.recordAnswer(word.id, ok) }
    }

    /** 提示：直接把答案给出来，不计对错、不写进词库统计 */
    fun revealAnswer() {
        val q = quiz
        if (!q.running || q.submitted) return
        quiz = q.copy(
            submitted = true,
            hinted = true,
            hintCount = q.hintCount + 1,
        )
    }

    /** 跳过本题：不计对错，直接换下一题 */
    fun skipQuestion() {
        val q = quiz
        if (!q.running) return
        moveNext(skipped = !q.submitted)
    }

    fun nextQuestion() {
        moveNext(skipped = false)
    }

    private fun moveNext(skipped: Boolean) {
        val q = quiz
        if (!q.running) return
        val next = words.filter { it.id != q.question?.id }.randomOrNull() ?: words.random()
        quiz = q.copy(
            question = next,
            ask = pickAsk(q.direction),
            input = "",
            submitted = false,
            lastCorrect = false,
            hinted = false,
            qIndex = q.qIndex + 1,
            skipCount = q.skipCount + if (skipped) 1 else 0,
        )
    }

    private fun pickAsk(dir: QuizDir): AskKind = when (dir) {
        QuizDir.EN_TO_CN -> AskKind.EN_TO_CN
        QuizDir.CN_TO_EN -> AskKind.CN_TO_EN
        QuizDir.MIX -> if (Math.random() < 0.5) AskKind.EN_TO_CN else AskKind.CN_TO_EN
    }

    private fun checkAnswer(word: Word, ask: AskKind, input: String): Boolean = when (ask) {
        AskKind.EN_TO_CN -> matchTranslation(word.translation, input)
        AskKind.CN_TO_EN -> normalizeAnswer(input) == normalizeAnswer(word.word)
    }

    /** 英文答案宽松比对：忽略大小写、空格、标点 */
    private fun normalizeAnswer(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    /** 中文答案比对：整体包含 或 命中其中某一条释义 */
    private fun matchTranslation(translation: String, input: String): Boolean {
        val a = normalizeAnswer(input)
        if (a.isEmpty()) return false
        val full = normalizeAnswer(translation)
        if (full == a) return true
        val parts = translation
            .split('；', ';', '，', ',', '、', '/', '|', '（', '）', '(', ')', '：', ':')
            .map { normalizeAnswer(it) }
            .filter { it.isNotEmpty() }
        if (parts.any { it == a }) return true
        if (a.length >= 2 && parts.any { it.contains(a) }) return true
        if (a.length >= 2 && full.contains(a)) return true
        return false
    }
}
