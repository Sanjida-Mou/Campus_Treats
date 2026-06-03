package com.baust.cafe.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.baust.cafe.R
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.*

class SelectLocationActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchEdit: EditText
    private lateinit var btnSearch: ImageView
    private lateinit var btnLocateMe: ImageButton
    private lateinit var btnConfirm: Button
    private lateinit var addressPreview: TextView
    private lateinit var backBtn: ImageView
    private var marker: Marker? = null
    private var selectedAddress: String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        setContentView(R.layout.activity_select_location)

        mapView = findViewById(R.id.selectLocationMapView)
        searchEdit = findViewById(R.id.locationSearchEdit)
        btnSearch = findViewById(R.id.btnDoSearch)
        btnLocateMe = findViewById(R.id.btnLocateMe)
        btnConfirm = findViewById(R.id.confirmLocationBtn)
        addressPreview = findViewById(R.id.currentAddressPreview)
        backBtn = findViewById(R.id.backBtn)

        setupMap()

        backBtn.setOnClickListener { finish() }
        btnLocateMe.setOnClickListener { getCurrentLocation() }
        btnSearch.setOnClickListener { searchLocation() }
        
        searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
                true
            } else false
        }

        btnConfirm.setOnClickListener {
            if (selectedAddress.isNotEmpty()) {
                val intent = Intent()
                intent.putExtra("SELECTED_ADDRESS", selectedAddress)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
            }
        }

        // Try to get current location initially
        getCurrentLocation()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(16.0)
        
        // Default BAUST
        val startPoint = GeoPoint(25.7538, 88.9056)
        mapController.setCenter(startPoint)

        marker = Marker(mapView)
        marker?.position = startPoint
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { updateMarkerPosition(it) }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))
    }

    private fun updateMarkerPosition(point: GeoPoint) {
        marker?.position = point
        mapView.invalidate()
        addressPreview.text = "Fetching address..."
        
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        selectedAddress = (0..address.maxAddressLineIndex).map { address.getAddressLine(it) }.joinToString(", ")
                        addressPreview.text = selectedAddress
                    } else {
                        selectedAddress = "Location at ${point.latitude}, ${point.longitude}"
                        addressPreview.text = selectedAddress
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { addressPreview.text = "Error fetching address" }
            }
        }.start()
    }

    private fun searchLocation() {
        val query = searchEdit.text.toString().trim()
        if (query.isEmpty()) return

        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val lat = addresses[0].latitude
                    val lon = addresses[0].longitude
                    val point = GeoPoint(lat, lon)
                    runOnUiThread {
                        mapView.controller.animateTo(point)
                        updateMarkerPosition(point)
                    }
                } else {
                    runOnUiThread { Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Search error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userPoint = GeoPoint(it.latitude, it.longitude)
                mapView.controller.animateTo(userPoint)
                updateMarkerPosition(userPoint)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
}
