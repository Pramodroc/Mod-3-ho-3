package project.case1.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            val fileName = getFileName(contentResolver, uri)
            val cacheFile = File(context.cacheDir, fileName)

            val outputStream = FileOutputStream(cacheFile)
            inputStream.copyTo(outputStream)

            outputStream.close()
            inputStream.close()

            cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        }

        if (name == null) {
            val path = uri.path
            val cut = path?.lastIndexOf('/') ?: -1
            if ((cut != -1) && (path != null)) {
                name = path.substring(cut + 1)
            }
        }

        if (name.isNullOrBlank() || !name.contains(".")) {
            val mimeType = contentResolver.getType(uri)
            val ext = if (mimeType != null) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "pdf"
            } else {
                "pdf"
            }
            name = "document_${System.currentTimeMillis()}.$ext"
        }

        return name
    }
}
