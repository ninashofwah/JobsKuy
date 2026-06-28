package com.proweb.jobskuy.ui.home

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
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
    private var jobId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJobDetailBinding.bind(view)

        jobId = arguments?.getString("JOB_ID")

        if (jobId != null) {
            db.collection("jobs").document(jobId!!).get()
                .addOnSuccessListener { doc ->
                    if (isAdded && _binding != null && doc.exists()) {
                        val job = doc.toObject(Job::class.java)
                        if (job != null) {
                            binding.tvJobDetailTitle.text = job.jobTitle
                            binding.tvJobDetailCompany.text = job.companyName
                            binding.tvJobDetailSalary.text = "${job.salary} / bulan"
                            binding.tvJobDetailDescription.text = job.jobDescription

                            // RENDERING UTAMA: Menampilkan foto lokasi kerja dari string Base64 lowongan
                            if (!job.jobImage.isNullOrEmpty()) {
                                try {
                                    val bytes = Base64.decode(job.jobImage, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    binding.ivJobDetailHeaderImage.setImageBitmap(bitmap)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    }
                }
        }

        binding.btnApplyJobNow.setOnClickListener {
            val bundle = Bundle().apply { putString("JOB_ID", jobId) }
            findNavController().navigate(R.id.action_jobDetail_to_applyJob, bundle)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}