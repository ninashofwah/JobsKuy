package com.proweb.jobskuy.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.SessionManager
import com.proweb.jobskuy.databinding.FragmentMyJobsBinding

class MyJobsFragment : Fragment(R.layout.fragment_my_jobs) {

    private var _binding: FragmentMyJobsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyJobsBinding.bind(view)

        sessionManager = SessionManager(requireContext())
        val currentRecruiterId = sessionManager.getUid() ?: ""

        binding.rvMyJobs.layoutManager = LinearLayoutManager(requireContext())

        if (currentRecruiterId.isNotEmpty()) {
            fetchPostedJobsHistory(currentRecruiterId)
        }
    }

    private fun fetchPostedJobsHistory(recruiterId: String) {
        // Query Firestore untuk memuat riwayat lowongan yang diposting oleh UMKM terkait
        db.collection("jobs")
            .whereEqualTo("recruiterUid", recruiterId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "Anda belum mempublikasikan lowongan", Toast.LENGTH_SHORT).show()
                } else {
                    // Hubungkan ke JobAdapter proyek Anda (Mengandung Logika Haversine)
                    // val adapter = JobAdapter(documents.toObjects(Job::class.java))
                    // binding.rvMyJobs.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal memuat arsip: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
