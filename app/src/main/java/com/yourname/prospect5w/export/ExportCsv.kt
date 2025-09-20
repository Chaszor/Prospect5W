package com.yourname.prospect5w.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.yourname.prospect5w.data.Interaction
import java.io.File

private fun csv(field: String?): String {
    // CSV escape per RFC 4180: wrap in quotes and double any existing quotes
    val safe = field?.replace("\"", "\"\"") ?: ""
    return "\"$safe\""
}

fun exportInteractions(ctx: Context, list: List<Interaction>) {
    val file = File(ctx.cacheDir, "prospects_${System.currentTimeMillis()}.csv")
    file.printWriter().use { out ->
        out.println("when,what,notes,where,why,next_follow_up")
        for (i in list) {
            val line = buildString {
                append(i.whenAt)                   // epoch millis
                append(',')
                append(csv(i.whatType))
                append(',')
                append(csv(i.whatNotes))
                append(',')
                append(csv(i.whereText))
                append(',')
                append(csv(i.whySummary))
                append(',')
                append(i.nextFollowUpAt?.toString() ?: "")
            }
            out.println(line)
        }
    }

    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(share, "Share CSV"))
}
