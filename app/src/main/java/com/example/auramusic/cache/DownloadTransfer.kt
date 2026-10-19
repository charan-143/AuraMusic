package com.example.auramusic.cache

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Copy only real bytes, check cancellation between reads, and reject truncated responses. */
object DownloadTransfer {
    fun copy(input: InputStream, output: OutputStream, expectedBytes: Long,
             checkActive: () -> Unit, onBytes: (Long) -> Unit = {}): Long {
        val buffer = ByteArray(8192)
        var copied = 0L
        while (true) {
            checkActive()
            val count = input.read(buffer)
            checkActive()
            if (count < 0) break
            output.write(buffer, 0, count)
            copied += count
            onBytes(copied)
        }
        if (expectedBytes >= 0 && copied != expectedBytes) throw IOException("Incomplete download; tap to retry")
        return copied
    }
}
