package com.twinhearts.app.core.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R

/**
 * Top bar: back / menu, logo, title, then search and Sync on the right.
 * It pads itself for the status bar, so screens do not handle the top inset (R-56).
 * Pass only the actions a screen needs; the rest are hidden.
 */
@Composable
fun TwinTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    showLogo: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    /** Show the web-style hamburger that opens the side menu (when [onMenuClick] is not given). */
    showSidebarButton: Boolean = false,
    onSearchClick: (() -> Unit)? = null,
    onSyncClick: (() -> Unit)? = null,
) {
    val colors = TwinTheme.colors
    val openSidebar = LocalOpenSidebar.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            BarIcon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.core_back), onBackClick)
        }
        if (showLogo) {
            Image(
                painter = painterResource(R.drawable.core_logo),
                contentDescription = stringResource(R.string.core_logo),
                modifier = Modifier
                    .padding(horizontal = TwinTheme.spacing.s)
                    .size(36.dp),
            )
        }
        if (onMenuClick != null) {
            BarIcon(Icons.Rounded.Menu, stringResource(R.string.core_menu), onMenuClick)
        } else if (showSidebarButton) {
            BarIcon(Icons.Rounded.Menu, stringResource(R.string.core_menu), openSidebar)
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TwinTheme.spacing.s),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (onSearchClick != null) {
            BarIcon(Icons.Rounded.Search, stringResource(R.string.core_search), onSearchClick)
        }
        if (onSyncClick != null) {
            BarIcon(Icons.Rounded.Sync, stringResource(R.string.core_sync), onSyncClick)
        }
    }
}

@Composable
private fun RowScope.BarIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = TwinTheme.colors.ink)
    }
}
