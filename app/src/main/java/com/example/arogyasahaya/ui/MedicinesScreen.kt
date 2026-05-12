package com.example.arogyasahaya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arogyasahaya.R
import com.example.arogyasahaya.data.local.entity.Medicine
import com.example.arogyasahaya.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesScreen(
    viewModel: HealthViewModel,
    onAddMedicine: () -> Unit
) {
    val medicines by viewModel.allMedicines.observeAsState(initial = emptyList())
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.my_medicines), 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ) 
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(
                        onClick = onAddMedicine,
                        modifier = Modifier.padding(end = 8.dp).size(48.dp).clip(CircleShape).background(HealthGreenLight)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = HealthGreen)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.active_medications), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (medicines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_medicines_added), color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(medicines) { med ->
                        MedicineManagementItem(
                            medicine = med,
                            formatter = timeFormatter,
                            onDelete = { viewModel.deleteMedicine(med) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineManagementItem(medicine: Medicine, formatter: SimpleDateFormat, onDelete: () -> Unit) {
    val isLowStock = medicine.remainingTablets < 7
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLowStock) HealthOrangeLight else HealthGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = if (isLowStock) HealthOrange else HealthGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    medicine.name, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    "${medicine.dosage} · ${medicine.frequency}", 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), 
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule, 
                        contentDescription = null, 
                        tint = HealthGreen, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        formatter.format(Date(medicine.time)),
                        color = HealthGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    if (isLowStock) {
                        Surface(
                            color = HealthOrangeLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "REFILL SOON",
                                color = HealthOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.background(HealthRedLight, CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = HealthRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}
