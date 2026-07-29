package com.example.rickandmortyproject.data.remote.dto

// API'den "origin" alanı olarak gelen küçük JSON objesini karşılıyor.
// Örnek JSON: { "name": "Earth (C-137)", "url": "https://..." }
data class OriginDto(
    val name: String,   // Karakterin geldiği yerin adı, örn: "Earth (C-137)"
    val url: String     //O yerin API'deki detay linki, biz şimdilik kullanmayacağız ama JSON'da var, ihmal edemeyiz
)