package com.example.rickandmortyproject.presentation.list

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rickandmortyproject.domain.model.Character

// Bu Composable, TEK BİR karakterin kart görünümünü tarif ediyor.
// LazyColumn içindeki "items(state.characters) { character -> ... }" bloğunda
// her karakter için bu fonksiyon çağrılacak.
@Composable
fun CharacterCard(
    character: Character,           // hangi karakteri çizeceğimiz
    isFavorite: Boolean,             // bu karakter favorilerde mi (kalp dolu mu boş mu)
    onCardClick: () -> Unit,         // karta tıklanınca ne olsun (ileride detay ekranına gidecek)
    onFavoriteClick: () -> Unit      // kalbe tıklanınca ne olsun (ileride Room'a kaydedecek)
) {
    // "Card" -> Material Design'ın hazır kart bileşeni, gölgeli/yuvarlak köşeli
    // bir kutu çizer, biz içine istediğimiz layoutu koyarız.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            // "clickable" -> bu Composable'a tıklanabilirlik ekliyor,
            // tıklanınca "onCardClick" fonksiyonunu (parametre olarak dışarıdan gelen) çalıştırır.
            .clickable { onCardClick() }
    ) {
        // "Row" -> içindeki elemanları YATAY (soldan sağa) diziyor.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically // dikeyde ortala
        ) {
            // "AsyncImage" -> Coil'in verdiği Composable, bir URL'den resmi
            // İNDİRİP asenkron olarak gösteriyor. Biz burada hiçbir manuel
            // network/thread kodu yazmıyoruz, Coil hepsini arka planda hallediyor.
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.name, // erişilebilirlik (görme engelliler için ekran okuyucu bunu okur)
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape) // resmi YUVARLAK kesiyor (profil fotoğrafı gibi)
            )

            Spacer(modifier = Modifier.width(12.dp)) // iki eleman arası boşluk

            // "Column" -> içindeki elemanları DİKEY (yukarıdan aşağı) diziyor.
            // "weight(1f)" -> bu Column, Row içinde KALAN TÜM boşluğu doldursun demek,
            // böylece sağdaki kalp butonu her zaman en sağda sabit kalır.
            Column(modifier = Modifier.weight(1f)) {
                Text(text = character.name)

                Spacer(modifier = Modifier.height(4.dp))
// "Spacer" -> görünmez, sadece BOŞLUK bırakan bir Composable. Burada,
// üstteki isim (Text) ile aşağıdaki durum satırı arasına dikey 4dp
// boşluk koyuyoruz - aksi halde ikisi birbirine yapışık görünürdü.

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "Row" -> içindeki elemanları YATAY (soldan sağa) sırayla diziyor.
                    // Burada 3 eleman yan yana dizilecek: [renkli nokta] [boşluk] [yazı].
                    //
                    // "verticalAlignment = Alignment.CenterVertically" -> Row içindeki
                    // elemanlar boy olarak FARKLI olabilir (nokta küçük, yazı daha uzun),
                    // bu satır hepsini DİKEY EKSENDE ortalar, yani nokta ile yazının
                    // tam ortadan hizalı görünmesini sağlar (biri yukarıda biri aşağıda
                    // kaymasın diye).

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

            // "IconButton" -> tıklanabilir bir ikon butonu.
            IconButton(onClick = { onFavoriteClick() }) {
                Icon(
                    // isFavorite'e göre DOLU ya da BOŞ kalp ikonu gösteriyoruz.
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorilere ekle",
                    tint = if (isFavorite) Color.Red else Color.Gray
                )
            }
        }
    }
}