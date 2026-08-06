package com.example.rickandmortyproject.di

import androidx.room.Room
import com.example.rickandmortyproject.data.local.AppDatabase
import com.example.rickandmortyproject.data.remote.RetrofitInstance
import com.example.rickandmortyproject.data.remote.RickAndMortyApi
import com.example.rickandmortyproject.data.repository.CharacterRepositoryImpl
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import com.example.rickandmortyproject.presentation.detail.CharacterDetailViewModel
import com.example.rickandmortyproject.presentation.favorites.FavoritesViewModel
import com.example.rickandmortyproject.presentation.list.CharacterListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single<RickAndMortyApi> { RetrofitInstance.api }

    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "rick_and_morty_database"
        ).build()
    }

    single {
        get<AppDatabase>().favoriteCharacterDao()
    }

    single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }

    viewModel { CharacterListViewModel(get()) }

    viewModel { FavoritesViewModel(get()) }

    // ============ YENİ EKLENEN: CharacterDetailViewModel tanımı ============
    //
    // Şu ana kadarki "viewModel { CharacterListViewModel(get()) }" gibi
    // tanımlarda, lambda'nın İÇİNDE hiçbir parametre YOKTU - direkt "get()"
    // ile Koin'in ZATEN bildiği bir nesneyi (repository) istiyorduk.
    //
    // Burada ise lambda'nın BAŞINA "params ->" yazdık. Bu, Koin'e "bu
    // ViewModel'i oluştururken, dışarıdan (Compose ekranından) bana EK
    // bilgi gelecek, onu 'params' adında bir kutuda topla, ben de ordan
    // ÇEKEYİM" demek.
    //
    // "params" nesnesi, tam olarak Compose tarafındaki şu satırdan besleniyor
    // (CharacterDetailScreen.kt'de yazmıştık):
    //     koinViewModel { parametersOf(characterId) }
    // "parametersOf(characterId)" çağrıldığında, characterId değeri (örneğin
    // 5) bir "parametre paketi" haline getirilip Koin'e GÖNDERİLİYOR. Koin
    // de bu paketi, burada "params" olarak bize GERİ VERİYOR.
    viewModel { params ->
        CharacterDetailViewModel(
            // "params.get<Int>()" -> "params" paketinin İÇİNDEN, TÜRÜ Int
            // olan değeri ÇEKİYORUZ. Compose tarafında SADECE TEK bir
            // parametre (characterId) gönderdiğimiz için, Koin bunu hangi
            // constructor parametresine (characterId'ye) karşılık geldiğini
            // TÜRÜNE bakarak (Int olduğu için) eşleştiriyor. Eğer BİRDEN
            // FAZLA parametre göndermiş olsaydık (örn. parametersOf(id,
            // isim)), params.get<Int>(), params.get<String>() şeklinde
            // TÜRLERİNE göre AYRI AYRI çekerdik.
            characterId = params.get<Int>(),

            // "get()" -> repository'yi ise HİÇ "params" ile UĞRAŞMADAN,
            // HER ZAMANKİ gibi Koin'in ZATEN Singleton olarak bildiği
            // single<CharacterRepository> tanımından otomatik buluyoruz.
            // Yani BU ViewModel'in İKİ farklı KAYNAKTAN bağımlılık aldığını
            // görüyoruz: biri "her seferinde DEĞİŞEN, dışarıdan gelen" veri
            // (characterId - params ile), diğeri "hep AYNI, Koin'in KENDİ
            // bildiği" veri (repository - get() ile).
            repository = get()
        )
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) BU DOSYA NEDEN VAR?
    Bu module'de "birini istersen diğerini otomatik bul" diye Koin'e TARİF
    ediyoruz. Bu sayede hiçbir sınıf, kendi ihtiyacı olan başka bir sınıfı
    KENDİ ELİYLE oluşturmak zorunda kalmıyor (Inversion of Control - kontrol,
    sınıfın kendisinden alınıp Koin'e devrediliyor).

 2) "single" NE DEMEK, ALTERNATİFİ VAR MI?
    "factory" her istendiğinde YENİ bir nesne oluşturur, "single" ise hep
    AYNI nesneyi verir. Retrofit, Room veritabanı ve Repository gibi "ağır"
    nesneler için "single" kullanmak doğrudur.

 3) BU MODULE NEREDE "AKTİF" EDİLECEK?
    RickAndMortyApplication.kt içinde, startKoin { modules(appModule) } ile
    zaten aktif ediyoruz.

 4) "single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }"
    SATIRINDAKİ İKİ "get()" NE İŞE YARIYOR?
    CharacterRepositoryImpl İKİ parametre alıyor: RickAndMortyApi ve
    FavoriteCharacterDao. Koin, sırayla İKİSİNİ de otomatik bulup veriyor.

 5) NEDEN ÜÇ AYRI "viewModel { }" TANIMI VAR?
    Her ekranın KENDİ ViewModel'i olmalı - MVVM'in temel kuralı. Üçü de
    AYNI CharacterRepository'yi paylaşıyor ama "ekran durumu" PAYLAŞILMIYOR.

 6) "params" NEREDEN GELİYOR, TAM AKIŞI ADIM ADIM İZLEYELİM:
    a) Kullanıcı liste ekranında bir karta tıklar (örn. id=5 olan karaktere).
    b) CharacterListScreen'deki onCardClick, "onCharacterClick(5)" çağırır.
    c) NavGraph.kt'deki lambda çalışır: navController.navigate(
       "character_detail/5").
    d) NavHost, "character_detail/{characterId}" route'unu TANIR, id'yi
       (5'i) route'tan ÇIKARIP CharacterDetailScreen'e "characterId = 5"
       olarak VERİR.
    e) CharacterDetailScreen içindeki "koinViewModel { parametersOf(
       characterId) }" satırı çalışır - yani "parametersOf(5)" demiş oluruz.
    f) Koin, BURADAKİ "viewModel { params -> ... }" tanımını BULUR, "params"
       kutusunun içine "5" değerini KOYAR.
    g) "params.get<Int>()" ile bu "5" değerini ÇEKİP, CharacterDetailViewModel'
       in "characterId" parametresine VERİRİZ.
    Yani "5" sayısı, TÜM bu katmanlardan (Compose -> Navigation -> Koin ->
    ViewModel) GÜVENLE, TÜRÜ KORUNARAK (hep Int olarak, hiç String'e çevrilip
    tekrar Int'e çevrilmeden) akıp gidiyor.

 7) NEDEN CharacterListViewModel VEYA FavoritesViewModel'de "params" GEREKMEDİ?
    Çünkü o ikisi, EKRAN AÇILDIĞINDA "hangi karakter/karakterler" diye BİR
    BİLGİYE ihtiyaç duymuyordu - hepsi AYNI, TÜM listeyi ya da TÜM favorileri
    gösteriyordu. CharacterDetailViewModel ise "HANGİ karakter" sorusuna
    cevap vermesi gereken TEK ViewModel'imiz, bu yüzden dışarıdan (Compose'tan)
    EK bir bilgiye (characterId'ye) ihtiyaç duyan TEK ViewModel de o.

 8) BU DOSYA HANGİ DOSYALARLA BAĞLANTILI?
    - presentation/detail/CharacterDetailScreen.kt -> oradaki
      "koinViewModel { parametersOf(characterId) }" çağrısı, BURADAKİ
      "viewModel { params -> ... }" tarifini TETİKLİYOR.
    - presentation/detail/CharacterDetailViewModel.kt -> constructor'daki
      "characterId: Int, repository: CharacterRepository" parametreleri,
      BURADA params.get<Int>() ve get() ile DOLDURULUYOR.
 ===========================================================
*/