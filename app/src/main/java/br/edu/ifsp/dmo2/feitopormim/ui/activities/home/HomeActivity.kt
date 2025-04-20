package br.edu.ifsp.dmo2.feitopormim.ui.activities.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.ifsp.dmo2.feitopormim.data.entity.Post
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivityHomeBinding
import br.edu.ifsp.dmo2.feitopormim.ui.activities.login.LoginActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.main.MainActivity
import br.edu.ifsp.dmo2.feitopormim.ui.adapter.PostAdapter
import br.edu.ifsp.dmo2.feitopormim.util.Base64Converter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var posts: ArrayList<Post>
    private lateinit var adapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val firebaseAuth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val email = firebaseAuth.currentUser!!.email.toString()
        /*db.collection("user").document(email).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val document = task.result
                    val imageString = document.data!!["picture"].toString()
                    val bitmap = Base64Converter.stringToBitmap(imageString)
                    binding.imgProfile.setImageBitmap(bitmap)
                    binding.textviewUsername.text = document.data!!["username"].toString()
                    binding.textviewFullname.text =
                        document.data!!["fullname"].toString()
                }
            }*/

        configListeners()
    }

    private fun configListeners(){
        binding.logoutButton.setOnClickListener{
            firebaseAuth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.loadFeedButton.setOnClickListener {
            val db = Firebase.firestore
            db.collection("posts").get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val document = task.result
                        posts = ArrayList()
                        for (document in document.documents) {
                            val imageString = document.data!!["image"].toString()
                            val bitmap = Base64Converter.stringToBitmap(imageString)
                            val descricao = document.data!!["text"].toString()
                            posts.add(Post(descricao, bitmap))
                        }
                        adapter = PostAdapter(posts.toTypedArray())
                        binding.listaPosts.layoutManager = LinearLayoutManager(this)
                        binding.listaPosts.adapter = adapter
                    }
                }
        }
    }
}