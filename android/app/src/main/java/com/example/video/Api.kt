package com.example.video

import android.util.Log
import android.widget.Toast
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


fun uploadFile(sessionId: String, sourceFile: File, fileName: String): Int {
    //val endpointUrl = "http://localhost:5000/upload/$sessionId"
    val endpointUrl = "http://192.168.1.7:5000/upload/$sessionId"
    var conn: HttpURLConnection? = null
    var dos: DataOutputStream? = null
    val lineEnd = "\r\n"
    val twoHyphens = "--"
    val boundary = "*****"
    var bytesRead: Int
    var bytesAvailable: Int
    var bufferSize: Int
    val buffer: ByteArray
    val maxBufferSize = 1 * 1024 * 1024
    var serverResponseCode = 0
    //errMsg=Environment.getExternalStorageDirectory().getAbsolutePath();
    if (!sourceFile.isFile()) {
        Log.e("uploadFile", "Source File Does not exist")
        return 0
    }
    try {
        // open a URL connection to the Servlet
        val fileInputStream = FileInputStream(sourceFile)
        val url = URL(endpointUrl)
        conn = url.openConnection() as HttpURLConnection // Open a HTTP  connection to  the URL
        conn.setDoInput(true) // Allow Inputs
        conn.setDoOutput(true) // Allow Outputs
        conn.setUseCaches(false) // Don't use a Cached Copy
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Connection", "Keep-Alive")
        conn.setRequestProperty("ENCTYPE", "multipart/form-data")
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
        conn.setRequestProperty("uploaded_file", fileName)
        //conn.setRequestProperty("pid", "4");
        dos = DataOutputStream(conn.getOutputStream())
        dos.writeBytes(twoHyphens + boundary + lineEnd)
        dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"$fileName\"$lineEnd")
        dos.writeBytes(lineEnd)
        bytesAvailable = fileInputStream.available() // create a buffer of  maximum size
        bufferSize = Math.min(bytesAvailable, maxBufferSize)
        buffer = ByteArray(bufferSize)

        // read file and write it into form...
        bytesRead = fileInputStream.read(buffer, 0, bufferSize)
        while (bytesRead > 0) {
            dos.write(buffer, 0, bufferSize)
            bytesAvailable = fileInputStream.available()
            bufferSize = Math.min(bytesAvailable, maxBufferSize)
            bytesRead = fileInputStream.read(buffer, 0, bufferSize)
        }

        // send multipart form data necesssary after file data...
        dos.writeBytes(lineEnd)
        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

        // Responses from the server (code and message)
        serverResponseCode = conn.getResponseCode()
        val serverResponseMessage: String = conn.getResponseMessage()
        Log.i(
            "uploadFile",
            "HTTP Response is : $serverResponseMessage: $serverResponseCode"
        )
        if (serverResponseCode !== 200) {
//            getActivity().runOnUiThread(Runnable {
//                Toast.makeText(
//                    context,
//                    "Il y a une erreur l'hors du trasfert de l'image.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            })
        }

        //close the streams //
        fileInputStream.close()
        dos.flush()
        dos.close()
    } catch (ex: MalformedURLException) {
        // dialog.dismiss();
        ex.printStackTrace()
//        Toast.makeText(context, "MalformedURLException", Toast.LENGTH_SHORT).show()
        Log.e("Upload file to server", "error: " + ex.message, ex)
    } catch (e: Exception) {
        //  dialog.dismiss();
        e.printStackTrace()
//        Toast.makeText(context, errMsg.toString() + "Exception : " + e.message, Toast.LENGTH_SHORT)
//            .show()
        Log.e("uploadFile server ex", "Exception : " + e.message, e)
    }
    //  dialog.dismiss();
    return serverResponseCode
}