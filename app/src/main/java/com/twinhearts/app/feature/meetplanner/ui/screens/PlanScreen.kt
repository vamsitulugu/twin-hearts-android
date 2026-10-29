package com.twinhearts.app.feature.meetplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassCard
import com.twinhearts.app.core.design.GlassIconButton
import com.twinhearts.app.core.design.SegmentedTabs
import com.twinhearts.app.core.design.TwinBackground
import com.twinhearts.app.core.design.TwinTextField
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.TripType
import com.twinhearts.app.feature.meetplanner.ui.CitySuggest
import com.twinhearts.app.feature.meetplanner.ui.MeetActions
import com.twinhearts.app.feature.meetplanner.ui.MeetUiState
import com.twinhearts.app.feature.meetplanner.ui.components.MeetDateField
import com.twinhearts.app.feature.meetplanner.ui.components.MeetHeader
import com.twinhearts.app.feature.meetplanner.ui.components.MeetTextArea
import com.twinhearts.app.feature.meetplanner.ui.components.ModeRow
import java.time.LocalDate
import androidx.compose.ui.unit.dp
/** Why this file exists: "Plan a New Meetup": name, city, dates, trip type, budget, travel mode, notes. */
@Composable
internal fun PlanScreen(state: MeetUiState, actions: MeetActions) {
    val colors = TwinTheme.colors
    val spacing = TwinTheme.spacing
    val d = state.draft
    Column(Modifier.fillMaxSize()) {
        MeetHeader(
            title = stringResource(R.string.meetplanner_plan_title),
            subtitle = stringResource(R.string.meetplanner_plan_subtitle),
            onBack = actions::back,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenEdge)
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            TwinTextField(
                value = d.title,
                onValueChange = actions::setTitle,
                label = stringResource(R.string.meetplanner_field_name),
                placeholder = stringResource(R.string.meetplanner_field_name_hint),
                maxLength = 80,
            )

            val city = d.city
            if (city == null) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                        TwinTextField(
                            value = state.cityQuery,
                            onValueChange = actions::onCityQuery,
                            label = stringResource(R.string.meetplanner_field_where),
                            placeholder = stringResource(R.string.meetplanner_field_city_hint),
                            leadingIcon = Icons.Rounded.Place,
                            imeAction = ImeAction.Search,
                            modifier = Modifier.weight(1f),
                        )
                        GlassIconButton(Icons.Rounded.MyLocation, stringResource(R.string.meetplanner_use_location), actions::useMyLocation)
                    }
                    CitySuggestions(state.citySuggest, actions::pickCity)
                }
            } else {
                Column {
                    Text(stringResource(R.string.meetplanner_field_where), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                    Spacer(Modifier.height(spacing.xs))
                    GlassCard(Modifier.fillMaxWidth(), contentPadding = spacing.m) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
                            Icon(Icons.Rounded.Place, contentDescription = null, tint = colors.accent)
                            Text(city.name, style = MaterialTheme.typography.titleSmall, color = colors.ink, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.meetplanner_change),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.accent,
                                modifier = Modifier.clickable(onClick = actions::changeCity).padding(spacing.xs),
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(stringResource(R.string.meetplanner_field_when), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    MeetDateField(stringResource(R.string.meetplanner_from), d.from, actions::setFrom, Modifier.weight(1f))
                    MeetDateField(stringResource(R.string.meetplanner_to), d.to, actions::setTo, Modifier.weight(1f))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(stringResource(R.string.meetplanner_field_trip_type), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                SegmentedTabs(
                    options = listOf(
                        stringResource(R.string.meetplanner_trip_day),
                        stringResource(R.string.meetplanner_trip_weekend),
                        stringResource(R.string.meetplanner_trip_long),
                    ),
                    selectedIndex = TripType.entries.indexOf(d.tripType),
                    onSelect = { actions.setTripType(TripType.entries[it]) },
                )
            }

            TwinTextField(
                value = d.budget,
                onValueChange = actions::setBudget,
                label = stringResource(R.string.meetplanner_field_budget),
                placeholder = stringResource(R.string.meetplanner_field_budget_hint),
                keyboardType = KeyboardType.Decimal,
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(stringResource(R.string.meetplanner_field_mode), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted)
                ModeRow(d.mode, actions::setMode)
            }

            MeetTextArea(
                value = d.notes,
                onValueChange = actions::setNotes,
                label = stringResource(R.string.meetplanner_field_notes),
                placeholder = stringResource(R.string.meetplanner_field_notes_hint),
            )

            GlassButton(
                text = stringResource(R.string.meetplanner_create_plan),
                onClick = actions::createPlan,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CitySuggestions(suggest: CitySuggest, onPick: (City) -> Unit) {
    val colors = TwinTheme.colors
    val shape = TwinTheme.shapes.field
    if (suggest == CitySuggest.Idle) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = TwinTheme.spacing.s)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape),
    ) {
        when (suggest) {
            CitySuggest.Loading -> Text(stringResource(R.string.meetplanner_searching), color = colors.inkFaint, modifier = Modifier.padding(TwinTheme.spacing.l))
            CitySuggest.Empty -> Text(stringResource(R.string.meetplanner_no_cities), color = colors.inkFaint, modifier = Modifier.padding(TwinTheme.spacing.l))
            CitySuggest.Failed -> Text(stringResource(R.string.meetplanner_city_search_failed), color = colors.error, modifier = Modifier.padding(TwinTheme.spacing.l))
            is CitySuggest.Results -> suggest.cities.forEach { c ->
                Column(Modifier.fillMaxWidth().clickable { onPick(c) }.padding(TwinTheme.spacing.m)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    Text(c.display, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint, maxLines = 1)
                }
            }
            CitySuggest.Idle -> Unit
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun PlanPreview() = TwinTheme {
    TwinBackground { PlanScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()), citySuggest = CitySuggest.Results(listOf(City("Goa", "Goa, India", 15.3, 74.1)))), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun PlanCityChosenLargeFontPreview() = TwinTheme {
    TwinBackground { PlanScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()).copy(city = City("Goa", "Goa, India", 15.3, 74.1))), MeetActions.None) }
}

@Preview(showBackground = true, backgroundColor = 0xFF14030E, widthDp = 360, heightDp = 780)
@Composable
private fun PlanCityErrorPreview() = TwinTheme {
    TwinBackground { PlanScreen(MeetUiState(draft = PlanDraft.fresh(LocalDate.now()), cityQuery = "Gx", citySuggest = CitySuggest.Failed), MeetActions.None) }
}
