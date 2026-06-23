package presentation.composables.main_screen

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import presentation.map.CustomSupportMapFragment
import presentation.screens.main_screen.MAIN_MAP_FRAGMENT_TAG

@Composable
fun MainMapContainer(
    modifier: Modifier = Modifier,
    onFragmentReady: (CustomSupportMapFragment) -> Unit,
) {
    val context = LocalContext.current
    val fragmentManager = (context as FragmentActivity).supportFragmentManager
    val containerId = remember { View.generateViewId() }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } }
    )

    LaunchedEffect(containerId) {
        val existing = fragmentManager.findFragmentByTag(MAIN_MAP_FRAGMENT_TAG) as? CustomSupportMapFragment
        val fragment = existing ?: CustomSupportMapFragment()
        if (!fragment.isAdded) {
            fragmentManager.beginTransaction()
                .add(containerId, fragment, MAIN_MAP_FRAGMENT_TAG)
                .commitNow()
        }
        onFragmentReady(fragment)
    }

    DisposableEffect(Unit) {
        onDispose {
            fragmentManager.findFragmentByTag(MAIN_MAP_FRAGMENT_TAG)?.let { fragment ->
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }
}
