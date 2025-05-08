package br.edu.ifsp.dmo2.feitopormim.data.entity

import android.graphics.Bitmap

class Post (private val username: String, private val date: String, private val descricao: String, private val location: String, private val foto: Bitmap){
    public fun getDescricao() : String{
        return descricao
    }
    public fun getFoto() : Bitmap {
        return foto
    }

    public fun getUsername() : String {
        return username
    }

    public fun getDate(): String{
        return date
    }

    public fun getLocation(): String{
        return location
    }
}