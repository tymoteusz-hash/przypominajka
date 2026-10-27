@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.ui.quiz.QuizPlayUiState
import pl.przypominajka.ui.quiz.QuizPlayViewModel
import pl.przypominajka.util.pluralPl
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

@Composable
fun DzwonQuizPlayScreen(
    onClose: () -> Unit,
    viewModel: QuizPlayViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    DzwonSheetScaffold(
        title = if (state.mode == QuizMode.CARDS) "Fiszki" else "Test ABCD",
        subtitle = state.title,
        onClose = onClose,
        headerExtra = {
            DzwonBellInRing(
                progress = when {
                    state.isLoading -> 0f
                    state.finished -> state.scorePercent / 100f
                    else -> state.index.toFloat() / state.total.coerceAtLeast(1)
                },
                size = 84.dp,
                contentDescription = if (state.finished) "Wynik: ${state.scorePercent}%" else "Ukończono ${state.index} z ${state.total} pytań",
            )
        },
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.isLoading -> Text("Wczytywanie pytań…", style = DzwonType.Body, color = c.ink2)
                state.finished -> QuizResults(state, viewModel::replayWrong, viewModel::restart, onClose)
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (state.isReplay) "Powtórka trudnych pytań" else "Twoja powtórka",
                            style = DzwonType.CaptionStrong, color = c.goldText, modifier = Modifier.weight(1f),
                        )
                        Text("${state.index + 1} / ${state.total}", style = DzwonType.TimeLarge, color = c.ink)
                    }
                    LinearProgressIndicator(
                        progress = { (state.index + 1).toFloat() / state.total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(), color = c.gold, trackColor = c.track,
                    )
                    state.current?.let { item ->
                        if (state.mode == QuizMode.CARDS) {
                            key(item.question.id, state.isReplay) {
                                FlashCard(item.question.question, item.question.answer, state.revealed, viewModel::reveal)
                            }
                            if (!state.revealed) {
                                DzwonPrimaryButton("Pokaż odpowiedź", viewModel::reveal, Modifier.fillMaxWidth())
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DzwonSecondaryButton("Jeszcze nie", { viewModel.answerCard(false) }, Modifier.weight(1f))
                                    DzwonPrimaryButton("Umiem", { viewModel.answerCard(true) }, Modifier.weight(1f))
                                }
                            }
                        } else {
                            DzwonCard(color = c.field, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
                                Text("PYTANIE", style = DzwonType.CaptionStrong, color = c.goldText)
                                Text(item.question.question, style = DzwonType.SheetTitle, color = c.ink,
                                    modifier = Modifier.padding(top = 10.dp))
                            }
                            item.options.forEachIndexed { index, option ->
                                key(item.question.id, option, state.isReplay) {
                                    AnswerOption(
                                        letter = ('A' + index).toString(), text = option,
                                        correct = option == item.question.answer, selectedOption = state.selectedOption,
                                        onClick = { viewModel.selectOption(option) },
                                    )
                                }
                            }
                            if (state.selectedOption != null) {
                                DzwonPrimaryButton(
                                    if (state.index + 1 >= state.total) "Zobacz wynik" else "Dalej",
                                    viewModel::next, Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashCard(question: String, answer: String, revealed: Boolean, onReveal: () -> Unit) {
    val c = LookTheme.dzwon
    val rotation by animateFloatAsState(if (revealed) 180f else 0f, tween(300), label = "dzwonFlip")
    val back = rotation > 90f
    Surface(
        onClick = onReveal, enabled = !revealed,
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            rotationY = rotation
            cameraDistance = 14f * density
        },
        color = if (back) c.halo else c.field,
        shape = DzwonCardShape, border = BorderStroke(1.dp, if (back) c.gold else c.line),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp)
                .graphicsLayer { rotationY = if (back) 180f else 0f }.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text(if (back) "ODPOWIEDŹ" else "PYTANIE", style = DzwonType.CaptionStrong, color = c.goldText)
            Text(if (back) answer else question, style = DzwonType.SheetTitle, color = c.ink, textAlign = TextAlign.Center)
            if (!back) Text("Dotknij, żeby odwrócić", style = DzwonType.Caption, color = c.ink2)
        }
    }
}

@Composable
private fun AnswerOption(letter: String, text: String, correct: Boolean, selectedOption: String?, onClick: () -> Unit) {
    val c = LookTheme.dzwon
    val answered = selectedOption != null
    val selected = selectedOption == text
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    val shakePx = with(LocalDensity.current) { 8.dp.toPx() }
    LaunchedEffect(answered, selected) {
        if (answered && selected && !correct) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shake.animateTo(0f, keyframes {
                durationMillis = 300
                -1f at 45
                1f at 105
                -0.6f at 165
                0.3f at 230
            })
        }
    }
    val error = MaterialTheme.colorScheme.error
    val accent = when {
        answered && correct -> c.goldText
        answered && selected -> error
        else -> c.ink2
    }
    Surface(
        onClick = onClick, enabled = !answered, shape = RoundedCornerShape(20.dp),
        color = when {
            answered && correct -> c.halo
            answered && selected -> MaterialTheme.colorScheme.errorContainer
            else -> c.surface
        },
        border = BorderStroke(if (answered && (correct || selected)) 2.dp else 1.dp,
            if (answered && (correct || selected)) accent else c.line),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .graphicsLayer { translationX = shake.value * shakePx },
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(letter, style = DzwonType.TimeLarge, color = accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text, style = DzwonType.Body, color = c.ink)
                if (answered && (correct || selected)) {
                    Text(if (correct) "Dobra odpowiedź" else "Zła odpowiedź", style = DzwonType.CaptionStrong, color = accent,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            if (answered && (correct || selected)) {
                Icon(if (correct) DzwonIcons.Check else DzwonIcons.Close, contentDescription = null, tint = accent)
            }
        }
    }
}

@Composable
private fun QuizResults(state: QuizPlayUiState, onReplayWrong: () -> Unit, onRestart: () -> Unit, onClose: () -> Unit) {
    val c = LookTheme.dzwon
    val score = state.scorePercent
    val displayedScore = remember { Animatable(0f) }
    LaunchedEffect(score) { displayedScore.animateTo(score.toFloat(), tween(350)) }
    Column(
        modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            ProgressRing(score / 100f, Modifier.fillMaxSize(), strokeWidth = 10.dp)
            Text("${displayedScore.value.roundToInt()}%", style = DzwonType.ScreenTitle, color = c.goldText)
            if (state.total > 0 && score >= 80) ResultCelebration(Modifier.fillMaxSize())
        }
        Text(
            when {
                state.total == 0 -> "Brak pytań"
                score >= 90 -> "Świetnie! Jesteś gotowy."
                score >= 70 -> "Dobrze idzie – jeszcze trochę."
                score >= 50 -> "Ponad połowa! Powtórz trudne pytania."
                else -> "Na spokojnie – powtórz materiał."
            },
            style = DzwonType.SheetTitle, color = c.ink, textAlign = TextAlign.Center,
        )
        Text(
            "${state.correctIds.size} z ${state.total} " + pluralPl(state.total, "odpowiedź dobra", "odpowiedzi dobre", "odpowiedzi dobrych") +
                if (state.isReplay) " (powtórka)" else "",
            style = DzwonType.Body, color = c.ink2, textAlign = TextAlign.Center,
        )
        if (state.total > 0 && score >= 80) {
            DzwonNotice("Dobra robota!", "Ta powtórka przyniosła świetny wynik.", containerColor = c.halo, icon = DzwonIcons.Check)
        }
        if (state.wrongIds.isNotEmpty()) {
            DzwonPrimaryButton("Powtórz trudne (${state.wrongIds.size})", onReplayWrong, Modifier.fillMaxWidth())
        }
        DzwonSecondaryButton("Cały zestaw jeszcze raz", onRestart, Modifier.fillMaxWidth())
        DzwonTextButton("Zakończ", onClose, Modifier.fillMaxWidth())
    }
}

/** Krótki rozbłysk złotych drobinek po udanej powtórce, bez zapętlenia. */
@Composable
private fun ResultCelebration(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val gold = LookTheme.dzwon.gold
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(350)) }
    Canvas(modifier) {
        val p = progress.value
        if (p > 0f && p < 1f) {
            repeat(16) { index ->
                val angle = index * Math.PI * 2 / 16
                val radius = size.minDimension * (0.2f + p * 0.3f)
                drawCircle(
                    color = gold.copy(alpha = 1f - p), radius = 3.dp.toPx(),
                    center = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * radius,
                        center.y + kotlin.math.sin(angle).toFloat() * radius,
                    ),
                )
            }
        }
    }
}
