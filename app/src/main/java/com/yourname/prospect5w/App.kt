package com.yourname.prospect5w

import android.app.Application
import com.yourname.prospect5w.data.ProspectDb
import com.yourname.prospect5w.data.Repository

class App : Application() {
    val db by lazy { ProspectDb.get(this) }
    val repo by lazy { Repository(db.dao()) }
}
