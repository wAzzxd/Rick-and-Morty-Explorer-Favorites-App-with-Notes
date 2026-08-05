package com.example.rickandmortyproject.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// "@Dao" -> Room'a "bu interface, veritabanıyla ilgili SORGULARI tarif
// ediyor" diyoruz. RickAndMortyApi'yi Retrofit'e nasıl tarif ettiysek,
// bunu da Room'a AYNI mantıkla tarif ediyoruz - gerçek kodu Room, arka
// planda bizim yerimize üretiyor (KSP sayesinde, Gradle'a
// eklediğimiz room-compiler tam olarak bunu yapıyordu).
@Dao
interface FavoriteCharacterDao {

    // "@Insert" -> veritabanına YENİ bir satır eklemek için.
    // "onConflict = OnConflictStrategy.REPLACE" -> eğer AYNI id'ye sahip
    // bir kayıt zaten VARSA, hata vermek yerine ÜZERİNE YAZ demek.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(character: FavoriteCharacterEntity)

    // "@Delete" -> verilen nesneyle EŞLEŞEN satırı veritabanından SİLER
    // (Room, hangi satırı sileceğini @PrimaryKey'e bakarak anlıyor).
    @Delete
    suspend fun deleteFavorite(character: FavoriteCharacterEntity)

    // "@Query" -> Room'un kendi SQL benzeri sorgu dilini kullanıyoruz.
    // Bu sorgu, TÜM favori karakterleri getirir.
    //
    // Dönüş tipi "Flow<List<FavoriteCharacterEntity>>" -> DİKKAT, "suspend
    // fun" DEĞİL bu sefer! Flow, TEK SEFERLİK bir cevap değil, SÜREKLİ
    // GÜNCEL kalan bir "akış". Veritabanında BİR DEĞİŞİKLİK olduğunda
    // (favoriye ekleme/çıkarma), bu Flow'u izleyen HERKES OTOMATİK olarak
    // yeni listeyi alır - biz elle "listeyi yenile" demek ZORUNDA kalmıyoruz.
    @Query("SELECT * FROM favorite_characters")
    fun getAllFavorites(): Flow<List<FavoriteCharacterEntity>>

    // Belirli bir karakterin favoride olup olmadığını kontrol etmek için.
    // "SELECT EXISTS(...)" -> SQL'de "bu sorgu sonuç döndürüyor mu" diye
    // sorup true/false döndüren bir kalıp - tüm satırı çekmek yerine
    // sadece "var mı yok mu" bilgisini almak DAHA HIZLI.
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_characters WHERE id = :characterId)")
    fun isFavorite(characterId: Int): Flow<Boolean>
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) BU DA BİR INTERFACE - RickAndMortyApi'YE ÇOK BENZİYOR, NEDEN?
    Retrofit VE Room, İKİSİ DE aynı tasarım deseni (design pattern) kullanıyor:
    "sen SADECE NE istediğini interface üzerinden TARİF ET, GERÇEK kodu ben
    (kütüphane) senin için ÜRETEYİM." Retrofit bunu ağ istekleri için, Room
    veritabanı sorguları için yapıyor - ikisi de "annotation + interface"
    kalıbını kullanarak bizim YAZACAĞIMIZ kod miktarını CİDDİ şekilde azaltıyor.

 2) NEDEN "getAllFavorites()" BİR Flow DÖNDÜRÜYOR, DİĞERLERİ GİBİ "suspend
    fun" DEĞİL?
    insertFavorite ve deleteFavorite, TEK SEFERLİK işlemler ("şunu ekle",
    "şunu sil") - bu yüzden suspend fun yeterli. Ama getAllFavorites,
    "favori listesini SÜREKLİ İZLE" anlamına geliyor - kullanıcı favorilerim
    sekmesindeyken, BAŞKA bir ekranda (liste ekranında) bir karakteri
    favoriye eklerse, favoriler ekranının BUNU OTOMATİK görmesini istiyoruz.
    Flow, tam olarak bu "canlı, sürekli güncel veri akışı" ihtiyacını
    karşılıyor.
 ===========================================================
*/