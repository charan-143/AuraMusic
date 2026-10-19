package com.example.auramusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.theme.PixelMotion
import com.example.auramusic.theme.PixelTheme

enum class PixelNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    LIBRARY("Library", Icons.Default.LibraryMusic),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun PixelBottomNavBar(selectedTab: PixelNavTab, onTabSelected: (PixelNavTab) -> Unit, modifier: Modifier = Modifier) {
    val c = PixelTheme.colors
    androidx.compose.material3.NavigationBar(modifier=modifier,containerColor=c.bottomNavBackground, tonalElevation=0.dp) {
        PixelNavTab.entries.forEach { tab ->
            NavigationBarItem(selected=selectedTab==tab, onClick={onTabSelected(tab)},
                icon={Icon(tab.icon,null)},label={Text(tab.label,fontSize=12.sp)},
                colors=androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor=c.activePillText,
                    selectedTextColor=c.textPrimary,indicatorColor=c.activePillBackground,unselectedIconColor=c.textSecondary,unselectedTextColor=c.textSecondary))
        }
    }
}
