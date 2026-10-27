@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon.quiz

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
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizListViewModel
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.pluralPl
import java.time.LocalDate

@Composable
fun DzwonQuizListScreen(
    onBack: () -> Unit,
    onNewQuiz: () -> Unit,
    onQuizForReminder: (reminderId: Long) -> Unit,
    onOpenQuiz: (quizId: Long) -> Unit,
    onPlay: (quizId: Long, mode: QuizMode) -> Unit,
    viewModel: QuizListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    val today = LocalDate.now()
    DzwonSheetScaffold(
        title = "Quizy",
        subtitle = "Małe powtórki, coraz więcej w głowie",
        onClose = onBack,
        bottomBar = {
            DzwonPrimaryButton(
                text = "Nowy quiz", icon = DzwonIcons.Plus, onClick = onNewQuiz,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLoading) {
                item { Text("Wczytywanie quizów…", style = DzwonType.Body, color = c.ink2) }
            }
            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestions") { DzwonSectionHeader("Przygotuj się") }
                items(state.suggestions, key = { "reminder_${it.reminder.id}" }) { suggestion ->
                    DzwonCard(
                        modifier = Modifier.fillMaxWidth(), color = c.field,
                        onClick = { onQuizForReminder(suggestion.reminder.id) },
                    ) {
                        DzwonListRow(
                            title = "${suggestion.type.label}: ${suggestion.reminder.title}",
                            subtitle = countdownText(suggestion.date, today),
                            trailing = {
                                DzwonRoundIconButton(
                                    icon = DzwonIcons.Plus, contentDescription = "Utwórz quiz: ${suggestion.reminder.title}",
                                    onClick = { onQuizForReminder(suggestion.reminder.id) }, containerColor = c.gold,
                                    contentColor = c.onGold,
                                )
                            },
                        )
                    }
                }
            }
            if (state.quizzes.isNotEmpty()) {
                item(key = "quizzes") { DzwonSectionHeader("Twoje zestawy (${state.quizzes.size})") }
                items(state.quizzes, key = { "quiz_${it.quiz.id}" }) { summary ->
                    QuizSetCard(summary, { onOpenQuiz(summary.quiz.id) }, { onPlay(summary.quiz.id, it) })
                }
            } else if (!state.isLoading) {
                item(key = "empty") {
                    DzwonEmptyState(
                        title = "Ucz się sprytniej",
                        text = "Wpisz pytania i odpowiedzi z materiału na sprawdzian albo wklej całą listę. " +
                            "Potem przećwicz je jako fiszki lub test ABCD.",
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizSetCard(summary: QuizSummary, onEdit: () -> Unit, onPlay: (QuizMode) -> Unit) {
    val c = LookTheme.dzwon
    val quiz = summary.quiz
    val count = summary.questionCount
    DzwonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        DzwonListRow(
            title = quiz.title,
            subtitle = listOf("$count ${pluralPl(count, "pytanie", "pytania", "pytań")}", quiz.subject)
                .filter { it.isNotBlank() }.joinToString(" · "),
            onClick = onEdit,
            trailing = {
                DzwonRoundIconButton(DzwonIcons.Pencil, "Edytuj quiz: ${quiz.title}", onEdit, containerColor = c.field)
            },
        )
        quiz.bestScore?.let { best ->
            Text(
                "Najlepszy wynik: $best%" + (quiz.lastScore?.let { " · ostatni: $it%" } ?: ""),
                style = DzwonType.CaptionStrong, color = c.goldText,
            )
            LinearProgressIndicator(
                progress = { best / 100f }, color = c.gold, trackColor = c.track,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            DzwonPrimaryButton("Fiszki", { onPlay(QuizMode.CARDS) }, Modifier.weight(1f), enabled = count > 0)
            DzwonSecondaryButton(
                "Test ABCD", { onPlay(QuizMode.CHOICE) }, Modifier.weight(1f),
                enabled = count >= QuizMode.MIN_CHOICE_QUESTIONS,
            )
        }
    }
}
