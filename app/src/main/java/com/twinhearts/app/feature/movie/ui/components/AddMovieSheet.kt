package com.twinhearts.app.feature.movie.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * Why this file exists: the "Add Movie" form of the web page. It saves into the same shared list as
 * "Search online". Poster upload, personal review and favourite scene are not here: the server list
 * has no place to keep them (see docs/features/movie.md).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovieSheet(
    onSave: (title: String, year: String, genre: String?, status: WatchStatus, stars: Int, owner: MovieOwner) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = TwinTheme.colors
    var title by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf(WatchStatus.Watchlist) }
    var stars by rememberSaveable { mutableIntStateOf(0) }
    var owner by rememberSaveable { mutableStateOf(MovieOwner.Mine) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.card) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.screenEdge, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            Text(stringResource(R.string.movie_add_title), color = colors.ink, style = MaterialTheme.typography.headlineSmall, fontFamily = SerifFamily)
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
            GlassButton(
                stringResource(R.string.movie_add_save), { onSave(title, year, genre, status, stars, owner) },
                Modifier.fillMaxWidth().padding(top = TwinTheme.spacing.s), enabled = title.isNotBlank(),
            )
        }
    }
}

@Composable
private fun Label(text: Int) {
    Text(stringResource(text), color = TwinTheme.colors.inkMuted, style = MaterialTheme.typography.labelMedium)
}

private const val STARS = 5
private const val MAX_TITLE = 120
private const val YEAR_DIGITS = 4
