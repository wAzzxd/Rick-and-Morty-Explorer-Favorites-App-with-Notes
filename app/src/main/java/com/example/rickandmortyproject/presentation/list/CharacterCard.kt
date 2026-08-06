package com.example.rickandmortyproject.presentation.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import coil.compose.AsyncImage
import com.example.rickandmortyproject.domain.model.Character

// Bu Composable, TEK BİR karakterin kart görünümünü tarif ediyor.
// LazyColumn içindeki "items(state.characters) { character -> ... }" bloğunda
// her karakter için bu fonksiyon çağrılacak.
//
// ============ GENEL YERLEŞİM HARİTASI (kartın içi böyle dizilecek) ============
//   ┌─────────────────────────────────────────────┐
//   │  [Resim]   [İsim]              [Kalp]        │
//   │            [● Durum - Tür]                   │
//   └─────────────────────────────────────────────┘
// Bunu sağlayan YAPI: en dışta TEK bir "Row" var (aşağıda göreceksin),
// içine SIRAYLA 3 şey ekliyoruz: Resim, sonra "isim+durum"u taşıyan bir
// Column, sonra Kalp butonu. Row, elemanları eklediğimiz SIRAYLA soldan
// sağa dizer - bu yüzden kodda YAZILIŞ SIRASI = EKRANDAKİ SOLDAN SAĞA SIRA.
@Composable
fun CharacterCard(
    character: Character,           // hangi karakteri çizeceğimiz
    isFavorite: Boolean,             // bu karakter favorilerde mi (kalp dolu mu boş mu)
    onCardClick: () -> Unit,         // karta tıklanınca ne olsun (ileride detay ekranına gidecek)
    onFavoriteClick: () -> Unit      // kalbe tıklanınca ne olsun (ileride Room'a kaydedecek)
) {
    // "Card" -> Material Design'ın hazır kart bileşeni, gölgeli/yuvarlak köşeli
    // bir kutu çizer, biz içine istediğimiz layoutu koyarız.
    // GÖRSEL ETKİSİ: ekrandaki HER BİR karakter satırının o beyaz/gölgeli
    // "kutu" görünümünü VEREN kod BURASI.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            // "clickable" -> bu Composable'a tıklanabilirlik ekliyor,
            // tıklanınca "onCardClick" fonksiyonunu (parametre olarak dışarıdan gelen) çalıştırır.
            .clickable { onCardClick() }
    ) {
        // "Row" -> içindeki elemanları YATAY (soldan sağa) diziyor.
        // ############ İŞTE "SOLDAN SAĞA DİZİLİM"İ SAĞLAYAN ANA YAPI BU ROW ############
        // Bu Row'un İÇİNE SIRAYLA eklediğimiz 3 şey (AsyncImage, Column, IconButton),
        // ekranda TAM OLARAK bu sırayla soldan sağa yerleşecek. Kalp butonunun
        // "en sağda" durmasının sebebi, kodda EN SON eklenen eleman OLMASI VE
        // ondan önceki Column'ın "weight(1f)" ile ARADAKİ TÜM boşluğu yutması
        // (bunu birazdan Column'da göreceğiz) - kalp bu yüzden otomatik olarak
        // sağa "itiliyor".
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically // dikeyde ortala
        ) {
            // "AsyncImage" -> Coil'in verdiği Composable, bir URL'den resmi
            // İNDİRİP asenkron olarak gösteriyor. Biz burada hiçbir manuel
            // network/thread kodu yazmıyoruz, Coil hepsini arka planda hallediyor.
            // GÖRSEL ETKİSİ: Row'un İÇİNDEKİ İLK eleman olduğu için, kartın
            // EN SOLUNDAKİ yuvarlak profil resmi TAM OLARAK bu kod.
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.name, // erişilebilirlik (görme engelliler için ekran okuyucu bunu okur)
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape) // resmi YUVARLAK kesiyor (profil fotoğrafı gibi)
            )

            Spacer(modifier = Modifier.width(12.dp)) // iki eleman arası boşluk
            // GÖRSEL ETKİSİ: resimle isim arasındaki BOŞLUĞUN GENİŞLİĞİNİ
            // (12dp) BELİRLEYEN satır bu - sadece boşluk, hiçbir şey ÇİZMİYOR.

            // "Column" -> içindeki elemanları DİKEY (yukarıdan aşağı) diziyor.
            // "weight(1f)" -> bu Column, Row içinde KALAN TÜM boşluğu doldursun demek,
            // böylece sağdaki kalp butonu her zaman en sağda sabit kalır.
            // GÖRSEL ETKİSİ: kartın ORTA BÖLGESİNDEKİ (isim + durum satırı)
            // her şey bu Column'ın İÇİNDE tanımlı. "weight(1f)" TAM OLARAK
            // kalp butonunun neden hep "en sağa yapışık" durduğunu açıklıyor:
            // bu Column, resim ile kalp ARASINDA KALAN TÜM YATAY ALANI kaplıyor,
            // kalbin sağa kaymasına başka SEÇENEK bırakmıyor.
            Column(modifier = Modifier.weight(1f)) {
                // GÖRSEL ETKİSİ: Column'ın İLK satırı = kartta EN ÜSTTE
                // görünen İSİM yazısı.
                Text(text = character.name)

                Spacer(modifier = Modifier.height(4.dp))
                // "Spacer" -> görünmez, sadece BOŞLUK bırakan bir Composable. Burada,
                // üstteki isim (Text) ile aşağıdaki durum satırı arasına dikey 4dp
                // boşluk koyuyoruz - aksi halde ikisi birbirine yapışık görünürdü.
                // GÖRSEL ETKİSİ: isim ile altındaki durum satırı ARASINDAKİ
                // DİKEY boşluğu (4dp) belirleyen satır bu.

                // GÖRSEL ETKİSİ: Column'ın İKİNCİ (ve son) satırı = kartta
                // isim yazısının HEMEN ALTINDA görünen "● Alive - Human" satırı.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "Row" -> içindeki elemanları YATAY (soldan sağa) sırayla diziyor.
                    // Burada 3 eleman yan yana dizilecek: [renkli nokta] [boşluk] [yazı].
                    //
                    // "verticalAlignment = Alignment.CenterVertically" -> Row içindeki
                    // elemanlar boy olarak FARKLI olabilir (nokta küçük, yazı daha uzun),
                    // bu satır hepsini DİKEY EKSENDE ortalar, yani nokta ile yazının
                    // tam ortadan hizalı görünmesini sağlar (biri yukarıda biri aşağıda
                    // kaymasın diye).

                    // GÖRSEL ETKİSİ: bu satırın EN SOLUNDAKİ küçük RENKLİ
                    // NOKTAYI (yeşil/kırmızı/gri) ÇİZEN kod, aşağıdaki Box.
                    Box(
                        // "Box" burada aslında tek bir amaçla kullanılıyor: KÜÇÜK RENKLİ
                        // BİR DAİRE çizmek. Box'ın içi boş, sadece boyut + şekil + renk
                        // veriyoruz, herhangi bir "içerik" (Text, Image vb.) yok.
                        modifier = Modifier
                            .size(8.dp)
                            // "size(8.dp)" -> bu Box'ı 8dp x 8dp boyutunda, yani KÜÇÜK
                            // bir kare alan yapıyor (dp = "density-independent pixel",
                            // farklı ekran çözünürlüklerinde hep AYNI GÖRÜNEN boyut birimi).

                            .clip(CircleShape)
                            // "clip(CircleShape)" -> bu kareyi YUVARLAK kesiyor. "clip"
                            // kelimesi tam olarak "kırp/kes" demek - elimizdeki kare
                            // şekli, verdiğimiz CircleShape'e göre budayıp daire haline
                            // getiriyor. Bu satır olmasaydı, aşağıdaki background rengi
                            // KARE olarak görünürdü, yuvarlak değil.

                            .background(
                                // "background(...)" -> bu Box'ın İÇİNİ boyuyoruz.
                                // "when (character.status) { ... }" -> Kotlin'in çoklu
                                // durum kontrolü. character.status'un DEĞERİNE göre
                                // (API'den "Alive", "Dead" veya başka bir şey gelebilir,
                                // örn. "unknown") FARKLI bir Color döndürüyoruz.
                                // GÖRSEL ETKİSİ: NOKTANIN RENGİNİ (yeşil/kırmızı/gri)
                                // TAM OLARAK karar veren kod bu "when" bloğu.
                                color = when (character.status) {
                                    "Alive" -> Color(0xFF4CAF50) // yeşil
                                    // "0xFF4CAF50" bir HEX renk kodu. Baştaki "FF" ALPHA
                                    // (saydamlık) değeri - FF tamamen OPAK (görünür) demek.
                                    // Geri kalan "4CAF50" ise Kırmızı-Yeşil-Mavi (RGB)
                                    // bileşenleri, bu kombinasyon Material Design'ın
                                    // standart "green 500" tonu.

                                    "Dead" -> Color(0xFFF44336)  // kırmızı
                                    // Aynı mantık, bu sefer Material'ın standart kırmızısı.

                                    else -> Color(0xFF9E9E9E)    // gri (unknown)
                                    // "else" -> "Alive" ve "Dead" DIŞINDA gelen HER ŞEY
                                    // (API'de "unknown" olarak geçen durum dahil) için
                                    // varsayılan gri renk. "when" bloklarında "else"
                                    // kolu, ELİMİZDE OLMAYAN/beklenmedik ihtimalleri
                                    // güvenle karşılamak için önemlidir - yoksa Kotlin
                                    // "tüm durumları karşılamadın" diye derleme hatası
                                    // bile verebilir (exhaustive when kuralı).
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    // Yine görünmez bir boşluk, ama bu sefer YATAY (width) - noktayla
                    // yanındaki yazı arasına küçük bir boşluk koyuyor.
                    // GÖRSEL ETKİSİ: renkli nokta ile yanındaki "Alive - Human"
                    // yazısı ARASINDAKİ yatay boşluğu (6dp) belirleyen satır.

                    // GÖRSEL ETKİSİ: bu Row'un İKİNCİ (ve son) elemanı = noktanın
                    // HEMEN SAĞINDAKİ "Alive - Human" yazısı.
                    Text(text = "${character.status} - ${character.species}")
                    // "${...}" -> Kotlin'in STRING TEMPLATE (metin şablonu) özelliği.
                    // Bir String'in İÇİNE, süslü parantez kullanarak değişken/ifade
                    // gömebiliyoruz. Bu satır, örneğin character.status "Alive" ve
                    // character.species "Human" ise, ekranda tam olarak
                    // "Alive - Human" yazısını basar. Eğer bunu template KULLANMADAN
                    // yazsaydık, "character.status + " - " + character.species" gibi
                    // daha uzun ve okunması zor bir birleştirme yapmamız gerekirdi.
                }
            }

            // ============ YENİ EKLENEN: Kalp animasyonu ============
            //
            // "Animatable(1f)" -> Compose'un, ELLE kontrol edebildiğimiz bir
            // "animasyonlu değer" nesnesi. Normal bir "var scale = 1f"
            // yazsaydık, değeri değiştirdiğimizde ANINDA (sıçrayarak) 1'den
            // 1.3'e geçerdi. Animatable, "bu değeri DEĞİŞTİR ama YUMUŞAKÇA,
            // ARADAKİ tüm adımları göstererek" imkanı veriyor.
            //
            // "remember { }" -> bu Animatable nesnesinin, kart YENİDEN
            // çizildiğinde (recomposition) SIFIRDAN oluşturulmamasını, AYNI
            // nesnenin KORUNMASINI sağlıyor - aksi halde her recomposition'da
            // animasyon YENİDEN başlardı, hiç bitmeyen bir döngü olurdu.
            // NOT: bu satırın kendisi HİÇBİR ŞEY ÇİZMİYOR, sadece animasyon
            // İÇİN bir "hafıza kutusu" hazırlıyor - görsel etkisi, aşağıdaki
            // Modifier.scale(...) satırında ortaya çıkacak.
            val scale = remember { Animatable(1f) }

            // "LaunchedEffect(isFavorite)" -> "isFavorite" DEĞİŞTİĞİNDE
            // (kullanıcı kalbe bastığında, false'tan true'ya ya da tam
            // tersi geçtiğinde) İÇİNDEKİ bloğu ÇALIŞTIRIR - bu, coroutine
            // gerektiren (animateTo suspend bir fonksiyon) bir iş olduğu
            // için LaunchedEffect kullanmak ZORUNLU (normal bir Composable
            // gövdesinde suspend fonksiyon DOĞRUDAN çağrılamaz).
            LaunchedEffect(isFavorite) {
                // "scale.animateTo(1.3f, spring(...))" -> mevcut ölçekten
                // (1f) HEDEFE (1.3f) doğru, YAY (spring) tarzında bir
                // animasyonla BÜYÜT. "Spring.DampingRatioMediumBouncy" ->
                // yayın ne kadar "zıplayarak" (bounce) yerleşeceğini
                // belirleyen bir sabit - MediumBouncy, hafif ama belirgin
                // bir zıplama hissi veriyor (çok az zıplama isteseydik
                // DampingRatioLowBouncy, hiç istemeseydik DampingRatioNoBouncy
                // kullanabilirdik).
                scale.animateTo(
                    targetValue = 1.3f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                // İlk animasyon (büyüme) BİTTİKTEN SONRA, İKİNCİ bir
                // animasyonla (küçülme) tekrar normal boyuta (1f) DÖNÜYORUZ.
                // İki animasyonun ARKA ARKAYA (sequential) çalışması, kalbin
                // "büyüyüp küçülme" (pulse/pop) hissini yaratıyor.
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
            }

            // "IconButton" -> tıklanabilir bir ikon butonu.
            // GÖRSEL ETKİSİ: bu, Row'a eklenen EN SON (3.) eleman olduğu
            // için, kartın EN SAĞINDAKİ kalp ikonu TAM OLARAK bu kod. "En
            // sağda durmasının" TEK sebebi kodda "en son yazılmış olması"
            // DEĞİL - asıl sebep, bir önceki Column'ın "weight(1f)" ile
            // ARADAKİ TÜM boşluğu kendine ayırıp bu IconButton'ı sağa
            // "sıkıştırması". (Column weight almasaydı, kalp ismin HEMEN
            // yanında, sola yakın dururdu.)
            IconButton(onClick = { onFavoriteClick() }) {
                Icon(
                    // isFavorite'e göre DOLU ya da BOŞ kalp ikonu gösteriyoruz.
                    // GÖRSEL ETKİSİ: kalbin DOLU KIRMIZI mı yoksa BOŞ GRİ
                    // ÇERÇEVELİ mi göründüğünü belirleyen satır TAM OLARAK bu.
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorilere ekle",
                    tint = if (isFavorite) Color.Red else Color.Gray,
                    // "Modifier.scale(scale.value)" -> ikonun GÖRSEL boyutunu,
                    // az önce tanımladığımız "scale" Animatable'ının O ANKİ
                    // değerine göre BÜYÜTÜP KÜÇÜLTÜYORUZ. "scale.value" her
                    // animasyon karesinde DEĞİŞTİĞİ için (1f -> 1.3f -> 1f
                    // arasında YUMUŞAKÇA), ikon da GÖZLE GÖRÜLÜR şekilde
                    // büyüyüp küçülüyor.
                    // GÖRSEL ETKİSİ: kalbe basınca gördüğün "büyüyüp küçülme"
                    // animasyonunun ekranda GERÇEKTEN uygulandığı YER burası -
                    // yukarıdaki Animatable ve LaunchedEffect sadece "değeri
                    // HESAPLIYORDU", bu satır ise o hesaplanan değeri
                    // GERÇEKTEN ikonun boyutuna UYGULUYOR.
                    modifier = Modifier.scale(scale.value)
                )
            }
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR - KALP ANİMASYONU ====================

 1) NEDEN "Animatable" KULLANDIK, "animateFloatAsState" DEĞİL?
    "animateFloatAsState", bir DEĞERİN doğrudan HEDEFİNE animasyonlu geçmesini
    sağlar (örn. "isFavorite true ise 1.3f'ye, false ise 1f'ye git" gibi TEK
    bir hedef). Ama biz burada İKİ AŞAMALI bir animasyon istiyoruz: ÖNCE
    büyü, SONRA küçül - yani "1f -> 1.3f -> 1f" şeklinde bir SIRALI (sequential)
    hareket. Bu tür "birden fazla adımı SIRAYLA çalıştırma" ihtiyacı olduğunda,
    Animatable + LaunchedEffect içinde manuel animateTo() çağrıları kullanmak
    gerekiyor - animateFloatAsState bu sıralı yapıyı TEK BAŞINA kuramaz.

 2) BU ANİMASYON, isFavorite HER DEĞİŞTİĞİNDE Mİ TETİKLENİYOR, YOKSA SADECE
    "EKLENİRKEN" Mİ?
    Şu anki haliyle HER İKİ yönde de (favoriye eklenirken VE çıkarılırken)
    tetikleniyor - çünkü LaunchedEffect(isFavorite) sadece "isFavorite
    DEĞERİ DEĞİŞTİ Mİ" diye bakıyor, yönünü AYIRT etmiyor. Bu, favoriden
    çıkarırken de kalbin "tık" diye tepki vermesini sağlıyor, kullanıcı
    deneyimi açısından tutarlı bir his veriyor.

 3) "spring(dampingRatio = ...)" DIŞINDA BAŞKA AYARLAR VAR MI?
    Evet - "stiffness" (sertlik) parametresi de var, animasyonun NE KADAR
    HIZLI hareket edeceğini belirler (Spring.StiffnessLow, StiffnessMedium,
    StiffnessHigh gibi hazır sabitler mevcut). Biz şimdilik varsayılan
    sertlikle bıraktık, istersek "spring(dampingRatio = ..., stiffness =
    Spring.StiffnessHigh)" diyerek daha HIZLI bir zıplama da elde edebilirdik.

 4) BU DOSYA HANGİ DOSYALARLA BAĞLANTILI OLACAK (GÜNCEL)?
    - domain/model/Character.kt -> character parametresinin tipi.
    - presentation/list/CharacterListScreen.kt VE
      presentation/favorites/FavoritesScreen.kt -> İKİSİ de bu Composable'ı
      ÇAĞIRIYOR, dolayısıyla bu animasyon HER İKİ ekranda da OTOMATİK olarak
      çalışacak - kodu TEK bir yerde yazdığımız için, TEK bir değişiklik
      TÜM kullanıldığı yerlere yansıyor (DRY prensibi).

 5) "YERLEŞİM" (LAYOUT) NASIL OKUNUR, GENEL KURAL NE?
    Compose'da "Row" YATAY dizer, "Column" DİKEY dizer. Bir elemanın
    ekranda NEREDE göründüğünü anlamak için şu soruları sor: "Bu hangi
    Row/Column'ın İÇİNDE?" ve "O Row/Column'da KAÇINCI SIRADA yazılmış?"
    Kodda YUKARIDAN AŞAĞI okuduğunda, Row'larda SOLDAN SAĞA, Column'larda
    YUKARIDAN AŞAĞI sıralamayı GÖRECEKSİN - bu proje boyunca hep bu kurala
    sadık kaldık.
 ===========================================================
*/