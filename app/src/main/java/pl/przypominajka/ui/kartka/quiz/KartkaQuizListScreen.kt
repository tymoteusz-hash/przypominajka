@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.QuizSummary
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizListViewModel
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.util.pluralPl
import java.time.LocalDate

@Composable
fun KartkaQuizListScreen(
    onBack: () -> Unit,
    onNewQuiz: () -> Unit,
    onQuizForReminder: (Long) -> Unit,
    onOpenQuiz: (Long) -> Unit,
    onPlay: (Long, QuizMode) -> Unit,
    viewModel: QuizListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    KartkaScaffold(
        onBack = onBack,
        bottomBar = {
            KartkaBottomBar {
                KartkaButton("Nowy quiz", onNewQuiz, Modifier.fillMaxWidth(), icon = KartkaIcons.Plus)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { KartkaPageTitle("Quizy", subtitle = "Nauka na sprawdzian") }
            if (state.isLoading) {
                item { Text("Wczytywanie quizów…", Modifier.padding(horizontal = 20.dp), style = KartkaType.BodySmall) }
            }
            if (state.suggestions.isNotEmpty()) {
                item { KartkaSectionHeader("Przygotuj się", Modifier.padding(horizontal = 20.dp)) }
                items(state.suggestions, key = { "reminder_${it.reminder.id}" }) { suggestion ->
                    KartkaListRow(
                        title = suggestion.reminder.title,
                        subtitle = "${suggestion.type.label} · ${kartkaCountdown(suggestion.date, today)}\nUtwórz quiz z tego materiału",
                        onClick = { onQuizForReminder(suggestion.reminder.id) },
                        onClickLabel = "Utwórz quiz",
                        trailing = {
                            KartkaIconButton(KartkaIcons.Plus, "Utwórz quiz: ${suggestion.reminder.title}",
                                { onQuizForReminder(suggestion.reminder.id) })
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            if (state.quizzes.isNotEmpty()) {
                item { KartkaSectionHeader("Twoje quizy · ${state.quizzes.size}", Modifier.padding(horizontal = 20.dp)) }
                items(state.quizzes, key = { "quiz_${it.quiz.id}" }) { summary ->
                    QuizRow(summary, { onOpenQuiz(summary.quiz.id) }, { onPlay(summary.quiz.id, it) })
                }
            } else if (!state.isLoading) {
                item {
                    KartkaEmptyState(
                        "Ucz się sprytniej",
                        "Wpisz pytania i odpowiedzi z tematów na kartkówkę albo sprawdzian (albo wklej całą listę), " +
                            "a potem przećwicz je jako fiszki lub test ABCD.",
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizRow(summary: QuizSummary, onOpen: () -> Unit, onPlay: (QuizMode) -> Unit) {
    val c = LookTheme.kartka
    val quiz = summary.quiz
    val count = summary.questionCount
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KartkaListRow(
            title = quiz.title,
            subtitle = listOf("$count ${pluralPl(count, "pytanie", "pytania", "pytań")}", quiz.subject)
                .filter { it.isNotBlank() }.joinToString(" · "),
            onClick = onOpen,
            onClickLabel = "Edytuj quiz",
            showDivider = false,
            trailing = { KartkaIconButton(KartkaIcons.Edit, "Edytuj quiz: ${quiz.title}", onOpen) },
        )
        quiz.bestScore?.let { best ->
            Text("Najlepszy wynik: $best%" + (quiz.lastScore?.let { " · ostatni: $it%" } ?: ""),
                style = KartkaType.CaptionStrong, color = c.ink2)
            LinearProgressIndicator(progress = { best / 100f }, color = c.accent, trackColor = c.surface,
                modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KartkaButton("Fiszki", { onPlay(QuizMode.CARDS) }, Modifier.weight(1f), enabled = count > 0)
            KartkaOutlineButton("Test ABCD", { onPlay(QuizMode.CHOICE) }, Modifier.weight(1f),
                enabled = count >= QuizMode.MIN_CHOICE_QUESTIONS)
        }
        KartkaDivider()
    }
}
