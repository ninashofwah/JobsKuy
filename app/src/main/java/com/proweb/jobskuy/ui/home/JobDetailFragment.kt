package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentJobDetailBinding

class JobDetailFragment : Fragment(R.layout.fragment_job_detail) {

    private var _binding: FragmentJobDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var currentJobId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJobDetailBinding.bind(view)

        currentJobId = arguments?.getString("JOB_ID")

        if (currentJobId != null) {
            db.collection("jobs").document(currentJobId!!).get()
                .addOnSuccessListener { doc ->
                    if (isAdded && _binding != null && doc.exists()) {
                        val job = doc.toObject(Job::class.java)
                        if (job != null) {
                            binding.tvDetailJobTitle.text = job.jobTitle
                            binding.tvDetailCompanyName.text = job.companyName
                            binding.tvDetailSalary.text = "Gaji: Rp ${job.salary}"
                            binding.tvDetailDescription.text = job.jobDescription
                            binding.tvDetailRequiredDocs.text = job.requiredDocuments
                        }
                    }
                }
        }

        binding.btnNavigateToApply.setOnClickListener {
            val bundle = Bundle().apply { putString("JOB_ID", currentJobId) }
            // PERBAIKAN MUTLAK: Menggunakan rute aksi milik JobDetailFragment secara sah
            findNavController().navigate(R.id.action_jobDetail_to_applyJob, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}