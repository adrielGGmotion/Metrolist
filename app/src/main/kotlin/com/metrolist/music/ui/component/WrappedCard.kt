package com.metrolist.music.ui.component

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.BartleFontFamily
import com.metrolist.music.wrapped.WrappedActivity
import com.metrolist.music.wrapped.WrappedData

@Composable
fun WrappedCard(
    wrappedData: WrappedData?,
    isLoading: Boolean,
    userName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Fetching your Wrapped data...")
                } else if (wrappedData != null) {
                    Text(text = "Your Wrapped is ready!", fontFamily = BartleFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Time to see what you loved this year.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(context, WrappedActivity::class.java).apply {
                            putExtra("USER_NAME", userName)
                        }
                        context.startActivity(intent)
                    }) {
                        Text(text = "Let's go!")
                    }
                }
            }
        }
}
