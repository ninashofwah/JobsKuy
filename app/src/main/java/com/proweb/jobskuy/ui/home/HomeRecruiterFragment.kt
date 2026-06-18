package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Application
import com.proweb.jobskuy.databinding.FragmentHomeRecruiterBinding

class HomeRecruiterFragment : Fragment(R.layout.fragment_home_recruiter) {

    private var _binding: FragmentHomeRecruiterBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeRecruiterBinding.bind(view)

        setupUI()
        loadStatistics()
        loadApplicantsForReview()
    }

    private fun setupUI() {
        // Klik Foto Profil - Menggunakan Action ID dari nav_graph.xml
        binding.ivProfileRecruiter.setOnClickListener {
            findNavController().navigate(R.id.action_homeRecruiter_to_profile)
        }

        // Tombol Cepat Buat Lowongan - Menggunakan Action ID dari nav_graph.xml
        binding.btnCreateJobQuick.setOnClickListener {
            findNavController().navigate(R.id.action_homeRecruiter_to_createJob)
        }

        binding.rvReviewApplicants.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadStatistics() {
        val recruiterUid = auth.currentUser?.uid ?: return

        // Hitung Jumlah Lowongan yang dibuat oleh Recruiter ini
        db.collection("jobs").whereEqualTo("recruiterUid", recruiterUid)
            .addSnapshotListener { value, _ ->
                binding.tvJobCount.text = value?.size().toString()
            }

        // Hitung Jumlah Pelamar Masuk
        db.collection("applications").whereEqualTo("recruiterUid", recruiterUid)
            .addSnapshotListener { value, _ ->
                binding.tvApplicantCount.text = value?.size().toString()
            }
    }

    private fun loadApplicantsForReview() {
        // Memuat daftar pelamar real-time
        val recruiterUid = auth.currentUser?.uid ?: return
        db.collection("applications")
            .whereEqualTo("recruiterUid", recruiterUid)
            .limit(5)
            .addSnapshotListener { value, _ ->
                // Perbaikan: ApplicationModel diubah menjadi Application sesuai ModelData.kt
                val list = value?.toObjects(Application::class.java) ?: emptyList()
                // Set Adapter Anda di sini: binding.rvReviewApplicants.adapter = ApplicantAdapter(list)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
