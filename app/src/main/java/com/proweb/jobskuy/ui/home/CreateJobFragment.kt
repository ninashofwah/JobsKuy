package com.proweb.jobskuy.ui.home

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentCreateJobBinding
import java.io.ByteArrayOutputStream
import java.io.InputStream

class CreateJobFragment : Fragment(R.layout.fragment_create_job) {

    private var _binding: FragmentCreateJobBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val dynamicDocumentRequirements = mutableListOf<String>()
    private var jobImageBase64: String = ""

    // Variabel penampung koordinat lokasi recruiter yang ditarik dari profil registrasi
    private var recruiterLatitude: Double = 0.0
    private var recruiterLongitude: Double = 0.0

    private val imageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) processJobImage(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateJobBinding.bind(view)

        // 1. AMBIL KOORDINAT LOKASI ASLI RECRUITER DARI PROFILE USERS SEJAK AWAL
        loadRecruiterLocationProfile()

        binding.btnSelectJobImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imageLauncher.launch(intent)
        }

        binding.btnAddDocumentBadge.setOnClickListener {
            val docName = binding.etInputDocumentName.text.toString().trim()
            if (docName.isNotEmpty() && !dynamicDocumentRequirements.contains(docName)) {
                dynamicDocumentRequirements.add(docName)
                addBadgeToContainer(docName)
                binding.etInputDocumentName.text.clear()
            }
        }

        binding.btnSaveJob.setOnClickListener {
            val company = binding.etCompanyName.text.toString().trim()
            val title = binding.etJobTitle.text.toString().trim()
            val desc = binding.etJobDescription.text.toString().trim()
            val maxStr = binding.etMaxApplicants.text.toString().trim()
            val salary = binding.etSalary.text.toString().trim()

            if (company.isEmpty() || title.isEmpty() || desc.isEmpty() || maxStr.isEmpty() || salary.isEmpty()) {
                Toast.makeText(context, "Lengkapi semua data lowongan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requiredDocsString = dynamicDocumentRequirements.joinToString(", ")
            val jobDocRef = db.collection("jobs").document()

            // 3. SEKARANG LOKASI OTOMATIS TERISI DARI KOORDINAT PROFIL RECRUITER
            val newJob = Job(
                jobId = jobDocRef.id,
                recruiterUid = auth.currentUser?.uid ?: "",
                companyName = company,
                jobTitle = title,
                jobDescription = desc,
                requiredDocuments = requiredDocsString,
                maxApplicants = maxStr.toIntOrNull() ?: 0,
                salary = salary,
                jobImage = jobImageBase64,
                latitude = recruiterLatitude,   // Terisi koordinat registrasi
                longitude = recruiterLongitude, // Terisi koordinat registrasi
                isClosed = false,
                createdAt = System.currentTimeMillis()
            )

            jobDocRef.set(newJob).addOnSuccessListener {
                Toast.makeText(context, "Lowongan Berhasil Dipasang dengan Lokasi Terkunci!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    /**
     * Fungsi mengambil data profil koordinat GPS recruiter saat awal registrasi
     */
    private fun loadRecruiterLocationProfile() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                if (isAdded && _binding != null && doc.exists()) {
                    // Ambil latitude & longitude bawaan registrasi perfil, default 0.0 jika kosong
                    recruiterLatitude = doc.getDouble("latitude") ?: 0.0
                    recruiterLongitude = doc.getDouble("longitude") ?: 0.0
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal sinkronisasi data lokasi profil.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processJobImage(uri: Uri) {
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos)
            jobImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            binding.ivJobLocationPreview.setImageBitmap(bitmap)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun addBadgeToContainer(name: String) {
        val badgeButton = Button(requireContext()).apply {
            text = "📄 $name"
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
            setOnClickListener {
                dynamicDocumentRequirements.remove(name)
                (parent as? ViewGroup)?.removeView(this)
            }
        }
        binding.containerDocumentBadges.addView(badgeButton)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
