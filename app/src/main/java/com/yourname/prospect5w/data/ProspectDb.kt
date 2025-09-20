package com.yourname.prospect5w.data

import android.content.Context
import androidx.room.*

@Database(entities = [Contact::class, Company::class, Interaction::class], version = 1)
abstract class ProspectDb : RoomDatabase() {
    abstract fun dao(): ProspectDao

    companion object {
        @Volatile private var INSTANCE: ProspectDb? = null
        fun get(context: Context): ProspectDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ProspectDb::class.java,
                    "prospect.db"
                ).build().also { INSTANCE = it }
            }
    }
}
