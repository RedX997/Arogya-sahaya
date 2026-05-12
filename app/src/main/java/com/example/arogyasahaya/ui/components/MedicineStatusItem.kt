package com.example.arogyasahaya.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.arogyasahaya.data.local.entity.Medicine
import com.example.arogyasahaya.ui.theme.HealthBlue
import com.example.arogyasahaya.ui.theme.HealthGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MedicineStatusItem(medicine: Medicine, onTakenClick: () -> Unit, formatter: SimpleDateFormat) {
    val timeStr = remember(medicine.time) { formatter.format(Date(medicine.time)) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            shadowElevation = 4.dp.toPx()
            shape = RoundedCornerShape(20.dp)
        },
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, onSurfaceColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (medicine.isTaken) HealthGreen.copy(alpha = 0.1f) else HealthBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (medicine.pillImageUri != null) {
                    AsyncImage(
                        model = medicine.pillImageUri,
                        contentDescription = "Pill Photo",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (medicine.isTaken) Icons.Default.CheckCircle else Icons.Default.Medication,
                        contentDescription = null,
                        tint = if (medicine.isTaken) HealthGreen else HealthBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    color = onSurfaceColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "$timeStr · ${medicine.dosage}",
                    color = onSurfaceColor.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
            
            if (medicine.isTaken) {
                Icon(Icons.Default.DoneAll, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(24.dp))
            } else {
                IconButton(
                    onClick = onTakenClick,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(HealthBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Mark Taken", tint = Color.White)
                }
            }
        }
    }
}
