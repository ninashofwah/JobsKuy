package com.proweb.jobskuy.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentRegisterEmailBinding
import kotlin.random.Random

class RegisterEmailFragment : Fragment(R.layout.fragment_register_email) {

    private var _binding: FragmentRegisterEmailBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private var generatedOtp: String = ""
    private var userEmail: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterEmailBinding.bind(view)

        // Mengambil operan role dari fragment pemilihan peran sebelumnya
        val selectedRole = arguments?.getString("role") ?: "Pencari Kerja"

        binding.btnSendOtp.setOnClickListener {
            userEmail = binding.etRegisterEmail.text.toString().trim()

            if (userEmail.isEmpty()) {
                Toast.makeText(context, "Masukkan email terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Memeriksa status pendaftaran email ke Firebase server
            auth.fetchSignInMethodsForEmail(userEmail).addOnSuccessListener { result ->
                val isNewUser = result.signInMethods?.isEmpty() ?: true

                if (!isNewUser) {
                    Toast.makeText(context, "Email sudah terdaftar! Gunakan email lain.", Toast.LENGTH_SHORT).show()
                } else {
                    // Generate kode OTP 6-digit secara lokal untuk efisiensi pengujian
                    generatedOtp = String.format("%06d", Random.nextInt(999999))

                    // Menampilkan OTP via Toast agar kamu bisa menyalinnya langsung saat demo/pengujian
                    Toast.makeText(context, "KODE OTP ANDA: $generatedOtp", Toast.LENGTH_LONG).show()

                    // Memunculkan kontainer input verifikasi OTP yang sebelumnya tersembunyi
                    binding.layoutOtpVerification.visibility = View.VISIBLE
                    binding.btnSendOtp.isEnabled = false
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Gagal memproses: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val inputOtp = binding.etOtpCode.text.toString().trim()

            if (inputOtp == generatedOtp) {
                Toast.makeText(context, "Email Sukses Terverifikasi!", Toast.LENGTH_SHORT).show()

                // Membungkus data untuk dikirim ke halaman input identitas diri
                val bundle = Bundle().apply {
                    putString("email", userEmail)
                    putString("role", selectedRole)
                }

                // Pemicu navigasi sesuai dengan ID action di nav_graph.xml
                findNavController().navigate(R.id.action_registerEmail_to_registerProfile, bundle)
            } else {
                Toast.makeText(context, "Kode OTP salah, periksa kembali!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}