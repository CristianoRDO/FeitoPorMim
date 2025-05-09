package br.edu.ifsp.dmo2.feitopormim.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.edu.ifsp.dmo2.feitopormim.R
import br.edu.ifsp.dmo2.feitopormim.data.entity.Post
import br.edu.ifsp.dmo2.feitopormim.databinding.PostItemBinding

class PostAdapter(private val posts: MutableList<Post>) :
    RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: PostItemBinding = PostItemBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = posts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]

        with(holder.binding) {
            txtDescricao.text = post.getDescricao()
            imgPost.setImageBitmap(post.getFoto())
            usernamePost.text = post.getUsername()
            datetimePost.text = post.getDate()
            locationTextPost.text = post.getLocation()
        }
    }

    fun addPosts(novosPosts: List<Post>) {
        val startIndex = posts.size
        posts.addAll(novosPosts)
        Log.d("PostAdapter", "Adicionando ${novosPosts.size} novos posts a partir do índice $startIndex.")
        notifyItemRangeInserted(startIndex, novosPosts.size)
    }

    fun clearPosts() {
        posts.clear()
        notifyDataSetChanged()
    }
}