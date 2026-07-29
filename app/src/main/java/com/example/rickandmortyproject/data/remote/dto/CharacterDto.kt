package com.example.rickandmortyproject.data.remote.dto

// API'nin tek bir karakter için döndürdüğü JSON'un birebir Kotlin karşılığı.
// Dikkat: alan isimleri (name, status, species...) JSON'daki isimlerle AYNI olmalı,
// çünkü Gson (JSON çevirici kütüphane) eşleştirmeyi isim bazlı otomatik yapıyor.
data class CharacterDto(
    val id: Int,              // Karakterin benzersiz numarası, örn: 1
    val name: String,         // Karakterin adı, örn: "Rick Sanchez"
    val status: String,       // "Alive", "Dead" veya "unknown"
    val species: String,      // Türü, örn: "Human"
    val gender: String,       // Cinsiyeti, örn: "Male"
    val origin: OriginDto,    // Yukarıda tanımladığımız iç içe obje - JSON'da origin de bir obje olduğu için burada da bir sınıf (OriginDto) kullanıyoruz, düz String değil
    val image: String,        // Profil resminin URL'i
    val episode: List<String> // Karakterin göründüğü bölümlerin URL listesi. Biz sadece SAYISINI kullanacağız (episode.size), tek tek URL'lerle ilgilenmeyeceğiz
)




//dto takısı: Data Transfer Object