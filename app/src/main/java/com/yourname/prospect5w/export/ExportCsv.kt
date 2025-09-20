package com.yourname.prospect5w.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.yourname.prospect5w.data.Interaction
import java.io.File

/**
 * Helper to escape CSV fields per RFC 4180.  Wraps non-null values in quotes and
 * doubles any existing quote characters.  Nulls become an empty quoted string.
 */
private fun csv(field: String?): String {
    val safe = field?.replace("\"", "\"\"") ?: ""
    return "\"$safe\""
}

/**
 * Export a list of interactions to a temporary CSV file and launch a share intent.
 *
 * The CSV has the header line: when,what,notes,where,why,next_follow_up
 * Each record is on its own line and uses comma separators.
 * Quotes within fields are escaped by doubling them.
 */
fun exportInteractions(ctx: Context, list: List<Interaction>) {
    // Create a temporary CSV file in the cache directory
    val file = File(ctx.cacheDir, "prospects_${'$'}{System.currentTimeMillis()}.csv")
    file.printWriter().use { out ->
        // Write header
        out.println("when,what,notes,where,why,next_follow_up")
        // Write each interaction record
        for (i in list) {
            val line = buildString {
                append(i.whenAt)                  // epoch millis
                append(',')
                append(csv(i.whatType))
                append(',')
                append(csv(i.whatNotes))
                append(',')
                append(csv(i.whereText))
                append(',')
                append(csv(i.whySummary))
                append(',')
                append(csv(i.nextFollowUpAt?.toString()))
            }
            out.println(line)
        }
    }

    // Prepare a share intent for the CSV file
    val uri = FileProvider.getUriForFile(ctx, "${'$'}{ctx.packageName}.files", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(share, "Share CSV"))
}