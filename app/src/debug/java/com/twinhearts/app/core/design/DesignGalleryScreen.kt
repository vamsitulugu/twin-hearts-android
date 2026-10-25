package com.twinhearts.app.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * DEBUG ONLY. One screen that shows every shared component, so you can check the look against
 * docs/reference/ in a few seconds. It is not part of release builds (it lives in src/debug).
 */
@Composable
fun DesignGalleryScreen(onBack: () -> Unit) {
    TwinBackground {
        Column(Modifier.fillMaxSize()) {
            TwinTopBar(title = "Design gallery", onBackClick = onBack, onSearchClick = {}, onSyncClick = {})
            GalleryBody()
        }
    }
}

@Composable
private fun GalleryBody() {
    val colors = TwinTheme.colors
    var tab by rememberSaveable { mutableStateOf(0) }
    var chip by rememberSaveable { mutableStateOf(0) }
    var switchOn by rememberSaveable { mutableStateOf(true) }
    var text by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TwinTheme.spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.xl),
    ) {
        item {
            Column {
                Text("Stay Connected", style = MaterialTheme.typography.displaySmall, color = colors.pinkSoft)
                Text("Body text in Inter, titles in DM Serif Display.", style = MaterialTheme.typography.bodyLarge, color = colors.inkMuted)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                SectionHeader("Colours", actionLabel = "See all", onAction = {})
                LazyRow(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    val swatches = listOf(colors.background, colors.card, colors.accent, colors.primaryStart, colors.primaryEnd, colors.brand, colors.ink, colors.neutralGlass, colors.pinkSoft, colors.orchid, colors.error, colors.success)
                    items(swatches.size) { i ->
                        Box(Modifier.size(44.dp).clip(CircleShape).background(swatches[i]))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                SectionHeader("Buttons")
                GlassButton("Primary", onClick = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Favorite)
                GlassButton("Secondary", onClick = {}, modifier = Modifier.fillMaxWidth(), style = GlassButtonStyle.Secondary)
                GlassButton("Ghost", onClick = {}, modifier = Modifier.fillMaxWidth(), style = GlassButtonStyle.Ghost)
                GlassButton("Loading", onClick = {}, modifier = Modifier.fillMaxWidth(), loading = true)
                GlassButton("Disabled", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
                GlassIconButton(Icons.Rounded.Favorite, "Favourite", onClick = {})
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                SectionHeader("Inputs")
                SegmentedTabs(listOf("Sign In", "Sign Up"), tab, { tab = it })
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.s)) {
                    listOf("Hug", "Touch", "Miss You").forEachIndexed { i, label ->
                        TwinFilterChip(label, selected = chip == i, onClick = { chip = i })
                    }
                }
                PillSwitch(checked = switchOn, onCheckedChange = { switchOn = it })
                TwinTextField(text, { text = it }, label = "Your Name", placeholder = "Your name", leadingIcon = Icons.Rounded.Person)
                TwinTextField(password, { password = it }, label = "Password", placeholder = "Your password", isPassword = true)
                TwinTextField("x", {}, label = "With error", error = "This is an error message")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                SectionHeader("Cards and tiles")
                GlassCard(Modifier.fillMaxWidth()) { Text("Glass card", color = colors.ink, style = MaterialTheme.typography.titleMedium) }
                Row(horizontalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                    StatTile("12", "Days together", Modifier.weight(1f), icon = Icons.Rounded.Favorite)
                    StatTile("3", "Goals", Modifier.weight(1f))
                }
                CoupleAvatars("Vamsi", "Partner")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m)) {
                SectionHeader("States")
                LoadingSkeleton()
                EmptyState(title = "No messages yet", message = "Say hi to your partner.")
                ErrorState(message = "Can't reach the server. Check your internet and try again.", onRetry = {})
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E)
@Composable
private fun DesignGalleryPreview() {
    TwinTheme { DesignGalleryScreen(onBack = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, fontScale = 1.3f)
@Composable
private fun DesignGalleryLargeFontPreview() {
    TwinTheme { DesignGalleryScreen(onBack = {}) }
}
