package com.example.rickandmortyproject.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmortyproject.domain.model.Character
import com.example.rickandmortyproject.domain.repository.CharacterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

    // "MutableStateFlow<String>" -> arama kutusundaki metni AYRI bir Flow'da
    // tutuyoruz (ana state'in içinde de var ama burada debounce UYGULAMAK
    // için AYRICA tutuyoruz). Kullanıcı her harf yazdığında bu Flow'a yeni
    // değer "emit" edilecek.
    private val searchQueryFlow = MutableStateFlow("")

    // "Job?" -> sayfalama coroutine'ini elle İPTAL edebilmek için referansını
    // tutuyoruz. Arama/filtre değiştiğinde, DEVAM EDEN eski bir yükleme
    // varsa onu iptal edip TEMİZ bir şekilde yeni aramaya başlamak istiyoruz.
    private var loadJob: Job? = null

    // "init { }" -> bu ViewModel ilk oluşturulduğu anda (ekran ilk açıldığında)
    // otomatik çalışan blok. Burada ilk sayfayı hemen çekmeye başlıyoruz.
    init {
        loadCharacters()

        // "searchQueryFlow" Flow'unu burada İZLEMEYE başlıyoruz.
        // ".debounce(300)" -> kullanıcı YAZMAYI BIRAKTIKTAN 300 milisaniye
        // sonra, hâlâ yeni bir harf gelmediyse, DEĞERİ bir alt adıma geçirir.
        // Yani "r-i-c-k" hızlıca yazılırsa, ARADAKİ değerler (r, ri, ric)
        // İPTAL edilir, sadece SON DEĞER ("rick") 300ms sessizlikten sonra geçer.
        //
        // ".distinctUntilChanged()" -> bir önceki emit edilen değerle AYNIYSA
        // (örn. kullanıcı bir harf yazıp SİLİP AYNI harfi tekrar yazdıysa),
        // TEKRAR işleme almaz - gereksiz aynı isteği önler.
        //
        // ".onEach { query -> ... }" -> debounce ve distinctUntilChanged'den
        // GEÇEN her yeni değer için bu bloğu çalıştırır - burada ARAMAYI
        // gerçekten TETİKLİYORUZ.
        //
        // ".launchIn(viewModelScope)" -> bu Flow zincirini viewModelScope
        // içinde ÇALIŞTIRMAYA başlatır. ViewModel yok edildiğinde bu da
        // otomatik iptal edilir (aynı viewModelScope.launch gibi güvenli).
        searchQueryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                _state.update { it.copy(searchQuery = query) }
                resetAndReload()
            }
            .launchIn(viewModelScope)

        // "repository.getFavorites()" -> Room'daki favori listesini CANLI
        // olarak izliyoruz (Flow olduğu için). Favoriler DEĞİŞTİĞİNDE
        // (başka bir ekrandan bile eklense/çıkarılsa) bu blok OTOMATİK
        // tekrar çalışır.
        //
        // ".map { favorites -> favorites.map { it.id }.toSet() }" -> gelen
        // TAM Character listesinden, sadece id'lerini alıp bir Set'e
        // çeviriyoruz. Set kullanmamızın sebebi: "bu id BU KÜMEDE var mı"
        // kontrolü (contains), bir List'e göre ÇOK DAHA HIZLI çalışır.
        //
        // ".onEach { favoriteIds -> ... }" -> her yeni favoriler kümesi
        // geldiğinde, ana state'i güncelliyoruz.
        //
        // ".launchIn(viewModelScope)" -> yine ViewModel'in ömrüne bağlı,
        // GÜVENLİ bir şekilde çalışan bir izleyici başlatıyoruz.
        repository.getFavorites()
            .map { favorites -> favorites.map { it.id }.toSet() }
            .onEach { favoriteIds ->
                _state.update { it.copy(favoriteIds = favoriteIds) }
            }
            .launchIn(viewModelScope)
    }

    // Compose ekranındaki TextField, kullanıcı her harf yazdığında bu
    // fonksiyonu çağıracak. Burada state'i DOĞRUDAN güncellemiyoruz (yazı
    // kutusunun GÖRÜNÜR metni ayrı tutuluyor, arama TETİKLEME işini
    // searchQueryFlow üstleniyor) - böylece kullanıcı yazarken TextField
    // asla "geç kalmış" görünmüyor, ama gerçek arama isteği debounce'lu.
    fun onSearchQueryChanged(query: String) {
        // Kullanıcının o an TAM OLARAK ne yazdığını GÖRSEL olarak hemen
        // gösterebilmek için state'i anında güncelliyoruz.
        _state.update { it.copy(searchQuery = query) }
        // Ama GERÇEK aramayı searchQueryFlow üzerinden, debounce'a TABİ
        // tutarak tetikliyoruz.
        searchQueryFlow.value = query
    }

    // Chip'lerden birine tıklanınca çağrılacak. Aynı chip'e TEKRAR
    // tıklanırsa filtreyi KALDIRIYORUZ (toggle davranışı).
    fun onStatusFilterChanged(status: String?) {
        val newFilter = if (_state.value.statusFilter == status) null else status
        _state.update { it.copy(statusFilter = newFilter) }
        resetAndReload()
    }

    // Arama/filtre DEĞİŞTİĞİNDE çağrılan yardımcı fonksiyon: state'i
    // BAŞLANGIÇ durumuna döndürüp (liste boşalır, sayfa 1'e döner),
    // yeni kritere göre BAŞTAN yükleme başlatıyoruz.
    private fun resetAndReload() {
        // Önce, EĞER devam eden bir yükleme coroutine'i varsa, İPTAL ediyoruz -
        // aksi halde ESKİ arama sonucu, YENİ arama sonucundan SONRA gelip
        // ekranı YANLIŞ veriyle doldurabilirdi (race condition).
        loadJob?.cancel()

        _state.update {
            it.copy(
                characters = emptyList(),
                currentPage = 1,
                endReached = false,
                error = null,
                // "isLoading = false, isLoadingMore = false" -> BU İKİ SATIRI
                // YENİ EKLEDİK. Neden gerekliydi: loadJob.cancel() ile durdurduğumuz
                // coroutine, CancellationException'ı artık "throw e" ile tekrar
                // fırlattığı için (önceki mesajdaki düzeltme), KENDİ isLoading/
                // isLoadingMore'u false yapan koduna hiç ULAŞAMADAN duruyor. Eğer
                // burada ELLE sıfırlamasaydık, isLoading SONSUZA KADAR true kalırdı,
                // bu da loadCharacters()'ın en başındaki "if (isLoading || ...) return"
                // koruması yüzünden YENİ yüklemenin hiç BAŞLAYAMAMASINA yol açardı -
                // tam olarak yaşadığın "sürekli dönen yükleniyor ikonu" sorunu buydu.
                isLoading = false,
                isLoadingMore = false
            )
        }
        loadCharacters()
    }

    // Kullanıcı listenin sonuna gelince (infinite scroll) bu fonksiyon tekrar çağrılacak.
    // Ayrıca resetAndReload() içinden de (arama/filtre değiştiğinde) çağrılıyor.
    fun loadCharacters() {
        // Zaten yükleniyorsa veya son sayfaya gelindiyse tekrar istek atma (gereksiz/çakışan istekleri önlüyoruz)
        if (_state.value.isLoading || _state.value.isLoadingMore || _state.value.endReached) return

        // "loadJob = viewModelScope.launch { }" -> başlattığımız coroutine'in
        // referansını SAKLIYORUZ, böylece resetAndReload() içinde onu
        // iptal edebiliyoruz. Bu, COROUTINE'İN kendisi UI thread'i bloklamadan
        // arka planda çalışır, ViewModel yok edildiğinde OTOMATİK iptal edilir,
        // hafıza sızıntısı (memory leak) olmaz - viewModelScope'un sağladığı güvenlik budur.
        loadJob = viewModelScope.launch {
            val isFirstPage = _state.value.currentPage == 1

            // "_state.update { it.copy(...) }" -> mevcut state'i alıp, SADECE
            // belirttiğimiz alanları değiştirip yeni bir state üretiyoruz
            // (data class'ın bize verdiği copy() fonksiyonu burada işe yarıyor).
            // isFirstPage'e göre ya "isLoading" ya da "isLoadingMore" true yapıyoruz,
            // ikisi farklı UI göstergeleri için (ilk yükleme = tam ekran gösterge,
            // sayfa sonu yükleme = listenin altında küçük bir progress).
            _state.update {
                if (isFirstPage) it.copy(isLoading = true, error = null)
                else it.copy(isLoadingMore = true)
            }

            try {
                // DİKKAT: artık sadece page değil, GÜNCEL arama metnini ve
                // filtreyi de repository'ye GEÇİYORUZ. Boş arama metnini
                // API'ye "name" olarak göndermemek için, boşsa null yapıyoruz
                // ("ifBlank { null }" -> metin boş veya sadece boşluksa null döner).
                val query = _state.value.searchQuery.ifBlank { null }
                val status = _state.value.statusFilter

                // Repository'den (interface üzerinden) veriyi istiyoruz.
                // Repository'nin arkasında Retrofit mi çalışıyor, bilmiyoruz, umurumuzda değil.
                val newCharacters = repository.getCharacters(
                    page = _state.value.currentPage,
                    name = query,
                    status = status
                )

                _state.update {
                    it.copy(
                        // Eski listeye YENİ gelenleri EKLİYORUZ (infinite scroll mantığı,
                        // sayfa değiştikçe listeyi SIFIRLAMIYORUZ, üstüne ekliyoruz).
                        // NOT: arama/filtre değiştiğinde resetAndReload() zaten listeyi
                        // BOŞALTTIĞI için, burada yine "ekleme" mantığı doğru çalışıyor.
                        characters = it.characters + newCharacters,
                        isLoading = false,
                        isLoadingMore = false,
                        currentPage = it.currentPage + 1,
                        // Eğer API'den boş liste geldiyse, son sayfaya gelmişiz demektir
                        endReached = newCharacters.isEmpty()
                    )
                }
            } catch (e: Exception) {
                // Ağ hatası (internet yok, sunucu cevap vermedi, rate limit vb.)
                // burada yakalanıyor. Kullanıcıya HAM/teknik hata mesajı ("HTTP 429")
                // göstermek yerine, hatayı TÜRÜNE göre ayırt edip daha ANLAŞILIR bir
                // mesaj üretiyoruz.
                //
                // "e is retrofit2.HttpException" -> Kotlin'in "is" anahtar kelimesi,
                // bir nesnenin HANGİ TÜRDEN olduğunu kontrol etmemizi sağlar (type
                // checking). Retrofit, sunucudan 200 (başarılı) DIŞINDA bir HTTP kodu
                // geldiğinde (404, 429, 500 gibi), hatayı bu ÖZEL exception türüyle
                // fırlatır. ".code()" ile TAM OLARAK hangi HTTP kodu geldiğini
                // (429, 500 vb.) öğrenebiliyoruz.


                if (e is kotlinx.coroutines.CancellationException) throw e
                // "CancellationException" -> Kotlin Coroutines'in kendi İÇ mekanizması.
                // Bir coroutine "cancel()" ile durdurulduğunda (bizim resetAndReload()
                // içinde loadJob?.cancel() dediğimizde olduğu gibi), Kotlin bunu bu
                // ÖZEL exception türüyle sinyalliyor. Bu bir HATA DEĞİL, coroutine'in
                // NORMAL şekilde durduğunu belirten bir mekanizma.
                //
                // "throw e" -> BU exception türünü YAKALAMIYORUZ, olduğu gibi TEKRAR
                // fırlatıyoruz. Bu ÇOK ÖNEMLİ bir kural: CancellationException'ı asla
                // "yutmamalıyız" (yani sessizce yakalayıp yok saymamalıyız) - aksi halde
                // Kotlin'in "yapılandırılmış eşzamanlılık" (structured concurrency)
                // sistemi bozulur, coroutine'lerin DÜZGÜN iptal edilip edilmediğini
                // takip edemez hale geliriz. Kısacası: "gerçek hatalar"ı yakala, ama
                // "ben zaten iptal edildim" sinyaline DOKUNMA, bırak yoluna devam etsin.


                val errorMessage = when {
                    e is retrofit2.HttpException && e.code() == 404 ->
                        // API, arama sonucunda HİÇ karakter bulamazsa 404
                        // döndürüyor (bu Rick and Morty API'sinin kendine
                        // özgü bir davranışı - normalde 404 "sayfa yok"
                        // anlamına gelir, ama burada "sonuç yok" demek).
                        // Bunu HATA gibi değil, "boş sonuç" gibi ele alıyoruz.
                        null

                    e is retrofit2.HttpException && e.code() == 429 ->
                        "Çok hızlı istek attık, birkaç saniye bekleyip tekrar deneyin."

                    e is retrofit2.HttpException && e.code() in 500..599 ->
                        "Sunucuda geçici bir sorun var, birazdan tekrar deneyin."

                    e is java.io.IOException ->
                        // "IOException" -> internet bağlantısı KOPUKSA (sunucuya
                        // hiç ULAŞILAMADIYSA) fırlatılan tür, HttpException'dan farklı -
                        // HttpException sunucudan CEVAP geldiğinde (ama hata koduyla),
                        // IOException ise sunucuya hiç ULAŞILAMADIĞINDA oluşur.
                        "İnternet bağlantınızı kontrol edin."

                    else ->
                        e.message ?: "Bilinmeyen bir hata oluştu"
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        // 404 (sonuç yok) durumunda error'u null bırakıp,
                        // endReached'i true yapıyoruz - böylece ekranda
                        // "hata" değil, sadece BOŞ bir liste görünür.
                        error = errorMessage,
                        endReached = errorMessage == null || it.endReached
                    )
                }
            }
        }
    }

    // Kullanıcı "Tekrar Dene" butonuna bastığında çağrılacak.
    fun retry() {
        _state.update { it.copy(error = null) }
        loadCharacters()
    }

    // Kalp butonuna basılınca çağrılacak. Karakter ZATEN favorideyse
    // ÇIKARIYORUZ, değilse EKLİYORUZ - "toggle" (aç/kapat) mantığı.
    // Not: state'i BURADA elle güncellemiyoruz! Çünkü yukarıdaki init{}
    // bloğundaki repository.getFavorites() izleyicisi, Room'daki değişikliği
    // OTOMATİK yakalayıp state'i kendisi güncelleyecek - biz sadece
    // Room'a "ekle/çıkar" komutunu veriyoruz, gerisi kendiliğinden akıyor.
    fun onFavoriteClick(character: Character) {
        viewModelScope.launch {
            val isCurrentlyFavorite = _state.value.favoriteIds.contains(character.id)
            if (isCurrentlyFavorite) {
                repository.removeFavorite(character)
            } else {
                repository.addFavorite(character)
            }
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR - MVVM & STATE ====================

 1) MVVM BURADA NASIL İŞLİYOR?
    - Model: domain.model.Character ve CharacterRepository (veriyi temsil eden/getiren katman)
    - ViewModel: bu sınıf - state'i tutuyor, iş mantığını (sayfalama, arama, filtre,
      hata yönetimi, favoriler) yürütüyor
    - View: CharacterListScreen.kt - sadece "state.value.characters"a bakıp ekrana
      çizecek, hiçbir iş mantığı (network, hata yönetimi, debounce, veritabanı)
      İÇERMEYECEK.
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
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - ARAMA & DEBOUNCE ====================

 1) NEDEN "searchQueryFlow" DİYE AYRI BİR MutableStateFlow VAR, STATE'İN
    İÇİNDEKİ "searchQuery" YETMİYOR MU?
    State'in içindeki searchQuery, SADECE "ekranda GÖRÜNEN metin ne" bilgisini
    tutuyor - TextField'ın anlık, GECİKMESİZ görüntüsü için. searchQueryFlow ise
    "GERÇEK arama isteğini ne zaman ATACAĞIZ" kararını debounce ile YÖNETMEK
    için var. İkisini AYIRMAMIZIN sebebi: kullanıcı yazarken TextField'ın
    GECİKMELİ görünmesini istemiyoruz (her harfte state.searchQuery ANINDA
    güncelleniyor), ama ASIL network isteğinin 300ms beklemesini istiyoruz.

 2) "resetAndReload()" NEDEN GEREKLİ, NEDEN DİREKT loadCharacters() ÇAĞIRMIYORUZ?
    loadCharacters(), MEVCUT sayfa numarasından DEVAM edip listeye EKLEME
    yapacak şekilde tasarlandı (infinite scroll için). Ama arama/filtre
    DEĞİŞTİĞİNDE, eski sonuçların üstüne YENİ arama sonucunu EKLEMEK değil,
    listeyi TAMAMEN SIFIRLAYIP baştan başlamak istiyoruz - resetAndReload()
    tam olarak bunu yapıyor: characters'ı boşaltıp, currentPage'i 1'e
    döndürüp, SONRA loadCharacters()'ı çağırıyor.

 3) "loadJob?.cancel()" NEDEN ÖNEMLİ?
    Diyelim kullanıcı "r" yazdı, 300ms'den ÖNCE de "rick" yazmayı bitirdi.
    debounce zaten "r" için isteği İPTAL edip sadece "rick" için tetikleyecek.
    Ama EĞER kullanıcı ÇOK YAVAŞ yazsa ve arada bir istek GERÇEKTEN atılmışsa,
    sonra YENİ bir arama/filtre değişikliği olduğunda, ESKİ isteğin CEVABI
    YENİ aramadan SONRA gelirse ekranı YANLIŞ (eski) veriyle doldurabilirdi.
    Buna "race condition" (yarış durumu) denir. loadJob.cancel() ile HER
    resetAndReload() öncesi eski coroutine'i güvenle iptal ediyoruz.

 4) API'DEN 404 GELMESİ NEDEN HATA OLARAK GÖSTERİLMİYOR?
    Rick and Morty API'si, arama sonucunda HİÇBİR karakter bulunamazsa
    404 (Not Found) döndürüyor - bu API'ye ÖZGÜ bir davranış, normalde
    404 "sayfa/adres yok" anlamına gelir. Biz bunu YAKALAYIP, kullanıcıya
    kırmızı bir hata mesajı YERİNE, sadece BOŞ bir liste gösteriyoruz
    (errorMessage = null, endReached = true) - çünkü "sonuç bulunamadı"
    aslında bir HATA değil, geçerli bir arama SONUCUDUR.
 ===========================================================
*/

/*
 ==================== KAVRAMSAL NOTLAR - FAVORİLER ====================

 1) NEDEN "favoriteIds: Set<Int>" TUTUYORUZ, HER Character'IN İÇİNE
    "isFavorite: Boolean" ALANI EKLEMEDİK?
    Character, domain modelimiz - API'den gelen SAF veriyi temsil ediyor,
    favori olup olmadığı bilgisi ONUN sorumluluğunda değil (Single
    Responsibility). Bunun yerine, "favoride olan id'lerin kümesi" diye
    AYRI bir bilgi tutup, ekranda "bu id kümede var mı" diye kontrol ederek
    kalp ikonunu dolduruyoruz - CharacterCard'a bakarsan, isFavorite
    parametresini ZATEN dışarıdan (state.favoriteIds.contains(character.id)
    şeklinde) alacak.

 2) NEDEN state GÜNCELLEMESİNİ "onFavoriteClick" İÇİNDE ELLE YAPMIYORUZ?
    Çünkü Room'daki DAO fonksiyonumuz (getAllFavorites) bir Flow döndürüyordu -
    yani Room'un KENDİSİ, veritabanında bir DEĞİŞİKLİK olduğunda bunu bize
    OTOMATİK haber veriyor. Biz sadece "ekle" ya da "çıkar" komutunu Room'a
    iletiyoruz, Room değişikliği yapınca Flow tetikleniyor, init{} bloğundaki
    izleyicimiz bunu YAKALAYIP state'i GÜNCELLIYOR. Bu, "tek gerçek kaynak"
    (single source of truth) prensibinin güzel bir örneği: favori bilgisinin
    TEK sahibi Room, ViewModel sadece onu YANSITIYOR.

 3) BU YAKLAŞIMIN FAYDASI NE, GERÇEK HAYATTA NE İŞE YARAR?
    Diyelim kullanıcı liste ekranındayken bir karakteri favoriye ekledi.
    Sonra favoriler sekmesine geçti - orada da AYNI Room veritabanını
    izleyen BAŞKA bir ViewModel olacak, o da OTOMATİK olarak yeni favoriyi
    görecek, çünkü ikisi de AYNI "tek gerçek kaynağı" (Room) izliyor. Elle
    "iki ekranı da senkronize et" diye bir şey yazmamıza HİÇ gerek kalmıyor.

 4) SIRADA
    CharacterListScreen.kt'de, isFavorite = false ve onFavoriteClick = { }
    sabit değerlerini, GERÇEK state.favoriteIds.contains(character.id) ve
    viewModel.onFavoriteClick(character) çağrılarıyla değiştireceğiz.
 ===========================================================
*/