@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizListViewModel
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.util.countdownText
import pl.przypominajka.util.pluralPl
import java.time.LocalDate

/** Zestawy do nauki i propozycje powiązane ze sprawdzianami jako przypięte karteczki. */
@Composable
fun TablicaQuizListScreen(
    onBack: () -> Unit,
    onNewQuiz: () -> Unit,
    onQuizForReminder: (reminderId: Long) -> Unit,
    onOpenQuiz: (quizId: Long) -> Unit,
    onPlay: (quizId: Long, mode: QuizMode) -> Unit,
    viewModel: QuizListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LookTheme.tablica
    TablicaScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = dockContentPadding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(key = "header") {
                BoardHeader("Quizy", subtitle = "Powtórki przed sprawdzianem", trailing = {
                    RoundIconButton(TablicaIcons.Back, "Wróć", onBack)
                })
            }
            item(key = "new") {
                NewNoteTile(onClick = onNewQuiz, text = "Nowy quiz", modifier = Modifier.fillMaxWidth())
            }
            if (state.isLoading) item(key = "loading") {
                Text("Wczytywanie quizów…", style = TablicaType.Body)
            }
            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestions") { SectionLabel("Przygotuj się") }
                itemsIndexed(state.suggestions, key = { _, s -> "suggestion_${s.reminder.id}" }) { index, s ->
                    Note(
                        color = colors.pink,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        tilt = noteTilt(index),
                        decoration = NoteDecoration.Pin,
                        onClick = { onQuizForReminder(s.reminder.id) },
                        onClickLabel = "Utwórz quiz: ${s.reminder.title}",
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("${s.type.label} · ${countdownText(s.date, LocalDate.now())}", style = TablicaType.Tag)
                        Text(s.reminder.title, style = TablicaType.NoteTitle)
                        Text("Utwórz quiz z tego materiału", style = TablicaType.Link)
                    }
                }
            }
            if (state.quizzes.isNotEmpty()) {
                item(key = "quizzes") { SectionLabel("Twoje quizy", trailing = state.quizzes.size.toString()) }
                itemsIndexed(state.quizzes, key = { _, s -> "quiz_${s.quiz.id}" }) { index, summary ->
                    val quiz = summary.quiz
                    val count = summary.questionCount
                    val paper = colors.noteFor(quiz.id)
                    Note(
                        color = paper,
                        modifier = Modifier.fillMaxWidth(),
                        tilt = noteTilt(index),
                        decoration = NoteDecoration.Tape,
                        onClick = { onOpenQuiz(quiz.id) },
                        onClickLabel = "Edytuj quiz: ${quiz.title}",
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(quiz.title, style = TablicaType.NoteTitle)
                        Text(listOf("$count ${pluralPl(count, "pytanie", "pytania", "pytań")}", quiz.subject)
                            .filter { it.isNotBlank() }.joinToString(" · "), style = TablicaType.BodyLight)
                        quiz.bestScore?.let { best ->
                            NoteDivider(color = paper.rule)
                            Text("Najlepszy wynik: $best%" + (quiz.lastScore?.let { " · ostatni: $it%" } ?: ""),
                                style = TablicaType.Hint)
                            LinearProgressIndicator(progress = { best / 100f }, color = paper.ink,
                                trackColor = paper.rule, modifier = Modifier.fillMaxWidth())
                        }
                        InkButton("Fiszki", { onPlay(quiz.id, QuizMode.CARDS) },
                            modifier = Modifier.fillMaxWidth(), enabled = count > 0)
                        OutlineInkButton("Test ABCD", { onPlay(quiz.id, QuizMode.CHOICE) },
                            modifier = Modifier.fillMaxWidth(), enabled = count >= QuizMode.MIN_CHOICE_QUESTIONS)
                        InkTextButton("Edytuj zestaw", { onOpenQuiz(quiz.id) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else if (!state.isLoading) {
                item(key = "empty") {
                    BoardEmptyState("Ucz się sprytniej",
                        "Wpisz pytania i odpowiedzi z tematów na kartkówkę albo sprawdzian lub wklej całą listę. " +
                            "Potem przećwicz je jako fiszki lub test ABCD.")
                }
            }
        }
    }
}
