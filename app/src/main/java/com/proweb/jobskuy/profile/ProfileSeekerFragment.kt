package com.proweb.jobskuy.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentProfileSeekerBinding
import java.io.File
import java.io.FileOutputStream

class ProfileSeekerFragment : Fragment(R.layout.fragment_profile_seeker) {

    private var _binding: FragmentProfileSeekerBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var targetSeekerUid: String? = null
    private var isMe = false
    private var selectedVideoBase64: String? = null

    // Launcher untuk memilih berkas video dari galeri HP
    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri = result.data?.data
            if (videoUri != null) {
                processAndPrepareVideo(videoUri)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileSeekerBinding.bind(view)

        // Memeriksa siapa yang sedang melihat halaman ini
        val argsUid = arguments?.getString("SEEKER_UID")
        val currentUid = auth.currentUser?.uid

        if (argsUid == null || argsUid == currentUid) {
            targetSeekerUid = currentUid
            isMe = true
        } else {
            targetSeekerUid = argsUid
            isMe = false
        }

        setupViewRoleConfig()
        loadPublicProfileData()

        binding.btnUploadVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "video/*" }
            videoPickerLauncher.launch(intent)
        }

        binding.btnSavePublicProfile.setOnClickListener {
            saveProfileToFirestore()
        }
    }

    private fun setupViewRoleConfig() {
        if (isMe) {
            // Mode Pemilik: Bisa edit dan upload video
            binding.tvProfileTitle.text = "Kelola Profil Publik Anda"
            binding.btnUploadVideo.visibility = View.VISIBLE
            binding.btnSavePublicProfile.visibility = View.VISIBLE
            binding.etPublicExperience.isEnabled = true
        } else {
            // Mode Recruiter: Hanya bisa melihat dan memutar video
            binding.tvProfileTitle.text = "Profil Publik Pelamar"
            binding.btnUploadVideo.visibility = View.GONE
            binding.btnSavePublicProfile.visibility = View.GONE
            binding.etPublicExperience.isEnabled = false
        }
    }

    private fun loadPublicProfileData() {
        if (targetSeekerUid == null) return

        db.collection("users").document(targetSeekerUid!!).get()
            .addOnSuccessListener { doc ->
                if (_binding == null || !doc.exists()) return@addOnSuccessListener

                binding.tvPublicName.text = doc.getString("name") ?: "Pencari Kerja"
                binding.tvPublicHeadline.text = doc.getString("headline") ?: "Android Developer"
                binding.etPublicExperience.setText(doc.getString("publicExperience") ?: "")

                // Load Foto Profil
                val photoBase64 = doc.getString("profilePhoto") ?: ""
                if (photoBase64.isNotEmpty()) {
                    val bitmap = decodeBase64ToBitmap(photoBase64)
                    if (bitmap != null) binding.ivPublicProfilePhoto.setImageBitmap(bitmap)
                }

                // Load Video Perkenalan
                val videoBase64 = doc.getString("introVideoBase64") ?: ""
                if (videoBase64.isNotEmpty()) {
                    binding.tvVideoPlaceholder.visibility = View.GONE
                    playBase64Video(videoBase64)
                } else {
                    binding.tvVideoPlaceholder.visibility = View.VISIBLE
                }
            }
    }

    private fun processAndPrepareVideo(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                // Batasi file video sederhana agar memori Firestore aman (< 1.5MB)
                if (bytes.size > 1500000) {
                    Toast.makeText(context, "Ukuran video terlalu besar! Maksimal 1.5 MB.", Toast.LENGTH_LONG).show()
                    return
                }
                selectedVideoBase64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                binding.tvVideoPlaceholder.visibility = View.GONE
                playBase64Video(selectedVideoBase64!!)
                Toast.makeText(context, "Video berhasil dimuat, siap disimpan!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memproses berkas video.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playBase64Video(base64Str: String) {
        try {
            val videoBytes = Base64.decode(base64Str, Base64.DEFAULT)
            // Simpan sementara di cache lokal hp untuk dimainkan oleh VideoView
            val tempFile = File(requireContext().cacheDir, "intro_temp.mp4")
            val fos = FileOutputStream(tempFile)
            fos.write(videoBytes)
            fos.close()

            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(binding.vvIntroductionVideo)

            binding.vvIntroductionVideo.apply {
                setMediaController(mediaController)
                setVideoPath(tempFile.absolutePath)
                requestFocus()
                setOnPreparedListener { start() } // Auto play saat siap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveProfileToFirestore() {
        val experienceText = binding.etPublicExperience.text.toString().trim()

        val updatedMap = hashMapOf<String, Any>(
            "publicExperience" to experienceText
        )

        if (selectedVideoBase64 != null) {
            updatedMap["introVideoBase64"] = selectedVideoBase64!!
        }

        db.collection("users").document(targetSeekerUid!!)
            .set(updatedMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Profil Publik Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal mengunggah ke server.", Toast.LENGTH_SHORT).show()
            }
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