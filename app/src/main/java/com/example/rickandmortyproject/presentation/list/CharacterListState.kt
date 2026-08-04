package com.example.rickandmortyproject.presentation.list

import com.example.rickandmortyproject.domain.model.Character

// Bu sınıf, "liste ekranı şu an TAM OLARAK ne durumda?" sorusunun cevabını
// TEK BİR YERDE topluyor. ViewModel bu sınıfın bir nesnesini üretip günceller,
// Compose ekranı da bu nesneye bakıp "ekranda ne göstereceğine" karar verir.
//
// "data class" seçtik çünkü bu sadece VERİ taşıyan bir kutu - kendine ait
// bir davranışı (fonksiyonu) yok, sadece alanları var. data class bize
// otomatik copy(), equals(), toString() veriyor (Character modelinde
// anlattığımız aynı sebepler burada da geçerli).
data class CharacterListState(

    // Şu ana kadar API'den başarıyla çekilip biriktirilen TÜM karakterlerin listesi.
    // Başlangıçta boş liste (emptyList()) - henüz hiçbir şey yüklenmedi demek.
    // Infinite scroll'da her yeni sayfa geldiğinde, bu liste SIFIRLANMAZ,
    // üzerine EKLENİR (ViewModel'de "it.characters + newCharacters" satırını
    // hatırlarsan, orada bu ekleme işlemi yapılıyor).
    val characters: List<Character> = emptyList(),

    // Ekran İLK açıldığında, henüz hiç veri yokken true olur.
    // Compose tarafında: "isLoading true ise TAM EKRAN shimmer (iskelet) göster,
    // henüz hiç kart yok" mantığı için kullanılacak.
    // Varsayılan (default) değeri false - state ilk oluştuğunda otomatik
    // yüklemeye başlamadan önceki "boş" hali temsil eder, gerçek true değeri
    // ViewModel'in loadCharacters() fonksiyonu çalıştığında atanacak.
    val isLoading: Boolean = false,

    // isLoading'den FARKLI bir amaç için var: kullanıcı listeyi aşağı kaydırıp
    // SONRAKİ sayfayı yüklerken true olur. Bu sırada ekranda zaten karakterler
    // GÖRÜNÜYOR (characters listesi dolu), sadece listenin EN ALTINDA küçük
    // bir "yükleniyor" göstergesi (örn. küçük bir progress bar) çıkacak.
    // İkisini ayrı tutmasaydık, sayfa sonu yüklerken de yanlışlıkla TÜM ekranı
    // kaplayan shimmer'ı tekrar gösterebilirdik - bu kullanıcı deneyimini bozardı.
    val isLoadingMore: Boolean = false,

    // Bir hata oluşursa (internet yok, sunucu 500 döndü vb.) hata mesajı BURAYA yazılır.
    // "String?" -> sonundaki "?" bunun NULL OLABİLECEĞİNİ belirtir. Hata yoksa
    // bu alan null kalır. Compose tarafında "error != null ise Retry butonlu
    // hata ekranı göster, null ise normal listeyi göster" mantığı kuracağız.
    val error: String? = null,

    // API'nin sayfalama sistemi 1'den başlıyor (Rick and Morty API'si page=1, page=2...
    // şeklinde çalışıyor). Bu alan, "bir sonraki istekte HANGİ sayfayı çekeceğiz"
    // bilgisini tutuyor. Her başarılı istekten sonra ViewModel bunu 1 artıracak
    // (currentPage = it.currentPage + 1), böylece bir sonraki loadCharacters()
    // çağrısı otomatik olarak doğru sayfayı ister.
    val currentPage: Int = 1,

    // API'den boş bir sonuç listesi geldiğinde (yani "results" alanı boşsa),
    // bu, ARTIK ÇEKİLECEK YENİ KARAKTER KALMADIĞI anlamına gelir - son sayfaya
    // ulaşılmış demektir. Bu true olduğunda, ViewModel bir daha ASLA yeni istek
    // atmayacak (loadCharacters() fonksiyonunun en başındaki "if" kontrolünü
    // hatırlarsan, endReached true ise fonksiyon hemen return ile çıkıyordu).
    // Bu olmasaydı, kullanıcı listenin en altına her geldiğinde gereksiz yere
    // sürekli boş sonuç dönen isteklere devam ederdik.
    val endReached: Boolean = false,

    val searchQuery: String = "",      // kullanıcının arama kutusuna yazdığı anlık metin

    val statusFilter: String? = null   // seçili durum filtresi: "Alive", "Dead", "unknown" ya da null (filtre yok)
)

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) "UI STATE PATTERN" NEDİR, NEDEN BÖYLE BİR YAKLAŞIM VAR?
    Modern Android geliştirmede, bir ekranın TÜM görsel durumunu ayrı ayrı
    değişkenlerle (var isLoading, var characters, var error gibi dağınık
    şekilde) değil, TEK bir data class içinde toplamak çok yaygın bir pattern'dir.
    Faydası: ekranın o anki durumu her zaman "tek bir fotoğraf" gibi tutarlı
    olur - state'in bir parçasını güncellerken diğer parçaların eski kalıp
    kalmadığını ayrı ayrı kontrol etmek zorunda kalmayız, copy() ile hepsini
    bir arada, tek seferde güncelleriz.

 2) HER ALANIN BİR "VARSAYILAN DEĞERİ" (= emptyList(), = false, = null, = 1)
    OLMASININ FAYDASI NE?
    Bu sayede ViewModel'de "CharacterListState()" yazarak, HİÇBİR parametre
    vermeden, "ekranın en başlangıç, hiçbir şey olmamış hali"ni kolayca
    oluşturabiliyoruz (private val _state = MutableStateFlow(CharacterListState())
    satırında tam olarak bunu yaptık). Varsayılan değerler olmasaydı, her
    alanı elle doldurmamız gerekirdi.

 3) BU SINIF NEREDE KULLANILACAK?
    - CharacterListViewModel içinde: _state (yazılabilir) ve state (sadece
      okunabilir) olarak tutulacak, ViewModel bu state'i sürekli günceller.
    - Az sonra yazacağımız Compose ekranında: "val state by viewModel.state
      .collectAsState()" diyerek bu state'i İZLEYECEĞİZ, her değiştiğinde
      Compose ekranı OTOMATİK olarak yeniden çizilecek (recomposition).

 4) NEDEN "var" DEĞİL DE HEP "val" KULLANDIK?
    Character modelinde anlattığımız aynı mantık: bu alanların kendisi
    (characters, isLoading vb.) SONRADAN elle değiştirilemez olsun istiyoruz.
    Bir state'i "güncellemek" istediğimizde, VAR OLAN nesneyi değiştirmiyoruz,
    copy() ile TAMAMEN YENİ bir state nesnesi üretiyoruz. Bu, hataları önleyen
    (immutability) modern Kotlin/Compose pratiğidir.
 ===========================================================
*/