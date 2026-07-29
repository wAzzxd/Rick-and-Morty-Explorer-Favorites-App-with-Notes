package com.example.rickandmortyproject.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// "ViewModel()" sınıfından miras alıyoruz - bu bize Android'in ViewModel
// yaşam döngüsü yönetimini (ekran döndürülünce silinmeme gibi) veriyor.
//
// "repository: CharacterRepository" -> DİKKAT: burada somut CharacterRepositoryImpl
// değil, INTERFACE alıyoruz. Koin bize gerçek implementasyonu otomatik verecek,
// ViewModel bunun Retrofit mi Room mu olduğunu hiç bilmiyor (Dependency Inversion).
class CharacterListViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    // "MutableStateFlow" -> içi değişebilen, gözlemlenebilir bir "kutu".
    // "private" yaptık çünkü DIŞARIDAN (Compose ekranından) kimse bu kutunun
    // içeriğini DOĞRUDAN değiştiremesin, sadece ViewModel'in kendisi değiştirebilsin.
    private val _state = MutableStateFlow(CharacterListState())

    // "StateFlow" (mutable olmayan, sadece OKUNABİLİR hali) -> Compose ekranı
    // bunu "izleyecek" (observe), _state her değiştiğinde ekran otomatik güncellenecek.
    // Bu ikili yapı (_state / state) Kotlin'de çok yaygın bir pattern:
    // "dışarıya sadece okuma izni ver, yazma iznini sende tut."
    val state: StateFlow<CharacterListState> = _state

    // "init { }" -> bu ViewModel ilk oluşturulduğu anda (ekran ilk açıldığında)
    // otomatik çalışan blok. Burada ilk sayfayı hemen çekmeye başlıyoruz.
    init {
        loadCharacters()
    }

    // Kullanıcı listenin sonuna gelince (infinite scroll) bu fonksiyon tekrar çağrılacak.
    fun loadCharacters() {
        // Zaten yükleniyorsa veya son sayfaya gelindiyse tekrar istek atma (gereksiz/çakışan istekleri önlüyoruz)
        if (_state.value.isLoading || _state.value.isLoadingMore || _state.value.endReached) return

        // "viewModelScope.launch { }" -> bir COROUTINE başlatıyoruz. Bu blok
        // arka planda çalışır, UI thread'i bloklamaz. ViewModel yok edildiğinde
        // (ekran tamamen kapatıldığında) bu coroutine de OTOMATİK iptal edilir,
        // hafıza sızıntısı (memory leak) olmaz - viewModelScope'un sağladığı güvenlik budur.
        viewModelScope.launch {
            val isFirstPage = _state.value.currentPage == 1

            // "_state.update { it.copy(...) }" -> mevcut state'i alıp, SADECE
            // belirttiğimiz alanları değiştirip yeni bir state üretiyoruz
            // (data class'ın bize verdiği copy() fonksiyonu burada işe yarıyor).
            // isFirstPage'e göre ya "isLoading" ya da "isLoadingMore" true yapıyoruz,
            // ikisi farklı UI göstergeleri için (ilk yükleme = tam ekran shimmer,
            // sayfa sonu yükleme = listenin altında küçük bir progress).
            _state.update {
                if (isFirstPage) it.copy(isLoading = true, error = null)
                else it.copy(isLoadingMore = true)
            }

            try {
                // Repository'den (interface üzerinden) veriyi istiyoruz.
                // Repository'nin arkasında Retrofit mi çalışıyor, bilmiyoruz, umurumuzda değil.
                val newCharacters = repository.getCharacters(page = _state.value.currentPage)

                _state.update {
                    it.copy(
                        // Eski listeye YENİ gelenleri EKLİYORUZ (infinite scroll mantığı,
                        // sayfa değiştikçe listeyi SIFIRLAMIYORUZ, üstüne ekliyoruz)
                        characters = it.characters + newCharacters,
                        isLoading = false,
                        isLoadingMore = false,
                        currentPage = it.currentPage + 1,
                        // Eğer API'den boş liste geldiyse, son sayfaya gelmişiz demektir
                        endReached = newCharacters.isEmpty()
                    )
                }
            } catch (e: Exception) {
                // Ağ hatası (internet yok, sunucu cevap vermedi vb.) burada yakalanıyor.
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message ?: "Bilinmeyen bir hata oluştu"
                    )
                }
            }
        }
    }

    // Kullanıcı "Yeniden Dene" butonuna bastığında çağrılacak.
    fun retry() {
        _state.update { it.copy(error = null) }
        loadCharacters()
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) MVVM BURADA NASIL İŞLİYOR?
    - Model: domain.model.Character ve CharacterRepository (veriyi temsil eden/getiren katman)
    - ViewModel: bu sınıf - state'i tutuyor, iş mantığını (sayfalama, hata yönetimi) yürütüyor
    - View:  sonra yazacağımız Compose ekranı - sadece "state.value.characters"a
      bakıp ekrana çizecek, hiçbir iş mantığı (network, hata yönetimi) İÇERMEYECEK.
    Bu ayrımın faydası: View'ı (Compose kodunu) değiştirsek bile (örn. XML'e geçsek),
    ViewModel'in TEK SATIRI değişmez - iş mantığı UI'dan tamamen bağımsız.

 2) NEDEN "StateFlow", NEDEN DÜZ BİR "var characters: List<Character>" DEĞİL?
    Compose, bir değerin NE ZAMAN değiştiğini otomatik anlayıp ekranı yeniden
    çizebilmek (recomposition) için o değerin "gözlemlenebilir" olmasını ister.
    StateFlow, tam olarak bunu sağlıyor: Compose ekranı "collectAsState()" ile
    bu flow'u izleyecek, _state.update{} her çağrıldığında ekran KENDİLİĞİNDEN
    güncellenecek - bizim elle "ekranı yenile" demememiz gerekmiyor.

 3) NEDEN TEK BİR BÜYÜK "CharacterListState" SINIFI, AYRI AYRI DEĞİŞKENLER
    ("var isLoading", "var characters" gibi) DEĞİL?
    Tüm ekran durumunu TEK bir nesnede toplamak, state'in her zaman TUTARLI
    olmasını sağlar (örn. "isLoading true iken aynı zamanda error da dolu"
    gibi çelişkili durumları tek bir copy() çağrısında yönetebiliyoruz).
    Bu yaklaşıma "UI State pattern" denir, modern Android geliştirmede çok yaygın.

 4) SIRADA NE VAR?
    Bu ViewModel'i Koin'e tanıtmamız gerekiyor (appModule'e bir satır ekleyeceğiz),
    sonra presentation/list içine gerçek Compose ekranını (View'ı) yazıp bu
    ViewModel'e bağlayacağız.
 ===========================================================
*/