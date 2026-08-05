package com.example.rickandmortyproject.presentation.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.rickandmortyproject.presentation.list.CharacterCard
import org.koin.androidx.compose.koinViewModel

// Bu ekran, MVVM'in "View" katmanı - CharacterListScreen'e ÇOK benziyor
// ama çok daha basit, çünkü FavoritesViewModel de basitti (sayfalama,
// arama, hata yönetimi YOK).
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    // "koinViewModel()" -> yine Koin'den FavoritesViewModel'i istiyoruz.
    // Bunu bir SONRAKİ adımda AppModule.kt'ye tanıtacağız, henüz tanıtmadık -
    // şu an bu satır kırmızı görünebilir, normal, birazdan düzelecek.
    viewModel: FavoritesViewModel = koinViewModel()
) {
    // "collectAsState()" -> ViewModel'deki StateFlow<List<Character>>'ı
    // Compose'un izleyebileceği bir yapıya çeviriyor. Room'da bir değişiklik
    // olduğunda (favoriye ekleme/çıkarma), bu liste OTOMATİK güncellenip
    // ekran yeniden çizilecek.
    val favorites by viewModel.favorites.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        // "if (favorites.isEmpty())" -> hiç favori yoksa, boş bir ekran
        // yerine kullanıcıya anlamlı bir mesaj gösteriyoruz.
        if (favorites.isEmpty()) {
            Text(
                text = "Henüz favori karakteriniz yok.\nBir karakteri favorilemek için kalp ikonuna dokunun.",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // "items(favorites, key = { it.id })" -> liste ekranındaki
                // AYNI mantık: her karakteri BENZERSİZ id'siyle eşleştirip,
                // Compose'un kartları KARIŞTIRMASINI önlüyoruz.
                items(
                    items = favorites,
                    key = { character -> character.id }
                ) { character ->
                    // DİKKAT: CharacterCard'ı YENİDEN YAZMIYORUZ - liste
                    // ekranında yazdığımız AYNI Composable'ı buradan da
                    // ÇAĞIRIYORUZ (import satırına bak: presentation.list
                    // paketinden import ediyoruz). Bu, Compose'un "yeniden
                    // kullanılabilir bileşen" felsefesinin tam örneği - bir
                    // kart tasarımını BİR KERE yazıp, istediğimiz her ekranda
                    // kullanabiliyoruz.
                    CharacterCard(
                        character = character,
                        // Bu ekrandaki HER karakter zaten favoride olduğu
                        // için, isFavorite'i HER ZAMAN true veriyoruz - burada
                        // state.favoriteIds.contains(...) gibi bir kontrole
                        // GEREK YOK, çünkü bu liste zaten SADECE favorilerden oluşuyor.
                        isFavorite = true,
                        // Detay ekranını yazınca burada da navigasyon olacak.
                        onCardClick = { },
                        // Kalbe basınca, bu ekrandan doğrudan ÇIKARMA işlemi
                        // yapıyoruz (toggle değil, çünkü zaten favoride).
                        onFavoriteClick = { viewModel.onRemoveFavoriteClick(character) }
                    )
                }
            }
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) CharacterCard'I NEDEN YENİDEN YAZMADIK, DOĞRUDAN import EDİP KULLANDIK?
    Hatırlarsan CharacterCard.kt'yi yazarken "stateless composable" (durumsuz
    bileşen) prensibinden bahsetmiştik: CharacterCard kendi başına HİÇBİR
    karar vermiyor, sadece dışarıdan verilen character/isFavorite/onClick
    fonksiyonlarına göre çiziyor. Bu sayede AYNI kartı, FARKLI ekranlarda
    (liste ekranı VE favoriler ekranı), FARKLI davranışlarla (toggle vs.
    doğrudan çıkarma) besleyerek TEKRAR TEKRAR kullanabiliyoruz - Clean
    Code'daki "DRY" (Don't Repeat Yourself - kendini tekrar etme) prensibinin
    somut bir örneği.

 2) "isFavorite = true" NEDEN SABİT, KONTROL GEREKMİYOR MU?
    Bu ekranın TEK veri kaynağı zaten Room'daki favori listesi - yani
    favorites listesindeki HER karakter, TANIM GEREĞİ favoridedir. Liste
    ekranında (CharacterListScreen) durum farklıydı çünkü orada TÜM
    karakterler gösteriliyordu, o yüzden HER birinin favoride olup
    olmadığını AYRI AYRI kontrol etmemiz gerekiyordu.

 3) BİR KARAKTERİ BURADA "ÇIKARDIĞIMDA" NE OLUYOR, LİSTE EKRANI ETKİLENİYOR MU?
    Evet, OLUMLU şekilde etkileniyor: onRemoveFavoriteClick, Room'dan o
    karakteri SİLİYOR. CharacterListViewModel'deki repository.getFavorites()
    izleyicisi bunu YAKALAYIP state.favoriteIds'i günceller - liste
    ekranındaki o karakterin kalbi de OTOMATİK boşalır. İki ekran da AYNI
    "tek gerçek kaynağı" (Room) izlediği için, birbirleriyle HİÇ konuşmadan
    (elle senkronize etmeden) senkron kalıyorlar.


 ===========================================================
*/