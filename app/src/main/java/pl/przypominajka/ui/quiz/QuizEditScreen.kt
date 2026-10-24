package pl.przypominajka.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import pl.przypominajka.ui.components.NoticeCard
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.przypominajka.data.QuizQuestion
import pl.przypominajka.ui.AppViewModelProvider
import pl.przypominajka.ui.components.FormSection
import pl.przypominajka.ui.components.PrimaryButton
import pl.przypominajka.ui.components.RoundIconButton
import pl.przypominajka.ui.components.BellTopBar
import pl.przypominajka.ui.components.AppCard
import pl.przypominajka.ui.components.appear
import pl.przypominajka.ui.components.SectionLabel
import pl.przypominajka.ui.theme.AppSurfaces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizEditScreen(
    onBack: () -> Unit,
    onPlay: (quizId: Long, mode: QuizMode) -> Unit,
    viewModel: QuizEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBulk by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<QuizQuestion?>(null) }

    val goBack = {
        viewModel.saveMeta()
        onBack()
    }
    BackHandler(onBack = goBack)

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = AppSurfaces.page,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BellTopBar(
                title = if (state.quizId == null) "Nowy quiz" else "Quiz",
                eyebrow = "Quizy",
                onBack = goBack,
            ) {
                if (state.quizId != null) {
                    RoundIconButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Usuń quiz",
                        onClick = { showDelete = true },
                    )
                }
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormSection(title = "Zestaw") {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Nazwa quizu") },
                    placeholder = { Text("np. Sprawdzian: Biologia") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = viewModel::onSubjectChange,
                    label = { Text("Temat / zakres materiału") },
                    placeholder = { Text("np. Fotosynteza, budowa komórki") },
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AiSection(
                state = state,
                onGenerate = viewModel::generateWithAi,
                onErrorShown = viewModel::aiErrorShown,
            )

            FormSection(
                title = "Dodaj pytanie",
                description = "Pisz pytania tak, jak mogą paść na kartkówce. Odpowiedź – krótko.",
            ) {
                OutlinedTextField(
                    value = state.newQuestion,
                    onValueChange = viewModel::onNewQuestionChange,
                    label = { Text("Pytanie") },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.newAnswer,
                    onValueChange = viewModel::onNewAnswerChange,
                    label = { Text("Odpowiedź") },
                    minLines = 1,
                    maxLines = 4,
                    isError = state.inputError != null,
                    supportingText = if (state.inputError != null) {
                        { Text(state.inputError.orEmpty()) }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::addQuestion, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Dodaj")
                    }
                    OutlinedButton(onClick = { showBulk = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Wklej listę")
                    }
                }
            }

            if (state.questions.isNotEmpty()) {
                SectionLabel(text = "Pytania", trailing = state.questions.size.toString())
                state.questions.forEachIndexed { index, question ->
                    QuestionCard(
                        number = index + 1,
                        question = question,
                        onClick = { editing = question },
                        onDelete = { viewModel.deleteQuestion(question) },
                    )
                }
            }

            val quizId = state.quizId
            if (quizId != null && state.questions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    text = "Ucz się: fiszki",
                    icon = Icons.Rounded.Style,
                    onClick = {
                        viewModel.saveMeta()
                        onPlay(quizId, QuizMode.CARDS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        viewModel.saveMeta()
                        onPlay(quizId, QuizMode.CHOICE)
                    },
                    enabled = state.questions.size >= QuizMode.MIN_CHOICE_QUESTIONS,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        if (state.questions.size >= QuizMode.MIN_CHOICE_QUESTIONS) {
                            "Sprawdź się: test ABCD"
                        } else {
                            "Test ABCD – dodaj co najmniej ${QuizMode.MIN_CHOICE_QUESTIONS} pytania"
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showBulk) {
        BulkAddDialog(
            onDismiss = { showBulk = false },
            onAdd = { text ->
                if (viewModel.addBulk(text) > 0) showBulk = false
            },
        )
    }

    editing?.let { question ->
        EditQuestionDialog(
            question = question,
            onDismiss = { editing = null },
            onSave = { newQuestion, newAnswer ->
                viewModel.updateQuestion(question, newQuestion, newAnswer)
                editing = null
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Usunąć quiz?") },
            text = { Text("Quiz „${state.title}” i wszystkie jego pytania zostaną usunięte.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteQuiz()
                }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Anuluj") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionCard(
    number: Int,
    question: QuizQuestion,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .appear(number.coerceAtMost(8)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(question.question, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = question.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Usuń pytanie $number",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Generowanie pytań przez AI z nazwy i tematu zestawu. */
@Composable
private fun AiSection(
    state: QuizEditUiState,
    onGenerate: (count: Int, extraTopic: String) -> Unit,
    onErrorShown: () -> Unit,
) {
    var count by rememberSaveable { mutableFloatStateOf(10f) }
    var extra by rememberSaveable { mutableStateOf("") }

    FormSection(
        title = "Pytania z AI",
        description = if (state.aiAvailable) {
            "AI ułoży pytania na podstawie nazwy i tematu zestawu. Sprawdź je – AI też się myli."
        } else {
            "Dodaj klucz API Anthropic w zakładce Konto, a AI ułoży pytania za Ciebie."
        },
    ) {
        if (state.aiAvailable) {
            OutlinedTextField(
                value = extra,
                onValueChange = { extra = it },
                label = { Text("Na czym się skupić? (opcjonalnie)") },
                placeholder = { Text("np. daty bitew, wzory, słówka z rozdziału 3") },
                minLines = 1,
                maxLines = 3,
                enabled = !state.isGenerating,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Liczba pytań", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    text = count.roundToInt().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = count,
                onValueChange = { count = it },
                valueRange = 5f..20f,
                steps = 14,
                enabled = !state.isGenerating,
                colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant),
            )
            state.aiError?.let { error ->
                NoticeCard(
                    title = "Nie udało się",
                    body = error,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actions = { TextButton(onClick = onErrorShown) { Text("OK") } },
                )
            }
            if (state.isGenerating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("AI układa pytania…", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                PrimaryButton(
                    text = "Wygeneruj ${count.roundToInt()} pytań",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = { onGenerate(count.roundToInt(), extra) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BulkAddDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wklej listę pytań") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Jedna linia = jedno pytanie. Oddziel pytanie od odpowiedzi myślnikiem lub znakiem =.\n" +
                        "Np.: Stolica Francji - Paryż",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 6,
                    maxLines = 12,
                    placeholder = { Text("Mitochondrium - centrum energetyczne komórki\n2³ = 8") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text("Dodaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
private fun EditQuestionDialog(
    question: QuizQuestion,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var q by rememberSaveable { mutableStateOf(question.question) }
    var a by rememberSaveable { mutableStateOf(question.answer) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj pytanie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text("Pytanie") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("Odpowiedź") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(q, a) }, enabled = q.isNotBlank() && a.isNotBlank()) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
