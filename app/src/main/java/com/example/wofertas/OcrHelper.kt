package com.example.wofertas

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ProdutoExtraido(
    val nome: String,
    val preco: Double?,
    val textoOriginal: String
)

object OcrHelper {

    private val PRECO_REGEX = Regex("""(?i)(?:R\$\s*)?(\d{1,4}(?:[.,\s]\d{3})*[\s,.]+\d{2})(?!\d)""")

    /**
     * Extrai produtos e preços de um Bitmap.
     */
    suspend fun extrairDeBitmap(bitmap: Bitmap): List<ProdutoExtraido> =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val encontrados = mutableListOf<ProdutoExtraido>()
                    val blocos = visionText.textBlocks

                    for (i in blocos.indices) {
                        val textoBloco = blocos[i].text.replace("\n", " ").trim()
                        val match = PRECO_REGEX.find(textoBloco)

                        if (match != null) {
                            val preco = converterPreco(match.groupValues[1])
                            var nome = textoBloco.substring(0, match.range.first).trim()
                            
                            // Se nome estiver vazio, tenta o bloco de cima
                            if (nome.length < 3 && i > 0) {
                                nome = (blocos[i-1].text.replace("\n", " ") + " " + nome).trim()
                            }
                            
                            nome = nome.replace(Regex("(?i)R\\$\\s*|\\b(UN|KG|L|ML|G|GR|1KG|500G|250G)\\b.*$"), "").trim()

                            if (nome.length >= 2) {
                                encontrados.add(ProdutoExtraido(nome.uppercase(), preco, textoBloco))
                            }
                        } else if (textoBloco.length >= 3) {
                            // Captura nomes avulsos mesmo sem preço detectado no bloco
                            encontrados.add(ProdutoExtraido(textoBloco.uppercase(), null, textoBloco))
                        }
                    }
                    cont.resume(encontrados.distinctBy { normalizar(it.nome) })
                }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

    private fun converterPreco(v: String): Double? {
        val n = v.replace(Regex("[^0-9]"), "")
        return if (n.length >= 3) {
            val r = n.substring(0, n.length - 2)
            val c = n.substring(n.length - 2)
            "$r.$c".toDoubleOrNull()
        } else null
    }

    /**
     * Compara o item da lista com o que o OCR leu.
     */
    fun corresponde(itemLista: String, nomeOcr: String): Boolean {
        val busca = normalizar(itemLista)
        val alvo  = normalizar(nomeOcr)
        if (busca.isEmpty() || alvo.isEmpty()) return false
        
        // Match se contiver a palavra (ex: "cafe" em "cafebridge")
        if (alvo.contains(busca) || busca.contains(alvo)) return true
        
        // Match por palavras parciais
        val palavrasBusca = busca.split(" ").filter { it.length >= 3 }
        return palavrasBusca.any { p -> alvo.contains(p) }
    }

    private fun normalizar(s: String): String =
        s.lowercase()
            .replace(Regex("[àáâãä]"), "a")
            .replace(Regex("[èéêë]"), "e")
            .replace(Regex("[ìíîï]"), "i")
            .replace(Regex("[òóôõö]"), "o")
            .replace(Regex("[ùúûü]"), "u")
            .replace(Regex("[ç]"), "c")
            .replace(Regex("[^a-z0-9 ]"), "") // Remove símbolos mas mantém espaços
            .trim()
}
