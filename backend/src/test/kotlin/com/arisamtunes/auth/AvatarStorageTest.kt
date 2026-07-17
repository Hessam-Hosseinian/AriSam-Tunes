package com.arisamtunes.auth

import java.nio.file.Files
import java.util.UUID
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvatarStorageTest {
    @Test
    fun `stores and deletes an avatar inside its configured folder`() {
        val root = Files.createTempDirectory("arisam-avatar-test")
        try {
            val storage = AvatarStorage(root)
            val bytes = byteArrayOf(1, 2, 3, 4)

            val fileName = storage.save(UUID.randomUUID(), "image/jpeg", bytes)
            val storedFile = root.resolve(fileName)

            assertTrue(Files.isRegularFile(storedFile))
            assertContentEquals(bytes, storedFile.readBytes())

            storage.delete(fileName)
            assertFalse(Files.exists(storedFile))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects unsupported content types`() {
        val root = Files.createTempDirectory("arisam-avatar-test")
        try {
            assertFailsWith<IllegalArgumentException> {
                AvatarStorage(root).save(UUID.randomUUID(), "image/svg+xml", byteArrayOf(1))
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
