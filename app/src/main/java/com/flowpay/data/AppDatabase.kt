package com.flowpay.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowpay.util.DateUtils
import java.time.LocalDate

@Database(
    entities = [
        CategoryEntity::class,
        RecurringCommitmentEntity::class,
        MonthlyOccurrenceEntity::class,
        OneOffEntryEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FlowPayDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flowpay.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                val monthStart = DateUtils.millisForDate(LocalDate.now().withDayOfMonth(1))
                db.execSQL("ALTER TABLE commitments ADD COLUMN kind TEXT NOT NULL DEFAULT 'RECORRENTE'")
                db.execSQL("ALTER TABLE commitments ADD COLUMN variableAmount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE commitments ADD COLUMN startDate INTEGER NOT NULL DEFAULT $now")
                db.execSQL("ALTER TABLE commitments ADD COLUMN endDate INTEGER")
                db.execSQL("ALTER TABLE commitments ADD COLUMN notificationSoundUri TEXT")
                db.execSQL("DELETE FROM occurrences WHERE status = 'PENDENTE' AND dueAt < $monthStart")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE commitments ADD COLUMN purpose TEXT NOT NULL DEFAULT 'PAYMENT'")
                db.execSQL("ALTER TABLE commitments ADD COLUMN reminderRepeatEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN reminderSilencedUntil INTEGER")
                db.execSQL("INSERT OR IGNORE INTO categories(name) VALUES('Emitir Nota Fiscal')")
                db.execSQL(
                    """
                    UPDATE commitments
                    SET purpose = 'TASK', flowType = 'ENTRADA'
                    WHERE categoryId IN (
                        SELECT id FROM categories
                        WHERE lower(name) IN (lower('Emitir Nota Fiscal'), lower('Nota Fiscal'), lower('Nota fiscal'))
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE commitments ADD COLUMN useGlobalReminderSettings INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE commitments ADD COLUMN reminderRepeatUnit TEXT NOT NULL DEFAULT 'MINUTE'")
                db.execSQL("ALTER TABLE commitments ADD COLUMN reminderRepeatInterval INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE commitments ADD COLUMN notificationDismissPolicy TEXT NOT NULL DEFAULT 'KEEP_UNTIL_DONE'")
                db.execSQL("ALTER TABLE occurrences ADD COLUMN reminderStoppedAt INTEGER")
            }
        }
    }
}
