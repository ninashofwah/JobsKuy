package com.proweb.jobskuy.ui.history

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.SessionManager
import com.proweb.jobskuy.databinding.FragmentMyApplicationsBinding

class MyApplicationsFragment : Fragment(R.layout.fragment_my_applications) {

    private var _binding: FragmentMyApplicationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyApplicationsBinding.bind(view)

        sessionManager = SessionManager(requireContext())
        val currentUserId = sessionManager.getUid() ?: ""

        binding.rvMyApplications.layoutManager = LinearLayoutManager(requireContext())

        if (currentUserId.isNotEmpty()) {
            fetchApplicationHistory(currentUserId)
        }
    }

    private fun fetchApplicationHistory(userId: String) {
        // Query Firestore mencari data lamaran kerja milik user aktif
        db.collection("applications")
            .whereEqualTo("seekerUid", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "Belum ada riwayat lamaran", Toast.LENGTH_SHORT).show()
                } else {
                    // Hubungkan ke ApplicantAdapter proyek Anda (mengikuti cetak biru image_0668a3)
                    // val adapter = ApplicantAdapter(documents.toObjects(Application::class.java))
                    // binding.rvMyApplications.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
