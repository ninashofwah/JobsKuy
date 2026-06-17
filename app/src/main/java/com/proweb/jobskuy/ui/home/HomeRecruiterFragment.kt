package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentHomeRecruiterBinding

class HomeRecruiterFragment : Fragment(R.layout.fragment_home_recruiter) {

    private var _binding: FragmentHomeRecruiterBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeRecruiterBinding.bind(view)

        binding.rvIncomingApplicants.layoutManager = LinearLayoutManager(requireContext())

        val recruiterUid = auth.currentUser?.uid ?: ""
        if (recruiterUid.isNotEmpty()) {

            db.collection("users").document(recruiterUid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val companyName = doc.getString("fullName") ?: "Pemilik UMKM"
                    binding.tvWelcomeRecruiter.text = "Dashboard $companyName"
                }
            }
            loadIncomingApplicants(recruiterUid)
        }

        binding.fabAddJob.setOnClickListener {

            Toast.makeText(context, "Membuka formulir pembuatan lowongan baru...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadIncomingApplicants(recruiterId: String) {
        db.collection("applications")
            .whereEqualTo("recruiterUid", recruiterId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                } else {
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}