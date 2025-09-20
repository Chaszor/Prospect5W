package com.yourname.prospect5w.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.yourname.prospect5w.data.Interaction
import java.io.File

fun exportInteractions(ctx: Context, list: List<Interaction>) {
    val f = File(ctx.cacheDir, "prospects_${'$'}{System.currentTimeMillis()}.csv")
    f.printWriter().use { out ->
        out.println("when,what,notes,where,why,next_follow_up")
        list.forEach { i ->
            out.println("${'$'}{i.whenAt},${'$'}{i.whatType},"${'$'}{i.whatNotes}","${'$'}{i.whereText ?: ""}","${'$'}{i.whySummary}",${'$'}{i.nextFollowUpAt ?: ""}")
        }
    }
    val uri = FileProvider.getUriForFile(ctx, "${'$'}{ctx.packageName}.files", f)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(share, "Share CSV"))
}
