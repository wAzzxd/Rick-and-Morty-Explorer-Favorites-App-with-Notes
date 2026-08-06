package com.example.rickandmortyproject.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// "characterId: Int" -> DİKKAT, bu ViewModel diğerlerinden FARKLI: repository
// dışında BİR PARAMETRE DAHA alıyor. Bu, "hangi karakterin detayını
// göstereceğiz" bilgisi - liste ekranından tıklanan karakterin id'si.
// Koin'e bunu NASIL vereceğimizi AppModule.kt'yi güncellerken göreceğiz
// (bu ana kadar kullanmadığımız "parametersOf" adlı yeni bir Koin özelliği
// devreye girecek).
class CharacterDetailViewModel(
    private val characterId: Int,
    private val repository: CharacterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CharacterDetailState())
    val state: StateFlow<CharacterDetailState> = _state

    init {
        loadCharacter()

        // Liste ekranındaki AYNI mantık: repository.isFavorite(characterId)
        // Room'u CANLI izleyen bir Flow<Boolean> döndürüyordu. Favoriler
        // değiştiğinde (bu ekrandan ya da BAŞKA bir ekrandan bile),
        // bu blok OTOMATİK tetiklenip state'i günceller.
        repository.isFavorite(characterId)
            .onEach { isFavorite ->
                _state.update { it.copy(isFavorite = isFavorite) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCharacter() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // repository.getCharacterById(...) -> daha önce yazdığımız,
                // Retrofit üzerinden tek bir karakteri çeken fonksiyon.
                // Bu proje boyunca İLK KEZ burada kullanıyoruz (liste ekranı
                // hep getCharacters() - çoğul - kullanmıştı).
                val character = repository.getCharacterById(characterId)
                _state.update {
                    it.copy(character = character, isLoading = false)
                }
            } catch (e: Exception) {
                // Liste ekranındaki AYNI kritik kural: coroutine iptal
                // sinyalini GERÇEK hatayla KARIŞTIRMIYORUZ.
                if (e is kotlinx.coroutines.CancellationException) throw e

                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Karakter yüklenemedi"
                    )
                }
            }
        }
    }

    // Kalp butonuna basılınca çağrılacak. Bu ekranda ELİMİZDE zaten TAM bir
    // Character nesnesi var (state.character), o yüzden CharacterListViewModel'
    // deki gibi "parametre olarak character al" yerine, doğrudan state'teki
    // karakteri kullanabiliyoruz.
    fun onFavoriteClick() {
        val character = _state.value.character ?: return
        viewModelScope.launch {
            if (_state.value.isFavorite) {
                repository.removeFavorite(character)
            } else {
                repository.addFavorite(character)
            }
        }
    }

    // Kullanıcı hata ekranındaki "Tekrar Dene" butonuna basınca çağrılacak.
    fun retry() {
        loadCharacter()
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) "val character = _state.value.character ?: return" SATIRI NE YAPIYOR?
    "?:" -> Kotlin'in "Elvis operatörü" denen bir kısayolu: "solundaki değer
    null İSE, sağındakini yap" demek. Yani "character null ise (henüz
    yüklenmediyse), fonksiyondan HİÇBİR ŞEY yapmadan ÇIK (return)" - bu,
    henüz veri gelmeden kullanıcı yanlışlıkla kalbe basarsa çökmemizi
    önleyen bir GÜVENLİK kontrolü.

 2) NEDEN CharacterListViewModel'deki "onFavoriteClick(character: Character)"
    FONKSİYONU PARAMETRE ALIYORDU AMA BURADAKİ ALMIYOR?
    Liste ekranında AYNI ANDA onlarca FARKLI karakter kartı vardı, her kart
    "BEN hangi karakterim" bilgisini KENDİSİ taşımak ZORUNDAYDI (parametre
    olarak). Burada ise ekranda SADECE TEK bir karakter var, ViewModel zaten
    "characterId" ile HANGİ karakterle ilgilendiğini BİLİYOR - state.character
    üzerinden erişebiliyoruz, tekrar dışarıdan almaya gerek yok.

 3) BU DOSYA HANGİ DOSYALARLA BAĞLANTILI OLACAK?
    - presentation/detail/CharacterDetailState.kt -> bu ViewModel, state'i
      BU sınıfın bir nesnesi olarak (_state = MutableStateFlow(
      CharacterDetailState())) TUTUYOR ve copy() ile güncelliyor.
    - domain/repository/CharacterRepository.kt -> constructor'dan İNTERFACE
      olarak alıyor (Dependency Inversion), getCharacterById/isFavorite/
      addFavorite/removeFavorite fonksiyonlarını KULLANIYOR.
    - di/AppModule.kt -> BİRAZDAN, bu ViewModel'i Koin'e tanıtırken
      "characterId" parametresini NASIL sağlayacağımızı (parametersOf ile)
      göreceğiz - bu dosya, Koin'in bu tanımına BAĞIMLI olacak.
    - presentation/detail/CharacterDetailScreen.kt -> BİRAZDAN yazacağımız
      ekran, koinViewModel() ile bu ViewModel'i alıp state'ini izleyecek,
      onFavoriteClick() ve retry() fonksiyonlarını ÇAĞIRACAK.
    - presentation/navigation/NavGraph.kt -> detay route'u tanımlanırken,
      route'tan ÇEKİLEN "id" parametresinin BU ViewModel'e nasıl ULAŞTIĞI
      (Koin üzerinden) orada netleşecek.
 ===========================================================
*/