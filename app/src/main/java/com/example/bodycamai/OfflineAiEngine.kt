package com.example.bodycamai

import android.media.Image
import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.core.DetectionBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Offline-first vision pipeline. No network is required for these bundled models. */
class OfflineAiEngine {
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
    )

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun analyze(image: Image, rotation: Int, callback: (AiFrameResult) -> Unit) {
        val input = InputImage.fromMediaImage(image, rotation)
        objectDetector.process(input).addOnSuccessListener { objects ->
            faceDetector.process(input).addOnSuccessListener { faces ->
                textRecognizer.process(input).addOnSuccessListener { text ->
                    callback(
                        AiFrameResult(
                            objects = objects.map { obj ->
                                DetectionBox(
                                    label = obj.labels.firstOrNull()?.text ?: "Объект",
                                    confidence = obj.labels.firstOrNull()?.confidence ?: 0f,
                                    bounds = obj.boundingBox,
                                    trackingId = obj.trackingId
                                )
                            },
                            faceCount = faces.size,
                            text = text.text
                        )
                    )
                }.addOnFailureListener {
                    callback(AiFrameResult(faceCount = faces.size))
                }
            }.addOnFailureListener {
                callback(AiFrameResult())
            }
        }.addOnFailureListener {
            callback(AiFrameResult())
        }
    }

    fun close() {
        objectDetector.close()
        faceDetector.close()
        textRecognizer.close()
    }
}
