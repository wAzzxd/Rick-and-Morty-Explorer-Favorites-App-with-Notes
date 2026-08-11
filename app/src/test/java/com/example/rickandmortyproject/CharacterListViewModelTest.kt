package com.example.rickandmortyproject

import com.example.rickandmortyproject.presentation.list.CharacterListViewModel
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

// "@OptIn(ExperimentalCoroutinesApi::class)" -> kullandığımız bazı test
// araçları (StandardTestDispatcher gibi) Kotlin'de hâlâ "deneysel" olarak
// işaretli. Bu satır "bunu bilerek kullanıyorum" demek, endişelenecek bir şey değil.
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelTest {

    // ViewModel'in içindeki viewModelScope.launch { } bloklarının, test
    // sırasında GERÇEK thread'ler yerine bu TEST dispatcher'ını kullanmasını
    // sağlıyoruz - bu sayede coroutine'lerin ne zaman bittiğini KONTROL edebiliyoruz.
    private val testDispatcher = StandardTestDispatcher()

    // "@Before" -> JUnit'e "her testten ÖNCE bu fonksiyonu çalıştır" diyoruz.
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    // "@After" -> JUnit'e "her testten SONRA bu fonksiyonu çalıştır" diyoruz,
    // böylece testler birbirini etkilemiyor.
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `karakterler basariyla yuklendiginde state guncellenmeli`() = runTest {
        // 1. Hazırlık: hata FIRLATMAYAN bir sahte repository oluşturuyoruz.
        val fakeRepository = FakeCharacterRepository(shouldThrowError = false)
        val viewModel = CharacterListViewModel(fakeRepository)

        // 2. testDispatcher'daki BEKLEYEN TÜM işleri (coroutine'leri) bitirmesini
        // istiyoruz - çünkü ViewModel'in init{} bloğu zaten loadCharacters()'ı
        // otomatik çağırmıştı, o işin TAMAMLANMASINI beklememiz gerekiyor.
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Kontrol: state'teki karakterler listesi, SAHTE repository'nin
        // döndürdüğü "Test Karakter" ismini içeriyor mu?
        //sahte repository'de diğer değerlerin değişmesi bunu etkilemiyor sadece name'in değişimi etkiliyor
        assertEquals("Test Karakter", viewModel.state.value.characters.first().name)
    }
}


//Her alan için ayrı assertEquals satırı
//kotlin
//val character = viewModel.state.value.characters.first()
//
//assertEquals("Test Karakter", character.name)
//assertEquals("Alive", character.status)
//assertEquals("Human", character.species)
//assertEquals(5, character.episodeCount)
//
//Her satır, tek bir alanı kontrol ediyor. Avantajı: bir tanesi yanlış giderse,
//JUnit tam olarak hangi alanın yanlış olduğunu söylüyor (örn. "status bekleniyordu Alive ama geldi Dead").



//Tüm nesneyi tek seferde karşılaştırmak
//val expectedCharacter = Character(
//    id = 1, name = "Test Karakter", status = "Alive", species = "Human",
//    gender = "Male", imageUrl = "", origin = "Earth", episodeCount = 5
//)
//
//assertEquals(expectedCharacter, viewModel.state.value.characters.first())