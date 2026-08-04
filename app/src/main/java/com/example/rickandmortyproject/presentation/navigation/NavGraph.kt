package com.example.rickandmortyproject.presentation.navigation

/*
====================================================================
 PROJE ÖZETİ - RICK AND MORTY EXPLORER & FAVORITES APP
 Buraya kadar attığımız TÜM adımları, birbirleriyle nasıl bağlantılı
 olduklarını ve HANGİ KAVRAMI (OOP/SOLID/MVVM/Clean Architecture)
 nerede kullandığımızı sırayla anlatıyorum.
====================================================================



 1) KLASÖR YAPISI - CLEAN ARCHITECTURE'IN İSKELETİ
 ---------------------------------------------------
 Projeyi 3 ana katmana böldük:
    data        -> dış dünyayla (API, ileride veritabanı) konuşan katman
    domain      -> saf iş mantığı, Android/Retrofit/Compose'dan HABERSİZ katman
    presentation -> ekranları (Compose) ve ViewModel'leri barındıran katman

 Bu ayrımın temel kuralı: BAĞIMLILIK HEP İÇE DOĞRU akar.
    presentation -> domain'e bağımlı
    data         -> domain'e bağımlı (interface'leri implement eder)
    domain       -> HİÇBİR ŞEYE bağımlı değil, en saf/bağımsız katman

 2) DOMAIN KATMANI - Character.kt
 -----------------------------------
 domain/model/Character.kt içine, uygulamanın her yerinde kullanacağımız
 TEMİZ veri modelini yazdık (id, name, status, species, gender, imageUrl,
 origin, episodeCount). Bunu "data class" yaptık çünkü sadece VERİ taşıyor,
 kendine ait davranışı yok - data class bize otomatik toString(), equals(),
 copy() veriyor. Bu sınıf, API'nin JSON formatından TAMAMEN bağımsız -
 yarın API değişse bile bu sınıf aynı kalabilir. Bu, Clean Architecture'ın
 "domain, dış dünyadan habersizdir" kuralının somut örneği.

 3) DATA KATMANI - Network (Retrofit)
 ---------------------------------------
 data/remote/dto/ içine API'nin HAM JSON formatına birebir uyan DTO'ları
 yazdık: OriginDto, CharacterDto, CharacterResponseDto, InfoDto. Bunlar
 domain modelinden FARKLI çünkü API'nin karmaşık/gereksiz alanlarını
 (örn. episode URL listesi) taşıyorlar; biz domain'de sadece SAYISINI
 istiyorduk.

 data/remote/RickAndMortyApi.kt -> bir INTERFACE. İçinde gerçek kod yok,
 sadece "hangi adrese, hangi parametrelerle istek atılacak" tarifi var
 (@GET, @Query, @Path annotation'ları ile). Fonksiyonların "suspend" olması,
 bunların COROUTINE(hafif iş parçacığı{threads}) içinde çağrılıp UI'ı bloklamadan çalışacağı
 anlamına geliyor.

 data/remote/RetrofitInstance.kt -> bir OBJECT (Singleton). Retrofit'in
 GERÇEK çalışan nesnesini burada "by lazy" ile (ilk kullanımda) oluşturduk.
 GsonConverterFactory sayesinde API'den gelen JSON, otomatik olarak
 DTO'larımıza (data transfer object) çevriliyor.

 4) REPOSITORY KATMANI - Dependency Inversion'ın kalbi
 --------------------------------------------------------
 domain/repository/CharacterRepository.kt -> bir INTERFACE. "Karakterleri
 nasıl getireceğini SEN karar ver" der, implementasyon detayını bilmez.
 Dönüş tipi domain.model.Character (DTO değil) - yani bu interface,
 API'nin var olduğunu bile bilmiyor.

 data/repository/CharacterRepositoryImpl.kt -> bu interface'i IMPLEMENT
 eden GERÇEK sınıf. Constructor'dan RickAndMortyApi alıyor (constructor
 injection). İçindeki toDomainModel() adlı EXTENSION FUNCTION, DTO'yu
 domain Character'a çeviriyor (mapping) - bu dönüşüm mantığını ayrı bir
 fonksiyonda tutmamızın sebebi SINGLE RESPONSIBILITY: repository'nin işi
 "veri getirmek", dönüşüm ayrı bir sorumluluk.

 BURADA GERÇEKLEŞEN KAVRAM: SOLID'in "D"si (Dependency Inversion).
 ViewModel (ileride yazacağımız), CharacterRepositoryImpl'i DEĞİL,
 CharacterRepository interface'ini tanıyacak. Böylece ViewModel,
 verinin Retrofit'ten mi, Room'dan mı, yoksa sahte test verisinden mi
 geldiğini HİÇ bilmiyor - üst katmanlar, alt katmanların DETAYLARINDAN
 bağımsız kalıyor.

 5) DEPENDENCY INJECTION - Koin
 ----------------------------------
 di/AppModule.kt -> Koin'e "hangi nesne istenirse nasıl oluşturulacağını"
 tarif ettiğimiz module. single<RickAndMortyApi>, single<CharacterRepository>
 (interface -> implementasyon eşlemesi burada kuruluyor) ve viewModel { }
 tanımları var.

 di/RickAndMortyApplication.kt -> Application sınıfından miras alan,
 uygulama AÇILIR AÇILMAZ (MainActivity'den bile önce) çalışan özel sınıfımız.
 onCreate() içinde startKoin { } ile Koin'i UYGULAMA BOYUNCA BİR KERE
 başlattık. AndroidManifest.xml'de android:name=".di.RickAndMortyApplication"
 diyerek Android'e "normal Application yerine BUNU kullan" dedik - bu satır
 olmasaydı Koin hiç başlamazdı.

 BURADA GERÇEKLEŞEN KAVRAM: Inversion of Control - hiçbir sınıf kendi
 ihtiyacı olan başka bir sınıfı KENDİ ELİYLE oluşturmuyor, kontrol Koin'e
 devredildi.

 6) PRESENTATION KATMANI - Liste Ekranı (MVVM)
 --------------------------------------------------
 presentation/list/CharacterListState.kt -> ekranın o anki durumunu TEK
 BİR data class'ta toplayan "UI State" (characters, isLoading, isLoadingMore,
 error, currentPage, endReached). Her alanın varsayılan değeri var, böylece
 CharacterListState() diyerek "hiçbir şey olmamış" başlangıç durumunu
 kolayca üretebiliyoruz.

 presentation/list/CharacterListViewModel.kt -> MVVM'in "VM" harfi.
 - private val _state (yazılabilir) / val state (sadece okunabilir) ayrımı:
   OOP'taki ENCAPSULATION (Kapsülleme) - dışarıdan (Compose'dan) kimse state'i doğrudan
   değiştiremesin, sadece ViewModel'in kendisi değiştirsin.
 - repository: CharacterRepository parametresi (yine interface'e bağımlılık,
   Koin bunu otomatik dolduruyor).
 - init { loadCharacters() } -> ViewModel oluşturulur oluşmaz ilk sayfayı çeker.
 - loadCharacters() -> viewModelScope.launch { } içinde coroutine başlatıp
   repository'den veri istiyor, başarılıysa state'i günceller (mevcut listeye
   yenilerini EKLER), hata olursa try-catch ile yakalayıp error alanını doldurur.

 presentation/list/CharacterListScreen.kt -> MVVM'in "View" katmanı.
 koinViewModel() ile ViewModel'i Koin'den aldık (elle "ViewModel(repository)"
 yazmadık). collectAsState() ile ViewModel'in StateFlow'unu İZLİYORUZ - state
 her değiştiğinde Compose ekranı OTOMATİK yeniden çiziliyor (recomposition).
 when { } bloğuyla isLoading/error/liste durumlarına göre farklı UI çiziyoruz.
 Bu dosyada HİÇBİR iş mantığı (network, hata kararı) YOK - sadece state'e
 bakıp çiziyor, bu da MVVM'in temel amacı: View ve iş mantığının ayrılması.

 presentation/list/CharacterCard.kt -> tek bir karakterin kart tasarımı.
 Coil'in AsyncImage'ı ile profil resmini gösteriyor, character.status'a göre
 renkli bir nokta (when bloğu) çiziyor, kalp butonu var (şimdilik isFavorite
 sabit false, Room'u bağlayınca gerçek değer gelecek). Bu Composable
 "STATELESS" - kendi kararını vermiyor, onCardClick/onFavoriteClick gibi
 fonksiyonları parametre olarak dışarıdan alıp, "bir şey oldu" bilgisini
 YUKARI fırlatıyor (state hoisting).

 7) MAINACTIVITY - HER ŞEYİN BAĞLANDIĞI YER
 ------------------------------------------------
 MainActivity.kt'deki setContent { } içinde artık Greeting("Android") değil,
 Scaffold içinde CharacterListScreen() çağrılıyor. Bu, tüm katmanların
 (Retrofit -> Repository -> Koin -> ViewModel -> Compose) İLK KEZ bir araya
 gelip ÇALIŞTIĞI nokta oldu - ekranda gerçek karakterler, resimli kartlar
 halinde listelendi.

 8) BURADAN SONRASI (henüz yapılmadı, planlanan sıra)
 ----------------------------------------------------------
    - Infinite scroll (LazyListState ile listenin sonuna gelince yeni sayfa çekmek)
    - Arama çubuğu + debounce (300ms) + status filter chip'leri
    - Shimmer loading efekti, Retry butonlu hata ekranı
    - Room veritabanı (Entity, DAO, Database) + gerçek favori ekleme/çıkarma
    - Detay ekranı + BU DOSYADA kurduğumuz Navigation (NavHost, route'lar)
    - Fade-in/slide-in animasyonları, resme tıklayınca tam ekran büyütme
    - Kalp butonuna scale/spring animasyonu
    - Bottom Navigation + "Favorilerim" sekmesi

====================================================================
*/

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rickandmortyproject.presentation.list.CharacterListScreen

// "sealed class" -> Kotlin'e özgü, "bu sınıfın SADECE BELLİ, SINIRLI sayıda
// alt türü olabilir" diyen özel bir yapı. Burada uygulamamızdaki TÜM
// ekranları (route'ları) buraya, tek bir yerde, GÜVENLİ şekilde tanımlıyoruz.
// Route isimlerini elle String olarak her yerde yazmak yerine (yazım hatasına
// açık), bu sınıfı kullanacağız - Kotlin, "when" ile kontrol ederken TÜM
// alt türlerin karşılandığından emin olmamızı sağlıyor.
sealed class Screen(val route: String) {
    object CharacterList : Screen("character_list")

    // İleride detay ekranını yazınca, buraya "id" parametresi alan bir
    // route daha ekleyeceğiz, örn: object CharacterDetail : Screen("character_detail/{id}")
}

// "NavHost" -> Compose Navigation'ın kalbi. Hangi route'ta hangi
// Composable'ın gösterileceğini burada eşliyoruz.
@Composable
fun AppNavGraph(
    // "rememberNavController()" -> ekranlar arası geçişi YÖNETEN nesneyi
    // oluşturuyor. "remember", Compose'a "bu nesneyi recomposition'lar
    // arasında HATIRLA, her seferinde YENİDEN oluşturma" demek.
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CharacterList.route, // uygulama AÇILINCA hangi ekran gösterilsin
                modifier = modifier

    ) {
        // "composable(route) { ... }" -> "bu route'a gelindiğinde, şu
        // Composable'ı çiz" demek.
        composable(Screen.CharacterList.route) {
            CharacterListScreen()
        }

        // İleride buraya detay ekranının route tanımını ekleyeceğiz.
    }
}