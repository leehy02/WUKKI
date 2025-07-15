package com.example.testapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.testapp.ui.theme.TestAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var usbConnection: UsbConnection

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbConnection = UsbConnection(this)
        usbConnection.setupUsbConnection { logMessage ->
            Log.d("USB_LOG", logMessage)
        }

        setContent {
            TestAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main_screen(usbConnection = usbConnection)
                }
            }
        }
    }
}