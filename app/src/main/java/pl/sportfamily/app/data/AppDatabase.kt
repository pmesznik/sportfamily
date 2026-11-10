package pl.sportfamily.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Child::class, Person::class, EventEntity::class, EventChild::class, EventException::class,
        ChecklistItem::class, ChecklistCheck::class, TaskEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PlannerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sportfamily.db").build()
    }
}
