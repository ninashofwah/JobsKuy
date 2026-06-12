package com.proweb.jobskuy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
// HAPUS jika ada import manual yang salah seperti: import com.proweb.jobskuy.databinding.ActivityMain
import com.proweb.jobskuy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Deklarasi variabel binding dengan benar
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PASTIKAN kodenya menggunakan ActivityMainBinding, bukan ActivityMain
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Hubungkan ke root view binding
        setContentView(binding.root)
    }
}