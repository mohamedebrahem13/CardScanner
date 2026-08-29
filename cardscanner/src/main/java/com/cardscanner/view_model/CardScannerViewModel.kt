package com.cardscanner.view_model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardscanner.CardScannerResult
import com.cardscanner.config.CardScannerConfig
import com.cardscanner.parser.CardOcrParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
internal class CardScannerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    private val analysisExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null

    private var camera: Camera? = null

    private var imageAnalysis: ImageAnalysis? = null

    private var cameraBindingJob: Job? = null

    private var analysisJob: Job? = null

    /*
     * Used to invalidate old camera / OCR work when a new
     * scanner session starts.
     */
    private val sessionId =
        AtomicLong(0L)

    private val nextAnalysisId =
        AtomicLong(0L)

    private val activeAnalysisId =
        AtomicLong(NO_ACTIVE_ANALYSIS)

    private val resultReturned =
        AtomicBoolean(false)

    private val cardNumberCounts =
        mutableMapOf<String, Int>()

    private val expiryCounts =
        mutableMapOf<String, Int>()

    private val cardHolderCounts =
        mutableMapOf<String, Int>()

    private var firstValidDetectionTime =
        0L

    private var currentConfig:
            CardScannerConfig? = null

    private val _results =
        MutableSharedFlow<CardScannerResult>(
            extraBufferCapacity = 1
        )

    val results =
        _results.asSharedFlow()

    /*
     * Torch state.
     */

    private val _torchAvailable =
        MutableStateFlow(false)

    val torchAvailable =
        _torchAvailable.asStateFlow()

    private val _torchEnabled =
        MutableStateFlow(false)

    val torchEnabled =
        _torchEnabled.asStateFlow()

    private var torchObservedCamera:
            Camera? = null

    private val torchStateObserver =
        Observer<Int> { state ->
            _torchEnabled.value =
                state == TorchState.ON
        }

    fun startScanner(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        config: CardScannerConfig
    ) {
        stopScanner()

        val currentSessionId =
            sessionId.incrementAndGet()

        currentConfig =
            config

        resetScanningState()

        cameraBindingJob =
            viewModelScope.launch {
                try {
                    val provider =
                        ProcessCameraProvider.awaitInstance(
                            appContext
                        )

                    if (
                        !isSessionActive(
                            currentSessionId
                        )
                    ) {
                        return@launch
                    }

                    cameraProvider =
                        provider

                    bindCameraUseCases(
                        provider = provider,
                        lifecycleOwner =
                            lifecycleOwner,
                        previewView =
                            previewView,
                        currentSessionId =
                            currentSessionId
                    )
                } catch (
                    exception: CancellationException
                ) {
                    /*
                     * Normal cancellation when the screen
                     * closes or scanner restarts.
                     */
                    throw exception
                } catch (
                    exception: Exception
                ) {
                    if (
                        isSessionActive(
                            currentSessionId
                        )
                    ) {
                        camera =
                            null

                        imageAnalysis =
                            null

                        clearTorchObservation()

                        Log.e(
                            TAG,
                            "Camera initialization failed",
                            exception
                        )
                    }
                }
            }
    }

    private fun bindCameraUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        currentSessionId: Long
    ) {
        if (
            !isSessionActive(
                currentSessionId
            )
        ) {
            return
        }

        provider.unbindAll()

        val preview =
            Preview.Builder()
                .build()
                .apply {
                    surfaceProvider =
                        previewView.surfaceProvider
                }

        val resolutionSelector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(
                            ANALYSIS_WIDTH,
                            ANALYSIS_HEIGHT
                        ),
                        ResolutionStrategy
                            .FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()

        val analysisUseCase =
            ImageAnalysis.Builder()
                .setResolutionSelector(
                    resolutionSelector
                )
                .setBackpressureStrategy(
                    ImageAnalysis
                        .STRATEGY_KEEP_ONLY_LATEST
                )
                .build()

        analysisUseCase.setAnalyzer(
            analysisExecutor
        ) { imageProxy ->
            analyzeImage(
                imageProxy =
                    imageProxy,
                currentSessionId =
                    currentSessionId
            )
        }

        imageAnalysis =
            analysisUseCase

        val boundCamera =
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector
                    .DEFAULT_BACK_CAMERA,
                preview,
                analysisUseCase
            )

        if (
            !isSessionActive(
                currentSessionId
            )
        ) {
            analysisUseCase.clearAnalyzer()

            provider.unbind(
                preview,
                analysisUseCase
            )

            return
        }

        camera =
            boundCamera

        observeTorchState(
            boundCamera
        )

        focusAndMeterAtCenter(
            boundCamera =
                boundCamera,
            previewView =
                previewView,
            currentSessionId =
                currentSessionId
        )
    }
    private fun analyzeImage(
        imageProxy: ImageProxy,
        currentSessionId: Long
    ) {
        if (
            !isSessionActive(currentSessionId) ||
            resultReturned.get()
        ) {
            imageProxy.close()
            return
        }

        val analysisId =
            nextAnalysisId.incrementAndGet()

        if (
            !activeAnalysisId.compareAndSet(
                NO_ACTIVE_ANALYSIS,
                analysisId
            )
        ) {
            imageProxy.close()
            return
        }

        val rotationDegrees =
            imageProxy.imageInfo.rotationDegrees

        val sourceBitmap =
            try {
                imageProxy.toBitmap()
            } catch (exception: Exception) {
                Log.d(
                    TAG,
                    "Could not convert frame to Bitmap",
                    exception
                )

                null
            } finally {
                imageProxy.close()
            }

        if (sourceBitmap == null) {
            activeAnalysisId.compareAndSet(
                analysisId,
                NO_ACTIVE_ANALYSIS
            )

            return
        }

        analysisJob =
            viewModelScope.launch {

                var uprightBitmap: Bitmap? = null
                var cardBitmap: Bitmap? = null

                try {
                    if (
                        !isSessionActive(currentSessionId)
                    ) {
                        return@launch
                    }

                    /*
                     * Rotate CameraX image.
                     */
                    val upright =
                        withContext(
                            Dispatchers.Default
                        ) {
                            sourceBitmap.rotateToUpright(
                                rotationDegrees
                            )
                        }

                    uprightBitmap =
                        upright

                    /*
                     * Crop physical card.
                     */
                    val card =
                        withContext(
                            Dispatchers.Default
                        ) {
                            upright.cropCenteredCardArea()
                        }

                    cardBitmap =
                        card

                    if (
                        !isSessionActive(currentSessionId)
                    ) {
                        return@launch
                    }

                    /*
                     * =========================================
                     * ML KIT ONLY
                     * =========================================
                     */
                    val fullCardText =
                        recognizeText(
                            card
                        )

                    processText(
                        fullText =
                            fullCardText,
                        currentSessionId =
                            currentSessionId
                    )

                } catch (
                    exception: CancellationException
                ) {
                    throw exception

                } catch (
                    exception: Exception
                ) {
                    Log.d(
                        TAG,
                        "OCR failed for current frame",
                        exception
                    )

                } finally {

                    recycleDistinctBitmaps(
                        sourceBitmap,
                        uprightBitmap,
                        cardBitmap
                    )

                    activeAnalysisId.compareAndSet(
                        analysisId,
                        NO_ACTIVE_ANALYSIS
                    )
                }
            }
    }

    private suspend fun recognizeText(
        bitmap: Bitmap
    ): String {
        val inputImage =
            InputImage.fromBitmap(
                bitmap,
                0
            )

        return textRecognizer
            .process(
                inputImage
            )
            .await()
            .text
    }

    /**
     * Returns true when the OCR text contains a valid
     * card candidate.
     */
    private suspend fun processText(
        fullText: String,
        currentSessionId: Long
    ): Boolean {
        if (
            !isSessionActive(
                currentSessionId
            ) ||
            resultReturned.get()
        ) {
            return false
        }

        val config =
            currentConfig
                ?: return false

        val parsedResult =
            withContext(
                Dispatchers.Default
            ) {
                CardOcrParser.parse(
                    fullText
                )
            } ?: return false

        if (
            !isSessionActive(
                currentSessionId
            )
        ) {
            return false
        }

        parsedResult.cardNumber
            ?: return false

        if (
            config.requireExpiryDate &&
            parsedResult
                .expiryDate
                .isNullOrEmpty()
        ) {
            /*
             * PAN might be valid but expiry could still be
             * missing. Let another enhanced variant try.
             */
            return false
        }

        processParsedResult(
            parsedResult =
                parsedResult,
            config =
                config,
            currentSessionId =
                currentSessionId
        )

        return true
    }

    private suspend fun processParsedResult(
        parsedResult: CardScannerResult,
        config: CardScannerConfig,
        currentSessionId: Long
    ) {
        if (
            !isSessionActive(
                currentSessionId
            ) ||
            resultReturned.get()
        ) {
            return
        }

        val cardNumber =
            parsedResult.cardNumber
                ?: return

        val expiryDate =
            parsedResult.expiryDate

        if (
            firstValidDetectionTime ==
            0L
        ) {
            firstValidDetectionTime =
                System.currentTimeMillis()
        }

        cardNumberCounts.increment(
            cardNumber
        )

        expiryDate?.let { value ->
            expiryCounts.increment(
                value
            )
        }

        parsedResult
            .cardHolderName
            ?.let { value ->
                cardHolderCounts
                    .increment(
                        value
                    )
            }

        val elapsedTime =
            System.currentTimeMillis() -
                    firstValidDetectionTime

        if (
            elapsedTime <
            config.scanStabilizationMs
        ) {
            return
        }

        val bestCardNumber =
            cardNumberCounts
                .maxByOrNull { entry ->
                    entry.value
                }
                ?: return

        if (
            bestCardNumber.value <
            config.minCardNumberDetections
        ) {
            return
        }

        val bestExpiryDate =
            expiryCounts
                .maxByOrNull { entry ->
                    entry.value
                }
                ?.key

        val bestCardHolderName =
            cardHolderCounts
                .maxByOrNull { entry ->
                    entry.value
                }
                ?.key

        if (
            config.requireExpiryDate &&
            bestExpiryDate
                .isNullOrEmpty()
        ) {
            return
        }

        if (
            !isSessionActive(
                currentSessionId
            )
        ) {
            return
        }

        if (
            !resultReturned
                .compareAndSet(
                    false,
                    true
                )
        ) {
            return
        }

        _results.emit(
            CardScannerResult(
                cardNumber =
                    bestCardNumber.key,
                expiryDate =
                    bestExpiryDate,
                cardHolderName =
                    bestCardHolderName
            )
        )
    }

    fun toggleTorch() {
        val boundCamera =
            camera ?: return

        val currentSessionId =
            sessionId.get()

        if (
            !isSessionActive(
                currentSessionId
            )
        ) {
            return
        }

        if (
            !boundCamera
                .cameraInfo
                .hasFlashUnit()
        ) {
            _torchAvailable.value =
                false

            _torchEnabled.value =
                false

            return
        }

        val shouldEnable =
            boundCamera
                .cameraInfo
                .torchState
                .value != TorchState.ON

        val torchFuture =
            boundCamera
                .cameraControl
                .enableTorch(
                    shouldEnable
                )

        torchFuture.addListener(
            {
                if (
                    isSessionActive(
                        currentSessionId
                    ) &&
                    camera ===
                    boundCamera
                ) {
                    runCatching {
                        torchFuture.get()
                    }.onSuccess {
                        _torchEnabled.value =
                            shouldEnable

                        applyExposureForTorch(
                            boundCamera =
                                boundCamera,
                            torchEnabled =
                                shouldEnable,
                            currentSessionId =
                                currentSessionId
                        )
                    }.onFailure { exception ->
                        _torchEnabled.value =
                            boundCamera
                                .cameraInfo
                                .torchState
                                .value ==
                                    TorchState.ON

                        Log.e(
                            TAG,
                            "Failed to change torch state",
                            exception
                        )
                    }
                }
            },
            ContextCompat
                .getMainExecutor(
                    appContext
                )
        )
    }

    private fun applyExposureForTorch(
        boundCamera: Camera,
        torchEnabled: Boolean,
        currentSessionId: Long
    ) {
        if (
            !isSessionActive(
                currentSessionId
            ) ||
            camera !==
            boundCamera
        ) {
            return
        }

        val exposureState =
            boundCamera
                .cameraInfo
                .exposureState

        if (
            !exposureState
                .isExposureCompensationSupported
        ) {
            return
        }

        val step =
            exposureState
                .exposureCompensationStep

        if (
            step.denominator ==
            0
        ) {
            return
        }

        val stepValue =
            step.numerator.toFloat() /
                    step.denominator.toFloat()

        if (
            stepValue <= 0f
        ) {
            return
        }

        val targetEv =
            if (torchEnabled) {
                TORCH_EXPOSURE_EV
            } else {
                DEFAULT_EXPOSURE_EV
            }

        val requestedIndex =
            (
                    targetEv /
                            stepValue
                    )
                .roundToInt()

        val range =
            exposureState
                .exposureCompensationRange

        val safeIndex =
            requestedIndex
                .coerceIn(
                    range.lower,
                    range.upper
                )

        if (
            safeIndex ==
            exposureState
                .exposureCompensationIndex
        ) {
            return
        }

        val exposureFuture =
            try {
                boundCamera
                    .cameraControl
                    .setExposureCompensationIndex(
                        safeIndex
                    )
            } catch (
                exception: Exception
            ) {
                Log.d(
                    TAG,
                    "Unable to request exposure",
                    exception
                )

                return
            }

        exposureFuture.addListener(
            {
                if (
                    isSessionActive(
                        currentSessionId
                    ) &&
                    camera ===
                    boundCamera
                ) {
                    runCatching {
                        exposureFuture.get()
                    }.onFailure { exception ->
                        Log.d(
                            TAG,
                            "Could not adjust exposure",
                            exception
                        )
                    }
                }
            },
            ContextCompat
                .getMainExecutor(
                    appContext
                )
        )
    }

    private fun observeTorchState(
        boundCamera: Camera
    ) {
        clearTorchObservation()

        torchObservedCamera =
            boundCamera

        val hasFlash =
            boundCamera
                .cameraInfo
                .hasFlashUnit()

        _torchAvailable.value =
            hasFlash

        if (!hasFlash) {
            _torchEnabled.value =
                false

            return
        }

        _torchEnabled.value =
            boundCamera
                .cameraInfo
                .torchState
                .value ==
                    TorchState.ON

        boundCamera
            .cameraInfo
            .torchState
            .observeForever(
                torchStateObserver
            )
    }

    private fun clearTorchObservation() {
        torchObservedCamera
            ?.cameraInfo
            ?.torchState
            ?.removeObserver(
                torchStateObserver
            )

        torchObservedCamera =
            null

        _torchAvailable.value =
            false

        _torchEnabled.value =
            false
    }

    private fun focusAndMeterAtCenter(
        boundCamera: Camera,
        previewView: PreviewView,
        currentSessionId: Long
    ) {
        previewView.post {
            if (
                !isSessionActive(
                    currentSessionId
                ) ||
                camera !==
                boundCamera ||
                previewView.width <=
                0 ||
                previewView.height <=
                0
            ) {
                return@post
            }

            val centerPoint =
                previewView
                    .meteringPointFactory
                    .createPoint(
                        previewView.width /
                                2f,
                        previewView.height /
                                2f,
                        FOCUS_AREA_SIZE
                    )

            val flags =
                FocusMeteringAction.FLAG_AF or
                        FocusMeteringAction.FLAG_AE or
                        FocusMeteringAction.FLAG_AWB

            val action =
                FocusMeteringAction
                    .Builder(
                        centerPoint,
                        flags
                    )
                    .setAutoCancelDuration(
                        FOCUS_AUTO_CANCEL_SECONDS,
                        TimeUnit.SECONDS
                    )
                    .build()

            val focusFuture =
                try {
                    boundCamera
                        .cameraControl
                        .startFocusAndMetering(
                            action
                        )
                } catch (
                    exception: Exception
                ) {
                    Log.d(
                        TAG,
                        "Could not start focus",
                        exception
                    )

                    return@post
                }

            focusFuture.addListener(
                {
                    if (
                        isSessionActive(
                            currentSessionId
                        ) &&
                        camera ===
                        boundCamera
                    ) {
                        runCatching {
                            focusFuture.get()
                        }.onFailure { exception ->
                            Log.d(
                                TAG,
                                "Center focus failed",
                                exception
                            )
                        }
                    }
                },
                ContextCompat
                    .getMainExecutor(
                        appContext
                    )
            )
        }
    }

    fun stopScanner() {
        /*
         * Invalidates all callbacks / OCR work belonging
         * to the current session.
         */
        sessionId.incrementAndGet()

        cameraBindingJob?.cancel()
        cameraBindingJob =
            null

        analysisJob?.cancel()
        analysisJob =
            null

        activeAnalysisId.set(
            NO_ACTIVE_ANALYSIS
        )

        runCatching {
            imageAnalysis
                ?.clearAnalyzer()
        }

        imageAnalysis =
            null

        val boundCamera =
            camera

        runCatching {
            if (
                boundCamera
                    ?.cameraInfo
                    ?.hasFlashUnit() ==
                true
            ) {
                boundCamera
                    .cameraControl
                    .enableTorch(
                        false
                    )
            }
        }.onFailure { exception ->
            Log.d(
                TAG,
                "Could not disable torch",
                exception
            )
        }

        resetExposure(
            boundCamera
        )

        clearTorchObservation()

        runCatching {
            cameraProvider
                ?.unbindAll()
        }.onFailure { exception ->
            Log.e(
                TAG,
                "Could not unbind CameraX",
                exception
            )
        }

        camera =
            null

        currentConfig =
            null

        resetScanningState()
    }

    private fun resetExposure(
        boundCamera: Camera?
    ) {
        if (
            boundCamera ==
            null
        ) {
            return
        }

        val exposureState =
            boundCamera
                .cameraInfo
                .exposureState

        if (
            !exposureState
                .isExposureCompensationSupported
        ) {
            return
        }

        runCatching {
            boundCamera
                .cameraControl
                .setExposureCompensationIndex(
                    0
                )
        }.onFailure { exception ->
            Log.d(
                TAG,
                "Could not reset exposure",
                exception
            )
        }
    }

    private fun resetScanningState() {
        resultReturned.set(
            false
        )

        firstValidDetectionTime =
            0L

        cardNumberCounts.clear()
        expiryCounts.clear()
        cardHolderCounts.clear()
    }

    private fun isSessionActive(
        currentSessionId: Long
    ): Boolean {
        return sessionId.get() ==
                currentSessionId &&
                currentConfig !=
                null
    }

    override fun onCleared() {
        stopScanner()

        textRecognizer.close()

        analysisExecutor
            .shutdownNow()

        cameraProvider =
            null

        currentConfig =
            null

        super.onCleared()
    }

    private fun MutableMap<String, Int>.increment(
        key: String
    ) {
        this[key] =
            getOrDefault(
                key,
                0
            ) + 1
    }

    private fun Bitmap.rotateToUpright(
        rotationDegrees: Int
    ): Bitmap {
        val rotation =
            (
                    rotationDegrees %
                            360 +
                            360
                    ) % 360

        if (
            rotation ==
            0
        ) {
            return this
        }

        val matrix =
            Matrix().apply {
                postRotate(
                    rotation.toFloat()
                )
            }

        return Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            matrix,
            true
        )
    }

    /**
     * Crops most of the background away and keeps
     * approximately the physical card region.
     */
    private fun Bitmap.cropCenteredCardArea():
            Bitmap {
        val maxWidth =
            (
                    width *
                            CARD_CROP_WIDTH_RATIO
                    )
                .roundToInt()
                .coerceIn(
                    1,
                    width
                )

        val maxHeight =
            (
                    height *
                            CARD_CROP_HEIGHT_RATIO
                    )
                .roundToInt()
                .coerceIn(
                    1,
                    height
                )

        var cropWidth =
            maxWidth

        var cropHeight =
            (
                    cropWidth /
                            CARD_ASPECT_RATIO
                    )
                .roundToInt()

        if (
            cropHeight >
            maxHeight
        ) {
            cropHeight =
                maxHeight

            cropWidth =
                (
                        cropHeight *
                                CARD_ASPECT_RATIO
                        )
                    .roundToInt()
        }

        cropWidth =
            cropWidth
                .coerceIn(
                    1,
                    width
                )

        cropHeight =
            cropHeight
                .coerceIn(
                    1,
                    height
                )

        val left =
            (
                    width -
                            cropWidth
                    ) / 2

        val top =
            (
                    height -
                            cropHeight
                    ) / 2

        return Bitmap.createBitmap(
            this,
            left,
            top,
            cropWidth,
            cropHeight
        )
    }

    private fun recycleDistinctBitmaps(
        vararg bitmaps: Bitmap?
    ) {
        val unique =
            mutableListOf<Bitmap>()

        bitmaps.forEach { bitmap ->
            if (
                bitmap != null &&
                unique.none { existing ->
                    existing ===
                            bitmap
                }
            ) {
                unique +=
                    bitmap
            }
        }

        unique.forEach { bitmap ->
            if (
                !bitmap.isRecycled
            ) {
                bitmap.recycle()
            }
        }
    }

    private companion object {
        const val TAG =
            "CardScannerViewModel"

        const val NO_ACTIVE_ANALYSIS =
            0L

        const val ANALYSIS_WIDTH =
            1920

        const val ANALYSIS_HEIGHT =
            1080

        const val FOCUS_AUTO_CANCEL_SECONDS =
            3L

        const val FOCUS_AREA_SIZE =
            0.35f

        const val CARD_ASPECT_RATIO =
            1.586f

        const val CARD_CROP_WIDTH_RATIO =
            0.92f

        const val CARD_CROP_HEIGHT_RATIO =
            0.58f

        /*
         * Reduce exposure when torch is enabled so
         * metallic digits are less likely to blow out.
         */
        const val TORCH_EXPOSURE_EV =
            -0.7f

        const val DEFAULT_EXPOSURE_EV =
            0f
    }
}