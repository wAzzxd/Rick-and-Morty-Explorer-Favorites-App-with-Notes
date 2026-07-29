package com.example.rickandmortyproject.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel

// Bu, listenin GÖRÜNDÜĞÜ asıl ekranı tarif eden Composable - MVVM'deki
// "View" katmanı burası. Bu fonksiyonun İÇİNDE hiçbir network/veritabanı
// kodu YOK, sadece ViewModel'in state'ine BAKIP ekranı çiziyor.
@Composable
fun CharacterListScreen(
    // "modifier: Modifier = Modifier" -> bu ekranı çağıran yerin (MainActivity'nin),
    // boyut/padding gibi şeyleri dışarıdan kontrol edebilmesi için. Varsayılan
    // değeri boş bir Modifier, yani hiçbir şey vermezsek hiçbir kısıtlama olmaz.
    modifier: Modifier = Modifier,

    // "koinViewModel()" -> Koin'e "bana bir CharacterListViewModel ver" diyoruz.
    // Koin, AppModule.kt'de yazdığımız tarife göre bunu otomatik oluşturup verir;
    // repository'yi elle geçirmemize gerek kalmıyor.
    viewModel: CharacterListViewModel = koinViewModel()
) {
    // "collectAsState()" -> ViewModel'deki StateFlow'u Compose'un İZLEYEBİLECEĞİ
    // bir yapıya çeviriyor. "by" sayesinde "state.value.characters" yerine
    // direkt "state.characters" yazabiliyoruz.
    //
    // BURASI ÇOK ÖNEMLİ: ViewModel içinde "_state.update { ... }" her çağrıldığında
    // (örn. yeni sayfa geldiğinde, hata oluştuğunda), bu satır sayesinde Compose
    // OTOMATİK olarak bu fonksiyonu YENİDEN ÇALIŞTIRIR (buna "recomposition" denir).
    // Biz elle "ekranı yenile" demiyoruz, State değiştiği an ekran kendiliğinden güncellenir.
    val state by viewModel.state.collectAsState()

    // "Box" -> içindeki elemanları ÜST ÜSTE bindirebilen bir kap (container).
    // Burada aslında tek seferde SADECE BİR şey gösteriyoruz (ya loading, ya hata,
    // ya liste), Box kullanmamızın sebebi CircularProgressIndicator ve hata
    // metnini EKRANIN ORTASINA (Alignment.Center) hizalayabilmek.
    Box(modifier = modifier.fillMaxSize()) {

        // "when { }" -> Kotlin'in çoklu-durum kontrol yapısı (diğer dillerdeki
        // switch-case'e benzer, ama çok daha güçlü). Burada state'in içindeki
        // değerlere göre HANGİ UI'ın çizileceğine karar veriyoruz.
        when {
            // Durum 1: İlk yükleme sürüyor (henüz hiç veri yok) -> ortada dönen çark göster.
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Durum 2: Bir hata var (error null DEĞİLSE) -> hata mesajını göster.
            // İleride burayı "Yeniden Dene" butonlu gerçek bir hata ekranıyla
            // değiştireceğiz, şimdilik sade bir metin yeterli.
            state.error != null -> {
                Text(
                    text = "Hata: ${state.error}",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Durum 3 (else): Ne yükleniyor ne de hata var -> demek ki elimizde
            // gösterilecek karakter listesi var, listeyi çiziyoruz.
            else -> {
                // "LazyColumn" -> Compose'un dikey, PERFORMANSLI liste bileşeni.
                // "Lazy" kelimesi önemli: 800 küsür karakter olsa bile, aynı anda
                // sadece o an EKRANDA GÖRÜNEN kartlar hafızada tutulur/çizilir,
                // kullanıcı aşağı kaydırdıkça yenileri "tembelce" (lazy) oluşturulur.
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // "items(state.characters) { character -> ... }" -> listedeki
                    // HER BİR Character nesnesi için, aşağıdaki bloğu çalıştırıp
                    // bir satır (kart) üretiyoruz. "character", o an sırası gelen
                    // TEK BİR karakteri temsil ediyor (lambda parametresi).
                    items(state.characters) { character ->

                        // Önceki adımda ayrı bir dosyada (CharacterCard.kt) yazdığımız
                        // kart tasarımını burada ÇAĞIRIYORUZ. Kendi çizim mantığını
                        // (resim, isim, durum rengi, kalp butonu) o dosyada tarif etmiştik,
                        // burada sadece "bu karakter için bir kart çiz" diyoruz.
                        CharacterCard(
                            character = character,

                            // isFavorite'i ŞİMDİLİK SABİT false veriyoruz. Neden?
                            // Çünkü henüz Room veritabanını bağlamadık, dolayısıyla
                            // "bu karakter gerçekten favorilerde mi" bilgisini
                            // HİÇBİR YERDEN sorgulayamıyoruz. Bu, projeyi ADIM ADIM
                            // ilerletme stratejimizin bir parçası: önce "iskeleti"
                            // (UI'ın nasıl göründüğünü) çalışır hale getiriyoruz,
                            // gerçek favori mantığını bir sonraki adımda (Room'u
                            // bağlayınca) buraya ekleyeceğiz.
                            isFavorite = false,

                            // "onCardClick = { }" -> boş bir lambda, yani "karta
                            // tıklanınca ŞİMDİLİK hiçbir şey olmasın" demek.
                            // Detay ekranını yazınca, burada "bu karakterin id'siyle
                            // detay ekranına GİT" kodu olacak (navigasyon).
                            onCardClick = { },

                            // Aynı mantık: kalbe tıklanınca ŞİMDİLİK hiçbir şey
                            // olmuyor. Room'u bağlayınca burada "bu karakteri
                            // favorilere EKLE ya da favorilerden ÇIKAR" kodu olacak.
                            onFavoriteClick = { }
                        )
                    }
                }
            }
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) BU DOSYA, MVVM'İN HANGİ KATMANI?
    Bu, "View" katmanı - SADECE state'e bakıp ekranı çiziyor, hiçbir iş mantığı
    (network isteği, hata yönetimi kararı, sayfalama hesaplaması) İÇERMİYOR.
    Tüm o mantık CharacterListViewModel içinde. Bu ayrım sayesinde, yarın bu
    ekranı komple farklı bir tasarımla (örn. Grid görünümü) değiştirsek bile,
    ViewModel'in TEK SATIRI değişmez.

 2) "state.isLoading", "state.error", "state.characters" NEREDEN GELİYOR?
    Bunların hepsi, daha önce yazdığımız CharacterListState data class'ının
    alanları. ViewModel, _state.update { it.copy(...) } ile bu alanları
    güncelliyor, biz burada sadece OKUYORUZ (state by viewModel.state.collectAsState()
    satırı sayesinde).

 3) NEDEN "isFavorite = false" GİBİ GEÇİCİ/SABİT DEĞERLER KULLANMAK KÖTÜ BİR
    ALIŞKANLIK DEĞİL?
    Büyük bir projeyi TEK SEFERDE, HER ŞEYİYLE mükemmel yazmaya çalışmak
    başlangıç için hem yorucu hem hataya açık. Bunun yerine "iskeleti kur,
    çalıştır, gör, sonra bir sonraki parçayı ekle" yaklaşımı (BİZİM ŞU ANA
    KADAR İZLEDİĞİMİZ YOL) gerçek profesyonel projelerde de kullanılan bir
    yöntemdir - buna bazen "incremental development" (artımlı geliştirme) denir.

 4) SIRADA NE VAR?
    Room veritabanını kurup gerçek favori ekleme/çıkarma mantığını
    CharacterListViewModel'e ekleyeceğiz, sonra buradaki isFavorite = false
    ve onFavoriteClick = { } satırlarını GERÇEK değerlerle değiştireceğiz.
 ===========================================================
*/