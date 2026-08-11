package com.example.rickandmortyproject

import com.example.rickandmortyproject.domain.model.Character
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

// Bu, CharacterRepository interface'ini implement eden, GERÇEK ağa/veritabanına
// hiç gitmeyen, tamamen bizim kontrolümüzde olan SAHTE (fake) bir sınıf.
// Testlerde, ViewModel'e gerçek CharacterRepositoryImpl yerine bunu vereceğiz.
class FakeCharacterRepository(
    // "shouldThrowError" -> testte "bu sefer hata fırlat" ya da "normal
    // veri döndür" davranışını DIŞARIDAN kontrol edebilmemizi sağlayan
    // bir anahtar. Varsayılan değeri false, yani belirtmezsek normal
    // (başarılı) davranır.
    private val shouldThrowError: Boolean = false
) : CharacterRepository {

    override suspend fun getCharacters(
        page: Int,
        name: String?,
        status: String?
    ): List<Character> {
        // Eğer testte "hata fırlat" istendiyse, GERÇEK bir ağ hatasını
        // taklit eden IOException fırlatıyoruz - ViewModel'deki catch
        // bloğu bunu yakalayıp "İnternet bağlantınızı kontrol edin" gibi
        // bir mesaja çevirecek.
        if (shouldThrowError) {
            throw IOException("Sahte internet hatası")
        }

        // Hata istenmediyse, elle yazılmış, SABİT bir karakter listesi
        // döndürüyoruz. page/name/status parametrelerini burada hiç
        // kullanmıyoruz - bu sahte sınıfın amacı gerçek filtreleme
        // yapmak değil, ViewModel'e ÖNGÖRÜLEBİLİR bir cevap vermek.
        return listOf(
            Character(
                id = 1,
                name = "Test Karakter",
                status = "Alive",
                species = "Human",
                gender = "Male",
                imageUrl = "",
                origin = "Earth",
                episodeCount = 5
            )
        )
    }

    // Bu testler için şimdilik kullanılmayacak fonksiyonlar - basitçe
    // "çağrılırsa hiçbir şey yapma" ya da "boş değer döndür" şeklinde
    // dolduruyoruz, TODO() bırakırsak yanlışlıkla çağrılırsa test çöker.
    override suspend fun getCharacterById(id: Int): Character {
        return Character(
            id = id,
            name = "Test Karakter",
            status = "Alive",
            species = "Human",
            gender = "Male",
            imageUrl = "",
            origin = "Earth",
            episodeCount = 5
        )
    }

    override suspend fun addFavorite(character: Character) {
    }

    override suspend fun removeFavorite(character: Character) {
    }

    override fun getFavorites(): Flow<List<Character>> {
        // "flowOf(emptyList())" -> hiç favori olmayan, TEK SEFERLİK bir
        // Flow üretir - CharacterListViewModel'in init{} bloğu bunu
        // izlemeye çalışacağı için, bomboş da olsa GEÇERLİ bir Flow
        // vermemiz gerekiyor, yoksa çökebilir.
        return flowOf(emptyList())
    }

    override fun isFavorite(characterId: Int): Flow<Boolean> {
        return flowOf(false)
    }
}