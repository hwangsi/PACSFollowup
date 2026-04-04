package com.example.pacsfollowup.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pacsfollowup.SharedViewModel
import com.example.pacsfollowup.ui.camera.CameraScreen
import com.example.pacsfollowup.ui.main.MainScreen
import com.example.pacsfollowup.ui.main.MainViewModel
import com.example.pacsfollowup.ui.review.ReviewScreen
import com.example.pacsfollowup.ui.review.ReviewViewModel

private object Routes {
    const val MAIN = "main"
    const val CAMERA = "camera"
    const val REVIEW = "review"
}

@Composable
fun AppNavigation() {
    val activity = LocalContext.current as ComponentActivity
    val navController = rememberNavController()

    // Activity 범위 ViewModel (화면 간 공유)
    val sharedViewModel: SharedViewModel = viewModel(activity)
    val mainViewModel: MainViewModel = viewModel(activity)

    NavHost(navController = navController, startDestination = Routes.MAIN) {

        composable(Routes.MAIN) {
            MainScreen(
                viewModel = mainViewModel,
                onNavigateToCamera = { navController.navigate(Routes.CAMERA) }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onPhotoCaptured = { bitmap ->
                    sharedViewModel.setCapturedBitmap(bitmap)
                    navController.navigate(Routes.REVIEW)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REVIEW) {
            val reviewViewModel: ReviewViewModel = viewModel()
            val capturedBitmap by sharedViewModel.capturedBitmap.collectAsState()

            ReviewScreen(
                capturedBitmap = capturedBitmap,
                viewModel = reviewViewModel,
                onSaved = { record ->
                    mainViewModel.addRecord(record)
                    sharedViewModel.setCapturedBitmap(null)
                    navController.popBackStack(Routes.MAIN, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
