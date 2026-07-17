package com.arisamtunes.data.social

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max

internal data class EncodedProfileImage(
    val bytes: ByteArray,
    val contentType: String = "image/jpeg",
    val fileName: String = "profile.jpg",
)

internal object ProfileImageEncoder {
    private const val MaxDimension = 1_440
    private const val MaxUploadBytes = 5 * 1024 * 1024

    fun encode(resolver: ContentResolver, uri: Uri): EncodedProfileImage {
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = max(info.size.width, info.size.height)
                if (largest > MaxDimension) {
                    val scale = MaxDimension.toFloat() / largest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            decodeLegacy(resolver, uri)
        }
        val scaled = decoded.scaledDown()
        val output = ByteArrayOutputStream()
        check(scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)) { "Could not encode profile image" }
        if (output.size() > MaxUploadBytes) {
            output.reset()
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 72, output)) { "Could not encode profile image" }
        }
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        val bytes = output.toByteArray()
        require(bytes.size <= MaxUploadBytes) { "Profile image is too large" }
        return EncodedProfileImage(bytes)
    }

    private fun decodeLegacy(resolver: ContentResolver, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Profile image could not be opened" }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported profile image" }
        var sampleSize = 1
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MaxDimension * 2) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Profile image could not be opened" }
            requireNotNull(BitmapFactory.decodeStream(input, null, options)) { "Unsupported profile image" }
        }
    }

    private fun Bitmap.scaledDown(): Bitmap {
        val largest = max(width, height)
        if (largest <= MaxDimension) return this
        val scale = MaxDimension.toFloat() / largest
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }
}
