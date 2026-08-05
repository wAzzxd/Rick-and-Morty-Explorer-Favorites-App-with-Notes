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
            // "value = state.searchQuery" -> kutunun İÇİNDEKİ metin, DOĞRUDAN
            // state'ten okunuyor - yani ViewModel'deki değer TEK GERÇEK KAYNAK
            // (single source of truth), TextField kendi başına bir metin
            // TUTMUYOR, sadece state'i YANSITIYOR.
            value = state.searchQuery,
            // "onValueChange" -> kullanıcı her HARF yazdığında/sildiğinde
            // tetiklenir, ViewModel'e "işte yeni metin" diye haber veriyoruz.
            // (ViewModel bu metni hem ANINDA state'e yazıyordu, hem de
            // debounce'lu searchQueryFlow üzerinden GERÇEK aramayı tetikliyordu.)
            onValueChange = { newQuery -> viewModel.onSearchQueryChanged(newQuery) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Karakter ara...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ara") },
            singleLine = true // metin kutusunun TEK SATIR kalmasını, Enter'a basınca alt satıra geçmemesini sağlıyor
        )

        // "LazyRow" -> LazyColumn'un YATAY versiyonu. Chip sayısı ekrana
        // sığmasa bile YANA doğru kaydırılabilir, performanslı bir liste.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            // Göstereceğimiz 3 durum: Alive, Dead, unknown. Bunu bir liste
            // olarak tanımlayıp items() ile TEK TEK chip'e çeviriyoruz -
            // aynı kodu 3 KERE elle yazmak yerine.
            items(listOf("Alive", "Dead", "unknown")) { status ->
                // "FilterChip" -> Material'ın "seçilebilir etiket" bileşeni,
                // seçiliyken farklı renkte/ikonlu görünür.
                FilterChip(
                    // "selected" -> bu chip'in ŞU AN seçili olup olmadığını
                    // state.statusFilter ile KARŞILAŞTIRARAK belirliyoruz.
                    selected = state.statusFilter == status,
                    // Tıklanınca ViewModel'e "bu durumu seç/kaldır" diyoruz
                    // (ViewModel'deki onStatusFilterChanged zaten TOGGLE
                    // mantığını içeriyordu - aynı chip'e tekrar basılırsa kaldırır).
                    onClick = { viewModel.onStatusFilterChanged(status) },
                    label = { Text(status) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // "Box(modifier = Modifier.weight(1f)...)" -> ".weight(1f)" SADECE
        // Column/Row içinde çalışan bir modifier: "kalan TÜM boşluğu bu Box
        // doldursun" demek. Arama kutusu ve chip'ler kendi doğal boyutlarını
        // alırken, listenin geri kalan TÜM alanı kaplamasını sağlıyor.
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {

            // "when { }" -> state'in içindeki değerlere göre HANGİ UI'ın
            // çizileceğine karar veriyoruz. Sıralama ÖNEMLİ: Kotlin, YUKARIDAN
            // AŞAĞI kontrol eder, İLK eşleşen koşulu çalıştırır.
            when {
                // Durum 1: İlk yükleme sürüyor (henüz hiç karakter yok).
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // Durum 2: Bir hata var VE elimizde HİÇ karakter yok (yani bu
                // İLK yükleme sırasında oluşan bir hata). Bu durumda TAM EKRAN
                // hata + Tekrar Dene butonu gösteriyoruz, çünkü gösterecek
                // başka hiçbir şey yok zaten.
                state.error != null && state.characters.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        Text(text = "Hata: ${state.error}")
                        Button(onClick = { viewModel.retry() }) {
                            Text("Tekrar Dene")
                        }
                    }
                }

                // Durum 3: Arama/filtre SONUCUNDA hiç karakter bulunamadıysa
                // (liste boş, ama hata da yok, isLoading da false) -> kullanıcıya
                // "sonuç bulunamadı" diye bir mesaj gösterelim, boş beyaz ekran
                // YERİNE. Bu durum, ViewModel'in 404'ü "hata" değil "boş sonuç"
                // olarak ele almasıyla (errorMessage = null, endReached = true)
                // mümkün oluyor.
                state.characters.isEmpty() && !state.isLoading -> {
                    Text(
                        text = "Sonuç bulunamadı",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Durum 4 (else): Elimizde EN AZ bir karakter var, listeyi çiziyoruz.
                else -> {
                    // "state = listState" -> LazyColumn'a, az önce oluşturduğumuz
                    // kaydırma durumunu VERİYORUZ. Bu bağlantı sayesinde
                    // listState.layoutInfo yukarıdaki hesaplamada GÜNCEL kalıyor.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        // "items(...)" -> listedeki HER BİR Character için bir kart üretiyor.
                        //
                        // "key = { character -> character.id }" -> LazyColumn, ekrandan
                        // çıkan kartları YOK ETMİYOR, YENİDEN KULLANIYOR (recycle). Bu key
                        // sayesinde Compose, hangi kartın hangi karaktere ait olduğunu HER
                        // ZAMAN doğru takip ediyor - kaydırırken/yeni sayfa eklenirken
                        // kartların içeriğinin birbirine karışmasını önlüyor.
                        items(
                            items = state.characters,
                            key = { character -> character.id }
                        ) { character ->

                            // Ayrı bir dosyada (CharacterCard.kt) yazdığımız kart
                            // tasarımını burada ÇAĞIRIYORUZ.
                            CharacterCard(
                                character = character,

                                // Artık SABİT false DEĞİL - state.favoriteIds kümesinde
                                // bu karakterin id'si VAR MI diye GERÇEKTEN kontrol
                                // ediyoruz. Bu küme, ViewModel'de Room'u izleyerek
                                // CANLI tutuluyordu - bu yüzden favoriye ekleyip/
                                // çıkardığında kalp ikonu ANINDA (Room güncellenir
                                // güncellenmez) doğru görünecek.
                                isFavorite = state.favoriteIds.contains(character.id),

                                // Detay ekranını yazınca burada navigasyon kodu olacak
                                // (navController.navigate(...) gibi).
                                onCardClick = { },

                                // Kalbe tıklanınca ViewModel'deki toggle fonksiyonunu
                                // çağırıyoruz. ViewModel zaten "favoride mi değil mi"
                                // kontrolünü kendi içinde yapıp Room'a ekleme/çıkarma
                                // komutunu veriyordu - biz burada sadece "TIKLANDI"
                                // bilgisini YUKARI (ViewModel'e) fırlatıyoruz, kararı
                                // ViewModel veriyor (state hoisting prensibi).
                                onFavoriteClick = { viewModel.onFavoriteClick(character) }
                            )
                        }

                        // "item { }" -> LazyColumn'a, listedeki TÜM karakterlerden
                        // SONRA, EN ALTA TEK BİR öğe daha ekliyoruz. Bu satır üç
                        // farklı duruma göre üç farklı şey gösterir: sayfa sonu
                        // yükleniyor, sayfa sonu hata verdi, ya da hiçbiri.
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                when {
                                    // Sayfa sonu yükleniyor -> küçük dönen çark.
                                    state.isLoadingMore -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    // Sayfa sonu YÜKLENİRKEN hata oluştu (örn. 429) ->
                                    // KÜÇÜK bir hata mesajı + Tekrar Dene butonu,
                                    // ÜSTTEKİ liste hiç kaybolmadan. Kullanıcı butona
                                    // bastığında viewModel.retry() çağrılır, o da
                                    // currentPage'i SIFIRLAMADAN kaldığı sayfadan devam eder.
                                    state.error != null -> {
                                        Column(modifier = Modifier.align(Alignment.Center)) {
                                            Text(text = state.error ?: "")
                                            Button(onClick = { viewModel.retry() }) {
                                                Text("Tekrar Dene")
                                            }
                                        }
                                    }
                                    // Hiçbir şey yükleniyor/hatalı değilse -> boş bırak,
                                    // gösterecek bir şey yok.
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
    işlemi) İÇERMİYOR. Tüm o mantık CharacterListViewModel içinde.

 2) "state.isLoading", "state.error", "state.characters", "state.favoriteIds"
    NEREDEN GELİYOR?
    Bunların hepsi CharacterListState data class'ının alanları. ViewModel
    _state.update { it.copy(...) } ile bunları günceller, biz burada sadece OKUYORUZ.

 3) "key" PARAMETRESİ NEDEN ÖNEMLİ?
    Listemiz SÜREKLİ BÜYÜYOR (infinite scroll ile yeni sayfalar ekleniyor) ve
    kullanıcı sürekli kaydırıyor. Bu durumda key vermek, Compose'un hangi
    kartın hangi veriye ait olduğunu KARIŞTIRMAMASI için gereklidir.
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
    Buna "state hoisting" denir - Compose'un temel prensiplerinden biri.
    TextField'ın kendi başına bir hafızası OLMASAYDI (yani sadece dışarıdan
    verilen değeri gösterseydi), ViewModel'deki state TEK GERÇEK KAYNAK
    olurdu. Bu sayede ekran döndürülse, süreç yeniden başlasa bile ViewModel
    hayatta kaldığı sürece arama metni KAYBOLMAZ.

 2) FilterChip'İN "selected" PARAMETRESİ NASIL ÇALIŞIYOR?
    state.statusFilter, ya null (hiçbir filtre seçili değil) ya da "Alive"/
    "Dead"/"unknown" string'lerinden biri. Her chip kendi status değerini
    bu alanla KARŞILAŞTIRIYOR - eşleşiyorsa "selected = true" olup görsel
    olarak vurgulanıyor.

 3) "Sonuç bulunamadı" MESAJI NE ZAMAN ÇIKIYOR?
    state.characters boşken VE isLoading false iken. Bu durum hem "arama
    sonucu hiçbir şey bulunamadı" (API 404 döndürdü, ViewModel bunu hata
    değil boş sonuç olarak işledi) hem de teorik olarak "henüz hiç veri
    gelmemiş ama yüklenmiyor da" durumlarını kapsıyor.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - FAVORİLER (BU EKRANDAKİ KISIM) ====================

 1) "isFavorite = state.favoriteIds.contains(character.id)" NEDEN HER
    RECOMPOSITION'DA YENİDEN HESAPLANIYOR, SORUN OLMUYOR MU?
    Set'te "contains" kontrolü ÇOK HIZLI çalıştığı için (Set<Int>'in temel
    özelliği), listedeki her karakter için bu kontrolü yapmak performans
    sorunu YARATMIYOR - yüzlerce karakter olsa bile.

 2) KALP BUTONUNA BASINCA EKRANDA NE OLUYOR, ADIM ADIM?
    a) Kullanıcı kalbe basar -> onFavoriteClick lambda'sı çalışır
    b) viewModel.onFavoriteClick(character) çağrılır
    c) ViewModel, Room'a ekleme/çıkarma komutu verir
    d) Room değişir -> repository.getFavorites() Flow'u YENİ bir liste yayınlar
    e) ViewModel'deki init{} bloğundaki izleyici bunu yakalar, state.favoriteIds
       GÜNCELLENİR
    f) state değiştiği için CharacterListScreen OTOMATİK yeniden çizilir
       (recomposition), kalp ikonu YENİ duruma göre güncellenir
    Bu ZİNCİRİN HİÇBİR YERİNDE biz elle "ekranı yenile" DEMEDİK - hepsi
    reaktif (Flow tabanlı) veri akışı sayesinde KENDİLİĞİNDEN oluyor.
 ===========================================================
*/