package com.proweb.jobskuy.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.SessionManager
import com.proweb.jobskuy.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth


class LoginFragment : Fragment(R.layout.fragment_login) {
    private lateinit var binding: FragmentLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        val selectedRole = arguments?.getString("role") ?: "Pencari Kerja"
        binding.tvLoginTitle.text = "Login Sebagai $selectedRole"

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Lengkapi kolom!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener { res ->
                val uid = res.user?.uid ?: ""

                // MENYIMPAN DATA KE SHAREDPREFERENCES (DATA LOKAL)
                val sessionManager = SessionManager(requireContext())
                sessionManager.createLoginSession(uid, selectedRole)

                if (selectedRole == "Pencari Kerja") {
                    findNavController().navigate(R.id.action_login_to_homeSeeker)
                } else {
                    findNavController().navigate(R.id.action_login_to_homeRecruiter)
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Login Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvToRegister.setOnClickListener {
            val bundle = Bundle().apply { putString("role", selectedRole) }
            findNavController().navigate(R.id.action_login_to_register, bundle)
        }
    }
}