package br.edu.ifsp.dmo2.feitopormim.ui.activities.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.ifsp.dmo2.feitopormim.R
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivityProfileBinding
import br.edu.ifsp.dmo2.feitopormim.ui.activities.home.HomeActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.login.LoginActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.main.MainActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.register.RegisterActivity
import br.edu.ifsp.dmo2.feitopormim.util.Base64Converter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var galery: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)

        setContentView(binding.root)

        verifyAuthentication()
        setupGalery()
        configListeners()
    }

    private fun verifyAuthentication() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun configListeners(){
        binding.arrowBack.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.addImageButton.setOnClickListener {
            galery.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.registerButton.setOnClickListener{
            if (firebaseAuth.currentUser != null){
                val email = firebaseAuth.currentUser!!.email.toString()
                val username = binding.inputUsername.text.toString()
                val nomeCompleto = binding.inputFullname.text.toString()

                val fotoPerfilString = Base64Converter.drawableToString(binding.imageProfile.drawable)
                val db = Firebase.firestore
                val dados = hashMapOf(
                    "fullname" to nomeCompleto,
                    "username" to username,
                    "picture" to fotoPerfilString
                )
                db.collection("user").document(email)
                    .set(dados)
                    .addOnSuccessListener {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
            }
        }
    }

    private fun setupGalery(){
        galery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                uri ->
            if (uri != null) {
                binding.imageProfile.setImageURI(uri)
            } else {
                Toast.makeText(this, getString(R.string.no_photo_selected), Toast.LENGTH_LONG).show()
            }
        }
    }
}