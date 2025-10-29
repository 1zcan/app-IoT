package com.example.ev2iot

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class DeviceListActivity : AppCompatActivity() {

    private val TAG = "EscaneoBT"
    private val CODIGO_PETICION_PERMISOS_BLUETOOTH = 101

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val lanzadorPedirEncenderBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Usuario aceptó encender Bluetooth")
            iniciarEscaneoDeDispositivos()
        } else {
            Log.w(TAG, "Usuario NEGÓ encender Bluetooth")
            Toast.makeText(this, "No se puede escanear sin encender Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var listViewDispositivos: ListView
    private lateinit var botonRefrescar: Button
    private var listaDispositivosEncontrados = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private val receptorBluetooth = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
                    }
                    val deviceName = device?.name ?: "Dispositivo Desconocido"
                    val deviceAddress = device?.address

                    val deviceInfo = "$deviceName\n$deviceAddress"
                    if (!listaDispositivosEncontrados.contains(deviceInfo)) {
                        listaDispositivosEncontrados.add(deviceInfo)
                        Log.i(TAG, "Dispositivo encontrado: $deviceInfo")
                        actualizarLista()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "¡Escaneo finalizado!")
                    Toast.makeText(context, "Escaneo finalizado", Toast.LENGTH_SHORT).show()
                    if (listaDispositivosEncontrados.isEmpty()) {
                        Toast.makeText(context, "No se encontraron dispositivos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        listViewDispositivos = findViewById(R.id.listViewDispositivos)
        botonRefrescar = findViewById(R.id.botonRefrescar)


        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, listaDispositivosEncontrados)
        listViewDispositivos.adapter = adapter


        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receptorBluetooth, filter)
        Log.d(TAG, "Receptor (BroadcastReceiver) registrado")

        botonRefrescar.setOnClickListener {
            Log.d(TAG, "Botón Refrescar presionado")
            listaDispositivosEncontrados.clear()
            actualizarLista()
            revisarYPedirPermisosBluetooth()
        }


        listViewDispositivos.setOnItemClickListener { parent, view, position, id ->


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return@setOnItemClickListener
            }
            bluetoothAdapter?.cancelDiscovery()
            Log.d(TAG, "Escaneo cancelado por clic de usuario.")


            val infoCompleta = listaDispositivosEncontrados[position]

            val direccionMac = infoCompleta.takeLast(17)

            Toast.makeText(this, "Conectando a: $direccionMac", Toast.LENGTH_SHORT).show()


            val intent = Intent(this, ChatActivity::class.java).apply {

                putExtra("DIRECCION_MAC", direccionMac)
            }
            startActivity(intent)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receptorBluetooth)
        Log.d(TAG, "Receptor (BroadcastReceiver) quitado")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        bluetoothAdapter?.cancelDiscovery()
        Log.d(TAG, "Escaneo cancelado en onDestroy")
    }

    private fun actualizarLista() {
        adapter.notifyDataSetChanged()
    }

    private fun revisarYPedirPermisosBluetooth() {
        val permisosNecesarios = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val permisosFaltantes = permisosNecesarios.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permisosFaltantes.isNotEmpty()) {
            Log.d(TAG, "Faltan permisos. Pidiéndolos...")
            ActivityCompat.requestPermissions(this, permisosFaltantes.toTypedArray(), CODIGO_PETICION_PERMISOS_BLUETOOTH)
        } else {
            Log.d(TAG, "Permisos ya concedidos. Pasando a revisar si BT está encendido...")
            revisarSiBluetoothEstaEncendido()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODIGO_PETICION_PERMISOS_BLUETOOTH) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "Permisos concedidos. Pasando a revisar si BT está encendido...")
                revisarSiBluetoothEstaEncendido()
            } else {
                Log.w(TAG, "Permisos denegados.")
                Toast.makeText(this, "No puedo buscar dispositivos sin permisos.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun revisarSiBluetoothEstaEncendido() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "¡Error! Dispositivo no soporta Bluetooth.")
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        if (bluetoothAdapter?.isEnabled == true) {
            Log.d(TAG, "Bluetooth ya está encendido. Iniciando escaneo...")
            iniciarEscaneoDeDispositivos()
        } else {
            Log.d(TAG, "Bluetooth está apagado. Pidiendo al usuario que lo encienda...")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            lanzadorPedirEncenderBluetooth.launch(enableBtIntent)
        }
    }

    private fun iniciarEscaneoDeDispositivos() {
        Toast.makeText(this, "Iniciando escaneo...", Toast.LENGTH_SHORT).show()

        val permisoScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.BLUETOOTH_ADMIN
        }

        if (bluetoothAdapter?.isDiscovering == true) {
            Log.d(TAG, "Ya estaba escaneando. Cancelando escaneo anterior...")
            if (ActivityCompat.checkSelfPermission(this, permisoScan) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Falta permiso ($permisoScan) para 'cancelDiscovery'")
                return
            }
            bluetoothAdapter?.cancelDiscovery()
        }

        if (ActivityCompat.checkSelfPermission(this, permisoScan) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "¡FALLA CRÍTICA DE PERMISOS! No se puede iniciar 'startDiscovery' sin $permisoScan.")
            Toast.makeText(this, "Error: Falta permiso ($permisoScan).", Toast.LENGTH_LONG).show()
            return
        }

        val exito = bluetoothAdapter?.startDiscovery()
        Log.d(TAG, "Llamada a startDiscovery() devolvió: $exito")

        if (exito == false) {
            Log.e(TAG, "startDiscovery() devolvió 'false', activar el GPS.")
            Toast.makeText(this, "Error: Revisa que la Ubicación (GPS) esté encendida.", Toast.LENGTH_LONG).show()
        }
    }
}