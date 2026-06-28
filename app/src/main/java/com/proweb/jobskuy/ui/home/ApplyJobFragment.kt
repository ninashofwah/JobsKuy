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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentApplyJobBinding
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ApplyJobFragment : Fragment(R.layout.fragment_apply_job) {

    private var _binding: FragmentApplyJobBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var jobId: String? = null
    private var recruiterUid: String = ""

    private val uploadedDocumentsMap = hashMapOf<String, String>()
    private var profileSavedDocsMap = hashMapOf<String, String>() // Gudang lokal penampung file dari profil

    private var currentSelectingDocName: String? = null
    private var currentClickedButton: Button? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fileUri: Uri? = result.data?.data
            val docName = currentSelectingDocName
            val targetButton = currentClickedButton

            if (fileUri != null && docName != null && targetButton != null) {
                processFileToBase64(fileUri, docName, targetButton)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplyJobBinding.bind(view)

        jobId = arguments?.getString("JOB_ID")
        val currentUid = auth.currentUser?.uid

        // 1. Ambil Gudang Dokumen Pribadi dari Akun Seeker Terlebih Dahulu
        if (currentUid != null) {
            binding.tvApplySeekerEmail.text = "Email: ${auth.currentUser?.email}"

            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { userDoc ->
                    if (isAdded && _binding != null && userDoc.exists()) {
                        binding.tvApplySeekerName.text = "Nama Lengkap: " + (userDoc.getString("name") ?: "")

                        // Tarik map savedDocuments dari profil
                        val savedDocs = userDoc.get("savedDocuments") as? Map<String, String>
                        if (!savedDocs.isNullOrEmpty()) {
                            profileSavedDocsMap.putAll(savedDocs)
                        }

                        // Load data lowongan setelah data profil siap
                        loadJobDetailsData()
                    }
                }
        }

        binding.btnSubmitApplication.setOnClickListener {
            val notes = binding.etApplyNotes.text.toString().trim()
            if (currentUid == null || jobId == null) return@setOnClickListener

            binding.btnSubmitApplication.isEnabled = false
            val applicationId = db.collection("applications").document().id

            val applicationMap = hashMapOf(
                "applicationId" to applicationId,
                "jobId" to jobId,
                "recruiterUid" to recruiterUid,
                "seekerUid" to currentUid,
                "seekerEmail" to auth.currentUser?.email,
                "notes" to notes,
                "uploadedDocuments" to uploadedDocumentsMap,
                "status" to "Diproses",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("applications").document(applicationId).set(applicationMap)
                .addOnSuccessListener {
                    if (isAdded && _binding != null) {
                        Toast.makeText(context, "Lamaran Berhasil Dikirim!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack(R.id.homeSeekerFragment, false)
                    }
                }
        }
    }

    private fun loadJobDetailsData() {
        if (jobId == null) return
        db.collection("jobs").document(jobId!!).get()
            .addOnSuccessListener { doc ->
                if (isAdded && _binding != null && doc.exists()) {
                    val job = doc.toObject(Job::class.java)
                    if (job != null) {
                        binding.tvApplySummaryTitle.text = job.jobTitle
                        binding.tvApplySummaryCompany.text = job.companyName
                        recruiterUid = job.recruiterUid

                        // Generate field unggah dokumen
                        generateUploadFields(job.requiredDocuments)
                    }
                }
            }
    }

    private fun generateUploadFields(documentsString: String) {
        if (documentsString.isEmpty()) return

        val documentList = documentsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        binding.containerDynamicUploads.removeAllViews()

        for (docName in documentList) {
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 8, 0, 8)
                }
            }

            val tvDocLabel = TextView(requireContext()).apply {
                text = "Unggah $docName"
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnUpload = Button(requireContext()).apply {
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                // LOGIKA AUTO-FILL: Cek apakah dokumen ini sudah ada di gudang profil pribadi
                if (profileSavedDocsMap.containsKey(docName)) {
                    val savedBase64 = profileSavedDocsMap[docName]!!
                    uploadedDocumentsMap[docName] = savedBase64 // Masukkan langsung ke map pengiriman lamaran

                    text = "✓ Terisi Otomatis"
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    text = "Pilih File"
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }

                setOnClickListener {
                    currentSelectingDocName = docName
                    currentClickedButton = this
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                    filePickerLauncher.launch(intent)
                }
            }

            rowLayout.addView(tvDocLabel)
            rowLayout.addView(btnUpload)
            binding.containerDynamicUploads.addView(rowLayout)
        }
    }

    private fun processFileToBase64(fileUri: Uri, docName: String, button: Button) {
        try {
            button.isEnabled = false
            button.text = "Memproses..."

            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(fileUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)

            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            uploadedDocumentsMap[docName] = base64String

            button.isEnabled = true
            button.text = "✓ Terbaca"
            button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark)
            Toast.makeText(context, "$docName berhasil dimuat!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            button.isEnabled = true
            button.text = "Pilih File"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}