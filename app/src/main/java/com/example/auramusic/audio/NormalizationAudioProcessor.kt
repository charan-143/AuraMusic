package com.example.auramusic.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NormalizationAudioProcessor : BaseAudioProcessor() {
    @Volatile var enabled = true
    private val normalizer = VolumeNormalizer()
    private var frame = FloatArray(2)

    override fun onConfigure(input: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (input.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(input)
        frame = FloatArray(input.channelCount)
        return input
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        if (!enabled) output.put(inputBuffer) else {
            inputBuffer.order(ByteOrder.nativeOrder())
            while (inputBuffer.remaining() >= frame.size * 2) {
                for (i in frame.indices) frame[i] = inputBuffer.short / 32768f
                normalizer.processFrame(frame, inputAudioFormat.sampleRate)
                for (sample in frame) output.putShort((sample * 32768).toInt().coerceIn(-32768, 32767).toShort())
            }
        }
        output.flip()
    }

    override fun onFlush() { normalizer.reset() }
}
