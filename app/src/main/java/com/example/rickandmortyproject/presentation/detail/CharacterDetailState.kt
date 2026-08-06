package com.example.rickandmortyproject.presentation.detail

import com.example.rickandmortyproject.domain.model.Character

// CharacterListState'e ÇOK benziyor ama çok daha basit - burada sayfalama,
// arama, filtre YOK, sadece TEK bir karakterin detayını göstereceğiz.
data class CharacterDetailState(
    // Henüz veri gelmediyse null - Compose tarafında "character == null ise
    // yükleniyor/hata göster, doluysa detayları göster" mantığı kuracağız.
    val character: Character? = null,

    // İlk (ve tek) yükleme sırasında true olur.
    val isLoading: Boolean = false,

    // Karakter favoride mi - liste ekranındaki AYNI mantık, burada da
    // kalp butonunu doğru göstermek için repository.isFavorite(id) Flow'unu
    // izleyip bu alanı GÜNCEL tutacağız.
    val isFavorite: Boolean = false,

    // Ağ hatası olursa (örn. id'si geçersiz, internet yok) buraya yazılır.
    val error: String? = null
)

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) NEDEN "character: Character?" NULLABLE, LİSTE EKRANINDAKİ GİBİ
    "characters: List<Character> = emptyList()" DEĞİL?
    Liste ekranında "henüz veri yok" durumu BOŞ LİSTE ile temsil edilebilir
    (emptyList()) - mantıklı bir başlangıç durumu. Ama burada TEK bir
    karakter bekliyoruz - "boş bir Character" diye bir şey YOK (tüm alanları
    zorunlu, varsayılan boş string'lerle doldurmak ANLAMSIZ ve YANILTICI
    olurdu, sanki gerçek bir karakter varmış gibi görünürdü). Bu yüzden
    "veri yok" durumunu EN DOĞRU şekilde null ile ifade ediyoruz.

 2) BU STATE, CharacterListState'TEKİ "favoriteIds: Set<Int>" YERİNE NEDEN
    TEK BİR "isFavorite: Boolean" TUTUYOR?
    Liste ekranında ONLARCA karakter aynı anda gösteriliyordu, bu yüzden
    HEPSİ için TEK bir küme (Set) tutup "contains" ile kontrol etmek
    mantıklıydı. Burada ise SADECE TEK bir karakterin detayını gösteriyoruz,
    o yüzden tek bir Boolean yeterli ve daha basit.

 3) BU DOSYA HANGİ DOSYALARLA BAĞLANTILI OLACAK?
    - domain/model/Character.kt -> "character" alanının tipi buradan geliyor,
      bu dosya o modele BAĞIMLI.
    - presentation/detail/CharacterDetailViewModel.kt -> BİRAZDAN yazacağımız
      ViewModel, bu state'i "private val _state = MutableStateFlow(
      CharacterDetailState())" şeklinde İÇİNDE tutacak ve güncelleyecek -
      CharacterListViewModel'in CharacterListState'i kullanma şekliyle BİREBİR
      aynı ilişki.
    - presentation/detail/CharacterDetailScreen.kt -> yazacağımız Compose
      ekranı, "val state by viewModel.state.collectAsState()" ile bu sınıfın
      alanlarını (character, isLoading, isFavorite, error) OKUYUP ekranı ona
      göre çizecek.
    Yani zincir şöyle işleyecek:
    CharacterDetailState (bu dosya) <- ViewModel içinde tutulur <- Screen bunu izler
 ===========================================================
*/