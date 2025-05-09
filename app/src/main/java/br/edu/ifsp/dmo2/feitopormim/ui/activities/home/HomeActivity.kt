package br.edu.ifsp.dmo2.feitopormim.ui.activities.home

import android.content.Intent
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import br.edu.ifsp.dmo2.feitopormim.ui.adapter.PostAdapter
import br.edu.ifsp.dmo2.feitopormim.ui.activities.myProfile.MyProfileActivity
import br.edu.ifsp.dmo2.feitopormim.util.Base64Converter
import br.edu.ifsp.dmo2.feitopormim.util.LocalizacaoHelper
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
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.VectorDrawable
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

class HomeActivity : AppCompatActivity(), LocalizacaoHelper.Callback {

    private lateinit var binding: ActivityHomeBinding
    private var currentDialogBinding: LayoutDialogAddPostBinding? = null
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var adapter: PostAdapter
    private lateinit var galery: ActivityResultLauncher<PickVisualMediaRequest>
    private var ultimoTimestamp: Timestamp? = null
    private var location = ""
    private var isLoading = false
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

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
            ultimoTimestamp = null
            adapter.limparPosts()
        }

        binding.searchIcon.setOnClickListener{
            location = binding.searchInput.text.toString()
            ultimoTimestamp = null
            adapter.limparPosts()
        }

        binding.listPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                Log.d("LoadFeed", "Total de itens: $totalItemCount, Último item visível: $lastVisibleItem")

                // Carregar mais apenas quando o usuário chegar no último item visível da lista carregada
                if (!isLoading && lastVisibleItem == totalItemCount - 1) {
                    Log.d("LoadFeed", "Carregando mais posts...")
                    isLoading = true
                    loadFeed()
                }
            }
        })

        binding.buttonAddPost.setOnClickListener {

            val dialogBinding = LayoutDialogAddPostBinding.inflate(layoutInflater)
            currentDialogBinding = dialogBinding // atualiza referência

            val dialog = MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setView(dialogBinding.root)
                .show()

            dialogBinding.addImageButton.setOnClickListener {
                galery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            dialogBinding.myCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    dialogBinding.locationPost.setText("Buscando localização...")
                    dialogBinding.confirmButton.isEnabled = false
                    solicitarLocalizacao()
                } else {
                    dialogBinding.locationPost.setText("Sem Localização")
                }
            }

            dialogBinding.confirmButton.setOnClickListener {
                if (firebaseAuth.currentUser != null){

                    val email = firebaseAuth.currentUser!!.email.toString()
                    val textPost = dialogBinding.inputTextPost.text.toString().trim()
                    val locationPost = dialogBinding.locationPost.text.toString().ifBlank { "Sem Localização" }
                    val db = com.google.firebase.Firebase.firestore
                    val drawable = dialogBinding.imagePost.drawable

                    when {
                        textPost.isBlank() -> {
                            Toast.makeText(this, "Preencha o campo Descrição.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        drawable is VectorDrawable || drawable is VectorDrawableCompat -> {
                            Toast.makeText(this, "Nenhuma imagem adicionada ao post.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    }

                    val imagePost = Base64Converter.drawableToString(drawable)

                    if (imagePost.isBlank()) {
                        Toast.makeText(this, "Erro ao converter a imagem.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val dados = hashMapOf(
                        "userPost" to email,
                        "textPost" to textPost,
                        "datePost" to Timestamp.now(),
                        "locationPost" to locationPost,
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

        //val location = "Araraquara, São Paulo"

        var query: Query = if (location.equals("")) {
            db.collection("posts")
                .orderBy("datePost", Query.Direction.DESCENDING)
                .limit(5)
        } else {
            db.collection("posts")
                .whereEqualTo("locationPost", location)
                .orderBy("datePost", Query.Direction.DESCENDING)
                .limit(5)
        }

        if (ultimoTimestamp != null) {
            Log.d("LoadFeed", "Buscando posts após o timestamp: $ultimoTimestamp")
            query = query.startAfter(ultimoTimestamp!!)
        }

        query.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (!document.isEmpty) {
                        Log.d("LoadFeed", "Documentos carregados com sucesso.")
                        // Atualiza o último timestamp
                        ultimoTimestamp = document.documents.last().getTimestamp("datePost")
                        Log.d("LoadFeed", "Último timestamp atualizado: $ultimoTimestamp")

                        val postsTemp = ArrayList<Post>()
                        val postFetchCount = document.documents.size
                        var postsFetched = 0

                        for (doc in document.documents) {
                            val userPost = doc.getString("userPost") ?: continue
                            var usernamePost = ""

                            Log.d("LoadFeed", "Buscando dados para o usuário: $userPost")

                            db.collection("user")
                                .document(userPost)
                                .get()
                                .addOnSuccessListener { userDocument ->
                                    if (userDocument.exists()) {
                                        usernamePost = userDocument.getString("username") ?: ""
                                        Log.d("LoadFeed", "Usuário encontrado: $usernamePost")
                                    }

                                    val imageString = doc.getString("imagePost") ?: ""
                                    val imagePost = Base64Converter.stringToBitmap(imageString)
                                    val textPost = doc.getString("textPost") ?: ""
                                    val locationPost = doc.getString("locationPost") ?: ""

                                    val datePost = doc.getTimestamp("datePost")
                                    val date: Date? = datePost?.toDate()
                                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                    val dataFormatada: String = formatter.format(date)

                                    postsTemp.add(Post(usernamePost, dataFormatada, textPost, locationPost, imagePost))

                                    postsFetched++
                                    if (postsFetched == postFetchCount) {
                                        Log.d("LoadFeed", "Todos os posts foram carregados.")
                                        postsTemp.sortByDescending {
                                            stringToDate(it.getDate())
                                        }

                                        if (::adapter.isInitialized.not()) {
                                            adapter = PostAdapter(postsTemp.toMutableList())
                                            binding.listPosts.layoutManager = LinearLayoutManager(this)
                                            binding.listPosts.adapter = adapter
                                        } else {
                                            adapter.adicionarPosts(postsTemp) // Adiciona à lista existente
                                        }
                                        isLoading = false // Libera para novos carregamentos
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("LoadFeed", "Erro ao buscar usuário: ", exception)
                                    postsFetched++
                                    if (postsFetched == postFetchCount) {
                                        isLoading = false
                                    }
                                }
                        }
                    } else {
                        Log.d("LoadFeed", "Nenhum documento encontrado.")
                        isLoading = false
                    }
                } else {
                    Log.e("LoadFeed", "Erro ao carregar posts", task.exception)
                    isLoading = false
                }
            }
    }


    fun stringToDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
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

    private fun solicitarLocalizacao() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            val localizacaoHelper = LocalizacaoHelper(applicationContext)
            localizacaoHelper.obterLocalizacaoAtual(this)
        }
    }

    override fun onLocalizacaoRecebida(endereco: Address, latitude: Double, longitude: Double) {
        runOnUiThread {
           currentDialogBinding!!.locationPost.text =  endereco.subAdminArea + ", " + endereco.adminArea
            currentDialogBinding!!.confirmButton.isEnabled = true
        }
    }

    override fun onErro(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
        currentDialogBinding!!.locationPost.text = "Sem Localização"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions,
            grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            solicitarLocalizacao()
        } else {
            Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
            currentDialogBinding!!.locationPost.text = "Sem Localização"
        }
    }
}