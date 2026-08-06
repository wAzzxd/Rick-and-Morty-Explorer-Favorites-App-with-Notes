package com.example.rickandmortyproject.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ============ GENEL YERLEŞİM HARİTASI (bu ekranın içi böyle dizilecek) ============
//   ┌──────────────────────────────────────────────┐
//   │  [← Geri]                                    │  <- üstteki Row (geri butonu)
//   │                                              │
//   │              [Büyük Resim] <- tıklanabilir   │
//   │           [İsim]  [Kalp]                     │
//   │           [● Durum]                          │
//   │           Tür: ...                           │
//   │           Cinsiyet: ...                      │
//   │           Köken: ...                         │
//   │           Bölüm Sayısı: ...                  │
//   └──────────────────────────────────────────────┘
// Bunu sağlayan YAPI: en dışta TEK bir "Column" var (fonksiyonun HEMEN
// içinde) - içine SIRAYLA "geri butonu Row'u" ve "Box (içerik)" ekleniyor,
// Column bunları YUKARIDAN AŞAĞI dizer. Kod ne SIRAYLA yazılmışsa, ekranda
// da o SIRAYLA (yukarıdan aşağı) görünür. Resme tıklanınca, TÜM bunların
// ÜSTÜNE (en üst katmanda) tam ekran bir resim overlay'i AÇILACAK - bunu
// en sonda, ayrı bir AnimatedVisibility bloğunda göreceğiz.
@Composable
fun CharacterDetailScreen(
    // "onBackClick" -> geri tuşuna/oka basılınca ne olacağını DIŞARIDAN
    // alıyoruz (state hoisting, CharacterCard'daki onCardClick gibi). Bu
    // ekran, navController'ı KENDİSİ bilmiyor, sadece "geri gidilmesi
    // gerekiyor" bilgisini YUKARI (NavGraph'a) fırlatıyor.
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // "characterId" -> NavGraph'tan buraya, hangi karakterin detayının
    // gösterileceği bilgisini taşıyan parametre.
    characterId: Int,
    // "koinViewModel { parametersOf(characterId) }" -> bu ViewModel'in
    // constructor'ı hem "characterId: Int" hem "repository" istiyordu.
    // "parametersOf(characterId)" ile Koin'e "characterId İÇİN bu değeri
    // kullan, repository'yi ise ZATEN bildiğin gibi (get() ile) kendin
    // bul" diyoruz.
    viewModel: CharacterDetailViewModel = koinViewModel { parametersOf(characterId) }
) {
    val state by viewModel.state.collectAsState()

    // ============ YENİ EKLENEN: tam ekran resim durumu ============
    //
    // "remember { mutableStateOf(false) }" -> "resim şu an BÜYÜTÜLMÜŞ MÜ"
    // bilgisini tutan basit bir state. "var ... by remember" yazımı,
    // "isImageExpanded.value = true" yerine DOĞRUDAN "isImageExpanded = true"
    // yazabilmemizi sağlayan bir Kotlin kısayolu (delegation).
    // Compose, bu değer DEĞİŞTİĞİNDE ekranı OTOMATİK yeniden çizecek.
    var isImageExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // GÖRSEL ETKİSİ: bu Row, Column'ın İLK elemanı olduğu için, ekranın
        // EN ÜSTÜNDEKİ "geri" satırını (sol üstteki ok butonu) ÇİZEN kod
        // TAM OLARAK bu Row. Üstte basit bir "geri" satırı - gerçek bir
        // TopAppBar da kullanılabilirdi ama şimdilik sade tutuyoruz.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GÖRSEL ETKİSİ: sol üstteki OK ikonunu ÇİZEN kod bu IconButton.
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
            }
        }

        // GÖRSEL ETKİSİ: bu Box, Column'ın İKİNCİ (ve son) elemanı - "geri"
        // satırının HEMEN ALTINDAKİ, ekranın GERİ KALAN TÜM alanını kaplayan
        // bölge. İçindeki "when" bloğu, o bölgede TAM OLARAK NEYİN
        // görüneceğine (yükleniyor mu, hata mı, detaylar mı) karar veriyor.
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    // GÖRSEL ETKİSİ: yükleniyor durumunda, ekranın TAM
                    // ORTASINDA görünen dönen çarkı ÇİZEN kod bu satır.
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null -> {
                    // GÖRSEL ETKİSİ: hata durumunda, ORTADA görünen hata
                    // yazısı + "Tekrar Dene" butonunu ÇİZEN kod bu Column.
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        Text(text = "Hata: ${state.error}")
                        Button(onClick = { viewModel.retry() }) {
                            Text("Tekrar Dene")
                        }
                    }
                }

                // "state.character != null" -> "smart cast" denen bir Kotlin
                // özelliği devreye giriyor: bu koşulun İÇİNDE, Kotlin artık
                // "state.character"ın KESİNLİKLE null OLMADIĞINI biliyor, bu
                // yüzden aşağıda tekrar "?" ya da "!!" kullanmamıza GEREK
                // KALMIYOR - derleyici bunu otomatik güvenli kabul ediyor.
                state.character != null -> {
                    val character = state.character!!

                    // ============ Fade-in / Slide-in animasyonu ============
                    //
                    // "AnimatedVisibility" -> içine koyduğun İÇERİĞİN, EKRANA
                    // GİRİŞ/ÇIKIŞ anında animasyonlu görünmesini sağlayan bir
                    // Composable. "visible = true" verdiğimiz İÇİN, bu
                    // Composable İLK KEZ çizildiği anda (yani karakter verisi
                    // GELDİĞİ anda) GİRİŞ animasyonunu OTOMATİK oynatır.
                    //
                    // GÖRSEL ETKİSİ: BU SATIR, detay ekranındaki TÜM içeriğin
                    // (resim, isim, kalp, durum, detaylar) EKRANA "belirerek
                    // ve aşağıdan yukarı kayarak" GİRMESİNİ sağlayan kod.
                    // Bunu SİLERSEK, içerik direkt, animasyonsuz, ANİDEN
                    // belirir.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        // "enter = fadeIn(...) + slideInVertically(...)" ->
                        // İKİ animasyonu "+" ile BİRLEŞTİRİYORUZ: hem
                        // SAYDAMLIK (fadeIn - şeffaftan opaka) hem KONUM
                        // (slideInVertically - aşağıdan yukarı kayma) AYNI
                        // ANDA oynuyor. Compose'da "+" ile animasyonları
                        // ÜST ÜSTE BİNDİRMEK çok yaygın bir kalıptır.
                        //
                        // NOT: "durationMillis = 400" ve "fullHeight / 4"
                        // değerleri, bunun NASIL bir "sürat/mesafe" ile
                        // oynayacağını belirliyor.
                        enter = fadeIn(animationSpec = tween(durationMillis = 400)) +
                                slideInVertically(
                                    // "initialOffsetY = { fullHeight -> fullHeight / 4 }" ->
                                    // animasyon BAŞLARKEN, içeriğin GERÇEK
                                    // konumundan NE KADAR AŞAĞIDA olacağını
                                    // belirliyoruz - "fullHeight / 4" demek,
                                    // "kendi yüksekliğinin 4'te 1'i kadar
                                    // AŞAĞIDAN başla, sonra YUKARI kayarak
                                    // GERÇEK konumuna yerleş" demek.
                                    initialOffsetY = { fullHeight -> fullHeight / 2 },
                                    animationSpec = tween(durationMillis = 1000)

                                    //??????


                                )
                    ) {
                        // GÖRSEL ETKİSİ: bu Column'ın İÇİNDEKİ HER ŞEY (resim,
                        // isim+kalp satırı, durum satırı, detay satırları),
                        // AnimatedVisibility'nin İÇİNDE olduğu için YUKARIDAKİ
                        // fade+slide animasyonuyla BİRLİKTE ekrana girer.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // GÖRSEL ETKİSİ: Column'ın İLK elemanı = ekranın
                            // EN ÜSTÜNDE, ORTALANMIŞ şekilde duran BÜYÜK
                            // yuvarlak profil resmi.
                            //
                            // YENİ EKLENEN: ".clickable { isImageExpanded = true }"
                            // - resme dokununca, biraz önce tanımladığımız
                            // state'i true yapıyoruz, bu da EN ALTTAKİ
                            // "AnimatedVisibility(visible = isImageExpanded, ...)"
                            // bloğunun devreye girmesini (tam ekran overlay'in
                            // AÇILMASINI) tetikliyor.
                            AsyncImage(
                                model = character.imageUrl,
                                contentDescription = character.name,
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(CircleShape)
                                    // "align(Alignment.CenterHorizontally)" ->
                                    // bu Modifier, SADECE Column İÇİNDE
                                    // çalışır - "beni YATAYDA ORTALA" demek.
                                    // GÖRSEL ETKİSİ: resmin ekranın SOLUNA/
                                    // SAĞINA YASLANMAYIP TAM ORTADA durmasını
                                    // SAĞLAYAN satır TAM OLARAK bu.
                                    .align(Alignment.CenterHorizontally)
                                    .clickable { isImageExpanded = true }
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            // GÖRSEL ETKİSİ: resim ile altındaki isim satırı
                            // ARASINDAKİ dikey boşluğu (16dp) belirleyen satır.

                            // GÖRSEL ETKİSİ: Column'ın İKİNCİ elemanı = resmin
                            // HEMEN ALTINDAKİ "İsim + Kalp" satırı.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                // "horizontalArrangement = Arrangement.Center" ->
                                // bu Row'un İÇİNDEKİ elemanları (isim, kalp),
                                // Row'un TÜM genişliği boyunca DEĞİL, sadece
                                // ORTADA, birbirine YAKIN şekilde grupla.
                                // GÖRSEL ETKİSİ: isim + kalbin ekranın TAM
                                // ORTASINDA, YAN YANA durmasını SAĞLAYAN satır.
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // GÖRSEL ETKİSİ: bu Row'un İLK elemanı = büyük
                                // punto ile yazılan KARAKTER İSMİ.
                                Text(
                                    text = character.name,
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // GÖRSEL ETKİSİ: bu Row'un İKİNCİ (ve son)
                                // elemanı = ismin HEMEN SAĞINDAKİ kalp butonu.
                                IconButton(onClick = { viewModel.onFavoriteClick() }) {
                                    Icon(
                                        imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Favorilere ekle",
                                        tint = if (state.isFavorite) Color.Red else Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            // GÖRSEL ETKİSİ: isim satırı ile altındaki durum
                            // satırı ARASINDAKİ (24dp'lik, biraz daha büyük)
                            // boşluğu belirleyen satır.

                            // GÖRSEL ETKİSİ: Column'ın ÜÇÜNCÜ elemanı = "İsim +
                            // Kalp" satırının ALTINDAKİ "● Alive" gibi durum
                            // satırı - liste kartındaki AYNI renkli nokta mantığı.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // GÖRSEL ETKİSİ: bu satırın SOLUNDAKİ renkli
                                // (yeşil/kırmızı/gri) NOKTAYI ÇİZEN Box bu.
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (character.status) {
                                                "Alive" -> Color(0xFF4CAF50)
                                                "Dead" -> Color(0xFFF44336)
                                                else -> Color(0xFF9E9E9E)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // GÖRSEL ETKİSİ: noktanın SAĞINDAKİ "Alive"
                                // gibi durum yazısını ÇİZEN kod.
                                Text(text = character.status)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // GÖRSEL ETKİSİ: Column'ın SON 4 elemanı - durum
                            // satırının ALTINDA, YUKARIDAN AŞAĞI SIRAYLA duran
                            // "Tür: ...", "Cinsiyet: ...", "Köken: ...",
                            // "Bölüm Sayısı: ..." satırları. Bu 4 çağrının
                            // EKRANDAKİ DİKEY SIRASI, kodda YAZILDIKLARI SIRAYLA
                            // BİREBİR AYNI.
                            DetailRow(label = "Tür", value = character.species)
                            DetailRow(label = "Cinsiyet", value = character.gender)
                            DetailRow(label = "Köken", value = character.origin)
                            DetailRow(label = "Bölüm Sayısı", value = character.episodeCount.toString())
                        }
                    }

                    // ============ YENİ EKLENEN: tam ekran resim overlay'i ============
                    //
                    // Bu blok, "when" bloğunun DIŞINDA değil, "state.character
                    // != null" durumunun İÇİNDE duruyor - çünkü resmi büyütmek
                    // için ZATEN elimizde bir "character" (dolayısıyla resim
                    // URL'si) olması LAZIM.
                    //
                    // "AnimatedVisibility(visible = isImageExpanded, ...)" ->
                    // BURADA, YUKARIDAKİ (detay ekranının kendisi) animasyondan
                    // FARKLI olarak, "visible" SABİT true DEĞİL, doğrudan
                    // "isImageExpanded" state'ine BAĞLI. Yani bu blok,
                    // isImageExpanded true OLDUĞUNDA görünür, false
                    // OLDUĞUNDA (kapatınca) KAYBOLUR - GERÇEK bir "göster/
                    // gizle" davranışı, önceki AnimatedVisibility'nin
                    // aksine (o SADECE "ilk göründüğünde animasyon oynat"
                    // amaçlıydı, kalıcı olarak açık kalıyordu).
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isImageExpanded,
                        // "scaleIn" -> içerik KÜÇÜK bir noktadan (varsayılan
                        // %0 boyuttan) BÜYÜYEREK belirir - "hafif büyütme
                        // animasyonu" istediğimiz TAM OLARAK bu efekt.
                        enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
                        // "scaleOut" -> KAPANIRKEN ise TERSİ: içerik
                        // küçülerek KAYBOLUR.
                        exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300))
                    ) {
                        // GÖRSEL ETKİSİ: bu Box, EKRANIN TAMAMINI (fillMaxSize)
                        // yarı saydam SİYAH bir renkle (background) kaplıyor -
                        // "arka planı karartıp öne resmi çıkarma" hissi
                        // buradan geliyor.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f))
                                // "clickable { isImageExpanded = false }" ->
                                // bu KARARTILMIŞ ALANIN HERHANGİ BİR YERİNE
                                // tıklanınca, state'i TEKRAR false yapıp
                                // overlay'i KAPATIYORUZ.
                                .clickable { isImageExpanded = false }
                        ) {
                            // GÖRSEL ETKİSİ: karartılmış zeminin TAM
                            // ORTASINDA, BÜYÜK boyutta gösterilen resim.
                            AsyncImage(
                                model = character.imageUrl,
                                contentDescription = character.name,
                                // "ContentScale.Fit" -> resmi, EKRANA
                                // SIĞACAK şekilde (kırpmadan, oranını
                                // BOZMADAN) ölçeklendiriyoruz.
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(22.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Etiket + değer satırlarını tekrar tekrar yazmamak için küçük, yeniden
// kullanılabilir bir yardımcı Composable.
// GÖRSEL ETKİSİ: bu fonksiyonun ÜRETTİĞİ HER Row, detay ekranındaki
// "Tür: Human" gibi TEK BİR satırı temsil ediyor - "label" solda kalın/
// normal punto, "value" onun HEMEN SAĞINDA.
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "$label: ",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Text(text = value)
    }
}

/*
 ==================== KAVRAMSAL NOTLAR - GENEL & FADE/SLIDE ====================

 1) "koinViewModel { parametersOf(characterId) }" TAM OLARAK NE YAPIYOR?
    Şu ana kadar "koinViewModel()" diye PARANTEZSİZ/boş çağırıyorduk çünkü
    o ViewModel'lerin (CharacterListViewModel, FavoritesViewModel) SADECE
    Koin'in ZATEN bildiği bağımlılıkları (repository gibi) vardı.
    CharacterDetailViewModel ise EK olarak "characterId: Int" istiyor - bu,
    Koin'in ÖNCEDEN bilemeyeceği, HER ekran açılışında FARKLI olabilecek
    bir değer (ilk karakter için 1, ikincisi için 2 gibi). "parametersOf(
    characterId)" ile bu DEĞERİ, ekran her açıldığında Koin'e ELLE
    SAĞLIYORUZ - Koin de bunu ViewModel'in constructor'ındaki İLK parametreye
    (characterId'ye) eşleştiriyor.

 2) "onBackClick: () -> Unit" NEDEN BU EKRANIN İÇİNDE navController.
    popBackStack() GİBİ BİR ŞEY DEĞİL DE DIŞARIDAN GELİYOR?
    Aynı state hoisting prensibi: CharacterCard'ın onCardClick'i nasıl
    "tıklandı" bilgisini yukarı fırlatıp KARARI dışarıya bırakıyorsa, bu
    ekran da "geri gidilmek istendi" bilgisini yukarı (NavGraph'a) fırlatıp,
    navController ile İLGİLİ TÜM SORUMLULUĞU orada bırakıyor.

 3) "AnimatedVisibility" (İLKİ) NEDEN "if (state.character != null)"
    BLOĞUNUN İÇİNDE, DIŞINDA DEĞİL?
    AnimatedVisibility'nin animasyonu, İÇİNE KOYDUĞUN İçeriğin İLK KEZ
    ÇİZİLDİĞİ ANDA tetiklenir. Eğer bunu "state.character != null" kontrolü
    OLMADAN, en dışa koysaydık, ekran AÇILDIĞI anda (henüz veri gelmeden,
    "character" hâlâ null iken) tetiklenmiş olurdu - biz ise TAM OLARAK
    "veri GELDİĞİ an" animasyonun oynamasını istiyoruz, bu yüzden bu bloğun
    İÇİNDE, character'ın DOLU olduğu ANDA yerleştirdik.

 4) "fadeIn(...) + slideInVertically(...)" NASIL "TOPLANIYOR"?
    Compose'da animasyon SPEC'leri (enter/exit tanımları) "+" operatörüyle
    BİRLEŞTİRİLEBİLİR - bu, "her iki animasyonu AYNI ANDA, ÜST ÜSTE BİNDİREREK
    oynat" demek. Yani içerik HEM saydamdan opağa geçiyor HEM DE aynı anda
    aşağıdan yukarı kayıyor - ikisi birlikte, çok daha "yumuşak/profesyonel"
    bir giriş hissi yaratıyor.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - TAM EKRAN RESİM ====================

 1) "remember { mutableStateOf(false) }" NEDEN "ViewModel'İN state'İ" DEĞİL
    DE BURADA, DOĞRUDAN Composable İÇİNDE TUTULUYOR?
    "resim büyütülmüş mü" bilgisi, SADECE bu ekranın GEÇİCİ, GÖRSEL bir
    durumu - hiçbir iş mantığı (network, veritabanı) içermiyor, ekran
    kapanınca ZATEN unutulması gereken bir bilgi. Bu tür SADECE-UI'a-ait,
    KALICI olması gerekmeyen state'ler için ViewModel'e taşımaya GEREK
    YOK - Composable'ın kendi "remember" hafızasında tutmak yeterli ve
    daha basit.

 2) İKİ FARKLI "AnimatedVisibility" VAR, ARALARINDAKİ FARK NE?
    - Birincisi (detay ekranının GENELİ için): "visible = true" SABİT,
      amacı sadece "İLK göründüğünde animasyon oynat", sonra hep açık kalır.
    - İkincisi (tam ekran resim için): "visible = isImageExpanded" DEĞİŞKEN,
      GERÇEK bir açma/kapama anahtarı gibi çalışıyor - state true/false
      arasında GİDİP GELDİKÇE, içerik de AÇILIP KAPANIYOR (giriş VE çıkış
      animasyonuyla).

 3) "scaleIn" / "scaleOut" NE YAPIYOR, KALP ANİMASYONUNDAKİ "scale"TEN
    FARKI NE?
    Kalp animasyonunda Animatable + LaunchedEffect ile ELLE bir "büyüyüp
    küçülme" (1f -> 1.3f -> 1f) SIRALI hareketi kurmuştuk. Burada ise
    "scaleIn"/"scaleOut", AnimatedVisibility'nin HAZIR sunduğu, "içerik
    GÖRÜNÜRKEN küçükten büyüğe, KAYBOLURKEN büyükten küçüğe" giden HAZIR
    bir animasyon türü - kendimiz elle Animatable kurmamıza GEREK KALMADI,
    çünkü bu, "görünme/kaybolma" senaryosu için Compose'un ZATEN sunduğu
    bir kısayol.

 4) "Color.Black.copy(alpha = 0.9f)" NE DEMEK?
    "copy" -> bir Color nesnesini, SADECE BELİRTTİĞİMİZ özelliği (burada
    alpha, yani SAYDAMLIK) değiştirerek YENİDEN üretiyoruz - renk siyah
    KALIYOR, sadece %90 OPAK (yani hafif YARI SAYDAM, arkası biraz hissedilir)
    hale getiriyoruz. "alpha = 1f" tamamen OPAK (arkası hiç görünmez),
    "alpha = 0f" tamamen ŞEFFAF (hiç görünmez) olurdu.

 5) BU DOSYA HANGİ DOSYALARLA BAĞLANTILI (GÜNCEL)?
    - presentation/detail/CharacterDetailViewModel.kt VE
      CharacterDetailState.kt -> DEĞİŞMEDİ, aynı ilişki devam ediyor.
    - domain/model/Character.kt -> character.imageUrl, tam ekran resimde
      de KULLANILIYOR (aynı URL, sadece daha büyük gösteriliyor).
    - presentation/navigation/NavGraph.kt -> bu ekranı bir route'a
      bağlarken, "characterId" parametresini route'tan ÇEKİYOR ve
      "onBackClick"e navController.popBackStack()'i VERİYOR.
    - presentation/list/CharacterListScreen.kt -> liste ekranındaki
      onCardClick, bu ekranın route'una GEÇİŞ yapacak şekilde bağlı.
 ===========================================================
*/