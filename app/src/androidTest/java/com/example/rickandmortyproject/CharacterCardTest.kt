package com.example.rickandmortyproject

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.rickandmortyproject.domain.model.Character
import com.example.rickandmortyproject.presentation.list.CharacterCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CharacterCardTest {

    // "@get:Rule" -> JUnit'e "bu nesneyi, her testten ÖNCE ve SONRA
    // otomatik olarak hazırla/temizle" diyoruz. "createComposeRule()",
    // testler için GERÇEKTEN bir Compose ekranı çizebilen özel bir araç -
    // bunu her UI testinde kullanacağız.
    @get:Rule
    val composeTestRule = createComposeRule()

    // Testlerde tekrar tekrar kullanacağımız sahte bir karakter - bunu
    // FakeCharacterRepository'nin verdiği sahte veriyle AYNI mantıkta düşün.
    private val testKarakteri = Character(
        id = 1,
        name = "Rick Sanchez",
        status = "Alive",
        species = "Human",
        gender = "Male",
        imageUrl = "",
        origin = "Earth",
        episodeCount = 10
    )

    @Test
    fun kalbe_tiklaninca_onFavoriteClick_cagrilmali() {
        // "tiklandiMi" -> kalbe GERÇEKTEN tıklanıp tıklanmadığını takip
        // etmek için kullandığımız basit bir değişken. Başlangıçta false.
        var tiklandiMi = false

        // Arrange: CharacterCard'ı GERÇEKTEN çiziyoruz, test karakterimizle.
        composeTestRule.setContent {
            CharacterCard(
                character = testKarakteri,
                isFavorite = false,
                onCardClick = { },
                // Kalbe tıklanınca bu lambda ÇALIŞACAK - biz de
                // "tiklandiMi"yi true yapıyoruz, böylece tıklamanın
                // GERÇEKTEN gerçekleştiğini anlayabileceğiz.
                onFavoriteClick = { tiklandiMi = true }
            )
        }

        // Act: "Favorilere ekle" açıklamasına (contentDescription) sahip
        // ikonu (kalp ikonunu) bulup TIKLIYORUZ - CharacterCard.kt'de
        // Icon'a "contentDescription = "Favorilere ekle"" verdiğimizi
        // hatırlarsan, testin bu ikonu BULABİLMESİNİN sebebi bu.
        composeTestRule.onNodeWithContentDescription("Favorilere ekle").performClick()

        // Assert: tıklama GERÇEKTEN gerçekleşti mi, "tiklandiMi" true oldu mu?
        assertTrue(tiklandiMi)
    }
}