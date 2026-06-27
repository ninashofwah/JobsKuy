package com.proweb.jobskuy.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentMyJobDetailBinding

class MyJobDetailFragment : Fragment(R.layout.fragment_my_job_detail) {

    private var _binding: FragmentMyJobDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var targetJobId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyJobDetailBinding.bind(view)

        targetJobId = arguments?.getString("JOB_ID")

        if (targetJobId != null) {
            db.collection("jobs").document(targetJobId!!).get()
                .addOnSuccessListener { doc ->
                    if (isAdded && _binding != null && doc.exists()) {
                        val job = doc.toObject(Job::class.java)
                        if (job != null) {
                            binding.tvShowJobTitle.text = job.jobTitle
                            binding.tvShowCompanyName.text = job.companyName
                            binding.tvShowSalary.text = job.salary
                            binding.tvShowDescription.text = job.jobDescription
                            binding.tvShowDocuments.text = job.requiredDocuments
                        }
                    }
                }
        }

        binding.btnEditJob.setOnClickListener {
            val bundle = Bundle().apply { putString("JOB_ID", targetJobId) }
            findNavController().navigate(R.id.action_myJobDetail_to_editJob, bundle)
        }

        binding.btnDeleteJob.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Lowongan")
                .setMessage("Apakah Anda yakin ingin menghapus lowongan pekerjaan ini secara permanen?")
                .setPositiveButton("Ya, Hapus") { _, _ ->
                    targetJobId?.let { id ->
                        db.collection("jobs").document(id).delete()
                            .addOnSuccessListener {
                                if (isAdded && _binding != null) {
                                    Toast.makeText(context, "Lowongan Berhasil Dihapus!", Toast.LENGTH_SHORT).show()
                                    findNavController().popBackStack()
                                }
                            }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}