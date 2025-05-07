package br.edu.ifsp.dmo2.feitopormim.ui.activities.home

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import br.edu.ifsp.dmo2.feitopormim.R
import br.edu.ifsp.dmo2.feitopormim.data.entity.Post
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivityHomeBinding
import br.edu.ifsp.dmo2.feitopormim.databinding.LayoutDialogAddPostBinding
import br.edu.ifsp.dmo2.feitopormim.ui.activities.login.LoginActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.main.MainActivity
import br.edu.ifsp.dmo2.feitopormim.ui.activities.post.PostActivity
import br.edu.ifsp.dmo2.feitopormim.ui.adapter.PostAdapter
import br.edu.ifsp.dmo2.feitopormim.ui.activities.myProfile.MyProfileActivity
import br.edu.ifsp.dmo2.feitopormim.util.Base64Converter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var currentDialogBinding: LayoutDialogAddPostBinding? = null
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var posts: ArrayList<Post>
    private lateinit var adapter: PostAdapter
    private lateinit var galery: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        initializeActionBar()
        verifyAuthentication()
        setupGalery()
        configListeners()
        loadFeed()
    }

    private fun initializeActionBar(){
        addMenuProvider(
            object : MenuProvider{
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.main_menu, menu)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    when (item.itemId) {
                        R.id.option_menu_home_page -> {
                            startActivity(Intent(applicationContext,HomeActivity::class.java))
                            finish()
                        }
                        R.id.option_menu_my_profile -> {
                            startActivity(Intent(applicationContext, MyProfileActivity::class.java))
                            finish()
                        }
                        R.id.option_menu_logout -> {
                            firebaseAuth.signOut()
                            startActivity(Intent(applicationContext, MainActivity::class.java))
                            finish()
                        }
                    }
                    return true
                }

            }
        )
    }

    private fun verifyAuthentication() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun configListeners(){
        binding.loadFeedButton.setOnClickListener {
            loadFeed()
        }

        binding.buttonAddPost.setOnClickListener {

            val dialogBinding = LayoutDialogAddPostBinding.inflate(layoutInflater)
            currentDialogBinding = dialogBinding // atualiza referência

            val dialog = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setView(dialogBinding.root)
                .show()

            dialogBinding.addImageButton.setOnClickListener {
                galery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            dialogBinding.confirmButton.setOnClickListener {
                if (firebaseAuth.currentUser != null){
                    val email = firebaseAuth.currentUser!!.email.toString()
                    val textPost = dialogBinding.inputTextPost.text.toString()
                    val imagePost = Base64Converter.drawableToString(dialogBinding.imagePost.drawable)
                    val db = com.google.firebase.Firebase.firestore

                    val dados = hashMapOf(
                        "userPost" to email,
                        "textPost" to textPost,
                        "datePost" to Timestamp.now(),
                        "imagePost" to imagePost
                    )
                    db.collection("posts")
                        .add(dados)
                        .addOnSuccessListener {
                            dialog.dismiss()
                            loadFeed()
                        }
                }
            }

            dialogBinding.cancelButton.setOnClickListener {
                dialog.dismiss()
                currentDialogBinding = null
            }
        }
    }

    private fun loadFeed() {
        val db = Firebase.firestore
        db.collection("posts")
            .orderBy("datePost", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    val postsTemp = ArrayList<Post>() // Lista temporária para armazenar os posts enquanto recupera os nomes

                    val postFetchCount = document.documents.size
                    var postsFetched = 0 // Contador para verificar quando todos os posts foram processados

                    for (document in document.documents) {
                        val userPost = document.data!!["userPost"].toString()
                        var usernamePost = ""

                        Log.d("Username", "Buscando usuário com o email: $userPost")

                        // Consulta assíncrona para buscar o nome do usuário
                        db.collection("user")
                            .document(userPost) // O documento da coleção "user" é o e-mail do usuário
                            .get()
                            .addOnSuccessListener { userDocument ->
                                if (userDocument.exists()) {
                                    usernamePost = userDocument.data!!["username"].toString()
                                    Log.d("Username", "Nome do usuário: $usernamePost")
                                } else {
                                    Log.d("Username", "Usuário não encontrado")
                                }

                                // Agora, adicionar o post à lista após obter o nome do usuário
                                val imageString = document.data!!["imagePost"].toString()
                                val imagePost = Base64Converter.stringToBitmap(imageString)
                                val textPost = document.data!!["textPost"].toString()

                                val datePost = document.getTimestamp("datePost")
                                val date: Date? = datePost?.toDate()
                                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val dataFormatada: String = formatter.format(date)

                                postsTemp.add(Post(usernamePost, dataFormatada, textPost, imagePost))

                                postsFetched++
                                if (postsFetched == postFetchCount) {
                                    // Quando todos os posts forem processados, defina o adaptador
                                    adapter = PostAdapter(postsTemp.toTypedArray())
                                    binding.listPosts.layoutManager = LinearLayoutManager(this)
                                    binding.listPosts.adapter = adapter
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d("Username", "Erro ao buscar usuário: ", exception)
                            }
                    }
                }
            }
    }


    private fun setupGalery(){
        galery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                currentDialogBinding?.imagePost?.setImageURI(uri)
            } else {
                Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
            }
        }
    }
}