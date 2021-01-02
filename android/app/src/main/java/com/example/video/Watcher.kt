package com.example.video

import android.util.Log
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

fun makePaddedFileName(id: String): String {
    // TODO: handle case where count is high / > 10 digits
    val maxLenth = 10
    val currentLength = id.length
    val diff = maxLenth - currentLength
    val zeroes = (0 until diff).map { i -> 0 }.joinToString("")
    val newName = "$zeroes$id.jpg"
    Log.e("pad", newName)
    return newName
}

fun fileIdFromFileName(fileName: String): Int {
    val idString = fileName.split(".jpg")[0]
    val id = idString.toInt()
    return id
}

fun fileIdToFileName(id: String): String {
    return makePaddedFileName(id)
}

fun pack(files: List<File>, outFileName: String) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outFileName))).use { out ->
        for (file in files) {
            FileInputStream(file).use { fi ->
                BufferedInputStream(fi).use { origin ->
                    val entry = ZipEntry(file.name)
                    out.putNextEntry(entry)
                    origin.copyTo(out, 1024)
                }
            }
        }
    }
}


fun deleteDir(folder: String, fileName: String) {
    val dir = File(folder, fileName)
    if (dir.isDirectory()) {
        for (child in dir.listFiles()) {
            child.delete()
        }
    }
    dir.delete()
}

fun copyFile(srcFolder: String, srcFile: String, targetFolder: String, targetFile: String) {
    File(srcFolder, srcFile)
        .copyTo(File(targetFolder, targetFile), true)
}

fun mkdir(path: String): File {
    val dir = File(path)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

object fileNameComparator: Comparator<String> {
    override fun compare(p0: String?, p1: String?): Int {
        val f1 = p0?.split(".jpg")?.get(0)?.toInt() as Int
        val f2 = p0?.split(".jpg")?.get(0)?.toInt() as Int
        if (f1 < f2) return -1
        else if (f1 > f2) return 1
        else return 0
    }
}

class Batch {
    var id: String = ""
    var status: String = "new"
    var fileNames: List<String> = listOf<String>()
    constructor(id: String, fileNames: List<String>) {
        this.id = id
        this.fileNames = fileNames
    }

    override fun toString(): String {
        return super.toString()
    }
}

class Watcher(
    private val folderPath: String,
    private val sessionId: String
) {
    val TAG = "Watcher - "
    val batches: HashMap<String, Batch> = hashMapOf<String, Batch>()
    var lastFileIndex: Int = 0
    val filesPerBatch: Int = 50
    val queue: Queue<Batch> = ArrayDeque<Batch>()
    var imagesDir: File
    var batchesDir: File
    var zippedDir: File
    var folder: File
    var hasRunCheck: Boolean = false

    init {
        val imagesDirName = "images"
        val batchesDirName = "batches"
        val zippedDirName = "zipped"
        folder = mkdir(folderPath)
        imagesDir = mkdir(folder.absolutePath + "/" + imagesDirName)
        batchesDir = mkdir(folder.absolutePath + "/" + batchesDirName)
        zippedDir = mkdir(folder.absolutePath + "/" + zippedDirName)
    }

   fun checkForFiles(): Unit{
       if (!hasRunCheck) {
           hasRunCheck = true
           return
       }
       val imagesAvailable = imagesDir.listFiles()
       if (imagesAvailable.isEmpty()) {
           return
       }
       // Check for the newest file currently available in images dir.
       // Get diff between index of last file processed and newest file.
       // For all batches possible, create the batch
       // TODO: handle case where recording has terminated and there
       // are unprocessed images but not enough to create a batch
       // of size `filesPerBatch`
       val newestFile = imagesDir.listFiles().last()
       val newestFileIndex = fileIdFromFileName(newestFile.name)
       val batchesPossible = (newestFileIndex - lastFileIndex) / filesPerBatch
       for (i in 0 until batchesPossible) {
           val endIndex = lastFileIndex + filesPerBatch
           Log.e("== last file in", lastFileIndex.toString())
           Log.e("== check end ind", endIndex.toString())
           val nextFiles = mutableListOf<String>()
           for (i in lastFileIndex until endIndex) {
               val fileName = fileIdToFileName(i.toString())
               nextFiles.add(fileName)
           }
           val batchId = batches.keys.size.toString()
           val batch = Batch(batchId, nextFiles)
           batches.put(batchId, batch)
           queue.add(batch)
           lastFileIndex = endIndex
       }
    }

//    fun enqueueBatches() {
//        val allNewBatches = batches.values.filter { batch ->  batch.status == "new"}
//        Log.e("all new batch", allNewBatches.toString())
//        if (allNewBatches.isEmpty()) {
//            Log.e("enqueueBatches", "no batches yet")
//            return
//        }
//
//        allNewBatches.forEach { batch -> run {
//            batch.status = "enqueued"
//            queue.add(batch)
//        } }
//
//    }

    fun deleteBatchFiles(batch: Batch) {
        batch.fileNames.forEach {fileName -> run {
            val file = File(imagesDir.absolutePath, fileName)
            if (file.exists()) {
                file.delete()
            }
        }}
    }

    fun processBatch(batch: Batch) {
        Log.e("processBatch", batch.id)
        Log.e("processBatch", batch.fileNames.toString())
        batch.status = "processing"
        val zipFilePath = zippedDir.absolutePath + "/" + batch.id + ".zip"
        val filesToZip = batch.fileNames.map { fileName -> File(imagesDir.absolutePath, fileName) }
        pack(filesToZip, zipFilePath)
        uploadFile(sessionId, File(zipFilePath), batch.id + ".zip")
        deleteBatchFiles(batch)
        batch.status = "finished"
    }

    fun processQueue() {
        val batchesAtATime = 3
        Log.e("processQueue all b", queue.toList().toString())

        for (i in 0 until batchesAtATime) {
            var batch: Batch? = null
            Log.e("processQueue loop", queue.toList().toString())
            try {
                batch = queue.remove()
            } catch(e: NoSuchElementException) {

            }
            if (batch == null) {
                Log.e("processQueue", "no batch in queue")
                return
            }
            else {
                Log.e("processQueue start b", batch.id)
                processBatch(batch)
            }
        }
    }

}