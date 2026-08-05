package com.example.rickandmortyproject.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// "@Entity" -> Room'a "bu sınıf bir VERİTABANI TABLOSUNU temsil ediyor" diyoruz.
// "tableName" ile tablonun veritabanındaki gerçek adını belirliyoruz.
@Entity(tableName = "favorite_characters")
data class FavoriteCharacterEntity(
    // "@PrimaryKey" -> bu alanın, tablodaki HER SATIRI BENZERSİZ şekilde
    // tanımlayan anahtar olduğunu belirtiyoruz. Karakterin API'deki id'sini
    // AYNEN kullanıyoruz - bu sayede "bu karakter zaten favoride mi" kontrolü
    // kolayca bu id ile yapılabiliyor.
    @PrimaryKey
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val gender: String,
    val imageUrl: String,
    val origin: String,
    val episodeCount: Int
)

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) BU SINIF, domain.model.Character İLE NEDEN AYNI GÖRÜNÜYOR AMA AYRI?
    Aynı Clean Architecture mantığı: bu sınıf ROOM'A ÖZGÜ (@Entity, @PrimaryKey
    annotation'ları var), yani "data" katmanına ait. domain.model.Character ise
    Room'dan, Retrofit'ten TAMAMEN habersiz, saf bir model. Yarın Room'u başka
    bir veritabanı kütüphanesiyle değiştirsek, SADECE bu dosya ve ona bağlı
    DAO/Database değişir - domain ve presentation katmanları ETKİLENMEZ.

 2) NEDEN Character MODELİNİ DOĞRUDAN KULLANMADIK, AYRI BİR ENTITY YAZDIK?
    Room, bir sınıfın veritabanı tablosu olabilmesi için @Entity, @PrimaryKey
    gibi kendi annotation'larını İSTİYOR. Eğer domain.model.Character'a bu
    annotation'ları eklemiş olsaydık, domain katmanını Room'a BAĞIMLI hale
    getirmiş olurduk - bu da Clean Architecture'ın "domain hiçbir şeye
    bağımlı değildir" kuralını BOZARDI. Bu yüzden ayrı bir Entity yazıp,
    ileride (repository katmanında) Entity <-> Character dönüşümünü
    kendimiz yapacağız (DTO <-> Character dönüşümünü yaptığımız gibi).
 ===========================================================
*/