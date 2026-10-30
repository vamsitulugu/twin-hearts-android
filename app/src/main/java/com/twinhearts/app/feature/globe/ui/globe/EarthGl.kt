package com.twinhearts.app.feature.globe.ui.globe

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/*
 * Why this file exists: the real 3D Earth. The web app uses Cesium (satellite Earth). Here the same
 * look is drawn with OpenGL ES 3: one full-screen quad, and for every pixel the shader finds the point
 * of the sphere under it and reads the satellite picture (assets/globe_earth.jpg, NASA Blue Marble,
 * public domain). The maths is the same orthographic ball as GlobeProjection, so the pins, arcs and
 * labels that Compose draws on top line up exactly. Stars and the blue atmosphere are drawn here too.
 */

private const val TEXTURE_ASSET = "globe_earth.jpg"
private const val GLES3_VERSION = 0x30000

// The sky-blue rim of the real atmosphere (physical colour, like the web globe), not a brand colour.
private const val ATMOSPHERE_R = 0.30f
private const val ATMOSPHERE_G = 0.58f
private const val ATMOSPHERE_B = 1.00f

/** The camera numbers the renderer needs. Written by Compose, read on the GL thread. */
class EarthGlState {
    @Volatile private var surface: GLSurfaceView? = null
    @Volatile var centerX = 0f
        private set
    @Volatile var centerY = 0f
        private set
    @Volatile var radius = 1f
        private set
    @Volatile var latDeg = 0.0
        private set
    @Volatile var lonDeg = 0.0
        private set
    @Volatile var background = Color.Black
        private set

    internal fun attach(view: GLSurfaceView?) {
        surface = view
    }

    /** Called every frame the globe moves; asks the GL thread to draw once. */
    fun push(centerX: Float, centerY: Float, radius: Float, latDeg: Double, lonDeg: Double, background: Color) {
        this.centerX = centerX
        this.centerY = centerY
        this.radius = radius
        this.latDeg = latDeg
        this.lonDeg = lonDeg
        this.background = background
        surface?.requestRender()
    }
}

/** True when the phone can run the shader (OpenGL ES 3.0). */
fun supportsEarthGl(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    return am.deviceConfigurationInfo.reqGlEsVersion >= GLES3_VERSION
}

@Composable
fun EarthGlView(
    state: EarthGlState,
    onReady: () -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ready by rememberUpdatedState(onReady)
    val failed by rememberUpdatedState(onFailed)
    val view = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 0, 0)
            setRenderer(
                EarthRenderer(
                    context.applicationContext,
                    state,
                    onReady = { post { ready() } },
                    onFailed = { post { failed() } },
                ),
            )
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }
    // GLSurfaceView must be told when the screen pauses, or it keeps the GPU busy in the background.
    DisposableEffect(lifecycle, view) {
        state.attach(view)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            state.attach(null)
            view.onPause()
        }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

private class EarthRenderer(
    private val context: Context,
    private val state: EarthGlState,
    private val onReady: () -> Unit,
    private val onFailed: () -> Unit,
) : GLSurfaceView.Renderer {
    private var program = 0
    private var texture = 0
    private var width = 1
    private var height = 1
    private var announced = false

    private var uRes = 0
    private var uCenter = 0
    private var uRadius = 0
    private var uRight = 0
    private var uUp = 0
    private var uFwd = 0
    private var uBg = 0
    private var uAtmo = 0
    private var uTex = 0

    private val quad = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
        position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        announced = false
        program = buildProgram()
        val loaded = program != 0 && loadTexture()
        if (!loaded) {
            onFailed()
            return
        }
        uRes = GLES30.glGetUniformLocation(program, "uRes")
        uCenter = GLES30.glGetUniformLocation(program, "uCenter")
        uRadius = GLES30.glGetUniformLocation(program, "uRadius")
        uRight = GLES30.glGetUniformLocation(program, "uRight")
        uUp = GLES30.glGetUniformLocation(program, "uUp")
        uFwd = GLES30.glGetUniformLocation(program, "uFwd")
        uBg = GLES30.glGetUniformLocation(program, "uBg")
        uAtmo = GLES30.glGetUniformLocation(program, "uAtmo")
        uTex = GLES30.glGetUniformLocation(program, "uTex")
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (program == 0 || texture == 0) return
        val bg = state.background
        GLES30.glClearColor(bg.red, bg.green, bg.blue, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)

        // The same three axes GlobeView uses, so Canvas pins and this picture agree.
        val axes = GlobeAxes(state.latDeg, state.lonDeg)
        GLES30.glUniform2f(uRes, width.toFloat(), height.toFloat())
        GLES30.glUniform2f(uCenter, state.centerX, state.centerY)
        GLES30.glUniform1f(uRadius, state.radius)
        GLES30.glUniform3f(uRight, axes.rx, axes.ry, 0f)
        GLES30.glUniform3f(uUp, axes.ux, axes.uy, axes.uz)
        GLES30.glUniform3f(uFwd, axes.fx, axes.fy, axes.fz)
        GLES30.glUniform3f(uBg, bg.red, bg.green, bg.blue)
        GLES30.glUniform3f(uAtmo, ATMOSPHERE_R, ATMOSPHERE_G, ATMOSPHERE_B)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glUniform1i(uTex, 0)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, quad)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)

        if (!announced) {
            announced = true
            onReady()
        }
    }

    private fun loadTexture(): Boolean {
        val maxSize = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, maxSize, 0)
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            // Phones that only allow 2048-wide textures get a half-size picture instead of a failure.
            inSampleSize = if (maxSize[0] in 1..4095) 2 else 1
        }
        val bitmap = try {
            context.assets.open(TEXTURE_ASSET).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: java.io.IOException) {
            null
        } ?: return false

        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        texture = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        bitmap.recycle()
        return GLES30.glGetError() == GLES30.GL_NO_ERROR
    }

    private fun buildProgram(): Int {
        val vs = compile(GLES30.GL_VERTEX_SHADER, VERTEX)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, FRAGMENT)
        if (vs == 0 || fs == 0) return 0
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, vs)
        GLES30.glAttachShader(p, fs)
        GLES30.glBindAttribLocation(p, 0, "aPos")
        GLES30.glLinkProgram(p)
        val status = IntArray(1)
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, status, 0)
        return if (status[0] == GLES30.GL_TRUE) p else 0
    }

    private fun compile(type: Int, source: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, source)
        GLES30.glCompileShader(s)
        val status = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, status, 0)
        return if (status[0] == GLES30.GL_TRUE) s else 0
    }
}

/** Same numbers as GlobeView (right / up / forward of the camera), as plain floats for the shader. */
private class GlobeAxes(latDeg: Double, lonDeg: Double) {
    val rx: Float
    val ry: Float
    val ux: Float
    val uy: Float
    val uz: Float
    val fx: Float
    val fy: Float
    val fz: Float

    init {
        val phi = Math.toRadians(latDeg)
        val lam = Math.toRadians(lonDeg)
        rx = (-Math.sin(lam)).toFloat()
        ry = Math.cos(lam).toFloat()
        ux = (-Math.sin(phi) * Math.cos(lam)).toFloat()
        uy = (-Math.sin(phi) * Math.sin(lam)).toFloat()
        uz = Math.cos(phi).toFloat()
        fx = (Math.cos(phi) * Math.cos(lam)).toFloat()
        fy = (Math.cos(phi) * Math.sin(lam)).toFloat()
        fz = Math.sin(phi).toFloat()
    }
}

private const val VERTEX = """#version 300 es
layout(location = 0) in vec2 aPos;
void main() { gl_Position = vec4(aPos, 0.0, 1.0); }
"""

// Colour grading (contrast, saturation, gamma) mirrors the web app's Cesium imagery correction.
private const val FRAGMENT = """#version 300 es
precision highp float;
uniform vec2 uRes;
uniform vec2 uCenter;
uniform float uRadius;
uniform vec3 uRight;
uniform vec3 uUp;
uniform vec3 uFwd;
uniform vec3 uBg;
uniform vec3 uAtmo;
uniform sampler2D uTex;
out vec4 outColor;

const float PI = 3.14159265359;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 stars(vec2 frag) {
    vec2 cell = floor(frag / 26.0);
    float r = hash(cell);
    if (r < 0.93) return vec3(0.0);
    vec2 pos = (cell + vec2(hash(cell + 3.1), hash(cell + 7.7))) * 26.0;
    float d = length(frag - pos);
    float size = 0.6 + hash(cell + 9.3) * 1.1;
    float a = smoothstep(size, 0.0, d) * (0.25 + 0.6 * hash(cell + 1.7));
    return vec3(a);
}

void main() {
    vec2 frag = vec2(gl_FragCoord.x, uRes.y - gl_FragCoord.y);
    vec2 d = (frag - uCenter) / uRadius;
    float r2 = dot(d, d);
    float dist = sqrt(r2);

    if (r2 > 1.0) {
        vec3 col = uBg + stars(frag);
        float glow = exp(-(dist - 1.0) * 8.5) * smoothstep(1.45, 1.0, dist);
        col = mix(col, uAtmo, clamp(glow * 0.7, 0.0, 1.0));
        outColor = vec4(col, 1.0);
        return;
    }

    float z = sqrt(1.0 - r2);
    vec3 p = d.x * uRight + (-d.y) * uUp + z * uFwd;
    float lon = atan(p.y, p.x);
    float lat = asin(clamp(p.z, -1.0, 1.0));
    vec2 uv = vec2(lon / (2.0 * PI) + 0.5, 0.5 - lat / PI);

    // Longitude jumps from 1 to 0 at the date line; pick the smaller slope so no seam line appears.
    vec2 uvB = vec2(fract(uv.x + 0.5), uv.y);
    vec2 gx = dFdx(uv);
    vec2 gy = dFdy(uv);
    vec2 gxB = dFdx(uvB);
    vec2 gyB = dFdy(uvB);
    if (abs(gxB.x) < abs(gx.x)) gx = gxB;
    if (abs(gyB.x) < abs(gy.x)) gy = gyB;
    vec3 c = textureGrad(uTex, uv, gx, gy).rgb;

    c = pow(c, vec3(0.92));
    c = (c - 0.5) * 1.22 + 0.5;
    c *= 1.05;
    float luma = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(luma), c, 1.3);

    // Round shading: darker toward the edge, a thin blue haze on the rim.
    float facing = z;
    c *= mix(0.62, 1.0, pow(facing, 0.5));
    c += uAtmo * pow(1.0 - facing, 3.2) * 0.55;
    float edge = smoothstep(1.0, 0.985, dist);
    outColor = vec4(mix(uAtmo, clamp(c, 0.0, 1.0), edge), 1.0);
}
"""
