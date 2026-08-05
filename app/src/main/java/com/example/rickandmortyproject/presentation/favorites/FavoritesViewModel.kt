package com.example.rickandmortyproject.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmortyproject.domain.model.Character
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Bu ViewModel, CharacterListViewModel'e göre ÇOK DAHA BASİT - çünkü ne
// sayfalama, ne arama, ne de ağ isteği hatası yönetmemiz gerekiyor. Tek işi:
// Room'daki favori listesini İZLEYİP ekrana sunmak, bir de favoriden
// ÇIKARMA işlemini yapmak.
class FavoritesViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    // "repository.getFavorites()" -> Room'daki TÜM favori karakterleri
    // CANLI bir akış (Flow) olarak veriyordu (CharacterRepositoryImpl'de
    // yazdığımız fonksiyon, hatırlarsan dao.getAllFavorites() üzerinden
    // Entity -> Character dönüşümü yapıp bize temiz bir Flow<List<Character>>
    // sunuyordu).
    //
    // ".stateIn(...)" -> BURASI YENİ bir kavram: bir Flow'u, StateFlow'a
    // ÇEVİRMENİN yolu. Compose'un collectAsState() ile izleyebilmesi için
    // StateFlow'a ihtiyacımız var (CharacterListViewModel'de _state ve state
    // ikilisiyle bunu ELLE yapmıştık - burada ise repository'den DOĞRUDAN
    // gelen Flow'u kullandığımız için, MutableStateFlow yazmak yerine
    // stateIn() ile OTOMATİK bir StateFlow üretiyoruz).
    //
    // "scope = viewModelScope" -> bu StateFlow'un, ViewModel'in ömrüne
    // bağlı olarak yaşamasını/ölmesini sağlıyor.
    //
    // "started = SharingStarted.WhileSubscribed(5000)" -> "bu Flow'u SADECE
    // birileri İZLERKEN aktif tut, kimse izlemiyorsa 5 saniye sonra durdur"
    // demek. Bu, kullanıcı ekrandan çıkıp geri geldiğinde (örn. başka sekmeye
    // geçip dönünce) gereksiz yere Room sorgusunu SIFIRDAN başlatmayı önleyen
    // küçük bir performans optimizasyonu - 5 saniyelik "tolerans süresi"
    // sayesinde hızlı ekran geçişlerinde Flow yeniden başlamıyor.
    //
    // "initialValue = emptyList()" -> Room'dan İLK veri gelene kadar
    // (ki bu çok hızlı olur ama YİNE DE bir an vardır), ekranın göstereceği
    // BAŞLANGIÇ değeri - boş liste.
    val favorites: StateFlow<List<Character>> = repository.getFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Favoriler ekranındaki bir karta da kalp butonu koyacağız (zaten
    // favoride olduğu için tıklanınca ÇIKARACAK). Bu fonksiyon, doğrudan
    // "çıkar" diyor - CharacterListViewModel'deki gibi toggle (ekle/çıkar
    // karar verme) mantığına gerek yok, çünkü bu ekrandaki HER karakter
    // zaten favoride.
    fun onRemoveFavoriteClick(character: Character) {
        viewModelScope.launch {
            repository.removeFavorite(character)
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) NEDEN BU VIEWMODEL, CharacterListViewModel GİBİ "_state / state" İKİLİSİ
    (MutableStateFlow + StateFlow) KULLANMIYOR?
    CharacterListViewModel'de state karmaşıktı (isLoading, error, sayfa
    numarası, arama metni, filtre... hepsi TEK bir CharacterListState
    içindeydi) - bu yüzden ELLE yönetilen bir MutableStateFlow gerekiyordu.
    Burada ise TEK ihtiyacımız "favori listesi" - repository zaten bunu
    HAZIR bir Flow olarak veriyor, biz sadece onu stateIn() ile Compose'un
    anlayacağı bir StateFlow'a ÇEVİRİYORUZ. Daha az kod, daha basit - iş
    ne kadar basitse, çözüm de o kadar basit olmalı (over-engineering'den
    kaçınmak da iyi bir pratiktir).

 2) "stateIn()" TAM OLARAK NE YAPIYOR, "collectAsState()" İLE KARIŞTIRMAYALIM?
    stateIn(), ViewModel TARAFINDA, bir Flow'u StateFlow'a çeviren bir
    fonksiyon - ViewModel katmanında çalışır. collectAsState() ise Compose
    TARAFINDA, bir StateFlow'u (veya Flow'u) Compose'un İZLEYEBİLECEĞİ bir
    "State" nesnesine çeviren, FARKLI bir fonksiyon. İkisi FARKLI katmanlarda,
    FARKLI amaçlarla kullanılıyor - biri "Flow'u StateFlow yap" (ViewModel'de),
    diğeri "StateFlow'u Compose State yap" (Compose ekranında).

 3) BU EKRAN, LİSTE EKRANIYLA AYNI Room VERİTABANINI Mı KULLANIYOR?
    Evet - Koin'deki single<AppDatabase> tanımı sayesinde, UYGULAMA BOYUNCA
    TEK BİR AppDatabase nesnesi var (Singleton). Hem CharacterListViewModel
    hem FavoritesViewModel, AYNI CharacterRepository'yi (dolayısıyla AYNI
    veritabanını) kullanıyor. Bu yüzden liste ekranında favoriye eklediğin
    bir karakter, favoriler ekranına geçtiğinde OTOMATİK orada da görünür -
    hiçbir manuel senkronizasyon koduna gerek kalmadan.

 ===========================================================
*/