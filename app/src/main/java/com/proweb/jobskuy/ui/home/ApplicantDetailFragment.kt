package com.proweb.jobskuy.ui.home

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentApplicantDetailBinding

class ApplicantDetailFragment : Fragment(R.layout.fragment_applicant_detail) {

    private var _binding: FragmentApplicantDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var applicationId: String? = null
    private var seekerUid: String? = null
    private var seekerName: String = "Pelamar"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplicantDetailBinding.bind(view)

        applicationId = arguments?.getString("APPLICATION_ID")

        if (applicationId != null) {
            loadApplicationDetails()
        }

        binding.ivDetailSeekerPhoto.setOnClickListener {
            if (seekerUid != null) {
                val bundle = Bundle().apply {
                    putString("SEEKER_UID", seekerUid)
                }
                findNavController().navigate(R.id.action_applicantDetail_to_profileSeekerFragment, bundle)
            } else {
                Toast.makeText(context, "ID Pengguna belum siap.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnProcessReject.setOnClickListener {
            showRejectDialog()
        }

        binding.btnProcessInterview.setOnClickListener {
            showInterviewDialog()
        }
    }

    private fun loadApplicationDetails() {
        db.collection("applications").document(applicationId!!).get()
            .addOnSuccessListener { doc ->
                if (isAdded && _binding != null && doc.exists()) {
                    seekerUid = doc.getString("seekerUid")
                    binding.tvDetailSeekerEmail.text = doc.getString("seekerEmail") ?: "-"
                    binding.tvDetailSeekerNotes.text = doc.getString("notes") ?: "Tidak ada pesan."

                    val currentStatus = doc.getString("status") ?: "Diproses"
                    binding.tvDetailCurrentStatus.text = "Status Saat Ini: $currentStatus"

                    // PERBAIKAN WARNA TEXT INDIKATOR STATUS DI DETAIL RECRUITER
                    updateStatusTextColor(currentStatus)

                    val docsMap = doc.get("uploadedDocuments") as? Map<String, String>
                    if (!docsMap.isNullOrEmpty()) {
                        val firstDoc = docsMap.entries.first()
                        binding.tvDetailDocNameHeader.text = "File Lampiran: ${firstDoc.key}"
                        val bitmap = decodeBase64ToBitmap(firstDoc.value)
                        if (bitmap != null) binding.ivDetailDocumentImage.setImageBitmap(bitmap)
                    } else {
                        binding.tvDetailDocNameHeader.text = "Tidak ada file dilampirkan."
                    }

                    if (seekerUid != null) {
                        loadSeekerPublicProfile(seekerUid!!)
                    }
                }
            }
    }

    private fun loadSeekerPublicProfile(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded && _binding != null && doc.exists()) {
                    seekerName = doc.getString("name") ?: "Pencari Kerja"
                    val phone = doc.getString("phone") ?: "-"
                    val address = doc.getString("address") ?: "-"
                    val photoBase64 = doc.getString("profilePhoto") ?: ""

                    binding.tvDetailSeekerName.text = "$seekerName (Klik untuk lihat profil publik)"
                    binding.tvDetailSeekerPhone.text = "Telepon: $phone"
                    binding.tvDetailSeekerAddress.text = "Alamat: $address"

                    if (photoBase64.isNotEmpty()) {
                        val bitmap = decodeBase64ToBitmap(photoBase64)
                        if (bitmap != null) binding.ivDetailSeekerPhoto.setImageBitmap(bitmap)
                    }
                }
            }
    }

    private fun updateStatusTextColor(status: String) {
        when (status) {
            "Wawancara" -> {
                binding.tvDetailCurrentStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            }
            "Ditolak" -> {
                binding.tvDetailCurrentStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            }
            else -> {
                binding.tvDetailCurrentStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))
            }
        }
    }

    private fun showRejectDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Konfirmasi Penolakan")

        val inputLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_process_reject, null)
        val etNotes = inputLayout.findViewById<EditText>(R.id.etRejectFeedback)
        builder.setView(inputLayout)

        builder.setPositiveButton("Kirim & Tolak") { dialog, _ ->
            val feedbackText = etNotes.text.toString().trim()
            if (feedbackText.isEmpty()) {
                Toast.makeText(context, "Harap berikan pesan masukan!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val updateData = hashMapOf<String, Any>(
                "status" to "Ditolak",
                "rejectionNotes" to feedbackText
            )

            db.collection("applications").document(applicationId!!)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, "Proses Penolakan Selesai", Toast.LENGTH_SHORT).show()
                    binding.tvDetailCurrentStatus.text = "Status Saat Ini: Ditolak"
                    updateStatusTextColor("Ditolak")
                    dialog.dismiss()
                }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun showInterviewDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Jadwalkan Wawancara")

        val inputLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_process_interview, null)
        val etDate = inputLayout.findViewById<EditText>(R.id.etInterviewDate)
        val etTime = inputLayout.findViewById<EditText>(R.id.etInterviewTime)
        val etLocation = inputLayout.findViewById<EditText>(R.id.etInterviewLocation)
        builder.setView(inputLayout)

        builder.setPositiveButton("Kirim Undangan") { dialog, _ ->
            val date = etDate.text.toString().trim()
            val time = etTime.text.toString().trim()
            val location = etLocation.text.toString().trim()

            if (date.isEmpty() || time.isEmpty() || location.isEmpty()) {
                Toast.makeText(context, "Harap lengkapi komponen jadwal!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val updateData = hashMapOf<String, Any>(
                "status" to "Wawancara",
                "interviewDate" to date,
                "interviewTime" to time,
                "interviewLocation" to location
            )

            db.collection("applications").document(applicationId!!)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, "Undangan Terkirim!", Toast.LENGTH_SHORT).show()
                    binding.tvDetailCurrentStatus.text = "Status Saat Ini: Wawancara"
                    updateStatusTextColor("Wawancara")
                    dialog.dismiss()
                }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}