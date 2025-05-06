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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
                
            }

            dialogBinding.cancelButton.setOnClickListener {
                dialog.dismiss()
                currentDialogBinding = null
            }
        }
    }

    private fun loadFeed(){
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