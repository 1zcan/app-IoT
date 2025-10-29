package com.example.ev2iot

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.nio.charset.StandardCharsets
import java.util.UUID
import android.content.Context

class MqttChatActivity : AppCompatActivity() {

    private val TAG = "MqttChat"

    private lateinit var chatHistory: TextView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: Button

    private lateinit var client: Mqtt5AsyncClient
    private val brokerUri = "broker.hivemq.com"
    private val topic = "ev2iot/chat/general"

    private var myClientId: String = ""

    private var myUsername: String = "Yo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_chat)

        chatHistory = findViewById(R.id.textViewChatHistory)
        messageBox = findViewById(R.id.editTextMensaje)
        sendButton = findViewById(R.id.botonEnviar)


        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        myUsername = prefs.getString("LOGGED_IN_USER", "Usuario") ?: "Usuario"


        myClientId = UUID.randomUUID().toString()

        client = MqttClient.builder()
            .useMqttVersion5()
            .serverHost(brokerUri)
            .serverPort(1883)
            .identifier(myClientId)
            .buildAsync()

        sendButton.setOnClickListener {
            val message = messageBox.text.toString()
            if (message.isNotEmpty()) {
                publishMessage(message)
                messageBox.setText("")
            }
        }

        connectAndSubscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
        Log.d(TAG, "Desconectado del broker")
    }

    private fun connectAndSubscribe() {
        chatHistory.text = "Conectando al broker $brokerUri..."


        client.publishes(MqttGlobalPublishFilter.ALL) { publish: Mqtt5Publish ->

            val rawPayload = StandardCharsets.UTF_8.decode(publish.payload.orElse(null)).toString()
            Log.d(TAG, "Payload recibido: $rawPayload")

            try {
                // Parseamos el nuevo JSON
                val receivedId = rawPayload.substringAfter("\"id\":\"").substringBefore("\"")
                val receivedUser = rawPayload.substringAfter("\"user\":\"").substringBefore("\"")
                val receivedMsg = rawPayload.substringAfter("\"msg\":\"").substringBefore("\"}")

                // Si el ID del mensaje NO es nuestro ID, lo mostramos
                if (receivedId != myClientId) {
                    Log.d(TAG, "Mensaje de $receivedUser: $receivedMsg")
                    runOnUiThread {

                        chatHistory.append("$receivedUser: $receivedMsg\n")
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error al parsear JSON, mostrando mensaje raw", e)
                runOnUiThread {
                    chatHistory.append("Desconocido: $rawPayload\n")
                }
            }
        }


        client.connect()
            .whenComplete { connAck, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Error al conectar", throwable)
                    runOnUiThread {
                        chatHistory.append("\nError al conectar: ${throwable.message}")
                    }
                } else {
                    Log.d(TAG, "¡Conexión exitosa!")
                    runOnUiThread {
                        chatHistory.append("\nConectado. Suscribiendo a $topic...")
                    }

                    client.subscribeWith()
                        .topicFilter(topic)
                        .send()
                        .whenComplete { subAck, subThrowable ->
                            if (subThrowable != null) {
                                Log.e(TAG, "Error al suscribir", subThrowable)
                                runOnUiThread {
                                    chatHistory.append("\nError al suscribir: ${subThrowable.message}")
                                }
                            } else {
                                Log.d(TAG, "¡Suscrito a $topic!")
                                runOnUiThread {
                                    chatHistory.append("\n\n¡Listo para chatear!\n\n")
                                }
                            }
                        }
                }
            }
    }

    private fun publishMessage(message: String) {


        val payload = """{"id":"$myClientId","user":"$myUsername","msg":"$message"}"""

        client.publishWith()
            .topic(topic)
            .payload(payload.toByteArray()) // Enviamos el JSON
            .send()
            .whenComplete { pubAck, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Error al publicar", throwable)
                    runOnUiThread {
                        Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "Mensaje publicado: $message")
                    runOnUiThread {
                        chatHistory.append("$myUsername: $message\n")
                    }
                }
            }
    }
}