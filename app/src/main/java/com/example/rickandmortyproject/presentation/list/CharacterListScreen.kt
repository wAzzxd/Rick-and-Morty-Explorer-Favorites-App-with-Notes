package com.example.rickandmortyproject.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun CharacterListScreen(
    modifier: Modifier = Modifier,
    viewModel: CharacterListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo
                .lastOrNull()?.index ?: 0
            val totalItemsCount = state.characters.size
            lastVisibleItemIndex >= totalItemsCount - 5 && totalItemsCount > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && state.error == null) {
            viewModel.loadCharacters()
        }
    }

    // "Column" -> arama kutusu, chip'ler ve listeyi DİKEY olarak sıralıyoruz.
    Column(modifier = modifier.fillMaxSize()) {

        // "OutlinedTextField" -> Material Design'ın çerçeveli metin kutusu.
        OutlinedTextField(
            // "value = state.searchQuery" -> kutunun İÇİNDEKİ metin, DOĞRUDAN
            // state'ten okunuyor - yani ViewModel'deki değer TEK GERÇEK KAYNAK
            // (single source of truth), TextField kendi başına bir metin
            // TUTMUYOR, sadece state'i YANSITIYOR.
            value = state.searchQuery,
            // "onValueChange" -> kullanıcı her HARF yazdığında/sildiğinde
            // tetiklenir, ViewModel'e "işte yeni metin" diye haber veriyoruz.
            onValueChange = { newQuery -> viewModel.onSearchQueryChanged(newQuery) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Karakter ara...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Ara") },
            singleLine = true // metin kutusunun TEK SATIR kalmasını, Enter'a basınca alt satıra geçmemesini sağlıyor
        )

        // "LazyRow" -> LazyColumn'un YATAY versiyonu. Chip sayısı ekrana
        // sığmasa bile YANA doğru kaydırılabilir, performanslı bir liste.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
        ) {
            // Göstereceğimiz 3 durum: Alive, Dead, unknown. Bunu bir liste
            // olarak tanımlayıp items() ile TEK TEK chip'e çeviriyoruz -
            // aynı kodu 3 KERE elle yazmak yerine.
            items(listOf("Alive", "Dead", "unknown")) { status ->
                // "FilterChip" -> Material'ın "seçilebilir etiket" bileşeni,
                // seçiliyken farklı renkte/ikonlu görünür.
                FilterChip(
                    // "selected" -> bu chip'in ŞU AN seçili olup olmadığını
                    // state.statusFilter ile KARŞILAŞTIRARAK belirliyoruz.
                    selected = state.statusFilter == status,
                    // Tıklanınca ViewModel'e "bu durumu seç/kaldır" diyoruz
                    // (ViewModel'deki onStatusFilterChanged zaten TOGGLE
                    // mantığını içeriyordu - aynı chip'e tekrar basılırsa kaldırır).
                    onClick = { viewModel.onStatusFilterChanged(status) },
                    label = { Text(status) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // Geri kalan alanı (kalan TÜM boşluğu) liste/loading/hata durumu dolduracak.
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.characters.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        Text(text = "Hata: ${state.error}")
                        Button(onClick = { viewModel.retry() }) {
                            Text("Tekrar Dene")
                        }
                    }
                }

                // Arama/filtre SONUCUNDA hiç karakter bulunamadıysa (liste boş
                // ama hata da yok, isLoading da false) -> kullanıcıya "sonuç
                // bulunamadı" diye bir mesaj gösterelim, boş beyaz ekran YERİNE.
                state.characters.isEmpty() && !state.isLoading -> {
                    Text(
                        text = "Sonuç bulunamadı",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        items(
                            items = state.characters,
                            key = { character -> character.id }
                        ) { character ->
                            CharacterCard(
                                character = character,
                                isFavorite = false,
                                onCardClick = { },
                                onFavoriteClick = { }
                            )
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                when {
                                    state.isLoadingMore -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    state.error != null -> {
                                        Column(modifier = Modifier.align(Alignment.Center)) {
                                            Text(text = state.error ?: "")
                                            Button(onClick = { viewModel.retry() }) {
                                                Text("Tekrar Dene")
                                            }
                                        }
                                    }
                                    else -> { }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}