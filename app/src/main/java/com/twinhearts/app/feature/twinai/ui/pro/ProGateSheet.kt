package com.twinhearts.app.feature.twinai.ui.pro

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinhearts.app.R
import com.twinhearts.app.core.design.GlassButton
import com.twinhearts.app.core.design.GlassButtonStyle
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.twinai.domain.model.ProPayment
import com.twinhearts.app.feature.twinai.domain.model.ProPlan
import com.twinhearts.app.feature.twinai.ui.components.GlassTextArea

/** Why this file exists: the Go Pro bottom sheet (plans, pay, send proof, done) shown before BOTH opens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProGateSheet(state: ProGateState, vm: ProGateViewModel) {
    if (state.step == ProStep.Hidden) return
    val colors = TwinTheme.colors
    ModalBottomSheet(
        onDismissRequest = vm::dismiss,
        containerColor = colors.card,
        contentColor = colors.ink,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = TwinTheme.spacing.l, vertical = TwinTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(TwinTheme.spacing.m),
        ) {
            when (state.step) {
                ProStep.Plans -> PlansStep(state, vm)
                ProStep.Pay -> PayStep(state, vm)
                ProStep.Form -> FormStep(state, vm)
                ProStep.Success -> SuccessStep(vm)
                ProStep.Hidden -> Unit
            }
        }
    }
}

@Composable
private fun PlansStep(state: ProGateState, vm: ProGateViewModel) {
    val colors = TwinTheme.colors
    Text(stringResource(R.string.twinai_pro_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.ink)
    Text(stringResource(R.string.twinai_pro_sub), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
    ProPlan.entries.forEach { plan ->
        val selected = plan == state.plan
        Row(
            Modifier
                .fillMaxWidth()
                .clip(TwinTheme.shapes.card)
                .background(if (selected) colors.brand.copy(alpha = 0.25f) else colors.glass)
                .border(1.dp, if (selected) colors.accent else colors.lineSoft, TwinTheme.shapes.card)
                .clickable { vm.selectPlan(plan) }
                .padding(TwinTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(if (plan == ProPlan.Monthly) R.string.twinai_pro_monthly else R.string.twinai_pro_lifetime), style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Text(stringResource(if (plan == ProPlan.Monthly) R.string.twinai_pro_monthly_note else R.string.twinai_pro_lifetime_note), style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            }
            Text("₹${plan.amount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.pinkSoft)
        }
    }
    GlassButton(stringResource(R.string.twinai_pro_continue), vm::goToPay, Modifier.fillMaxWidth())
    GlassButton(stringResource(R.string.twinai_pro_later), vm::dismiss, Modifier.fillMaxWidth(), GlassButtonStyle.Ghost)
}

@Composable
private fun PayStep(state: ProGateState, vm: ProGateViewModel) {
    val colors = TwinTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val label = stringResource(if (state.plan == ProPlan.Monthly) R.string.twinai_pro_monthly else R.string.twinai_pro_lifetime)
    Text("₹${state.plan.amount}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = colors.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(stringResource(R.string.twinai_pro_pay_note, state.plan.amount), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text("UPI: ${ProPayment.UPI_ID} · ${ProPayment.UPI_NAME}", style = MaterialTheme.typography.bodySmall, color = colors.inkFaint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    GlassButton(
        stringResource(R.string.twinai_pro_pay_upi),
        {
            val link = ProPayment.upiLink(state.plan, state.coupleId, label)
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, R.string.twinai_pro_no_upi_app, Toast.LENGTH_SHORT).show()
            }
        },
        Modifier.fillMaxWidth(),
    )
    GlassButton(
        stringResource(R.string.twinai_pro_copy_upi),
        {
            clipboard.setText(AnnotatedString(ProPayment.UPI_ID))
            Toast.makeText(context, R.string.twinai_pro_upi_copied, Toast.LENGTH_SHORT).show()
        },
        Modifier.fillMaxWidth(),
        GlassButtonStyle.Secondary,
    )
    GlassButton(stringResource(R.string.twinai_pro_paid), vm::paid, Modifier.fillMaxWidth())
    GlassButton(stringResource(R.string.twinai_pro_back), vm::back, Modifier.fillMaxWidth(), GlassButtonStyle.Ghost)
}

@Composable
private fun FormStep(state: ProGateState, vm: ProGateViewModel) {
    val colors = TwinTheme.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> vm.onProof(uri?.toString()) }
    Text(stringResource(R.string.twinai_pro_form_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.ink)
    Field(R.string.twinai_pro_f_name, state.form.paidName) { v -> vm.onForm { it.copy(paidName = v) } }
    Field(R.string.twinai_pro_f_phone, state.form.paidPhone) { v -> vm.onForm { it.copy(paidPhone = v) } }
    Field(R.string.twinai_pro_f_partner_name, state.form.partnerName) { v -> vm.onForm { it.copy(partnerName = v) } }
    Field(R.string.twinai_pro_f_partner_phone, state.form.partnerPhone) { v -> vm.onForm { it.copy(partnerPhone = v) } }
    Field(R.string.twinai_pro_f_txn, state.form.transactionId) { v -> vm.onForm { it.copy(transactionId = v) } }
    GlassButton(
        stringResource(if (state.proofUri == null) R.string.twinai_pro_f_proof else R.string.twinai_pro_f_proof_chosen),
        { picker.launch("image/*") },
        Modifier.fillMaxWidth(),
        GlassButtonStyle.Secondary,
    )
    Field(R.string.twinai_pro_f_note, state.form.note) { v -> vm.onForm { it.copy(note = v) } }
    GlassButton(stringResource(R.string.twinai_pro_send), vm::submit, Modifier.fillMaxWidth(), loading = state.submitting)
    GlassButton(stringResource(R.string.twinai_pro_back), vm::back, Modifier.fillMaxWidth(), GlassButtonStyle.Ghost)
}

@Composable
private fun Field(label: Int, value: String, onChange: (String) -> Unit) {
    GlassTextArea(value = value, onValueChange = onChange, hint = stringResource(label), maxLines = 1)
}

@Composable
private fun SuccessStep(vm: ProGateViewModel) {
    val colors = TwinTheme.colors
    Text("✅", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(stringResource(R.string.twinai_pro_done_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(stringResource(R.string.twinai_pro_done_sub), style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    GlassButton(stringResource(R.string.twinai_pro_ok), vm::dismiss, Modifier.fillMaxWidth())
}
