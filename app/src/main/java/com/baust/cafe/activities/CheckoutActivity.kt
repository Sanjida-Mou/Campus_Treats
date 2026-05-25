package com.baust.cafe.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
    private lateinit var tipCard: androidx.cardview.widget.CardView
    private lateinit var advancePaymentText: TextView
    private var marker: Marker? = null
    private var userPhone: String = ""

    private var cartItems: ArrayList<CartItem> = arrayListOf()
    private var totalAmount: Double = 0.0
    private var isPreorder: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load OSM configuration
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        setContentView(R.layout.activity_checkout)

        // Initialize UI
        mapView = findViewById(R.id.mapView)
        btnMyLocation = findViewById(R.id.btnMyLocation)
        checkoutRecyclerView = findViewById(R.id.checkoutRecyclerView)
        totalAmountText = findViewById(R.id.totalAmount)
        placeOrderButton = findViewById(R.id.placeOrderButton)
        backButton = findViewById(R.id.backButton)
        userNameText = findViewById(R.id.userName)
        userEmailText = findViewById(R.id.userEmail)
        addressEdit = findViewById(R.id.checkoutAddress)
        apartmentEdit = findViewById(R.id.checkoutApartment)
        paymentGroup = findViewById(R.id.paymentGroup)
        deliveryAddressCard = findViewById(R.id.deliveryAddressCard)
        tipCard = findViewById(R.id.tipCard)
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
        
        placeOrderButton.setOnClickListener {
            placeOrder()
        }

        btnMyLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun setupPreorderUI() {
        deliveryAddressCard.visibility = android.view.View.GONE
        tipCard.visibility = android.view.View.GONE
        findViewById<RadioButton>(R.id.radioCod).visibility = android.view.View.GONE
        paymentGroup.check(R.id.radioBkash) // Default to bkash as COD is hidden
        advancePaymentText.visibility = android.view.View.VISIBLE
        
        val titleText = findViewById<TextView>(R.id.checkoutTitle)
        if (titleText != null) titleText.text = "Pre-order"
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
        val mapController = mapView.controller
        mapController.setZoom(17.0)
        
        // BAUST University Location
        val baustPoint = GeoPoint(25.7538, 88.9056)
        mapController.setCenter(baustPoint)

        marker = Marker(mapView)
        marker?.position = baustPoint
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker?.title = "Delivery Location"
        mapView.overlays.add(marker)

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    updateMarkerPosition(it)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }

        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))
    }

    private fun updateMarkerPosition(point: GeoPoint) {
        marker?.position = point
        mapView.invalidate()
        addressEdit.setText("Location Selected on Map")
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userPoint = GeoPoint(it.latitude, it.longitude)
                mapView.controller.animateTo(userPoint)
                updateMarkerPosition(userPoint)
            } ?: Toast.makeText(this, "Could not get location. Is GPS on?", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun setupRecyclerView() {
        // Reuse CartAdapter but with no callback (read-only for checkout)
        val adapter = CartAdapter(cartItems) {}
        checkoutRecyclerView.layoutManager = LinearLayoutManager(this)
        checkoutRecyclerView.adapter = adapter
    }

    private fun loadUserDetails() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userNameText.text = user.displayName ?: "User"
            userEmailText.text = user.email ?: ""
            
            // Try to load extra details from Database
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

    private fun calculateTotal() {
        totalAmount = cartItems.sumOf { it.price * it.quantity }
        totalAmountText.text = String.format(Locale.getDefault(), "Tk. %.2f", totalAmount)
        
        if (isPreorder) {
            val advance = totalAmount * 0.20
            advancePaymentText.text = String.format(Locale.getDefault(), "Advance Payment (20%%): Tk. %.2f", advance)
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
        
        when (selectedPaymentId) {
            R.id.radioBkash -> showMobilePaymentDialog("bKash")
            R.id.radioNagad -> showMobilePaymentDialog("Nagad")
            R.id.radioRocket -> showMobilePaymentDialog("Rocket")
            R.id.radioCard -> showCardPaymentDialog()
            else -> finalizeOrder("Cash on Delivery")
        }
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

        title.text = "Agreement only"
        label.text = "Your $method Account Number"
        inputField.hint = "e.g 01XXXXXXXXX"
        rights.text = "© 2024 $method, All Rights Reserved"

        var step = 1 // 1: Number, 2: OTP, 3: PIN
        val simulatedOTP = (1000..9999).random().toString()

        when (method) {
            "bKash" -> background.setBackgroundColor(getColor(R.color.bkash_pink))
            "Nagad" -> background.setBackgroundColor(getColor(R.color.nagad_orange))
            "Rocket" -> background.setBackgroundColor(getColor(R.color.rocket_purple))
        }

        confirmBtn.setOnClickListener {
            val input = inputField.text.toString().trim()

            when (step) {
                1 -> { // Number Step
                    if (input.length >= 11) {
                        step = 2
                        label.text = "Enter OTP sent to $input"
                        inputField.setText("")
                        inputField.hint = "Enter 4-digit OTP"
                        inputField.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        bottomInstruction.text = "Please enter the code sent to your phone"
                        Toast.makeText(this, "Simulated OTP: $simulatedOTP", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Enter valid $method number", Toast.LENGTH_SHORT).show()
                    }
                }
                2 -> { // OTP Step
                    if (input == simulatedOTP) {
                        step = 3
                        label.text = "Enter your $method PIN"
                        inputField.setText("")
                        inputField.hint = "Enter PIN"
                        inputField.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                        bottomInstruction.text = "Confirm and proceed"
                    } else {
                        Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show()
                    }
                }
                3 -> { // PIN Step
                    if (input.length >= 4) {
                        dialog.dismiss()
                        finalizeOrder(method)
                    } else {
                        Toast.makeText(this, "Enter valid PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        cancelBtn.setOnClickListener { dialog.dismiss() }
        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showCardPaymentDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_card_payment)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val payBtn = dialog.findViewById<Button>(R.id.payCardButton)
        val cancelBtn = dialog.findViewById<Button>(R.id.cancelCardButton)

        payBtn.setOnClickListener {
            val cardNum = dialog.findViewById<EditText>(R.id.cardNumber).text.toString()
            if (cardNum.length >= 16) {
                dialog.dismiss()
                finalizeOrder("Credit Card")
            } else {
                Toast.makeText(this, "Enter valid card details", Toast.LENGTH_SHORT).show()
            }
        }

        cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun finalizeOrder(paymentMethod: String) {
        val address = if (isPreorder) "Pickup from Cafe" else addressEdit.text.toString().trim()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("Orders")
        val orderId = database.push().key ?: return

        val orderItems = cartItems.map {
            OrderItem(
                itemId = it.itemId,
                itemName = it.itemName,
                quantity = it.quantity,
                price = it.price
            )
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
            status = "pending",
            specialInstructions = if (isPreorder) "Pre-order (Paid 20%): $paymentMethod" else "Payment: $paymentMethod"
        )

        placeOrderButton.isEnabled = false
        database.child(orderId).setValue(order)
            .addOnSuccessListener {
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
}
