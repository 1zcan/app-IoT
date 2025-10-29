package com.example.ev2iot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class SelectConnection : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_connection)

        val botonBluetooth = findViewById<Button>(R.id.botonBluetooth)
        val botonWifi = findViewById<Button>(R.id.botonWifi)


        botonBluetooth.setOnClickListener {
            val intent = Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }

        botonWifi.setOnClickListener {
            val intent = Intent(this, MqttChatActivity::class.java)
            startActivity(intent)
        }
    }


}