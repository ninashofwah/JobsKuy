package com.proweb.jobskuy.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.SessionManager
import com.proweb.jobskuy.data.User
import com.proweb.jobskuy.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Inisialisasi SessionManager untuk menyimpan status login otomatis
    private lateinit var sessionManager: SessionManager

    // Nilai koordinat default sebelum satelit mengunci lokasi emulator
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        // Menginisialisasi objek SessionManager lokal
        sessionManager = SessionManager(requireContext())

        // Menangkap data email dan role yang dioper aman dari Langkah 1 (OTP Halaman Sebelah)
        val verifiedEmail = arguments?.getString("email") ?: ""
        val selectedRole = arguments?.getString("role") ?: "Pencari Kerja"

        binding.tvRegisterTitle.text = "Daftar Profil $selectedRole"

        // Langsung pemicu sistem pelacak GPS aktif begitu halaman formulir terbuka
        initiateGpsTracking()

        binding.btnSubmitRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // 1. Validasi tidak boleh ada kolom identitas yang kosong
            if (fullName.isEmpty() || phoneNumber.isEmpty() || address.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(context, "Semua kolom wajib diisi lengkap!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validasi kecocokan input password
            if (password != confirmPassword) {
                Toast.makeText(context, "Konfirmasi Kata Sandi tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Daftarkan kredensial akun ke dalam server Firebase Authentication
            auth.createUserWithEmailAndPassword(verifiedEmail, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""

                    // Membungkus seluruh berkas data user baru ke dalam objek model terstruktur
                    val newUser = User(
                        uid = uid,
                        fullName = fullName,
                        email = verifiedEmail,
                        phoneNumber = phoneNumber,
                        address = address,
                        role = selectedRole,
                        latitude = latitude,
                        longitude = longitude
                    )

                    // 4. Masukkan objek data lengkap ke dalam koleksi dokumen Cloud Firestore
                    db.collection("users").document(uid).set(newUser)
                        .addOnSuccessListener {
                            // 1. Beri notifikasi sukses yang jelas ke user
                            Toast.makeText(context, "Registrasi Sukses! Silakan masuk menggunakan akun baru Anda.", Toast.LENGTH_LONG).show()

                            // 2. SOLUSI AMAN: Mundur otomatis ke halaman Login utama tanpa back manual
                            // Fungsi popBackStack ini akan menghancurkan form registrasi dan langsung membuka LoginFragment
                            findNavController().popBackStack(R.id.loginFragment, false)
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal membuat otentikasi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun initiateGpsTracking() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Memeriksa izin perangkat keras lokasi Android
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Mencoba membaca cache koordinat terakhir perangkat emulator
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    binding.etAddress.setText("Koordinat GPS Aktif: ($latitude, $longitude)")
                } else {
                    // JIKA CACHE LOKASI KOSONG/NULL, PAKSA EMULATOR MEMBUAT REQUEST UPDATE BARU SECARA REAL-TIME
                    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                        .setMaxUpdates(1) // Hanya meminta 1x tangkapan koordinat instan
                        .build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            val newLocation = locationResult.lastLocation
                            if (newLocation != null) {
                                latitude = newLocation.latitude
                                longitude = newLocation.longitude
                                binding.etAddress.setText("Koordinat GPS Aktif: ($latitude, $longitude)")
                            } else {
                                binding.etAddress.setText("Gagal mengunci GPS. Buka Google Maps sebentar!")
                            }
                        }
                    }

                    // Menembakkan permintaan pelacakan aktif ke hardware satelit tiruan emulator
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper())
                }
            }
        } else {
            // Meminta izin pop-up persetujuan lokasi secara runtime jika user belum mengizinkan
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 102)
        }
    }

    // Mengulangi permintaan GPS otomatis jika user baru saja menekan tombol "Allow" pada pop-up izin
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 102 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initiateGpsTracking()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
