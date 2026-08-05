package com.example.rickandmortyproject.data.repository

import com.example.rickandmortyproject.data.local.FavoriteCharacterDao
import com.example.rickandmortyproject.data.local.FavoriteCharacterEntity
import com.example.rickandmortyproject.data.remote.RickAndMortyApi
import com.example.rickandmortyproject.data.remote.dto.CharacterDto
import com.example.rickandmortyproject.domain.model.Character
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ": CharacterRepository" -> bu sınıf, o interface'i IMPLEMENT ediyor.
// Yani "sözleşmede söz verdiğim tüm fonksiyonları burada gerçekten dolduruyorum" demek.
//
// "api: RickAndMortyApi" VE "dao: FavoriteCharacterDao" constructor'dan
// (kurucu fonksiyondan) alınıyor. Buna CONSTRUCTOR INJECTION denir: bu sınıf,
// ihtiyaç duyduğu şeyleri (api, dao) kendisi oluşturmuyor, dışarıdan HAZIR
// olarak alıyor. Koin, AppModule.kt'deki tanımlara göre bunları otomatik sağlıyor.
class CharacterRepositoryImpl(
    private val api: RickAndMortyApi,
    private val dao: FavoriteCharacterDao
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

    // ============ YENİ EKLENEN FAVORİ FONKSİYONLARI ============

    // "character.toEntity()" -> domain Character'ı, Room'un anlayacağı
    // FavoriteCharacterEntity'ye ÇEVİRİP, DAO'nun insertFavorite fonksiyonuna
    // veriyoruz. DAO da bunu GERÇEKTEN veritabanına yazıyor.
    override suspend fun addFavorite(character: Character) {
        dao.insertFavorite(character.toEntity())
    }

    override suspend fun removeFavorite(character: Character) {
        dao.deleteFavorite(character.toEntity())
    }

    // "dao.getAllFavorites()" -> Flow<List<FavoriteCharacterEntity>> döndürüyordu.
    // ".map { entities -> ... }" -> BURADAKİ "map", listelerdeki map'ten FARKLI -
    // bu Flow'un KENDİ map operatörü: Flow'dan HER YENİ liste geldiğinde,
    // içindeki HER Entity'yi Character'a çevirip YENİ bir Flow<List<Character>>
    // üretiyor. Yani Room'dan gelen HAM veri, domain katmanına ULAŞMADAN ÖNCE
    // burada "temizleniyor".
    override fun getFavorites(): Flow<List<Character>> {
        return dao.getAllFavorites().map { entities ->
            entities.map { entity -> entity.toDomainModel() }
        }
    }

    override fun isFavorite(characterId: Int): Flow<Boolean> {
        return dao.isFavorite(characterId)
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

// ============ YENİ EKLENEN EXTENSION FUNCTION'LAR (Entity <-> Character) ============

// "Character.toEntity()" -> domain modelini, Room'un @Entity annotation'lı
// sınıfına çeviriyor. Bu sefer alanlar BİREBİR aynı isimde/tipte olduğu için
// dönüşüm çok BASİT - CharacterDto'daki gibi karmaşık bir "origin.name" ya da
// "episode.size" hesaplaması YOK, çünkü Entity'yi BİZ tasarladık, domain
// modeliyle UYUMLU olacak şekilde.
private fun Character.toEntity(): FavoriteCharacterEntity {
    return FavoriteCharacterEntity(
        id = this.id,
        name = this.name,
        status = this.status,
        species = this.species,
        gender = this.gender,
        imageUrl = this.imageUrl,
        origin = this.origin,
        episodeCount = this.episodeCount
    )
}

// Ters yönde dönüşüm: Room'dan OKUNAN bir Entity'yi, tekrar domain Character'a
// çeviriyoruz - böylece ViewModel/UI katmanı, bu verinin Room'dan mı yoksa
// Retrofit'ten mi geldiğini HİÇ FARK ETMEDEN, hep AYNI Character tipiyle çalışıyor.
private fun FavoriteCharacterEntity.toDomainModel(): Character {
    return Character(
        id = this.id,
        name = this.name,
        status = this.status,
        species = this.species,
        gender = this.gender,
        imageUrl = this.imageUrl,
        origin = this.origin,
        episodeCount = this.episodeCount
    )
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) DEPENDENCY INVERSION burada CANLI olarak gerçekleşiyor:
    Bu sınıf (CharacterRepositoryImpl) SOMUT/gerçek bir sınıf, ama ViewModel bunu
    doğrudan tanımayacak. ViewModel sadece "CharacterRepository" interface'ini bilecek.
    Koin, ViewModel'e "interface'i istedin, işte gerçek implementasyon" diyerek
    bu Impl sınıfını arkadan verecek. Bu sayede üst katmanlar (ViewModel),
    alt katmanların (Retrofit, Room, network/veritabanı detayları) DETAYLARINDAN
    habersiz kalıyor.

 2) SINGLE RESPONSIBILITY (SOLID'in "S"si):
    Bu sınıfın TEK işi: "veriyi getir/sakla ve domain modeline çevir". UI ile
    hiç ilgilenmiyor, sadece VERİ katmanının koordinasyonunu yapıyor - ister
    Retrofit'ten (uzak sunucu), ister Room'dan (yerel veritabanı) gelsin,
    dışarıya HEP aynı temiz Character tipini sunuyor.

 3) NEDEN AYRI "toDomainModel()" / "toEntity()" FONKSİYONLARI, NEDEN İÇE GÖMÜLMEDİ?
    Dönüşüm mantığı büyüyebilir (tarih formatlama, boş kontrol vb.). Ayrı
    fonksiyonlar olarak tutmak, bu mantığı tek bir yerde toplar ve ileride
    kendi başına test edilebilir hale getirir.

 4) NEDEN İKİ FARKLI "toDomainModel()" FONKSİYONU VAR (biri CharacterDto için,
    biri FavoriteCharacterEntity için)? ÇAKIŞMIYORLAR MI?
    Kotlin'de "extension function"lar, HANGİ TİP üzerinde tanımlandıklarına göre
    ayırt edilir - "CharacterDto.toDomainModel()" ile "FavoriteCharacterEntity.
    toDomainModel()" AYNI İSİMDE ama FARKLI tipler üzerinde çalıştıkları için
    Kotlin bunları birbirine KARIŞTIRMAZ (buna "function overloading" - fonksiyon
    aşırı yükleme - denir, OOP'un bir başka örneği). İkisi de "elimdeki HAM veriyi
    temiz Character'a çevir" görevini yapıyor, ama kaynakları (API mi, veritabanı
    mı) farklı.

 5) "getFavorites()" İÇİNDEKİ ".map { }" NEDEN "İKİ KERE" KULLANILIYOR
    (dao.getAllFavorites().map { entities -> entities.map { ... } })?
    Dıştaki ".map" -> Flow'un map'i: Flow'dan gelen HER YENİ LİSTEYİ işler.
    İçteki ".map" -> normal List'in map'i: o listenin İÇİNDEKİ HER TEK
    Entity'yi Character'a çevirir. İkisi FARKLI şeyler ama AYNI isimde -
    Kotlin, hangi "map"in hangi tipte çalıştığını CONTEXT'ten (o an
    hangi nesne üzerinde çağrıldığından) anlıyor.

 6) BU SINIF NEREYE BAĞLANACAK?
    AppModule.kt'de zaten güncellediğimiz şu tanım burada devreye giriyor:
      single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }
    Yani "CharacterRepository interface'i istenirse, CharacterRepositoryImpl ver,
    onun da ihtiyacı olan api VE dao'yu (iki ayrı get() ile) otomatik bul ve ver" demek.

 7) SIRADA NE VAR?
    CharacterListViewModel'i güncelleyip, her karakterin GERÇEKTEN favoride
    olup olmadığını (isFavorite Flow'unu izleyerek) göstermesini ve favori
    butonuna basınca gerçekten addFavorite/removeFavorite çağırmasını
    sağlayacağız.
 ===========================================================
*/