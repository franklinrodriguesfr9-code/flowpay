package com.flowpay.ui

import android.app.Application
import android.net.Uri
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowpay.FlowPayApplication
import com.flowpay.alarm.AlarmScheduler
import com.flowpay.alarm.NotificationHelper
import com.flowpay.backup.BackupService
import com.flowpay.data.CategoryEntity
import com.flowpay.data.ChangeScope
import com.flowpay.data.CommitmentInput
import com.flowpay.data.CommitmentView
import com.flowpay.data.NotificationDismissPolicy
import com.flowpay.data.OccurrenceStatus
import com.flowpay.data.OccurrenceStatusInput
import com.flowpay.data.OccurrenceView
import com.flowpay.data.OneOffEntryView
import com.flowpay.data.OneOffInput
import com.flowpay.data.ReminderRepeatUnit
import com.flowpay.data.ReportSnapshot
import com.flowpay.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class FlowPayUiState(
    val pendingOccurrences: List<OccurrenceView> = emptyList(),
    val monthOccurrences: List<OccurrenceView> = emptyList(),
    val commitments: List<CommitmentView> = emptyList(),
    val recentOneOffs: List<OneOffEntryView> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val report: ReportSnapshot = ReportSnapshot.empty(
        year = LocalDate.now().year,
        month = LocalDate.now().monthValue,
    ),
    val selectedMonth: YearMonth = YearMonth.from(LocalDate.now()),
    val selectedDate: LocalDate = LocalDate.now(),
    val notificationSoundUri: String? = null,
    val notificationSoundTitle: String = "Som padrao do Android",
    val reminderRepeatEnabled: Boolean = true,
    val reminderRepeatUnit: ReminderRepeatUnit = ReminderRepeatUnit.MINUTE,
    val reminderRepeatInterval: Int = 5,
    val notificationDismissPolicy: NotificationDismissPolicy = NotificationDismissPolicy.KEEP_UNTIL_DONE,
    val message: String? = null,
)

class FlowPayViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FlowPayApplication
    private val dao = app.database.dao()
    private val repository = app.repository
    private val scheduler = AlarmScheduler(application, dao)
    private val backupService = BackupService(application, app.database)
    private val preferences = AppPreferences(application)
    private val selectedMonth = MutableStateFlow(YearMonth.from(LocalDate.now()))
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val monthOccurrences = MutableStateFlow<List<OccurrenceView>>(emptyList())
    private val report = MutableStateFlow(
        ReportSnapshot.empty(LocalDate.now().year, LocalDate.now().monthValue),
    )
    private val notificationSoundUri = MutableStateFlow(preferences.notificationSoundUri)
    private val notificationSoundTitle = MutableStateFlow(soundTitle(preferences.notificationSoundUri))
    private val reminderRepeatEnabled = MutableStateFlow(preferences.reminderRepeatEnabled)
    private val reminderRepeatUnit = MutableStateFlow(preferences.reminderRepeatUnit)
    private val reminderRepeatInterval = MutableStateFlow(preferences.reminderRepeatInterval)
    private val notificationDismissPolicy = MutableStateFlow(preferences.notificationDismissPolicy)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FlowPayUiState> = combine(
        dao.observePendingOccurrences(),
        dao.observeCommitments(),
        dao.observeRecentOneOffEntries(),
        dao.observeCategories(),
        monthOccurrences,
        report,
        selectedMonth,
        selectedDate,
        notificationSoundUri,
        notificationSoundTitle,
        reminderRepeatEnabled,
        reminderRepeatUnit,
        reminderRepeatInterval,
        notificationDismissPolicy,
        message,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val pending = values[0] as List<OccurrenceView>
        val commitments = values[1] as List<CommitmentView>
        val recent = values[2] as List<OneOffEntryView>
        val categories = values[3] as List<CategoryEntity>
        val monthItems = values[4] as List<OccurrenceView>
        val currentReport = values[5] as ReportSnapshot
        val currentMonth = values[6] as YearMonth
        val currentDate = values[7] as LocalDate
        val currentSoundUri = values[8] as String?
        val currentSoundTitle = values[9] as String
        val currentRepeatEnabled = values[10] as Boolean
        val currentRepeatUnit = values[11] as ReminderRepeatUnit
        val currentRepeatInterval = values[12] as Int
        val currentDismissPolicy = values[13] as NotificationDismissPolicy
        val currentMessage = values[14] as String?
        FlowPayUiState(
            pendingOccurrences = pending,
            monthOccurrences = monthItems,
            commitments = commitments,
            recentOneOffs = recent,
            categories = categories,
            report = currentReport,
            selectedMonth = currentMonth,
            selectedDate = currentDate,
            notificationSoundUri = currentSoundUri,
            notificationSoundTitle = currentSoundTitle,
            reminderRepeatEnabled = currentRepeatEnabled,
            reminderRepeatUnit = currentRepeatUnit,
            reminderRepeatInterval = currentRepeatInterval,
            notificationDismissPolicy = currentDismissPolicy,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FlowPayUiState(),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDefaultCategories()
            repository.cleanupOldPendingOccurrences()
            repository.ensureOccurrencesAroundNow()
            scheduler.rescheduleAll()
            refreshMonthDataInternal()
        }
    }

    fun saveCommitment(input: CommitmentInput) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCommitment(input)
            refreshAfterMutation(messageText = "Compromisso salvo")
        }
    }

    fun editOccurrence(occurrenceId: Long, input: CommitmentInput, scope: ChangeScope) {
        viewModelScope.launch(Dispatchers.IO) {
            val affectedIds = repository.editOccurrence(occurrenceId, input, scope)
            refreshAfterMutation(cancelIds = affectedIds, messageText = "Lancamento atualizado")
        }
    }

    fun archiveCommitment(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val affectedIds = repository.archiveCommitment(id)
            refreshAfterMutation(cancelIds = affectedIds, messageText = "Compromisso desativado")
        }
    }

    fun cancelOccurrence(occurrenceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val affectedIds = repository.cancelOccurrence(occurrenceId)
            refreshAfterMutation(cancelIds = affectedIds, messageText = "Lancamento excluido")
        }
    }

    fun cancelFutureFrom(occurrenceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val affectedIds = repository.cancelFutureFrom(occurrenceId)
            refreshAfterMutation(cancelIds = affectedIds, messageText = "Recorrencia atualizada")
        }
    }

    fun deleteOccurrence(occurrenceId: Long, scope: ChangeScope) {
        viewModelScope.launch(Dispatchers.IO) {
            val affectedIds = repository.deleteOccurrence(occurrenceId, scope)
            refreshAfterMutation(cancelIds = affectedIds, messageText = "Lancamento excluido")
        }
    }

    fun extendCommitment(commitmentId: Long, months: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.extendCommitment(commitmentId, months)
            refreshAfterMutation(messageText = "Recorrencia prorrogada")
        }
    }

    fun completeOccurrence(occurrenceId: Long, actualCents: Long, completedDate: LocalDate, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOccurrenceStatus(
                occurrenceId,
                OccurrenceStatusInput(
                    status = OccurrenceStatus.FEITO,
                    actualCents = actualCents,
                    completedDate = completedDate,
                    notes = notes,
                ),
            )
            refreshAfterMutation(cancelIds = listOf(occurrenceId), messageText = "Marcado como feito")
        }
    }

    fun updateOccurrenceStatus(occurrenceId: Long, input: OccurrenceStatusInput) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOccurrenceStatus(occurrenceId, input)
            refreshAfterMutation(cancelIds = listOf(occurrenceId), messageText = "Status atualizado")
        }
    }

    fun addOneOff(input: OneOffInput) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addOneOff(input)
            refreshAfterMutation(messageText = "Lancamento salvo")
        }
    }

    fun exportBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                backupService.shareBackup()
            }.onSuccess {
                message.value = "Backup gerado"
            }.onFailure {
                message.value = "Nao foi possivel exportar o backup"
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val currentPendingIds = dao.getPendingScheduleItems().map { it.occurrenceId }
                val json = app.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("Arquivo vazio")
                currentPendingIds.forEach(scheduler::cancel)
                backupService.importBackup(json)
                refreshAfterMutation()
            }.onSuccess {
                message.value = "Backup importado"
            }.onFailure {
                message.value = it.message ?: "Nao foi possivel importar o backup"
            }
        }
    }

    fun previousReportMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
        selectedDate.value = selectedMonth.value.atDay(1)
        refreshMonthData()
    }

    fun nextReportMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
        selectedDate.value = selectedMonth.value.atDay(1)
        refreshMonthData()
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        selectedMonth.value = YearMonth.from(date)
        refreshMonthData()
    }

    fun setNotificationSound(uri: Uri?) {
        val value = uri?.toString()
        preferences.notificationSoundUri = value
        notificationSoundUri.value = value
        notificationSoundTitle.value = soundTitle(value)
        NotificationHelper.ensureChannel(getApplication(), uri)
        if (uri != null) RingtoneManager.getRingtone(getApplication(), uri)?.play()
        message.value = "Som de notificacao atualizado"
    }

    fun setReminderSettings(
        repeatEnabled: Boolean,
        unit: ReminderRepeatUnit,
        interval: Int,
        dismissPolicy: NotificationDismissPolicy,
    ) {
        val boundedInterval = interval.coerceIn(1, AppPreferences.maxIntervalFor(unit))
        preferences.reminderRepeatEnabled = repeatEnabled
        preferences.reminderRepeatUnit = unit
        preferences.reminderRepeatInterval = boundedInterval
        preferences.notificationDismissPolicy = dismissPolicy
        reminderRepeatEnabled.value = repeatEnabled
        reminderRepeatUnit.value = unit
        reminderRepeatInterval.value = boundedInterval
        notificationDismissPolicy.value = dismissPolicy
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.rescheduleAll()
            message.value = "Preferencias de notificacao atualizadas"
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getPendingScheduleItems().map { it.occurrenceId }.forEach(scheduler::cancel)
            repository.clearAllData()
            refreshAfterMutation(messageText = "Dados apagados")
        }
    }

    fun clearMessage() {
        message.value = null
    }

    fun refreshReport() {
        refreshMonthData()
    }

    fun refreshMonthData() {
        viewModelScope.launch(Dispatchers.IO) { refreshMonthDataInternal() }
    }

    private suspend fun refreshMonthDataInternal() {
        val month = selectedMonth.value
        report.value = repository.buildReport(month.year, month.monthValue)
        monthOccurrences.value = repository.dueOccurrencesForMonth(month.year, month.monthValue)
    }

    private suspend fun refreshAfterMutation(
        cancelIds: List<Long> = emptyList(),
        messageText: String? = null,
    ) {
        cancelIds.distinct().forEach(scheduler::cancel)
        repository.ensureOccurrencesAroundNow()
        scheduler.rescheduleAll()
        refreshMonthDataInternal()
        if (messageText != null) message.value = messageText
    }

    private fun soundTitle(uriText: String?): String {
        val context = getApplication<Application>()
        val uri = uriText?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        return runCatching {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Som padrao do Android"
    }
}
