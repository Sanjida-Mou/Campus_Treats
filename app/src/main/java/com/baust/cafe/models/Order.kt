package com.baust.cafe.models

data class Order(
    val orderId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val items: List<OrderItem> = listOf(),
    val totalAmount: Double = 0.0,
    val orderTime: Long = System.currentTimeMillis(),
    val pickupTime: String = "",
    val deliveryOption: String = "pickup", // "pickup" or "delivery"
    val deliveryAddress: String = "",
    val status: String = "pending", // pending, preparing, ready, delivered, cancelled
    val specialInstructions: String = ""
)

// This is a separate data class in the same file
data class OrderItem(
    val itemId: String = "",
    val itemName: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0
)