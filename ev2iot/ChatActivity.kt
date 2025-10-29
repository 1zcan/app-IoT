package com.example.ev2iot

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatBT"

    private lateinit var chatHistory: TextView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: Button

    private var macAddress: String? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedThread: ConnectedThread? = null


    private var myUsername: String = "Yo"

    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        const val MESSAGE_READ = 0
        const val MESSAGE_WRITE = 1
        const val MESSAGE_TOAST = 2
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {

                    val readBuf = msg.obj as ByteArray
                    val readMessage = String(readBuf, 0, msg.arg1)


                    val parts = readMessage.split(":", limit = 2)
                    if (parts.size == 2) {
                        val user = parts[0]
                        val message = parts[1]
                        chatHistory.append("$user: $message\n")
                    } else {

                        chatHistory.append("Desconocido: $readMessage\n")
                    }

                }
                MESSAGE_WRITE -> {

                    val writeBuf = msg.obj as ByteArray
                    val writeMessage = String(writeBuf)
                    chatHistory.append("$myUsername: $writeMessage\n")

                }
                MESSAGE_TOAST -> {
                    Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatHistory = findViewById(R.id.textViewChatHistory)
        messageBox = findViewById(R.id.editTextMensaje)
        sendButton = findViewById(R.id.botonEnviar)


        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        myUsername = prefs.getString("LOGGED_IN_USER", "Usuario") ?: "Usuario"


        macAddress = intent.getStringExtra("DIRECCION_MAC")
        if (macAddress == null) {
            Toast.makeText(this, "Error: No se recibió la dirección MAC", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Log.d(TAG, "Recibida MAC: $macAddress")

        sendButton.setOnClickListener {
            val message = messageBox.text.toString()
            if (message.isNotEmpty()) {

                val payload = "$myUsername:$message"

                connectedThread?.write(payload.toByteArray())

                messageBox.setText("")

            }
        }

        chatHistory.text = "Conectando a $macAddress..."
        ConnectThread().start()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectedThread?.cancel()
        bluetoothSocket?.close()
    }

    private inner class ConnectThread : Thread() {
        private val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(macAddress)

        override fun run() {
            if (ActivityCompat.checkSelfPermission(this@ChatActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                handler.obtainMessage(MESSAGE_TOAST, "Error: Falta permiso BLUETOOTH_CONNECT").sendToTarget()
                return
            }
            if (ActivityCompat.checkSelfPermission(this@ChatActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                handler.obtainMessage(MESSAGE_TOAST, "Error: Falta permiso BLUETOOTH_SCAN").sendToTarget()
                return
            }

            bluetoothAdapter?.cancelDiscovery()

            try {
                bluetoothSocket = device?.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
                Log.d(TAG, "¡Conexión exitosa!")
                connectedThread = ConnectedThread(bluetoothSocket!!)
                connectedThread?.start()

            } catch (e: IOException) {
                Log.e(TAG, "Fallo al conectar socket", e)
                handler.obtainMessage(MESSAGE_TOAST, "Fallo al conectar").sendToTarget()
                try {
                    bluetoothSocket?.close()
                } catch (eClose: IOException) {
                    Log.e(TAG, "Fallo al cerrar socket de conexión", eClose)
                }
                return
            }
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val buffer: ByteArray = ByteArray(1024)

        init {
            handler.obtainMessage(MESSAGE_TOAST, "¡Conectado! Listo para chatear.").sendToTarget()
            runOnUiThread { chatHistory.text = "" }
        }

        override fun run() {
            var numBytes: Int
            while (true) {
                try {
                    numBytes = inputStream.read(buffer)

                    handler.obtainMessage(MESSAGE_READ, numBytes, -1, buffer.clone()).sendToTarget()
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream desconectado", e)
                    handler.obtainMessage(MESSAGE_TOAST, "Desconectado").sendToTarget()
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {

                outputStream.write(bytes)



                val fullMessage = String(bytes)
                val messageOnly = fullMessage.substringAfter(":", "")

                handler.obtainMessage(MESSAGE_WRITE, -1, -1, messageOnly.toByteArray()).sendToTarget()

            } catch (e: IOException) {
                Log.e(TAG, "Error al enviar datos", e)
                handler.obtainMessage(MESSAGE_TOAST, "Error al enviar").sendToTarget()
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Fallo al cerrar socket", e)
            }
        }
    }
}