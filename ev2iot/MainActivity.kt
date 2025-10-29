package com.example.ev2iot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val campoUsuario = findViewById<EditText>(R.id.editTextUsuario)
        val campoContrasena = findViewById<EditText>(R.id.editTextContrasena)
        val botonDeLogin = findViewById<Button>(R.id.botonLogin)
        val botonIrARegistro = findViewById<Button>(R.id.botonIrARegistro)

        botonDeLogin.setOnClickListener {
            val usuarioEscrito = campoUsuario.text.toString()
            val contrasenaEscrita = campoContrasena.text.toString()

            if (validarCredenciales(usuarioEscrito, contrasenaEscrita)) {

                guardarUsuarioLogueado(usuarioEscrito)

                val intent = Intent(this, SelectConnection::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Error: Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
            }
        }

        botonIrARegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validarCredenciales(usuario: String, contrasena: String): Boolean {
        val USUARIO_ADMIN = "admin"
        val CONTRASENA_ADMIN = "123456"

        if (usuario == USUARIO_ADMIN && contrasena == CONTRASENA_ADMIN) {
            return true
        }

        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            val sharedPreferences = EncryptedSharedPreferences.create(
                "auth_prefs",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val contrasenaGuardada = sharedPreferences.getString(usuario, null)
            return contrasenaGuardada != null && contrasenaGuardada == contrasena

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    /**
     * Guarda el nombre de usuario en un SharedPreferences (no encriptado)
     * para que el resto de la app sepa quién está logueado.
     */
    private fun guardarUsuarioLogueado(username: String) {

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("LOGGED_IN_USER", username)
            apply()
        }
    }
}