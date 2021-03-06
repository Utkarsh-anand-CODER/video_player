package com.example.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.lang.Exception

data class Video(val id:String, var title:String, val duration: Long=0, val folderName:String,
                 val size:String, var path:String, var artUri: Uri)

data class Folder(val id:String,val folderName: String)
@SuppressLint("InlinedApi", "Recycle")
fun getAllVideos(context: Context): ArrayList<Video> {
    val SortEditor = context.getSharedPreferences("Sorting", AppCompatActivity.MODE_PRIVATE)
    MainActivity.sortValue =SortEditor.getInt("sortValue", 0)
    val tempfolderList = ArrayList<String>()
    val tempList = ArrayList<Video>()
    val projection = arrayOf(
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.BUCKET_ID
    )
    val cursor = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection, null, null, MainActivity.sortList[MainActivity.sortValue]
    )

    if (cursor != null)
        if (cursor.moveToNext())
            do {
                val titleC =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE))
                val idC =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val folderC =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val folderIdC =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                val sizeC =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                val pathC =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                val durationC =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                        .toLong()
                try {
                    val file = File(pathC)
                    val artUriC = Uri.fromFile(file)
                    val video = Video(
                        title = titleC,
                        id = idC,
                        folderName = folderC,
                        duration = durationC,
                        size = sizeC,
                        path = pathC,
                        artUri = artUriC
                    )
                    if (file.exists()) tempList.add(video)
                    if (!tempfolderList.contains(folderC)) {
                        tempfolderList.add(folderC)
                        MainActivity.folderList.add(Folder(id = folderIdC, folderName = folderC))
                    }
                } catch (e: Exception) {
                }
            } while (cursor.moveToNext())
    cursor?.close()
    return tempList

}