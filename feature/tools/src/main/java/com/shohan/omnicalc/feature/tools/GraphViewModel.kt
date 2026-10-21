package com.shohan.omnicalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohan.omnicalc.core.math.CalcResult
import com.shohan.omnicalc.core.math.Engine
import com.shohan.omnicalc.core.math.EvalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Graph plotter (spec §10): samples y = f(x) through the shared engine at
 * 240 x-positions; null entries mark gaps where evaluation failed (the line
 * breaks there — never a fake straight segment). Pure computation, no
 * network, no eval().
 */
class GraphViewModel(
    private val errorMessage: (com.shohan.omnicalc.core.math.ErrorCode) -> String,
) : ViewModel() {

    data class PlotState(
        val expr: String = "x^2",
        val xMinText: String = "-10",
        val xMaxText: String = "10",
        /** Sampled points; null = undefined at that x (line break). */
        val samples: List<Pair<Double, Double>?> = emptyList(),
        val plotted: Int = 0,
        val skipped: Int = 0,
        val error: String? = null,
        val revision: Int = 0,
    )

    private val _ui = MutableStateFlow(PlotState())
    val ui: StateFlow<PlotState> = _ui.asStateFlow()

    fun setExpr(v: String) = _ui.update { it.copy(expr = v) }
    fun setXMin(v: String) = _ui.update { it.copy(xMinText = v) }
    fun setXMax(v: String) = _ui.update { it.copy(xMaxText = v) }

    fun plot() {
        val st = _ui.value
        val mc = MathContext(24, RoundingMode.HALF_UP)
        val min = parse(st.xMinText) ?: return fail(null)
        val max = parse(st.xMaxText) ?: return fail(null)
        if (min.compareTo(max) >= 0) return fail(null)
        if (st.expr.isBlank()) return fail(null)

        val n = 240
        val step = max.subtract(min).divide(BigDecimal(n), mc)
        val samples = ArrayList<Pair<Double, Double>?>(n + 1)
        var plotted = 0
        var skipped = 0
        var currentError: String? = null
        var x = min
        for (i in 0..n) {
            val result = Engine.evaluate(
                st.expr,
                EvalContext(variables = mapOf("x" to x)),
            )
            when (result) {
                is CalcResult.Success -> {
                    val xv = x.toDouble()
                    val yv = result.value.toDouble()
                    if (xv.isFinite() && yv.isFinite() && kotlin.math.abs(yv) < 1e308) {
                        samples.add(xv to yv)
                        plotted++
                    } else {
                        samples.add(null)
                        skipped++
                    }
                }
                is CalcResult.Failure -> {
                    samples.add(null)
                    skipped++
                    if (currentError == null) currentError = errorMessage(result.error.code)
                }
            }
            x = x.add(step, mc)
        }
        _ui.update {
            it.copy(
                samples = samples,
                plotted = plotted,
                skipped = skipped,
                // Only surface an error when NOTHING plotted (partial gaps are
                // shown as skipped count + break lines instead).
                error = if (plotted == 0) (currentError ?: it.error) else null,
                revision = it.revision + 1,
            )
        }
    }

    private fun fail(message: String?) = _ui.update {
        it.copy(error = message ?: it.error ?: " ", samples = emptyList(), plotted = 0, skipped = 0, revision = it.revision + 1)
    }

    private fun parse(text: String): BigDecimal? = try {
        BigDecimal(com.shohan.omnicalc.core.common.Numerals.normalizeToWesternDigits(text.trim()))
    } catch (e: NumberFormatException) {
        null
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application
                val ctx = app.applicationContext
                GraphViewModel { code ->
                    ctx.getString(com.shohan.omnicalc.core.localization.EngineErrorStrings.messageRes(code))
                }
            }
        }
    }
}
