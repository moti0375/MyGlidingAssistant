package presentation.composables

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

private const val MAP_FRAGMENT_TAG = "flight_map_container"

@Composable
fun MapContainer(
    modifier: Modifier = Modifier,
    onFragmentReady: (CustomSupportMapFragment) -> Unit,
) {
    val context = LocalContext.current
    val fragmentManager = (context as FragmentActivity).supportFragmentManager
    val containerId = remember { View.generateViewId() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FragmentContainerView(ctx).apply { id = containerId }
        }
    )

    LaunchedEffect(containerId) {
        val existing = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as? CustomSupportMapFragment
        val fragment = existing ?: CustomSupportMapFragment()
        if (!fragment.isAdded) {
            fragmentManager.beginTransaction()
                .add(containerId, fragment, MAP_FRAGMENT_TAG)
                .commitNow()
        }
        onFragmentReady(fragment)
    }

    DisposableEffect(Unit) {
        onDispose {
            fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG)?.let { fragment ->
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }
}
