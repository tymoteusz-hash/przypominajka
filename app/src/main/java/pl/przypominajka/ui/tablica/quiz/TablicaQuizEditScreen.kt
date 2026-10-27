@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.tablica.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
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
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizEditUiState
import pl.przypominajka.ui.quiz.QuizEditViewModel
import pl.przypominajka.ui.quiz.QuizMode
import pl.przypominajka.ui.tablica.components.*
import pl.przypominajka.ui.tablica.components.RoundIconButton
import kotlin.math.roundToInt

/** Edytor zestawu na tablicy; zapis i generowanie pozostają we wspólnym ViewModelu. */
@Composable
fun TablicaQuizEditScreen(
    onBack: () -> Unit,
    onPlay: (quizId: Long, mode: QuizMode) -> Unit,
    viewModel: QuizEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LookTheme.tablica
    val snackbar = remember { SnackbarHostState() }
    var showBulk by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val goBack = { viewModel.saveMeta(); onBack() }
    BackHandler(onBack = goBack)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    TablicaScreen(Modifier.imePadding()) {
        Column(Modifier.fillMaxSize()) {
            TablicaTopBar(
                title = if (state.quizId == null) "Nowy quiz" else "Quiz",
                subtitle = "Zestaw do nauki",
                onBack = goBack,
                actions = {
                    if (state.quizId != null) RoundIconButton(Icons.Rounded.Delete, "Usuń quiz", { showDelete = true })
                },
            )
            if (state.isLoading) {
                Text("Wczytywanie zestawu…", style = TablicaType.Body, modifier = Modifier.padding(24.dp))
            } else LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp,
                    bottom = LocalDockPadding.current + 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item(key = "meta") {
                    Note(colors.yellow, Modifier.fillMaxWidth(), decoration = NoteDecoration.Tape,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Zestaw", style = TablicaType.NoteTitle)
                        QuizField(state.title, viewModel::onTitleChange, "Nazwa quizu", "np. Sprawdzian: Biologia", singleLine = true)
                        QuizField(state.subject, viewModel::onSubjectChange, "Temat / zakres materiału", "np. Fotosynteza, budowa komórki", maxLines = 3)
                    }
                }
                item(key = "ai") {
                    AiNote(state, viewModel::generateWithAi, viewModel::aiErrorShown)
                }
                item(key = "add") {
                    Note(colors.blue, Modifier.fillMaxWidth(), decoration = NoteDecoration.Tape,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Dodaj pytanie", style = TablicaType.NoteTitle)
                        Text("Pisz pytania tak, jak mogą paść na kartkówce. Odpowiedź – krótko.", style = TablicaType.BodyLight)
                        QuizField(state.newQuestion, viewModel::onNewQuestionChange, "Pytanie", maxLines = 4,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next))
                        QuizField(state.newAnswer, viewModel::onNewAnswerChange, "Odpowiedź", maxLines = 4)
                        state.inputError?.let { Text(it, style = TablicaType.Body) }
                        InkButton("Dodaj", viewModel::addQuestion, Modifier.fillMaxWidth(), icon = TablicaIcons.Plus)
                        OutlineInkButton("Wklej listę", { showBulk = true }, Modifier.fillMaxWidth())
                    }
                }
                if (state.questions.isNotEmpty()) {
                    item(key = "questions") { SectionLabel("Pytania", trailing = state.questions.size.toString()) }
                    itemsIndexed(state.questions, key = { _, q -> q.id }) { index, question ->
                        Note(colors.noteFor(question.id), Modifier.fillMaxWidth(), tilt = noteTilt(index),
                            onClick = { editingId = question.id }, onClickLabel = "Edytuj pytanie ${index + 1}",
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pytanie ${index + 1}", style = TablicaType.Tag, modifier = Modifier.weight(1f))
                                RoundIconButton(Icons.Rounded.Delete, "Usuń pytanie ${index + 1}",
                                    { viewModel.deleteQuestion(question) }, style = RoundIconStyle.Plain)
                            }
                            Text(question.question, style = TablicaType.NoteText)
                            NoteDivider()
                            Text(question.answer, style = TablicaType.BodyLight)
                            InkTextButton("Edytuj", { editingId = question.id })
                        }
                    }
                }
                val id = state.quizId
                if (id != null && state.questions.isNotEmpty()) item(key = "play") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InkButton("Ucz się: fiszki", { viewModel.saveMeta(); onPlay(id, QuizMode.CARDS) }, Modifier.fillMaxWidth())
                        OutlineInkButton("Sprawdź się: test ABCD", { viewModel.saveMeta(); onPlay(id, QuizMode.CHOICE) },
                            Modifier.fillMaxWidth(), enabled = state.questions.size >= QuizMode.MIN_CHOICE_QUESTIONS)
                        if (state.questions.size < QuizMode.MIN_CHOICE_QUESTIONS) {
                            Text("Test ABCD – dodaj co najmniej ${QuizMode.MIN_CHOICE_QUESTIONS} pytania.", style = TablicaType.Hint)
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter)
            .padding(start = 16.dp, end = 16.dp, bottom = LocalDockPadding.current + 8.dp)) { data ->
            Snackbar(data, containerColor = colors.dock, contentColor = colors.dockText)
        }
    }

    if (showBulk) BulkDialog(onDismiss = { showBulk = false }, onAdd = viewModel::addBulk)
    state.questions.firstOrNull { it.id == editingId }?.let { question ->
        EditQuestionDialog(question, { editingId = null }) { q, a ->
            viewModel.updateQuestion(question, q, a)
            editingId = null
        }
    }
    if (showDelete) ConfirmNoteDialog(
        title = "Usunąć quiz?",
        text = "Quiz „${state.title}” i wszystkie jego pytania zostaną usunięte.",
        confirmText = "Usuń",
        onConfirm = { showDelete = false; viewModel.deleteQuiz() },
        onDismiss = { showDelete = false },
    )
}

@Composable
private fun QuizField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = TablicaType.Tag)
        NoteTextField(value, onValueChange, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            label = label, placeholder = placeholder, handwritten = false, singleLine = singleLine,
            maxLines = maxLines, enabled = enabled, keyboardOptions = keyboardOptions)
    }
}

@Composable
private fun AiNote(state: QuizEditUiState, onGenerate: (Int, String) -> Unit, onErrorShown: () -> Unit) {
    var count by rememberSaveable { mutableFloatStateOf(10f) }
    var extra by rememberSaveable { mutableStateOf("") }
    val paper = LookTheme.tablica.lilac
    Note(paper, Modifier.fillMaxWidth(), decoration = NoteDecoration.Pin,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Pytania z AI", style = TablicaType.NoteTitle)
        Text(if (state.aiAvailable) "AI ułoży pytania na podstawie nazwy i tematu zestawu. Sprawdź je – AI też się myli."
            else "Dodaj klucz API Anthropic w zakładce Konto, a AI ułoży pytania za Ciebie.", style = TablicaType.BodyLight)
        if (state.aiAvailable) {
            QuizField(extra, { extra = it }, "Na czym się skupić? (opcjonalnie)",
                "np. daty bitew, wzory, słówka z rozdziału 3", maxLines = 3, enabled = !state.isGenerating)
            Text("Liczba pytań: ${count.roundToInt()}", style = TablicaType.Strong)
            Slider(value = count, onValueChange = { count = it }, valueRange = 5f..20f, steps = 14,
                enabled = !state.isGenerating, modifier = Modifier.heightIn(min = 48.dp),
                colors = SliderDefaults.colors(thumbColor = paper.ink, activeTrackColor = paper.ink,
                    inactiveTrackColor = paper.rule, activeTickColor = paper.bg, inactiveTickColor = paper.ink))
            state.aiError?.let {
                NoteDivider()
                Text("Nie udało się", style = TablicaType.Strong)
                Text(it, style = TablicaType.BodyLight)
                InkTextButton("OK", onErrorShown)
            }
            if (state.isGenerating) Text("AI układa pytania…", style = TablicaType.Strong)
            else InkButton("Wygeneruj ${count.roundToInt()} pytań", { onGenerate(count.roundToInt(), extra) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BulkDialog(onDismiss: () -> Unit, onAdd: (String) -> Int) {
    var text by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    NoteDialog(onDismissRequest = onDismiss, title = "Wklej listę pytań", tilt = 0f,
        modifier = Modifier.imePadding(), buttons = {
            NoteDialogButtons("Dodaj", {
                if (onAdd(text) > 0) onDismiss() else invalid = true
            }, onDismiss, confirmEnabled = text.isNotBlank())
        }) {
        Text("Jedna linia = jedno pytanie. Oddziel pytanie od odpowiedzi myślnikiem lub znakiem =.\nNp.: Stolica Francji - Paryż",
            style = TablicaType.BodyLight)
        QuizField(text, { text = it; invalid = false }, "Lista pytań",
            "Mitochondrium - centrum energetyczne komórki\n2³ = 8", maxLines = 12)
        if (invalid) Text("Nie rozpoznano pytań. Użyj formatu: pytanie - odpowiedź.", style = TablicaType.Body)
    }
}

@Composable
private fun EditQuestionDialog(question: QuizQuestion, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var q by rememberSaveable(question.id) { mutableStateOf(question.question) }
    var a by rememberSaveable(question.id) { mutableStateOf(question.answer) }
    NoteDialog(onDismissRequest = onDismiss, title = "Edytuj pytanie", tilt = 0f,
        modifier = Modifier.imePadding(), buttons = {
            NoteDialogButtons("Zapisz", { onSave(q, a) }, onDismiss, confirmEnabled = q.isNotBlank() && a.isNotBlank())
        }) {
        QuizField(q, { q = it }, "Pytanie")
        Spacer(Modifier.height(10.dp))
        QuizField(a, { a = it }, "Odpowiedź")
    }
}
