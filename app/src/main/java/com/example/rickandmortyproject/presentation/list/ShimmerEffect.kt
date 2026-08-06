package com.example.rickandmortyproject.presentation.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// "Modifier.shimmerEffect()" -> bir EXTENSION FUNCTION, herhangi bir
// Composable'ın Modifier zincirine ".shimmerEffect()" diye EKLEYEBİLECEĞİMİZ
// yeniden kullanılabilir bir efekt. CharacterCard'daki extension function
// mantığıyla (CharacterDto.toDomainModel() gibi) AYNI fikir - burada Modifier
// üzerine yeni bir "yetenek" ekliyoruz.
fun Modifier.shimmerEffect(): Modifier = composed {
    // "rememberInfiniteTransition()" -> Compose'un, SÜREKLİ (durmadan)
    // tekrar eden animasyonlar için verdiği özel bir araç. Normal
    // animasyonlar (kalp animasyonumuz gibi) BİR KERE oynayıp DURUR, ama
    // shimmer'ın "sürekli parlayıp kayması" gerekiyor - bu yüzden "infinite"
    // (sonsuz) bir transition kullanıyoruz.
    val transition = rememberInfiniteTransition(label = "shimmer")

    // "animateFloat(...)" -> bu sonsuz transition İÇİNDE, 0f'den 1000f'e
    // giden, SÜREKLİ TEKRAR EDEN bir sayısal değer üretiyoruz. Bu değeri,
    // birazdan gradient'in (renk geçişinin) EKRANDA hangi konumda
    // olduğunu belirlemek için kullanacağız.
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            // "tween(durationMillis = 1200, easing = LinearEasing)" ->
            // 1.2 saniyede, SABİT HIZLA (LinearEasing - hızlanma/yavaşlama
            // olmadan, hep aynı hızda) 0'dan 1000'e git.
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            // "RepeatMode.Restart" -> 1000'e ulaşınca, TEKRAR baştan (0'dan)
            // başla - bu da huzmenin SÜREKLİ soldan sağa akmasını sağlıyor
            // (RepeatMode.Reverse olsaydı, ileri geri "sallanan" bir hareket
            // olurdu, biz TEK YÖNLÜ akış istiyoruz).
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    // "Brush.linearGradient(...)" -> bir "fırça" tanımlıyoruz, bu fırça
    // düz bir renk DEĞİL, birden fazla rengin YUMUŞAKÇA birbirine geçtiği
    // bir GRADIENT (geçiş). Burada 3 renk kullanıyoruz: açık gri - biraz
    // daha açık gri (parlak nokta) - açık gri. Ortadaki daha AÇIK renk,
    // "parlayan ışık huzmesi" hissini veriyor.
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE0E0E0), // koyuca gri
                Color(0xFFF5F5F5), // parlak nokta (huzme burada)
                Color(0xFFE0E0E0)  // tekrar koyuca gri
            ),
            // "start = Offset(translateAnimation.value - 500f, 0f)" ve
            // "end = Offset(translateAnimation.value, 0f)" -> gradient'in
            // BAŞLANGIÇ ve BİTİŞ noktalarını, animasyon değerine göre
            // SÜREKLİ KAYDIRIYORUZ. translateAnimation.value 0'dan 1000'e
            // giderken, bu gradient de EKRANDA SOLDAN SAĞA doğru "akıyor" -
            // işte gördüğümüz "kayan parlaklık" efekti TAM OLARAK bu.
            start = Offset(translateAnimation.value - 500f, 0f),
            end = Offset(translateAnimation.value, 0f)
        )
    )
}

// Gerçek kartın (CharacterCard) İSKELETİNİ taklit eden, shimmer efektli
// SAHTE bir kart. Gerçek veriyi DEĞİL, sadece GRİ BLOKLAR gösteriyor -
// kullanıcıya "burada birazdan bir kart görünecek" hissi veriyor.
@Composable
fun ShimmerCharacterCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // GÖRSEL ETKİSİ: gerçek karttaki YUVARLAK profil resminin
            // YERİNE, aynı boyutta, shimmer'lı GRİ bir daire.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // GÖRSEL ETKİSİ: gerçek karttaki İSİM yazısının YERİNE,
                // ince, kısa bir shimmer'lı GRİ çubuk.
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f) // genişliğin sadece %60'ı kadar - isim genelde durum satırından KISA olur
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // GÖRSEL ETKİSİ: gerçek karttaki "Alive - Human" satırının
                // YERİNE, biraz daha UZUN bir shimmer'lı GRİ çubuk.
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

// Liste ekranı ilk yüklenirken, TEK bir shimmer kart YERİNE, gerçek
// bir LİSTE hissi vermek için ART ARDA BİRKAÇ tane gösteriyoruz.
@Composable
fun ShimmerCharacterList() {
    LazyColumn {
        // "items(6) { ... }" -> Character listesi yerine, sadece "6 tane
        // göster" diyoruz (0'dan 5'e kadar sayılar), her biri için AYNI
        // shimmer kartı çiziyoruz - ekranı ortalama olarak DOLDURACAK
        // kadar bir sayı seçtik.
        items(6) {
            ShimmerCharacterCard()
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) BU FONKSİYON NEDEN "fun Modifier.shimmerEffect(): Modifier" ŞEKLİNDE
    YAZILDI, NEDEN AYRI BİR @Composable FONKSİYON DEĞİL?
    Modifier'lar, Compose'da "zincirleme" (chaining) şeklinde kullanılır:
    "Modifier.size(100.dp).clip(CircleShape).background(Color.Red)" gibi.
    Kendi ÖZEL efektimizi de bu ZİNCİRE "Modifier.shimmerEffect()" diye
    EKLEYEBİLMEK için, onu da bir Modifier EXTENSION FUNCTION olarak
    yazmamız gerekiyor - bu, Compose'da özel görsel efektler yazmanın
    standart yoludur.

 2) "composed { }" NE İŞE YARIYOR?
    Modifier'lar normalde "state" (hafıza) TUTAMAZ - ama biz burada
    "rememberInfiniteTransition" ile bir ANİMASYON DURUMU tutmak
    istiyoruz. "composed { }" bloğu, bu Modifier'a GEÇICI olarak bir
    Composable bağlamı (context) kazandırıyor, böylece İÇİNDE "remember",
    "rememberInfiniteTransition" gibi Compose fonksiyonlarını GÜVENLE
    kullanabiliyoruz.

 3) "ShimmerCharacterCard" GERÇEK "CharacterCard" İLE AYNI BOYUTLARI
    KULLANMAYA NEDEN ÖZEN GÖSTERDİK (64.dp resim, benzer padding)?
    Gerçek veri geldiğinde, shimmer kartların YERİNİ gerçek kartlar alacak -
    eğer boyutlar ÇOK FARKLI olsaydı, ekran "zıplayarak" (içerik aniden
    büyüyüp küçülerek) değişirdi, bu da kötü bir kullanıcı deneyimi
    yaratırdı. Boyutları YAKIN tutmak, geçişi daha PÜRÜZSÜZ hissettiriyor.

 4) BU MODIFIER/COMPOSABLE'LAR NEREDE KULLANILACAK?
    CharacterListScreen.kt'de, "state.isLoading" true iken (veri henüz
    gelmeden), GERÇEK CircularProgressIndicator YERİNE, ShimmerCharacterList()
    çağıracağız - bu değişikliği BİR SONRAKİ adımda yapacağız.
 ===========================================================
*/