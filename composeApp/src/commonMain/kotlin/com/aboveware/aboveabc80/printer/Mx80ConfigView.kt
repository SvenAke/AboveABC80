package com.aboveware.aboveabc80.printer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Mx80ConfigView() {
    val settings = PrinterManager.mx80Settings
    var explainedSwitch by remember { mutableStateOf<DipSwitch?>(null) }

    if (explainedSwitch != null) {
        AlertDialog(
            onDismissRequest = { explainedSwitch = null },
            confirmButton = { TextButton(onClick = { explainedSwitch = null }) { Text("OK") } },
            title = { Text("Configuration Changed") },
            text = {
                Text(
                    settings.getCurrentExplanation(explainedSwitch!!.bank, explainedSwitch!!.num),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            containerColor = Color(0xFF333333),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            DipSwitchBankView("SW 1", settings.sw1) { explainedSwitch = it }
            DipSwitchBankView("SW 2", settings.sw2) { explainedSwitch = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Explanations:",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        settings.sw1.forEach { sw ->
            Text("${sw.bank}-${sw.num}: ${sw.description}", color = Color.Gray, fontSize = 11.sp)
        }
        settings.sw2.forEach { sw ->
            Text("${sw.bank}-${sw.num}: ${sw.description}", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun DipSwitchBankView(
    title: String,
    switches: List<DipSwitch>,
    onSwitchClicked: (DipSwitch) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // The "Red" Switch block
        Column(
            modifier = Modifier
                .background(Color(0xFF8B0000), RoundedCornerShape(4.dp))
                .padding(4.dp)
        ) {
            Row {
                switches.forEach { sw ->
                    DipSwitchUnit(sw, onSwitchClicked)
                }
            }
        }
    }
}

@Composable
fun DipSwitchUnit(sw: DipSwitch, onSwitchClicked: (DipSwitch) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(2.dp)
            .width(20.dp)
    ) {
        Text(sw.num.toString(), color = Color.White, fontSize = 10.sp)

        // The actual sliding switch visual
        Box(
            modifier = Modifier
                .height(40.dp)
                .width(16.dp)
                .background(Color.Black, RoundedCornerShape(2.dp))
                .clickable {
                    sw.isOn = !sw.isOn
                    onSwitchClicked(sw)
                }
                .padding(2.dp)
        ) {
            // The white slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .align(if (sw.isOn) Alignment.TopCenter else Alignment.BottomCenter)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )
        }

        Text(if (sw.isOn) "ON" else "OFF", color = Color.LightGray, fontSize = 8.sp)
    }
}
