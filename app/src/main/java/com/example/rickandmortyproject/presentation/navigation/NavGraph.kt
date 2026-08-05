package com.example.rickandmortyproject.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rickandmortyproject.presentation.favorites.FavoritesScreen
import com.example.rickandmortyproject.presentation.list.CharacterListScreen

// "sealed class" -> Kotlin'e özgü, "bu sınıfın SADECE BELLİ, SINIRLI sayıda
// alt türü olabilir" diyen özel bir yapı. Burada uygulamamızdaki TÜM
// ekranları (route'ları) buraya, tek bir yerde, GÜVENLİ şekilde tanımlıyoruz.
sealed class Screen(val route: String) {
    object CharacterList : Screen("character_list")

    // YENİ EKLENEN: Favoriler ekranının route'u. Bottom Navigation'ı
    // kurduğumuzda, bu iki route arasında (CharacterList <-> Favorites)
    // sekmelerle geçiş yapacağız.
    object Favorites : Screen("favorites")
}

// "NavHost" -> Compose Navigation'ın kalbi. Hangi route'ta hangi
// Composable'ın gösterileceğini burada eşliyoruz.
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CharacterList.route,
        modifier = modifier
    ) {
        composable(Screen.CharacterList.route) {
            CharacterListScreen()
        }

        // YENİ EKLENEN: Favoriler route'u için hangi Composable'ın
        // çizileceğini tanımlıyoruz.
        composable(Screen.Favorites.route) {
            FavoritesScreen()
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) "sealed class Screen" NEDEN Bİ LİSTE/ENUM YERİNE BÖYLE TANIMLANDI?
    sealed class, enum'a benzer ama daha ESNEK: her alt tür (CharacterList,
    Favorites) kendi başına farklı parametreler taşıyabilir (ileride
    CharacterDetail("character_detail/{id}") gibi bir route eklediğimizde,
    o "id" parametresini route string'inin İÇİNE gömebileceğiz - enum'da
    bu kadar esnek olmazdı).

 2) BOTTOM NAVIGATION'I NEREYE EKLEYECEĞİZ?
    Bu dosyaya DEĞİL - MainActivity.kt'ye. Bottom Navigation, "hangi sekmenin
    seçili olduğunu gösteren, tıklanınca navController.navigate(...) çağıran"
    bir UI bileşeni; NavGraph.kt sadece "route -> ekran" eşlemesini tutuyor,
    Bottom Navigation'ın kendisi bu eşlemeyi KULLANAN bir üst katman.


 ===========================================================
*/