package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentCreateJobBinding

class CreateJobFragment : Fragment(R.layout.fragment_create_job) {

    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateJobBinding.bind(view)

        binding.btnSaveJob.setOnClickListener {
            val company = binding.etCompanyName.text.toString().trim()
            val title = binding.etJobTitle.text.toString().trim()
            val desc = binding.etJobDescription.text.toString().trim()
            val docs = binding.etRequiredDocuments.text.toString().trim()
            val maxStr = binding.etMaxApplicants.text.toString().trim()
            val salary = binding.etSalary.text.toString().trim()

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(context, "Sesi habis, silakan login kembali.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (company.isEmpty() || title.isEmpty() || desc.isEmpty() || docs.isEmpty() || maxStr.isEmpty() || salary.isEmpty()) {
                Toast.makeText(context, "Harap lengkapi semua data lowongan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maxApplicants = maxStr.toIntOrNull() ?: 0
            val jobDocRef = db.collection("jobs").document()

            val newJob = Job(
                jobId = jobDocRef.id,
                recruiterUid = currentUser.uid,
                companyName = company,
                jobTitle = title,
                jobDescription = desc,
                requiredDocuments = docs,
                maxApplicants = maxApplicants,
                currentApplicants = 0,
                salary = salary,
                createdAt = System.currentTimeMillis()
            )

            jobDocRef.set(newJob)
                .addOnSuccessListener {
                    Toast.makeText(context, "Lowongan Berhasil Diunggah!", Toast.LENGTH_SHORT).show()
                    // Solusi kembali ke dashboard secara aman lewat rute NavGraph resmi
                    findNavController().navigate(R.id.action_createJob_to_homeRecruiter)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal mengunggah: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}