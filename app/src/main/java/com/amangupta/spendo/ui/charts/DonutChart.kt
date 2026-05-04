package com.amangupta.spendo.ui.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amangupta.spendo.data.CategorySpending
import java.util.*
import kotlin.math.*

val CATEGORY_COLORS = mapOf(
    "Food"          to Color(0xFFFF6B6B),
    "Groceries"     to Color(0xFF4ADE80),
    "Shopping"      to Color(0xFF818CF8),
    "Fuel"          to Color(0xFFFBBF24),
    "Travel"        to Color(0xFF38BDF8),
    "Subscription"  to Color(0xFFF472B6),
    "Entertainment" to Color(0xFFF472B6),
    "Health"        to Color(0xFF34D399),
    "Bills"         to Color(0xFFA78BFA),
    "Unknown"       to Color(0xFF9CA3AF),
    "Others"        to Color(0xFF6B7280)
)

fun categoryColor(name: String): Color =
    CATEGORY_COLORS[name] ?: Color(0xFF6B7280)

@Composable
fun DonutChart(
    data: List<CategorySpending>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val total = data.sumOf { it.total }.takeIf { it > 0 } ?: return
    val sweeps = data.map { (it.total / total * 360f).toFloat() }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label = "donut_anim"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        val outerR = size.width / 2f
                        val innerR = outerR * 0.55f
                        if (dist !in innerR..outerR) return@detectTapGestures
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0) angle += 360f
                        var cum = 0f
                        data.forEachIndexed { i, item ->
                            cum += sweeps[i]
                            if (angle <= cum) {
                                onCategoryClick(item.category)
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            val stroke = size.width * 0.15f
            val r = (size.width - stroke) / 2f
            val tl = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(r * 2f, r * 2f)
            var start = -90f

            data.forEachIndexed { i, item ->
                val sweep = sweeps[i] * animProgress
                val isSelected = item.category == selectedCategory
                val alpha = when {
                    selectedCategory == null -> 1f
                    isSelected -> 1f
                    else -> 0.25f
                }
                drawArc(
                    color = categoryColor(item.category).copy(alpha = alpha),
                    startAngle = start,
                    sweepAngle = (sweep - 2f).coerceAtLeast(0f),
                    useCenter = false,
                    topLeft = tl,
                    size = arcSize,
                    style = Stroke(width = if (isSelected) stroke * 1.15f else stroke)
                )
                start += sweep
            }
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val display = if (selectedCategory != null)
                data.find { it.category == selectedCategory }
            else null

            Text(
                text = "₹${String.format(Locale.getDefault(), "%,.0f", display?.total ?: total)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = display?.category ?: "total",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
