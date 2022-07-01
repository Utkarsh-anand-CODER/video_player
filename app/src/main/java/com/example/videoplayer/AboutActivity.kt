package com.example.videoplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.videoplayer.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(MainActivity.themesList[MainActivity.themeIndex])
        val binding=ActivityAboutBinding.inflate(layoutInflater)

        setContentView(binding.root)
        supportActionBar?.title="About"
        binding.aboutText.text="Devoloped By:Utkarsh anand .\n\n"

    }
}