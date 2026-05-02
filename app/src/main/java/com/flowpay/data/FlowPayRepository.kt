package com.flowpay.data

import com.flowpay.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CommitmentInput(
    val id: Long? = null,
    val title: String,
    val kind: CommitmentKind,
    val purpose: CommitmentPurpose,
    val flowType: FlowType,
    val categoryName: String,
    val dueDate: LocalDate,
    val reminderHour: Int,
    val reminderMinute: Int,
    val expectedCents: Long?,
    val variableAmount: Boolean,
    val endDate: LocalDate?,
    val createRetroactive: Boolean = false,
    val retroactiveUntil: LocalDate? = null,
    val notificationSoundUri: String? = null,
    val useGlobalReminderSettings: Boolean = true,
    val reminderRepeatEnabled: Boolean = true,
    val reminderRepeatUnit: ReminderRepeatUnit = ReminderRepeatUnit.MINUTE,
    val reminderRepeatInterval: Int = 5,
    val notificationDismissPolicy: NotificationDismissPolicy = NotificationDismissPolicy.KEEP_UNTIL_DONE,
    val notes: String,
)

enum class ChangeScope {
    ONLY_THIS,
    ALL,
    FROM_THIS_FORWARD,
    UNTIL_THIS,
}

data class OccurrenceStatusInput(
    val status: OccurrenceStatus,
    val actualCents: Long?,
    val completedDate: LocalDate?,
    val notes: String,
)

data class OneOffInput(
    val title: String,
    val flowType: FlowType,
    val categoryName: String,
    val amountCents: Long,
    val occurredAt: Long,
    val notes: String,
)

class FlowPayRepository(private val dao: FlowPayDao) {
    suspend fun seedDefaultCategories() {
        listOf(
            "Emitir Nota Fiscal",
            "Cartao de credito",
            "Academia",
            "Faculdade",
            "Energia",
            "Internet",
            "Terreno",
            "Combustivel",
            "Alimentacao",
            "Saude",
            "Outros",
        ).forEach { findOrCreateCategory(it) }
    }

    suspend fun saveCommitment(input: CommitmentInput): Long {
        val now = System.currentTimeMillis()
        val categoryId = findOrCreateCategory(input.categoryName)
        val existing = input.id?.let { dao.getCommitment(it) }
        val effectiveDueDate = effectiveStartDate(input)
        val startMillis = DateUtils.millisForDate(effectiveDueDate)
        val endDate = effectiveEndDate(input, effectiveDueDate)
        val endMillis = endDate?.let(DateUtils::endOfDayMillis)
        val expectedCents = input.expectedCents ?: 0
        val purpose = effectivePurpose(input)
        val flowType = if (purpose == CommitmentPurpose.TASK) FlowType.ENTRADA else input.flowType
        val id = if (existing == null) {
            dao.insertCommitment(
                RecurringCommitmentEntity(
                    title = input.title.trim(),
                    kind = input.kind,
                    purpose = purpose,
                    flowType = flowType,
                    categoryId = categoryId,
                    dueDay = effectiveDueDate.dayOfMonth.coerceIn(1, 31),
                    reminderHour = input.reminderHour.coerceIn(0, 23),
                    reminderMinute = input.reminderMinute.coerceIn(0, 59),
                    expectedCents = expectedCents,
                    variableAmount = input.variableAmount,
                    startDate = startMillis,
                    endDate = endMillis,
                    notificationSoundUri = input.notificationSoundUri,
                    reminderRepeatEnabled = input.reminderRepeatEnabled,
                    useGlobalReminderSettings = input.useGlobalReminderSettings,
                    reminderRepeatUnit = input.reminderRepeatUnit,
                    reminderRepeatInterval = input.reminderRepeatInterval.coerceIn(1, maxIntervalFor(input.reminderRepeatUnit)),
                    notificationDismissPolicy = input.notificationDismissPolicy,
                    notes = input.notes.trim(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            dao.updateCommitment(
                existing.copy(
                    title = input.title.trim(),
                    kind = input.kind,
                    purpose = purpose,
                    flowType = flowType,
                    categoryId = categoryId,
                    dueDay = effectiveDueDate.dayOfMonth.coerceIn(1, 31),
                    reminderHour = input.reminderHour.coerceIn(0, 23),
                    reminderMinute = input.reminderMinute.coerceIn(0, 59),
                    expectedCents = expectedCents,
                    variableAmount = input.variableAmount,
                    startDate = startMillis,
                    endDate = endMillis,
                    notificationSoundUri = input.notificationSoundUri,
                    reminderRepeatEnabled = input.reminderRepeatEnabled,
                    useGlobalReminderSettings = input.useGlobalReminderSettings,
                    reminderRepeatUnit = input.reminderRepeatUnit,
                    reminderRepeatInterval = input.reminderRepeatInterval.coerceIn(1, maxIntervalFor(input.reminderRepeatUnit)),
                    notificationDismissPolicy = input.notificationDismissPolicy,
                    notes = input.notes.trim(),
                    active = true,
                    updatedAt = now,
                ),
            )
            existing.id
        }
        val commitment = dao.getCommitment(id) ?: return id
        if (existing != null) {
            dao.deletePendingOccurrencesForCommitmentFrom(id, startMillis)
        }
        if (input.createRetroactive) {
            ensureOccurrencesForRange(
                commitment = commitment,
                start = input.dueDate,
                end = input.retroactiveUntil ?: LocalDate.now(),
            )
        }
        ensureNextOccurrence(commitment)
        return id
    }

    suspend fun archiveCommitment(id: Long): List<Long> {
        val pendingIds = dao.getPendingOccurrenceIdsForCommitment(id)
        dao.archiveCommitment(id, System.currentTimeMillis())
        dao.deletePendingOccurrencesForCommitment(id)
        return pendingIds
    }

    suspend fun editOccurrence(occurrenceId: Long, input: CommitmentInput, scope: ChangeScope): List<Long> {
        val occurrence = dao.getOccurrenceById(occurrenceId) ?: return emptyList()
        val existing = dao.getCommitment(occurrence.commitmentId) ?: return emptyList()
        val categoryId = findOrCreateCategory(input.categoryName)
        val purpose = effectivePurpose(input)
        val flowType = if (purpose == CommitmentPurpose.TASK) FlowType.ENTRADA else input.flowType
        val startDate = effectiveStartDate(input)
        val endDate = effectiveEndDate(input, startDate)
        val updatedCommitment = existing.copy(
            title = input.title.trim(),
            kind = input.kind,
            purpose = purpose,
            flowType = flowType,
            categoryId = categoryId,
            dueDay = startDate.dayOfMonth.coerceIn(1, 31),
            reminderHour = input.reminderHour.coerceIn(0, 23),
            reminderMinute = input.reminderMinute.coerceIn(0, 59),
            expectedCents = input.expectedCents ?: 0,
            variableAmount = input.variableAmount,
            startDate = DateUtils.millisForDate(startDate),
            endDate = endDate?.let(DateUtils::endOfDayMillis),
            notificationSoundUri = input.notificationSoundUri,
            reminderRepeatEnabled = input.reminderRepeatEnabled,
            useGlobalReminderSettings = input.useGlobalReminderSettings,
            reminderRepeatUnit = input.reminderRepeatUnit,
            reminderRepeatInterval = input.reminderRepeatInterval.coerceIn(1, maxIntervalFor(input.reminderRepeatUnit)),
            notificationDismissPolicy = input.notificationDismissPolicy,
            notes = input.notes.trim(),
            active = true,
            updatedAt = System.currentTimeMillis(),
        )

        if (scope == ChangeScope.ALL || scope == ChangeScope.FROM_THIS_FORWARD) {
            dao.updateCommitment(updatedCommitment)
        }

        val allOccurrences = dao.getAllOccurrences()
            .filter { it.commitmentId == occurrence.commitmentId && it.status != OccurrenceStatus.CANCELADO }
        val targets = allOccurrences.filter {
            when (scope) {
                ChangeScope.ONLY_THIS -> it.id == occurrenceId
                ChangeScope.ALL -> true
                ChangeScope.FROM_THIS_FORWARD -> it.dueAt >= occurrence.dueAt
                ChangeScope.UNTIL_THIS -> it.dueAt <= occurrence.dueAt
            }
        }
        targets.forEach { target ->
            dao.updateOccurrence(target.updatedFromInput(updatedCommitment, input))
        }
        return targets.filter { it.status == OccurrenceStatus.PENDENTE }.map { it.id }
    }

    suspend fun cancelOccurrence(occurrenceId: Long): List<Long> {
        val occurrence = dao.getOccurrenceById(occurrenceId) ?: return emptyList()
        dao.cancelOccurrence(occurrenceId, System.currentTimeMillis())
        val commitment = dao.getCommitment(occurrence.commitmentId)
        if (commitment?.kind == CommitmentKind.UNICO) {
            dao.archiveCommitment(commitment.id, System.currentTimeMillis())
        }
        return listOf(occurrenceId)
    }

    suspend fun deleteOccurrence(occurrenceId: Long, scope: ChangeScope): List<Long> {
        val occurrence = dao.getOccurrenceById(occurrenceId) ?: return emptyList()
        val commitment = dao.getCommitment(occurrence.commitmentId) ?: return emptyList()
        val now = System.currentTimeMillis()
        return when (scope) {
            ChangeScope.ONLY_THIS -> cancelOccurrence(occurrenceId)
            ChangeScope.ALL -> {
                val ids = dao.getOccurrenceIdsForCommitment(commitment.id)
                dao.cancelOccurrencesForCommitment(commitment.id, now)
                dao.archiveCommitment(commitment.id, now)
                ids
            }
            ChangeScope.FROM_THIS_FORWARD -> {
                val ids = dao.getOccurrenceIdsForCommitmentFrom(commitment.id, occurrence.dueAt)
                dao.cancelOccurrencesForCommitmentFrom(commitment.id, occurrence.dueAt, now)
                val selectedMonth = YearMonth.of(occurrence.year, occurrence.month)
                val newEnd = selectedMonth.minusMonths(1).atEndOfMonth()
                val start = commitment.startLocalDate()
                if (newEnd.isBefore(start)) {
                    dao.archiveCommitment(commitment.id, now)
                } else {
                    dao.updateCommitment(commitment.copy(endDate = DateUtils.endOfDayMillis(newEnd), updatedAt = now))
                }
                ids
            }
            ChangeScope.UNTIL_THIS -> {
                val ids = dao.getOccurrenceIdsForCommitmentUntil(commitment.id, occurrence.dueAt)
                dao.cancelOccurrencesForCommitmentUntil(commitment.id, occurrence.dueAt, now)
                ids
            }
        }
    }

    suspend fun cancelFutureFrom(occurrenceId: Long): List<Long> {
        val occurrence = dao.getOccurrenceById(occurrenceId) ?: return emptyList()
        val commitment = dao.getCommitment(occurrence.commitmentId) ?: return emptyList()
        if (commitment.kind == CommitmentKind.UNICO) return cancelOccurrence(occurrenceId)

        val affectedIds = dao.getPendingOccurrenceIdsForCommitmentFrom(commitment.id, occurrence.dueAt)
        dao.cancelPendingOccurrencesForCommitmentFrom(commitment.id, occurrence.dueAt, System.currentTimeMillis())
        val selectedMonth = YearMonth.of(occurrence.year, occurrence.month)
        val newEnd = selectedMonth.minusMonths(1).atEndOfMonth()
        val start = commitment.startLocalDate()
        if (newEnd.isBefore(start)) {
            dao.archiveCommitment(commitment.id, System.currentTimeMillis())
        } else {
            dao.updateCommitment(
                commitment.copy(
                    endDate = DateUtils.endOfDayMillis(newEnd),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        return affectedIds
    }

    suspend fun extendCommitment(commitmentId: Long, months: Int) {
        val commitment = dao.getCommitment(commitmentId) ?: return
        if (commitment.kind != CommitmentKind.RECORRENTE || !commitment.active) return
        val boundedMonths = months.coerceIn(1, 12)
        val currentEnd = commitment.endLocalDate() ?: commitment.startLocalDate()
        val newEnd = currentEnd.plusMonths(boundedMonths.toLong())
        dao.updateCommitment(
            commitment.copy(
                endDate = DateUtils.endOfDayMillis(newEnd),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun silenceOccurrenceToday(occurrenceId: Long) {
        dao.silenceOccurrenceUntil(occurrenceId, DateUtils.endOfDayMillis(LocalDate.now()))
    }

    suspend fun updateOccurrenceStatus(occurrenceId: Long, input: OccurrenceStatusInput) {
        val occurrence = dao.getOccurrenceById(occurrenceId) ?: return
        if (input.status == OccurrenceStatus.PENDENTE) {
            dao.markOccurrencePending(occurrenceId, input.notes.trim())
            dao.getCommitment(occurrence.commitmentId)?.let { commitment ->
                if (!commitment.active) {
                    dao.updateCommitment(commitment.copy(active = true, updatedAt = System.currentTimeMillis()))
                }
            }
            return
        }
        val completedAt = DateUtils.millisForDate(input.completedDate ?: LocalDate.now())
        dao.updateOccurrence(
            occurrence.copy(
                status = OccurrenceStatus.FEITO,
                actualCents = input.actualCents ?: occurrence.expectedCents,
                completedAt = completedAt,
                reminderSilencedUntil = null,
                reminderStoppedAt = null,
                notes = input.notes.trim(),
            ),
        )
        val commitment = dao.getCommitment(occurrence.commitmentId)
        if (commitment?.active == true && commitment.kind == CommitmentKind.RECORRENTE) {
            ensureOccurrence(commitment, YearMonth.of(occurrence.year, occurrence.month).plusMonths(1))
        } else if (commitment?.kind == CommitmentKind.UNICO) {
            dao.archiveCommitment(commitment.id, System.currentTimeMillis())
        }
    }

    suspend fun completeOccurrence(occurrenceId: Long, actualCents: Long, completedAt: Long, notes: String) {
        val occurrence = dao.getOccurrenceById(occurrenceId) ?: return
        dao.updateOccurrence(
            occurrence.copy(
                status = OccurrenceStatus.FEITO,
                actualCents = actualCents,
                completedAt = completedAt,
                reminderSilencedUntil = null,
                notes = notes.trim(),
            ),
        )
        val commitment = dao.getCommitment(occurrence.commitmentId)
        if (commitment?.active == true && commitment.kind == CommitmentKind.RECORRENTE) {
            val nextMonth = YearMonth.of(occurrence.year, occurrence.month).plusMonths(1)
            ensureOccurrence(commitment, nextMonth)
        } else if (commitment?.kind == CommitmentKind.UNICO) {
            dao.archiveCommitment(commitment.id, System.currentTimeMillis())
        }
    }

    suspend fun addOneOff(input: OneOffInput): Long {
        val categoryId = findOrCreateCategory(input.categoryName)
        return dao.insertOneOffEntry(
            OneOffEntryEntity(
                title = input.title.trim(),
                flowType = input.flowType,
                categoryId = categoryId,
                amountCents = input.amountCents,
                occurredAt = input.occurredAt,
                notes = input.notes.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clearAllData() {
        dao.deleteAllOccurrences()
        dao.deleteAllOneOffEntries()
        dao.deleteAllCommitments()
        dao.deleteAllCategories()
        seedDefaultCategories()
    }

    suspend fun ensureOccurrencesAroundNow() {
        val current = YearMonth.from(LocalDate.now())
        val months = listOf(current, current.plusMonths(1))
        dao.getActiveCommitments().forEach { commitment ->
            if (commitment.kind == CommitmentKind.RECORRENTE) {
                months.forEach { ensureOccurrence(commitment, it) }
            } else {
                ensureNextOccurrence(commitment)
            }
        }
    }

    suspend fun ensureOccurrencesForMonth(year: Int, month: Int) {
        val target = YearMonth.of(year, month)
        dao.getActiveCommitments().forEach { commitment ->
            ensureOccurrence(commitment, target)
        }
    }

    suspend fun cleanupOldPendingOccurrences() {
        val startOfMonth = DateUtils.millisForDate(LocalDate.now().withDayOfMonth(1))
        dao.deletePendingOccurrencesBefore(startOfMonth)
    }

    suspend fun buildReport(year: Int, month: Int): ReportSnapshot {
        ensureOccurrencesForMonth(year, month)
        val (start, end) = DateUtils.monthBounds(year, month)
        val occurrences = dao.getOccurrencesForPeriod(start, end)
        val oneOffs = dao.getOneOffEntriesForPeriod(start, end)

        val recurringLines = occurrences.map { occurrence ->
            val amount = if (occurrence.status == OccurrenceStatus.FEITO) {
                occurrence.actualCents ?: occurrence.expectedCents
            } else {
                occurrence.expectedCents
            }
            ReportLine(
                title = occurrence.title,
                categoryName = occurrence.categoryName ?: "Sem categoria",
                kind = occurrence.kind,
                purpose = occurrence.purpose,
                flowType = occurrence.flowType,
                amountCents = amount,
                dateMillis = occurrence.completedAt ?: occurrence.dueAt,
                dueAt = occurrence.dueAt,
                completedAt = occurrence.completedAt,
                status = occurrence.status,
                source = "Compromisso",
                notes = occurrence.notes,
            )
        }
        val oneOffLines = oneOffs.map { entry ->
            ReportLine(
                title = entry.title,
                categoryName = entry.categoryName ?: "Sem categoria",
                kind = CommitmentKind.UNICO,
                purpose = CommitmentPurpose.PAYMENT,
                flowType = entry.flowType,
                amountCents = entry.amountCents,
                dateMillis = entry.occurredAt,
                dueAt = entry.occurredAt,
                completedAt = entry.occurredAt,
                status = null,
                source = "Avulso",
                notes = entry.notes,
            )
        }
        val completedLines = recurringLines.filter { it.status == OccurrenceStatus.FEITO } + oneOffLines
        val pendingLines = recurringLines.filter { it.status == OccurrenceStatus.PENDENTE }
        val income = completedLines.filter { it.flowType == FlowType.ENTRADA }.sumOf { it.amountCents }
        val expenses = completedLines.filter { it.flowType == FlowType.SAIDA }.sumOf { it.amountCents }
        val now = System.currentTimeMillis()
        val overdueLines = pendingLines.filter { it.dateMillis < now }
        val openLines = pendingLines.filter { it.dateMillis >= now }
        val pending = pendingLines.sumOf { it.amountCents }
        val paidCount = completedLines.count { it.purpose != CommitmentPurpose.TASK }
        val taskCount = recurringLines.count { it.purpose == CommitmentPurpose.TASK }
        val categoryTotals = completedLines
            .groupBy { it.categoryName to it.flowType }
            .map { (key, lines) ->
                CategoryTotal(
                    categoryName = key.first,
                    flowType = key.second,
                    totalCents = lines.sumOf { it.amountCents },
                )
            }
            .sortedWith(compareBy<CategoryTotal> { it.flowType.name }.thenByDescending { it.totalCents })

        return ReportSnapshot(
            year = year,
            month = month,
            incomeCents = income,
            expenseCents = expenses,
            pendingCents = pending,
            openCents = openLines.sumOf { it.amountCents },
            overdueCents = overdueLines.sumOf { it.amountCents },
            paidCount = paidCount,
            taskCount = taskCount,
            completedCount = completedLines.size,
            pendingCount = pendingLines.size,
            openCount = openLines.size,
            overdueCount = overdueLines.size,
            categoryTotals = categoryTotals,
            lines = (completedLines + pendingLines).sortedBy { it.dateMillis },
        )
    }

    suspend fun dueOccurrencesForMonth(year: Int, month: Int): List<OccurrenceView> {
        ensureOccurrencesForMonth(year, month)
        val (start, end) = DateUtils.monthBounds(year, month)
        return dao.getOccurrencesDueForPeriod(start, end)
    }

    private suspend fun findOrCreateCategory(rawName: String): Long? {
        val name = rawName.trim().ifBlank { "Outros" }
        dao.findCategoryByName(name)?.let { return it.id }
        val inserted = dao.insertCategory(CategoryEntity(name = name))
        if (inserted > 0) return inserted
        return dao.findCategoryByName(name)?.id
    }

    private fun effectiveStartDate(input: CommitmentInput): LocalDate {
        if (input.kind == CommitmentKind.UNICO) return input.dueDate
        if (input.createRetroactive || !input.dueDate.isBefore(LocalDate.now())) return input.dueDate
        val today = LocalDate.now()
        val candidate = DateUtils.dueDate(today.year, today.monthValue, input.dueDate.dayOfMonth)
        return if (candidate.isBefore(today)) candidate.plusMonths(1) else candidate
    }

    private fun effectiveEndDate(input: CommitmentInput, start: LocalDate): LocalDate? {
        if (input.kind == CommitmentKind.UNICO) return null
        val rawEnd = input.endDate ?: start.plusMonths(11)
        if (rawEnd.isBefore(start)) return start
        val months = ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(rawEnd)) + 1
        return if (months > 12) YearMonth.from(start).plusMonths(11).atEndOfMonth() else rawEnd
    }

    private fun effectivePurpose(input: CommitmentInput): CommitmentPurpose {
        val category = input.categoryName.trim()
        return if (category.equals("Emitir Nota Fiscal", ignoreCase = true) ||
            category.equals("Nota Fiscal", ignoreCase = true) ||
            category.equals("Nota fiscal", ignoreCase = true)
        ) {
            CommitmentPurpose.TASK
        } else {
            input.purpose
        }
    }

    private suspend fun ensureNextOccurrence(commitment: RecurringCommitmentEntity) {
        val start = commitment.startLocalDate()
        if (commitment.kind == CommitmentKind.UNICO) {
            ensureOccurrence(commitment, YearMonth.from(start))
            return
        }
        val today = LocalDate.now()
        val current = YearMonth.from(today)
        val next = current.plusMonths(1)
        val candidate = DateUtils.dueDate(current.year, current.monthValue, commitment.dueDay)
        ensureOccurrence(commitment, if (candidate.isBefore(today)) next else current)
    }

    private suspend fun ensureOccurrencesForRange(
        commitment: RecurringCommitmentEntity,
        start: LocalDate,
        end: LocalDate,
    ) {
        var month = YearMonth.from(start)
        val last = YearMonth.from(end)
        while (!month.isAfter(last)) {
            ensureOccurrence(commitment, month)
            month = month.plusMonths(1)
        }
    }

    private suspend fun ensureOccurrence(commitment: RecurringCommitmentEntity, month: YearMonth) {
        if (!commitment.shouldCreateFor(month)) return
        val dueAt = DateUtils.dueAtMillis(
            year = month.year,
            month = month.monthValue,
            dueDay = commitment.dueDay,
            hour = commitment.reminderHour,
            minute = commitment.reminderMinute,
        )
        val existing = dao.getOccurrence(commitment.id, month.year, month.monthValue)
        if (existing == null) {
            dao.insertOccurrence(
                MonthlyOccurrenceEntity(
                    commitmentId = commitment.id,
                    year = month.year,
                    month = month.monthValue,
                    dueAt = dueAt,
                    expectedCents = commitment.expectedCents,
                    reminderSilencedUntil = null,
                    reminderStoppedAt = null,
                ),
            )
            return
        }
        if (existing.status == OccurrenceStatus.PENDENTE) {
            dao.updateOccurrence(
                existing.copy(
                    dueAt = dueAt,
                    expectedCents = commitment.expectedCents,
                    reminderStoppedAt = null,
                ),
            )
        }
    }
}

private fun MonthlyOccurrenceEntity.updatedFromInput(
    commitment: RecurringCommitmentEntity,
    input: CommitmentInput,
): MonthlyOccurrenceEntity {
    val month = YearMonth.of(year, month)
    val dueAt = DateUtils.dueAtMillis(
        year = month.year,
        month = month.monthValue,
        dueDay = input.dueDate.dayOfMonth,
        hour = input.reminderHour,
        minute = input.reminderMinute,
    )
    val expected = input.expectedCents ?: 0
    return copy(
        dueAt = dueAt,
        expectedCents = expected,
        actualCents = if (status == OccurrenceStatus.FEITO) expected else actualCents,
        reminderStoppedAt = null,
        notes = input.notes.trim(),
    )
}

private fun maxIntervalFor(unit: ReminderRepeatUnit): Int = when (unit) {
    ReminderRepeatUnit.MINUTE -> 59
    ReminderRepeatUnit.HOUR -> 23
    ReminderRepeatUnit.DAY -> 30
}

private fun RecurringCommitmentEntity.startLocalDate(): LocalDate =
    if (startDate > 0) DateUtils.millisToLocalDate(startDate) else LocalDate.now()

private fun RecurringCommitmentEntity.endLocalDate(): LocalDate? =
    endDate?.let(DateUtils::millisToLocalDate)

private fun RecurringCommitmentEntity.shouldCreateFor(month: YearMonth): Boolean {
    val start = YearMonth.from(startLocalDate())
    if (month.isBefore(start)) return false
    val end = endLocalDate()?.let(YearMonth::from)
    if (end != null && month.isAfter(end)) return false
    if (kind == CommitmentKind.UNICO) {
        return month == start
    }
    return true
}
