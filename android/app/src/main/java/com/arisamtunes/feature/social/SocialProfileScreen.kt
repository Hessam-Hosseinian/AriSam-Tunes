package com.arisamtunes.feature.social

import com.arisamtunes.core.design.spacing.AriSamDimensions
import com.arisamtunes.core.design.colors.AriSamPalette
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arisamtunes.R
import com.arisamtunes.core.design.components.PressScaleBox
import com.arisamtunes.core.design.components.ShimmerBox
import com.arisamtunes.core.design.theme.AriSamThemeTokens
import com.arisamtunes.data.catalog.PlaylistDto
import com.arisamtunes.data.social.PublicUserDto
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sin

private val ProfileHeroHeight = AriSamDimensions.dp448
private val ProfileBackdropHeight = AriSamDimensions.dp362
private val ProfileCardShape = RoundedCornerShape(AriSamDimensions.dp28)
private val ProfileAccent = AriSamPalette.cyanAccent
private val ProfileViolet = AriSamPalette.indigoSoft
private val ProfileInk = AriSamPalette.profileInk

@Composable
fun SocialProfileRoute(
    onBack: (() -> Unit)? = null,
    onFollowersClick: (String) -> Unit,
    onFollowingClick: (String) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onMessageClick: (String) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    viewModel: SocialProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playlists = viewModel.playlists.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val actionFailed = stringResource(R.string.social_action_failed)
    val avatarUpdated = stringResource(R.string.profile_photo_updated)
    val avatarUploadFailed = stringResource(R.string.profile_photo_upload_failed)
    val premiumUpdated = stringResource(R.string.profile_premium_updated)
    val premiumUpdateFailed = stringResource(R.string.profile_premium_update_failed)
    val snackbarBottomPadding = if (onBack != null) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        AriSamDimensions.dp0
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::uploadAvatar)
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onForeground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.onForeground()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, actionFailed, avatarUpdated, avatarUploadFailed, premiumUpdated, premiumUpdateFailed) {
        viewModel.effects.collect { effect ->
            snackbarHostState.showSnackbar(
                when (effect) {
                    SocialEffect.ActionFailed -> actionFailed
                    SocialEffect.AvatarUpdated -> avatarUpdated
                    SocialEffect.AvatarUploadFailed -> avatarUploadFailed
                    SocialEffect.PremiumUpdated -> premiumUpdated
                    SocialEffect.PremiumUpdateFailed -> premiumUpdateFailed
                },
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> ProfileLoadingState()
            state.hasError && state.user == null -> ProfileErrorState(viewModel::refresh)
            state.user == null -> ProfileEmptyState()
            else -> state.user?.let { user -> ProfileContent(
                user = user,
                playlists = playlists,
                isUpdatingFollow = state.isUpdatingFollow,
                isUploadingAvatar = state.isUploadingAvatar,
                isUpdatingPremium = state.isUpdatingPremium,
                isOwnProfile = state.isOwnProfile,
                onBack = onBack,
                onFollowClick = viewModel::toggleFollow,
                onFollowersClick = { onFollowersClick(user.id) },
                onFollowingClick = { onFollowingClick(user.id) },
                onPlaylistClick = onPlaylistClick,
                onMessageClick = { onMessageClick(user.id) },
                onAvatarClick = {
                    avatarPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onPremiumClick = viewModel::activatePremium,
                onSettingsClick = onSettingsClick,
            ) } ?: ProfileEmptyState()
        }
        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).padding(bottom = snackbarBottomPadding),
        )
    }
}

@Composable
private fun ProfileContent(
    user: PublicUserDto,
    playlists: LazyPagingItems<PlaylistDto>,
    isUpdatingFollow: Boolean,
    isUploadingAvatar: Boolean,
    isUpdatingPremium: Boolean,
    isOwnProfile: Boolean,
    onBack: (() -> Unit)?,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onMessageClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onSettingsClick: (() -> Unit)?,
) {
    val spacing = AriSamThemeTokens.spacing
    val listState = rememberLazyListState()
    val heroScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 1_000f
        }
    }
    val backgroundTop = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .42f)
    val backgroundBottom = MaterialTheme.colorScheme.background
    val navigationBarPadding = if (onBack != null) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        AriSamDimensions.dp0
    }
    var entered by remember(user.id) { mutableStateOf(false) }
    LaunchedEffect(user.id) { entered = true }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val background = Brush.verticalGradient(
                    colors = listOf(backgroundTop, backgroundBottom),
                )
                onDrawBehind { drawRect(background) }
            },
        contentPadding = PaddingValues(bottom = spacing.xxl + navigationBarPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item(key = "profile_hero") {
            ProfileHeroStage(
                user = user,
                isOwnProfile = isOwnProfile,
                isUploadingAvatar = isUploadingAvatar,
                entered = entered,
                scrollOffset = heroScrollOffset,
                onBack = onBack,
                onAvatarClick = onAvatarClick,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick,
            )
        }
        item(key = "profile_identity") {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(460, delayMillis = 180)) +
                    slideInVertically(tween(540, delayMillis = 180, easing = FastOutSlowInEasing)) { it / 4 },
                modifier = Modifier.padding(horizontal = spacing.lg),
            ) {
                ProfileIdentityCard(user, isOwnProfile)
            }
        }
        item(key = "profile_actions") {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(420, delayMillis = 260)) +
                    slideInVertically(tween(560, delayMillis = 260, easing = FastOutSlowInEasing)) { it / 3 },
                modifier = Modifier.padding(horizontal = spacing.lg),
            ) {
                ProfileActions(
                    isOwnProfile = isOwnProfile,
                    isFollowing = user.isFollowing,
                    isUpdatingFollow = isUpdatingFollow,
                    isUploadingAvatar = isUploadingAvatar,
                    isPremium = user.isPremium,
                    isUpdatingPremium = isUpdatingPremium,
                    onFollowClick = onFollowClick,
                    onMessageClick = onMessageClick,
                    onAvatarClick = onAvatarClick,
                    onPremiumClick = onPremiumClick,
                    onSettingsClick = onSettingsClick,
                )
            }
        }
        item(key = "playlist_heading") {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(420, delayMillis = 340)) +
                    slideInVertically(tween(560, delayMillis = 340, easing = FastOutSlowInEasing)) { it / 2 },
            ) {
                PlaylistSectionHeading(playlists.itemCount, isOwnProfile)
            }
        }
        when {
            playlists.loadState.refresh is LoadState.Loading -> {
                items(2, key = { "playlist_shimmer_$it" }) {
                    PlaylistShimmerRow()
                }
            }
            playlists.loadState.refresh is LoadState.Error -> item(key = "playlist_refresh_error") {
                ProfilePlaylistLoadError(playlists::retry)
            }
            playlists.itemCount == 0 -> item(key = "playlist_empty") {
                ProfilePlaylistEmptyState()
            }
            else -> {
                items(
                    count = (playlists.itemCount + 1) / 2,
                    key = { "playlist_row_$it" },
                ) { rowIndex ->
                    val firstIndex = rowIndex * 2
                    val secondIndex = firstIndex + 1
                    val rowPlaylists = listOfNotNull(
                        playlists[firstIndex],
                        if (secondIndex < playlists.itemCount) playlists[secondIndex] else null,
                    )
                    StaggeredPlaylistRow(
                        rowIndex = rowIndex,
                        playlists = rowPlaylists,
                        onPlaylistClick = onPlaylistClick,
                    )
                }
                when (playlists.loadState.append) {
                    is LoadState.Loading -> item(key = "playlist_append_loading") {
                        Box(
                            Modifier.fillMaxWidth().padding(AriSamDimensions.dp20),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(Modifier.size(AriSamDimensions.dp24), strokeWidth = AriSamDimensions.dp2)
                        }
                    }
                    is LoadState.Error -> item(key = "playlist_append_error") {
                        ProfilePlaylistLoadError(playlists::retry)
                    }
                    is LoadState.NotLoading -> Unit
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroStage(
    user: PublicUserDto,
    isOwnProfile: Boolean,
    isUploadingAvatar: Boolean,
    entered: Boolean,
    scrollOffset: Float,
    onBack: (() -> Unit)?,
    onAvatarClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
) {
    val spacing = AriSamThemeTokens.spacing
    val statusBarPadding = if (onBack != null) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        AriSamDimensions.dp0
    }
    Box(Modifier.fillMaxWidth().height(ProfileHeroHeight)) {
        ProfileAuroraBackdrop(
            Modifier
                .fillMaxWidth()
                .height(ProfileBackdropHeight)
                .graphicsLayer {
                    translationY = scrollOffset * .28f
                    val scale = 1f + (scrollOffset / 8_000f).coerceIn(0f, .06f)
                    scaleX = scale
                    scaleY = scale
                },
        )

        onBack?.let { navigateBack ->
            Surface(
                onClick = navigateBack,
                modifier = Modifier
                    .padding(start = spacing.md, top = spacing.md + statusBarPadding)
                    .size(AriSamDimensions.dp46),
                shape = CircleShape,
                color = AriSamPalette.black.copy(alpha = .28f),
                contentColor = AriSamPalette.white,
                border = BorderStroke(AriSamDimensions.dp1, AriSamPalette.white.copy(alpha = .16f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                }
            }
        }

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(520, delayMillis = 60)) +
                slideInVertically(tween(650, easing = FastOutSlowInEasing)) { it / 5 },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = AriSamDimensions.dp28 + statusBarPadding),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                ProfileAvatar(
                    user = user,
                    isOwnProfile = isOwnProfile,
                    isUploading = isUploadingAvatar,
                    onClick = onAvatarClick,
                )
                Text(
                    text = user.displayName,
                    color = AriSamPalette.white,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = AriSamDimensions.dp32),
                )
                ListenerBadge(isPremium = user.isPremium)
                MusicPulseDecoration(Modifier.padding(top = AriSamDimensions.dp2))
            }
        }

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(480, delayMillis = 120)) +
                slideInVertically(
                    animationSpec = spring(dampingRatio = .78f, stiffness = 260f),
                    initialOffsetY = { it },
                ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = spacing.lg),
        ) {
            ProfileStatsCard(
                user = user,
                animateValues = entered,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick,
            )
        }
    }
}

@Composable
private fun ProfileAuroraBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "profileAurora")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "profileAuroraDrift",
    )
    val breathe by transition.animateFloat(
        initialValue = .9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "profileAuroraBreathe",
    )

    Box(
        modifier
            .clip(RoundedCornerShape(bottomStart = AriSamDimensions.dp42, bottomEnd = AriSamDimensions.dp42))
            .background(
                Brush.linearGradient(
                    listOf(ProfileInk, AriSamPalette.cyan900, AriSamPalette.navyVioletDark),
                ),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = AriSamDimensions.dp58, y = AriSamDimensions.negative72)
                .size(AriSamDimensions.dp245)
                .graphicsLayer {
                    translationX = drift * 38f
                    translationY = drift * 18f
                    scaleX = breathe
                    scaleY = breathe
                }
                .blur(AriSamDimensions.dp54)
                .background(ProfileAccent.copy(alpha = .38f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = AriSamDimensions.negative58, y = AriSamDimensions.dp44)
                .size(AriSamDimensions.dp220)
                .graphicsLayer {
                    translationX = -drift * 32f
                    translationY = drift * 24f
                    scaleX = 2f - breathe
                    scaleY = 2f - breathe
                }
                .blur(AriSamDimensions.dp60)
                .background(ProfileViolet.copy(alpha = .32f), CircleShape),
        )
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AriSamPalette.white.copy(alpha = .14f), AriSamPalette.transparent),
                    center = center.copy(y = size.height * .12f),
                    radius = size.width * .7f,
                ),
                radius = size.width * .7f,
                center = center.copy(y = size.height * .12f),
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    user: PublicUserDto,
    isOwnProfile: Boolean,
    isUploading: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "profileAvatarMotion")
    val changePhotoDescription = stringResource(R.string.profile_change_photo)
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(2_300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "profileAvatarRing",
    )
    val floatY by transition.animateFloat(
        initialValue = -2f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "profileAvatarFloat",
    )

    Box(
        Modifier
            .size(AriSamDimensions.dp132)
            .graphicsLayer { translationY = floatY },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(AriSamDimensions.dp126)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = (1.09f - ringScale) * 5.5f + .18f
                }
                .border(AriSamDimensions.dp2, ProfileAccent, CircleShape),
        )
        val avatarModifier = Modifier
            .size(AriSamDimensions.dp112)
            .border(AriSamDimensions.dp4, AriSamPalette.white.copy(alpha = .94f), CircleShape)
            .clip(CircleShape)
        if (isOwnProfile) {
            PressScaleBox(
                onClick = onClick,
                enabled = !isUploading,
                modifier = Modifier.semantics { contentDescription = changePhotoDescription },
            ) {
                ProfileAvatarImage(user, avatarModifier)
            }
        } else {
            ProfileAvatarImage(user, avatarModifier)
        }

        when {
            isUploading -> Box(
                Modifier
                    .size(AriSamDimensions.dp112)
                    .clip(CircleShape)
                    .background(AriSamPalette.black.copy(alpha = .58f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.size(AriSamDimensions.dp32), color = AriSamPalette.white, strokeWidth = AriSamDimensions.dp3)
            }
            isOwnProfile -> Surface(
                onClick = onClick,
                modifier = Modifier.align(Alignment.BottomEnd).size(AriSamDimensions.dp38),
                shape = CircleShape,
                color = ProfileAccent,
                contentColor = ProfileInk,
                border = BorderStroke(AriSamDimensions.dp3, AriSamPalette.white),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PhotoCamera,
                        stringResource(R.string.profile_change_photo),
                        Modifier.size(AriSamDimensions.dp19),
                    )
                }
            }
        }

        if (user.isPremium) {
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).size(AriSamDimensions.dp36),
                shape = CircleShape,
                color = AriSamThemeTokens.tehranAmber,
                border = BorderStroke(AriSamDimensions.dp3, AriSamPalette.white),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Verified,
                        stringResource(R.string.premium_active),
                        tint = ProfileInk,
                        modifier = Modifier.size(AriSamDimensions.dp19),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatarImage(user: PublicUserDto, modifier: Modifier) {
    if (user.avatarUrl.isNullOrBlank()) {
        Box(
            modifier.background(Brush.linearGradient(listOf(ProfileViolet, ProfileAccent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, null, tint = AriSamPalette.white, modifier = Modifier.size(AriSamDimensions.dp42))
        }
    } else {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.displayName,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(AriSamPalette.teal900),
        )
    }
}

@Composable
private fun ListenerBadge(isPremium: Boolean) {
    Surface(
        shape = CircleShape,
        color = AriSamPalette.white.copy(alpha = .1f),
        contentColor = if (isPremium) AriSamThemeTokens.tehranAmber else AriSamPalette.white.copy(alpha = .78f),
        border = BorderStroke(AriSamDimensions.dp1, AriSamPalette.white.copy(alpha = .13f)),
    ) {
        Row(
            Modifier.padding(horizontal = AriSamDimensions.dp12, vertical = AriSamDimensions.dp6),
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isPremium) Icons.Rounded.Verified else Icons.Rounded.Headphones,
                null,
                Modifier.size(AriSamDimensions.dp15),
            )
            Text(
                stringResource(if (isPremium) R.string.profile_premium_listener else R.string.profile_listener),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MusicPulseDecoration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "profileMusicPulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing)),
        label = "profileMusicPhase",
    )
    Canvas(modifier.size(width = AriSamDimensions.dp112, height = AriSamDimensions.dp24)) {
        val barCount = 13
        val spacing = size.width / barCount
        repeat(barCount) { index ->
            val wave = ((sin(phase + index * .72f) + 1f) * .5f)
            val barHeight = AriSamDimensions.dp4.toPx() + wave * AriSamDimensions.dp10.toPx()
            val x = spacing * index + spacing / 2f
            drawLine(
                color = AriSamPalette.white.copy(alpha = .22f + wave * .28f),
                start = androidx.compose.ui.geometry.Offset(x, center.y - barHeight / 2f),
                end = androidx.compose.ui.geometry.Offset(x, center.y + barHeight / 2f),
                strokeWidth = AriSamDimensions.dp2.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ProfileStatsCard(
    user: PublicUserDto,
    animateValues: Boolean,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ProfileCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .97f),
        border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)),
        shadowElevation = AriSamDimensions.dp14,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = AriSamDimensions.dp18),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedProfileStat(
                value = user.followersCount,
                label = stringResource(R.string.social_followers),
                animate = animateValues,
                onClick = onFollowersClick,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(AriSamDimensions.dp1).height(AriSamDimensions.dp46).background(MaterialTheme.colorScheme.outlineVariant))
            AnimatedProfileStat(
                value = user.followingCount,
                label = stringResource(R.string.social_following),
                animate = animateValues,
                onClick = onFollowingClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnimatedProfileStat(
    value: Long,
    label: String,
    animate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedValue by animateFloatAsState(
        targetValue = if (animate) value.toFloat() else 0f,
        animationSpec = tween(900, delayMillis = 120, easing = FastOutSlowInEasing),
        label = "profileStat_$label",
    )
    val description = "${animatedValue.roundToLong()} $label"
    PressScaleBox(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp2),
        ) {
            Text(
                animatedValue.roundToLong().toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProfileIdentityCard(user: PublicUserDto, isOwnProfile: Boolean) {
    Surface(
        shape = ProfileCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .78f)),
    ) {
        Column(
            Modifier.padding(AriSamDimensions.dp20),
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(AriSamDimensions.dp36),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Headphones, null, Modifier.size(AriSamDimensions.dp19))
                    }
                }
                Spacer(Modifier.width(AriSamDimensions.dp10))
                Column {
                    Text(
                        stringResource(R.string.profile_about),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.profile_identity_eyebrow),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                text = user.bio?.takeIf(String::isNotBlank)
                    ?: stringResource(if (isOwnProfile) R.string.social_own_profile_hint else R.string.social_new_profile_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfileActions(
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isUpdatingFollow: Boolean,
    isUploadingAvatar: Boolean,
    isPremium: Boolean,
    isUpdatingPremium: Boolean,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onSettingsClick: (() -> Unit)?,
) {
    if (isOwnProfile) {
        Column(verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isPremium) AriSamPalette.amberAccent.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(AriSamDimensions.dp1, if (isPremium) AriSamPalette.amberAccent.copy(alpha = .55f) else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(AriSamDimensions.dp16), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp8)) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = AriSamPalette.amberAccent)
                        Text(
                            stringResource(if (isPremium) R.string.premium_active else R.string.profile_premium_demo_title),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(stringResource(R.string.profile_premium_demo_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onPremiumClick, enabled = !isUpdatingPremium, modifier = Modifier.fillMaxWidth()) {
                        if (isUpdatingPremium) CircularProgressIndicator(Modifier.size(AriSamDimensions.dp19), strokeWidth = AriSamDimensions.dp2)
                        else Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(AriSamDimensions.dp19))
                        Spacer(Modifier.width(AriSamDimensions.dp8))
                        Text(stringResource(if (isPremium) R.string.profile_premium_renew else R.string.profile_premium_activate))
                    }
                }
            }
            FilledTonalButton(
                onClick = onAvatarClick,
                enabled = !isUploadingAvatar,
                modifier = Modifier.fillMaxWidth().height(AriSamDimensions.dp54),
                shape = MaterialTheme.shapes.large,
            ) {
                if (isUploadingAvatar) CircularProgressIndicator(Modifier.size(AriSamDimensions.dp20), strokeWidth = AriSamDimensions.dp2)
                else Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(AriSamDimensions.dp19))
                Spacer(Modifier.width(AriSamDimensions.dp8))
                Text(stringResource(if (isUploadingAvatar) R.string.profile_photo_uploading else R.string.profile_change_photo), fontWeight = FontWeight.Bold)
            }
            onSettingsClick?.let { openSettings ->
                OutlinedButton(onClick = openSettings, modifier = Modifier.fillMaxWidth().height(AriSamDimensions.dp54), shape = MaterialTheme.shapes.large) {
                    Icon(Icons.Rounded.Settings, null, Modifier.size(AriSamDimensions.dp19))
                    Spacer(Modifier.width(AriSamDimensions.dp8))
                    Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
        ) {
            Button(
                onClick = onFollowClick,
                enabled = !isUpdatingFollow,
                modifier = Modifier.weight(1f).height(AriSamDimensions.dp54),
                shape = MaterialTheme.shapes.large,
                colors = if (isFollowing) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                if (isUpdatingFollow) {
                    CircularProgressIndicator(
                        Modifier.size(AriSamDimensions.dp20),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = AriSamDimensions.dp2,
                    )
                } else {
                    AnimatedContent(
                        targetState = isFollowing,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(spring(stiffness = 500f), initialScale = .8f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = .8f))
                        },
                        label = "profileFollowState",
                    ) { following ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp7),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (following) Icon(Icons.Rounded.Check, null, Modifier.size(AriSamDimensions.dp18))
                            Text(
                                stringResource(if (following) R.string.social_unfollow else R.string.social_follow),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onMessageClick,
                modifier = Modifier.weight(1f).height(AriSamDimensions.dp54),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.primary.copy(alpha = .48f)),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Chat, null, Modifier.size(AriSamDimensions.dp18))
                Spacer(Modifier.width(AriSamDimensions.dp7))
                Text(stringResource(R.string.chat_message), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlaylistSectionHeading(count: Int, isOwnProfile: Boolean) {
    val spacing = AriSamThemeTokens.spacing
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp3)) {
            Text(
                stringResource(R.string.social_public_playlists),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                stringResource(
                    if (isOwnProfile) R.string.profile_shared_sound_own else R.string.profile_shared_sound_other,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                pluralStringResource(R.plurals.social_playlist_count, count, count),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Surface(
            modifier = Modifier.size(AriSamDimensions.dp48),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.size(AriSamDimensions.dp24))
            }
        }
    }
}

@Composable
private fun StaggeredPlaylistRow(
    rowIndex: Int,
    playlists: List<PlaylistDto>,
    onPlaylistClick: (PlaylistDto) -> Unit,
) {
    var visible by remember(playlists.map(PlaylistDto::id)) { mutableStateOf(false) }
    LaunchedEffect(playlists.map(PlaylistDto::id)) {
        delay((rowIndex.coerceAtMost(6) * 55L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(360)) + slideInVertically(
            animationSpec = spring(dampingRatio = .82f, stiffness = 300f),
            initialOffsetY = { it / 4 },
        ),
        exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { it / 5 },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md),
        ) {
            playlists.forEach { playlist ->
                PressScaleBox(
                    onClick = { onPlaylistClick(playlist) },
                    modifier = Modifier.weight(1f),
                ) {
                    CinematicPlaylistCard(playlist)
                }
            }
            if (playlists.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CinematicPlaylistCard(playlist: PlaylistDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ProfileCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f)),
    ) {
        Column(Modifier.padding(AriSamDimensions.dp9), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(AriSamDimensions.dp22))
                    .background(Brush.linearGradient(listOf(ProfileViolet, ProfileAccent))),
            ) {
                if (!playlist.coverImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = playlist.coverImageUrl,
                        contentDescription = playlist.name,
                        error = painterResource(R.drawable.arisam_app_icon_dark),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(AriSamPalette.transparent, ProfileInk.copy(alpha = .66f)),
                                    startY = 90f,
                                ),
                            ),
                    )
                } else {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(AriSamDimensions.dp64)
                            .background(AriSamPalette.white.copy(alpha = .14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = AriSamPalette.white, modifier = Modifier.size(AriSamDimensions.dp34))
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(AriSamDimensions.dp9),
                    shape = CircleShape,
                    color = AriSamPalette.black.copy(alpha = .48f),
                    contentColor = AriSamPalette.white,
                ) {
                    Row(
                        Modifier.padding(horizontal = AriSamDimensions.dp9, vertical = AriSamDimensions.dp5),
                        horizontalArrangement = Arrangement.spacedBy(AriSamDimensions.dp4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, Modifier.size(AriSamDimensions.dp13))
                        Text(playlist.songCount.toString(), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Column(
                Modifier.padding(horizontal = AriSamDimensions.dp3, vertical = AriSamDimensions.dp2),
                verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp2),
            ) {
                Text(
                    playlist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.home_song_count, playlist.songCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlaylistShimmerRow() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AriSamThemeTokens.spacing.md),
    ) {
        repeat(2) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp9)) {
                ShimmerBox(Modifier.fillMaxWidth().aspectRatio(1f))
                ShimmerBox(Modifier.fillMaxWidth(.78f).height(AriSamDimensions.dp18))
                ShimmerBox(Modifier.fillMaxWidth(.46f).height(AriSamDimensions.dp12))
            }
        }
    }
}

@Composable
private fun ProfilePlaylistEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        shape = ProfileCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(AriSamDimensions.dp1, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(AriSamDimensions.dp28),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp9),
        ) {
            Surface(
                modifier = Modifier.size(AriSamDimensions.dp62),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .11f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(AriSamDimensions.dp30))
                }
            }
            Text(
                stringResource(R.string.profile_no_playlists_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.social_no_public_playlists),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfilePlaylistLoadError(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AriSamThemeTokens.spacing.lg),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.error.copy(alpha = .08f),
        contentColor = MaterialTheme.colorScheme.error,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AriSamDimensions.dp16, vertical = AriSamDimensions.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.social_action_failed),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(AriSamDimensions.dp17))
                Spacer(Modifier.width(AriSamDimensions.dp6))
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun ProfileLoadingState() {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp18),
        contentPadding = PaddingValues(bottom = AriSamDimensions.dp32),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(ProfileHeroHeight)) {
                ShimmerBox(Modifier.fillMaxWidth().height(ProfileBackdropHeight))
                ShimmerBox(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AriSamDimensions.dp34)
                        .size(AriSamDimensions.dp116)
                        .clip(CircleShape),
                )
                ShimmerBox(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = AriSamDimensions.dp16)
                        .fillMaxWidth()
                        .height(AriSamDimensions.dp94),
                )
            }
        }
        item { ShimmerBox(Modifier.padding(horizontal = AriSamDimensions.dp16).fillMaxWidth().height(AriSamDimensions.dp132)) }
        item { ShimmerBox(Modifier.padding(horizontal = AriSamDimensions.dp16).fillMaxWidth().height(AriSamDimensions.dp54)) }
        item { PlaylistShimmerRow() }
    }
}

@Composable
private fun ProfileErrorState(onRetry: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.background),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(360)) + scaleIn(spring(dampingRatio = .72f), initialScale = .82f),
        ) {
            Column(
                Modifier.padding(AriSamDimensions.dp32),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp12),
            ) {
                Surface(
                    modifier = Modifier.size(AriSamDimensions.dp76),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = .12f),
                    contentColor = MaterialTheme.colorScheme.error,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Headphones, null, Modifier.size(AriSamDimensions.dp36))
                    }
                }
                Text(
                    stringResource(R.string.social_profile_empty),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                FilledTonalButton(onClick = onRetry) {
                    Icon(Icons.Rounded.Refresh, null)
                    Spacer(Modifier.width(AriSamDimensions.dp8))
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun ProfileEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(AriSamDimensions.dp32),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AriSamDimensions.dp10),
        ) {
            Icon(
                Icons.Rounded.Person,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AriSamDimensions.dp52),
            )
            Text(
                stringResource(R.string.social_profile_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
