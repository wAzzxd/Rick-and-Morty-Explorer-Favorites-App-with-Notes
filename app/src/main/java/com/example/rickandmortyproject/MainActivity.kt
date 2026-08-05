package com.example.rickandmortyproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rickandmortyproject.presentation.navigation.AppNavGraph
import com.example.rickandmortyproject.presentation.navigation.Screen
import com.example.rickandmortyproject.ui.theme.RickandmortyprojectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickandmortyprojectTheme {
                // "navController" -> BURADA, MainActivity seviyesinde
                // oluşturuyoruz artık (AppNavGraph'ın İÇİNDE değil), çünkü
                // hem AppNavGraph'ın (ekranları göstermek için) hem de
                // Bottom Navigation'ın (hangi sekmenin seçili olduğunu
                // bilmek ve tıklanınca geçiş yapmak için) AYNI navController'a
                // ihtiyacı var - ikisi de AYNI "navigasyon beynini" paylaşmalı.
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    // "bottomBar" -> Scaffold'ın, ekranın EN ALTINA sabit bir
                    // bileşen (bizim durumumuzda Bottom Navigation) koymamızı
                    // sağlayan hazır bir "slot" (yuva). Scaffold, bu bileşenin
                    // yüksekliğini otomatik hesaplayıp, üstteki içeriğin
                    // (innerPadding üzerinden) onun ALTINDA KALMAMASINI sağlıyor.
                    bottomBar = {
                        AppBottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Bottom Navigation'ı ayrı bir Composable'a çıkardık (MainActivity'nin
// içine gömmek yerine) - bu, kodun OKUNABİLİRLİĞİNİ artırıyor ve ileride
// bu bileşeni test etmek/değiştirmek istersek izole bir yerde olmasını sağlıyor.
@Composable
fun AppBottomNavigationBar(navController: androidx.navigation.NavHostController) {
    // "currentBackStackEntryAsState()" -> navController'ın o an hangi
    // route'ta OLDUĞUMUZU CANLI olarak izlememizi sağlıyor. Kullanıcı bir
    // sekmeden diğerine geçtiğinde, bu değer OTOMATİK güncellenir, biz de
    // "hangi sekme seçili görünsün" kararını buna göre veriyoruz.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // "NavigationBar" -> Material3'ün hazır Bottom Navigation bileşeni,
    // ekranın altına sabit bir çubuk çizer, içine NavigationBarItem'lar koyarız.
    NavigationBar {
        // 1. sekme: Karakter Listesi
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Karakterler") },
            label = { Text("Karakterler") },
            // "selected" -> bu sekmenin ŞU AN aktif ekranla eşleşip
            // eşleşmediğini kontrol ediyoruz. "hierarchy" kontrolü, iç içe
            // (nested) navigasyon senaryolarında bile doğru sekmeyi
            // vurgulamamızı sağlayan GÜVENLİ bir karşılaştırma yöntemi.
            selected = currentDestination?.hierarchy?.any {
                it.route == Screen.CharacterList.route
            } == true,
            onClick = {
                // "navController.navigate(...)" -> tıklanan sekmenin
                // route'una GEÇİŞ yapıyoruz.
                navController.navigate(Screen.CharacterList.route) {
                    // "popUpTo(findStartDestination().id) { saveState = true }"
                    // -> sekmeler arası geçişte, geri tuşuna basıldığında
                    // kullanıcının "sekme sekme geriye" gitmesini DEĞİL,
                    // direkt uygulamadan çıkmasını istiyoruz (Instagram,
                    // Twitter gibi uygulamalardaki standart davranış). Bu
                    // satır olmasaydı, her sekme değişimi navigasyon
                    // "yığınına" (back stack) eklenir, geri tuşu sekme
                    // sekme geriye giderdi - kafa karıştırıcı bir deneyim olurdu.
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    // "launchSingleTop = true" -> AYNI sekmeye TEKRAR
                    // tıklarsak, o ekranın YENİ bir kopyasını oluşturmuyoruz,
                    // var olanı kullanıyoruz - gereksiz ekran yığılmasını önlüyor.
                    launchSingleTop = true
                    // "restoreState = true" -> bir sekmeden ayrılıp geri
                    // döndüğümüzde, o ekranın KALDIĞI YERİ (kaydırma
                    // pozisyonu, state'i) HATIRLIYORUZ, sıfırdan başlamıyoruz.
                    restoreState = true
                }
            }
        )

        // 2. sekme: Favoriler
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favoriler") },
            label = { Text("Favoriler") },
            selected = currentDestination?.hierarchy?.any {
                it.route == Screen.Favorites.route
            } == true,
            onClick = {
                navController.navigate(Screen.Favorites.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) NEDEN "navController"I ARTIK MainActivity SEVİYESİNDE OLUŞTURUYORUZ,
    AppNavGraph'IN İÇİNDE DEĞİL?
    Bottom Navigation'ın (hangi sekme seçili, tıklanınca nereye gidilecek)
    VE AppNavGraph'ın (hangi route'ta hangi ekran gösterilecek) AYNI
    navController'ı PAYLAŞMASI gerekiyor - ikisi de aynı "navigasyon
    hafızasını" okuyup yazmalı. Bu yüzden navController'ı EN ÜST ortak
    noktada (MainActivity) oluşturup, hem Scaffold'ın bottomBar'ına hem de
    AppNavGraph'a PARAMETRE olarak geçiriyoruz.

 2) "popUpTo + launchSingleTop + restoreState" ÜÇLÜSÜ NEDEN HEP BİRLİKTE
    KULLANILIYOR?
    Bu üçü, Google'ın RESMİ olarak önerdiği "Bottom Navigation ile doğru
    gezinme" kalıbıdır. Amaçları: (1) geri tuşunun sekme sekme değil,
    doğrudan uygulamadan çıkacak şekilde davranması, (2) aynı sekmeye
    defalarca basmanın ekran yığmaması, (3) sekmeler arası geçişte her
    ekranın KALDIĞI YERİ hatırlaması (örn. liste ekranında kaydırdığın
    yerden, favorilere gidip geri dönünce KALDIĞIN YERDEN devam etmen).

 ===========================================================
*/

//her Composable, dışarıdan bir Modifier alabilmeli ki onu çağıran yer (burada MainActivity), o Composable'ın
//boyutunu/padding'ini/konumunu dışarıdan kontrol edebilsin. Eğer Modifier'ı
//Composable'ın içine sabit gömseydik (Modifier.fillMaxSize() diye direkt yazsaydık),
//dışarıdan hiçbir şekilde bu ekranın davranışını özelleştiremezdik.