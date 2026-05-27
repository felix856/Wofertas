package com.example.wofertas

/**
 * Utilitário para transferir dados grandes entre telas sem estourar o limite de 1MB do Android Intent.
 */
object DataHolder {
    var bigString: String? = null
    
    fun clean() {
        bigString = null
    }
}
