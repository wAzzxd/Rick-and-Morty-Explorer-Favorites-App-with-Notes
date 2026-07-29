package com.example.rickandmortyproject.data.remote

import com.example.rickandmortyproject.data.remote.dto.CharacterDto
import com.example.rickandmortyproject.data.remote.dto.CharacterResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit'e "API şöyle bir şey, bu adreslere şu parametrelerle istek at" diye
// tarif ettiğimiz bir interface. İçinde gerçek kod yok, sadece sözleşme var.
interface RickAndMortyApi {

    // GET isteği: https://rickandmortyapi.com/api/character?page=1&name=...&status=...
    @GET("character")
    suspend fun getCharacters(
        @Query("page") page: Int,
        @Query("name") name: String? = null,
        @Query("status") status: String? = null
    ): CharacterResponseDto

    // GET isteği: https://rickandmortyapi.com/api/character/{id}
    @GET("character/{id}")
    suspend fun getCharacterById(
        @Path("id") id: Int
    ): CharacterDto
}


//Neden interface, class değil?
//Kotlin'de bir interface, "hangi fonksiyonlar olacak" der ama "o fonksiyonlar nasıl
//çalışacak" demez — yani gövdesi (içi) yoktur, sadece imza vardır. Burada Retrofit'e "böyle bir
//API var, şu adreslere şu şekilde istek at" diye bir sözleşme çiziyoruz; gerçek ağ isteğini atan
//kodu Retrofit kütüphanesi bizim yerimize otomatik üretiyor
//(Retrofit.Builder().build().create(RickAndMortyApi::class.java) dediğimizde).
//Bu yüzden hiç { } içi yok, sadece fonksiyon tanımları var.

//Neden suspend fun?
//Ağ isteği atmak zaman alır (birkaç yüz milisaniye - birkaç saniye). Eğer bu işi normal bir
//fonksiyonla yapsaydık, ana thread'i (UI thread'ini) bloklardı — yani istek bitene kadar
//uygulaman donmuş gibi görünürdü, kullanıcı hiçbir yere dokunamazdı. suspend anahtar
//kelimesi, bu fonksiyonun bir coroutine içinde çağrılması gerektiğini söyler — coroutine, işi
//arka planda (başka bir thread'de) yapıp sonucu beklerken UI'ı bloklamamamızı sağlayan
//Kotlin'in asenkron programlama aracı. İleride ViewModel'de bunu viewModelScope.launch
//{ } içinde çağıracağız.

//@Query("name") name: String? = null neden nullable ve default değerli?
//Arama yapılmadığında name parametresini API'ye hiç göndermek istemiyoruz (sadece
//sayfa numarasıyla tüm karakterleri çekmek istiyoruz). String? yaparak "bu değer
//olmayabilir" dedik, = null yaparak da "eğer çağırırken bu parametreyi hiç vermezsen
//otomatik null olsun" dedik. Retrofit, null olan @Query parametrelerini URL'e hiç
//eklemiyor — otomatik atlıyor. Bu sayede tek bir fonksiyonla hem "tüm karakterleri getir"
//hem "isme göre filtrele" hem "duruma göre filtrele" senaryolarını karşılıyoruz.