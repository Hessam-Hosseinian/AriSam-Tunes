package com.arisamtunes.auth

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class AvatarStorage(
    private val root: Path = Path.of(System.getenv("AVATAR_DATA_FOLDER") ?: "uploads/avatars"),
) {
    private val normalizedRoot = root.toAbsolutePath().normalize()

    fun save(userId: UUID, contentType: String, bytes: ByteArray): String {
        Files.createDirectories(normalizedRoot)
        val extension = when (contentType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw IllegalArgumentException("Unsupported avatar content type")
        }
        val fileName = "${userId}-${UUID.randomUUID()}.$extension"
        val destination = normalizedRoot.resolve(fileName).normalize()
        require(destination.startsWith(normalizedRoot))
        Files.write(destination, bytes)
        return fileName
    }

    fun delete(fileName: String) {
        val target = normalizedRoot.resolve(fileName).normalize()
        if (target.startsWith(normalizedRoot)) Files.deleteIfExists(target)
    }
}
