package com.proweb.jobskuy.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentMyApplicationDetailBinding

class MyApplicationDetailFragment : Fragment(R.layout.fragment_my_application_detail) {

    private var _binding: FragmentMyApplicationDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var applicationId: String? = null

    // Variabel lokal penampung jadwal dari data terupdate recruiter
    private var interviewDate = ""
    private var interviewTime = ""
    private var interviewLocation = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyApplicationDetailBinding.bind(view)

        applicationId = arguments?.getString("APPLICATION_ID")

        if (applicationId != null) {
            listenToApplicationStatus()
        }

        // AKSI MEMBUKA POP-UP JADWAL: Sekarang dipastikan menangkap data kiriman recruiter
        binding.btnViewInterviewSchedule.setOnClickListener {
            showSchedulePopup()
        }
    }

    private fun listenToApplicationStatus() {
        db.collection("applications").document(applicationId!!)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val status = snapshot.getString("status") ?: "Diproses"
                val jobId = snapshot.getString("jobId") ?: ""

                binding.tvTimelineStatusText.text = "Status Lamaran: $status"

                // PERBAIKAN WARNA STATUS KONDISIONAL DI DETAIL
                when (status) {
                    "Wawancara" -> {
                        binding.tvTimelineStatusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))

                        // Menarik data koordinasi jadwal wawancara asli
                        interviewDate = snapshot.getString("interviewDate") ?: "Belum diatur"
                        interviewTime = snapshot.getString("interviewTime") ?: "Belum diatur"
                        interviewLocation = snapshot.getString("interviewLocation") ?: "Belum diatur"

                        binding.btnViewInterviewSchedule.visibility = View.VISIBLE
                        binding.layoutRejectionFeedback.visibility = View.GONE
                    }
                    "Ditolak" -> {
                        binding.tvTimelineStatusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                        val feedback = snapshot.getString("rejectionNotes") ?: "Tetap semangat, jangan menyerah!"
                        binding.tvRejectionFeedbackNotes.text = feedback

                        binding.layoutRejectionFeedback.visibility = View.VISIBLE
                        binding.btnViewInterviewSchedule.visibility = View.GONE
                    }
                    else -> { // Status "Diproses"
                        binding.tvTimelineStatusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
                        binding.btnViewInterviewSchedule.visibility = View.GONE
                        binding.layoutRejectionFeedback.visibility = View.GONE
                    }
                }

                // Ambil info nama pekerjaan
                if (jobId.isNotEmpty()) {
                    db.collection("jobs").document(jobId).get().addOnSuccessListener { jobDoc ->
                        if (_binding != null && jobDoc.exists()) {
                            binding.tvMyBadgeJobTitle.text = jobDoc.getString("jobTitle") ?: "Pekerjaan"
                            binding.tvMyBadgeCompanyName.text = jobDoc.getString("companyName") ?: "Perusahaan"
                        }
                    }
                }

                // Tampilkan nama berkas yang dikirim
                val docsMap = snapshot.get("uploadedDocuments") as? Map<String, String>
                if (!docsMap.isNullOrEmpty()) {
                    binding.tvMySubmittedDocs.text = "✓ " + docsMap.keys.joinToString("\n✓ ")
                } else {
                    binding.tvMySubmittedDocs.text = "Tidak melampirkan berkas dokumen."
                }
            }
    }

    private fun showSchedulePopup() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_view_schedule, null)

        // Suntikkan data jadwal yang berhasil ditarik dari snapshot tadi ke dalam pop-up dialog
        dialogView.findViewById<TextView>(R.id.tvPopupShowDate).text = interviewDate
        dialogView.findViewById<TextView>(R.id.tvPopupShowTime).text = interviewTime
        dialogView.findViewById<TextView>(R.id.tvPopupShowLocation).text = interviewLocation

        AlertDialog.Builder(requireContext())
            .setTitle("📅 Rincian Jadwal Wawancara")
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}