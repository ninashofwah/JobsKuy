package com.proweb.jobskuy.data

data class Application(
    val applicationId: String = "",
    val jobId: String = "",
    val recruiterUid: String = "",      // ID Pemilik Lowongan (untuk memfilter list)
    val seekerUid: String = "",         // ID Pencari Kerja yang melamar
    val seekerName: String = "",        // Nama lengkap pelamar
    val jobTitle: String = "",          // Posisi lowongan yang dilamar
    val companyName: String = "",       // Nama
    val appliedAt: Long = 0,
    val status: String = "Pending"      // Status lamaran (Pending / Diterima / Ditolak)
)