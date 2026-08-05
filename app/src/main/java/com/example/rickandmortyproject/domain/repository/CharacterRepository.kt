package com.example.rickandmortyproject.domain.repository

import com.example.rickandmortyproject.domain.model.Character
import kotlinx.coroutines.flow.Flow

// Bu bir INTERFACE. Yani içinde gerçek kod (gövde) yok, sadece
// "hangi fonksiyonlar olacak" diye bir sözleşme/imza listesi var.
// Bunu ViewModel kullanacak ama arkasında gerçekte NE olduğunu
// (Retrofit mi, Room mu, sahte test verisi mi) hiç bilmeyecek.
interface CharacterRepository {

    // Karakter listesini getirir. page zorunlu, name ve status opsiyonel
    // (arama/filtre yapılmıyorsa null geçilir, tüm karakterler gelir).
    suspend fun getCharacters(
        page: Int,
        name: String? = null,
        status: String? = null
    ): List<Character>

    // Tek bir karakterin detayını, id'sine göre getirir.
    suspend fun getCharacterById(id: Int): Character

    // ============ YENİ EKLENEN FAVORİ FONKSİYONLARI ============

    // Bir karakteri favorilere EKLER (veya zaten varsa üzerine yazar).
    // "suspend" -> Room işlemleri de (Retrofit gibi) zaman alabilir
    // (disk okuma/yazma), bu yüzden coroutine içinde çağrılmalı.
    suspend fun addFavorite(character: Character)

    // Bir karakteri favorilerden ÇIKARIR.
    suspend fun removeFavorite(character: Character)

    // TÜM favori karakterlerin listesini, CANLI/GÜNCEL bir akış olarak verir.
    // "Flow<List<Character>>" -> suspend fun DEĞİL, çünkü bu TEK SEFERLİK
    // bir cevap değil, SÜREKLİ güncel kalan bir veri akışı. Favorilere bir
    // şey eklenip/çıkarıldığında, bunu izleyen HERKES otomatik haberdar olur.
    fun getFavorites(): Flow<List<Character>>

    // Belirli bir karakterin favoride olup OLMADIĞINI, yine CANLI bir
    // akış olarak verir - karakter favoriye eklenir/çıkarılırsa, bu Flow'u
    // izleyen kart/ekran OTOMATİK güncellenir (kalp ikonunun dolup boşalması gibi).
    fun isFavorite(characterId: Int): Flow<Boolean>
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) BU INTERFACE NEDEN VAR? (Dependency Inversion Principle - SOLID'in "D"si)
    ViewModel, ileride bu dosyayı kullanacak ("private val repository: CharacterRepository"
    şeklinde). Yani ViewModel, GERÇEK implementasyona (CharacterRepositoryImpl'e) değil,
    bu SOYUT sözleşmeye bağımlı olacak. Bunun faydası:
      - Yarın Retrofit yerine başka bir kütüphane kullansak, sadece implementasyon
        (data/repository/CharacterRepositoryImpl.kt) değişir, ViewModel'in TEK SATIRI değişmez.
      - Test yazarken, gerçek ağ isteği atmayan sahte (Fake) bir repository verip
        ViewModel'i internet olmadan test edebiliriz.

 2) DÖNÜŞ TİPİ NEDEN "Character" (domain model), "CharacterDto"/"FavoriteCharacterEntity"
    DEĞİL?
    Bu interface, API'nin ham/karmaşık JSON formatından VE Room'un veritabanı
    detaylarından (Entity, @PrimaryKey gibi) TAMAMEN habersiz. Sadece "temiz"
    domain modelini konuşuyor. ViewModel, bir karakterin Retrofit'ten mi yoksa
    Room'dan mı geldiğini HİÇ bilmeden, HEP AYNI Character tipiyle çalışıyor.

 3) "suspend fun" NEDEN?
    Bu fonksiyonlar ağ isteği/disk işlemi atacağı için zaman alabilir. "suspend"
    anahtar kelimesi, bu fonksiyonun bir coroutine içinde çağrılması gerektiğini
    söyler, böylece bu işlem UI thread'ini (ana thread'ini) bloklamadan, arka
    planda çalışabilir.

 4) NEDEN "getFavorites()" VE "isFavorite()" suspend fun DEĞİL DE Flow
    DÖNDÜRÜYOR?
    addFavorite/removeFavorite, TEK SEFERLİK işlemler ("şunu ekle/çıkar") -
    suspend fun yeterli. Ama "favori listesi nedir" ya da "bu karakter favoride
    mi" sorularının cevabı ZAMAN İÇİNDE DEĞİŞEBİLİR (kullanıcı başka bir ekranda
    favoriye ekleyebilir) - Flow, bu SÜREKLİ GÜNCEL kalma ihtiyacını karşılıyor.

 5) NEREYE BAĞLANACAK?
    "data/repository/CharacterRepositoryImpl.kt" bu interface'i implement edecek
    (gerçek kodu yazacak, hem RickAndMortyApi'yi hem FavoriteCharacterDao'yu
    kullanarak). Koin de, ViewModel bu interface'i istediğinde, arkadan
    CharacterRepositoryImpl nesnesini verecek şekilde ayarlanacak.
 ===========================================================
*/