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
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PlannerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sportfamily.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        /** v0.8: znacznik importu planu. Dane użytkownika zostają. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN importTag TEXT")
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
