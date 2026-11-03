package com.twinhearts.app.feature.rewards.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.twinhearts.app.R
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.feature.rewards.domain.CoinEarner
import com.twinhearts.app.feature.rewards.domain.EarnAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinEarnerImpl @Inject constructor(
    private val repository: RewardsRepository,
    @ApplicationContext private val context: Context,
) : CoinEarner {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())

    override fun earn(action: EarnAction) {
        scope.launch {
            val r = repository.earn(action)
            if (r is AppResult.Success && r.data.earned > 0) {
                main.postDelayed(
                    { Toast.makeText(context, context.getString(R.string.rewards_earn_toast, r.data.earned), Toast.LENGTH_SHORT).show() },
                    700,
                )
            }
        }
    }
}
