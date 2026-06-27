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

        if (jobId != null) {
            db.collection("jobs").document(jobId!!).get()
                .addOnSuccessListener { doc ->
                    if (isAdded && _binding != null && doc.exists()) {
                        val job = doc.toObject(Job::class.java)
                        if (job != null) {
                            binding.tvApplySummaryTitle.text = job.jobTitle
                            binding.tvApplySummaryCompany.text = job.companyName
                            recruiterUid = job.recruiterUid

                            // MEMBACA DAN MEWAKILI JUMLAH FILE SECARA DINAMIS SESUAI BAGIAN BADGE RECRUITER
                            generateUploadFields(job.requiredDocuments)
                        }
                    }
                }
        }

        val currentUid = auth.currentUser?.uid
        val currentEmail = auth.currentUser?.email
        if (currentUid != null) {
            binding.tvApplySeekerEmail.text = "Email: $currentEmail"
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    if (isAdded && _binding != null && doc.exists()) {
                        val fullName = doc.getString("name") ?: "Pencari Kerja"
                        binding.tvApplySeekerName.text = "Nama Lengkap: $fullName"
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
                "seekerEmail" to currentEmail,
                "notes" to notes,
                "uploadedDocuments" to uploadedDocumentsMap,
                "status" to "Diproses",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("applications").document(applicationId)
                .set(applicationMap)
                .addOnSuccessListener {
                    if (isAdded && _binding != null) {
                        Toast.makeText(context, "Lamaran Kerja & Berkas Sukses Dikirim!", Toast.LENGTH_SHORT).show()
                        binding.btnSubmitApplication.isEnabled = true
                        findNavController().popBackStack(R.id.homeSeekerFragment, false)
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded && _binding != null) {
                        binding.btnSubmitApplication.isEnabled = true
                        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
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
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
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
                text = "Pilih File"
                textSize = 12f
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                setOnClickListener {
                    currentSelectingDocName = docName
                    currentClickedButton = this

                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
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
            val byteArray = outputStream.toByteArray()

            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            uploadedDocumentsMap[docName] = base64String

            button.isEnabled = true
            button.text = "✓ Terbaca"
            button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark)
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            Toast.makeText(context, "$docName berhasil diproses!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            button.isEnabled = true
            button.text = "Gagal"
            button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark)
            Toast.makeText(context, "Gagal memproses berkas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}