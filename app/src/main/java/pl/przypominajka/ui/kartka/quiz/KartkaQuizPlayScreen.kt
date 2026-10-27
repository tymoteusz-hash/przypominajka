@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.quiz

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.ui.quiz.QuizPlayUiState
import pl.przypominajka.ui.quiz.QuizPlayViewModel
import pl.przypominajka.util.pluralPl

@Composable
fun KartkaQuizPlayScreen(
    onClose: () -> Unit,
    viewModel: QuizPlayViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.kartka
    KartkaPage(
        title = state.title.ifBlank { "Quiz" }, onBack = onClose,
        subtitle = if (state.mode == QuizMode.CARDS) "Fiszki" else "Test ABCD",
        verticalSpacing = 16.dp,
    ) {
        when {
            state.isLoading -> Text("Wczytywanie pytań…", style = KartkaType.BodySmall, color = c.ink2)
            state.finished -> Results(state, viewModel::replayWrong, viewModel::restart, onClose)
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KartkaEyebrow(if (state.isReplay) "Powtórka trudnych" else "Przećwicz zestaw", Modifier.weight(1f))
                    KartkaChip("${state.index + 1} / ${state.total}")
                }
                LinearProgressIndicator(progress = { (state.index + 1).toFloat() / state.total.coerceAtLeast(1) },
                    color = c.accent, trackColor = c.surface, modifier = Modifier.fillMaxWidth())
                state.current?.let { item ->
                    if (state.mode == QuizMode.CARDS) {
                        key(state.index, state.isReplay) {
                            StudyCard(item.question.question, item.question.answer, state.revealed, viewModel::reveal)
                        }
                        if (!state.revealed) {
                            KartkaButton("Pokaż odpowiedź", viewModel::reveal, Modifier.fillMaxWidth())
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                KartkaOutlineButton("Jeszcze nie", { viewModel.answerCard(false) }, Modifier.weight(1f))
                                KartkaButton("Umiem", { viewModel.answerCard(true) }, Modifier.weight(1f))
                            }
                        }
                    } else {
                        KartkaCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            KartkaEyebrow("Pytanie")
                            Text(item.question.question, style = KartkaType.TitleLarge, color = c.ink)
                        }
                        item.options.forEachIndexed { index, option ->
                            key(state.index, state.isReplay, index) {
                                AnswerRow(('A' + index).toString(), option, option == item.question.answer,
                                    state.selectedOption, { viewModel.selectOption(option) })
                            }
                        }
                        if (state.selectedOption != null) {
                            Text(
                                if (state.selectedOption == item.question.answer) "Dobra odpowiedź."
                                else "Zła odpowiedź. Poprawna: ${item.question.answer}",
                                style = KartkaType.BodySmall, color = c.ink,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                            KartkaButton(if (state.index + 1 >= state.total) "Zobacz wynik" else "Dalej",
                                viewModel::next, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyCard(question: String, answer: String, revealed: Boolean, onReveal: () -> Unit) {
    val c = LookTheme.kartka
    KartkaCard(
        modifier = Modifier.heightIn(min = 260.dp),
        onClick = if (revealed) null else onReveal,
        onClickLabel = "Pokaż odpowiedź",
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Crossfade(
            targetState = revealed,
            animationSpec = tween(150),
            label = "Odpowiedź na fiszce",
        ) { showAnswer ->
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                KartkaEyebrow(if (showAnswer) "Odpowiedź" else "Pytanie", color = c.accent)
                Text(if (showAnswer) answer else question, style = KartkaType.TitleLarge, color = c.ink,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (!showAnswer) {
                    Text("Dotknij, żeby pokazać odpowiedź", style = KartkaType.Caption, color = c.ink2)
                }
            }
        }
    }
}

@Composable
private fun AnswerRow(letter: String, text: String, correct: Boolean, selected: String?, onClick: () -> Unit) {
    val c = LookTheme.kartka
    val answered = selected != null
    val chosen = selected == text
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(answered, chosen) {
        if (answered && chosen && !correct) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val error = MaterialTheme.colorScheme.error
    val highlighted = answered && (correct || chosen)
    val border = when {
        answered && correct -> c.accent
        answered && chosen -> error
        else -> c.line
    }
    Surface(
        onClick = onClick, enabled = !answered,
        color = when {
            answered && correct -> c.accentSoft
            answered && chosen -> MaterialTheme.colorScheme.errorContainer
            else -> c.bg
        },
        contentColor = if (answered && chosen && !correct) MaterialTheme.colorScheme.onErrorContainer else c.ink,
        shape = KartkaDefaults.ButtonShape,
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(letter, style = KartkaType.CaptionStrong)
            Text(text, Modifier.weight(1f), style = KartkaType.Body)
            if (highlighted) {
                Icon(if (correct) KartkaIcons.CheckThin else KartkaIcons.Close,
                    if (correct) "Dobra odpowiedź" else "Zła odpowiedź", tint = border, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Results(state: QuizPlayUiState, onReplay: () -> Unit, onRestart: () -> Unit, onClose: () -> Unit) {
    val c = LookTheme.kartka
    val score = state.scorePercent
    KartkaEyebrow(if (state.isReplay) "Wynik powtórki" else "Wynik")
    KartkaCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("$score%", style = KartkaType.Hero, color = c.accent)
        LinearProgressIndicator(progress = { score / 100f }, color = c.accent, trackColor = c.line,
            modifier = Modifier.fillMaxWidth())
        Text(when {
            state.total == 0 -> "Brak pytań"
            score >= 90 -> "Świetnie! Jesteś gotowy."
            score >= 70 -> "Dobrze idzie – jeszcze trochę."
            score >= 50 -> "Ponad połowa! Powtórz trudne pytania."
            else -> "Na spokojnie – powtórz materiał."
        }, style = KartkaType.TitleLarge, color = c.ink)
        Text("${state.correctIds.size} z ${state.total} " +
            pluralPl(state.total, "odpowiedź dobra", "odpowiedzi dobre", "odpowiedzi dobrych") +
            if (state.isReplay) " (powtórka)" else "", style = KartkaType.BodySmall, color = c.ink2)
    }
    if (state.wrongIds.isNotEmpty()) {
        KartkaButton("Powtórz trudne (${state.wrongIds.size})", onReplay, Modifier.fillMaxWidth(), icon = KartkaIcons.Refresh)
    }
    KartkaOutlineButton("Cały zestaw jeszcze raz", onRestart, Modifier.fillMaxWidth())
    KartkaTextButton("Zakończ", onClose, Modifier.fillMaxWidth())
}
