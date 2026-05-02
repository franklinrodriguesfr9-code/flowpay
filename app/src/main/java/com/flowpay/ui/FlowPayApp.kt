@file:OptIn(ExperimentalMaterial3Api::class)

package com.flowpay.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.flowpay.alarm.NotificationHelper
import com.flowpay.data.CategoryTotal
import com.flowpay.data.ChangeScope
import com.flowpay.data.CommitmentInput
import com.flowpay.data.CommitmentKind
import com.flowpay.data.CommitmentPurpose
import com.flowpay.data.CommitmentView
import com.flowpay.data.FlowType
import com.flowpay.data.NotificationDismissPolicy
import com.flowpay.data.OccurrenceStatus
import com.flowpay.data.OccurrenceStatusInput
import com.flowpay.data.OccurrenceView
import com.flowpay.data.ReminderRepeatUnit
import com.flowpay.data.ReportLine
import com.flowpay.data.ReportSnapshot
import com.flowpay.report.ReportExporter
import com.flowpay.util.DateUtils
import com.flowpay.util.MoneyUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private enum class MainTab {
    Inicio,
    Calendario,
    Add,
    Resumo,
    Perfil,
}

private data class NavItem(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector,
)

private enum class HomeFilter {
    UMA_VEZ,
    PENDENTE,
    PAGO,
    VENCIDA,
    FEITO,
}

private const val INVOICE_CATEGORY = "Emitir Nota Fiscal"

private val defaultCategories = listOf(
    "Cartao de Credito",
    "Combustivel",
    "Academia",
    "Alimentacao",
    "Energia",
    "Internet",
    "Faculdade",
    "Terreno",
    INVOICE_CATEGORY,
    "Saude",
    "Outros",
)

@Composable
fun FlowPayApp(viewModel: FlowPayViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(MainTab.Inicio) }
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        scope.launch { snackbarHostState.showSnackbar(message) }
        viewModel.clearMessage()
    }

    val navItems = listOf(
        NavItem(MainTab.Inicio, "Inicio", Icons.Outlined.Home),
        NavItem(MainTab.Calendario, "Calendario", Icons.Outlined.CalendarToday),
        NavItem(MainTab.Add, "", Icons.Outlined.Add),
        NavItem(MainTab.Resumo, "Resumo", Icons.Outlined.BarChart),
        NavItem(MainTab.Perfil, "Perfil", Icons.Outlined.Person),
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.tab,
                        onClick = {
                            if (item.tab == MainTab.Add) {
                                showAddDialog = true
                            } else {
                                selectedTab = item.tab
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label.ifBlank { "Adicionar lancamento" },
                                modifier = if (item.tab == MainTab.Add) Modifier.size(32.dp) else Modifier.size(24.dp),
                            )
                        },
                        label = { if (item.label.isNotBlank()) Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            MonthHeader(
                month = uiState.selectedMonth,
                onPrevious = viewModel::previousReportMonth,
                onNext = viewModel::nextReportMonth,
            )
            ExactAlarmPermissionCard()
            when (selectedTab) {
                MainTab.Inicio -> HomeScreen(
                    report = uiState.report,
                    monthOccurrences = uiState.monthOccurrences,
                    onComplete = viewModel::completeOccurrence,
                )
                MainTab.Calendario -> CalendarScreen(
                    month = uiState.selectedMonth,
                    selectedDate = uiState.selectedDate,
                    occurrences = uiState.monthOccurrences,
                    commitments = uiState.commitments,
                    onSelectDate = viewModel::selectDate,
                    onComplete = viewModel::completeOccurrence,
                    onEdit = viewModel::editOccurrence,
                    onDelete = viewModel::deleteOccurrence,
                    onStatusChange = viewModel::updateOccurrenceStatus,
                    onExtend = viewModel::extendCommitment,
                )
                MainTab.Add -> Unit
                MainTab.Resumo -> SummaryScreen(report = uiState.report)
                MainTab.Perfil -> ProfileScreen(
                    soundUri = uiState.notificationSoundUri,
                    soundTitle = uiState.notificationSoundTitle,
                    repeatEnabled = uiState.reminderRepeatEnabled,
                    repeatUnit = uiState.reminderRepeatUnit,
                    repeatInterval = uiState.reminderRepeatInterval,
                    dismissPolicy = uiState.notificationDismissPolicy,
                    onExportBackup = viewModel::exportBackup,
                    onImportBackup = viewModel::importBackup,
                    onSoundPicked = viewModel::setNotificationSound,
                    onReminderSettingsChanged = viewModel::setReminderSettings,
                    onClearData = viewModel::clearAllData,
                )
            }
        }
    }

    if (showAddDialog) {
        LaunchDialog(
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveCommitment(it)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrevious) { Text("<") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FlowPay", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "%02d/%d".format(month.monthValue, month.year),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        TextButton(onClick = onNext) { Text(">") }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ExactAlarmPermissionCard() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    var canSchedule by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }
    if (canSchedule) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Ative alarmes exatos", fontWeight = FontWeight.SemiBold)
            Text("Permita Alarmes e lembretes para tocar no horario certo.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    },
                ) { Text("Abrir") }
                OutlinedButton(onClick = { canSchedule = alarmManager.canScheduleExactAlarms() }) {
                    Text("Atualizar")
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun HomeScreen(
    report: ReportSnapshot,
    monthOccurrences: List<OccurrenceView>,
    onComplete: (Long, Long, LocalDate, String) -> Unit,
) {
    var filter by remember { mutableStateOf<HomeFilter?>(null) }
    var search by remember { mutableStateOf("") }
    var completing by remember { mutableStateOf<OccurrenceView?>(null) }
    val now = System.currentTimeMillis()
    val filtered = monthOccurrences.filter { occurrence ->
        val matchesFilter = when (filter) {
            HomeFilter.UMA_VEZ -> occurrence.kind == CommitmentKind.UNICO
            HomeFilter.PENDENTE -> occurrence.status == OccurrenceStatus.PENDENTE && occurrence.dueAt >= now
            HomeFilter.PAGO -> occurrence.status == OccurrenceStatus.FEITO && occurrence.purpose != CommitmentPurpose.TASK
            HomeFilter.VENCIDA -> occurrence.status == OccurrenceStatus.PENDENTE && occurrence.dueAt < now
            HomeFilter.FEITO -> occurrence.status == OccurrenceStatus.FEITO && occurrence.purpose == CommitmentPurpose.TASK
            null -> true
        }
        matchesFilter && occurrence.matchesSearch(search)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            HomeFilterChip("Uma vez", filter, HomeFilter.UMA_VEZ) { filter = toggle(filter, HomeFilter.UMA_VEZ) }
            HomeFilterChip("Pendente", filter, HomeFilter.PENDENTE) { filter = toggle(filter, HomeFilter.PENDENTE) }
            HomeFilterChip("Pago", filter, HomeFilter.PAGO) { filter = toggle(filter, HomeFilter.PAGO) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            HomeFilterChip("Vencida", filter, HomeFilter.VENCIDA) { filter = toggle(filter, HomeFilter.VENCIDA) }
            HomeFilterChip("Feito", filter, HomeFilter.FEITO) { filter = toggle(filter, HomeFilter.FEITO) }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        label = { Text("Buscar por nome, categoria ou valor") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))

    if (filtered.isEmpty()) {
        EmptyState("Nenhuma conta neste mes", "Toque no botao + para adicionar um novo compromisso.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SmallSummary(report) }
            items(filtered, key = { it.occurrenceId }) { occurrence ->
                OccurrenceCard(
                    occurrence = occurrence,
                    onComplete = { completing = occurrence },
                )
            }
        }
    }

    completing?.let { occurrence ->
        CompleteOccurrenceDialog(
            occurrence = occurrence,
            onDismiss = { completing = null },
            onConfirm = { amount, date, notes ->
                onComplete(occurrence.occurrenceId, amount, date, notes)
                completing = null
            },
        )
    }
}

@Composable
private fun HomeFilterChip(label: String, current: HomeFilter?, target: HomeFilter, onClick: () -> Unit) {
    FilterChip(
        selected = current == target,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    )
}

@Composable
private fun CalendarScreen(
    month: YearMonth,
    selectedDate: LocalDate,
    occurrences: List<OccurrenceView>,
    commitments: List<CommitmentView>,
    onSelectDate: (LocalDate) -> Unit,
    onComplete: (Long, Long, LocalDate, String) -> Unit,
    onEdit: (Long, CommitmentInput, ChangeScope) -> Unit,
    onDelete: (Long, ChangeScope) -> Unit,
    onStatusChange: (Long, OccurrenceStatusInput) -> Unit,
    onExtend: (Long, Int) -> Unit,
) {
    var completing by remember { mutableStateOf<OccurrenceView?>(null) }
    var editing by remember { mutableStateOf<OccurrenceView?>(null) }
    var pendingEdit by remember { mutableStateOf<Pair<OccurrenceView, CommitmentInput>?>(null) }
    var deleting by remember { mutableStateOf<OccurrenceView?>(null) }
    var changingStatus by remember { mutableStateOf<OccurrenceView?>(null) }
    var extending by remember { mutableStateOf<OccurrenceView?>(null) }
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val selectedItems = occurrences.filter { DateUtils.millisToLocalDate(it.dueAt) == selectedDate }
    val commitmentMap = commitments.associateBy { it.id }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(days.chunked(7)) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val dayItems = occurrences.filter { DateUtils.millisToLocalDate(it.dueAt) == date }
                    CalendarDayCell(
                        date = date,
                        selected = date == selectedDate,
                        hasItems = dayItems.isNotEmpty(),
                        count = dayItems.size,
                        onClick = { onSelectDate(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item {
            Text(
                "Itens de ${DateUtils.formatDate(DateUtils.millisForDate(selectedDate))}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (selectedItems.isEmpty()) {
            item { EmptyState("Nada nesta data", "Escolha outro dia ou adicione pelo botao +.") }
        } else {
            items(selectedItems, key = { it.occurrenceId }) { occurrence ->
                val commitment = commitmentMap[occurrence.commitmentId]
                OccurrenceCard(
                    occurrence = occurrence,
                    onComplete = { completing = occurrence },
                    onEdit = { editing = occurrence },
                    onStatus = { changingStatus = occurrence },
                    onDelete = { deleting = occurrence },
                    onExtend = if (commitment?.isLastMonth(occurrence) == true) {
                        { extending = occurrence }
                    } else {
                        null
                    },
                )
            }
        }
    }

    completing?.let { occurrence ->
        CompleteOccurrenceDialog(
            occurrence = occurrence,
            onDismiss = { completing = null },
            onConfirm = { amount, date, notes ->
                onComplete(occurrence.occurrenceId, amount, date, notes)
                completing = null
            },
        )
    }

    editing?.let { occurrence ->
        val commitment = commitmentMap[occurrence.commitmentId]
        LaunchDialog(
            initialOccurrence = occurrence,
            initialCommitment = commitment,
            onDismiss = { editing = null },
            onSave = {
                if (occurrence.kind == CommitmentKind.RECORRENTE) {
                    pendingEdit = occurrence to it
                } else {
                    onEdit(occurrence.occurrenceId, it, ChangeScope.ONLY_THIS)
                }
                editing = null
            },
        )
    }

    deleting?.let { occurrence ->
        ScopeChoiceDialog(
            title = "Excluir lancamento?",
            occurrence = occurrence,
            onDismiss = { deleting = null },
            onScope = { scope ->
                onDelete(occurrence.occurrenceId, scope)
                deleting = null
            },
        )
    }

    pendingEdit?.let { (occurrence, input) ->
        ScopeChoiceDialog(
            title = "Aplicar edicao em quais lancamentos?",
            occurrence = occurrence,
            onDismiss = { pendingEdit = null },
            onScope = { scope ->
                onEdit(occurrence.occurrenceId, input, scope)
                pendingEdit = null
            },
        )
    }

    changingStatus?.let { occurrence ->
        StatusDialog(
            occurrence = occurrence,
            onDismiss = { changingStatus = null },
            onConfirm = { input ->
                onStatusChange(occurrence.occurrenceId, input)
                changingStatus = null
            },
        )
    }

    extending?.let { occurrence ->
        ExtendDialog(
            title = occurrence.title,
            onDismiss = { extending = null },
            onConfirm = { months ->
                onExtend(occurrence.commitmentId, months)
                extending = null
            },
        )
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    selected: Boolean,
    hasItems: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.titleMedium,
        )
        if (hasItems) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(if (count > 1) 7.dp else 5.dp),
                content = {},
            )
        }
    }
}

@Composable
private fun SummaryScreen(report: ReportSnapshot) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf("moeda") }
    var showCategories by remember { mutableStateOf(false) }
    var showLines by remember { mutableStateOf(false) }
    val paidCents = report.lines
        .filter { it.status == OccurrenceStatus.FEITO && it.purpose != CommitmentPurpose.TASK }
        .sumOf { it.amountCents }
    val taskCents = report.lines
        .filter { it.purpose == CommitmentPurpose.TASK }
        .sumOf { it.amountCents }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = mode == "quantidade", onClick = { mode = "quantidade" }, label = { Text("Quantidade") })
                FilterChip(selected = mode == "moeda", onClick = { mode = "moeda" }, label = { Text("Moeda") })
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (mode == "moeda") "Resumo em moeda" else "Resumo em quantidade", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (mode == "moeda") MoneyUtils.format(report.balanceCents) else report.lines.size.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(if (mode == "moeda") "Saldo do periodo" else "Lancamentos no periodo")
                }
            }
        }
        item {
            if (mode == "moeda") {
                StatusMetric("Pagas:", MoneyUtils.format(paidCents), PositiveGreen)
                StatusMetric("Pendentes:", MoneyUtils.format(report.openCents), WarningBlue)
                StatusMetric("Vencidas:", MoneyUtils.format(report.overdueCents), DangerRed)
                StatusMetric("Notas fiscais:", MoneyUtils.format(taskCents), MaterialTheme.colorScheme.primary)
            } else {
                StatusMetric("Pagas:", report.paidCount.toString(), PositiveGreen)
                StatusMetric("Pendentes:", report.openCount.toString(), WarningBlue)
                StatusMetric("Vencidas:", report.overdueCount.toString(), DangerRed)
                StatusMetric("Notas fiscais:", report.taskCount.toString(), MaterialTheme.colorScheme.primary)
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportMetric("Entradas feitas", MoneyUtils.format(report.incomeCents))
                    ReportMetric("Saidas feitas", MoneyUtils.format(report.expenseCents))
                    ReportMetric("Saldo", MoneyUtils.format(report.balanceCents))
                    ReportMetric("Pendentes previstos", MoneyUtils.format(report.pendingCents))
                }
            }
        }
        item {
            Button(
                onClick = { ReportExporter.shareReport(context, report) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Exportar relatorio CSV + TXT") }
        }
        item {
            ExpandableHeader(
                title = "Totais por categoria",
                expanded = showCategories,
                onToggle = { showCategories = !showCategories },
            )
        }
        if (showCategories) {
            if (report.categoryTotals.isEmpty()) {
                item { Text("Nenhum item pago neste periodo.") }
            } else {
                items(report.categoryTotals) { total -> CategoryTotalRow(total) }
            }
        }
        item {
            ExpandableHeader(
                title = "Lancamentos do periodo",
                expanded = showLines,
                onToggle = { showLines = !showLines },
            )
        }
        if (showLines) {
            if (report.lines.isEmpty()) {
                item { Text("Nenhum lancamento no periodo.") }
            } else {
                items(report.lines) { line -> ReportLineCard(line) }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    soundUri: String?,
    soundTitle: String,
    repeatEnabled: Boolean,
    repeatUnit: ReminderRepeatUnit,
    repeatInterval: Int,
    dismissPolicy: NotificationDismissPolicy,
    onExportBackup: () -> Unit,
    onImportBackup: (Uri) -> Unit,
    onSoundPicked: (Uri?) -> Unit,
    onReminderSettingsChanged: (Boolean, ReminderRepeatUnit, Int, NotificationDismissPolicy) -> Unit,
    onClearData: () -> Unit,
) {
    val context = LocalContext.current
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var localRepeatEnabled by remember(repeatEnabled) { mutableStateOf(repeatEnabled) }
    var localRepeatUnit by remember(repeatUnit) { mutableStateOf(repeatUnit) }
    var localRepeatInterval by remember(repeatInterval) { mutableStateOf(repeatInterval.toString()) }
    var localDismissPolicy by remember(dismissPolicy) { mutableStateOf(dismissPolicy) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImportUri = uri
    }
    val soundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        onSoundPicked(uri)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("FlowPay", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    AssistChip(onClick = {}, label = { Text("GRATIS") })
                    Text("Perfil local, sem login e sem sincronizacao.")
                }
            }
        }
        item {
            SettingsGroup("Backup") {
                Button(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) { Text("Exportar backup completo") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Importar backup") }
            }
        }
        item {
            SettingsGroup("Notificacoes") {
                Text("Som escolhido: $soundTitle", fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = {
                        soundLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Escolha o som do FlowPay")
                                soundUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Escolher som") }
                OutlinedButton(
                    onClick = { context.startActivity(NotificationHelper.channelSettingsIntent(context)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Configurar canal do Android") }
                FilterChip(
                    selected = localRepeatEnabled,
                    onClick = { localRepeatEnabled = !localRepeatEnabled },
                    label = { Text(if (localRepeatEnabled) "Repeticao ligada" else "Repeticao desligada") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatUnitChip("Min", ReminderRepeatUnit.MINUTE, localRepeatUnit) {
                        localRepeatUnit = it
                        localRepeatInterval = localRepeatInterval.toIntOrNull()
                            ?.coerceIn(1, maxIntervalForUi(it))
                            ?.toString()
                            ?: "5"
                    }
                    RepeatUnitChip("Hora", ReminderRepeatUnit.HOUR, localRepeatUnit) {
                        localRepeatUnit = it
                        localRepeatInterval = localRepeatInterval.toIntOrNull()
                            ?.coerceIn(1, maxIntervalForUi(it))
                            ?.toString()
                            ?: "1"
                    }
                    RepeatUnitChip("Dia", ReminderRepeatUnit.DAY, localRepeatUnit) {
                        localRepeatUnit = it
                        localRepeatInterval = localRepeatInterval.toIntOrNull()
                            ?.coerceIn(1, maxIntervalForUi(it))
                            ?.toString()
                            ?: "1"
                    }
                }
                OutlinedTextField(
                    value = localRepeatInterval,
                    onValueChange = { localRepeatInterval = it.filter(Char::isDigit).take(2) },
                    label = { Text("Intervalo (${repeatUnitLabel(localRepeatUnit)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = localDismissPolicy == NotificationDismissPolicy.KEEP_UNTIL_DONE,
                        onClick = { localDismissPolicy = NotificationDismissPolicy.KEEP_UNTIL_DONE },
                        label = { Text("Ate pagar/feito") },
                    )
                    FilterChip(
                        selected = localDismissPolicy == NotificationDismissPolicy.STOP_ON_DISMISS,
                        onClick = { localDismissPolicy = NotificationDismissPolicy.STOP_ON_DISMISS },
                        label = { Text("Parar ao dispensar") },
                    )
                }
                Button(
                    onClick = {
                        onReminderSettingsChanged(
                            localRepeatEnabled,
                            localRepeatUnit,
                            localRepeatInterval.toIntOrNull() ?: 1,
                            localDismissPolicy,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Salvar notificacoes") }
                Text("Intervalos curtos podem atrasar quando o Android entra em economia de bateria ou tela bloqueada.")
            }
        }
        item {
            SettingsGroup("Funcionamento em segundo plano") {
                PermissionStatusRows()
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Abrir otimizacao de bateria") }
            }
        }
        item {
            SettingsGroup("Premium futuro") {
                Text("Espaco preparado para recursos futuros, sem cobranca nesta versao.")
            }
        }
        item {
            SettingsGroup("Dados") {
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Limpar dados do app")
                }
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Importar backup?") },
            text = { Text("Isso substitui todos os dados atuais pelos dados do arquivo escolhido.") },
            confirmButton = {
                Button(onClick = {
                    onImportBackup(uri)
                    pendingImportUri = null
                }) { Text("Importar") }
            },
            dismissButton = { TextButton(onClick = { pendingImportUri = null }) { Text("Cancelar") } },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Limpar dados?") },
            text = { Text("Isso remove compromissos, pendencias e lancamentos salvos neste aparelho.") },
            confirmButton = {
                Button(onClick = {
                    onClearData()
                    confirmClear = false
                }) { Text("Limpar") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun LaunchDialog(
    onDismiss: () -> Unit,
    onSave: (CommitmentInput) -> Unit,
    initialOccurrence: OccurrenceView? = null,
    initialCommitment: CommitmentView? = null,
) {
    val isEditing = initialCommitment != null
    val initialCategory = initialCommitment?.categoryName ?: defaultCategories.first()
    val categoryInList = defaultCategories.any { it.equals(initialCategory, ignoreCase = true) }
    var kind by remember { mutableStateOf(initialCommitment?.kind ?: CommitmentKind.UNICO) }
    var title by remember { mutableStateOf(initialCommitment?.title ?: "") }
    var category by remember { mutableStateOf(if (categoryInList) initialCategory else "Outros") }
    var customCategory by remember { mutableStateOf(if (categoryInList) "" else initialCategory) }
    var amount by remember {
        mutableStateOf(
            initialCommitment?.expectedCents
                ?.takeIf { it > 0 }
                ?.let(MoneyUtils::format)
                .orEmpty(),
        )
    }
    var variableAmount by remember { mutableStateOf(initialCommitment?.variableAmount ?: false) }
    var dueDate by remember {
        mutableStateOf(
            initialCommitment?.startDate
                ?.takeIf { it > 0 }
                ?.let(DateUtils::millisToLocalDate)
                ?: initialOccurrence?.dueAt?.let(DateUtils::millisToLocalDate)
                ?: LocalDate.now(),
        )
    }
    var timeHour by remember { mutableStateOf(initialCommitment?.reminderHour ?: 9) }
    var timeMinute by remember { mutableStateOf(initialCommitment?.reminderMinute ?: 0) }
    var endDate by remember {
        mutableStateOf(
            initialCommitment?.endDate?.let(DateUtils::millisToLocalDate) ?: dueDate.plusMonths(11),
        )
    }
    var createRetroactive by remember { mutableStateOf(false) }
    var retroactiveUntil by remember { mutableStateOf(LocalDate.now()) }
    var flowType by remember { mutableStateOf(initialCommitment?.flowType ?: FlowType.ENTRADA) }
    var notes by remember { mutableStateOf(initialCommitment?.notes ?: initialOccurrence?.notes.orEmpty()) }
    var useGlobalReminder by remember { mutableStateOf(initialCommitment?.useGlobalReminderSettings ?: true) }
    var repeatEnabled by remember { mutableStateOf(initialCommitment?.reminderRepeatEnabled ?: true) }
    var repeatUnit by remember { mutableStateOf(initialCommitment?.reminderRepeatUnit ?: ReminderRepeatUnit.MINUTE) }
    var repeatInterval by remember { mutableStateOf((initialCommitment?.reminderRepeatInterval ?: 5).toString()) }
    var dismissPolicy by remember {
        mutableStateOf(initialCommitment?.notificationDismissPolicy ?: NotificationDismissPolicy.KEEP_UNTIL_DONE)
    }
    val isInvoice = isInvoiceCategory(category)
    val effectiveFlowType = if (isInvoice) FlowType.ENTRADA else flowType
    val periodMonths = periodMonthsInclusive(dueDate, endDate)
    val invalidPeriod = kind == CommitmentKind.RECORRENTE && (endDate.isBefore(dueDate) || periodMonths > 12)
    val canSave = !invalidPeriod

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar lancamento" else "Novo lancamento") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == CommitmentKind.UNICO, onClick = { kind = CommitmentKind.UNICO }, label = { Text("Unico") })
                    FilterChip(selected = kind == CommitmentKind.RECORRENTE, onClick = { kind = CommitmentKind.RECORRENTE }, label = { Text("Recorrente") })
                }
                FlowTypeSelector(flowType = effectiveFlowType, enabled = !isInvoice, onChange = { flowType = it })
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                CategorySelector(
                    category = category,
                    customCategory = customCategory,
                    onCategory = {
                        category = it
                        if (isInvoiceCategory(it)) flowType = FlowType.ENTRADA
                    },
                    onCustomCategory = { customCategory = it },
                )
                if (isInvoice) {
                    Text("Esta categoria e um compromisso de entrada e sera marcada como Feito.")
                }
                if (kind == CommitmentKind.RECORRENTE) {
                    FilterChip(selected = variableAmount, onClick = { variableAmount = !variableAmount }, label = { Text("Valor variavel") })
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (variableAmount) "Valor previsto (opcional)" else "Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateButton("Data inicio", dueDate, onChange = {
                    dueDate = it
                    if (endDate.isBefore(it) || periodMonthsInclusive(it, endDate) > 12) {
                        endDate = it.plusMonths(11)
                    }
                })
                if (kind == CommitmentKind.RECORRENTE) {
                    DateButton("Data fim", endDate, onChange = { endDate = it })
                    Text("Periodo: ${periodMonths.coerceAtLeast(0)} mes(es). Limite: 12 meses.")
                    if (invalidPeriod) {
                        Text("A data fim precisa ficar entre a data inicio e no maximo 12 meses.", color = MaterialTheme.colorScheme.error)
                    }
                }
                TimeButton("Horario do lembrete", timeHour, timeMinute, onChange = { h, m -> timeHour = h; timeMinute = m })
                FilterChip(
                    selected = useGlobalReminder,
                    onClick = { useGlobalReminder = !useGlobalReminder },
                    label = { Text(if (useGlobalReminder) "Usar notificacao padrao" else "Personalizar notificacao") },
                )
                if (!useGlobalReminder) {
                    FilterChip(
                        selected = repeatEnabled,
                        onClick = { repeatEnabled = !repeatEnabled },
                        label = { Text(if (repeatEnabled) "Repetir lembrete" else "Nao repetir") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatUnitChip("Min", ReminderRepeatUnit.MINUTE, repeatUnit) {
                            repeatUnit = it
                            repeatInterval = repeatInterval.toIntOrNull()?.coerceIn(1, maxIntervalForUi(it))?.toString() ?: "5"
                        }
                        RepeatUnitChip("Hora", ReminderRepeatUnit.HOUR, repeatUnit) {
                            repeatUnit = it
                            repeatInterval = repeatInterval.toIntOrNull()?.coerceIn(1, maxIntervalForUi(it))?.toString() ?: "1"
                        }
                        RepeatUnitChip("Dia", ReminderRepeatUnit.DAY, repeatUnit) {
                            repeatUnit = it
                            repeatInterval = repeatInterval.toIntOrNull()?.coerceIn(1, maxIntervalForUi(it))?.toString() ?: "1"
                        }
                    }
                    OutlinedTextField(
                        value = repeatInterval,
                        onValueChange = { repeatInterval = it.filter(Char::isDigit).take(2) },
                        label = { Text("Intervalo (${repeatUnitLabel(repeatUnit)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = dismissPolicy == NotificationDismissPolicy.KEEP_UNTIL_DONE,
                            onClick = { dismissPolicy = NotificationDismissPolicy.KEEP_UNTIL_DONE },
                            label = { Text("Ate pagar/feito") },
                        )
                        FilterChip(
                            selected = dismissPolicy == NotificationDismissPolicy.STOP_ON_DISMISS,
                            onClick = { dismissPolicy = NotificationDismissPolicy.STOP_ON_DISMISS },
                            label = { Text("Parar ao dispensar") },
                        )
                    }
                }
                if (kind == CommitmentKind.RECORRENTE && dueDate.isBefore(LocalDate.now()) && !isEditing) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("A data inicial esta no passado.")
                            FilterChip(
                                selected = createRetroactive,
                                onClick = { createRetroactive = !createRetroactive },
                                label = { Text("Criar retroativo") },
                            )
                            if (createRetroactive) {
                                DateButton("Retroativo ate", retroactiveUntil, onChange = { retroactiveUntil = it })
                            }
                        }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observacao") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val finalCategory = if (category == "Outros") customCategory.ifBlank { "Outros" } else category
                    val purpose = if (isInvoiceCategory(finalCategory)) CommitmentPurpose.TASK else CommitmentPurpose.PAYMENT
                    onSave(
                        CommitmentInput(
                            id = initialCommitment?.id,
                            title = title.ifBlank { if (kind == CommitmentKind.UNICO) "Lancamento unico" else "Compromisso recorrente" },
                            kind = kind,
                            purpose = purpose,
                            flowType = if (purpose == CommitmentPurpose.TASK) FlowType.ENTRADA else flowType,
                            categoryName = finalCategory,
                            dueDate = dueDate,
                            reminderHour = timeHour,
                            reminderMinute = timeMinute,
                            expectedCents = MoneyUtils.parseToCents(amount).takeIf { !variableAmount || amount.isNotBlank() },
                            variableAmount = variableAmount,
                            endDate = if (kind == CommitmentKind.RECORRENTE) endDate else null,
                            createRetroactive = kind == CommitmentKind.RECORRENTE && createRetroactive,
                            retroactiveUntil = retroactiveUntil,
                            useGlobalReminderSettings = useGlobalReminder,
                            reminderRepeatEnabled = repeatEnabled,
                            reminderRepeatUnit = repeatUnit,
                            reminderRepeatInterval = repeatInterval.toIntOrNull() ?: 5,
                            notificationDismissPolicy = dismissPolicy,
                            notes = notes,
                        ),
                    )
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CompleteOccurrenceDialog(
    occurrence: OccurrenceView,
    onDismiss: () -> Unit,
    onConfirm: (Long, LocalDate, String) -> Unit,
) {
    val task = occurrence.purpose == CommitmentPurpose.TASK
    var amount by remember { mutableStateOf(MoneyUtils.format(occurrence.expectedCents)) }
    var completedDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf(occurrence.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task) "Marcar como feito" else "Marcar como pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(occurrence.title)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Valor real") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateButton(if (task) "Data feita" else "Data de pagamento", completedDate, onChange = { completedDate = it })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observacao") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(MoneyUtils.parseToCents(amount), completedDate, notes) }) {
                Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ScopeChoiceDialog(
    title: String,
    occurrence: OccurrenceView,
    onDismiss: () -> Unit,
    onScope: (ChangeScope) -> Unit,
) {
    if (occurrence.kind == CommitmentKind.UNICO) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text("Este lancamento sera atualizado individualmente.") },
            confirmButton = { Button(onClick = { onScope(ChangeScope.ONLY_THIS) }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(occurrence.title)
                Text("Escolha como aplicar esta alteracao na recorrencia.")
                Button(onClick = { onScope(ChangeScope.ONLY_THIS) }, modifier = Modifier.fillMaxWidth()) { Text("Apenas este") }
                OutlinedButton(onClick = { onScope(ChangeScope.ALL) }, modifier = Modifier.fillMaxWidth()) { Text("Todos") }
                OutlinedButton(onClick = { onScope(ChangeScope.FROM_THIS_FORWARD) }, modifier = Modifier.fillMaxWidth()) { Text("Deste em diante") }
                OutlinedButton(onClick = { onScope(ChangeScope.UNTIL_THIS) }, modifier = Modifier.fillMaxWidth()) { Text("Deste para tras") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun StatusDialog(
    occurrence: OccurrenceView,
    onDismiss: () -> Unit,
    onConfirm: (OccurrenceStatusInput) -> Unit,
) {
    var status by remember { mutableStateOf(occurrence.status) }
    var amount by remember { mutableStateOf(MoneyUtils.format(occurrence.actualCents ?: occurrence.expectedCents)) }
    var completedDate by remember {
        mutableStateOf(occurrence.completedAt?.let(DateUtils::millisToLocalDate) ?: LocalDate.now())
    }
    var notes by remember { mutableStateOf(occurrence.notes) }
    val doneLabel = if (occurrence.purpose == CommitmentPurpose.TASK) "Feito" else "Pago"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alterar status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(occurrence.title)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == OccurrenceStatus.PENDENTE,
                        onClick = { status = OccurrenceStatus.PENDENTE },
                        label = { Text("Pendente") },
                    )
                    FilterChip(
                        selected = status == OccurrenceStatus.FEITO,
                        onClick = { status = OccurrenceStatus.FEITO },
                        label = { Text(doneLabel) },
                    )
                }
                if (status == OccurrenceStatus.FEITO) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Valor real") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateButton(if (occurrence.purpose == CommitmentPurpose.TASK) "Data feita" else "Data de pagamento", completedDate) {
                        completedDate = it
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observacao") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        OccurrenceStatusInput(
                            status = status,
                            actualCents = if (status == OccurrenceStatus.FEITO) MoneyUtils.parseToCents(amount) else null,
                            completedDate = if (status == OccurrenceStatus.FEITO) completedDate else null,
                            notes = notes,
                        ),
                    )
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ExtendDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var monthsText by remember { mutableStateOf("1") }
    val months = monthsText.toIntOrNull()?.coerceIn(1, 12) ?: 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prorrogar recorrencia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title)
                Text("Informe quantos meses deseja adicionar, de 1 a 12.")
                OutlinedTextField(
                    value = monthsText,
                    onValueChange = { monthsText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Meses") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(months) }) { Text("Prorrogar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun OccurrenceCard(
    occurrence: OccurrenceView,
    onComplete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onStatus: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onExtend: (() -> Unit)? = null,
) {
    val overdue = occurrence.status == OccurrenceStatus.PENDENTE && occurrence.dueAt < System.currentTimeMillis()
    val status = statusLabel(occurrence, overdue)
    val amountColor = if (occurrence.flowType == FlowType.ENTRADA) PositiveGreen else DangerRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(occurrence.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${occurrence.categoryName ?: "Sem categoria"} - ${DateUtils.formatDate(occurrence.dueAt)}")
                }
                Text(
                    MoneyUtils.format(occurrence.actualCents ?: occurrence.expectedCents),
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(if (occurrence.flowType == FlowType.ENTRADA) "Entrada" else "Saida", amountColor)
                StatusPill(if (occurrence.kind == CommitmentKind.UNICO) "Uma vez" else "Recorrente", MaterialTheme.colorScheme.secondary)
                StatusPill(status, if (overdue) DangerRed else MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (occurrence.status == OccurrenceStatus.PENDENTE) {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = if (occurrence.purpose == CommitmentPurpose.TASK) MaterialTheme.colorScheme.primary else PositiveGreen),
                    ) {
                        Text(actionLabel(occurrence))
                    }
                }
                onExtend?.let {
                    OutlinedButton(onClick = it) { Text("Prorrogar") }
                }
                onStatus?.let {
                    OutlinedButton(onClick = it) { Text("Status") }
                }
                Spacer(Modifier.weight(1f))
                onEdit?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                    }
                }
                onDelete?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Excluir")
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallSummary(report: ReportSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReportMetric("Pagas", report.paidCount.toString())
            ReportMetric("Pendentes", report.openCount.toString())
            ReportMetric("Vencidas", report.overdueCount.toString())
            ReportMetric("Feitas", report.taskCount.toString())
        }
    }
}

@Composable
private fun StatusMetric(label: String, value: String, color: Color) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(value, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun CategoryTotalRow(total: CategoryTotal) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(total.categoryName, fontWeight = FontWeight.SemiBold)
                Text(if (total.flowType == FlowType.ENTRADA) "Entrada" else "Saida")
            }
            Text(
                MoneyUtils.format(total.totalCents),
                fontWeight = FontWeight.Bold,
                color = if (total.flowType == FlowType.ENTRADA) PositiveGreen else DangerRed,
            )
        }
    }
}

@Composable
private fun ReportLineCard(line: ReportLine) {
    val overdue = line.status == OccurrenceStatus.PENDENTE && DateUtils.daysLate(line.dueAt) > 0
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(line.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(MoneyUtils.format(line.amountCents), color = if (line.flowType == FlowType.ENTRADA) PositiveGreen else DangerRed)
            }
            Text("${line.categoryName} - ${line.source}")
            Text("Vencimento: ${DateUtils.formatDate(line.dueAt)}")
            line.completedAt?.let { Text("Concluido em: ${DateUtils.formatDate(it)}") }
            if (overdue) Text("Atraso: ${DateUtils.daysLate(line.dueAt)} dia(s)", color = DangerRed)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun RepeatUnitChip(
    label: String,
    unit: ReminderRepeatUnit,
    selectedUnit: ReminderRepeatUnit,
    onSelect: (ReminderRepeatUnit) -> Unit,
) {
    FilterChip(
        selected = selectedUnit == unit,
        onClick = { onSelect(unit) },
        label = { Text(label) },
    )
}

@Composable
private fun PermissionStatusRows() {
    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    val notificationsOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val exactAlarmOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    val appNotificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()

    ReportMetric("Notificacoes do app", if (appNotificationsOk && notificationsOk) "Ativas" else "Revisar")
    ReportMetric("Alarmes exatos", if (exactAlarmOk) "Ativos" else "Revisar")
}

@Composable
private fun ExpandableHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun CategorySelector(
    category: String,
    customCategory: String,
    onCategory: (String) -> Unit,
    onCustomCategory: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Categoria: $category")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            defaultCategories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onCategory(option)
                        expanded = false
                    },
                )
            }
        }
    }
    if (category == "Outros") {
        OutlinedTextField(
            value = customCategory,
            onValueChange = onCustomCategory,
            label = { Text("Nome da categoria") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FlowTypeSelector(flowType: FlowType, enabled: Boolean, onChange: (FlowType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = flowType == FlowType.ENTRADA,
            enabled = enabled,
            onClick = { onChange(FlowType.ENTRADA) },
            label = { Text("Entrada") },
        )
        FilterChip(
            selected = flowType == FlowType.SAIDA,
            enabled = enabled,
            onClick = { onChange(FlowType.SAIDA) },
            label = { Text("Saida") },
        )
    }
}

@Composable
private fun DateButton(label: String, date: LocalDate, onChange: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onChange(LocalDate.of(year, month + 1, day)) },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            ).show()
        },
    ) {
        Text("$label: ${DateUtils.formatDate(DateUtils.millisForDate(date))}")
    }
}

@Composable
private fun TimeButton(label: String, hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute -> onChange(selectedHour, selectedMinute) },
                hour,
                minute,
                true,
            ).show()
        },
    ) {
        Text("$label: %02d:%02d".format(hour, minute))
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle)
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

private fun toggle(current: HomeFilter?, target: HomeFilter): HomeFilter? =
    if (current == target) null else target

private fun statusLabel(occurrence: OccurrenceView, overdue: Boolean): String {
    if (occurrence.status == OccurrenceStatus.FEITO) {
        return if (occurrence.purpose == CommitmentPurpose.TASK) "Feito" else "Pago"
    }
    return if (overdue) "Vencida" else "Pendente"
}

private fun actionLabel(occurrence: OccurrenceView): String {
    return when {
        occurrence.purpose == CommitmentPurpose.TASK -> "Feito"
        occurrence.flowType == FlowType.ENTRADA -> "Receber"
        else -> "Pagar"
    }
}

private fun OccurrenceView.matchesSearch(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true
    val amount = MoneyUtils.format(actualCents ?: expectedCents)
    val haystack = listOf(
        title,
        categoryName.orEmpty(),
        amount,
        amount.filter(Char::isDigit),
        DateUtils.formatDate(dueAt),
    ).joinToString(" ").lowercase()
    return haystack.contains(normalized.lowercase())
}

private fun repeatUnitLabel(unit: ReminderRepeatUnit): String = when (unit) {
    ReminderRepeatUnit.MINUTE -> "minutos"
    ReminderRepeatUnit.HOUR -> "horas"
    ReminderRepeatUnit.DAY -> "dias"
}

private fun maxIntervalForUi(unit: ReminderRepeatUnit): Int = when (unit) {
    ReminderRepeatUnit.MINUTE -> 59
    ReminderRepeatUnit.HOUR -> 23
    ReminderRepeatUnit.DAY -> 30
}

private fun isInvoiceCategory(category: String?): Boolean =
    category.equals(INVOICE_CATEGORY, ignoreCase = true) ||
        category.equals("Nota Fiscal", ignoreCase = true) ||
        category.equals("Nota fiscal", ignoreCase = true)

private fun periodMonthsInclusive(start: LocalDate, end: LocalDate): Long {
    if (end.isBefore(start)) return 0
    return ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1
}

private fun CommitmentView.isLastMonth(occurrence: OccurrenceView): Boolean {
    if (kind != CommitmentKind.RECORRENTE || endDate == null) return false
    return YearMonth.from(DateUtils.millisToLocalDate(endDate)) == YearMonth.from(DateUtils.millisToLocalDate(occurrence.dueAt))
}

private val PositiveGreen = Color(0xFF1E7D5A)
private val DangerRed = Color(0xFFC04444)
private val WarningBlue = Color(0xFF2F72D6)
