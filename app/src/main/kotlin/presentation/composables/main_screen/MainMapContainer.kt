package presentation.composables.main_screen

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import presentation.map.CustomSupportMapFragment

const val MAIN_MAP_FRAGMENT_TAG = "main_map_fragment"

// Stable ID generated once per process lifetime — prevents "Can't change container ID" crash
// when the composable re-enters composition after navigation back to the main screen.
private val mapContainerViewId: Int by lazy { View.generateViewId() }

@Composable
fun MainMapContainer(
    modifier: Modifier = Modifier,
    onFragmentReady: (CustomSupportMapFragment) -> Unit,
) {
    val context = LocalContext.current
    val fragmentManager = (context as FragmentActivity).supportFragmentManager

    AndroidView(
        modifier = modifier,
        factory = { ctx -> FragmentContainerView(ctx).apply { id = mapContainerViewId } }
    )

    LaunchedEffect(Unit) {
        val existing = fragmentManager.findFragmentByTag(MAIN_MAP_FRAGMENT_TAG) as? CustomSupportMapFragment
        val fragment = existing ?: CustomSupportMapFragment()
        when {
            !fragment.isAdded -> fragmentManager.beginTransaction()
                .add(mapContainerViewId, fragment, MAIN_MAP_FRAGMENT_TAG)
                .commitNow()
            fragment.isDetached -> fragmentManager.beginTransaction()
                .attach(fragment)
                .commitNow()
        }
        onFragmentReady(fragment)
    }

    DisposableEffect(Unit) {
        onDispose {
            fragmentManager.findFragmentByTag(MAIN_MAP_FRAGMENT_TAG)?.let { fragment ->
                if (!fragment.isDetached) {
                    // commitNowAllowingStateLoss ensures the detach is processed synchronously,
                    // so the fragment is already detached before the next LaunchedEffect fires.
                    fragmentManager.beginTransaction()
                        .detach(fragment)
                        .commitNowAllowingStateLoss()
                }
            }
        }
    }
}