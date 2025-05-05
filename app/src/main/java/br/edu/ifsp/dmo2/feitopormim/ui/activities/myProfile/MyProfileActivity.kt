package br.edu.ifsp.dmo2.feitopormim.ui.activities.myProfile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.edu.ifsp.dmo2.feitopormim.R
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivityMyProfileBinding
import br.edu.ifsp.dmo2.feitopormim.ui.activities.home.HomeActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.login.LoginActivity
import br.edu.ifsp.dmo2.feitopormim.util.Base64Converter
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProfileBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var galery: ActivityResultLauncher<PickVisualMediaRequest>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verifyAuthentication()
        setupGalery()
        loadDataUser()
        configListeners()
    }

    private fun verifyAuthentication() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadDataUser() {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val db = Firebase.firestore
            db.collection("user").document(user!!.email.toString()).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        if (document != null && document.exists()) {
                            // Recupera os dados do documento
                            val username = document.data!!["username"].toString()
                            val imageString = document.data!!["picture"].toString()
                            val bitmap = Base64Converter.stringToBitmap(imageString)

                            binding.imageProfile.setImageBitmap(bitmap)
                            binding.inputUsername.setText(username)
                        } else {
                            Toast.makeText(this,
                                getString(R.string.user_not_found), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this,
                            getString(R.string.error_loading_user_data), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun configListeners(){
        binding.arrowBack.setOnClickListener{
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        binding.addImageButton.setOnClickListener {
            galery.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.myCheckBox.setOnCheckedChangeListener{ _, isChecked ->
            if (isChecked) {
                binding.textInputContainer2.visibility = View.VISIBLE
                binding.textInputContainer3.visibility = View.VISIBLE
            } else {
                binding.textInputContainer2.visibility = View.GONE
                binding.textInputContainer3.visibility = View.GONE
            }
        }

        binding.registerButton.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null && user.email != null) {
                val db = Firebase.firestore

                val image = binding.imageProfile.drawable
                val imageBase64 = Base64Converter.drawableToString(image)
                val username = binding.inputUsername.text.toString()

                val updateData: MutableMap<String, Any> = hashMapOf(
                    "username" to username,
                    "picture" to imageBase64
                )

                // Se o checkbox estiver marcado, tentar trocar a senha primeiro
                if (binding.myCheckBox.isChecked) {
                    val currentPassword = binding.inputPassword.text.toString()
                    val newPassword = binding.inputPassword.text.toString()

                    if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                        Toast.makeText(this,
                            getString(R.string.fill_all_password_fields), Toast.LENGTH_SHORT).show()
                    } else{
                        // Autenticar novamente para poder trocar a senha
                        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Senha correta", Toast.LENGTH_SHORT).show()
                                // Senha atual está correta, trocar senha e atualizar dados
                                /*user.updatePassword(novaSenha)
                                    .addOnSuccessListener {
                                        Log.d("Auth", "Senha alterada com sucesso.")

                                        // Atualizar os dados do Firestore
                                        val userDocRef = db.collection("user").document(user.email!!)
                                        userDocRef.update(updateData)
                                            .addOnSuccessListener {
                                                Log.d("Firestore", "Dados atualizados com sucesso.")
                                            }
                                            .addOnFailureListener { exception ->
                                                Log.e("Firestore", "Erro ao atualizar dados: ${exception.message}")
                                            }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("Auth", "Erro ao alterar senha: ${exception.message}")
                                    }*/
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, getString(R.string.incorrect_current_password), Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // Apenas atualizar os dados (sem alterar a senha)
                    val userDocRef = db.collection("user").document(user.email!!)
                    userDocRef.update(updateData)
                }
            }
        }
    }

    private fun setupGalery(){
        galery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                binding.imageProfile.setImageURI(uri)
            } else {
                Toast.makeText(this, getString(R.string.no_photo_selected), Toast.LENGTH_LONG).show()
            }
        }
    }
}