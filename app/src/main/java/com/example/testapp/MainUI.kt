package com.example.testapp

import android.os.Build
import androidx.compose.runtime.MutableState
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp.ui.theme.TestAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.experimental.or

@RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR2)
@Composable
fun Main_screen(modifier: Modifier = Modifier, usbConnection: UsbConnection? = null) {
    // State to hold the received data
    val receivedData = remember { mutableStateOf("No data received.") }
    val statusByte = remember { mutableStateOf("No data") }
    val totalByte = remember { mutableStateOf("No count") }
    val memberByte1 = remember { mutableStateOf("No member") }
    val memberByte2 = remember { mutableStateOf("No member") }
    val memberByte3 = remember { mutableStateOf("No member") }
    var totalMember = remember { mutableStateOf(0) }
    var totalStorage = remember { mutableStateOf(0) }
    var findMember = remember { mutableStateOf(0) }
    var memberBytesBinary = remember { mutableStateOf("000000000000000000000000") }
    var connectStatus = remember { mutableStateOf("WUKKI Disconnected") }
    var statusColor = remember { mutableStateOf(Color(0xFFFF7C7C)) }  // MutableState<Color>
    var lastReceivedTimestamp = remember { mutableStateOf(System.currentTimeMillis()) }  // MutableState<Long>
    var statusNumber = remember { mutableStateOf(0) }


    LaunchedEffect(usbConnection) {
        var delayTime = 1000L // 기본값 1초

        while (true) {
            usbConnection?.let { connection ->
                val data = connection.receiveData { logMessage ->
                    // Log the message (optional for debugging)
                    println(logMessage)
                }
                data?.let {
                    // Update the state with the received data
                    receivedData.value = it.joinToString(" ") { byte -> "%02X".format(byte) }
                    if (it.isNotEmpty()) {
                        lastReceivedTimestamp.value = System.currentTimeMillis()

                        statusColor.value = Color(0xFF7CACFF)
                        connectStatus.value = "WUKKI Connected"

                        statusByte.value = "%02X".format(it[2])
                        totalByte.value = "%02X".format(it[3])

                        memberBytesBinary.value = listOf(it[4], it[5], it[6])
                            .joinToString("") { byte -> byte.toUByte().toInt().toString(2).padStart(8, '0') }

                        val onesCount = memberBytesBinary.value.count { it == '1' }

                        if(it[2] == 0x01.toByte()){ //scanning
                            findMember.value = it[3].toInt()
                            totalMember.value = it[3].toInt()
                            statusColor.value = Color(0xFF7CACFF)
                            connectStatus.value = "WUKKI Connected"
                            statusNumber.value = 1
                            memberBytesBinary.value = listOf(it[4], it[5], it[6])
                                .joinToString("") { byte -> byte.toUByte().toInt().toString(2).padStart(8, '0') }
                        }else if(it[2] == 0x02.toByte() || it[2] == 0x03.toByte()){ //monitoring boosting
                            findMember.value = onesCount
                            totalMember.value = it[3].toInt()
                            statusColor.value = Color(0xFF7CACFF)
                            connectStatus.value = "WUKKI Connected"
                            statusNumber.value = 2
                            memberBytesBinary.value = listOf(it[4], it[5], it[6])
                                .joinToString("") { byte -> byte.toUByte().toInt().toString(2).padStart(8, '0') }
                        }else if(it[2] == 0x00.toByte()){ //connected
                            findMember.value = 0
                            totalMember.value = 0
                            statusColor.value = Color(0xFF7CACFF)
                            connectStatus.value = "WUKKI Connected"
                            statusNumber.value = 3
                            memberBytesBinary.value = "000000000000000000000000"
                        }

                        // status == 01이면 수시로, 02,03이면 1초 마다
                        // 1ms로 하니 통신 속도에 오류가 생겨 100ms로 설정했습니다.
                        //통신주기 1초마다로 수정
                        delayTime = when (it[2]) {
                            0x01.toByte() -> 1000L // scanning일 때 1초마다
                            0x02.toByte(), 0x03.toByte() -> 1000L // monitoring, boosting일 때 detect시 수신
                            else -> 1000L
                        }
                    } else {
                        statusByte.value = "NO"
                        totalByte.value = "NO"
                        memberByte1.value = "NO"
                        memberByte2.value = "NO"
                        memberByte3.value = "NO"
                    }
                }
            }
            if (delayTime > 0) {
                delay(delayTime) // delay 적용
            }
        }
    }

    LaunchedEffect(lastReceivedTimestamp.value) {
        while (true) {
            delay(3000) // 3초마다 체크
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastReceivedTimestamp.value > 3000) {
                connectStatus.value = "WUKKI Disconnected"
                statusColor.value = Color(0xFFFF7C7C)
                findMember.value = 0
                totalMember.value = 0
                statusNumber.value = 0
                memberBytesBinary.value = "000000000000000000000000"
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val buttonWidth = (screenWidth * 0.20).dp
    val textSize = (screenWidth * 0.025).sp
    val imgSize = (screenWidth * 0.1).dp
    val paddingSize = (screenWidth * 0.1 * 0.2).dp

    val buttonModifier = Modifier
        .width(buttonWidth)
        .fillMaxHeight(0.5f)
        .shadow(
            elevation = 1.dp, // 그림자 크기 1dp
            shape = RoundedCornerShape(7.dp), // 모서리를 둥글게
            clip = false // 클리핑 없이 그림자가 밖으로 나옴
        )
        .drawBehind {
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFA7A7A7).copy(alpha = 1f),
                    Color.Transparent,
                ),
                startY = size.height - 13.dp.toPx(),
                endY = size.height + 6.dp.toPx()
            )
            drawRoundRect( // 그라데이션 그림자 그리기
                brush = gradientBrush,
                topLeft = Offset(0f, size.height - 13.dp.toPx()),
                size = Size(size.width, 19.dp.toPx()),
                cornerRadius = CornerRadius(7.dp.toPx())
            )
        }
        .clip(RoundedCornerShape(7.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFCECECE), RoundedCornerShape(7.dp))

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {

        Row(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.3f)
            .padding(start = 5.dp, top = 50.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start) {
            Rounded_Box(1,totalMember.value,findMember.value)
            Rounded_Box(3,totalMember.value,findMember.value)

            Spacer(modifier = Modifier.weight(1f))

            //리셋 버튼
            Button(onClick = {
                usbConnection?.sendData(byteArrayOf(0x57, 0x4B, 0x52, 0x52, 0x52)) {
                    Log.d("USB", it) // 로그에 전송 결과 출력
                }
            }, modifier = buttonModifier,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, // 버튼 배경색을 흰색으로 설정
                    contentColor = Color(0xFF838383) // 텍스트 색상
                )) {
                Column(modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally, // 이미지와 텍스트를 중앙 정렬
                    verticalArrangement = Arrangement.Center ) {
                    Image(
                        painter = painterResource(id = R.drawable.reset), // 이미지 리소스
                        contentDescription = "Button Icon", // 접근성 설명
                        modifier = Modifier.size(imgSize), // 이미지 크기 조정
                    )
                    Spacer(modifier = Modifier.height(paddingSize))
                    Text(
                        text = "reset & group",
                        fontSize = textSize, // Adjusted font size
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF555555), // Text color
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
        Row (modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(start = 5.dp, end = 5.dp, top = 20.dp)){
            Number_Button(statusNumber.value,totalMember.value,findMember.value,memberBytesBinary.value)
            func_Button(statusNumber, usbConnection, connectStatus, statusColor, lastReceivedTimestamp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f),
            contentAlignment = Alignment.Center
        ) {
            Status_Card(
                status = connectStatus.value,
                statusColor = statusColor.value
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        //데이터 수신 시 확인을 위한 테스트 코드
//        Text(
//            text = "수신 테스트 : ${receivedData.value} ",
//            color = Color(0xFF838383),
//            style = MaterialTheme.typography.bodyLarge,
//            fontWeight = FontWeight.Medium,
//            fontSize = 10.sp,
//            modifier = Modifier.padding(start = 25.dp)
//        )
//        Text(
//            text = "member data : ${memberBytesBinary.value} ",
//            color = Color(0xFF838383),
//            style = MaterialTheme.typography.bodyLarge,
//            fontWeight = FontWeight.Medium,
//            fontSize = 10.sp,
//            modifier = Modifier.padding(start = 25.dp)
//        )
    }
}

@RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR2)
@Composable
fun Status_Card(status : String, statusColor : Color, modifier: Modifier = Modifier){
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val txtboxWidth = (screenWidth * 0.55).dp

    Card(modifier = Modifier
        .width(txtboxWidth)
        .drawBehind {
            val shadowOffsetY = 13.dp.toPx() // 아래쪽 이동
            val shadowBlurRadius = 6.dp.toPx() // 그림자 퍼짐 정도
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFA7A7A7).copy(alpha = 1f), // 그림자 시작 색상
                    Color.Transparent // 그림자 끝 색상
                ),
                startY = size.height - shadowOffsetY, // 시작 위치
                endY = size.height + shadowBlurRadius // 끝 위치
            )
            drawRoundRect( // 그라데이션 그림자 그리기
                brush = gradientBrush,
                topLeft = Offset(0f, size.height - shadowOffsetY),
                size = Size(size.width, shadowOffsetY + shadowBlurRadius),
                cornerRadius = CornerRadius(7.dp.toPx())
            )
        }, // 사각형 디자인 하기
        shape = RoundedCornerShape(7.dp), // 사각형 모서리 둥글게 조절하기
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFCECECE))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$status",
                color = statusColor,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR2)
@Composable
fun Rounded_Box(number: Int,total: Int,find: Int, modifier: Modifier = Modifier){
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val boxWidth = (screenWidth * 0.24).dp
    val circleSize = (screenWidth * 0.23 * 0.75).dp
    val textSize = (screenWidth * 0.23 * 0.2).sp
    val textSize2 = (screenWidth * 0.23 * 0.25).sp
    val paddingHeight = (screenWidth * 0.23 * 0.1).dp

    var member = when (number){
        1 -> total
        //2 -> find
        else -> total - find
    }

    val circleColor = when (number) { // 전달 인수에 따라 색깔 지정
        1 -> Color(0xFF4080FF)
        //2 -> Color(0xFF4080FF)
        else -> Color(0xFFFF5959)
    }

    val box_text = when (number) { // 전달 인수에 따라 색깔 지정
        1 -> "total"
        //2 -> "checked"
        else -> "missing"
    }

    Card(modifier = Modifier
        .padding(15.dp)
        .width(boxWidth)
        .fillMaxHeight(0.9f)
        .drawBehind {
            val shadowOffsetY = 13.dp.toPx() // 아래쪽 이동
            val shadowBlurRadius = 6.dp.toPx() // 그림자 퍼짐 정도
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFA7A7A7).copy(alpha = 1f), // 그림자 시작 색상
                    Color.Transparent // 그림자 끝 색상
                ),
                startY = size.height - shadowOffsetY, // 시작 위치
                endY = size.height + shadowBlurRadius // 끝 위치
            )
            drawRoundRect( // 그라데이션 그림자 그리기
                brush = gradientBrush,
                topLeft = Offset(0f, size.height - shadowOffsetY),
                size = Size(size.width, shadowOffsetY + shadowBlurRadius),
                cornerRadius = CornerRadius(7.dp.toPx())
            )
        }, // 사각형 디자인 하기
        shape = RoundedCornerShape(7.dp), // 사각형 모서리 둥글게 조절하기
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFCECECE))
        ) {
        Column(modifier = Modifier
            .padding(3.dp)
            .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box( modifier = Modifier
                .size(circleSize) // 원 크기 설정
                .background(Color.White, shape = CircleShape)
                .border(3.dp, circleColor, shape = CircleShape), // 빨간색 원
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$member",
                    color = circleColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = textSize2,
                    fontWeight = FontWeight.Medium
                    )
            }
            Text(
                text = box_text,
                color = Color(0xFF555555),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontSize = textSize,
                modifier = Modifier.padding(top = paddingHeight)
            )
        }
    } // Card
}

@RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR2)
@Composable
fun Number_Button(status: Int, total: Int,find: Int, memberList : String, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val circleSize = (screenWidth * 0.13).dp
    val textSize = (screenWidth * 0.04).sp

//    var circleColor = when (number) { // 전달 인수에 따라 색깔 지정
//        1 -> Color(0xFF555555)
//        2 -> Color(0xFF4080FF)
//        else -> Color(0xFFFF5959)
//    }

    var circleColor = Color(0xFFEEEEEE)

    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .fillMaxHeight()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (j in 0 until 4) {
                    if (status == 0){ //Disconnected
                        circleColor = Color(0xFFEEEEEE)
                    }else if (status == 3){ //Connected
                        circleColor = Color(0xFFEEEEEE)
                    }else if(status == 1){ //Scanning
                        if (i*4+j < total){
                            circleColor = Color(0xFFD8E6FF)
                        }else{
                            circleColor = Color(0xFFEEEEEE)
                        }
                    }else if(status == 2){ //Monitoring, Boosting
                        if (i*4+j < total){
                            if (memberList[i*4+j] == '1'){
                                circleColor = Color(0xFFD8E6FF)
                            } else if (memberList[i*4+j] == '0'){
                                circleColor = Color(0xFFFFBDBD)
                            }
                        }else{
                            circleColor = Color(0xFFEEEEEE)
                        }
                    }else{
                        circleColor = Color(0xFFEEEEEE)
                    }

                    //전에 쓰던 코드
//                    if (memberList[i*4+j] == '1'){
//                        circleColor = Color(0xFFFFFFFF)
//                    } else if (memberList[i*4+j] == '0'){
//                        circleColor = Color(0xFFEEEEEE)
//                    }

                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier
                            .size(circleSize) // Increased button size
                            .shadow(
                                elevation = 1.dp, // 그림자 크기 1dp
                                shape = CircleShape,
                                clip = false
                            )
                            .drawBehind {
                                val gradientBrush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFA7A7A7).copy(alpha = 1f), // 그림자 시작 색상
                                        Color.Transparent // 그림자 끝 색상
                                    ),
                                    startY = size.height - 35.dp.toPx(), // 시작 위치
                                    endY = size.height + 10.dp.toPx() // 끝 위치
                                )
                                drawCircle(
                                    brush = gradientBrush, // 원에 그라데이션 적용
                                    radius = size.width / 2, // 원 반지름
                                    center = Offset(
                                        size.width / 2,
                                        size.height - 20.dp.toPx()
                                    ) // 원의 중심 위치 (아래쪽으로 이동)
                                )
                            }
                            .border(1.dp, Color(0xFFCECECE), CircleShape), // Added border,
                        shape = CircleShape, // Circle shape for the button
                        colors = ButtonDefaults.buttonColors(
                            containerColor = circleColor, // Button background color
                            contentColor = Color.White // Button content color
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                        contentPadding = PaddingValues(0.dp) // Removed internal padding
                    ) {
                        Text(
                            text = "${i * 4 + j + 1}",
                            fontSize = textSize, // Adjusted font size
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9E9E9E), // Text color
                            textAlign = TextAlign.Center // Text alignment
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR2)
@Composable
fun func_Button(status: MutableState<Int>,
                usbConnection: UsbConnection?,
                connectStatus: MutableState<String>,
                statusColor: MutableState<Color>,
                lastReceivedTimestamp: MutableState<Long>){
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp
    val buttonHeight = (screenHeight * 0.11).dp
    val textSize = (screenWidth * 0.03).sp
    val imgSize = (screenWidth * 0.1).dp
    val imgSize_small = (screenWidth * 0.07).dp
    val paddingSize = (screenWidth * 0.1 * 0.2).dp

    var onColor = remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var offColor = remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var highColor = remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var pwrColor = remember { mutableStateOf(Color(0xFFFFFFFF)) }

    val buttonModifier = Modifier
        .fillMaxWidth(0.8f)
        .height(buttonHeight)
        .shadow(
            elevation = 1.dp, // 그림자 크기 1dp
            shape = RoundedCornerShape(7.dp), // 모서리를 둥글게
            clip = false // 클리핑 없이 그림자가 밖으로 나옴
        )
        .drawBehind {
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFA7A7A7).copy(alpha = 1f),
                    Color.Transparent,
                ),
                startY = size.height - 13.dp.toPx(),
                endY = size.height + 6.dp.toPx()
            )
            drawRoundRect( // 그라데이션 그림자 그리기
                brush = gradientBrush,
                topLeft = Offset(0f, size.height - 13.dp.toPx()),
                size = Size(size.width, 19.dp.toPx()),
                cornerRadius = CornerRadius(7.dp.toPx())
            )
        }
        .clip(RoundedCornerShape(7.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFCECECE), RoundedCornerShape(7.dp))

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Button(onClick = {
            usbConnection?.sendData(byteArrayOf(0x57, 0x4B, 0x53, 0x53, 0x53)) {
                Log.d("USB", it) // 로그에 전송 결과 출력
            }
            onColor.value = Color(0xFFFFFFFF)
            offColor.value = Color(0xFFEEEEEE)

//            connectStatus.value = "WUUKI Connected"
//            statusColor.value = Color(0xFF60FFB5)
//            lastReceivedTimestamp.value = System.currentTimeMillis()

//            coroutineScope.launch {
//                delay(1000L)
//                if (System.currentTimeMillis() - lastReceivedTimestamp.value > 1000) {
//                    statusColor.value = Color(0xFFFF7C7C) // 빨간색 (Disconnected)
//                    connectStatus.value = "WUUKI Disconnected"
//                }
//            }
        }, modifier = buttonModifier
            .background(onColor.value),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // 버튼 배경색을 흰색으로 설정
                contentColor = Color(0xFF838383), // 텍스트 색상
            )) {
                Column(modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally, // 이미지와 텍스트를 중앙 정렬
                    verticalArrangement = Arrangement.Center ) {
                    Image(
                        painter = painterResource(id = R.drawable.scan), // 이미지 리소스
                        contentDescription = "Button Icon", // 접근성 설명
                        modifier = Modifier.size(imgSize), // 이미지 크기 조정
                    )
                    Spacer(modifier = Modifier.height(paddingSize))
                    Text(
                        text = "scan on",
                        fontSize = textSize, // Adjusted font size
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF838383), // Text color
                    )
                }
//
        }
        Button(onClick = {
            usbConnection?.sendData(byteArrayOf(0x57, 0x4B, 0x54, 0x54, 0x54)) {
                Log.d("USB", it) // 로그에 전송 결과 출력
            }
            status.value = 3
            offColor.value = Color(0xFFFFFFFF)
            onColor.value = Color(0xFFEEEEEE)

//            coroutineScope.launch {
//                delay(3000L)
//                if (System.currentTimeMillis() - lastReceivedTimestamp.value > 3000) {
//                    statusColor.value = Color(0xFFFF7C7C) // 빨간색 (Disconnected)
//                    connectStatus.value = "WUUKI Disconnected"
//                }
//            }
        }, modifier = buttonModifier
            .background(offColor.value),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // 버튼 배경색을 흰색으로 설정
                contentColor = Color(0xFF838383) // 텍스트 색상
            )) {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally, // 이미지와 텍스트를 중앙 정렬
                verticalArrangement = Arrangement.Center ) {
                Image(
                    painter = painterResource(id = R.drawable.scan), // 이미지 리소스
                    contentDescription = "Button Icon", // 접근성 설명
                    modifier = Modifier.size(imgSize_small), // 이미지 크기 조정
                )
                Spacer(modifier = Modifier.height(paddingSize))
                Text(
                    text = "scan off",
                    fontSize = textSize, // Adjusted font size
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF838383), // Text color
                )
            }
        }

        Button(onClick = {
            usbConnection?.sendData(byteArrayOf(0x57, 0x4B, 0x31, 0x31, 0x31)) {
                Log.d("USB", it)
            }
            highColor.value = Color(0xFFFFFFFF)
            pwrColor.value = Color(0xFFEEEEEE)

//            coroutineScope.launch {
//                delay(1000L)
//                if (System.currentTimeMillis() - lastReceivedTimestamp.value > 1000) {
//                    statusColor.value = Color(0xFFFF7C7C) // 빨간색 (Disconnected)
//                    connectStatus.value = "WUUKI Disconnected"
//                }
//            }
        }, modifier = buttonModifier
            .background(highColor.value),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // 버튼 배경색을 흰색으로 설정
                contentColor = Color(0xFF838383) // 텍스트 색상
            )) {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally, // 이미지와 텍스트를 중앙 정렬
                verticalArrangement = Arrangement.Center ) {
                Image(
                    painter = painterResource(id = R.drawable.wifi), // 이미지 리소스
                    contentDescription = "Button Icon", // 접근성 설명
                    modifier = Modifier.size(imgSize), // 이미지 크기 조정
                )
                Spacer(modifier = Modifier.height(paddingSize))
                Text(
                    text = "high pwr",
                    fontSize = textSize, // Adjusted font size
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF838383), // Text color
                )
            }
        }
        Button(onClick = {
            usbConnection?.sendData(byteArrayOf(0x57, 0x4B, 0x30, 0x30, 0x30)) {
                Log.d("USB", it)
            }
            pwrColor.value = Color(0xFFFFFFFF)
            highColor.value = Color(0xFFEEEEEE)

//            coroutineScope.launch {
//                delay(1000L)
//                if (System.currentTimeMillis() - lastReceivedTimestamp.value > 1000) {
//                    statusColor.value = Color(0xFFFF7C7C) // 빨간색 (Disconnected)
//                    connectStatus.value = "WUUKI Disconnected"
//                }
//            }
        }, modifier = buttonModifier
            .background(pwrColor.value),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // 버튼 배경색을 흰색으로 설정
                contentColor = Color(0xFF838383) // 텍스트 색상
            )) {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally, // 이미지와 텍스트를 중앙 정렬
                verticalArrangement = Arrangement.Center ) {
                Image(
                    painter = painterResource(id = R.drawable.wifi), // 이미지 리소스
                    contentDescription = "Button Icon", // 접근성 설명
                    modifier = Modifier.size(imgSize_small), // 이미지 크기 조정
                )
                Spacer(modifier = Modifier.height(paddingSize))
                Text(
                    text = "nor pwr",
                    fontSize = textSize, // Adjusted font size
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF838383), // Text color
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@Preview(showBackground = true, widthDp = 412, heightDp = 732)
@Composable
fun Main_screenPreview() {
    TestAppTheme {
        Main_screen()
    }
}
