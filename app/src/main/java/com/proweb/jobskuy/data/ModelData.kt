package com.proweb.jobskuy.data

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val role: String = "",          // "Pencari Kerja" atau "Penyedia Kerja"
    val latitude: Double = 0.0,     // Otomatis terisi lewat GPS saat register
    val longitude: Double = 0.0,
    val resumeUrl: String = "",
    val videoUrl: String = ""      // Link video perkenalan pendek (.mp4)
)
