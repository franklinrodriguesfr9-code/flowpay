package com.flowpay.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FlowType {
    ENTRADA,
    SAIDA,
}

enum class OccurrenceStatus {
    PENDENTE,
    FEITO,
    CANCELADO,
}

enum class CommitmentKind {
    UNICO,
    RECORRENTE,
}

enum class CommitmentPurpose {
    PAYMENT,
    TASK,
}

enum class ReminderRepeatUnit {
    MINUTE,
    HOUR,
    DAY,
}

enum class NotificationDismissPolicy {
    KEEP_UNTIL_DONE,
    STOP_ON_DISMISS,
}

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "commitments",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class RecurringCommitmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val kind: CommitmentKind = CommitmentKind.RECORRENTE,
    val purpose: CommitmentPurpose = CommitmentPurpose.PAYMENT,
    val flowType: FlowType,
    val categoryId: Long?,
    val dueDay: Int,
    val reminderHour: Int,
    val reminderMinute: Int,
    val expectedCents: Long,
    val variableAmount: Boolean = false,
    val startDate: Long = 0,
    val endDate: Long? = null,
    val notificationSoundUri: String? = null,
    val reminderRepeatEnabled: Boolean = true,
    val useGlobalReminderSettings: Boolean = true,
    val reminderRepeatUnit: ReminderRepeatUnit = ReminderRepeatUnit.MINUTE,
    val reminderRepeatInterval: Int = 5,
    val notificationDismissPolicy: NotificationDismissPolicy = NotificationDismissPolicy.KEEP_UNTIL_DONE,
    val notes: String,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurringCommitmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commitmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("commitmentId"),
        Index(value = ["commitmentId", "year", "month"], unique = true),
    ],
)
data class MonthlyOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commitmentId: Long,
    val year: Int,
    val month: Int,
    val dueAt: Long,
    val status: OccurrenceStatus = OccurrenceStatus.PENDENTE,
    val expectedCents: Long,
    val actualCents: Long? = null,
    val completedAt: Long? = null,
    val reminderSilencedUntil: Long? = null,
    val reminderStoppedAt: Long? = null,
    val notes: String = "",
)

@Entity(
    tableName = "one_off_entries",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class OneOffEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val flowType: FlowType,
    val categoryId: Long?,
    val amountCents: Long,
    val occurredAt: Long,
    val notes: String,
    val createdAt: Long,
)

data class CommitmentView(
    val id: Long,
    val title: String,
    val kind: CommitmentKind,
    val purpose: CommitmentPurpose,
    val flowType: FlowType,
    val categoryName: String?,
    val dueDay: Int,
    val reminderHour: Int,
    val reminderMinute: Int,
    val expectedCents: Long,
    val variableAmount: Boolean,
    val startDate: Long,
    val endDate: Long?,
    val notificationSoundUri: String?,
    val reminderRepeatEnabled: Boolean,
    val useGlobalReminderSettings: Boolean,
    val reminderRepeatUnit: ReminderRepeatUnit,
    val reminderRepeatInterval: Int,
    val notificationDismissPolicy: NotificationDismissPolicy,
    val notes: String,
    val active: Boolean,
)

data class OccurrenceView(
    val occurrenceId: Long,
    val commitmentId: Long,
    val title: String,
    val kind: CommitmentKind,
    val purpose: CommitmentPurpose,
    val categoryName: String?,
    val flowType: FlowType,
    val dueAt: Long,
    val expectedCents: Long,
    val actualCents: Long?,
    val completedAt: Long?,
    val status: OccurrenceStatus,
    val notes: String,
)

data class OneOffEntryView(
    val entryId: Long,
    val title: String,
    val categoryName: String?,
    val flowType: FlowType,
    val amountCents: Long,
    val occurredAt: Long,
    val notes: String,
)

data class ScheduleItem(
    val occurrenceId: Long,
    val commitmentId: Long,
    val title: String,
    val kind: CommitmentKind,
    val purpose: CommitmentPurpose,
    val flowType: FlowType,
    val categoryName: String?,
    val dueAt: Long,
    val expectedCents: Long,
    val reminderHour: Int,
    val reminderMinute: Int,
    val notificationSoundUri: String?,
    val reminderRepeatEnabled: Boolean,
    val useGlobalReminderSettings: Boolean,
    val reminderRepeatUnit: ReminderRepeatUnit,
    val reminderRepeatInterval: Int,
    val notificationDismissPolicy: NotificationDismissPolicy,
    val reminderSilencedUntil: Long?,
    val reminderStoppedAt: Long?,
    val status: OccurrenceStatus,
)
