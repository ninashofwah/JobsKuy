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
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentProfileAccountSeekerBinding
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileAccountSeekerFragment : Fragment(R.layout.fragment_profile_account_seeker) {

    private var _binding: FragmentProfileAccountSeekerBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var currentName = ""
    private var currentPhone = ""
    private var currentAddress = ""
    private var currentPhotoBase64 = ""

    // Variabel pelacak dokumen dinamis yang sedang dipilih oleh pengguna
    private var currentDocTypeSelection: String? = null
    private var currentActiveButton: Button? = null

    private var dialogImageView: ImageView? = null

    private val profilePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) processPhotoToBase64(imageUri)
        }
    }

    private val documentPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val fileUri = result.data?.data
            if (fileUri != null && currentDocTypeSelection != null && currentActiveButton != null) {
                uploadDocumentToFirestore(fileUri, currentDocTypeSelection!!, currentActiveButton!!)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileAccountSeekerBinding.bind(view)

        loadSeekerData()

        binding.btnEditProfileSeeker.setOnClickListener { showEditDialog() }

        binding.btnLogoutSeeker.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "Sesi berakhir!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileAccount_to_roleSelection)
        }

        // AKSI TAMBAH BARIS NAMA DOKUMEN BARU SECARA KUSTOM JALUR KLIK
        binding.btnAddNewProfileDocRow.setOnClickListener {
            val customDocName = binding.etNewProfileDocName.text.toString().trim()
            if (customDocName.isEmpty()) {
                Toast.makeText(context, "Masukkan nama dokumen terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Buat baris penampung baru di layar secara instan
            createDynamicDocRow(customDocName, false)
            binding.etNewProfileDocName.text.clear()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        documentPickerLauncher.launch(intent)
    }

    private fun loadSeekerData() {
        val currentUid = auth.currentUser?.uid ?: return
        binding.tvSeekerProfileEmail.text = auth.currentUser?.email ?: "-"

        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { document ->
                if (isAdded && _binding != null && document != null && document.exists()) {
                    currentName = document.getString("name") ?: ""
                    currentPhone = document.getString("phone") ?: ""
                    currentAddress = document.getString("address") ?: ""
                    currentPhotoBase64 = document.getString("profilePhoto") ?: ""
                    val roleStr = document.getString("role") ?: "Pencari Kerja"

                    binding.tvSeekerProfileName.text = if (currentName.isEmpty()) "-" else currentName
                    binding.tvSeekerProfilePhone.text = "Telepon: ${if (currentPhone.isEmpty()) "-" else currentPhone}"
                    binding.tvSeekerProfileAddress.text = "Alamat: ${if (currentAddress.isEmpty()) "-" else currentAddress}"
                    binding.tvSeekerProfileRole.text = "Kategori Akun: $roleStr"

                    if (currentPhotoBase64.isNotEmpty()) {
                        val bitmap = decodeBase64ToBitmap(currentPhotoBase64)
                        if (bitmap != null) binding.ivProfilePicSeeker.setImageBitmap(bitmap)
                    }

                    // MEMBERSIHKAN KONTANER LAMA DAN MERENDER ULANG SELURUH DAFTAR YANG ADA DI FIRESTORE
                    binding.containerProfileDocuments.removeAllViews()
                    val savedDocs = document.get("savedDocuments") as? Map<String, String>
                    if (!savedDocs.isNullOrEmpty()) {
                        for (docName in savedDocs.keys) {
                            createDynamicDocRow(docName, true)
                        }
                    }
                }
            }
    }

    /**
     * FUNGSI GENERATOR BARIS DOKUMEN SECARA DINAMIS
     */
    private fun createDynamicDocRow(docName: String, isAlreadyUploaded: Boolean) {
        val context = requireContext()

        val rowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val tvDocLabel = TextView(context).apply {
            text = "📄 $docName"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.black))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnUpload = Button(context).apply {
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 8
            }

            if (isAlreadyUploaded) {
                text = "✓ Tersimpan"
                backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_green_dark)
            } else {
                text = "Pilih File"
                backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.black)
            }

            setOnClickListener {
                currentDocTypeSelection = docName
                currentActiveButton = this
                openFilePicker()
            }
        }

        // Tombol Hapus Dokumen Langsung Dari Akun Profil Seeker
        val btnDeleteRow = Button(context).apply {
            text = "X"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.white))
            backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_red_dark)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            setOnClickListener {
                val currentUid = auth.currentUser?.uid ?: return@setOnClickListener

                // Menghapus field map bersangkutan langsung dari Firestore secara bersih
                val updates = hashMapOf<String, Any>(
                    "savedDocuments.$docName" to FieldValue.delete()
                )
                db.collection("users").document(currentUid).update(updates).addOnSuccessListener {
                    binding.containerProfileDocuments.removeView(rowLayout)
                    Toast.makeText(context, "$docName dihapus.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rowLayout.addView(tvDocLabel)
        rowLayout.addView(btnUpload)
        rowLayout.addView(btnDeleteRow)
        binding.containerProfileDocuments.addView(rowLayout)
    }

    private fun uploadDocumentToFirestore(uri: Uri, docType: String, button: Button) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            button.isEnabled = false
            button.text = "Menyimpan..."

            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos)
            val base64Str = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

            val docUpdate = hashMapOf<String, Any>(
                "savedDocuments.$docType" to base64Str
            )

            db.collection("users").document(currentUid)
                .update(docUpdate)
                .addOnSuccessListener {
                    if (_binding != null) {
                        setButtonToUploadedState(button)
                        Toast.makeText(context, "Dokumen $docType Berhasil Tersimpan Permanen!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    val fallbackMap = hashMapOf("savedDocuments" to hashMapOf(docType to base64Str))
                    db.collection("users").document(currentUid).set(fallbackMap, SetOptions.merge()).addOnSuccessListener {
                        setButtonToUploadedState(button)
                    }
                }

        } catch (e: Exception) {
            button.isEnabled = true
            button.text = "Gagal"
        }
    }

    private fun setButtonToUploadedState(button: Button) {
        button.isEnabled = true
        button.text = "✓ Tersimpan"
        button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark)
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
            .setTitle("Edit Profil Seeker")
            .setView(dialogView)
            .setPositiveButton("Simpan") { dialog, _ ->
                val newName = etName.text.toString().trim()
                val newPhone = etPhone.text.toString().trim()
                val newAddress = etAddress.text.toString().trim()
                val oldPassword = etOldPassword.text.toString().trim()
                val newPassword = etNewPassword.text.toString().trim()

                if (newName.isEmpty()) {
                    Toast.makeText(context, "Nama wajib diisi!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (oldPassword.isNotEmpty() || newPassword.isNotEmpty()) {
                    if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                        Toast.makeText(context, "Isi password lama & baru!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                    user.reauthenticate(credential).addOnSuccessListener {
                        user.updatePassword(newPassword).addOnSuccessListener {
                            saveDataToFirestore(newName, newPhone, newAddress, dialog)
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, "Password lama salah!", Toast.LENGTH_SHORT).show()
                    }
                } else {
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
        db.collection("users").document(currentUid).set(updateMap, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Profil Diperbarui!", Toast.LENGTH_SHORT).show()
                loadSeekerData()
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