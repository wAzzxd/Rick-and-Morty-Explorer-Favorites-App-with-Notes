package com.example.rickandmortyproject.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rickandmortyproject.presentation.detail.CharacterDetailScreen
import com.example.rickandmortyproject.presentation.favorites.FavoritesScreen
import com.example.rickandmortyproject.presentation.list.CharacterListScreen

// "sealed class" -> Kotlin'e özgü, "bu sınıfın SADECE BELLİ, SINIRLI sayıda
// alt türü olabilir" diyen özel bir yapı. Burada uygulamamızdaki TÜM
// ekranları (route'ları) buraya, tek bir yerde, GÜVENLİ şekilde tanımlıyoruz.
sealed class Screen(val route: String) {
    object CharacterList : Screen("character_list")

    // Favoriler ekranının route'u. Bottom Navigation, bu iki route
    // (CharacterList <-> Favorites) arasında sekmelerle geçiş yapıyor.
    object Favorites : Screen("favorites")

    // YENİ EKLENEN: Detay ekranının route'u. "{characterId}" kısmı, route
    // string'inin İÇİNE gömülen bir "yer tutucu" (placeholder) - gerçek
    // kullanımda buraya GERÇEK bir sayı gelecek (örn. "character_detail/5").
    // "createRoute(id: Int)" -> bu yer tutucuyu, GERÇEK bir id ile
    // DOLDURULMUŞ route string'ine çeviren yardımcı fonksiyon - liste
    // ekranından navigasyon yaparken bunu çağıracağız, elle string
    // birleştirme (ve olası yazım hatası) yapmamak için.
    object CharacterDetail : Screen("character_detail/{characterId}") {
        fun createRoute(characterId: Int) = "character_detail/$characterId"
    }
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
            CharacterListScreen(
                // YENİ EKLENEN: liste ekranına artık navController'ı VERİYORUZ
                // (bir sonraki adımda CharacterListScreen.kt'yi güncelleyip
                // bu parametreyi EKLEYECEĞİZ), çünkü onCardClick artık
                // GERÇEKTEN bir yere navigasyon yapacak.
                onCharacterClick = { characterId ->
                    navController.navigate(Screen.CharacterDetail.createRoute(characterId))
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen()
        }

        // YENİ EKLENEN: Detay route'u. Bu, DİĞERLERİNDEN farklı çünkü
        // "arguments" parametresi ALIYOR - route'un İÇİNDEKİ "{characterId}"
        // yer tutucusunun GERÇEKTE ne tür bir veri taşıdığını (Int)
        // Navigation kütüphanesine söylüyoruz.
        composable(
            route = Screen.CharacterDetail.route,
            arguments = listOf(
                navArgument("characterId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            // "backStackEntry.arguments?.getInt("characterId")" -> route'tan
            // GERÇEKTEN gelen id değerini ÇEKİYORUZ. "?: return@composable"
            // -> eğer bir şekilde id gelmezse (olmaması gereken bir durum,
            // ama Kotlin'in null-safety'si bizi buna karşı KORUYOR), bu
            // composable bloğundan GÜVENLE çıkıyoruz, çökme YAŞANMIYOR.
            val characterId = backStackEntry.arguments?.getInt("characterId")
                ?: return@composable

            CharacterDetailScreen(
                characterId = characterId,
                // "onBackClick = { navController.popBackStack() }" ->
                // popBackStack(), navigasyon YIĞININDAN (back stack) BİR
                // ÖNCEKİ ekrana DÖNMEK demek - fiziksel geri tuşuyla AYNI
                // davranışı, ekrandaki bir OK butonuna bağlamamızı sağlıyor.
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) "sealed class Screen" NEDEN Bİ LİSTE/ENUM YERİNE BÖYLE TANIMLANDI?
    sealed class, enum'a benzer ama daha ESNEK: her alt tür (CharacterList,
    Favorites, CharacterDetail) kendi başına farklı yapılar/fonksiyonlar
    taşıyabilir - tam olarak CharacterDetail'in createRoute() fonksiyonuna
    sahip olması gibi. Enum'da bu kadar esnek olmazdı.

 2) BOTTOM NAVIGATION'I NEREYE EKLEYECEĞİZ?
    Bu dosyaya DEĞİL - MainActivity.kt'ye (zaten ekledik). Bottom Navigation,
    "hangi sekmenin seçili olduğunu gösteren, tıklanınca navController.
    navigate(...) çağıran" bir UI bileşeni; NavGraph.kt sadece "route -> ekran"
    eşlemesini tutuyor.

 3) "navArgument" NE İŞE YARIYOR, NEDEN GEREKLİ?
    Route string'i ("character_detail/{characterId}") sadece bir METİN -
    Navigation kütüphanesi, bu METNİN İÇİNDEKİ "{characterId}" kısmının
    GERÇEKTE bir Int mi, String mi, yoksa başka bir tür mü olduğunu
    KENDİLİĞİNDEN bilemez. navArgument ile bunu AÇIKÇA belirtiyoruz -
    bu sayede Navigation, route'tan veriyi ÇEKERKEN doğru TÜRE (Int) otomatik
    çeviriyor, biz elle "String.toInt()" yapıp hata riski almıyoruz.

 4) BU DOSYA HANGİ DOSYALARLA BAĞLANTILI?
    - presentation/detail/CharacterDetailScreen.kt -> characterId ve
      onBackClick parametreleriyle BURADAN çağrılıyor.
    - presentation/list/CharacterListScreen.kt -> BİRAZDAN "onCharacterClick"
      parametresi EKLEYECEĞİZ, bu dosyadaki composable(Screen.CharacterList.
      route) bloğu o parametreyi DOLDURUYOR.
    - MainActivity.kt -> navController'ı BURAYA (AppNavGraph'a) PARAMETRE
      olarak GEÇİRİYOR, Bottom Navigation ile AYNI navController'ı paylaşıyorlar.

 ===========================================================
*/