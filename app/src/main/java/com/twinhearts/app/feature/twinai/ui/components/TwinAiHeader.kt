package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.core.design.TwinTheme

/*
 * Why this file exists: the Twin page's own top bar. The shared TwinTopBar has no second line
 * ("Your Personal AI"), so this private copy adds it. A Change Request in SHARED_CHANGES.md
 * proposes adding a `subtitle` to TwinTopBar so this can be dropped later (R-31).
 */
@Composable
fun TwinAiHeader(
    subtitle: String,
    syncing: Boolean,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = TwinTheme.spacing.s, vertical = TwinTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s),
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.core_back),
            tint = colors.ink,
            modifier = Modifier
                .size(TwinTheme.shapes.minTouch)
                .clip(TwinTheme.shapes.circle)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(12.dp),
        )
        Image(
            painter = painterResource(R.drawable.core_logo),
            contentDescription = stringResource(R.string.core_logo),
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
        )
        SquareButton(Icons.Rounded.Menu, stringResource(R.string.core_menu), onMenu)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.twinai_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .size(TwinTheme.shapes.minTouch)
                .clip(TwinTheme.shapes.circle)
                .background(colors.glass)
                .border(1.dp, colors.lineSoft, TwinTheme.shapes.circle)
                .clickable(role = Role.Button, onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Search, stringResource(R.string.core_search), tint = colors.ink, modifier = Modifier.size(22.dp))
        }
        SyncButton(syncing, onSync)
    }
}

@Composable
private fun SquareButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    Box(
        Modifier
            .size(TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.small)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = colors.ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SyncButton(syncing: Boolean, onClick: () -> Unit) {
    val colors = TwinTheme.colors
    val reduce = rememberReduceMotion()
    val angle = if (syncing && !reduce) {
        val t = rememberInfiniteTransition(label = "sync")
        val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "syncAngle")
        a
    } else {
        0f
    }
    Row(
        Modifier
            .heightIn(min = TwinTheme.shapes.minTouch)
            .clip(TwinTheme.shapes.pill)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, TwinTheme.shapes.pill)
            .clickable(enabled = !syncing, role = Role.Button, onClick = onClick)
            .padding(horizontal = TwinTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xs),
    ) {
        Icon(
            Icons.Rounded.Sync, null, tint = colors.ink,
            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = angle },
        )
        Text(stringResource(R.string.core_sync), style = MaterialTheme.typography.titleSmall, color = colors.ink)
    }
}

/** YOU / BOTH switch. The chosen side is burgundy, as in the reference screenshot. */
@Composable
fun TwinModeTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TwinTheme.colors
    val pill = TwinTheme.shapes.pill
    Row(
        modifier = modifier
            .clip(pill)
            .background(colors.glass)
            .border(1.dp, colors.lineSoft, pill)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(pill)
                    .then(if (selected) Modifier.background(colors.brand) else Modifier)
                    .clickable(role = Role.Tab, onClick = { onSelect(index) })
                    .semantics { this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) colors.ink else colors.inkFaint,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
