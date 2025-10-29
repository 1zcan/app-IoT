package com.example.ev2iot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
// Importaciones de Seguridad
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val campoNuevoUsuario = findViewById<EditText>(R.id.editTextNuevoUsuario)
        val campoNuevaContrasena = findViewById<EditText>(R.id.editTextNuevaContrasena)
        val botonRegistrar = findViewById<Button>(R.id.botonRegistrarCuenta)

        botonRegistrar.setOnClickListener {
            val nuevoUsuario = campoNuevoUsuario.text.toString()
            val nuevaContrasena = campoNuevaContrasena.text.toString()

            if (nuevoUsuario.isEmpty() || nuevaContrasena.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {

                try {

                    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)


                    val sharedPreferences = EncryptedSharedPreferences.create(
                        "auth_prefs",
                        masterKeyAlias,
                        this,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )


                    with(sharedPreferences.edit()) {

                        putString(nuevoUsuario, nuevaContrasena)
                        apply()
                    }

                    Toast.makeText(this, "¡Cuenta registrada exitosamente!", Toast.LENGTH_SHORT).show()
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this, "Error al guardar la cuenta", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }

            }
        }
    }
}