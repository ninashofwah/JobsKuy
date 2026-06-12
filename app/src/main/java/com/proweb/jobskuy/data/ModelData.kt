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

data class Job(
    val jobId: String = "",
    val recruiterId: String = "",
    val recruiterName: String = "",
    val title: String = "",
    val description: String = "",
    val salary: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @field:JvmField val isClosed: Boolean = false
)

data class Application(
    val applicationId: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val recruiterId: String = "",
    val seekerId: String = "",
    val seekerName: String = "",
    val resumeUrl: String = "",
    val videoUrl: String = "",
    val status: String = "Terkirim" // Alur: Terkirim -> Review Berkas -> Jadwal Interview -> Diterima/Ditolak
)