package com.example.rickandmortyproject.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

// Bu, listenin GÖRÜNDÜĞÜ asıl ekranı tarif eden Composable - MVVM'deki
// "View" katmanı burası. Bu fonksiyonun İÇİNDE hiçbir network/veritabanı
// kodu YOK, sadece ViewModel'in state'ine BAKIP ekranı çiziyor.
@Composable
fun CharacterListScreen(
    // "modifier: Modifier = Modifier" -> bu ekranı çağıran yerin (AppNavGraph'ın),
    // boyut/padding gibi şeyleri dışarıdan kontrol edebilmesi için.
    modifier: Modifier = Modifier,

    // YENİ EKLENEN: bir karta tıklandığında, hangi karakterin id'sine
    // tıklandığını YUKARI (NavGraph'a) fırlatan fonksiyon. Bu ekran,
    // navController'ı hiç TANIMIYOR - sadece "şu id'ye tıklandı" bilgisini
    // dışarı veriyor, NAVİGASYON KARARINI (nereye gidileceğini) NavGraph
    // veriyor. Bu da state hoisting'in AYNI prensibi: karar verme yetkisini
    // ÜST katmana bırakıyoruz.
    onCharacterClick: (Int) -> Unit = {},

    // "koinViewModel()" -> Koin'e "bana bir CharacterListViewModel ver" diyoruz.
    // Koin, AppModule.kt'de yazdığımız tarife göre bunu otomatik oluşturup verir.
    viewModel: CharacterListViewModel = koinViewModel()
) {
    // "collectAsState()" -> ViewModel'deki StateFlow'u Compose'un İZLEYEBİLECEĞİ
    // bir yapıya çeviriyor. State her değiştiğinde ekran OTOMATİK yeniden çizilir.
    val state by viewModel.state.collectAsState()

    // "rememberLazyListState()" -> LazyColumn'un KAYDIRMA DURUMUNU (hangi
    // öğe görünüyor, ne kadar kaydırılmış vb.) tutan bir nesne oluşturuyoruz.
    // "remember" sayesinde bu nesne, recomposition'lar arasında HEP AYNI
    // KALIR, her ekran yenilendiğinde sıfırdan oluşmaz.
    val listState = rememberLazyListState()

    // "derivedStateOf { }" -> "kullanıcı sona yaklaştı mı" bilgisini HER
    // recomposition'da yeniden HESAPLAMAK yerine, sadece İÇİNDEKİ değerler
    // GERÇEKTEN değiştiğinde hesaplayan bir performans optimizasyonu.
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo
                .lastOrNull()?.index ?: 0
            val totalItemsCount = state.characters.size
            // Görünen son öğe, listenin sonuna 5 öğe kala bir yere geldiyse
            // -> true, yani "yeni sayfa yükleme zamanı geldi" demek.
            lastVisibleItemIndex >= totalItemsCount - 5 && totalItemsCount > 0
        }
    }

    // "LaunchedEffect(shouldLoadMore.value)" -> shouldLoadMore DEĞİŞTİĞİNDE
    // (false'tan true'ya geçtiğinde) içindeki bloğu BİR KEZ çalıştırır.
    // "&& state.error == null" -> zaten bir hata varsa (örn. 429), otomatik
    // tekrar istek atmayı DURDURUYORUZ - kullanıcı sadece "Tekrar Dene"
    // butonuna basınca yeni deneme tetiklenecek.
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && state.error == null) {
            viewModel.loadCharacters()
        }
    }

    // "Column" -> arama kutusu, chip'ler ve listeyi DİKEY olarak sıralıyoruz.
    Column(modifier = modifier.fillMaxSize()) {

        // "OutlinedTextField" -> Material Design'ın çerçeveli metin kutusu.
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { newQuery -> viewModel.onSearchQueryChanged(newQuery) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Karakter ara...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ara") },
            singleLine = true
        )

        // "LazyRow" -> LazyColumn'un YATAY versiyonu.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(listOf("Alive", "Dead", "unknown")) { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { viewModel.onStatusFilterChanged(status) },
                    label = { Text(status) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when {
                // DEĞİŞTİ: eskiden burada tek başına ortalanmış bir
                // CircularProgressIndicator vardı - artık YERİNE
                // ShimmerCharacterList() çağrılıyor. Bu fonksiyon,
                // ShimmerEffect.kt dosyasında yazdığımız, gerçek kartların
                // İSKELETİNİ taklit eden, gri/parlayan 6 tane sahte kart
                // gösteren bir LazyColumn. Kullanıcı "boş beyaz ekran +
                // tek bir dönen çark" yerine, "birazdan burada kartlar
                // olacak" hissi veren, daha modern bir yükleme deneyimi
                // görüyor.
                state.isLoading -> {
                    ShimmerCharacterList()
                }

                state.error != null && state.characters.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        Text(text = "Hata: ${state.error}")
                        Button(onClick = { viewModel.retry() }) {
                            Text("Tekrar Dene")
                        }
                    }
                }

                state.characters.isEmpty() && !state.isLoading -> {
                    Text(
                        text = "Sonuç bulunamadı",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        items(
                            items = state.characters,
                            key = { character -> character.id }
                        ) { character ->

                            CharacterCard(
                                character = character,
                                isFavorite = state.favoriteIds.contains(character.id),

                                // DEĞİŞTİ: artık boş lambda DEĞİL - tıklandığında
                                // dışarıdan gelen onCharacterClick'i, TIKLANAN
                                // karakterin id'siyle ÇAĞIRIYORUZ. NavGraph'taki
                                // "onCharacterClick = { characterId -> navController.
                                // navigate(...) }" tanımı sayesinde bu, GERÇEKTEN
                                // detay ekranına geçiş yapacak.
                                onCardClick = { onCharacterClick(character.id) },

                                onFavoriteClick = { viewModel.onFavoriteClick(character) }
                            )
                        }

                        // NOT: burada, sayfa sonu yüklenirken (isLoadingMore)
                        // shimmer KULLANMADIK, sade bir CircularProgressIndicator
                        // bıraktık. Sebep: shimmer, "büyük bir bölge YÜKLENİYOR"
                        // hissini vermek için güzel (ilk açılış gibi), ama
                        // "listenin altına küçük bir ekleme yapılıyor" durumunda
                        // küçük bir dönen çark zaten yeterli ve daha SADE -
                        // her yükleme durumunu shimmer yapmak GEREKMİYOR,
                        // duruma uygun olanı seçmek önemli.
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                when {
                                    state.isLoadingMore -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    state.error != null -> {
                                        Column(modifier = Modifier.align(Alignment.Center)) {
                                            Text(text = state.error ?: "")
                                            Button(onClick = { viewModel.retry() }) {
                                                Text("Tekrar Dene")
                                            }
                                        }
                                    }
                                    else -> { }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR - GENEL (MVVM & KEY) ====================

 1) BU DOSYA, MVVM'İN HANGİ KATMANI?
    Bu, "View" katmanı - SADECE state'e bakıp ekranı çiziyor, hiçbir iş mantığı
    (network isteği, hata yönetimi kararı, sayfalama hesaplaması, veritabanı
    işlemi, NAVİGASYON KARARI) İÇERMİYOR. Tüm o mantık ViewModel'de veya
    (navigasyon için) NavGraph'ta.

 2) "state.isLoading", "state.error", "state.characters", "state.favoriteIds"
    NEREDEN GELİYOR?
    Bunların hepsi CharacterListState data class'ının alanları. ViewModel
    _state.update { it.copy(...) } ile bunları günceller, biz burada sadece OKUYORUZ.

 3) "key" PARAMETRESİ NEDEN ÖNEMLİ?
    Listemiz SÜREKLİ BÜYÜYOR (infinite scroll ile yeni sayfalar ekleniyor) ve
    kullanıcı sürekli kaydırıyor. Bu durumda key vermek, Compose'un hangi
    kartın hangi veriye ait olduğunu KARIŞTIRMAMASI için gereklidir.

 4) "onCharacterClick: (Int) -> Unit = {}" NEDEN VARSAYILAN DEĞERİ BOŞ BİR
    LAMBDA ({})?
    Bu, bu ekranı ÇAĞIRAN her yerin MUTLAKA bir tıklama davranışı vermek
    ZORUNDA olmamasını sağlıyor - örneğin bu ekranı ileride bir ÖNİZLEME
    (Preview) fonksiyonunda test ederken, navigasyon parametresi VERMEDEN
    de kullanabiliriz, hiçbir şey çökmez, sadece tıklama HİÇBİR ŞEY yapmaz.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - INFINITE SCROLL ====================

 1) NEDEN "derivedStateOf" KULLANDIK?
    listState.layoutInfo, kullanıcı her piksel kaydırdığında DEĞİŞİYOR.
    derivedStateOf, "bu hesaplamanın SONUCU değişmediği sürece, tekrar tekrar
    recomposition tetikleme" diyerek performansı korur.

 2) NEDEN "LaunchedEffect" KULLANDIK?
    Composable fonksiyonlar Compose tarafından ÇOK SIK yeniden ÇAĞRILIR
    (recomposition). loadCharacters()'ı DOĞRUDAN Composable'ın gövdesinde
    çağırsaydık, her recomposition'da TEKRAR TEKRAR çağrılırdı. LaunchedEffect,
    "bu kod sadece BELİRLİ bir değer DEĞİŞTİĞİNDE çalışsın" garantisini veriyor.

 3) "5 ÖĞE KALA" NEDEN, TAM SONA GELİNCE DEĞİL?
    Kullanıcı gerçekten sona ulaştığında yeni veri ÇOKTAN gelmiş/gelmekte
    oluyor, kaydırma kesintisiz hissettiriyor.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - ARAMA & FİLTRE UI ====================

 1) "value = state.searchQuery" NEDEN TextField'IN KENDİ İÇ STATE'İ DEĞİL DE
    DIŞARIDAN (ViewModel'den) GELİYOR?
    Buna "state hoisting" denir. TextField'ın kendi başına bir hafızası
    OLMASAYDI, ViewModel'deki state TEK GERÇEK KAYNAK olurdu.

 2) FilterChip'İN "selected" PARAMETRESİ NASIL ÇALIŞIYOR?
    state.statusFilter, ya null ya da "Alive"/"Dead"/"unknown" string'lerinden
    biri. Her chip kendi status değerini bu alanla KARŞILAŞTIRIYOR.

 3) "Sonuç bulunamadı" MESAJI NE ZAMAN ÇIKIYOR?
    state.characters boşken VE isLoading false iken.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - FAVORİLER (BU EKRANDAKİ KISIM) ====================

 1) "isFavorite = state.favoriteIds.contains(character.id)" NEDEN HER
    RECOMPOSITION'DA YENİDEN HESAPLANIYOR, SORUN OLMUYOR MU?
    Set'te "contains" kontrolü ÇOK HIZLI çalıştığı için performans sorunu
    YARATMIYOR - yüzlerce karakter olsa bile.

 2) KALP BUTONUNA BASINCA EKRANDA NE OLUYOR, ADIM ADIM?
    a) Kullanıcı kalbe basar -> onFavoriteClick lambda'sı çalışır
    b) viewModel.onFavoriteClick(character) çağrılır
    c) ViewModel, Room'a ekleme/çıkarma komutu verir
    d) Room değişir -> repository.getFavorites() Flow'u YENİ bir liste yayınlar
    e) ViewModel'deki init{} bloğundaki izleyici bunu yakalar, state.favoriteIds
       GÜNCELLENİR
    f) state değiştiği için ekran OTOMATİK yeniden çizilir, kalp ikonu
       YENİ duruma göre güncellenir
    Bu ZİNCİRİN HİÇBİR YERİNDE biz elle "ekranı yenile" DEMEDİK.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - NAVİGASYON (BU EKRANDAKİ KISIM) ====================

 1) KARTA TIKLANINCA EKRANDA NE OLUYOR, ADIM ADIM?
    a) Kullanıcı karta basar -> CharacterCard'daki "clickable" tetiklenir
    b) onCardClick lambda'sı çalışır -> "onCharacterClick(character.id)" çağrılır
    c) Bu fonksiyon, NavGraph.kt'de CharacterListScreen çağrılırken VERİLEN
       lambda'ya bağlı: "onCharacterClick = { characterId -> navController.
       navigate(Screen.CharacterDetail.createRoute(characterId)) }"
    d) navController, "character_detail/5" gibi bir route'a GEÇİŞ yapar
    e) NavHost, bu route'u tanıyıp CharacterDetailScreen'i, doğru
       characterId ile ÇİZER
    Bu ekran (CharacterListScreen), navController'ın VARLIĞINDAN bile
    HABERSİZ - sadece "id'si X olan bir karaktere tıklandı" diyor, GERİ
    KALAN her şeyi üst katman (NavGraph) hallediyor.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - SHIMMER (YENİ EKLENEN) ====================

 1) "ShimmerCharacterList()" NEREDEN GELİYOR, BU DOSYADA IMPORT YOK NEDEN?
    ShimmerCharacterList(), AYNI PAKETTE (presentation.list) duran
    ShimmerEffect.kt dosyasında tanımlı bir @Composable fonksiyon. Kotlin'de,
    AYNI PAKETTEKİ dosyalar birbirini import ETMEDEN, DOĞRUDAN kullanabilir -
    bu yüzden burada ayrıca "import ...ShimmerCharacterList" satırına GEREK
    YOK (CharacterCard'ı da hiç import etmeden kullandığımızı fark etmiş
    olabilirsin, AYNI sebep).

 2) BU DOSYA HANGİ DOSYALARLA BAĞLANTILI (GÜNCEL)?
    - presentation/list/CharacterListViewModel.kt VE CharacterListState.kt ->
      state'i buradan okuyoruz, DEĞİŞMEDİ.
    - presentation/list/CharacterCard.kt -> gerçek liste elemanlarını
      ÇİZERKEN kullanılıyor, DEĞİŞMEDİ.
    - presentation/list/ShimmerEffect.kt -> YENİ BAĞLANTI: state.isLoading
      true iken, ShimmerCharacterList() BURADAN çağrılıyor. ShimmerEffect.kt
      içindeki Modifier.shimmerEffect() extension'ı da, ShimmerCharacterCard
      İÇİNDE kullanılıyor - yani zincir şöyle: CharacterListScreen (bu
      dosya) -> ShimmerCharacterList() -> ShimmerCharacterCard() ->
      Modifier.shimmerEffect().
    - presentation/navigation/NavGraph.kt -> "onCharacterClick" parametresini
      BURAYA veriyor.
 ===========================================================
*/