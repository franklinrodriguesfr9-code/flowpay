package com.flowpay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlowPayDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun findCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM commitments WHERE active = 1 ORDER BY startDate, dueDay, reminderHour, reminderMinute, title")
    suspend fun getActiveCommitments(): List<RecurringCommitmentEntity>

    @Query("SELECT * FROM commitments ORDER BY id")
    suspend fun getAllCommitments(): List<RecurringCommitmentEntity>

    @Query("SELECT * FROM commitments WHERE id = :id LIMIT 1")
    suspend fun getCommitment(id: Long): RecurringCommitmentEntity?

    @Query(
        """
        SELECT c.id, c.title, c.kind, c.purpose, c.flowType, cat.name AS categoryName, c.dueDay,
               c.reminderHour, c.reminderMinute, c.expectedCents, c.variableAmount,
               c.startDate, c.endDate, c.notificationSoundUri, c.reminderRepeatEnabled,
               c.useGlobalReminderSettings, c.reminderRepeatUnit, c.reminderRepeatInterval,
               c.notificationDismissPolicy, c.notes, c.active
        FROM commitments c
        LEFT JOIN categories cat ON cat.id = c.categoryId
        WHERE c.active = 1
        ORDER BY c.startDate, c.dueDay, c.reminderHour, c.reminderMinute, c.title
        """,
    )
    fun observeCommitments(): Flow<List<CommitmentView>>

    @Insert
    suspend fun insertCommitment(commitment: RecurringCommitmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitments(commitments: List<RecurringCommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: RecurringCommitmentEntity)

    @Query("UPDATE commitments SET active = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveCommitment(id: Long, updatedAt: Long)

    @Query("SELECT id FROM occurrences WHERE commitmentId = :commitmentId")
    suspend fun getOccurrenceIdsForCommitment(commitmentId: Long): List<Long>

    @Query("SELECT id FROM occurrences WHERE commitmentId = :commitmentId AND dueAt >= :fromMillis")
    suspend fun getOccurrenceIdsForCommitmentFrom(commitmentId: Long, fromMillis: Long): List<Long>

    @Query("SELECT id FROM occurrences WHERE commitmentId = :commitmentId AND dueAt <= :untilMillis")
    suspend fun getOccurrenceIdsForCommitmentUntil(commitmentId: Long, untilMillis: Long): List<Long>

    @Query("SELECT id FROM occurrences WHERE commitmentId = :commitmentId AND status = 'PENDENTE'")
    suspend fun getPendingOccurrenceIdsForCommitment(commitmentId: Long): List<Long>

    @Query("SELECT id FROM occurrences WHERE commitmentId = :commitmentId AND status = 'PENDENTE' AND dueAt >= :fromMillis")
    suspend fun getPendingOccurrenceIdsForCommitmentFrom(commitmentId: Long, fromMillis: Long): List<Long>

    @Query("DELETE FROM occurrences WHERE commitmentId = :commitmentId AND status = 'PENDENTE'")
    suspend fun deletePendingOccurrencesForCommitment(commitmentId: Long)

    @Query("DELETE FROM occurrences WHERE commitmentId = :commitmentId AND status = 'PENDENTE' AND dueAt >= :fromMillis")
    suspend fun deletePendingOccurrencesForCommitmentFrom(commitmentId: Long, fromMillis: Long)

    @Query("UPDATE occurrences SET status = 'CANCELADO', reminderSilencedUntil = NULL, reminderStoppedAt = :stoppedAt WHERE id = :occurrenceId")
    suspend fun cancelOccurrence(occurrenceId: Long, stoppedAt: Long)

    @Query("UPDATE occurrences SET status = 'CANCELADO', reminderSilencedUntil = NULL, reminderStoppedAt = :stoppedAt WHERE commitmentId = :commitmentId")
    suspend fun cancelOccurrencesForCommitment(commitmentId: Long, stoppedAt: Long)

    @Query("UPDATE occurrences SET status = 'CANCELADO', reminderSilencedUntil = NULL, reminderStoppedAt = :stoppedAt WHERE commitmentId = :commitmentId AND dueAt >= :fromMillis")
    suspend fun cancelOccurrencesForCommitmentFrom(commitmentId: Long, fromMillis: Long, stoppedAt: Long)

    @Query("UPDATE occurrences SET status = 'CANCELADO', reminderSilencedUntil = NULL, reminderStoppedAt = :stoppedAt WHERE commitmentId = :commitmentId AND dueAt <= :untilMillis")
    suspend fun cancelOccurrencesForCommitmentUntil(commitmentId: Long, untilMillis: Long, stoppedAt: Long)

    @Query("UPDATE occurrences SET status = 'CANCELADO', reminderSilencedUntil = NULL, reminderStoppedAt = :stoppedAt WHERE commitmentId = :commitmentId AND status = 'PENDENTE' AND dueAt >= :fromMillis")
    suspend fun cancelPendingOccurrencesForCommitmentFrom(commitmentId: Long, fromMillis: Long, stoppedAt: Long)

    @Query("UPDATE occurrences SET status = 'PENDENTE', actualCents = NULL, completedAt = NULL, reminderSilencedUntil = NULL, reminderStoppedAt = NULL, notes = :notes WHERE id = :occurrenceId")
    suspend fun markOccurrencePending(occurrenceId: Long, notes: String)

    @Query("UPDATE occurrences SET reminderStoppedAt = :stoppedAt, reminderSilencedUntil = NULL WHERE id = :occurrenceId")
    suspend fun stopOccurrenceReminder(occurrenceId: Long, stoppedAt: Long)

    @Deprecated("Use cancelOccurrence(occurrenceId, stoppedAt)")
    @Query("UPDATE occurrences SET status = 'CANCELADO', reminderSilencedUntil = NULL WHERE id = :occurrenceId")
    suspend fun cancelOccurrence(occurrenceId: Long)

    @Deprecated("Use cancelPendingOccurrencesForCommitmentFrom(commitmentId, fromMillis, stoppedAt)")
    @Query("UPDATE occurrences SET status = 'CANCELADO', reminderSilencedUntil = NULL WHERE commitmentId = :commitmentId AND status = 'PENDENTE' AND dueAt >= :fromMillis")
    suspend fun cancelPendingOccurrencesForCommitmentFrom(commitmentId: Long, fromMillis: Long)

    @Query("UPDATE occurrences SET reminderSilencedUntil = :untilMillis WHERE id = :occurrenceId")
    suspend fun silenceOccurrenceUntil(occurrenceId: Long, untilMillis: Long)

    @Query("DELETE FROM occurrences WHERE status = 'PENDENTE' AND dueAt < :beforeMillis")
    suspend fun deletePendingOccurrencesBefore(beforeMillis: Long)

    @Query("SELECT * FROM occurrences WHERE commitmentId = :commitmentId AND year = :year AND month = :month LIMIT 1")
    suspend fun getOccurrence(commitmentId: Long, year: Int, month: Int): MonthlyOccurrenceEntity?

    @Query("SELECT * FROM occurrences WHERE id = :id LIMIT 1")
    suspend fun getOccurrenceById(id: Long): MonthlyOccurrenceEntity?

    @Query("SELECT * FROM occurrences ORDER BY id")
    suspend fun getAllOccurrences(): List<MonthlyOccurrenceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrence(occurrence: MonthlyOccurrenceEntity): Long

    @Update
    suspend fun updateOccurrence(occurrence: MonthlyOccurrenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<MonthlyOccurrenceEntity>)

    @Query(
        """
        SELECT o.id AS occurrenceId, c.id AS commitmentId, c.title, c.kind, c.purpose, cat.name AS categoryName,
               c.flowType, o.dueAt, o.expectedCents, o.actualCents, o.completedAt, o.status, o.notes
        FROM occurrences o
        JOIN commitments c ON c.id = o.commitmentId
        LEFT JOIN categories cat ON cat.id = c.categoryId
        WHERE o.status = 'PENDENTE'
        ORDER BY o.dueAt, c.title
        """,
    )
    fun observePendingOccurrences(): Flow<List<OccurrenceView>>

    @Query(
        """
        SELECT o.id AS occurrenceId, c.id AS commitmentId, c.title, c.kind, c.purpose, cat.name AS categoryName,
               c.flowType, o.dueAt, o.expectedCents, o.actualCents, o.completedAt, o.status, o.notes
        FROM occurrences o
        JOIN commitments c ON c.id = o.commitmentId
        LEFT JOIN categories cat ON cat.id = c.categoryId
        WHERE (o.status = 'PENDENTE' AND o.dueAt BETWEEN :startMillis AND :endMillis)
           OR (o.status = 'FEITO' AND o.completedAt BETWEEN :startMillis AND :endMillis)
        ORDER BY o.dueAt, c.title
        """,
    )
    suspend fun getOccurrencesForPeriod(startMillis: Long, endMillis: Long): List<OccurrenceView>

    @Query(
        """
        SELECT o.id AS occurrenceId, c.id AS commitmentId, c.title, c.kind, c.purpose, cat.name AS categoryName,
               c.flowType, o.dueAt, o.expectedCents, o.actualCents, o.completedAt, o.status, o.notes
        FROM occurrences o
        JOIN commitments c ON c.id = o.commitmentId
        LEFT JOIN categories cat ON cat.id = c.categoryId
        WHERE o.dueAt BETWEEN :startMillis AND :endMillis
          AND o.status != 'CANCELADO'
        ORDER BY o.dueAt, c.title
        """,
    )
    suspend fun getOccurrencesDueForPeriod(startMillis: Long, endMillis: Long): List<OccurrenceView>

    @Query(
        """
        SELECT o.id AS occurrenceId, c.id AS commitmentId, c.title, c.kind, c.purpose, c.flowType,
               cat.name AS categoryName, o.dueAt, o.expectedCents,
               c.reminderHour, c.reminderMinute, c.notificationSoundUri,
               c.reminderRepeatEnabled, c.useGlobalReminderSettings, c.reminderRepeatUnit,
               c.reminderRepeatInterval, c.notificationDismissPolicy,
               o.reminderSilencedUntil, o.reminderStoppedAt, o.status
        FROM occurrences o
        JOIN commitments c ON c.id = o.commitmentId
        LEFT JOIN categories cat ON cat.id = c.categoryId
        WHERE o.status = 'PENDENTE' AND c.active = 1
        """,
    )
    suspend fun getPendingScheduleItems(): List<ScheduleItem>

    @Query(
        """
        SELECT o.id AS occurrenceId, c.id AS commitmentId, c.title, c.kind, c.purpose, c.flowType,
               cat.name AS categoryName, o.dueAt, o.expectedCents,
               c.reminderHour, c.reminderMinute, c.notificationSoundUri,
               c.reminderRepeatEnabled, c.useGlobalReminderSettings, c.reminderRepeatUnit,
               c.reminderRepeatInterval, c.notificationDismissPolicy,
               o.reminderSilencedUntil, o.reminderStoppedAt, o.status
        FROM occurrences o
        JOIN commitments c ON c.id = o.commitmentId
        LEFT JOIN categories cat ON cat.id = c.categoryId
        WHERE o.id = :occurrenceId
        LIMIT 1
        """,
    )
    suspend fun getScheduleItem(occurrenceId: Long): ScheduleItem?

    @Insert
    suspend fun insertOneOffEntry(entry: OneOffEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOneOffEntries(entries: List<OneOffEntryEntity>)

    @Query("SELECT * FROM one_off_entries ORDER BY id")
    suspend fun getAllOneOffEntries(): List<OneOffEntryEntity>

    @Query(
        """
        SELECT e.id AS entryId, e.title, cat.name AS categoryName,
               e.flowType, e.amountCents, e.occurredAt, e.notes
        FROM one_off_entries e
        LEFT JOIN categories cat ON cat.id = e.categoryId
        ORDER BY e.occurredAt DESC
        LIMIT 50
        """,
    )
    fun observeRecentOneOffEntries(): Flow<List<OneOffEntryView>>

    @Query(
        """
        SELECT e.id AS entryId, e.title, cat.name AS categoryName,
               e.flowType, e.amountCents, e.occurredAt, e.notes
        FROM one_off_entries e
        LEFT JOIN categories cat ON cat.id = e.categoryId
        WHERE e.occurredAt BETWEEN :startMillis AND :endMillis
        ORDER BY e.occurredAt, e.title
        """,
    )
    suspend fun getOneOffEntriesForPeriod(startMillis: Long, endMillis: Long): List<OneOffEntryView>

    @Query("DELETE FROM occurrences")
    suspend fun deleteAllOccurrences()

    @Query("DELETE FROM one_off_entries")
    suspend fun deleteAllOneOffEntries()

    @Query("DELETE FROM commitments")
    suspend fun deleteAllCommitments()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
