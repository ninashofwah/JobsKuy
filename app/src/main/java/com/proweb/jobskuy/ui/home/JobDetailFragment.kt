package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Application
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentJobDetailBinding

class JobDetailFragment : Fragment(R.layout.fragment_job_detail) {

    private var _binding: FragmentJobDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var targetJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJobDetailBinding.bind(view)

        val jobId = arguments?.getString("JOB_ID") ?: return

        db.collection("jobs").document(jobId).get().addOnSuccessListener { doc ->
            if (doc != null) {
                targetJob = doc.toObject(Job::class.java)
                binding.tvDetailTitle.text = targetJob?.jobTitle
                binding.tvDetailCompany.text = targetJob?.companyName
                binding.tvDetailSalary.text = targetJob?.salary
                binding.tvDetailDesc.text = targetJob?.jobDescription
                binding.tvDetailDocs.text = targetJob?.requiredDocuments
            }
        }

        binding.btnApplyJob.setOnClickListener {
            val user = auth.currentUser
            val job = targetJob
            if (user != null && job != null) {
                val appRef = db.collection("applications").document()
                val newApp = Application(
                    applicationId = appRef.id,
                    jobId = job.jobId,
                    recruiterUid = job.recruiterUid,
                    seekerUid = user.uid,
                    seekerName = user.email?.substringBefore("@") ?: "Pelamar Kerja",
                    jobTitle = job.jobTitle,
                    companyName = job.companyName,
                    appliedAt = System.currentTimeMillis(),
                    status = "Diproses" // Status awal pelacakan lamaran
                )

                appRef.set(newApp).addOnSuccessListener {
                    Toast.makeText(context, "Sukses melamar pekerjaan!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}