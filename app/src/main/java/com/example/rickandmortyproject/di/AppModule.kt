package com.example.rickandmortyproject.di

import androidx.room.Room
import com.example.rickandmortyproject.data.local.AppDatabase
import com.example.rickandmortyproject.data.remote.RetrofitInstance
import com.example.rickandmortyproject.data.remote.RickAndMortyApi
import com.example.rickandmortyproject.data.repository.CharacterRepositoryImpl
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import com.example.rickandmortyproject.presentation.favorites.FavoritesViewModel
import com.example.rickandmortyproject.presentation.list.CharacterListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// "module { }" Koin'e özgü bir yapı (DSL - Domain Specific Language):
// içine "şu istenirse böyle oluştur" diye tarifler yazıyoruz.
val appModule = module {

    // "single { }" -> bu nesneden UYGULAMA BOYUNCA sadece BİR TANE oluşturulsun,
    // her istendiğinde aynısı tekrar verilsin (Singleton mantığı, RetrofitInstance'taki
    // "by lazy" ile aynı amaca hizmet ediyor, burada Koin üzerinden yapıyoruz).
    single<RickAndMortyApi> { RetrofitInstance.api }

    // "Room.databaseBuilder(...)" -> AppDatabase'in GERÇEK, çalışan bir
    // örneğini (instance) oluşturuyoruz.
    //
    // "androidContext()" -> Koin'in bize sağladığı, uygulamanın Context'ine
    // erişme yöntemi (hatırlarsan, RickAndMortyApplication'da
    // startKoin { androidContext(this) } demiştik - Koin bu Context'i
    // burada, ihtiyaç duyan HERKESE otomatik dağıtıyor).
    //
    // "AppDatabase::class.java" -> hangi veritabanı sınıfını oluşturacağımızı
    // Room'a söylüyoruz.
    //
    // "\"rick_and_morty_database\"" -> veritabanı dosyasının, telefonun
    // hafızasındaki GERÇEK dosya adı.
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "rick_and_morty_database"
        ).build()
    }

    // DAO'yu, veritabanından ÇEKEREK Koin'e tanıtıyoruz. Böylece DAO'ya
    // ihtiyaç duyan herhangi bir sınıf (ileride Repository), "get()" ile
    // bunu otomatik alabilecek - AppDatabase'i elle çağırmasına gerek
    // kalmadan.
    single {
        get<AppDatabase>().favoriteCharacterDao()
    }

    // "get()" -> Koin'e "bu parametreyi de SEN bul, ben nereden geldiğini bilmek istemiyorum" demek.
    // Burada CharacterRepositoryImpl'in ihtiyacı olan RickAndMortyApi'yi Koin,
    // yukarıdaki satırda tanımladığımız "single<RickAndMortyApi>"den otomatik bulup verecek.
    //
    // "single<CharacterRepository> { ... }" -> DİKKAT: burada < > içinde INTERFACE yazıyoruz
    // (implementasyon değil). Yani "CharacterRepository interface'i istenirse,
    // CharacterRepositoryImpl nesnesi ver" demiş oluyoruz. Dependency Inversion burada
    // somutlaşıyor: ViewModel ileride sadece "CharacterRepository" isteyecek, Koin arkadan
    // gerçek implementasyonu (Impl) getirecek, ViewModel bunu hiç bilmeyecek.
    single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }

    // "viewModel { }" -> Koin'in Compose/Android'e özel ViewModel tanımlama şekli.
    // Compose ekranında "koinViewModel()" dediğimizde, Koin bu tarifi kullanıp
    // bize hazır bir CharacterListViewModel verecek -
    // "get()" -> ihtiyacı olan CharacterRepository'yi yukarıdaki tanımdan otomatik bulur.
    viewModel { CharacterListViewModel(get()) }

    // YENİ EKLENEN SATIR: FavoritesViewModel'i de AYNI mantıkla Koin'e
    // tanıtıyoruz. "get()" -> ihtiyacı olan CharacterRepository'yi (AYNI
    // single<CharacterRepository> tanımından, yani AYNI Repository nesnesini)
    // otomatik bulup veriyor. Bu sayede FavoritesViewModel ve
    // CharacterListViewModel, TAM OLARAK AYNI Repository'yi (dolayısıyla
    // AYNI Room veritabanını) paylaşıyorlar - biri favoriye eklerken,
    // diğeri bunu ANINDA görebiliyor.
    viewModel { FavoritesViewModel(get()) }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) BU DOSYA NEDEN VAR?
    Şu ana kadar RetrofitInstance ve CharacterRepositoryImpl'i elle birbirine
    bağlamadık (yani hiçbir yerde "CharacterRepositoryImpl(RetrofitInstance.api)"
    diye elle yazmadık). Bunun yerine bu module'de "birini istersen diğerini
    otomatik bul" diye Koin'e TARİF ettik. İleride bir ViewModel yazdığımızda,
    ViewModel'in constructor'ına "CharacterRepository" parametresi koyacağız,
    Koin bunu OTOMATİK olarak burada tanımladığımız şekilde dolduracak.
    Bu sayede hiçbir sınıf, kendi ihtiyacı olan başka bir sınıfı KENDİ ELİYLE
    oluşturmak zorunda kalmıyor (buna "Inversion of Control" denir - kontrol,
    sınıfın kendisinden alınıp dışarıdaki bir çerçeveye - Koin'e - devrediliyor).

 2) "single" NE DEMEK, ALTERNATİFİ VAR MI?
    Koin'de "single" dışında "factory" de var: "factory" her istendiğinde YENİ
    bir nesne oluşturur, "single" ise hep AYNI nesneyi verir. Retrofit, Room
    veritabanı ve Repository gibi "ağır" ve durum (state) taşımayan nesneler
    için "single" kullanmak doğrudur - her ekranda yeniden ağ bağlantısı/
    veritabanı bağlantısı kurmaya gerek yok.

 3) BU MODULE NEREDE "AKTİF" EDİLECEK?
    RickAndMortyApplication.kt içinde, startKoin { modules(appModule) } ile
    zaten aktif ediyoruz - o adımı daha önce tamamlamıştık.

 4) "single<CharacterRepository> { CharacterRepositoryImpl(get(), get()) }"
    SATIRINDAKİ İKİ "get()" NE İŞE YARIYOR?
    CharacterRepositoryImpl İKİ parametre alıyor: biri RickAndMortyApi
    (Retrofit için), diğeri FavoriteCharacterDao (Room için). Koin, sırayla
    İKİSİNİ de yukarıdaki tanımlardan otomatik bulup Repository'ye veriyor -
    biz hangisinin hangi sırada geldiğini elle takip etmiyoruz, Koin bunu
    constructor'daki parametre TÜRLERİNE bakarak otomatik eşleştiriyor.

 5) NEDEN İKİ AYRI "viewModel { }" TANIMI VAR (CharacterListViewModel VE
    FavoritesViewModel için)?
    Her ekranın KENDİ ViewModel'i olmalı - bu MVVM'in temel kuralı, her View
    kendi state'ini yöneten kendi ViewModel'ine sahip olur. Ama İKİSİ de
    AYNI CharacterRepository'yi (get() ile) kullanıyor - yani "veri kaynağı"
    PAYLAŞILIYOR, ama "ekran durumu" (state) PAYLAŞILMIYOR, her ekran kendi
    state'ini KENDİ ViewModel'inde tutuyor. Bu, "tek veri kaynağı, çoklu
    ekran state'i" deseninin güzel bir örneği.


 ===========================================================
*/