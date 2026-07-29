package com.example.rickandmortyproject.di

import com.example.rickandmortyproject.data.remote.RetrofitInstance
import com.example.rickandmortyproject.data.remote.RickAndMortyApi
import com.example.rickandmortyproject.data.repository.CharacterRepositoryImpl
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import org.koin.dsl.module
import com.example.rickandmortyproject.presentation.list.CharacterListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel


// "module { }" Koin'e özgü bir yapı (DSL - Domain Specific Language):
// içine "şu istenirse böyle oluştur" diye tarifler yazıyoruz.

    // "single { }" -> bu nesneden UYGULAMA BOYUNCA sadece BİR TANE oluşturulsun,
    // her istendiğinde aynısı tekrar verilsin (Singleton mantığı, RetrofitInstance'taki
    // "by lazy" ile aynı amaca hizmet ediyor, burada Koin üzerinden yapıyoruz).

    // "get()" -> Koin'e "bu parametreyi de SEN bul, ben nereden geldiğini bilmek istemiyorum" demek.
    // Burada CharacterRepositoryImpl'in ihtiyacı olan RickAndMortyApi'yi Koin,
    // yukarıdaki satırda tanımladığımız "single<RickAndMortyApi>"den otomatik bulup verecek.
    //
    // "single<CharacterRepository> { ... }" -> DİKKAT: burada < > içinde INTERFACE yazıyoruz
    // (implementasyon değil). Yani "CharacterRepository interface'i istenirse,
    // CharacterRepositoryImpl nesnesi ver" demiş oluyoruz. Dependency Inversion burada
    // somutlaşıyor: ViewModel ileride sadece "CharacterRepository" isteyecek, Koin arkadan
    // gerçek implementasyonu (Impl) getirecek, ViewModel bunu hiç bilmeyecek.

    val appModule = module {
        single<RickAndMortyApi> { RetrofitInstance.api }
        single<CharacterRepository> { CharacterRepositoryImpl(get()) }

        // "viewModel { }" -> Koin'in Compose/Android'e özel ViewModel tanımlama şekli.
        // Compose ekranında "koinViewModel()" dediğimizde, Koin bu tarifi kullanıp
        // bize hazır bir CharacterListViewModel verecek -
        // "get()" -> ihtiyacı olan CharacterRepository'yi yukarıdaki tanımdan otomatik bulur.
        viewModel { CharacterListViewModel(get()) }
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
    bir nesne oluşturur, "single" ise hep AYNI nesneyi verir. Retrofit ve
    Repository gibi "ağır" ve durum (state) taşımayan nesneler için "single"
    kullanmak doğrudur - her ekranda yeniden ağ bağlantısı kurmaya gerek yok.

 3) BU MODULE NEREDE "AKTİF" EDİLECEK?
    Az sonra Application sınıfımızda (MainActivity'den ayrı, uygulamanın
    başlangıç noktası olan bir sınıf yazacağız) Koin'i başlatıp bu module'ü
    ona tanıtacağız: startKoin { modules(appModule) }. O adıma geldiğimizde
    detaylı

 */