package pl.przypominajka.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.QuizSummary
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.AppFab
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.LocalBottomBarPadding
import pl.przypominajka.ui.components.PastelBadge
import pl.przypominajka.ui.components.PrimaryButton
import pl.przypominajka.ui.components.ProgressBar
import pl.przypominajka.ui.components.ScreenHeader
import pl.przypominajka.ui.components.SectionLabel
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.floating
import pl.przypominajka.ui.components.icon
import pl.przypominajka.ui.components.pastel
import pl.przypominajka.ui.theme.AppSurfaces
import pl.przypominajka.ui.theme.Pastel
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.pluralPl
import java.time.LocalDate

/** Kolejne quizy dostają kolejne pastelowe kolory – lista jest wtedy bardziej "żywa". */
private val QuizPastels = listOf(Pastel.GOLD, Pastel.DUSK, Pastel.SAGE, Pastel.PLUM, Pastel.TERRACOTTA, Pastel.SAND)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    onBack: () -> Unit,
    onNewQuiz: () -> Unit,
    onQuizForReminder: (reminderId: Long) -> Unit,
    onOpenQuiz: (quizId: Long) -> Unit,
    onPlay: (quizId: Long, mode: QuizMode) -> Unit,
    viewModel: QuizListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val bottomBar = LocalBottomBarPadding.current
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex < 2 } }

    Scaffold(
        containerColor = AppSurfaces.page,
        contentWindowInsets = WindowInsets(0),
        topBar = { BellTopBar(title = "Quizy", eyebrow = "Nauka na sprawdzian", onBack = onBack) },
        floatingActionButton = {
            AppFab(
                text = "Nowy quiz",
                icon = Icons.Rounded.Add,
                expanded = fabExpanded,
                onClick = onNewQuiz,
                modifier = Modifier.padding(bottom = bottomBar),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 4.dp, bottom = bottomBar + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestionsLabel") {
                    SectionLabel("Przygotuj się", modifier = Modifier.padding(horizontal = 16.dp).appear(1))
                }
                itemsIndexed(items = state.suggestions, key = { _, it -> "s_${it.reminder.id}" }) { index, suggestion ->
                    val pastel = suggestion.type.pastel
                    AppCard(
                        onClick = { onQuizForReminder(suggestion.reminder.id) },
                        color = pastel.container,
                        border = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .appear(2 + index),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(pastel.content),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(suggestion.type.icon, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${suggestion.type.label} · ${countdownText(suggestion.date, today)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = pastel.content,
                                )
                                Text(
                                    text = suggestion.reminder.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Utwórz quiz z tego materiału",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Utwórz quiz",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (state.quizzes.isNotEmpty()) {
                item(key = "quizzesLabel") {
                    SectionLabel(
                        text = "Twoje quizy",
                        trailing = state.quizzes.size.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp).appear(3),
                    )
                }
                itemsIndexed(items = state.quizzes, key = { _, it -> it.quiz.id }) { index, summary ->
                    QuizCard(
                        summary = summary,
                        pastel = QuizPastels[index % QuizPastels.size],
                        onClick = { onOpenQuiz(summary.quiz.id) },
                        onPlay = { mode -> onPlay(summary.quiz.id, mode) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem()
                            .appear(4 + index),
                    )
                }
            } else if (!state.isLoading) {
                item(key = "empty") { EmptyQuizzes() }
            }
        }
    }
}

@Composable
private fun QuizCard(
    summary: QuizSummary,
    pastel: Pastel,
    onClick: () -> Unit,
    onPlay: (QuizMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quiz = summary.quiz
    val count = summary.questionCount
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PastelBadge(icon = Icons.Rounded.Quiz, pastel = pastel)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(quiz.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = listOf(
                            "$count ${pluralPl(count, "pytanie", "pytania", "pytań")}",
                            quiz.subject,
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val best = quiz.bestScore
            if (best != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = Pastel.GOLD.content,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Najlepszy wynik: $best%" + (quiz.lastScore?.let { " · ostatni: $it%" } ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                ProgressBar(
                    progress = best / 100f,
                    color = pastel.content,
                    trackColor = pastel.container,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = "Fiszki",
                    icon = Icons.Rounded.Style,
                    onClick = { onPlay(QuizMode.CARDS) },
                    enabled = count > 0,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { onPlay(QuizMode.CHOICE) },
                    enabled = count >= QuizMode.MIN_CHOICE_QUESTIONS,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text("Test ABCD", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun EmptyQuizzes() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp)
            .appear(2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PastelBadge(icon = Icons.Rounded.Lightbulb, pastel = Pastel.GOLD, size = 88.dp, modifier = Modifier.floating())
        Text(
            text = "Ucz się sprytniej",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Wpisz pytania i odpowiedzi z tematów na kartkówkę albo sprawdzian (albo wklej całą listę), " +
                "a potem przećwicz je jako fiszki lub test ABCD.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
