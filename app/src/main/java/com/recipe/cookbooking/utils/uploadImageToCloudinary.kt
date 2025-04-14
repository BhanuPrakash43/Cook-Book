package com.recipe.cookbooking.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun uploadImageToCloudinary(
    context: Context,
    imageUri: Uri,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val cloudName = "dt6cnp3y0"
    val uploadPreset = "cookbook_uploads"
    val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    try {
        // Create a temporary file to store the image
        val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)

        // Copy the image data to the temporary file
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: run {
            onFailure("Failed to read image data")
            return
        }

        val imageStream = context.contentResolver.openInputStream(imageUri)
        val bytes = imageStream?.readBytes() ?: run {
            onFailure("Failed to read image data")
            return
        }

        // Create RequestBody from bytes
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "image.jpg",
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), bytes)
            )
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        // Create the request
        val request = Request.Builder()
            .url(uploadUrl)
            .post(requestBody)
            .build()

        // Execute the request
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CloudinaryUpload", "Upload failed", e)
                tempFile.delete() // Clean up the temp file
                onFailure(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                tempFile.delete()

                if (!response.isSuccessful) {
                    Log.e("CloudinaryUpload", "Response not successful: ${response.code}")
                    onFailure("Server returned error: ${response.code}")
                    return
                }

                response.body?.string()?.let { json ->
                    try {
                        val jsonObject = JSONObject(json)
                        val imageUrl = jsonObject.getString("secure_url")
                        Log.d("CloudinaryUpload", "Upload successful: $imageUrl")
                        onSuccess(imageUrl)
                    } catch (e: Exception) {
                        Log.e("CloudinaryUpload", "Failed to parse response", e)
                        onFailure("Failed to parse Cloudinary response")
                    }
                } ?: onFailure("Empty response from server")
            }
        })
    } catch (e: Exception) {
        Log.e("CloudinaryUpload", "Exception during upload", e)
        onFailure("Error processing image: ${e.message}")
    }
}
