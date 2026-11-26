package com.playcrastination.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.playcrastination.R
import com.playcrastination.domain.model.CompletionGoal
import com.playcrastination.domain.model.GameStatus
import com.playcrastination.domain.model.Priority
import java.text.NumberFormat

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier.clip(shape)
            .background(Brush.linearGradient(listOf(PlayColors.Surface, PlayColors.Panel)))
            .border(1.dp, PlayColors.Line, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(PlaySpacing.Gap),
        content = content
    )
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) Text(subtitle, color = PlayColors.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun LoadingContent() {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PlayColors.Cyan, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun EmptyPanel(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    Panel(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, color = PlayColors.Muted, style = MaterialTheme.typography.bodyMedium)
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
fun ErrorPanel(onRetry: () -> Unit) {
    EmptyPanel(
        title = stringResource(R.string.read_error),
        body = stringResource(R.string.read_error_description),
        action = stringResource(R.string.retry),
        onAction = onRetry
    )
}

@Composable
fun StatusTag(status: GameStatus, modifier: Modifier = Modifier) {
    val color = when {
        status.isCompleted -> PlayColors.Success
        status == GameStatus.PLAYING -> PlayColors.Cyan
        status == GameStatus.ON_HOLD -> PlayColors.Warning
        else -> PlayColors.LightViolet
    }
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(stringResource(status.label), color = color, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PlayProgress(percent: Int, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        percent.coerceIn(0, 100) / 100f,
        tween(ProgressAnimationMillis),
        label = "timeProgress"
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        color = PlayColors.Cyan,
        trackColor = PlayColors.Line,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

@Composable
fun formattedDuration(minutes: Long?): String {
    if (minutes == null) return stringResource(R.string.unknown_value)
    val formatter = NumberFormat.getNumberInstance(LocalConfiguration.current.locales[0])
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0L -> stringResource(R.string.minutes, formatter.format(remainder))
        remainder == 0L -> stringResource(R.string.hours, formatter.format(hours))
        else -> stringResource(R.string.hours_and_minutes, formatter.format(hours), formatter.format(remainder))
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val step = size.minDimension / 24
        val hexagon = Path().apply {
            moveTo(12 * step, step)
            lineTo(22 * step, 6.5f * step)
            lineTo(22 * step, 17.5f * step)
            lineTo(12 * step, 23 * step)
            lineTo(2 * step, 17.5f * step)
            lineTo(2 * step, 6.5f * step)
            close()
        }
        drawPath(hexagon, PlayColors.Violet, style = Stroke(1.4f * step))
        drawPath(Path().apply {
            moveTo(9 * step, 7 * step)
            lineTo(17 * step, 12 * step)
            lineTo(9 * step, 17 * step)
            close()
        }, PlayColors.Text)
        drawCircle(PlayColors.Cyan, 1.5f * step, Offset(19 * step, 4 * step))
    }
}

@Composable
fun OrbitArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width * 0.77f, size.height * 0.47f)
        val radius = size.minDimension * 0.72f
        drawCircle(
            Brush.radialGradient(listOf(PlayColors.Violet.copy(alpha = 0.36f), Color.Transparent), center, radius),
            radius, center
        )
        for (index in 1..5) {
            val orbit = radius * (0.2f + index * 0.16f)
            drawOval(
                color = if (index == 3) PlayColors.Cyan.copy(alpha = 0.39f) else PlayColors.LightViolet.copy(alpha = 0.26f),
                topLeft = Offset(center.x - orbit, center.y - orbit * 0.66f),
                size = Size(orbit * 2, orbit * 1.32f),
                style = Stroke(1.dp.toPx())
            )
        }
        drawLine(PlayColors.LightViolet.copy(alpha = 0.30f), Offset(center.x, 0f), Offset(center.x, size.height))
        drawLine(PlayColors.LightViolet.copy(alpha = 0.30f), Offset(0f, center.y), Offset(size.width, center.y))
        drawCircle(PlayColors.Cyan, 3.dp.toPx(), Offset(center.x + radius * 0.46f, center.y + radius * 0.31f))
        drawCircle(PlayColors.LightViolet, 2.dp.toPx(), Offset(center.x - radius * 0.48f, center.y - radius * 0.29f))
    }
}

@Composable
fun GameArtwork(url: String, title: String, modifier: Modifier = Modifier) {
    Box(
        modifier.background(Brush.linearGradient(listOf(Color(0xFF2B2051), PlayColors.Navigation))),
        contentAlignment = Alignment.Center
    ) {
        OrbitArtwork(Modifier.matchParentSize())
        Text(title.take(1).uppercase(), color = PlayColors.LightViolet, fontSize = 25.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        if (url.startsWith("https://", ignoreCase = true)) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@get:StringRes
val GameStatus.label: Int
    get() = when (this) {
        GameStatus.NOT_STARTED -> R.string.status_not_started
        GameStatus.PLAYING -> R.string.status_playing
        GameStatus.ON_HOLD -> R.string.status_on_hold
        GameStatus.MAIN_STORY_COMPLETED -> R.string.status_main_done
        GameStatus.MAIN_AND_EXTRAS_COMPLETED -> R.string.status_extras_done
        GameStatus.COMPLETED_100_PERCENT -> R.string.status_all_done
        GameStatus.ABANDONED -> R.string.status_abandoned
        GameStatus.WISHLIST -> R.string.status_wishlist
    }

@get:StringRes
val CompletionGoal.label: Int
    get() = when (this) {
        CompletionGoal.MAIN_STORY -> R.string.goal_main
        CompletionGoal.MAIN_AND_EXTRAS -> R.string.goal_extras
        CompletionGoal.COMPLETIONIST -> R.string.goal_all
    }

@get:StringRes
val Priority.label: Int
    get() = when (this) {
        Priority.LOW -> R.string.priority_low
        Priority.NORMAL -> R.string.priority_normal
        Priority.HIGH -> R.string.priority_high
    }
