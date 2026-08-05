package com.example.rickandmortyproject.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// "@Database" -> Room'a "bu sınıf, veritabanının kendisi" diyoruz.
// "entities = [...]" -> bu veritabanında HANGİ tabloların olacağını
// listeliyoruz (şu an sadece bir tane: FavoriteCharacterEntity).
// "version = 1" -> veritabanı ŞEMASININ (yapısının) sürüm numarası.
// İleride tabloya YENİ bir alan eklersek, bu sayıyı artırıp Room'a
// "yapı değişti" haber vermemiz gerekecek (migration konusu, ileri seviye).
@Database(
    entities = [FavoriteCharacterEntity::class],
    version = 1
)
// "abstract class" -> Room, bu sınıfın GERÇEK implementasyonunu bizim
// yerimize KSP ile otomatik üretecek, biz sadece "İSKELETİ" (hangi DAO'lar
// olacağını) tarif ediyoruz - RickAndMortyApi interface'inde olduğu gibi,
// gerçek kod bizim yerimize üretiliyor.
abstract class AppDatabase : RoomDatabase() {
    // Bu fonksiyonu Room OTOMATİK dolduracak, bize DAO'nun ÇALIŞAN halini
    // verecek.
    abstract fun favoriteCharacterDao(): FavoriteCharacterDao
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) "abstract class" NE DEMEK, INTERFACE'TEN FARKI NE?
    "abstract class", interface'e ÇOK benzer (içindeki fonksiyonların
    gövdesi/kodu YOK, sadece TARİFİ var) ama Room'un TEKNİK GEREKSİNİMİ
    bu sınıfın "abstract class" olmasını İSTİYOR (RoomDatabase'den miras
    alması gerektiği için - Kotlin'de bir sınıf, SADECE BİR class'tan miras
    alabilir ama BİRDEN FAZLA interface'i implement edebilir; RoomDatabase
    zaten bir class olduğu için, biz de class olmak ZORUNDAYIZ).

 2) BU DOSYA NEREDE "GERÇEK BİR NESNEYE" DÖNÜŞECEK?
    RetrofitInstance.kt'de Retrofit'i "object + by lazy" ile nasıl TEK BİR
    nesne haline getirdiysek, AppDatabase'i de Koin module'ünde (AppModule.kt)
    Room.databaseBuilder(...) ile OLUŞTURACAĞIZ - bunu bir SONRAKİ adımda
    yapacağız.
 ===========================================================
*/