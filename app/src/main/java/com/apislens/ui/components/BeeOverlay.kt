package com.apislens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.*
import kotlin.random.Random

data class BeeState(
    val id: Long,
    val x: Float,
    val y: Float,
    val angle: Float,
    val speed: Float,
    val targetAngle: Float,
    val trail: List<Triple<Float, Float, Long>>,
    val birthTime: Long
)

@Composable
fun BeeOverlay(
    beeCount: Int,
    modifier: Modifier = Modifier
) {
    if (beeCount <= 0) return

    val density = LocalDensity.current
    val cellSize = with(density) { 20.dp.toPx() }

    var bees by remember { mutableStateOf(listOf<BeeState>()) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(beeCount) {
        while (true) {
            awaitFrame()
            val now = System.nanoTime() / 1_000_000
            val dt = if (lastFrameTime == 0L) 16f else (now - lastFrameTime).coerceAtMost(50).toFloat()
            lastFrameTime = now

            bees = bees.toMutableList().also { list ->
                while (list.size < beeCount) {
                    list.add(BeeState(
                        id = Random.nextLong(),
                        x = Random.nextFloat() * 800f,
                        y = Random.nextFloat() * 1600f,
                        angle = Random.nextFloat() * 2f * PI.toFloat(),
                        speed = 80f + Random.nextFloat() * 120f,
                        targetAngle = Random.nextFloat() * 2f * PI.toFloat(),
                        trail = emptyList(),
                        birthTime = now
                    ))
                }
                while (list.size > beeCount) {
                    list.removeAt(0)
                }

                for (i in list.indices) {
                    val bee = list[i]
                    val age = now - bee.birthTime
                    if (age > 2000 && Random.nextFloat() < 0.02f) {
                        list[i] = bee.copy(targetAngle = Random.nextFloat() * 2f * PI.toFloat())
                        continue
                    }

                    var angleDiff = bee.targetAngle - bee.angle
                    while (angleDiff > PI.toFloat()) angleDiff -= 2f * PI.toFloat()
                    while (angleDiff < -PI.toFloat()) angleDiff += 2f * PI.toFloat()
                    val newAngle = bee.angle + angleDiff * 3f * (dt / 1000f)
                    val newX = (bee.x + cos(newAngle) * bee.speed * (dt / 1000f)).coerceIn(0f, 2000f)
                    val newY = (bee.y + sin(newAngle) * bee.speed * (dt / 1000f)).coerceIn(0f, 4000f)

                    val bounced = newX == bee.x || newY == bee.y
                    val finalAngle = if (bounced) newAngle + PI.toFloat() else newAngle

                    val newTrail = (bee.trail + Triple(newX, newY, now))
                        .filter { now - it.third < 3000L }

                    list[i] = bee.copy(
                        x = newX,
                        y = newY,
                        angle = finalAngle,
                        trail = newTrail
                    )
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val now = System.nanoTime() / 1_000_000
        for (bee in bees) {
            drawBeeTrail(bee, now)
            drawBee(bee, cellSize)
        }
    }
}

private fun DrawScope.drawBee(bee: BeeState, cellSize: Float) {
    val size = cellSize * 0.5f
    val cx = bee.x
    val cy = bee.y
    val angle = bee.angle

    rotate(degrees = (angle * 180f / PI.toFloat()) - 90f, pivot = Offset(cx, cy)) {
        drawOval(
            color = Color(0xFFD4A017),
            topLeft = Offset(cx - size * 0.5f, cy - size * 0.35f),
            size = Size(size, size * 0.7f)
        )
        drawLine(
            color = Color.Black,
            start = Offset(cx - size * 0.5f, cy - size * 0.1f),
            end = Offset(cx + size * 0.5f, cy - size * 0.1f),
            strokeWidth = size * 0.08f
        )
        drawLine(
            color = Color.Black,
            start = Offset(cx - size * 0.5f, cy + size * 0.1f),
            end = Offset(cx + size * 0.5f, cy + size * 0.1f),
            strokeWidth = size * 0.08f
        )
        drawOval(
            color = Color(0xFF4FC3F7).copy(alpha = 0.6f),
            topLeft = Offset(cx - size * 0.3f, cy - size * 0.9f),
            size = Size(size * 0.5f, size * 0.6f)
        )
        drawOval(
            color = Color(0xFF4FC3F7).copy(alpha = 0.6f),
            topLeft = Offset(cx - size * 0.2f, cy + size * 0.3f),
            size = Size(size * 0.5f, size * 0.6f)
        )
    }
}

private fun DrawScope.drawBeeTrail(bee: BeeState, now: Long) {
    val trail = bee.trail
    if (trail.size < 2) return

    for (i in 1 until trail.size) {
        val age = now - trail[i].third
        val alpha = (1f - age / 3000f).coerceIn(0f, 1f) * 0.4f
        drawLine(
            color = Color(0xFFD4A017).copy(alpha = alpha),
            start = Offset(trail[i - 1].first, trail[i - 1].second),
            end = Offset(trail[i].first, trail[i].second),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
