package com.example.rickandmortyproject.domain.repository

import com.example.rickandmortyproject.domain.model.Character

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

 2) DÖNÜŞ TİPİ NEDEN "Character" (domain model), "CharacterDto" DEĞİL?
    Bu interface, API'nin ham/karmaşık JSON formatından haberdar bile değil.
    Sadece "temiz" domain modelini konuşuyor. Bu ayrım, Clean Architecture'ın temeli:
    domain katmanı, dış dünyadan (API, veritabanı) tamamen habersiz, saf iş mantığı katmanı.

 3) "suspend fun" NEDEN?
    Bu fonksiyonlar ağ isteği atacağı için zaman alabilir. "suspend" anahtar kelimesi,
    bu fonksiyonun bir coroutine içinde çağrılması gerektiğini belirtir, böylece
    bu işlem UI thread'ini (ana thread'i) bloklamadan, arka planda çalışabilir.

 4) NEREYE BAĞLANACAK?
    "data/repository/CharacterRepositoryImpl.kt" bu interface'i implement edecek
    (gerçek kodu yazacak). Koin de, ViewModel bu interface'i istediğinde,
    arkadan CharacterRepositoryImpl nesnesini verecek şekilde ayarlanacak.
 ===========================================================
*/