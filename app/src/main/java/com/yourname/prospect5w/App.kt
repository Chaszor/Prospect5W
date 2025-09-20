package com.yourname.prospect5w

import android.app.Application
import com.yourname.prospect5w.data.ProspectDb

class App : Application() {
    val db by lazy { ProspectDb.get(this) }
}
