package br.edu.ifsp.dmo2.feitopormim.ui.activities.post

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.ifsp.dmo2.feitopormim.databinding.ActivityPostBinding
import com.android.volley.Request
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class PostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val queue = Volley.newRequestQueue(this)
        val url = "http://10.105.68.97:8080/posts/1"
        val jsonRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                binding.txtDescricao.text = response.getString("descricao")
                val queue = Volley.newRequestQueue(this)
                val urlImage = "http://10.105.68.97:8080/images/" +
                        response.getString("foto")
                val imageRequest = ImageRequest(urlImage,
                    { response ->
                        binding.imgPost.setImageBitmap(response)
                    },
                    0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    { error ->
                        error.printStackTrace()
                    })
                queue.add(imageRequest)
            },
            { error ->
                error.printStackTrace()
            }
        )
        queue.add(jsonRequest)

    }
}