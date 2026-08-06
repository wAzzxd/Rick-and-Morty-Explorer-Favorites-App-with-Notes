package com.example.rickandmortyproject.presentation

/*
====================================================================
 PROJE ÖZETİ - RICK AND MORTY EXPLORER & FAVORITES APP
 Buraya kadar attığımız TÜM adımları, birbirleriyle nasıl bağlantılı
 olduklarını ve HANGİ KAVRAMI (OOP/SOLID/MVVM/Clean Architecture)
 nerede kullandığımızı sırayla anlatıyor. Bu dosya SADECE dokümantasyon
 amaçlı - hiçbir kod çalıştırmıyor, projenin hiçbir yerinden import
 edilmiyor. Sadece "ben bunu neden böyle yapmıştım" dediğinde bakman için.
====================================================================

 1) KLASÖR YAPISI - CLEAN ARCHITECTURE'IN İSKELETİ
 ---------------------------------------------------
 Projeyi 3 ana katmana böldük:
    data        -> dış dünyayla (API, Room veritabanı) konuşan katman
    domain      -> saf iş mantığı, Android/Retrofit/Room/Compose'dan HABERSİZ katman
    presentation -> ekranları (Compose) ve ViewModel'leri barındıran katman

 Bu ayrımın temel kuralı: BAĞIMLILIK HEP İÇE DOĞRU akar.
    presentation -> domain'e bağımlı
    data         -> domain'e bağımlı (interface'leri implement eder)
    domain       -> HİÇBİR ŞEYE bağımlı değil, en saf/bağımsız katman

 2) DOMAIN KATMANI - Character.kt
 -----------------------------------
 domain/model/Character.kt içine, uygulamanın her yerinde kullanacağımız
 TEMİZ veri modelini yazdık (id, name, status, species, gender, imageUrl,
 origin, episodeCount). "data class" yaptık çünkü sadece VERİ taşıyor,
 kendine ait davranışı yok - data class bize otomatik toString(), equals(),
 copy() veriyor. Bu sınıf, API'nin JSON formatından VE Room'un veritabanı
 detaylarından TAMAMEN bağımsız - yarın API ya da veritabanı değişse bile
 bu sınıf aynı kalabilir.

 3) DATA KATMANI - Network (Retrofit)
 ---------------------------------------
 data/remote/dto/ içine API'nin HAM JSON formatına birebir uyan DTO'ları
 yazdık: OriginDto, CharacterDto, CharacterResponseDto, InfoDto.

 data/remote/RickAndMortyApi.kt -> bir INTERFACE. @GET, @Query, @Path
 annotation'larıyla "hangi adrese, hangi parametrelerle istek atılacak"
 tarifini yapıyor. Fonksiyonlar "suspend" çünkü coroutine içinde çağrılıp
 UI'ı bloklamadan çalışıyorlar.

 data/remote/RetrofitInstance.kt -> bir OBJECT (Singleton). Retrofit'in
 gerçek nesnesini "by lazy" ile oluşturuyor, GsonConverterFactory ile
 JSON -> DTO çevirimini otomatikleştiriyor.

 4) DATA KATMANI - Yerel Veritabanı (Room)
 ---------------------------------------------
 data/local/FavoriteCharacterEntity.kt -> @Entity ile işaretli, favori
 karakterlerin veritabanı tablosunu temsil eden data class. domain.model.
 Character'dan AYRI çünkü Room'a özgü annotation'lar (@Entity, @PrimaryKey)
 taşıyor - domain katmanını Room'a bağımlı hale getirmemek için.

 data/local/FavoriteCharacterDao.kt -> bir INTERFACE (@Dao). Retrofit'in
 API interface'ine ÇOK benzer mantık: @Insert, @Delete, @Query ile
 "veritabanına ne yapılacağını" tarif ediyoruz, Room gerçek kodu bizim
 yerimize üretiyor (KSP sayesinde). getAllFavorites() ve isFavorite()
 fonksiyonları Flow döndürüyor - yani favori listesi/durumu CANLI olarak
 izlenebiliyor, veritabanında değişiklik olunca izleyenler OTOMATİK haberdar
 oluyor.

 data/local/AppDatabase.kt -> Room'un veritabanı tanımı (@Database),
 hangi Entity'lerin (tabloların) olduğunu ve versiyon numarasını belirtiyor.

 5) REPOSITORY KATMANI - Dependency Inversion'ın kalbi
 --------------------------------------------------------
 domain/repository/CharacterRepository.kt -> bir INTERFACE. Karakter
 getirme (getCharacters, getCharacterById) VE favori yönetimi (addFavorite,
 removeFavorite, getFavorites, isFavorite) fonksiyonlarının hepsini
 tanımlıyor - implementasyon detayını (Retrofit mi Room mu) hiç bilmiyor.

 data/repository/CharacterRepositoryImpl.kt -> bu interface'i IMPLEMENT
 eden GERÇEK sınıf. Constructor'dan HEM RickAndMortyApi HEM
 FavoriteCharacterDao alıyor (constructor injection, iki bağımlılık birden).
 CharacterDto.toDomainModel() ve FavoriteCharacterEntity.toDomainModel() /
 Character.toEntity() adlı EXTENSION FUNCTION'lar, ham veriyi (API'den ya
 da Room'dan) temiz domain modeline (ve tam tersine) çeviriyor.

 BURADA GERÇEKLEŞEN KAVRAM: SOLID'in "D"si (Dependency Inversion).
 ViewModel'ler, CharacterRepositoryImpl'i DEĞİL, CharacterRepository
 interface'ini tanıyor. Böylece verinin Retrofit'ten mi, Room'dan mı
 geldiğini HİÇ bilmeden çalışıyorlar.

 6) DEPENDENCY INJECTION - Koin
 ----------------------------------
 di/AppModule.kt -> Koin'e "hangi nesne istenirse nasıl oluşturulacağını"
 tarif ettiğimiz module:
    single<RickAndMortyApi>       -> Retrofit
    single<AppDatabase>            -> Room veritabanı (Room.databaseBuilder)
    single (DAO)                   -> AppDatabase'den çekiliyor
    single<CharacterRepository>    -> hem api hem dao alarak Impl'i üretiyor
    viewModel { CharacterListViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { params -> CharacterDetailViewModel(params.get(), get()) }
        -> BU SONUNCUSU DİĞERLERİNDEN FARKLI: "characterId" gibi HER
        ekran açılışında DEĞİŞEBİLEN bir parametre alıyor, Compose
        tarafındaki "koinViewModel { parametersOf(characterId) }" çağrısı
        bu değeri "params" olarak Koin'e taşıyor.

 di/RickAndMortyApplication.kt -> Application sınıfından miras alan,
 uygulama AÇILIR AÇILMAZ çalışan özel sınıfımız. onCreate() içinde
 startKoin { androidContext(this); modules(appModule) } ile Koin'i
 BİR KERE başlattık. AndroidManifest.xml'de android:name=
 ".di.RickAndMortyApplication" ile Android'e bunu bildirdik.

 BURADA GERÇEKLEŞEN KAVRAM: Inversion of Control - hiçbir sınıf kendi
 ihtiyacı olan başka bir sınıfı KENDİ ELİYLE oluşturmuyor, kontrol Koin'e
 devredildi.

 7) PRESENTATION KATMANI - Liste Ekranı (MVVM)
 --------------------------------------------------
 presentation/list/CharacterListState.kt -> ekranın TÜM durumunu (characters,
 isLoading, isLoadingMore, error, currentPage, endReached, searchQuery,
 statusFilter, favoriteIds) TEK bir data class'ta topluyor.

 presentation/list/CharacterListViewModel.kt -> MVVM'in "VM" harfi, en
 kapsamlı sınıfımız:
    - _state (private, yazılabilir) / state (public, sadece okunabilir):
      OOP'taki ENCAPSULATION.
    - init{} içinde: ilk sayfayı çeker, searchQueryFlow'u debounce(300)+
      distinctUntilChanged() ile izler, repository.getFavorites()'i izleyip
      favoriteIds kümesini GÜNCEL tutar.
    - loadCharacters(): sayfalama + arama + filtreyi BİRLİKTE repository'ye
      gönderir, HTTP hatalarını (404/429/500/IOException) TÜRÜNE göre ayırt
      edip anlaşılır mesajlara çevirir. CancellationException'ı YAKALAMAZ,
      "throw e" ile tekrar fırlatır (coroutine iptalini GERÇEK hatayla
      karıştırmamak için).
    - resetAndReload(): arama/filtre değiştiğinde listeyi SIFIRLAYIP baştan
      yükler, eski isteği loadJob.cancel() ile iptal eder (race condition
      önleme), isLoading/isLoadingMore'u ELLE false yapar (iptal edilen
      coroutine bunu kendi yapamadığı için).
    - onFavoriteClick(): favoride mi diye bakıp Room'a ekle/çıkar komutu
      verir, state'i ELLE güncellemez - Room'daki değişiklik Flow üzerinden
      OTOMATİK state'e yansır (tek gerçek kaynak prensibi).

 presentation/list/CharacterListScreen.kt -> MVVM'in "View" katmanı.
 Arama kutusu (OutlinedTextField), durum filtre chip'leri (FilterChip,
 LazyRow içinde), infinite scroll (LazyListState + derivedStateOf +
 LaunchedEffect), loading/hata/boş-sonuç/liste durumlarını when{} ile
 yönetiyor, hem tam ekran hem "liste sonunda küçük" Tekrar Dene butonları
 var. "onCharacterClick: (Int) -> Unit" parametresiyle, tıklanan karakterin
 id'sini YUKARI (NavGraph'a) fırlatıyor - navigasyon KARARINI kendisi
 vermiyor. HİÇBİR iş mantığı içermiyor, sadece state'i okuyup çiziyor.

 presentation/list/CharacterCard.kt -> tek bir karakterin kart tasarımı.
 Coil AsyncImage, durum renkli nokta (when), kalp butonu. "STATELESS
 composable" - isFavorite/onCardClick/onFavoriteClick parametrelerini
 DIŞARIDAN alıyor, kendi karar vermiyor (state hoisting). Bu sayede AYNI
 kart, hem liste ekranında hem favoriler ekranında TEKRAR KULLANILIYOR.
 Kalp butonunda Animatable + LaunchedEffect(isFavorite) ile "büyüyüp
 küçülme" (spring/bounce) animasyonu var - isFavorite HER değiştiğinde
 (eklerken de çıkarırken de) tetikleniyor.

 presentation/list/ShimmerEffect.kt -> Modifier.shimmerEffect() adlı bir
 EXTENSION FUNCTION: rememberInfiniteTransition + animateFloat ile SÜREKLİ
 (0'dan 1000'e, tekrar tekrar) değişen bir animasyon değeri üretip, bunu
 bir Brush.linearGradient'in başlangıç/bitiş noktalarına bağlıyor - bu da
 "soldan sağa akan parlak huzme" hissini veriyor. ShimmerCharacterCard
 (gerçek kartın gri iskelet hâli) ve ShimmerCharacterList (6 tanesini
 LazyColumn'da art arda gösteren) Composable'ları da burada. Liste
 ekranında state.isLoading true iken, CircularProgressIndicator YERİNE
 bu shimmer listesi gösteriliyor.

 8) PRESENTATION KATMANI - Favoriler Ekranı (MVVM)
 --------------------------------------------------------
 presentation/favorites/FavoritesViewModel.kt -> ÇOK daha basit bir
 ViewModel: repository.getFavorites()'i .stateIn(viewModelScope,
 SharingStarted.WhileSubscribed(5000), emptyList()) ile DOĞRUDAN bir
 StateFlow'a çeviriyor (CharacterListViewModel'deki gibi elle
 MutableStateFlow yönetmeye GEREK yok, iş basit olduğu için). Tek ekstra
 fonksiyonu onRemoveFavoriteClick() - doğrudan çıkarma (toggle değil,
 çünkü bu ekrandaki her karakter zaten favoride).

 presentation/favorites/FavoritesScreen.kt -> favoriler boşsa bilgilendirici
 mesaj, doluysa LazyColumn + CharacterCard (isFavorite HER ZAMAN true,
 onFavoriteClick DOĞRUDAN kaldırma).

 KRİTİK KAVRAM: Liste, favoriler VE detay ekranı, AYNI CharacterRepository'yi
 (dolayısıyla AYNI Room veritabanını) Koin üzerinden paylaşıyor. Biri
 favoriye ekleyince, diğerleri bunu Flow sayesinde OTOMATİK görüyor - hiçbir
 manuel senkronizasyon kodu YOK.

 9) PRESENTATION KATMANI - Detay Ekranı (MVVM)
 --------------------------------------------------
 presentation/detail/CharacterDetailState.kt -> character (nullable! "boş
 karakter" diye bir şey olmadığı için null kullanıyoruz), isLoading,
 isFavorite, error alanlarını tutan basit bir state.

 presentation/detail/CharacterDetailViewModel.kt -> DİĞER ViewModel'lerden
 FARKI: constructor'da "characterId: Int" alıyor (Koin'e parametersOf ile
 GEÇİRİLİYOR). repository.getCharacterById(characterId) ile TEK bir
 karakteri çekiyor, repository.isFavorite(characterId) Flow'unu izleyip
 kalp durumunu GÜNCEL tutuyor. onFavoriteClick() burada PARAMETRE almıyor
 (CharacterListViewModel'deki gibi) çünkü zaten TEK bir karakterle
 ilgileniyor, state.character'ı DOĞRUDAN kullanabiliyor.

 presentation/detail/CharacterDetailScreen.kt -> Büyük profil resmi
 (tıklanınca tam ekran açılan), isim+kalp, durum, tür/cinsiyet/köken/
 bölüm sayısı satırları. İKİ AYRI AnimatedVisibility var:
    - Biri (visible=true SABİT): ekranın TÜM içeriğinin fadeIn+
      slideInVertically ile "belirerek ve aşağıdan kayarak" GİRİŞ yapması
      için - SADECE character verisi GELDİĞİNDE (state.character != null
      bloğunun İÇİNDE) tetikleniyor.
    - Diğeri (visible=isImageExpanded DEĞİŞKEN): resme tıklayınca AÇILAN,
      tekrar tıklayınca KAPANAN tam ekran resim overlay'i - scaleIn/
      scaleOut ile küçükten büyüğe/büyükten küçüğe animasyonlu.
 "isImageExpanded" gibi SADECE-UI'a-ait, GEÇİCİ state'ler ViewModel'de
 DEĞİL, doğrudan Composable içinde "remember { mutableStateOf(false) }"
 ile tutuluyor - ViewModel'e taşımaya gerek yok çünkü hiçbir iş mantığı
 (network, veritabanı) içermiyor.

 10) NAVİGASYON - NavGraph.kt
 --------------------------------
 sealed class Screen -> tüm route'ları TEK bir yerde, güvenli şekilde
 (yazım hatasına kapalı) tanımlıyor. CharacterDetail route'u
 "character_detail/{characterId}" şeklinde bir YER TUTUCU içeriyor,
 createRoute(id) fonksiyonu bu yer tutucuyu GERÇEK id ile dolduruyor.

 AppNavGraph -> NavHost ile hangi route'ta hangi ekranın gösterileceğini
 eşliyor. composable(...) bloğuna "arguments = listOf(navArgument(...))"
 vererek, route'taki "{characterId}" kısmının GERÇEKTE bir Int olduğunu
 Navigation kütüphanesine bildiriyoruz - bu sayede backStackEntry.arguments?.
 getInt("characterId") ile GÜVENLE, doğru TÜRDE çekebiliyoruz.

 MainActivity, DOĞRUDAN CharacterListScreen değil, SADECE AppNavGraph()'ı
 çağırıyor (bir ekranı birden fazla yerden çağırmanın çift-render hatasına
 yol açtığını YAŞAYARAK öğrendik). navController, MainActivity seviyesinde
 oluşturulup HEM Bottom Navigation'a HEM AppNavGraph'a PARAMETRE olarak
 geçiriliyor - ikisi AYNI navigasyon "beynini" paylaşıyor.

 KRİTİK KAVRAM: CharacterListScreen ve CharacterDetailScreen, navController'ı
 HİÇ TANIMIYOR - sadece "şuna tıklandı" / "geri gidilmek istendi" bilgisini
 YUKARI (NavGraph'a) fırlatıyorlar, GERÇEK navigasyon kararını NavGraph
 veriyor (state hoisting'in navigasyon versiyonu).

 11) MAINACTIVITY - HER ŞEYİN BAĞLANDIĞI YER + BOTTOM NAVIGATION
 ------------------------------------------------------------------
 setContent { } içinde Scaffold(bottomBar = { AppBottomNavigationBar(...) })
 + AppNavGraph() çağrılıyor. Bottom Navigation'daki her sekme,
 popUpTo + launchSingleTop + restoreState üçlüsüyle (Google'ın ÖNERDİĞİ
 standart kalıp) geçiş yapıyor - bu sayede geri tuşu sekme sekme değil
 doğrudan uygulamadan çıkıyor, aynı sekmeye tekrar basmak ekran yığmıyor,
 her sekme kaldığı yeri hatırlıyor.

 Bu, tüm katmanların (Retrofit/Room -> Repository -> Koin -> ViewModel ->
 Compose -> Navigation) bir araya gelip ÇALIŞTIĞI nokta.

 12) TAMAMLANAN ÖZELLİKLER (proje dokümanındaki TÜM maddeler)
 ----------------------------------------------------------------
     Sayfalama + Infinite scroll
     Arama çubuğu + 300ms debounce
     Shimmer loading efekti (kayan parlaklık huzmeli gri iskelet kartlar)
     Hata yönetimi (404/429/500/internet yok, ayrı ayrı mesajlarla) +
       Tekrar Dene butonu (kaldığı sayfadan devam eder)
     Detay ekranı (büyük resim, durum, tür, cinsiyet, köken, bölüm sayısı)
     Fade-in/slide-in animasyonu (detay ekranı içeriğinin girişi)
     Resme tıklayınca tam ekran büyütme (scale animasyonuyla açılıp kapanan overlay)
     Kalp butonu scale/spring (büyüyüp küçülme) animasyonu
     Room veritabanı + gerçek favori ekleme/çıkarma (hem listede hem detayda)
     Favoriler ekranı + Bottom Navigation (Liste/Favoriler sekmeleri)

 13) ÖĞRENDİĞİMİZ ÖNEMLİ HATA/DERS NOTLARI
 ----------------------------------------------
    - LazyColumn öğelerine "key" vermemek, kaydırırken içeriklerin
      birbirine karışmasına yol açabiliyor.
    - Bir ekranı BİRDEN FAZLA yerden çağırmak (MainActivity + NavGraph gibi)
      çift-render'a, üst üste binmiş görüntüye yol açıyor.
    - Coroutine iptali (CancellationException), catch (e: Exception)
      bloğunda YANLIŞLIKLA gerçek hata gibi ele alınabiliyor - "throw e"
      ile bunu SPESIFIK olarak tekrar fırlatmak gerekiyor.
    - Bir coroutine'i loadJob.cancel() ile iptal etmek, o coroutine'in
      isLoading/isLoadingMore gibi state alanlarını KENDİ BAŞINA false
      yapmasını ENGELLİYOR - iptal eden tarafın bunu ELLE sıfırlaması gerekiyor.
    - AnimatedVisibility, bazı Compose sürümlerinde ColumnScope'a özel bir
      versiyonla KARIŞABİLİYOR - tam paket yolunu (androidx.compose.animation.
      AnimatedVisibility) yazmak bu belirsizliği çözüyor.
====================================================================
*/