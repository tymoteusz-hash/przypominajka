@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.kartka.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.QuizQuestion
import pl.przypominajka.data.QuizRepository
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.kartka.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizEditUiState
import pl.przypominajka.ui.quiz.QuizEditViewModel
import pl.przypominajka.ui.quiz.QuizMode
import kotlin.math.roundToInt

@Composable
fun KartkaQuizEditScreen(
    onBack: () -> Unit,
    onPlay: (Long, QuizMode) -> Unit,
    viewModel: QuizEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showBulk by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val back = { viewModel.saveMeta(); onBack() }
    BackHandler(onBack = back)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }
    KartkaScaffold(
        onBack = back,
        snackbar = { KartkaSnackbarHost(snackbar) },
        actions = {
            if (state.quizId != null) {
                KartkaIconButton(KartkaIcons.Trash, "Usuń quiz", { showDelete = true })
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { KartkaPageTitle(if (state.quizId == null) "Nowy quiz" else "Quiz", subtitle = "Twój zestaw do nauki") }
            if (state.isLoading) {
                item { Text("Wczytywanie quizu…", Modifier.padding(horizontal = 20.dp), style = KartkaType.BodySmall) }
            } else {
                item {
                    EditSection("Zestaw") {
                        KartkaTextField(state.title, viewModel::onTitleChange, label = "Nazwa quizu",
                            placeholder = "np. Sprawdzian: Biologia")
                        KartkaTextField(state.subject, viewModel::onSubjectChange, label = "Temat / zakres materiału",
                            placeholder = "np. Fotosynteza, budowa komórki", singleLine = false, maxLines = 3)
                    }
                }
                item { AiSection(state, viewModel::generateWithAi, viewModel::aiErrorShown) }
                item {
                    EditSection("Dodaj pytanie") {
                        Text("Pisz pytania tak, jak mogą paść na kartkówce. Odpowiedź – krótko.",
                            style = KartkaType.BodySmall, color = LookTheme.kartka.ink2)
                        KartkaTextField(state.newQuestion, viewModel::onNewQuestionChange, label = "Pytanie",
                            singleLine = false, maxLines = 4,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next))
                        KartkaTextField(state.newAnswer, viewModel::onNewAnswerChange, label = "Odpowiedź",
                            singleLine = false, maxLines = 4, isError = state.inputError != null, supportingText = state.inputError)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KartkaButton("Dodaj", viewModel::addQuestion, Modifier.weight(1f), icon = KartkaIcons.Plus)
                            KartkaOutlineButton("Wklej listę", { showBulk = true }, Modifier.weight(1f))
                        }
                    }
                }
                item { KartkaSectionHeader("Pytania · ${state.questions.size}", Modifier.padding(horizontal = 20.dp)) }
                if (state.questions.isEmpty()) {
                    item {
                        Text("Zestaw zapisze się po dodaniu pierwszego pytania.", Modifier.padding(horizontal = 20.dp),
                            style = KartkaType.BodySmall, color = LookTheme.kartka.ink2)
                    }
                }
                itemsIndexed(state.questions, key = { _, question -> question.id }) { index, question ->
                    KartkaListRow(
                        title = "${index + 1}. ${question.question}", subtitle = question.answer,
                        onClick = { editingId = question.id }, onClickLabel = "Edytuj pytanie ${index + 1}",
                        trailing = {
                            KartkaIconButton(KartkaIcons.Trash, "Usuń pytanie ${index + 1}", { viewModel.deleteQuestion(question) })
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                val quizId = state.quizId
                if (quizId != null && state.questions.isNotEmpty()) {
                    item {
                        EditSection("Przećwicz zestaw") {
                            KartkaButton("Ucz się: fiszki", { viewModel.saveMeta(); onPlay(quizId, QuizMode.CARDS) },
                                Modifier.fillMaxWidth(), icon = KartkaIcons.Quiz)
                            KartkaOutlineButton("Sprawdź się: test ABCD",
                                { viewModel.saveMeta(); onPlay(quizId, QuizMode.CHOICE) }, Modifier.fillMaxWidth(),
                                enabled = state.questions.size >= QuizMode.MIN_CHOICE_QUESTIONS)
                            if (state.questions.size < QuizMode.MIN_CHOICE_QUESTIONS) {
                                Text("Test ABCD – dodaj co najmniej ${QuizMode.MIN_CHOICE_QUESTIONS} pytania.",
                                    style = KartkaType.Caption, color = LookTheme.kartka.ink2)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showBulk) {
        BulkDialog({ showBulk = false }) { text ->
            if (viewModel.addBulk(text) > 0) showBulk = false
        }
    }
    state.questions.firstOrNull { it.id == editingId }?.let { question ->
        key(question.id) {
            QuestionDialog(question, { editingId = null }) { q, a ->
                viewModel.updateQuestion(question, q, a)
                editingId = null
            }
        }
    }
    if (showDelete) {
        KartkaDialog(
            onDismissRequest = { showDelete = false }, title = "Usunąć quiz?",
            text = "Quiz „${state.title}” i wszystkie jego pytania zostaną usunięte.",
            confirmLabel = "Usuń", destructive = true,
            onConfirm = { showDelete = false; viewModel.deleteQuiz() },
        )
    }
}

@Composable
private fun EditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KartkaSectionHeader(title)
        content()
    }
}

@Composable
private fun AiSection(state: QuizEditUiState, onGenerate: (Int, String) -> Unit, onErrorShown: () -> Unit) {
    var count by rememberSaveable { mutableFloatStateOf(10f) }
    var extra by rememberSaveable { mutableStateOf("") }
    val c = LookTheme.kartka
    EditSection("Pytania z AI") {
        Text(
            if (state.aiAvailable) "AI ułoży pytania na podstawie nazwy i tematu zestawu. Sprawdź je – AI też się myli."
            else "Dodaj klucz API Anthropic w zakładce Konto, a AI ułoży pytania za Ciebie.",
            style = KartkaType.BodySmall, color = c.ink2,
        )
        if (state.aiAvailable) {
            KartkaTextField(extra, { extra = it }, label = "Na czym się skupić? (opcjonalnie)",
                placeholder = "np. daty bitew, wzory, słówka z rozdziału 3", singleLine = false, maxLines = 3,
                enabled = !state.isGenerating)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Liczba pytań", Modifier.weight(1f), style = KartkaType.Body)
                KartkaChip(count.roundToInt().toString())
            }
            Slider(value = count, onValueChange = { count = it }, valueRange = 5f..20f, steps = 14,
                enabled = !state.isGenerating, modifier = Modifier.heightIn(min = 48.dp),
                colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.line))
            state.aiError?.let {
                KartkaNotice("Nie udało się", it, actionLabel = "OK", onAction = onErrorShown)
            }
            if (state.isGenerating) {
                KartkaNotice("AI układa pytania…", "Pytania pojawią się w zestawie po zakończeniu generowania.")
            } else {
                KartkaButton("Wygeneruj ${count.roundToInt()} pytań", { onGenerate(count.roundToInt(), extra) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BulkDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    KartkaDialog(
        onDismissRequest = onDismiss, title = "Wklej listę pytań",
        text = "Jedna linia = jedno pytanie. Oddziel pytanie od odpowiedzi myślnikiem lub znakiem =.\nNp.: Stolica Francji - Paryż",
        confirmLabel = "Dodaj", confirmEnabled = text.isNotBlank(),
        onConfirm = {
            // Błąd także w oknie: komunikat ekranu pod dialogiem jest zasłonięty.
            error = if (QuizRepository.parseBulk(text).isEmpty())
                "Nie rozpoznano pytań. Użyj formatu: pytanie - odpowiedź." else null
            onAdd(text)
        },
    ) {
        KartkaTextField(text, { text = it; error = null }, label = "Lista pytań", singleLine = false,
            placeholder = "Mitochondrium - centrum energetyczne komórki\n2³ = 8",
            minLines = 6, maxLines = 12, isError = error != null, supportingText = error)
    }
}

@Composable
private fun QuestionDialog(question: QuizQuestion, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var q by rememberSaveable { mutableStateOf(question.question) }
    var a by rememberSaveable { mutableStateOf(question.answer) }
    KartkaDialog(onDismissRequest = onDismiss, title = "Edytuj pytanie", confirmLabel = "Zapisz",
        confirmEnabled = q.isNotBlank() && a.isNotBlank(), onConfirm = { onSave(q, a) }) {
        KartkaTextField(q, { q = it }, label = "Pytanie", singleLine = false, maxLines = 6)
        KartkaTextField(a, { a = it }, label = "Odpowiedź", singleLine = false, maxLines = 6)
    }
}
