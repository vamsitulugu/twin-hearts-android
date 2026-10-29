package com.twinhearts.app.feature.movie.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.ui.MovieForm
import com.twinhearts.app.feature.movie.ui.readPosterJpeg
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.SerifFamily
import com.twinhearts.app.core.design.TwinFilterChip
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.movie.domain.model.MovieOwner
import com.twinhearts.app.feature.movie.domain.model.WatchStatus
import com.twinhearts.app.feature.movie.domain.movieGenres

/**
 * Why this file exists: the "Add Movie" / "Edit Movie" form of the web page: poster, name, year, genre,
 * rating, status, owner, personal review and favourite scene. Saves into the shared list. Pass [initial]
 * to edit a saved movie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieSheet(
    onSave: (MovieForm) -> Unit,
    onDismiss: () -> Unit,
    initial: MovieItem? = null,
    initialOwner: MovieOwner = MovieOwner.Mine,
) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(initial?.year.orEmpty()) }
    var genre by rememberSaveable { mutableStateOf(initial?.genre) }
    var status by rememberSaveable { mutableStateOf(initial?.status ?: WatchStatus.Watchlist) }
    var stars by rememberSaveable { mutableIntStateOf(initial?.rating?.toInt() ?: 0) }
    var owner by rememberSaveable { mutableStateOf(initialOwner) }
    var review by rememberSaveable { mutableStateOf(initial?.overview.orEmpty()) }
    var scene by rememberSaveable { mutableStateOf(initial?.scene.orEmpty()) }
    // A newly picked poster: its picture link (for the preview) and the shrunk bytes to send.
    var posterUri by rememberSaveable { mutableStateOf<String?>(null) }
    var posterBytes by remember { mutableStateOf<ByteArray?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readPosterJpeg(context, uri)
            if (bytes == null) {
                Toast.makeText(context, R.string.movie_toast_poster_unreadable, Toast.LENGTH_SHORT).show()
            } else {
                posterBytes = bytes
                posterUri = uri.toString()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.card) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(
                stringResource(if (initial == null) R.string.movie_add_title else R.string.movie_edit_title),
                color = colors.ink, style = MaterialTheme.typography.headlineSmall, fontFamily = SerifFamily,
            )
            PosterPick(posterUri ?: initial?.posterUrl) { picker.launch(arrayOf("image/*")) }
            TwinTextField(
                value = title, onValueChange = { title = it }, label = stringResource(R.string.movie_add_name),
                placeholder = stringResource(R.string.movie_add_name_hint), maxLength = MAX_TITLE,
            )
            TwinTextField(
                value = year, onValueChange = { year = it.filter(Char::isDigit) }, label = stringResource(R.string.movie_add_year),
                placeholder = stringResource(R.string.movie_add_year_hint), keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done, maxLength = YEAR_DIGITS,
            )
            Label(R.string.movie_add_genre)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                movieGenres.forEach { name -> TwinFilterChip(name, genre == name, { genre = if (genre == name) null else name }) }
            }
            Label(R.string.movie_add_rating)
            Row {
                (1..STARS).forEach { star ->
                    IconButton(onClick = { stars = if (stars == star) 0 else star }) {
                        Icon(
                            if (star <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder, null,
                            tint = if (star <= stars) colors.accent else colors.inkFaint,
                        )
                    }
                }
            }
            Label(R.string.movie_add_status)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TwinFilterChip(stringResource(R.string.movie_add_status_want), status == WatchStatus.Watchlist, { status = WatchStatus.Watchlist })
                TwinFilterChip(stringResource(R.string.movie_add_status_now), status == WatchStatus.Watching, { status = WatchStatus.Watching })
                TwinFilterChip(stringResource(R.string.movie_add_status_done), status == WatchStatus.Watched, { status = WatchStatus.Watched })
            }
            Label(R.string.movie_add_owner)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                TwinFilterChip(stringResource(R.string.movie_owner_mine), owner == MovieOwner.Mine, { owner = MovieOwner.Mine })
                TwinFilterChip(stringResource(R.string.movie_owner_partner), owner == MovieOwner.Partner, { owner = MovieOwner.Partner })
                TwinFilterChip(stringResource(R.string.movie_owner_together), owner == MovieOwner.Together, { owner = MovieOwner.Together })
            }
            Label(R.string.movie_add_review)
            NotesField(review, { review = it }, R.string.movie_add_review_hint)
            Label(R.string.movie_add_scene)
            NotesField(scene, { scene = it }, R.string.movie_add_scene_hint)
            GlassButton(
                stringResource(if (initial == null) R.string.movie_add_save else R.string.movie_edit_save),
                { onSave(MovieForm(title, year, genre, status, stars, owner, review, scene, posterBytes)) },
                Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s), enabled = title.isNotBlank(),
            )
        }
    }
}

/** The tappable poster box at the top of the form (web ".mc-poster-pick"). */
@Composable
private fun PosterPick(poster: String?, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        Modifier.fillMaxWidth().height(POSTER_BOX_DP.dp).clip(TwinTheme.shapes.card).background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (poster != null) {
            AsyncImage(poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.AddPhotoAlternate, null, tint = colors.inkMuted)
                Text(stringResource(R.string.movie_add_poster), color = colors.inkMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** A few lines of free text (the shared text field is single line). */
@Composable
private fun NotesField(value: String, onChange: (String) -> Unit, hint: Int) {
    val colors = TwinTheme.colors
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.take(MAX_NOTES)) }, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(hint), color = colors.inkFaint) }, minLines = NOTES_LINES,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.ink, unfocusedTextColor = colors.ink, cursorColor = colors.accent,
            focusedBorderColor = colors.accent, unfocusedBorderColor = colors.lineSoft,
        ),
    )
}

@Composable
private fun Label(text: Int) {
    Text(stringResource(text), color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.labelMedium)
}

private const val STARS = 5
private const val POSTER_BOX_DP = 160
private const val NOTES_LINES = 3
private const val MAX_NOTES = 500
private const val MAX_TITLE = 120
private const val YEAR_DIGITS = 4
