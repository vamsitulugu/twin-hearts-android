package com.twinhearts.app.feature.help.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinhearts.app.core.design.TwinTheme

/*
 * Small building blocks used by more than one Help screen. They stay inside this feature
 * (R-31): the pink-filled tab look of the web Help page is not one of the shared components.
 */

/** A 1 dp line along the bottom edge (the web app bar and chat header borders). */
internal fun Modifier.bottomBorder(color: Color): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawLine(color, Offset(0f, size.height - stroke / 2f), Offset(size.width, size.height - stroke / 2f), stroke)
}

/** A 1 dp line along the top edge (the web chat input bar border). */
internal fun Modifier.topBorder(color: Color): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawLine(color, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
}

/** The dashed line between a question and its answer (web `.faq-a` border-top: dashed). */
internal fun Modifier.dashedTopBorder(color: Color): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawLine(
        color = color,
        start = Offset(0f, stroke / 2f),
        end = Offset(size.width, stroke / 2f),
        strokeWidth = stroke,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
    )
}

/**
 * Tabs whose selected one is filled with the burgundy-pink gradient, as in the web Help page
 * (reference screenshot). [compact] is the smaller boxed version used for the AI / Live Agent toggle.
 */
@Composable
internal fun HelpPillTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = TwinTheme.colors
    val shapes = TwinTheme.shapes
    val outer = if (compact) shapes.small else shapes.field
    val inner = if (compact) RoundedCornerShape(9.dp) else shapes.field
    val container = if (compact) {
        Modifier
            .clip(outer)
            .background(colors.card)
            .border(1.dp, colors.line, outer)
            .padding(3.dp)
    } else {
        Modifier
    }
    Row(
        modifier = modifier.then(container),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else TwinTheme.spacing.s),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = if (compact) 36.dp else 44.dp)
                    .clip(inner)
                    .then(if (selected) Modifier.background(colors.primaryBrush) else Modifier)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = TwinTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                    color = if (selected) colors.ink else colors.inkMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
