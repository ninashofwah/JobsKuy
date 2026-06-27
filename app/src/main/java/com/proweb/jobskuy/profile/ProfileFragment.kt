package com.proweb.jobskuy.ui.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentProfileBinding
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var currentName = ""
    private var currentPhone = ""
    private var currentAddress = ""
    private var currentPhotoBase64 = ""

    private var dialogImageView: ImageView? = null

    private val profilePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                processPhotoToBase64(imageUri)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        loadProfileData()

        binding.btnEditProfileRecruiter.setOnClickListener {
            showEditDialog()
        }

        binding.btnLogoutRecruiter.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "Berhasil keluar!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileRecruiter_to_roleSelection)
        }
    }

    private fun loadProfileData() {
        val currentUid = auth.currentUser?.uid
        val currentEmail = auth.currentUser?.email

        if (currentUid != null) {
            binding.tvProfileEmail.text = currentEmail ?: "-"

            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { document ->
                    if (isAdded && _binding != null && document != null && document.exists()) {
                        currentName = document.getString("name") ?: ""
                        currentPhone = document.getString("phone") ?: ""
                        currentAddress = document.getString("address") ?: ""
                        currentPhotoBase64 = document.getString("profilePhoto") ?: ""
                        val roleStr = document.getString("role") ?: "Recruiter"

                        binding.tvProfileName.text = if (currentName.isEmpty()) "-" else currentName
                        binding.tvProfilePhone.text = "Telepon: ${if (currentPhone.isEmpty()) "-" else currentPhone}"
                        binding.tvProfileAddress.text = "Alamat: ${if (currentAddress.isEmpty()) "-" else currentAddress}"
                        binding.tvProfileRoleBadge.text = "Peran: $roleStr"

                        if (currentPhotoBase64.isNotEmpty()) {
                            val bitmap = decodeBase64ToBitmap(currentPhotoBase64)
                            if (bitmap != null) binding.ivProfilePicRecruiter.setImageBitmap(bitmap)
                        }
                    }
                }
        }
    }

    private fun showEditDialog() {
        val user = auth.currentUser ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEditDialogName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditDialogPhone)
        val etAddress = dialogView.findViewById<EditText>(R.id.etEditDialogAddress)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etEditDialogOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etEditDialogNewPassword)
        val btnChangePhoto = dialogView.findViewById<Button>(R.id.btnDialogChangePhoto)

        dialogImageView = dialogView.findViewById(R.id.ivDialogProfilePicture)

        etName.setText(currentName)
        etPhone.setText(currentPhone)
        etAddress.setText(currentAddress)

        if (currentPhotoBase64.isNotEmpty()) {
            val bitmap = decodeBase64ToBitmap(currentPhotoBase64)
            if (bitmap != null) dialogImageView?.setImageBitmap(bitmap)
        }

        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            profilePhotoLauncher.launch(intent)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profil Anda")
            .setView(dialogView)
            .setPositiveButton("Simpan Perubahan") { dialog, _ ->
                val newName = etName.text.toString().trim()
                val newPhone = etPhone.text.toString().trim()
                val newAddress = etAddress.text.toString().trim()
                val oldPassword = etOldPassword.text.toString().trim()
                val newPassword = etNewPassword.text.toString().trim()

                if (newName.isEmpty()) {
                    Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // JIKA USER INGIN MENGGANTI PASSWORD
                if (oldPassword.isNotEmpty() || newPassword.isNotEmpty()) {
                    if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                        Toast.makeText(context, "Keduanya (Password Lama & Baru) wajib diisi jika ingin ganti password!", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    if (newPassword.length < 6) {
                        Toast.makeText(context, "Password baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Pembuatan Kredensial Otentikasi Ulang Firebase Auth
                    val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)

                    user.reauthenticate(credential)
                        .addOnSuccessListener {
                            // Jika password lama benar, eksekusi update password baru
                            user.updatePassword(newPassword)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                    saveDataToFirestore(newName, newPhone, newAddress, dialog)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Gagal ganti password: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Password lama salah! Gagal verifikasi.", Toast.LENGTH_LONG).show()
                        }
                } else {
                    // Jika tidak ganti password, langsung simpan biodata profil saja
                    saveDataToFirestore(newName, newPhone, newAddress, dialog)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveDataToFirestore(name: String, phone: String, address: String, dialog: DialogInterface) {
        val currentUid = auth.currentUser?.uid ?: return
        val updateMap = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "address" to address,
            "profilePhoto" to currentPhotoBase64
        )

        db.collection("users").document(currentUid)
            .set(updateMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Profil Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                loadProfileData()
                dialog.dismiss()
            }
    }

    private fun processPhotoToBase64(uri: Uri) {
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 25, outputStream)
            val byteArray = outputStream.toByteArray()
            currentPhotoBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

            val bitmapPreview = decodeBase64ToBitmap(currentPhotoBase64)
            if (bitmapPreview != null) dialogImageView?.setImageBitmap(bitmapPreview)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membaca foto: ${e.message}", Toast.LENGTH_SHORT).show()
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
