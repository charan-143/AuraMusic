package com.example.auramusic.cache

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class DownloadTransferTest {
    @Test fun rejectsTruncatedResponses() {
        assertThrows(IOException::class.java) {
            DownloadTransfer.copy(ByteArrayInputStream(byteArrayOf(1, 2)), ByteArrayOutputStream(), 5, {})
        }
    }

    @Test fun cancellationStopsWriting() {
        val output = ByteArrayOutputStream()
        var cancelled = false
        assertThrows(CancellationException::class.java) {
            DownloadTransfer.copy(ByteArrayInputStream(ByteArray(32_000)), output, 32_000,
                { if (cancelled) throw CancellationException() }, { cancelled = true })
        }
        assertEquals(8192, output.size())
    }

    @Test fun unknownLengthCopiesUntilEnd() {
        val output = ByteArrayOutputStream()
        val bytes = byteArrayOf(4, 5, 6)
        assertEquals(3L, DownloadTransfer.copy(ByteArrayInputStream(bytes), output, -1, {}))
        assertArrayEquals(bytes, output.toByteArray())
    }
}
