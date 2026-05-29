package com.baust.cafe.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.CartAdapter
import com.baust.cafe.models.CartItem
import com.baust.cafe.models.Order
import com.baust.cafe.models.OrderItem
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private lateinit var checkoutRecyclerView: RecyclerView
    private lateinit var totalAmountText: TextView
    private lateinit var placeOrderButton: Button
    private lateinit var backButton: ImageView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var addressEdit: EditText
    private lateinit var apartmentEdit: EditText
    private lateinit var paymentGroup: RadioGroup
    private lateinit var mapView: MapView
    private lateinit var btnMyLocation: ImageButton
    private lateinit var deliveryAddressCard: androidx.cardview.widget.CardView
    private lateinit var advancePaymentText: TextView
    private lateinit var deliveryChargeText: TextView
    private var marker: Marker? = null
    private var userPhone: String = ""

    private var cartItems: ArrayList<CartItem> = arrayListOf()
    private var subtotal: Double = 0.0
    private var deliveryCharge: Double = 0.0
    private var totalAmount: Double = 0.0
    private var isPreorder: Boolean = false

    private var verifiedTrxId: String = ""
    private var selectedPaymentMethod: String = "Cash on Delivery"

    // BAUST Coordinates
    private val baustLat = 25.7538
    private val baustLon = 88.9056

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        setContentView(R.layout.activity_checkout)

        // Initialize UI
        mapView = findViewById(R.id.mapView)
        btnMyLocation = findViewById(R.id.btnMyLocation)
        checkoutRecyclerView = findViewById(R.id.checkoutRecyclerView)
        totalAmountText = findViewById(R.id.totalAmount)
        deliveryChargeText = findViewById(R.id.deliveryChargeText)
        placeOrderButton = findViewById(R.id.placeOrderButton)
        backButton = findViewById(R.id.backButton)
        userNameText = findViewById(R.id.userName)
        userEmailText = findViewById(R.id.userEmail)
        addressEdit = findViewById(R.id.checkoutAddress)
        apartmentEdit = findViewById(R.id.checkoutApartment)
        paymentGroup = findViewById(R.id.paymentGroup)
        deliveryAddressCard = findViewById(R.id.deliveryAddressCard)
        advancePaymentText = findViewById(R.id.advancePaymentText)

        // Get data from Intent
        cartItems = intent.getSerializableExtra("CART_ITEMS") as? ArrayList<CartItem> ?: arrayListOf()
        isPreorder = intent.getBooleanExtra("IS_PREORDER", false)

        if (isPreorder) {
            setupPreorderUI()
        } else {
            setupMap()
        }
        
        setupRecyclerView()
        loadUserDetails()
        calculateTotal()

        backButton.setOnClickListener { finish() }
        placeOrderButton.setOnClickListener { placeOrder() }
        btnMyLocation.setOnClickListener { getCurrentLocation() }

        // Trigger payment dialog immediately on selection
        paymentGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioBkash -> {
                    selectedPaymentMethod = "bKash"
                    showMobilePaymentDialog("bKash")
                }
                R.id.radioNagad -> {
                    selectedPaymentMethod = "Nagad"
                    showMobilePaymentDialog("Nagad")
                }
                R.id.radioCod -> {
                    selectedPaymentMethod = "Cash on Delivery"
                    verifiedTrxId = ""
                }
            }
        }
    }

    private fun setupPreorderUI() {
        deliveryAddressCard.visibility = android.view.View.GONE
        findViewById<RadioButton>(R.id.radioCod).visibility = android.view.View.GONE
        paymentGroup.check(R.id.radioBkash)
        advancePaymentText.visibility = android.view.View.VISIBLE
        deliveryCharge = 0.0
        deliveryChargeText.visibility = android.view.View.GONE
        
        val titleText = findViewById<TextView>(R.id.checkoutTitle)
        if (titleText != null) titleText.text = "Pre-order"
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(17.0)
        
        val baustPoint = GeoPoint(baustLat, baustLon)
        mapController.setCenter(baustPoint)

        marker = Marker(mapView)
        marker?.position = baustPoint
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker?.title = "Delivery Location"
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
        addressEdit.setText("Fetching address...")
        
        calculateDeliveryCharge(point)
        
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
                runOnUiThread {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val fullAddress = (0..address.maxAddressLineIndex).map { address.getAddressLine(it) }.joinToString(", ")
                        addressEdit.setText(fullAddress)
                    } else {
                        addressEdit.setText("Location Selected on Map")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { addressEdit.setText("Location Selected on Map") }
            }
        }.start()
    }

    private fun calculateDeliveryCharge(point: GeoPoint) {
        if (isPreorder) {
            deliveryCharge = 0.0
            updateTotalDisplay()
            return
        }

        val results = FloatArray(1)
        Location.distanceBetween(baustLat, baustLon, point.latitude, point.longitude, results)
        val distanceKm = results[0] / 1000.0

        deliveryCharge = when {
            distanceKm <= 2.0 -> 40.0
            distanceKm <= 5.0 -> 60.0
            distanceKm <= 8.0 -> 90.0
            distanceKm <= 12.0 -> 120.0
            else -> 120.0 + ((distanceKm - 12.0).toInt() + 1) * 10.0
        }
        
        updateTotalDisplay()
    }

    private fun getCurrentLocation() {
        val locationManager = getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please turn on your GPS/Location", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userPoint = GeoPoint(location.latitude, location.longitude)
                    mapView.controller.animateTo(userPoint)
                    updateMarkerPosition(userPoint)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            val userPoint = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                            mapView.controller.animateTo(userPoint)
                            updateMarkerPosition(userPoint)
                        } else {
                            Toast.makeText(this, "Still waiting for GPS signal. Please try tapping the map manually.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun calculateTotal() {
        subtotal = cartItems.sumOf { it.price * it.quantity }
        updateTotalDisplay()
    }

    private fun updateTotalDisplay() {
        totalAmount = subtotal + deliveryCharge
        deliveryChargeText.text = String.format(Locale.getDefault(), "Delivery: ৳%.0f", deliveryCharge)
        totalAmountText.text = String.format(Locale.getDefault(), "Tk. %.2f", totalAmount)
        
        if (isPreorder) {
            val advance = subtotal * 0.50
            advancePaymentText.text = String.format(Locale.getDefault(), "Advance Payment (50%%): Tk. %.2f", advance)
        }
    }

    private fun placeOrder() {
        if (!isPreorder) {
            val address = addressEdit.text.toString().trim()
            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter delivery address", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val selectedPaymentId = paymentGroup.checkedRadioButtonId
        if (isPreorder && selectedPaymentId == R.id.radioCod) {
             Toast.makeText(this, "Advance payment is required for pre-orders", Toast.LENGTH_SHORT).show()
             return
        }

        if ((selectedPaymentId == R.id.radioBkash || selectedPaymentId == R.id.radioNagad) && verifiedTrxId.isEmpty()) {
            showMobilePaymentDialog(if (selectedPaymentId == R.id.radioBkash) "bKash" else "Nagad")
            return
        }
        
        finalizeOrder(selectedPaymentMethod, verifiedTrxId)
    }

    private fun showMobilePaymentDialog(method: String) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_mobile_payment)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val title = dialog.findViewById<TextView>(R.id.paymentTitle)
        val background = dialog.findViewById<LinearLayout>(R.id.inputBackground)
        val label = dialog.findViewById<TextView>(R.id.inputLabel)
        val inputField = dialog.findViewById<EditText>(R.id.paymentInputField)
        val bottomInstruction = dialog.findViewById<TextView>(R.id.bottomInstruction)
        val rights = dialog.findViewById<TextView>(R.id.rightsText)
        val confirmBtn = dialog.findViewById<Button>(R.id.confirmPayment)
        val cancelBtn = dialog.findViewById<Button>(R.id.cancelPayment)
        val closeBtn = dialog.findViewById<ImageView>(R.id.closeDialog)

        val amountToPay = if (isPreorder) subtotal * 0.50 else totalAmount
        val merchantNumber = "01XXXXXXXXX"

        title.text = "Manual Payment"
        label.text = "1. Send Tk. ${String.format("%.2f", amountToPay)} to $merchantNumber via $method app.\n2. Enter your $method number below:"
        inputField.hint = "e.g 01XXXXXXXXX"
        rights.text = "© 2024 $method, All Rights Reserved"

        var step = 1
        var senderNumber = ""

        when (method) {
            "bKash" -> background.setBackgroundColor(getColor(R.color.bkash_pink))
            "Nagad" -> background.setBackgroundColor(getColor(R.color.nagad_orange))
        }

        confirmBtn.setOnClickListener {
            val input = inputField.text.toString().trim()
            when (step) {
                1 -> {
                    if (input.length >= 11) {
                        senderNumber = input
                        step = 2
                        label.text = "Enter the Transaction ID (TrxID) from your $method message:"
                        inputField.setText("")
                        inputField.hint = "e.g. A1B2C3D4"
                        inputField.inputType = android.text.InputType.TYPE_CLASS_TEXT
                        bottomInstruction.text = "We will verify this manually"
                    } else {
                        Toast.makeText(this, "Enter valid $method number", Toast.LENGTH_SHORT).show()
                    }
                }
                2 -> {
                    if (input.isNotEmpty()) {
                        verifiedTrxId = input
                        dialog.dismiss()
                        Toast.makeText(this, "TrxID saved. You can now place your order.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Please enter Transaction ID", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        cancelBtn.setOnClickListener { 
            paymentGroup.check(R.id.radioCod)
            dialog.dismiss() 
        }
        closeBtn.setOnClickListener { 
            paymentGroup.check(R.id.radioCod)
            dialog.dismiss() 
        }
        dialog.show()
    }

    private fun finalizeOrder(paymentMethod: String, trxId: String = "") {
        val address = if (isPreorder) "Pickup from Cafe" else addressEdit.text.toString().trim()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("Orders")
        val orderId = database.push().key ?: return

        val orderItems = cartItems.map {
            OrderItem(itemId = it.itemId, itemName = it.itemName, quantity = it.quantity, price = it.price)
        }

        val order = Order(
            orderId = orderId,
            studentId = userId,
            studentName = userNameText.text.toString(),
            studentPhone = userPhone,
            items = orderItems,
            totalAmount = totalAmount,
            deliveryAddress = address,
            deliveryOption = if (isPreorder) "pickup" else "delivery",
            status = if (trxId.isNotEmpty()) "pending_verification" else "pending",
            specialInstructions = if (isPreorder) "Pre-order (Paid 50%): $paymentMethod" else "Payment: $paymentMethod",
            transactionId = trxId,
            paymentStatus = if (trxId.isNotEmpty()) "pending_verification" else "verified"
        )

        placeOrderButton.isEnabled = false
        database.child(orderId).setValue(order)
            .addOnSuccessListener {
                com.baust.cafe.utils.CartManager.clearCart()
                val currentUserName = userNameText.text.toString().split(" - ")[0]
                saveAdminNotification("New Order Received!", "@$currentUserName ordered food - Tk $totalAmount", "order")
                Toast.makeText(this, "Order placed successfully via $paymentMethod!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                placeOrderButton.isEnabled = true
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAdminNotification(title: String, message: String, type: String) {
        val notifyDb = FirebaseDatabase.getInstance().getReference("AdminNotifications")
        val id = notifyDb.push().key ?: return
        val notification = com.baust.cafe.models.AdminNotification(
            id = id,
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            read = false
        )
        notifyDb.child(id).setValue(notification)
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    private fun setupRecyclerView() {
        val adapter = CartAdapter(cartItems) {
            calculateTotal()
            com.baust.cafe.utils.CartManager.syncWithFirebase()
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        checkoutRecyclerView.layoutManager = LinearLayoutManager(this)
        checkoutRecyclerView.adapter = adapter
    }
    private fun loadUserDetails() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userNameText.text = user.displayName ?: "User"
            userEmailText.text = user.email ?: ""
            FirebaseDatabase.getInstance().getReference("Users").child(user.uid).get().addOnSuccessListener {
                if (it.exists()) {
                    val name = it.child("name").value.toString()
                    userPhone = it.child("phoneNumber").value.toString()
                    userNameText.text = name
                    userEmailText.text = "${user.email}\n$userPhone"
                }
            }
        }
    }
}
