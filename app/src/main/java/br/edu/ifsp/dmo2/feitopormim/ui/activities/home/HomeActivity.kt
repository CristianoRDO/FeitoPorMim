package br.edu.ifsp.dmo2.feitopormim.ui.activities.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.ifsp.dmo2.feitopormim.R
import br.edu.ifsp.dmo2.feitopormim.data.entity.Post
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivityHomeBinding
import br.edu.ifsp.dmo2.feitopormim.databinding.LayoutDialogAddPostBinding
import br.edu.ifsp.dmo2.feitopormim.ui.activities.login.LoginActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.main.MainActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.post.PostActivity
import br.edu.ifsp.dmo2.feitopormim.ui.adapter.PostAdapter
import br.edu.ifsp.dmo2.feitopormim.util.Base64Converter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var currentDialogBinding: LayoutDialogAddPostBinding? = null
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var posts: ArrayList<Post>
    private lateinit var adapter: PostAdapter
    private lateinit var galeria: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        galeria = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                currentDialogBinding?.imagePost?.setImageURI(uri)
            } else {
                Toast.makeText(this, "Nenhuma imagem selecionada", Toast.LENGTH_SHORT).show()
            }
        }

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
                        binding.listPosts.layoutManager = LinearLayoutManager(this)
                        binding.listPosts.adapter = adapter
                    }
                }
        }

        /*binding.buttonAddPost.setOnClickListener{
            startActivity(Intent(this, PostActivity::class.java))
            finish()
        }*/

        binding.buttonAddPost.setOnClickListener {

            val dialogBinding = LayoutDialogAddPostBinding.inflate(layoutInflater)
            currentDialogBinding = dialogBinding // atualiza referência

            val dialog = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setView(dialogBinding.root)
                .show()

            dialogBinding.addImageButton.setOnClickListener {
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            dialogBinding.confirmButton.setOnClickListener {
                
            }

            dialogBinding.cancelButton.setOnClickListener {
                dialog.dismiss()
                currentDialogBinding = null
            }
        }
    }
}