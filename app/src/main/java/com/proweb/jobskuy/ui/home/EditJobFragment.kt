package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentEditJobBinding

class EditJobFragment : Fragment(R.layout.fragment_edit_job) {

    private var _binding: FragmentEditJobBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var jobId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditJobBinding.bind(view)

        jobId = arguments?.getString("JOB_ID")

        if (jobId != null) {
            db.collection("jobs").document(jobId!!).get()
                .addOnSuccessListener { doc ->
                    if (isAdded && _binding != null && doc.exists()) {
                        val job = doc.toObject(Job::class.java)
                        if (job != null) {
                            binding.etEditCompanyName.setText(job.companyName)
                            binding.etEditJobTitle.setText(job.jobTitle)
                            binding.etEditSalary.setText(job.salary)
                            binding.etEditJobDescription.setText(job.jobDescription)
                            binding.etEditRequiredDocuments.setText(job.requiredDocuments)
                        }
                    }
                }
        }

        binding.btnUpdateJob.setOnClickListener {
            val company = binding.etEditCompanyName.text.toString().trim()
            val title = binding.etEditJobTitle.text.toString().trim()
            val salary = binding.etEditSalary.text.toString().trim()
            val desc = binding.etEditJobDescription.text.toString().trim()
            val docs = binding.etEditRequiredDocuments.text.toString().trim()

            if (company.isEmpty() || title.isEmpty() || salary.isEmpty() || desc.isEmpty() || docs.isEmpty()) {
                Toast.makeText(context, "Semua kolom edit wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (jobId != null) {
                val updateMap = hashMapOf<String, Any>(
                    "companyName" to company,
                    "jobTitle" to title,
                    "salary" to salary,
                    "jobDescription" to desc,
                    "requiredDocuments" to docs
                )

                db.collection("jobs").document(jobId!!).update(updateMap)
                    .addOnSuccessListener {
                        if (isAdded && _binding != null) {
                            Toast.makeText(context, "Data Lowongan Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                    .addOnFailureListener { e ->
                        if (isAdded && _binding != null) {
                            Toast.makeText(context, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}