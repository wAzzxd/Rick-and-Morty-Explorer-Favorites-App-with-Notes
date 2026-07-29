package com.example.rickandmortyproject.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

// "Application" Android'in bize verdiği hazır bir sınıf. Ondan miras alarak
// (": Application()" yazarak) kendi özel Application sınıfımızı oluşturuyoruz.
//
// FARKI NE: MainActivity sadece "bir ekran" temsil eder, kullanıcı o ekranı
// kapatıp başka ekrana geçtiğinde MainActivity'nin yaşam döngüsü değişir.
// Application ise UYGULAMA AÇIK OLDUĞU SÜRECE hep var olan, TEK bir nesnedir.
// Kullanıcı hangi ekranda olursa olsun (liste, detay, favoriler) hep aynı
// Application nesnesi arka planda çalışır. Bu yüzden "bir kere kurulacak,
// her yerden erişilecek" şeyleri (Koin gibi) burada başlatırız.
class RickAndMortyApplication : Application() {

    // "onCreate()" -> Application sınıfının, Android tarafından OTOMATİK
    // çağrılan özel bir fonksiyonu. Kullanıcı uygulama ikonuna tıkladığı an,
    // MainActivity açılmadan HEMEN ÖNCE bu fonksiyon çalışır.
    // "override" yazıyoruz çünkü bu fonksiyon zaten Application sınıfında var,
    // biz sadece "içini kendi istediğimiz gibi dolduruyoruz."
    override fun onCreate() {
        // "super.onCreate()" -> önce Android'in KENDİ onCreate kodunun
        // çalışmasına izin veriyoruz (arka planda yaptığı önemli hazırlıklar var).
        // Bunu HER ZAMAN en başta çağırmak gerekir, yoksa uygulama beklenmedik
        // şekillerde bozulabilir.
        super.onCreate()

        // "startKoin { }" -> Koin kütüphanesini uyandırıp çalıştırıyoruz.
        // Bu satırdan sonra, uygulamanın HERHANGİ bir yerinde (ViewModel,
        // Activity, herhangi bir sınıf) Koin'den nesne isteyebiliriz.
        // Bu fonksiyon SADECE BİR KERE, uygulama açılışında çağrılmalı -
        // burası (Application.onCreate) tam olarak o "bir kere" anıdır.
        startKoin {

            // "androidContext(...)" -> Koin'e "işte sana Android'in Context'i"
            // diyoruz. Context, Android'de "uygulamanın çalıştığı ortam,
            // kaynaklara (dosyalar, veritabanı, sistem servisleri) erişim
            // noktası" gibi düşünülebilir. Bazı kütüphaneler (örn. ileride
            // ekleyeceğimiz Room veritabanı) çalışmak için mutlaka bir
            // Context'e ihtiyaç duyar; Koin bu Context'i burada tanıyıp
            // ihtiyacı olan her yere otomatik dağıtabilecek.
            //
            // "this@RickAndMortyApplication" -> Kotlin'de "@SınıfAdı" yazımı,
            // iç içe geçmiş bloklar (burada startKoin bloğunun içindeyiz)
            // içinden "dışarıdaki asıl sınıfın kendisini" (yani bu
            // RickAndMortyApplication nesnesinin kendisini) işaret etmek
            // için kullanılır. Application zaten bir Context türü olduğu için
            // burada kendimizi (this@RickAndMortyApplication) Context olarak veriyoruz.
            androidContext(this@RickAndMortyApplication)

            // "modules(appModule)" -> daha önce di/AppModule.kt içinde
            // yazdığımız "hangi nesne istenirse nasıl oluşturulacağı" tarifini
            // (appModule) Koin'e tanıtıyoruz. Bu satırdan sonra Koin,
            // "CharacterRepository istendi" dendiğinde ne yapacağını biliyor.
            modules(appModule)
        }
    }
}

/*
 ==================== KAVRAMSAL NOTLAR ====================

 1) NEDEN "MainActivity" İÇİNDE DEĞİL DE AYRI BİR "Application" SINIFINDA
    BAŞLATIYORUZ?
    MainActivity, kullanıcı geri tuşuna basıp kapatabileceği, yeniden
    açılabileceği bir ekrandır - yaşam döngüsü kararsızdır (oluşur, yok olur,
    tekrar oluşur). Koin'i orada başlatsaydık, MainActivity her yeniden
    oluştuğunda Koin'i TEKRAR TEKRAR başlatmaya çalışabilirdik, bu da hataya
    yol açar ("Koin already started" gibi). Application sınıfı ise uygulama
    açıkken sadece BİR KERE oluşur, bu yüzden "bir kere kurulum" işleri için
    doğru ve güvenli yer burasıdır.

 2) BU SINIF, ANDROID TARAFINDAN NASIL "BULUNUYOR"?
    Sadece bu dosyayı yazmak yetmez - AndroidManifest.xml dosyasında
    <application android:name=".di.RickAndMortyApplication" ...> diyerek
    Android'e "normal Application yerine BUNU kullan" demiş olacağız.
    Bunu unutursak, bu sınıf hiç çalışmaz, Koin hiç başlamaz ve ileride
    ViewModel'ler "Koin başlatılmadı" hatası verir.

 3) "OBJECT" (RetrofitInstance'ta kullandığımız) İLE "CLASS : Application()"
    ARASINDAKİ FARK NE?
    "object" kendi kendine, biz istediğimizde var olan bir singleton'du.
    Application sınıfı ise Android SİSTEMİNİN kendisinin, uygulama açılırken
    otomatik olarak BİR TANE oluşturduğu özel bir sınıf - biz onu manuel
    "RickAndMortyApplication()" diye çağırmıyoruz, Android bizim yerimize
    çağırıyor. Bu yüzden "object" değil, Application'dan miras alan bir
    "class" olarak tanımlıyoruz.
 ===========================================================
*/