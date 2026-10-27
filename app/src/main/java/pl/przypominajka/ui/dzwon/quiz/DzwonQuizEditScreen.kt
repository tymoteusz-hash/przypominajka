@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package pl.przypominajka.ui.dzwon.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.dzwon.components.*
import pl.przypominajka.ui.look.LookTheme
import pl.przypominajka.ui.quiz.QuizEditUiState
import pl.przypominajka.ui.quiz.QuizEditViewModel
import pl.przypominajka.ui.quiz.QuizMode
import kotlin.math.roundToInt

@Composable
fun DzwonQuizEditScreen(
    onBack: () -> Unit,
    onPlay: (quizId: Long, mode: QuizMode) -> Unit,
    viewModel: QuizEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LookTheme.dzwon
    var showBulk by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val goBack: () -> Unit = { viewModel.saveMeta(); onBack() }
    BackHandler(onBack = goBack)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    DzwonSheetScaffold(
        title = if (state.quizId == null) "Nowy quiz" else "Twój zestaw",
        subtitle = "Pytanie po pytaniu do celu",
        onClose = goBack,
        actions = {
            if (state.quizId != null) {
                DzwonRoundIconButton(DzwonIcons.Trash, "Usuń quiz", { showDelete = true })
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.isLoading) {
                item { Text("Wczytywanie zestawu…", style = DzwonType.Body, color = c.ink2) }
            } else {
                item(key = "meta") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DzwonSectionHeader("Zestaw")
                        DzwonTextField(
                            value = state.title, onValueChange = viewModel::onTitleChange,
                            label = "Nazwa quizu", placeholder = "np. Sprawdzian: Biologia",
                        )
                        DzwonTextField(
                            value = state.subject, onValueChange = viewModel::onSubjectChange,
                            label = "Temat / zakres materiału", placeholder = "np. Fotosynteza, budowa komórki",
                            singleLine = false, maxLines = 3,
                        )
                    }
                }
                item(key = "ai") { AiQuestions(state, viewModel::generateWithAi, viewModel::aiErrorShown) }
                item(key = "add") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DzwonSectionHeader("Dodaj pytanie")
                        Text(
                            "Pisz pytania tak, jak mogą paść na kartkówce. Odpowiedź – krótko.",
                            style = DzwonType.Caption, color = c.ink2,
                        )
                        DzwonTextField(
                            value = state.newQuestion, onValueChange = viewModel::onNewQuestionChange,
                            label = "Pytanie", singleLine = false, maxLines = 4,
                        )
                        DzwonTextField(
                            value = state.newAnswer, onValueChange = viewModel::onNewAnswerChange,
                            label = "Odpowiedź", singleLine = false, maxLines = 4,
                        )
                        state.inputError?.let {
                            Text(it, style = DzwonType.Caption, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                        DzwonPrimaryButton("Dodaj pytanie", viewModel::addQuestion, Modifier.fillMaxWidth(), icon = DzwonIcons.Plus)
                        DzwonSecondaryButton("Wklej listę", { showBulk = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.ContentPaste)
                    }
                }
                if (state.questions.isEmpty()) {
                    item(key = "empty") {
                        DzwonEmptyState("Pierwsze pytanie przed Tobą", "Dodaj je ręcznie, wklej listę albo skorzystaj z AI.", showBell = false)
                    }
                } else {
                    item(key = "questions") { DzwonSectionHeader("Pytania (${state.questions.size})") }
                    itemsIndexed(state.questions, key = { _, q -> q.id }) { index, question ->
                        DzwonCard(modifier = Modifier.fillMaxWidth()) {
                            DzwonListRow(
                                title = question.question, subtitle = question.answer, lead = "${index + 1}",
                                onClick = { editingId = question.id },
                                trailing = {
                                    DzwonRoundIconButton(
                                        DzwonIcons.Trash, "Usuń pytanie ${index + 1}",
                                        { viewModel.deleteQuestion(question) }, containerColor = c.field,
                                    )
                                },
                            )
                        }
                    }
                }
                val id = state.quizId
                if (id != null && state.questions.isNotEmpty()) {
                    item(key = "learn") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DzwonSectionHeader("Czas na powtórkę")
                            DzwonPrimaryButton(
                                "Ucz się: fiszki", { viewModel.saveMeta(); onPlay(id, QuizMode.CARDS) }, Modifier.fillMaxWidth(),
                            )
                            DzwonSecondaryButton(
                                text = if (state.questions.size >= QuizMode.MIN_CHOICE_QUESTIONS) "Sprawdź się: test ABCD"
                                    else "Test ABCD – dodaj co najmniej ${QuizMode.MIN_CHOICE_QUESTIONS} pytania",
                                onClick = { viewModel.saveMeta(); onPlay(id, QuizMode.CHOICE) },
                                modifier = Modifier.fillMaxWidth(), enabled = state.questions.size >= QuizMode.MIN_CHOICE_QUESTIONS,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBulk) {
        BulkQuestionsSheet(
            onDismiss = { showBulk = false },
            onAdd = { if (viewModel.addBulk(it) > 0) showBulk = false },
        )
    }
    val editing = state.questions.firstOrNull { it.id == editingId }
    if (editing != null) {
        var question by rememberSaveable(editing.id) { mutableStateOf(editing.question) }
        var answer by rememberSaveable(editing.id) { mutableStateOf(editing.answer) }
        val sheetState = rememberDzwonSheetState()
        val scope = rememberCoroutineScope()
        DzwonBottomSheet(onDismissRequest = { editingId = null }, title = "Edytuj pytanie", sheetState = sheetState) {
            DzwonTextField(question, { question = it }, label = "Pytanie", singleLine = false, maxLines = 6)
            DzwonTextField(answer, { answer = it }, label = "Odpowiedź", singleLine = false, maxLines = 6)
            DzwonPrimaryButton(
                "Zapisz", {
                    sheetState.hideThen(scope) {
                        viewModel.updateQuestion(editing, question, answer)
                        editingId = null
                    }
                }, Modifier.fillMaxWidth(), enabled = question.isNotBlank() && answer.isNotBlank(),
            )
            DzwonTextButton("Anuluj", { sheetState.hideThen(scope) { editingId = null } }, Modifier.fillMaxWidth())
        }
    }
    if (showDelete) {
        DzwonDialog(
            onDismissRequest = { showDelete = false }, title = "Usunąć quiz?",
            text = "Quiz „${state.title}” i wszystkie jego pytania zostaną usunięte.",
            confirmText = "Usuń", destructive = true,
            onConfirm = { showDelete = false; viewModel.deleteQuiz() },
        )
    }
    // Okno pozostaje czytelne również nad arkuszem importu, gdy nie rozpoznano listy.
    state.message?.let { message ->
        DzwonDialog(
            onDismissRequest = viewModel::messageShown, title = "Quiz", text = message,
            confirmText = "OK", onConfirm = viewModel::messageShown, dismissText = null,
        )
    }
}

@Composable
private fun AiQuestions(state: QuizEditUiState, onGenerate: (Int, String) -> Unit, onErrorShown: () -> Unit) {
    val c = LookTheme.dzwon
    var count by rememberSaveable { mutableFloatStateOf(10f) }
    var extra by rememberSaveable { mutableStateOf("") }
    DzwonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DzwonSectionHeader("Pytania z AI")
            Text(
                if (state.aiAvailable) "AI ułoży pytania na podstawie nazwy i tematu zestawu. Sprawdź je – AI też się myli."
                else "Dodaj klucz API Anthropic w zakładce Konto, a AI ułoży pytania za Ciebie.",
                style = DzwonType.Caption, color = c.ink2,
            )
            if (state.aiAvailable) {
                DzwonTextField(
                    value = extra, onValueChange = { extra = it }, label = "Na czym się skupić? (opcjonalnie)",
                    placeholder = "np. daty bitew, wzory, słówka z rozdziału 3", singleLine = false,
                    maxLines = 3, enabled = !state.isGenerating,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Liczba pytań", style = DzwonType.Body, modifier = Modifier.weight(1f))
                    Text("${count.roundToInt()}", style = DzwonType.TimeLarge, color = c.goldText)
                }
                Slider(
                    value = count, onValueChange = { count = it }, valueRange = 5f..20f, steps = 14,
                    enabled = !state.isGenerating, modifier = Modifier.heightIn(min = 48.dp),
                    colors = SliderDefaults.colors(thumbColor = c.gold, activeTrackColor = c.gold, inactiveTrackColor = c.track),
                )
                state.aiError?.let {
                    DzwonNotice("Nie udało się", it, containerColor = c.field, actions = {
                        DzwonTextButton("OK", onErrorShown)
                    })
                }
                if (state.isGenerating) {
                    Text("AI układa pytania…", style = DzwonType.Body, color = c.goldText,
                        modifier = Modifier.padding(vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
                } else {
                    DzwonPrimaryButton(
                        "Wygeneruj ${count.roundToInt()} pytań", { onGenerate(count.roundToInt(), extra) },
                        Modifier.fillMaxWidth(), icon = Icons.Rounded.AutoAwesome,
                    )
                }
            }
        }
    }
}

@Composable
private fun BulkQuestionsSheet(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberDzwonSheetState()
    val scope = rememberCoroutineScope()
    DzwonBottomSheet(onDismissRequest = onDismiss, title = "Wklej listę pytań", sheetState = sheetState) {
        Text(
            "Jedna linia = jedno pytanie. Oddziel pytanie od odpowiedzi myślnikiem lub znakiem =.\n" +
                "Np.: Stolica Francji - Paryż",
            style = DzwonType.Caption, color = LookTheme.dzwon.ink2,
        )
        DzwonTextField(
            text, { text = it }, label = "Lista pytań i odpowiedzi", singleLine = false, maxLines = 12,
            placeholder = "Mitochondrium - centrum energetyczne komórki",
            modifier = Modifier.heightIn(min = 160.dp),
        )
        DzwonPrimaryButton("Dodaj", { onAdd(text) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
        DzwonTextButton("Anuluj", { sheetState.hideThen(scope, onDismiss) }, Modifier.fillMaxWidth())
    }
}
