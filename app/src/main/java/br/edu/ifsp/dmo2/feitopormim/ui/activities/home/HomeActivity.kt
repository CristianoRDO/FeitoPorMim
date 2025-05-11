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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

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
    private var primeiroCarregamento = true

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
        binding.reloadButton.setOnClickListener {
            clearFeed()
        }

        binding.searchIcon.setOnClickListener{
            location = binding.searchInput.text.toString()
            clearFeed()
        }

        binding.listPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                // Carregar mais apenas quando o usuário chegar no último item visível da lista carregada
                if (!isLoading && lastVisibleItem == totalItemCount - 1) {
                    isLoading = true
                    Log.d("Feed", "Estou na funcao scrooll: $ultimoTimestamp")
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
                    dialogBinding.locationPost.setText(getString(R.string.searching_location))
                    dialogBinding.confirmButton.isEnabled = false
                    solicitarLocalizacao()
                } else {
                    dialogBinding.locationPost.setText(getString(R.string.no_location))
                }
            }

            dialogBinding.cancelButton.setOnClickListener {
                dialog.dismiss()
                currentDialogBinding = null
            }

            dialogBinding.confirmButton.setOnClickListener {
                if (firebaseAuth.currentUser != null){

                    val email = firebaseAuth.currentUser!!.email.toString()
                    val textPost = dialogBinding.inputTextPost.text.toString().trim()
                    val locationPost = dialogBinding.locationPost.text.toString().ifBlank { getString(R.string.no_location) }
                    val db = com.google.firebase.Firebase.firestore
                    val drawable = dialogBinding.imagePost.drawable

                    when {
                        textPost.isBlank() -> {
                            Toast.makeText(this, getString(R.string.fill_description_field), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        drawable is VectorDrawable || drawable is VectorDrawableCompat -> {
                            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    }

                    val imagePost = Base64Converter.drawableToString(drawable)

                    if (imagePost.isBlank()) {
                        Toast.makeText(this, getString(R.string.error_converting_image), Toast.LENGTH_SHORT).show()
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
                            clearFeed()
                        }
                }
            }
        }
    }

    private fun loadFeed() {
        val db = Firebase.firestore
        var query = createQueryBase(db)

        // Verifica se existe um timestamp do último post carregado para usar como referência (paginação)
        if (ultimoTimestamp != null) {
            Log.d("Feed", "Último timestamp encontrado: $ultimoTimestamp")
            query = query.startAfter(ultimoTimestamp!!)
        } else {
            Log.d("Feed", "Nenhum timestamp anterior encontrado, carregando do início")
        }

        // Executa a consulta ao Firestore
        query.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (!document.isEmpty) {
                        Log.d("Feed", "Posts carregados: ${document.size()}")

                        // Atualiza o timestamp com o do último documento para controle de paginação
                        ultimoTimestamp = document.documents.last().getTimestamp("datePost")
                        Log.d("Feed", "Novo último timestamp: $ultimoTimestamp")

                        // Processa os documentos carregados
                        processPosts(db, document.documents)
                    } else {
                            if(primeiroCarregamento){
                                adapter = PostAdapter(mutableListOf())
                                binding.listPosts.layoutManager = LinearLayoutManager(this)
                                binding.listPosts.adapter = adapter
                                primeiroCarregamento = false
                            }
                            Log.d("Feed", "Nenhum post novo encontrado")
                            isLoading = false
                    }
                } else {
                    Log.e("Feed", "Erro ao carregar feed: ${task.exception}")
                    isLoading = false
                }
            }
    }


    private fun createQueryBase(db: FirebaseFirestore): Query {
        val base = db.collection("posts")
            .orderBy("datePost", Query.Direction.DESCENDING)
            .limit(5)

        return if (location.isNullOrBlank()) base
        else base.whereEqualTo("locationPost", location)
    }

    private fun processPosts(db: FirebaseFirestore, documents: List<DocumentSnapshot>) {
        val postsTemp = ArrayList<Post>()
        val totalPosts = documents.size
        var postsProcessados = 0

        Log.d("processPosts", "Iniciando o processamento de ${totalPosts} posts.")

        for (doc in documents) {
            val userEmail = doc.getString("userPost") ?: continue // Caso não encontre o usuário, passa para a próxima iteração.

            Log.d("processPosts", "Processando post do usuário: $userEmail")

            // Processa o post
            db.collection("user").document(userEmail)
                .get()
                .addOnSuccessListener { userDoc ->
                    val username = userDoc.getString("username") ?: ""
                    Log.d("processPosts", "Usuário encontrado: $username")

                    postsTemp.add(createPost(doc, username))
                    postsProcessados++

                    Log.d("processPosts", "Post processado. Posts processados: $postsProcessados/$totalPosts")

                    if (postsProcessados == totalPosts) {
                        Log.d("processPosts", "Todos os posts foram processados. Chamando finishLoading.")
                        finishLoading(postsTemp) // Finaliza o carregamento após processar todos os posts
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("processPosts", "Falha ao buscar dados do usuário. Usando nome de usuário vazio.")
                    postsTemp.add(createPost(doc, ""))
                    postsProcessados++

                    Log.d("processPosts", "Post processado com falha. Posts processados: $postsProcessados/$totalPosts")

                    if (postsProcessados == totalPosts) {
                        Log.d("processPosts", "Todos os posts foram processados (com falha). Chamando finishLoading.")
                        finishLoading(postsTemp) // Finaliza o carregamento após processar todos os posts
                    }
                }
        }
    }

    private fun createPost(doc: DocumentSnapshot, username: String): Post {
        val imageString = doc.getString("imagePost") ?: ""
        val imagePost = Base64Converter.stringToBitmap(imageString)
        val textPost = doc.getString("textPost") ?: ""
        val locationPost = doc.getString("locationPost") ?: ""
        val date = doc.getTimestamp("datePost")?.toDate()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = formatter.format(date)

        return Post(username, formattedDate, textPost, locationPost, imagePost)
    }

    private fun finishLoading(posts: List<Post>) {
        val sortedPosts = posts.sortedByDescending { stringToDate(it.getDate()) }

        if (!::adapter.isInitialized && primeiroCarregamento) {
            adapter = PostAdapter(sortedPosts.toMutableList())
            binding.listPosts.layoutManager = LinearLayoutManager(this)
            binding.listPosts.adapter = adapter
            primeiroCarregamento = false
            Log.d("processPosts", "Adapter Inicialiazado)")
        } else {
            adapter.addPosts(sortedPosts)
        }
        isLoading = false
    }

    private fun clearFeed(){
        isLoading = true
        ultimoTimestamp = null
        adapter.clearPosts()
        loadFeed()
    }

    private fun stringToDate(dateString: String): Date? {
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
        currentDialogBinding!!.locationPost.text = getString(R.string.no_location)
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
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
            currentDialogBinding!!.locationPost.text = getString(R.string.no_location)
        }
    }
}