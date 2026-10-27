@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.ui.quiz.QuizPlayUiState
import pl.przypominajka.ui.quiz.QuizPlayViewModel
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.util.pluralPl

/** Nauka na odwracanych karteczkach oraz test z odpowiedziami przypiętymi pod pytaniem. */
@Composable
fun TablicaQuizPlayScreen(
    onClose: () -> Unit,
    viewModel: QuizPlayViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LookTheme.tablica
    val scroll = rememberScrollState()
    LaunchedEffect(state.index, state.finished, state.isReplay) { scroll.scrollTo(0) }
    TablicaScreen {
        Column(Modifier.fillMaxSize()) {
            TablicaTopBar(state.title.ifBlank { "Quiz" }, onClose,
                subtitle = (if (state.mode == QuizMode.CARDS) "Fiszki" else "Test ABCD") +
                    if (state.isReplay) " · Powtórka trudnych" else "")
            Column(
                modifier = Modifier.weight(1f).verticalScroll(scroll)
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = LocalDockPadding.current + 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                when {
                    state.isLoading -> Text("Wczytywanie pytań…", style = TablicaType.Body)
                    state.finished -> ResultsNote(state, viewModel::replayWrong, viewModel::restart, onClose)
                    else -> state.current?.let { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel(if (state.mode == QuizMode.CARDS) "Fiszki" else "Test ABCD",
                                trailing = "${state.index + 1} / ${state.total}")
                            LinearProgressIndicator(
                                progress = { (state.index + 1).toFloat() / state.total.coerceAtLeast(1) },
                                color = colors.ink, trackColor = colors.dot, modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (state.mode == QuizMode.CARDS) {
                            key(item.question.id, state.isReplay) {
                                FlashNote(item.question.question, item.question.answer, state.revealed, viewModel::reveal)
                            }
                            if (!state.revealed) {
                                InkButton("Pokaż odpowiedź", viewModel::reveal, Modifier.fillMaxWidth())
                            } else {
                                Text("Czy znasz odpowiedź?", style = TablicaType.Subtitle)
                                // Osobne wiersze mieszczą też większą czcionkę systemową.
                                InkButton("Umiem", { viewModel.answerCard(true) }, Modifier.fillMaxWidth())
                                OutlineInkButton("Jeszcze nie", { viewModel.answerCard(false) }, Modifier.fillMaxWidth())
                            }
                        } else {
                            Note(colors.yellow, Modifier.fillMaxWidth(), tilt = -0.8f, decoration = NoteDecoration.Tape,
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Pytanie", style = TablicaType.Tag)
                                Text(item.question.question, style = TablicaType.NoteTitle)
                            }
                            item.options.forEachIndexed { index, option ->
                                key(item.question.id, index, state.isReplay) {
                                    AnswerNote(
                                        letter = ('A' + index).toString(),
                                        text = option,
                                        correct = option == item.question.answer,
                                        selected = state.selectedOption,
                                        index = index,
                                        onClick = { viewModel.selectOption(option) },
                                    )
                                }
                            }
                            if (state.selectedOption != null) {
                                Text(if (state.selectedOption == item.question.answer) "Dobra odpowiedź!"
                                    else "Poprawna odpowiedź: ${item.question.answer}", style = TablicaType.Strong,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                InkButton(if (state.index + 1 >= state.total) "Zobacz wynik" else "Dalej",
                                    viewModel::next, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashNote(question: String, answer: String, revealed: Boolean, onReveal: () -> Unit) {
    val rotation by animateFloatAsState(if (revealed) 180f else 0f, tween(240), label = "odwrocenieFiszki")
    val back = rotation > 90f
    Note(
        color = if (back) LookTheme.tablica.mint else LookTheme.tablica.yellow,
        modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp).graphicsLayer {
            rotationY = rotation
            cameraDistance = 14f * density
        },
        decoration = NoteDecoration.Tape,
        onClick = if (!revealed) onReveal else null,
        onClickLabel = "Pokaż odpowiedź",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().graphicsLayer { rotationY = if (back) 180f else 0f },
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(if (back) "Odpowiedź" else "Pytanie", style = TablicaType.Tag)
            Text(if (back) answer else question, style = TablicaType.NoteTitle,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
            if (!back) Text("Dotknij, żeby odwrócić", style = TablicaType.Hint,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AnswerNote(letter: String, text: String, correct: Boolean, selected: String?, index: Int, onClick: () -> Unit) {
    val answered = selected != null
    val chosen = selected == text
    val colors = LookTheme.tablica
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(answered, chosen) {
        if (answered && chosen && !correct) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val paper = when {
        answered && correct -> colors.mint
        answered && chosen -> colors.pink
        else -> colors.blue
    }
    Note(paper, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), tilt = noteTilt(index) * 0.5f,
        onClick = if (!answered) onClick else null,
        onClickLabel = "Wybierz odpowiedź $letter",
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$letter. $text", style = TablicaType.Strong)
        if (answered && (correct || chosen)) {
            Text(if (correct) "Dobra odpowiedź" else "Zła odpowiedź", style = TablicaType.Hint)
        }
    }
}

@Composable
private fun ResultsNote(state: QuizPlayUiState, onReplayWrong: () -> Unit, onRestart: () -> Unit, onClose: () -> Unit) {
    val score = state.scorePercent
    val wrong = state.wrongIds.size
    val paper = if (score >= 70) LookTheme.tablica.mint else LookTheme.tablica.yellow
    Note(paper, Modifier.fillMaxWidth(), tilt = -1f, decoration = NoteDecoration.Pin,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (state.isReplay) "Wynik powtórki" else "Twój wynik", style = TablicaType.NoteTitle)
        Text("$score%", style = TablicaType.ScreenTitle, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        LinearProgressIndicator(progress = { score / 100f }, color = paper.ink, trackColor = paper.rule,
            modifier = Modifier.fillMaxWidth())
        Text(when {
            state.total == 0 -> "Brak pytań"
            score >= 90 -> "Świetnie! Jesteś gotowy."
            score >= 70 -> "Dobrze idzie – jeszcze trochę."
            score >= 50 -> "Ponad połowa! Powtórz trudne pytania."
            else -> "Na spokojnie – powtórz materiał."
        }, style = TablicaType.NoteText)
        Text("${state.correctIds.size} z ${state.total} " +
            pluralPl(state.total, "odpowiedź dobra", "odpowiedzi dobre", "odpowiedzi dobrych") +
            if (state.isReplay) " (powtórka)" else "", style = TablicaType.BodyLight)
        if (state.isReplay) Text("Wynik powtórki nie trafia do statystyk.", style = TablicaType.Hint)
        NoteDivider()
        if (wrong > 0) InkButton("Powtórz trudne ($wrong)", onReplayWrong, Modifier.fillMaxWidth())
        OutlineInkButton("Cały zestaw jeszcze raz", onRestart, Modifier.fillMaxWidth())
        InkTextButton("Zakończ", onClose, Modifier.fillMaxWidth())
    }
}
