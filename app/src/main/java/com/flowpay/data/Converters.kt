package com.flowpay.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toFlowType(value: String?): FlowType? = value?.let(FlowType::valueOf)

    @TypeConverter
    fun fromFlowType(value: FlowType?): String? = value?.name

    @TypeConverter
    fun toOccurrenceStatus(value: String?): OccurrenceStatus? = value?.let(OccurrenceStatus::valueOf)

    @TypeConverter
    fun fromOccurrenceStatus(value: OccurrenceStatus?): String? = value?.name

    @TypeConverter
    fun toCommitmentKind(value: String?): CommitmentKind? = value?.let(CommitmentKind::valueOf)

    @TypeConverter
    fun fromCommitmentKind(value: CommitmentKind?): String? = value?.name

    @TypeConverter
    fun toCommitmentPurpose(value: String?): CommitmentPurpose? = value?.let(CommitmentPurpose::valueOf)

    @TypeConverter
    fun fromCommitmentPurpose(value: CommitmentPurpose?): String? = value?.name

    @TypeConverter
    fun toReminderRepeatUnit(value: String?): ReminderRepeatUnit? = value?.let(ReminderRepeatUnit::valueOf)

    @TypeConverter
    fun fromReminderRepeatUnit(value: ReminderRepeatUnit?): String? = value?.name

    @TypeConverter
    fun toNotificationDismissPolicy(value: String?): NotificationDismissPolicy? = value?.let(NotificationDismissPolicy::valueOf)

    @TypeConverter
    fun fromNotificationDismissPolicy(value: NotificationDismissPolicy?): String? = value?.name
}
