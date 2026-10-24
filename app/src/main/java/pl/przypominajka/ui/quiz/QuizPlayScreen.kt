package pl.przypominajka.ui.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.Confetti
import pl.przypominajka.ui.components.CountUpText
import pl.przypominajka.ui.components.HeroCard
import pl.przypominajka.ui.components.PrimaryButton
import pl.przypominajka.ui.components.ProgressBar
import pl.przypominajka.ui.components.ProgressRing
import pl.przypominajka.ui.components.RoundIconButton
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.pressScale
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.util.pluralPl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayScreen(
    onClose: () -> Unit,
    viewModel: QuizPlayViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppSurfaces.page,
            topBar = {
                BellTopBar(
                    title = state.title,
                    eyebrow = if (state.mode == QuizMode.CARDS) "Fiszki" else "Test ABCD",
                    onBack = onClose,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    state.finished -> ResultsView(
                        state = state,
                        onReplayWrong = viewModel::replayWrong,
                        onRestart = viewModel::restart,
                        onClose = onClose,
                    )

                    else -> {
                        ProgressHeader(state)
                        val item = state.current
                        if (item != null) {
                            if (state.mode == QuizMode.CARDS) {
                                // key – nowa karta zaczyna zawsze od przodu i wjeżdża od nowa.
                                key(state.index) {
                                    FlipCard(
                                        question = item.question.question,
                                        answer = item.question.answer,
                                        revealed = state.revealed,
                                        onFlip = viewModel::reveal,
                                        modifier = Modifier.appear(),
                                    )
                                }
                                if (!state.revealed) {
                                    PrimaryButton(
                                        text = "Pokaż odpowiedź",
                                        onClick = viewModel::reveal,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        PrimaryButton(
                                            text = "Jeszcze nie",
                                            onClick = { viewModel.answerCard(known = false) },
                                            containerColor = Pastel.TERRACOTTA.container,
                                            contentColor = Pastel.TERRACOTTA.content,
                                            modifier = Modifier.weight(1f),
                                        )
                                        PrimaryButton(
                                            text = "Umiem",
                                            onClick = { viewModel.answerCard(known = true) },
                                            containerColor = Pastel.SAGE.content,
                                            contentColor = Color.White,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            } else {
                                key(state.index) {
                                    QuestionBox(question = item.question.question, modifier = Modifier.appear())
                                }
                                item.options.forEachIndexed { index, option ->
                                    key(state.index, index) {
                                        OptionRow(
                                            letter = ('A' + index).toString(),
                                            text = option,
                                            isCorrect = option == item.question.answer,
                                            selectedOption = state.selectedOption,
                                            onClick = { viewModel.selectOption(option) },
                                            modifier = Modifier.appear(index + 1),
                                        )
                                    }
                                }
                                if (state.selectedOption != null) {
                                    PrimaryButton(
                                        text = if (state.index + 1 >= state.total) "Zobacz wynik" else "Dalej",
                                        onClick = viewModel::next,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .appear(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.finished && state.total > 0 && state.scorePercent >= 80) {
            Confetti(trigger = 1, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ProgressHeader(state: QuizPlayUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.mode == QuizMode.CARDS) "FISZKI" else "TEST ABCD",
                style = MaterialTheme.typography.labelSmall,
                color = AppSurfaces.accent,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.index + 1} / ${state.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProgressBar(
            progress = (state.index + 1).toFloat() / state.total.coerceAtLeast(1),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Karta, która obraca się po dotknięciu – z przodu pytanie (ciemna, z żywym tłem), z tyłu odpowiedź. */
@Composable
private fun FlipCard(
    question: String,
    answer: String,
    revealed: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 140f),
        label = "flip",
    )
    val showFront = rotation <= 90f
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(shape)
            .clickable(enabled = !revealed, onClick = onFlip),
    ) {
        if (showFront) {
            HeroCard(modifier = Modifier.heightIn(min = 300.dp), contentPadding = 26.dp) {
                Box(Modifier.fillMaxWidth().heightIn(min = 248.dp), contentAlignment = Alignment.Center) {
                    CardText(label = "PYTANIE", text = question, color = Color.White)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dotknij, żeby odwrócić", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp)
                    .graphicsLayer { rotationY = 180f }
                    .background(Pastel.SAGE.container)
                    .border(2.dp, Pastel.SAGE.content, shape)
                    .padding(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                CardText(
                    label = "ODPOWIEDŹ",
                    text = answer,
                    color = MaterialTheme.colorScheme.onSurface,
                    labelColor = Pastel.SAGE.content,
                )
            }
        }
    }
}

@Composable
private fun CardText(label: String, text: String, color: Color, labelColor: Color = color.copy(alpha = 0.65f)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        Text(text = text, style = MaterialTheme.typography.headlineSmall, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
private fun QuestionBox(question: String, modifier: Modifier = Modifier) {
    HeroCard(modifier = modifier, contentPadding = 26.dp) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CardText(label = "PYTANIE", text = question, color = Color.White)
        }
    }
}

/** Odpowiedź ABCD: po wyborze dobra robi się zielona, zła czerwona i "potrząsa" kartą. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionRow(
    letter: String,
    text: String,
    isCorrect: Boolean,
    selectedOption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val answered = selectedOption != null
    val isSelected = selectedOption == text
    val container by animateColorAsState(
        when {
            answered && isCorrect -> Pastel.SAGE.container
            answered && isSelected -> Pastel.TERRACOTTA.container
            else -> AppSurfaces.card
        },
        label = "optionBg",
    )
    val border by animateColorAsState(
        when {
            answered && isCorrect -> Pastel.SAGE.content
            answered && isSelected -> Pastel.TERRACOTTA.content
            else -> AppSurfaces.cardBorder
        },
        label = "optionBorder",
    )
    // Potrząśnięcie złej odpowiedzi.
    val shake = remember { Animatable(0f) }
    val shakePx = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(answered, isSelected) {
        if (answered && isSelected && !isCorrect) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    -1f at 60
                    1f at 140
                    -0.7f at 220
                    0.5f at 300
                    -0.2f at 360
                },
            )
        }
    }
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = !answered,
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = BorderStroke(if (answered && (isCorrect || isSelected)) 2.dp else 1.dp, border),
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.97f)
            .graphicsLayer { translationX = shake.value * shakePx },
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter, style = MaterialTheme.typography.labelLarge, color = colors.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when {
                answered && isCorrect -> Icon(Icons.Rounded.CheckCircle, contentDescription = "Dobra odpowiedź", tint = Pastel.SAGE.content)
                answered && isSelected -> Icon(Icons.Rounded.Cancel, contentDescription = "Zła odpowiedź", tint = Pastel.TERRACOTTA.content)
            }
        }
    }
}

@Composable
private fun ResultsView(
    state: QuizPlayUiState,
    onReplayWrong: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
) {
    val score = state.scorePercent
    val wrong = state.wrongIds.size
    val ringColor = when {
        score >= 80 -> Pastel.SAGE.content
        score >= 50 -> Pastel.GOLD.content
        else -> Pastel.TERRACOTTA.content
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp).appear()) {
            ProgressRing(
                progress = score / 100f,
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 16.dp,
                modifier = Modifier.fillMaxSize(),
            )
            CountUpText(
                target = score,
                suffix = "%",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = when {
                state.total == 0 -> "Brak pytań"
                score >= 90 -> "Świetnie! Jesteś gotowy."
                score >= 70 -> "Dobrze idzie – jeszcze trochę."
                score >= 50 -> "Ponad połowa! Powtórz trudne pytania."
                else -> "Na spokojnie – powtórz materiał."
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.appear(1),
        )
        Text(
            text = "${state.correctIds.size} z ${state.total} " +
                pluralPl(state.total, "odpowiedź dobra", "odpowiedzi dobre", "odpowiedzi dobrych") +
                if (state.isReplay) " (powtórka)" else "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.appear(2),
        )
        Spacer(Modifier.height(8.dp))
        if (wrong > 0) {
            PrimaryButton(
                text = "Powtórz trudne ($wrong)",
                icon = Icons.Rounded.Replay,
                onClick = onReplayWrong,
                modifier = Modifier
                    .fillMaxWidth()
                    .appear(3),
            )
        }
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .appear(4),
        ) { Text("Cały zestaw jeszcze raz", style = MaterialTheme.typography.labelLarge) }
        TextButton(onClick = onClose) { Text("Zakończ") }
    }
}
