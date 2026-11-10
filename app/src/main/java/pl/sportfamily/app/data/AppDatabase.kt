package pl.sportfamily.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Child::class, Person::class, EventEntity::class, EventChild::class, EventException::class,
        ChecklistItem::class, ChecklistCheck::class, TaskEntity::class, SchoolDayOff::class,
        EventDayRule::class, OccurrenceOverride::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PlannerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sportfamily.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()

        /** v0.8: znacznik importu planu. Dane użytkownika zostają. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN importTag TEXT")
            }
        }

        /** v0.11: godziny i osoby osobno dla dni tygodnia oraz zmiany pojedynczych terminów. */
        /** v0.13: trener (imię i telefon) w wydarzeniu. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN coachName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE events ADD COLUMN coachPhone TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v0.12: pole "Zabierz też" w wydarzeniu i w zmianie pojedynczego terminu. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN alsoTake TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE occurrence_overrides ADD COLUMN alsoTake TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `event_day_rules` (`eventId` INTEGER NOT NULL, `weekday` INTEGER NOT NULL, `startMinute` INTEGER, `endMinute` INTEGER, `dropOffPersonId` INTEGER, `pickUpPersonId` INTEGER, PRIMARY KEY(`eventId`, `weekday`), FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `occurrence_overrides` (`eventId` INTEGER NOT NULL, `day` INTEGER NOT NULL, `startMinute` INTEGER, `endMinute` INTEGER, `dropOffPersonId` INTEGER, `pickUpPersonId` INTEGER, PRIMARY KEY(`eventId`, `day`), FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        /** v0.10: powtarzanie co N tygodni i telefon osoby. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN repeatEveryWeeks INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE persons ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v0.9: dni wolne od lekcji dodane ręcznie. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `school_days_off` (`day` INTEGER NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`day`))")
            }
        }
    }
}
