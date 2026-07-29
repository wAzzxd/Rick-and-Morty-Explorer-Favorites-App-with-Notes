package com.example.rickandmortyproject.data.remote.dto

//(API'nin sayfalama bilgisiyle beraber dönen üst paket)

// API'nin /character endpoint'inden dönen EN DIŞ (en üst) JSON yapısı.
// JSON'un kendisi { "info": {...}, "results": [...] } şeklinde geldiği için
// bu sınıf da tam olarak o iki alanı taşıyor.
data class CharacterResponseDto(
    val info: InfoDto,              // Sayfalama bilgisi (kaç sayfa var, sonraki sayfa linki vb.)
    val results: List<CharacterDto> // Asıl istediğimiz şey: bu sayfadaki karakterlerin listesi
)

// "info" objesinin karşılığı. Pagination (sayfalama) yaparken next/prev'e bakacağız.
data class InfoDto(
    val count: Int,     // Toplam karakter sayısı (826 gibi)
    val pages: Int,     // Toplam sayfa sayısı
    val next: String?,  // Bir sonraki sayfanın URL'i. "?" işaretine dikkat: son sayfadaysak bu null gelir, o yüzden nullable (String?) yaptık
    val prev: String?   // Bir önceki sayfanın URL'i. İlk sayfadaysak null gelir
)

//next: String? ve prev: String? içindeki ?
//Kotlin'de bir tipin sonuna ? koyarsan, o değişkenin null olabileceğini söylersin.