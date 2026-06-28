package com.proweb.jobskuy.data

data class Job(
    val jobId: String = "",
    val recruiterUid: String = "",
    val companyName: String = "",       // Nama Penyedia Kerja
    val jobTitle: String = "",          // Posisi Pekerjaan (sebelumnya title)
    val jobDescription: String = "",    // Deskripsi Pekerjaan (sebelumnya description)
    val requiredDocuments: String = "", // Persyaratan Dokumen
    val maxApplicants: Int = 0,         // Batas Maksimal Pelamar Kerja
    val currentApplicants: Int = 0,     // Total Pelamar Saat Ini
    val salary: String = "",            // Gaji / Bulan
    val jobImage: String = "",
    val latitude: Double = 0.0,         // Untuk perhitungan jarak
    val longitude: Double = 0.0,        // Untuk perhitungan jarak
    @field:JvmField val isClosed: Boolean = false,
    val createdAt: Long = 0
)
