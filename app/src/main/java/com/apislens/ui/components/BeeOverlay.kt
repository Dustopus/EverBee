package com.apislens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
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
    modifier: Modifier = Modifier,
    spawnX: Float? = null,
    spawnY: Float? = null
) {
    if (beeCount <= 0) return

    val density = LocalDensity.current
    val cellSize = with(density) { 20.dp.toPx() }

    val topBarHeightPx = with(density) { 64.dp.toPx() }
    val bottomNavHeightPx = with(density) { 80.dp.toPx() }

    var bees by remember { mutableStateOf(listOf<BeeState>()) }
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    val minX = 0f
    val minY = topBarHeightPx
    val maxX = canvasWidth.coerceAtLeast(100f)
    val maxY = (canvasHeight - bottomNavHeightPx).coerceAtLeast(minY + 100f)

    val spawnPosX = spawnX
    val spawnPosY = spawnY

    LaunchedEffect(beeCount) {
        while (true) {
            awaitFrame()
            val now = System.nanoTime() / 1_000_000
            val dt = if (lastFrameTime == 0L) 16f else (now - lastFrameTime).coerceAtMost(50).toFloat()
            lastFrameTime = now

            bees = bees.toMutableList().also { list ->
                while (list.size < beeCount) {
                    val sx = spawnPosX ?: Random.nextFloat() * maxX.coerceAtLeast(100f)
                    val sy = spawnPosY ?: (minY + Random.nextFloat() * (maxY - minY).coerceAtLeast(100f))
                    list.add(BeeState(
                        id = Random.nextLong(),
                        x = sx,
                        y = sy,
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

                    val rawX = bee.x + cos(newAngle) * bee.speed * (dt / 1000f)
                    val rawY = bee.y + sin(newAngle) * bee.speed * (dt / 1000f)

                    val bounced = rawX < minX || rawX > maxX || rawY < minY || rawY > maxY
                    val newX = rawX.coerceIn(minX, maxX)
                    val newY = rawY.coerceIn(minY, maxY)

                    val finalAngle = if (bounced) {
                        var bounceAngle = newAngle + PI.toFloat()
                        if (rawX < minX || rawX > maxX) {
                            bounceAngle = PI.toFloat() - newAngle
                        }
                        if (rawY < minY || rawY > maxY) {
                            bounceAngle = -newAngle
                        }
                        bounceAngle
                    } else newAngle

                    val newTargetAngle = if (bounced) {
                        finalAngle + (Random.nextFloat() - 0.5f) * PI.toFloat()
                    } else bee.targetAngle

                    val newTrail = (bee.trail + Triple(newX, newY, now))
                        .filter { now - it.third < 3000L }

                    list[i] = bee.copy(
                        x = newX,
                        y = newY,
                        angle = finalAngle,
                        targetAngle = newTargetAngle,
                        trail = newTrail
                    )
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        canvasWidth = size.width
        canvasHeight = size.height
        val now = System.nanoTime() / 1_000_000
        for (bee in bees) {
            drawBeeTrail(bee, now)
            drawBee(bee, cellSize)
        }
    }
}

private fun DrawScope.drawBee(bee: BeeState, cellSize: Float) {
    val s = cellSize * 0.5f
    val cx = bee.x
    val cy = bee.y
    val angle = bee.angle

    rotate(degrees = (angle * 180f / PI.toFloat()), pivot = Offset(cx, cy)) {
        val bodyLen = s * 1.2f
        val headLen = s * 0.4f
        val tailLen = s * 0.35f

        drawOval(
            color = Color(0xFFD4A017),
            topLeft = Offset(cx - bodyLen * 0.5f, cy - s * 0.3f),
            size = Size(bodyLen, s * 0.6f)
        )

        drawOval(
            color = Color(0xFF2C2C2C),
            topLeft = Offset(cx - bodyLen * 0.5f + bodyLen * 0.25f, cy - s * 0.3f),
            size = Size(bodyLen * 0.2f, s * 0.6f)
        )

        drawOval(
            color = Color(0xFF2C2C2C),
            topLeft = Offset(cx - bodyLen * 0.5f + bodyLen * 0.6f, cy - s * 0.3f),
            size = Size(bodyLen * 0.2f, s * 0.6f)
        )

        drawOval(
            color = Color(0xFF2C2C2C),
            topLeft = Offset(cx + bodyLen * 0.5f - headLen, cy - s * 0.22f),
            size = Size(headLen, s * 0.44f)
        )

        drawOval(
            color = Color(0xFF1A1A1A),
            topLeft = Offset(cx - bodyLen * 0.5f - tailLen * 0.3f, cy - s * 0.18f),
            size = Size(tailLen, s * 0.36f)
        )

        val wingW = s * 0.6f
        val wingH = s * 0.35f

        drawOval(
            color = Color(0xFF4FC3F7).copy(alpha = 0.55f),
            topLeft = Offset(cx - bodyLen * 0.1f, cy - s * 0.3f - wingH),
            size = Size(wingW, wingH)
        )
        drawOval(
            color = Color(0xFF4FC3F7).copy(alpha = 0.55f),
            topLeft = Offset(cx - bodyLen * 0.1f, cy + s * 0.3f + wingH * 0.15f),
            size = Size(wingW, wingH)
        )

        drawOval(
            color = Color(0xFF4FC3F7).copy(alpha = 0.4f),
            topLeft = Offset(cx + bodyLen * 0.15f, cy - s * 0.28f - wingH * 0.65f),
            size = Size(wingW * 0.55f, wingH * 0.65f)
        )
        drawOval(
            color = Color(0xFF4FC3F7).copy(alpha = 0.4f),
            topLeft = Offset(cx + bodyLen * 0.15f, cy + s * 0.28f + wingH * 0.15f),
            size = Size(wingW * 0.55f, wingH * 0.65f)
        )
    }
}

private fun DrawScope.drawBeeTrail(bee: BeeState, now: Long) {
    val trail = bee.trail
    if (trail.size < 2) return

    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    for (i in 1 until trail.size) {
        val age = now - trail[i].third
        val alpha = (1f - age / 3000f).coerceIn(0f, 1f) * 0.7f
        drawLine(
            color = Color(0xFFD4A017).copy(alpha = alpha),
            start = Offset(trail[i - 1].first, trail[i - 1].second),
            end = Offset(trail[i].first, trail[i].second),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = dashEffect
        )
    }
}
