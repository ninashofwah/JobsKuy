package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentHomeSeekerBinding
import kotlin.math.*

class HomeSeekerFragment : Fragment(R.layout.fragment_home_seeker) {

    private var _binding: FragmentHomeSeekerBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Koordinat default user (akan diupdate dari database profil user)
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeSeekerBinding.bind(view)

        binding.rvJobsNearby.layoutManager = LinearLayoutManager(requireContext())

        // 1. Ambil lokasi GPS user yang tersimpan di Firestore untuk hitung Haversine
        val currentUid = auth.currentUser?.uid ?: ""
        if (currentUid.isNotEmpty()) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("fullName") ?: "Pencari Kerja"
                        binding.tvWelcomeSeeker.text = "Halo, $name!"

                        userLat = doc.getDouble("latitude") ?: 0.0
                        userLng = doc.getDouble("longitude") ?: 0.0

                        // 2. Setelah lokasi user didapat, muat lowongan terdekat
                        loadNearbyJobs()
                    }
                }
        }
    }

    private fun loadNearbyJobs() {
        db.collection("jobs").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "Belum ada lowongan tersedia", Toast.LENGTH_SHORT).show()
                } else {
                    // Logika Haversine dipasang di sini untuk mengurutkan lowongan terdekat
                    // Tambahkan array adapter lowongan kamu di sini untuk rvJobsNearby
                }
            }
    }

    // Fungsi Rumus Haversine Formula (Mencari jarak KM antara GPS User vs GPS Lowongan UMKM)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius bumi dalam kilometer
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}