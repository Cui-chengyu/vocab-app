package com.cui.vocab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun QuizScreen(vm: MainViewModel) {
    val state = vm.quiz
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.running) {
            RunningQuiz(vm, state)
        } else {
            QuizSetup(vm, state)
        }
    }
}

@Composable
private fun QuizSetup(vm: MainViewModel, state: QuizState) {
    // 词库规模
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "${vm.words.size}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "个单词参与检测",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "每次从整个词库随机抽一个，同一题不会连着重复",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }

    Text(
        text = "出题方向",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectPill("英 → 中", state.direction == QuizDir.EN_TO_CN) { vm.startQuiz(QuizDir.EN_TO_CN) }
        SelectPill("中 → 英", state.direction == QuizDir.CN_TO_EN) { vm.startQuiz(QuizDir.CN_TO_EN) }
        SelectPill("随机", state.direction == QuizDir.MIX) { vm.startQuiz(QuizDir.MIX) }
    }
    Caption(
        when (state.direction) {
            QuizDir.EN_TO_CN -> "看英文单词，写出中文释义。"
            QuizDir.CN_TO_EN -> "看中文释义，写出英文单词。"
            QuizDir.MIX -> "每道题随机决定方向。"
        }
    )

    Button(
        onClick = { vm.startQuiz(state.direction) },
        enabled = vm.words.isNotEmpty(),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
    ) {
        Text(
            text = "开始检测",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }

    Caption("答题时可以随时「提示」看答案、「下一题」跳过，两者都不计入对错，也不会写进词库统计。")

    state.summary?.let { summary ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "上次成绩",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun RunningQuiz(vm: MainViewModel, state: QuizState) {
    val word = state.question
    val focus = remember { FocusRequester() }

    LaunchedEffect(word?.id, state.submitted) {
        if (!state.submitted) {
            runCatching { focus.requestFocus() }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatPill(
            value = "第 ${state.qIndex} 题",
            label = if (state.ask == AskKind.EN_TO_CN) "英 → 中" else "中 → 英",
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = { vm.stopQuiz() }, shape = MaterialTheme.shapes.extraLarge) {
            Text("停止")
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        StatPill(
            value = "${state.correct}",
            label = "对",
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        StatPill(
            value = "${state.wrong}",
            label = "错",
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
        StatPill(value = "${state.streak}", label = "连对")
        StatPill(value = "${state.bestStreak}", label = "最长")
    }

    if (state.hintCount > 0 || state.skipCount > 0) {
        Caption("看答案 ${state.hintCount} 次 · 跳过 ${state.skipCount} 题")
    }

    if (word != null) {
        // 题目卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (state.ask == AskKind.EN_TO_CN) "这个单词是什么意思？" else "这个词的中文释义是什么？",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
                Text(
                    text = if (state.ask == AskKind.EN_TO_CN) word.word else word.translation,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = vm::onQuizInput,
            label = { Text(if (state.ask == AskKind.EN_TO_CN) "填写中文释义" else "填写英文单词") },
            enabled = !state.submitted,
            singleLine = false,
            minLines = 1,
            maxLines = 4,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (state.submitted) vm.nextQuestion() else vm.submitAnswer()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )

        if (!state.submitted) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { vm.revealAnswer() },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) { Text("提示") }
                OutlinedButton(
                    onClick = { vm.skipQuestion() },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) { Text("下一题") }
            }
            Caption("提示：直接给出答案，不计对错 · 下一题：跳过本题")
        }

        if (state.submitted) {
            val ok = state.lastCorrect
            val hinted = state.hinted
            val container = when {
                hinted -> MaterialTheme.colorScheme.secondaryContainer
                ok -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
            val onContainer = when {
                hinted -> MaterialTheme.colorScheme.onSecondaryContainer
                ok -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onErrorContainer
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = container),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = when {
                            hinted -> "已给出答案（不计对错）"
                            ok -> "答对了 🎉"
                            else -> "答错了"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onContainer,
                    )
                    Text(
                        text = "正确答案",
                        style = MaterialTheme.typography.labelMedium,
                        color = onContainer.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "${word.word} — ${word.translation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainer,
                    )
                }
            }
        }

        Button(
            onClick = { if (state.submitted) vm.nextQuestion() else vm.submitAnswer() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(
                text = if (state.submitted) "下一题" else "提交答案",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
