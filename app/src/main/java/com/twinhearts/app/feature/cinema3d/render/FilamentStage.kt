package com.twinhearts.app.feature.cinema3d.render

import android.os.SystemClock
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.twinhearts.app.feature.cinema3d.domain.CameraRig
import com.twinhearts.app.feature.cinema3d.domain.FrameStats
import com.twinhearts.app.feature.cinema3d.domain.FrameStatsTracker
import java.nio.ByteBuffer
import kotlin.math.pow

/** What loading one model told us. [probes] is "node name -> found?" so a missing name is obvious on screen. */
data class ModelInfo(
    val label: String,
    val loadMillis: Long,
    val entityCount: Int,
    val renderableCount: Int,
    val probes: List<Pair<String, Boolean>>,
)

/** A model that is in the scene (or hidden from it). Only [FilamentStage] touches [asset]. */
class StageModel internal constructor(
    internal val asset: FilamentAsset,
    val info: ModelInfo,
) {
    internal var visible: Boolean = true
}

/**
 * Everything Filament for the theatre, behind a small API: one engine, one scene, one camera, one surface.
 *
 * THREADING: Filament is not thread-safe. Create, call and destroy this on the main thread only.
 * The Choreographer callback also runs on the main thread, so [rig] can be changed from touch handlers directly.
 *
 * Needs OpenGL ES 3.0. Creating it throws on a phone that cannot provide that; the caller shows an error.
 */
class FilamentStage(private val surfaceView: SurfaceView) {

    /** The camera to render from. Read once per frame. */
    var rig: CameraRig = CameraRig()

    private val engine: Engine
    private val renderer: Renderer
    private val scene: Scene
    private val view: View
    private val camera: Camera
    private val cameraEntity: Int
    private val sunEntity: Int
    private val indirectLight: IndirectLight
    private val skybox: Skybox
    private val materialProvider: UbershaderProvider
    private val assetLoader: AssetLoader
    private val resourceLoader: ResourceLoader
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val choreographer: Choreographer = Choreographer.getInstance()
    private val stats = FrameStatsTracker()
    private val models = mutableListOf<StageModel>()

    private var swapChain: SwapChain? = null
    private var aspect = 1.0
    private var running = false
    private var destroyed = false

    /** Frame timing over the last second. */
    val frameStats: FrameStats get() = stats.latest

    init {
        ensureNativeLibraries()
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        cameraEntity = EntityManager.get().create()
        camera = engine.createCamera(cameraEntity)
        view.camera = camera
        view.scene = scene

        skybox = Skybox.Builder().color(0.02f, 0.02f, 0.03f, 1f).build(engine)
        scene.skybox = skybox

        // Flat ambient light, so nothing is pitch black while there is no baked lighting yet.
        // 0.564 is roughly the first spherical-harmonics term of an evenly lit environment; the brightness
        // slider on the spike screen absorbs any scale error.
        indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.564f, 0.564f, 0.564f))
            .intensity(30_000f)
            .build(engine)
        scene.indirectLight = indirectLight

        // One directional light, no shadows (the phone budget in the plan).
        sunEntity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 0.96f, 0.9f)
            .intensity(100_000f)
            .direction(-0.3f, -1f, -0.4f)
            .castShadows(false)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)

        materialProvider = UbershaderProvider(engine)
        assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
        resourceLoader = ResourceLoader(engine)

        setBrightness(0f)
        setHighQuality(true)

        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                }
                swapChain = null
            }

            override fun onResized(width: Int, height: Int) {
                if (width <= 0 || height <= 0) return
                view.viewport = Viewport(0, 0, width, height)
                aspect = width.toDouble() / height.toDouble()
                camera.setProjection(rig.fovDegrees, aspect, NEAR_METRES, FAR_METRES, Camera.Fov.VERTICAL)
            }
        }
        uiHelper.attachTo(surfaceView)
    }

    /**
     * Parses a .glb, builds its textures, and adds it to the scene. Runs on the main thread and blocks for
     * roughly the time shown in [ModelInfo.loadMillis]; for the spike that is acceptable, the real build
     * should switch to Filament's async loader.
     * Returns null if the file could not be parsed.
     */
    fun loadModel(label: String, glb: ByteBuffer, probeNames: List<String>): StageModel? {
        if (destroyed) return null
        val startedAt = SystemClock.elapsedRealtime()
        val asset = assetLoader.createAsset(glb) ?: return null
        resourceLoader.loadResources(asset)
        asset.releaseSourceData()
        scene.addEntities(asset.entities)

        val info = ModelInfo(
            label = label,
            loadMillis = SystemClock.elapsedRealtime() - startedAt,
            entityCount = asset.entities.size,
            renderableCount = asset.renderableEntities.size,
            probes = probeNames.map { name -> name to (asset.getFirstEntityByName(name) != 0) },
        )
        return StageModel(asset, info).also { models += it }
    }

    fun setVisible(model: StageModel, visible: Boolean) {
        if (destroyed || model.visible == visible) return
        if (visible) scene.addEntities(model.asset.entities) else scene.removeEntities(model.asset.entities)
        model.visible = visible
    }

    /** [ev] is exposure in stops: 0 is the default, +1 is twice as bright. */
    fun setBrightness(ev: Float) {
        val iso = 100f * 2f.pow(ev)
        camera.setExposure(16f, 1f / 125f, iso)
    }

    /** High = FXAA + 4x MSAA. Low = no anti-aliasing. (Resolution scaling comes in C7.) */
    fun setHighQuality(high: Boolean) {
        view.antiAliasing = if (high) View.AntiAliasing.FXAA else View.AntiAliasing.NONE
        view.multiSampleAntiAliasingOptions = View.MultiSampleAntiAliasingOptions().also { it.enabled = high }
    }

    fun start() {
        if (destroyed || running) return
        running = true
        stats.reset()
        choreographer.postFrameCallback(frameCallback)
    }

    fun stop() {
        if (!running) return
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }

    /** Frees everything, in the order Filament needs. Safe to call twice. */
    fun destroy() {
        if (destroyed) return
        stop()
        destroyed = true
        uiHelper.detach()

        models.forEach { model ->
            if (model.visible) scene.removeEntities(model.asset.entities)
            assetLoader.destroyAsset(model.asset)
        }
        models.clear()
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        assetLoader.destroy()
        resourceLoader.destroy()

        scene.removeEntity(sunEntity)
        engine.lightManager.destroy(sunEntity)
        EntityManager.get().destroy(sunEntity)
        engine.destroyIndirectLight(indirectLight)
        engine.destroySkybox(skybox)

        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyRenderer(renderer)
        engine.destroy()
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            choreographer.postFrameCallback(this)
            val chain = swapChain ?: return
            if (!uiHelper.isReadyToRender) return

            val eye = rig.eye()
            val look = rig.target()
            camera.lookAt(eye.x, eye.y, eye.z, look.x, look.y, look.z, 0.0, 1.0, 0.0)

            if (renderer.beginFrame(chain, frameTimeNanos)) {
                renderer.render(view)
                renderer.endFrame()
                stats.onFrame(frameTimeNanos)
            }
        }
    }

    private companion object {
        const val NEAR_METRES = 0.1
        const val FAR_METRES = 100.0

        private var nativeLoaded = false

        /** Loads Filament's native code once per process (main thread only, like the rest of this class). */
        fun ensureNativeLibraries() {
            if (nativeLoaded) return
            Filament.init()
            Gltfio.init()
            nativeLoaded = true
        }
    }
}
