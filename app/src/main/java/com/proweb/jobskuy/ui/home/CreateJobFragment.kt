package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
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

    // Menyimpan daftar nama dokumen tambahan yang diinput oleh recruiter
    private val dynamicDocumentRequirements = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateJobBinding.bind(view)

        // 1. Logika Klik Tombol "+ Tambah" Dokumen Berkas Baru
        binding.btnAddDocumentBadge.setOnClickListener {
            val docName = binding.etInputDocumentName.text.toString().trim()
            if (docName.isEmpty()) {
                Toast.makeText(context, "Ketik nama dokumen dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dynamicDocumentRequirements.contains(docName)) {
                Toast.makeText(context, "Syarat dokumen sudah ditambahkan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tambahkan ke list data lokal
            dynamicDocumentRequirements.add(docName)

            // Gambar Badge Baru ke dalam Layar UI secara real-time
            addBadgeToContainer(docName)

            // Bersihkan kolom input setelah selesai ditambahkan
            binding.etInputDocumentName.text.clear()
        }

        // 2. Kirim Form Data Lowongan Akhir ke Server Cloud Firestore
        binding.btnSaveJob.setOnClickListener {
            val company = binding.etCompanyName.text.toString().trim()
            val title = binding.etJobTitle.text.toString().trim()
            val desc = binding.etJobDescription.text.toString().trim()
            val maxStr = binding.etMaxApplicants.text.toString().trim()
            val salary = binding.etSalary.text.toString().trim()

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(context, "Sesi habis, silakan login kembali.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (company.isEmpty() || title.isEmpty() || desc.isEmpty() || maxStr.isEmpty() || salary.isEmpty()) {
                Toast.makeText(context, "Harap lengkapi seluruh kolom isian lowongan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // MENGGABUNGKAN LIST MENJADI STRING TEKS SEPERTI: "Ijazah, SKCK, Video"
            val requiredDocsString = dynamicDocumentRequirements.joinToString(", ")

            val maxApplicants = maxStr.toIntOrNull() ?: 0
            val jobDocRef = db.collection("jobs").document()

            val newJob = Job(
                jobId = jobDocRef.id,
                recruiterUid = currentUser.uid,
                companyName = company,
                jobTitle = title,
                jobDescription = desc,
                requiredDocuments = requiredDocsString, // Tersimpan rapi dalam bentuk string terpisah koma
                maxApplicants = maxApplicants,
                currentApplicants = 0,
                salary = salary,
                createdAt = System.currentTimeMillis()
            )

            binding.btnSaveJob.isEnabled = false

            jobDocRef.set(newJob)
                .addOnSuccessListener {
                    if (isAdded && _binding != null) {
                        Toast.makeText(context, "Lowongan Berhasil Diunggah!", Toast.LENGTH_SHORT).show()
                        binding.btnSaveJob.isEnabled = true
                        findNavController().popBackStack()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded && _binding != null) {
                        binding.btnSaveJob.isEnabled = true
                        Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    /**
     * Menggambar badge tombol dinamis yang bisa dihapus kembali saat diklik oleh Recruiter
     */
    private fun addBadgeToContainer(name: String) {
        val badgeButton = MaterialButton(requireContext()).apply {
            text = "📄 $name"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
            strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.black)
            strokeWidth = 2

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 12, 0)
            }

            // BONUS KEMUDAHAN: Jika badge diklik kembali oleh recruiter, maka syarat tersebut dibatalkan
            setOnClickListener {
                dynamicDocumentRequirements.remove(name)
                binding.containerDocumentBadges.removeView(this)
                Toast.makeText(context, "$name dihapus dari syarat", Toast.LENGTH_SHORT).show()
            }
        }

        binding.containerDocumentBadges.addView(badgeButton)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
