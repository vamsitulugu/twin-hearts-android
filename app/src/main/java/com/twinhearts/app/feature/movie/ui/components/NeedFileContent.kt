package com.twinhearts.app.feature.movie.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.ui.readLocalVideo
import kotlinx.coroutines.launch

/**
 * Why this file exists: the movie started, but this phone has no copy of the file (never picked, or the app was
 * restarted). Instead of a frozen countdown, it asks for the file and joins the movie at the right spot.
 */
@Composable
fun NeedFileContent(
    movieTitle: String,
    onPicked: (uri: String, title: String, durationSec: Int) -> Unit,
    onLeave: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val video = readLocalVideo(context, uri)
            if (video == null) Toast.makeText(context, R.string.movie_toast_bad_video, Toast.LENGTH_SHORT).show()
            else onPicked(video.uri, video.title, video.durationSec)
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.movie_need_file_title), color = TwinTheme.colors.ink,
            style = MaterialTheme.typography.headlineSmall, fontFamily = SerifFamily, textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.movie_need_file_body, movieTitle), color = TwinTheme.colors.inkMuted,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 14.dp),
        )
        GlassButton(stringResource(R.string.movie_need_file_pick), { launcher.launch(arrayOf("video/*")) })
        GlassButton(
            stringResource(R.string.movie_need_file_leave), onLeave, Modifier.padding(top = 10.dp), style = GlassButtonStyle.Ghost,
        )
    }
}
