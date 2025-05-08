package br.edu.ifsp.dmo2.feitopormim.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.ifsp.dmo2.feitopormim.R
import br.edu.ifsp.dmo2.feitopormim.data.entity.Post

class PostAdapter(private val posts: Array<Post>) :
    RecyclerView.Adapter<PostAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    {
        val imgPost : ImageView = view.findViewById(R.id.img_post)
        val txtPost : TextView = view.findViewById(R.id.txt_descricao)
        val usernamePost : TextView = view.findViewById(R.id.username_post)
        val dateTimePost : TextView = view.findViewById(R.id.datetime_post)
        val locationPost : TextView = view.findViewById(R.id.location_text_post)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return ViewHolder(view)
    }
    override fun getItemCount(): Int {
        return posts.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtPost.text = posts[position].getDescricao()
        holder.imgPost.setImageBitmap(posts[position].getFoto())
        holder.usernamePost.text = posts[position].getUsername()
        holder.dateTimePost.text = posts[position].getDate()
        holder.locationPost.text = posts[position].getLocation()
    }
}