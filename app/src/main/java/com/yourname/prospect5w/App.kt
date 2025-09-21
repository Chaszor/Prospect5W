package com.yourname.prospect5w

import android.app.Application
import com.yourname.prospect5w.data.ProspectDb

/**
 * Application class that holds a singleton instance of the [ProspectDb].
 * The database is created lazily on first access to avoid performing
 * work before it is needed.
 */
class App : Application() {
    val db: ProspectDb by lazy { ProspectDb.get(this) }
}
