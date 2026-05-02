package com.flowpay.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.flowpay.data.AppDatabase
import com.flowpay.data.CategoryEntity
import com.flowpay.data.CommitmentKind
import com.flowpay.data.CommitmentPurpose
import com.flowpay.data.FlowType
import com.flowpay.data.MonthlyOccurrenceEntity
import com.flowpay.data.NotificationDismissPolicy
import com.flowpay.data.OccurrenceStatus
import com.flowpay.data.OneOffEntryEntity
import com.flowpay.data.RecurringCommitmentEntity
import com.flowpay.data.ReminderRepeatUnit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class BackupService(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val dao = database.dao()

    suspend fun shareBackup() {
        val file = writeBackupFile()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, "Exportar backup FlowPay")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    suspend fun importBackup(jsonText: String) {
        val backup = parseBackup(jsonText)
        database.withTransaction {
            dao.deleteAllOccurrences()
            dao.deleteAllOneOffEntries()
            dao.deleteAllCommitments()
            dao.deleteAllCategories()
            dao.insertCategories(backup.categories)
            dao.insertCommitments(backup.commitments)
            dao.insertOccurrences(backup.occurrences)
            dao.insertOneOffEntries(backup.oneOffEntries)
        }
    }

    suspend fun backupJson(): String {
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("categories", dao.getAllCategories().toJsonArray { it.toJson() })
            .put("commitments", dao.getAllCommitments().toJsonArray { it.toJson() })
            .put("occurrences", dao.getAllOccurrences().toJsonArray { it.toJson() })
            .put("oneOffEntries", dao.getAllOneOffEntries().toJsonArray { it.toJson() })
        return root.toString(2)
    }

    private suspend fun writeBackupFile(): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "flowpay_backup_${System.currentTimeMillis()}.flowpay.json")
        file.writeText(backupJson(), Charsets.UTF_8)
        return file
    }

    private fun parseBackup(jsonText: String): BackupPayload {
        try {
            val root = JSONObject(jsonText)
            val version = root.optInt("schemaVersion", -1)
            require(version in 1..SCHEMA_VERSION) { "Versao de backup nao suportada: $version" }
            return BackupPayload(
                categories = root.getJSONArray("categories").mapObjects { it.toCategory() },
                commitments = root.getJSONArray("commitments").mapObjects { it.toCommitment() },
                occurrences = root.getJSONArray("occurrences").mapObjects { it.toOccurrence() },
                oneOffEntries = root.getJSONArray("oneOffEntries").mapObjects { it.toOneOffEntry() },
            )
        } catch (error: JSONException) {
            throw IllegalArgumentException("Arquivo de backup invalido", error)
        }
    }

    private data class BackupPayload(
        val categories: List<CategoryEntity>,
        val commitments: List<RecurringCommitmentEntity>,
        val occurrences: List<MonthlyOccurrenceEntity>,
        val oneOffEntries: List<OneOffEntryEntity>,
    )

    companion object {
        const val SCHEMA_VERSION = 4
    }
}

private fun CategoryEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)

private fun JSONObject.toCategory(): CategoryEntity = CategoryEntity(
    id = getLong("id"),
    name = getString("name"),
)

private fun RecurringCommitmentEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("kind", kind.name)
    .put("purpose", purpose.name)
    .put("flowType", flowType.name)
    .putNullable("categoryId", categoryId)
    .put("dueDay", dueDay)
    .put("reminderHour", reminderHour)
    .put("reminderMinute", reminderMinute)
    .put("expectedCents", expectedCents)
    .put("variableAmount", variableAmount)
    .put("startDate", startDate)
    .putNullable("endDate", endDate)
    .putNullableString("notificationSoundUri", notificationSoundUri)
    .put("reminderRepeatEnabled", reminderRepeatEnabled)
    .put("useGlobalReminderSettings", useGlobalReminderSettings)
    .put("reminderRepeatUnit", reminderRepeatUnit.name)
    .put("reminderRepeatInterval", reminderRepeatInterval)
    .put("notificationDismissPolicy", notificationDismissPolicy.name)
    .put("notes", notes)
    .put("active", active)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun JSONObject.toCommitment(): RecurringCommitmentEntity = RecurringCommitmentEntity(
    id = getLong("id"),
    title = getString("title"),
    kind = CommitmentKind.valueOf(optString("kind", CommitmentKind.RECORRENTE.name)),
    purpose = CommitmentPurpose.valueOf(optString("purpose", CommitmentPurpose.PAYMENT.name)),
    flowType = FlowType.valueOf(getString("flowType")),
    categoryId = optNullableLong("categoryId"),
    dueDay = getInt("dueDay"),
    reminderHour = getInt("reminderHour"),
    reminderMinute = getInt("reminderMinute"),
    expectedCents = getLong("expectedCents"),
    variableAmount = optBoolean("variableAmount", false),
    startDate = optLong("startDate", 0),
    endDate = optNullableLong("endDate"),
    notificationSoundUri = optNullableString("notificationSoundUri"),
    reminderRepeatEnabled = optBoolean("reminderRepeatEnabled", true),
    useGlobalReminderSettings = optBoolean("useGlobalReminderSettings", true),
    reminderRepeatUnit = ReminderRepeatUnit.valueOf(optString("reminderRepeatUnit", ReminderRepeatUnit.MINUTE.name)),
    reminderRepeatInterval = optInt("reminderRepeatInterval", 5),
    notificationDismissPolicy = NotificationDismissPolicy.valueOf(
        optString("notificationDismissPolicy", NotificationDismissPolicy.KEEP_UNTIL_DONE.name),
    ),
    notes = optString("notes", ""),
    active = optBoolean("active", true),
    createdAt = getLong("createdAt"),
    updatedAt = getLong("updatedAt"),
)

private fun MonthlyOccurrenceEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("commitmentId", commitmentId)
    .put("year", year)
    .put("month", month)
    .put("dueAt", dueAt)
    .put("status", status.name)
    .put("expectedCents", expectedCents)
    .putNullable("actualCents", actualCents)
    .putNullable("completedAt", completedAt)
    .putNullable("reminderSilencedUntil", reminderSilencedUntil)
    .putNullable("reminderStoppedAt", reminderStoppedAt)
    .put("notes", notes)

private fun JSONObject.toOccurrence(): MonthlyOccurrenceEntity = MonthlyOccurrenceEntity(
    id = getLong("id"),
    commitmentId = getLong("commitmentId"),
    year = getInt("year"),
    month = getInt("month"),
    dueAt = getLong("dueAt"),
    status = OccurrenceStatus.valueOf(getString("status")),
    expectedCents = getLong("expectedCents"),
    actualCents = optNullableLong("actualCents"),
    completedAt = optNullableLong("completedAt"),
    reminderSilencedUntil = optNullableLong("reminderSilencedUntil"),
    reminderStoppedAt = optNullableLong("reminderStoppedAt"),
    notes = optString("notes", ""),
)

private fun OneOffEntryEntity.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("flowType", flowType.name)
    .putNullable("categoryId", categoryId)
    .put("amountCents", amountCents)
    .put("occurredAt", occurredAt)
    .put("notes", notes)
    .put("createdAt", createdAt)

private fun JSONObject.toOneOffEntry(): OneOffEntryEntity = OneOffEntryEntity(
    id = getLong("id"),
    title = getString("title"),
    flowType = FlowType.valueOf(getString("flowType")),
    categoryId = optNullableLong("categoryId"),
    amountCents = getLong("amountCents"),
    occurredAt = getLong("occurredAt"),
    notes = optString("notes", ""),
    createdAt = getLong("createdAt"),
)

private fun JSONObject.putNullable(name: String, value: Long?): JSONObject {
    if (value == null) {
        put(name, JSONObject.NULL)
    } else {
        put(name, value)
    }
    return this
}

private fun JSONObject.putNullableString(name: String, value: String?): JSONObject {
    if (value == null) {
        put(name, JSONObject.NULL)
    } else {
        put(name, value)
    }
    return this
}

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) getLong(name) else null

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) getString(name) else null

private fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray {
    val array = JSONArray()
    forEach { array.put(transform(it)) }
    return array
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
    val values = mutableListOf<T>()
    for (index in 0 until length()) {
        values += transform(getJSONObject(index))
    }
    return values
}
