package br.edu.ifsp.dmo2.feitopormim.ui.activities.signup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.ifsp.dmo2.feitopormim.R
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivitySignUpBinding
import br.edu.ifsp.dmo2.feitopormim.ui.activities.home.HomeActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.login.LoginActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)

        setContentView(binding.root)

        verifyAuthentication()
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
        binding.btnVoltar.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnCadastrar.setOnClickListener{

            /*startActivity(Intent(this, PostActivity::class.java))
            finish()*/

            val email = binding.email.text.toString()
            val password = binding.senha.text.toString()
            val confPassword = binding.confirmSenha.text.toString()


            if(email.isNotBlank() && password.isNotBlank() && confPassword.isNotBlank()){
                if(password == confPassword){
                    firebaseAuth
                        .createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                startActivity(Intent(this, ProfileActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                            }
                        }
                }else{
                    Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_LONG).show()
            }
        }
    }
}