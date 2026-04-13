package com.apislens.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.*
import com.apislens.data.local.entity.ChargeRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChargeHistoryChart(records: List<ChargeRecord>, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    AndroidView(factory = { context ->
        LineChart(context).apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.parseColor("#333333")
            setLayerType(android.view.View.LAYER_TYPE_NONE, null)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }, update = { chart ->
        val entries = records.mapIndexed { index, record -> Entry(index.toFloat(), record.endLevel?.toFloat() ?: 0f) }
        val dataSet = LineDataSet(entries, "电量 (%)").apply {
            color = Color.parseColor("#FF6600")
            setCircleColor(Color.parseColor("#FF6600"))
            circleRadius = 4f
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        chart.data = LineData(dataSet)
        chart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(records.map { dateFormat.format(Date(it.startTime)) })
        chart.invalidate()
    }, modifier = modifier.height(200.dp))
}

@Composable
fun UsageTimeChart(usageMinutes: List<Pair<Long, Int>>, modifier: Modifier = Modifier) {
    AndroidView(factory = { context ->
        LineChart(context).apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            setBackgroundColor(Color.TRANSPARENT)
        }
    }, modifier = modifier.height(200.dp))
}
