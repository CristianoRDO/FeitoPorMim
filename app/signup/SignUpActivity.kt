package br.edu.ifsp.dmo2.feitopormim.ui.activities.signup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)

        setContentView(binding.root)

        configListeners()
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
            val senha = binding.senha.text.toString()
            val confSenha = binding.confirmSenha.text.toString()


            if(email.isNotBlank() && senha.isNotBlank() && confSenha.isNotBlank()){
                if(senha == confSenha){
                    firebaseAuth
                        .createUserWithEmailAndPassword(email, senha)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                startActivity(Intent(this, ProfileActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                            }
                        }
                }else{
                    Toast.makeText(this, "Senhas diferentes", Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_LONG).show()
            }
        }
    }
}