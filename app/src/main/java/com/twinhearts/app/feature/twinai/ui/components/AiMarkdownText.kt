package com.twinhearts.app.feature.twinai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.MdBlock
import com.twinhearts.app.feature.twinai.domain.MdSpan
import com.twinhearts.app.feature.twinai.domain.TwinAiMarkdown
import kotlinx.coroutines.delay

/** Why this file exists: draws Twin's markdown answer (see [TwinAiMarkdown]) with the app's text styles. */
@Composable
fun AiMarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { TwinAiMarkdown.parse(text) }
    val colors = TwinTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> Text(styled(block.spans), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                is MdBlock.Heading -> Text(
                    styled(block.spans),
                    style = if (block.level == 3) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                )
                is MdBlock.Bullet -> Row {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge, color = colors.accent)
                    Text(styled(block.spans), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                }
                is MdBlock.Numbered -> Row {
                    Text("${block.number}.  ", style = MaterialTheme.typography.bodyLarge, color = colors.accent)
                    Text(styled(block.spans), style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                }
                is MdBlock.Quote -> Text(
                    styled(block.spans),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.inkMuted,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = TwinTheme.spacing.s),
                )
                is MdBlock.Code -> CodeBlock(block)
            }
        }
    }
}

@Composable
private fun CodeBlock(block: MdBlock.Code) {
    val colors = TwinTheme.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) { delay(1_500); copied = false }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background.copy(alpha = 0.6f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.glass)
                .padding(horizontal = TwinTheme.spacing.m, vertical = TwinTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(block.language, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
            Text(
                text = stringResource(if (copied) R.string.twinai_copied else R.string.twinai_copy),
                style = MaterialTheme.typography.labelSmall,
                color = colors.pinkSoft,
                modifier = Modifier.clickable {
                    clipboard.setText(AnnotatedString(block.code)); copied = true
                },
            )
        }
        Text(
            text = block.code,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(TwinTheme.spacing.m),
        )
    }
}

@Composable
private fun styled(spans: List<MdSpan>): AnnotatedString {
    val codeBg = TwinTheme.colors.glass
    val linkStyle = TextLinkStyles(SpanStyle(color = TwinTheme.colors.pinkSoft, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        spans.forEach { span ->
            if (span.link != null) {
                // Tapping opens the browser (Compose's default link handling).
                withLink(LinkAnnotation.Url(span.link, linkStyle)) { append(span.text) }
                return@forEach
            }
            val style = SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                fontFamily = if (span.code) FontFamily.Monospace else null,
                background = if (span.code) codeBg else Color.Unspecified,
            )
            withStyle(style) { append(span.text) }
        }
    }
}
