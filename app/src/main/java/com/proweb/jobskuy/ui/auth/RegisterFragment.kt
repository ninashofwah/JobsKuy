package com.proweb.jobskuy.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.User
import com.proweb.jobskuy.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Variabel penampung koordinat lokasi GPS
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        val selectedRole = arguments?.getString("role") ?: "Pencari Kerja"
        binding.tvRegisterTitle.text = "Daftar Sebagai $selectedRole"

        // Otomatis meminta titik koordinat GPS perangkat saat form dibuka
        getDeviceCurrentLocation()

        binding.btnSubmitRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val confirmEmail = binding.etConfirmEmail.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // 1. Validasi Kolom Kosong
            if (fullName.isEmpty() || email.isEmpty() || confirmEmail.isEmpty() ||
                phoneNumber.isEmpty() || address.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(context, "Harap lengkapi seluruh formulir!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validasi Kesamaan Email Dan Konfirmasinya
            if (email != confirmEmail) {
                Toast.makeText(context, "Email dan Konfirmasi Email tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Validasi Kesamaan Password Dan Konfirmasinya
            if (password != confirmPassword) {
                Toast.makeText(context, "Kata Sandi dan Konfirmasi tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4. Proses Pembuatan Akun Baru ke Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { res ->
                    val uid = res.user?.uid ?: ""

                    // Membuat objek data profil lengkap beserta koordinat GPS lokasi
                    val newUser = User(
                        uid = uid,
                        fullName = fullName,
                        email = email,
                        phoneNumber = phoneNumber,
                        address = address,
                        role = selectedRole,
                        latitude = latitude,
                        longitude = longitude
                    )

                    // Menyimpan berkas lengkap ke Cloud Firestore Database secara real-time
                    db.collection("users").document(uid).set(newUser)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Akun Berhasil Terdaftar di Database!", Toast.LENGTH_LONG).show()
                            findNavController().popBackStack() // Kembali ke halaman login
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Gagal simpan profil: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal membuat akun: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getDeviceCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                }
            }
        } else {
            // Minta izin sensor GPS jika belum diaktifkan oleh pengguna
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 102)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}