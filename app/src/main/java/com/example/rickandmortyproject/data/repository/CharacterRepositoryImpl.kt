package com.example.rickandmortyproject.data.repository

import com.example.rickandmortyproject.data.remote.RickAndMortyApi
import com.example.rickandmortyproject.data.remote.dto.CharacterDto
import com.example.rickandmortyproject.domain.model.Character
import com.example.rickandmortyproject.domain.repository.CharacterRepository

// ": CharacterRepository" -> bu sınıf, o interface'i IMPLEMENT ediyor.
// Yani "sözleşmede söz verdiğim tüm fonksiyonları burada gerçekten dolduruyorum" demek.
//
// "api: RickAndMortyApi" constructor'dan (kurucu fonksiyondan) alınıyor.
// Buna CONSTRUCTOR INJECTION denir: bu sınıf, ihtiyaç duyduğu şeyi (api'yi) kendisi
// oluşturmuyor, dışarıdan HAZIR olarak alıyor. Az sonra bunu Koin otomatik sağlayacak.
class CharacterRepositoryImpl(
    private val api: RickAndMortyApi
) : CharacterRepository {

    // "override" -> interface'teki fonksiyonun gerçek/somut halini yazıyoruz.
    override suspend fun getCharacters(
        page: Int,
        name: String?,
        status: String?
    ): List<Character> {
        // 1. Retrofit üzerinden gerçek ağ isteğini atıyoruz, ham DTO cevabı geliyor.
        val response = api.getCharacters(page, name, status)

        // 2. Gelen DTO listesindeki HER BİR elemanı, aşağıdaki toDomainModel()
        //    fonksiyonuyla domain Character'a çeviriyoruz. "map", Kotlin'de
        //    bir listenin her elemanına aynı dönüşümü uygulayıp yeni bir liste döndürür.
        return response.results.map { dto -> dto.toDomainModel() }
    }

    override suspend fun getCharacterById(id: Int): Character {
        val dto = api.getCharacterById(id)
        return dto.toDomainModel()
    }
}

// EXTENSION FUNCTION: Kotlin'e özgü bir özellik. "CharacterDto.toDomainModel()" yazınca,
// sanki CharacterDto sınıfının kendi metoduymuş gibi çağırabiliyoruz, ama aslında
// bu fonksiyonu biz burada, DIŞARIDAN ekliyoruz. CharacterDto'nun orijinal tanımına
// hiç dokunmadık.
//
// "private" -> bu fonksiyon SADECE bu dosya içinde kullanılabilir, dışarıya kapalı.
// Çünkü bu dönüşüm mantığı sadece repository'nin iç işi, başka hiçbir yerin
// bunu doğrudan çağırmasına gerek yok.
private fun CharacterDto.toDomainModel(): Character {
    return Character(
        id = this.id,
        name = this.name,
        status = this.status,
        species = this.species,
        gender = this.gender,
        imageUrl = this.image,          // DTO'daki "image" alanı, domain'de "imageUrl" oldu
        origin = this.origin.name,      // DTO'da origin bir OBJE (OriginDto), biz sadece "name" alanını alıp düz String yaptık
        episodeCount = this.episode.size // DTO'da episode bir URL LİSTESİ, biz sadece kaç tane olduğunu (size) alıyoruz, URL'lerin kendisiyle ilgilenmiyoruz
    )
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) DEPENDENCY INVERSION burada CANLI olarak gerçekleşiyor:
    Bu sınıf (CharacterRepositoryImpl) SOMUT/gerçek bir sınıf, ama ViewModel bunu
    doğrudan tanımayacak. ViewModel sadece "CharacterRepository" interface'ini bilecek.
    Koin, ViewModel'e "interface'i istedin, işte gerçek implementasyon" diyerek
    bu Impl sınıfını arkadan verecek. Bu sayede üst katmanlar (ViewModel),
    alt katmanların (Retrofit, network detayları) DETAYLARINDAN habersiz kalıyor.

 2) SINGLE RESPONSIBILITY (SOLID'in "S"si):
    Bu sınıfın TEK işi: "veriyi getir ve domain modeline çevir". Ne UI ile ilgileniyor,
    ne de Room/veritabanı ile (o da ayrı bir yerde, ileride ele alınacak).

 3) NEDEN AYRI BİR "toDomainModel()" FONKSİYONU, NEDEN İÇE GÖMÜLMEDİ?
    DTO -> Domain dönüşüm mantığı büyüyebilir (tarih formatlama, boş kontrol vb.).
    Ayrı bir fonksiyon olarak tutmak, bu mantığı tek bir yerde toplar ve
    ileride kendi başına test edilebilir hale getirir.

 4) BU SINIF NEREYE BAĞLANACAK?
    Bir sonraki adımda yazacağımız Koin "module"ünde şöyle bir tanım göreceğiz:
      single<CharacterRepository> { CharacterRepositoryImpl(get()) }
    Yani "CharacterRepository interface'i istenirse, CharacterRepositoryImpl ver,
    onun da ihtiyacı olan api'yi (get() ile) otomatik bul ve ver" demek.
 ===========================================================
*/