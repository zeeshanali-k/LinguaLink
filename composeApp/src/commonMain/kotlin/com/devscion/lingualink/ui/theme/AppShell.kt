package com.devscion.lingualink.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavSection(val label: String, val items: List<NavEntry>)
data class NavEntry(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val badge: String? = null,
)

/**
 * App shell: ambient background + sidebar nav (desktop) or bottom nav (mobile).
 * The body slot receives a screen identifier so screens can render their own topbar
 * and content. Compact-width breakpoint follows Material's 600.dp guideline.
 */
@Composable
fun AppShell(
    sections: List<NavSection>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userInitials: String,
    userName: String,
    userStatus: String,
    body: @Composable (compact: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 760.dp
        val sidebarCollapsed = maxWidth in 760.dp..1100.dp
        Box(modifier = Modifier.fillMaxSize()) {
            AmbientMeshBackground(modifier = Modifier.fillMaxSize())
            CompositionLocalProvider(LocalIsCompactWidth provides compact) {
                if (compact) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { body(true) }
                        BottomNav(
                            sections = sections,
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Sidebar(
                            sections = sections,
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                            userInitials = userInitials,
                            userName = userName,
                            userStatus = userStatus,
                            collapsed = sidebarCollapsed,
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { body(false) }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Sidebar ─────────────────────────────

@Composable
private fun Sidebar(
    sections: List<NavSection>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userInitials: String,
    userName: String,
    userStatus: String,
    collapsed: Boolean,
) {
    val t = LL.tokens
    val width = if (collapsed) 76.dp else 240.dp
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(t.bg1.copy(alpha = 0.6f), t.bg0.copy(alpha = 0.4f))
                )
            )
            .border(width = 1.dp, brush = Brush.horizontalGradient(listOf(Color.Transparent, t.border)), shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Brand
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            BrandMark()
            if (!collapsed) BrandWordmark()
        }

        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!collapsed) {
                    Text(
                        section.label.uppercase(),
                        color = t.text3,
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                section.items.forEach { entry ->
                    SidebarItem(
                        entry = entry,
                        active = entry.id == currentRoute,
                        onClick = { onNavigate(entry.id) },
                        collapsed = collapsed,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // User card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = if (t.isDark) 0.03f else 0.0f))
                .border(1.dp, t.border, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GradientAvatar(initials = userInitials, size = 32.dp, brush = AvatarBrushes.Indigo)
            if (!collapsed) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(userName, color = t.text0, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PulseDot(color = t.green, size = 6.dp)
                        Text(userStatus, color = t.green, fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 0.6.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    entry: NavEntry,
    active: Boolean,
    onClick: () -> Unit,
    collapsed: Boolean,
) {
    val t = LL.tokens
    Box(modifier = Modifier.fillMaxWidth()) {
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 0.dp)
                    .width(2.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(t.brandGradientVertical)
            )
        }
        val activeBg = if (active)
            Brush.horizontalGradient(listOf(t.cyan.copy(alpha = 0.10f), t.violet.copy(alpha = 0.06f)))
        else
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(activeBg)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(entry.icon, contentDescription = entry.label, tint = if (active) t.text0 else t.text1, modifier = Modifier.size(16.dp))
            if (!collapsed) {
                Text(
                    entry.label,
                    color = if (active) t.text0 else t.text1,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (entry.badge != null) {
                    Text(
                        entry.badge,
                        color = t.cyan,
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(t.cyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Bottom nav (compact) ─────────────────────────────

@Composable
private fun BottomNav(
    sections: List<NavSection>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    val t = LL.tokens
    val items = sections.flatMap { it.items }
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(t.border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(t.bg1.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { entry ->
                val active = entry.id == currentRoute
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onNavigate(entry.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        entry.icon, contentDescription = entry.label,
                        tint = if (active) t.cyan else t.text2,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        entry.label,
                        color = if (active) t.text0 else t.text2,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

/**
 * Simple breadcrumb topbar used by non-call routes. Call screen has its own.
 */
@Composable
fun ScreenTopbar(
    crumb: String,
    title: String,
    rightContent: @Composable () -> Unit = {},
    showLanguagePill: Boolean = false,
    sourceCode: String = "",
    targetCode: String = "",
) {
    val t = LL.tokens
    val compact = LL.isCompactWidth
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(t.cyan.copy(alpha = 0.025f), Color.Transparent)
                    )
                )
                .padding(horizontal = if (compact) 16.dp else 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    crumb.uppercase(),
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    title,
                    color = t.text0,
                    fontSize = if (compact) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (showLanguagePill) {
                Chip(
                    text = "$sourceCode → $targetCode",
                    kind = ChipKind.Violet,
                )
            }
            rightContent()
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}
