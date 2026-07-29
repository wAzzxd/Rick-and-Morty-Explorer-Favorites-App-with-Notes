package com.example.rickandmortyproject.domain.model

data class Character(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val gender: String,
    val imageUrl: String,
    val origin: String,
    val episodeCount: Int
)

//Kotlin'de bir sınıfı sadece veri taşımak için kullanıyorsak (Character'ın kendine ait bir
//"davranışı" yok, sadece id, isim, durum gibi bilgileri tutuyor), data class yazarız. Bunun
//getirileri:

//toString() — println(character) yazdığında otomatik olarak Character(id=1, name=Rick, ...)
// gibi okunaklı bir çıktı verir. Normal class'ta bunu elle yazman gerekirdi

//equals() — iki Character nesnesini == ile karşılaştırdığında, referansları değil (aynı
//bellek adresi mi), içindeki değerleri karşılaştırır. Yani id, name vs. hepsi aynıysa true döner.

//copy() — bir nesnenin sadece bir alanını değiştirip yenisini oluşturmanı sağlar:
//character.copy(status = "Dead") gibi. İleride favorilere ekleme/çıkarma gibi işlerde çok işimize yarayacak.




//val neden hep val, var değil?
//val = değiştirilemez (immutable), var = değiştirilebilir. Burada hepsini val yaptık
//çünkü bir karakterin API'den geldikten sonra id'si veya adı sonradan değişmemeli — bu bir
//best practice: veriyi mümkün olduğunca değiştirilemez tutmak, beklenmedik hatalardan
//(bir yerde yanlışlıkla değiştirilmesi gibi) korur. Bu aynı zamanda OOP'ta encapsulation'ın
//bir parçası: nesnenin iç durumunu dışarıdan rastgele değiştirilmeye kapatıyoruz.


//Bu model neden domain katmanında, data katmanında değil?
//Çünkü bu, saf bir Kotlin sınıfı — içinde ne Retrofit'e özgü bir @SerializedName anotasyonu
//var, ne Room'a özgü bir @Entity etiketi. Yarın API'nin JSON formatı değişse bile, ya da
//Room yerine başka bir veritabanı kullansak bile, bu model hiç değişmeden kalabilir.
//API'den gelen ham veri (data/remote) ve veritabanına yazılan veri (data/local) kendi
//ayrı modellerine sahip olacak, sonra onları bu domain.model.Character'a "çevireceğiz"
//(mapping). Şimdilik garip gelebilir ama Retrofit modelini yazınca fark daha net olacak.