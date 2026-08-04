package com.example.rickandmortyproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rickandmortyproject.presentation.list.CharacterListScreen
import com.example.rickandmortyproject.presentation.navigation.AppNavGraph
import com.example.rickandmortyproject.ui.theme.RickandmortyprojectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickandmortyprojectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    AppNavGraph(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

//her Composable, dışarıdan bir Modifier alabilmeli ki onu çağıran yer (burada MainActivity), o Composable'ın
//boyutunu/padding'ini/konumunu dışarıdan kontrol edebilsin. Eğer Modifier'ı
//Composable'ın içine sabit gömseydik (Modifier.fillMaxSize() diye direkt yazsaydık),
//dışarıdan hiçbir şekilde bu ekranın davranışını özelleştiremezdik.
