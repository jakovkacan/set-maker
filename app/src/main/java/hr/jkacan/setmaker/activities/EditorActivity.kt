package hr.jkacan.setmaker.activities

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import coil.compose.AsyncImage
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.databinding.ActivityEditorBinding
import hr.jkacan.setmaker.data.state.EditorState
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import hr.jkacan.setmaker.models.set.SetEdge
import hr.jkacan.setmaker.models.set.SetNode
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.utils.ThemeHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var setGraphRepository: hr.jkacan.setmaker.data.dao.SetGraphRepository
    private lateinit var songRepository: hr.jkacan.setmaker.data.dao.SongRepository
    private var currentSetId: Int = -1
    private var refreshTrigger by mutableIntStateOf(0)
    private var debugMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        // Get repositories from application
        val application = application as hr.jkacan.setmaker.SetMakerApplication
        setGraphRepository = application.setGraphRepository
        songRepository = application.songRepository

        // Get set information from intent
        currentSetId = intent.getIntExtra("SET_ID", -1)
        val setName = intent.getStringExtra("SET_NAME")

        supportActionBar?.title = setName

        if (currentSetId == -1) {
            // Handle error case
            finish()
            return
        }

        setupEditor()
    }

    private fun showSongPicker(fromNodeId: Int, toNodeId: Int?) {
        val songPicker = hr.jkacan.setmaker.fragments.SongPickerBottomSheet.newInstance()
        songPicker.onSongSelected = { song ->
            addNodeToGraph(song, fromNodeId, toNodeId)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun showSongPickerForBranch(fromNodeId: Int) {
        val songPicker = hr.jkacan.setmaker.fragments.SongPickerBottomSheet.newInstance()
        songPicker.onSongSelected = { song ->
            addBranchNode(song, fromNodeId)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun addBranchNode(song: Song, fromNodeId: Int) {
        // Validate song has valid ID
        val songId = song.id ?: 0
        if (songId <= 0) {
            return
        }

        // Insert new node
        val newNodeId = setGraphRepository.insertNode(
            setId = currentSetId,
            songId = songId,
            note = null
        )

        if (newNodeId > 0) {
            // Create edge from parent to new branch node
            setGraphRepository.insertEdge(
                setId = currentSetId,
                fromNodeId = fromNodeId,
                toNodeId = newNodeId.toInt()
            )

            // Refresh the editor to show the new branch
            refreshTrigger++
        }
    }

    private fun addNodeToGraph(song: Song, fromNodeId: Int, toNodeId: Int?) {
        // Validate song has valid ID
        val songId = song.id ?: 0
        if (songId <= 0) {
            return
        }

        // Insert new node
        val newNodeId = setGraphRepository.insertNode(
            setId = currentSetId,
            songId = songId,
            note = null
        )

        if (newNodeId > 0) {
            if (toNodeId == null) {
                // Adding to a leaf node (no toNodeId)
                // Create edge from fromNodeId to newNodeId
                setGraphRepository.insertEdge(
                    setId = currentSetId,
                    fromNodeId = fromNodeId,
                    toNodeId = newNodeId.toInt()
                )
            } else {
                // Inserting between two nodes
                // 1. Delete the old edge from fromNodeId to toNodeId
                setGraphRepository.deleteEdgeBetweenNodes(currentSetId, fromNodeId, toNodeId)

                // 2. Create edge from fromNodeId to newNodeId
                setGraphRepository.insertEdge(
                    setId = currentSetId,
                    fromNodeId = fromNodeId,
                    toNodeId = newNodeId.toInt()
                )

                // 3. Create edge from newNodeId to toNodeId
                setGraphRepository.insertEdge(
                    setId = currentSetId,
                    fromNodeId = newNodeId.toInt(),
                    toNodeId = toNodeId
                )
            }

            // Refresh the editor to show the new node
            refreshTrigger++
        }
    }

    private fun showSongPickerForFirstNode() {
        val songPicker = hr.jkacan.setmaker.fragments.SongPickerBottomSheet.newInstance()
        songPicker.onSongSelected = { song ->
            addFirstNodeToGraph(song)
        }
        songPicker.show(supportFragmentManager, "SongPicker")
    }

    private fun addFirstNodeToGraph(song: Song) {
        // Validate song has valid ID
        val songId = song.id ?: 0
        if (songId <= 0) {
            return
        }

        // Insert the first node
        val newNodeId = setGraphRepository.insertNode(
            setId = currentSetId,
            songId = songId,
            note = null
        )

        if (newNodeId > 0) {
            // Refresh the editor to show the new node
            refreshTrigger++
        }
    }

    private fun setupEditor() {
        binding.composeView.setContent {
            EditorCanvas()

            // Update FAB visibility based on graph state
            androidx.compose.runtime.LaunchedEffect(refreshTrigger) {
                val nodesWithSongs = setGraphRepository.getNodesWithSongsBySet(currentSetId)
                binding.fabAddSong.visibility = if (nodesWithSongs.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }

        // Initial FAB setup
        updateFabVisibility()
        binding.fabAddSong.setOnClickListener {
            showSongPickerForFirstNode()
        }
    }

    private fun updateFabVisibility() {
        val nodesWithSongs = setGraphRepository.getNodesWithSongsBySet(currentSetId)
        binding.fabAddSong.visibility = if (nodesWithSongs.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    @Composable
    private fun EditorCanvas() {
        // Load real data from repositories - key by refreshTrigger to reload when data changes
        val nodesWithSongs = remember(currentSetId, refreshTrigger) {
            setGraphRepository.getNodesWithSongsBySet(currentSetId)
        }
        val edges = remember(currentSetId, refreshTrigger) {
            setGraphRepository.getEdgesBySet(currentSetId)
        }

        // Convert database models to domain models
        val setNodes = nodesWithSongs.map { it.node }
        val songs = nodesWithSongs.associate { it.node.songId to it.song }

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        // Get screen/canvas center
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        val density = LocalDensity.current

        val centerX = with(density) { screenWidth.toPx() / 2f }
        val centerY = with(density) { screenHeight.toPx() / 2f }

        // Compute layout - this will recompute when nodesWithSongs or edges change
        val editorState = remember(nodesWithSongs, edges) {
            if (setNodes.isEmpty()) {
                // Empty graph state
                EditorState(
                    nodes = emptyMap(),
                    edges = emptyList(),
                    pan = Offset.Zero,
                    zoom = 1f
                )
            } else {
                computeGraphLayout(
                    nodes = setNodes,
                    edges = edges,
                    songs = songs.values.toList()
                )
            }
        }


        // State for dragging
        var draggingNodeId by remember { mutableStateOf<Long?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.background))
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // 1. Apply zoom limiting
                        val newScale = (scale * zoom).coerceIn(0.5f, 3f)

                        // 2. Zoom towards centroid
                        offset += Offset(
                            x = (1 - zoom) * (centroid.x - offset.x - centerX),
                            y = (1 - zoom) * (centroid.y - offset.y - centerY)
                        )

                        // 3. Update scale after offset adjustment
                        scale = newScale

                        // 4. Apply pan
                        offset += pan
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )

        ) {
            // Draw edges behind nodes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            ) {
                EdgeLayer(
                    edges = editorState.edges,
                    nodes = editorState.nodes,
                    pan = editorState.pan,
                    zoom = editorState.zoom,
                    debugMode = debugMode
                )
            }

            // Draw plus icons on edges
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.5f)
            ) {
                PlusIconLayer(
                    edges = editorState.edges,
                    nodes = editorState.nodes,
                    pan = editorState.pan,
                    zoom = editorState.zoom,
                    onPlusClick = { fromId, toId ->
                        // Regular tap: insert between nodes
                        showSongPicker(fromId.toInt(), toId?.toInt())
                    },
                    onPlusLongPress = { fromId, toId ->
                        // Long tap: create a branch from parent
                        if (toId != null) {
                            showSongPickerForBranch(fromId.toInt())
                        } else {
                            // For leaf nodes, long press behaves the same as regular tap
                            showSongPicker(fromId.toInt(), null)
                        }
                    }
                )
            }

            // Draw nodes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                NodeLayer(
                    nodes = editorState.nodes.values.toList(),
                    pan = editorState.pan,
                    zoom = editorState.zoom,
                    draggingNodeId = draggingNodeId,
                    dragOffset = dragOffset,
                    debugMode = debugMode,
                    onDragStart = { nodeId ->
                        draggingNodeId = nodeId
                        dragOffset = Offset.Zero
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                    },
                    onDragEnd = {
                        draggingNodeId = null
                        dragOffset = Offset.Zero
                        // Graph will auto-refresh via refreshTrigger if needed
                    }
                )
            }

            // Show empty state message when there are no songs
            if (setNodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colorResource(id = R.color.text_secondary).copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No songs in this set yet",
                            fontSize = 20.sp,
                            color = colorResource(id = R.color.text_primary),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tap the + button below to add your first song",
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.text_secondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EdgeLayer(
        edges: List<UiEdge>,
        nodes: Map<Long, UiNode>,
        pan: Offset,
        zoom: Float,
        debugMode: Boolean = false
    ) {
        val density = LocalDensity.current

        Canvas(modifier = Modifier.fillMaxSize()) {
            val nodeWidth = 120.dp.toPx()
            val nodeHeight = 150.dp.toPx()
            val horizontalSpacing = 180.dp.toPx()
            val verticalSpacing = 220.dp.toPx()

            // Draw regular edges
            edges.forEach { edge ->
                val fromNode = nodes[edge.fromId]
                val toNode = nodes[edge.toId]

                if (fromNode != null && toNode != null) {
                    drawEdge(
                        fromNode = fromNode,
                        toNode = toNode,
                        nodeWidth = nodeWidth,
                        nodeHeight = nodeHeight,
                        horizontalSpacing = horizontalSpacing,
                        verticalSpacing = verticalSpacing,
                        pan = pan,
                        zoom = zoom,
                        canvasWidth = size.width
                    )
                }
            }

            // Find leaf nodes (nodes with no outgoing edges)
            val nodesWithChildren = edges.map { it.fromId }.toSet()
            val leafNodes = nodes.values.filter { it.id !in nodesWithChildren }

            // Draw edges for leaf nodes
            leafNodes.forEach { leafNode ->
                val fromPos = calculateNodePosition(
                    leafNode,
                    size.width,
                    horizontalSpacing,
                    verticalSpacing,
                    pan,
                    zoom
                )

                // Start from bottom center of leaf node
                val start = Offset(
                    fromPos.x + nodeWidth / 2,
                    fromPos.y + nodeHeight
                )

                // End point is straight down, 100dp below
                val edgeLength = 100.dp.toPx()
                val end = Offset(
                    start.x,
                    start.y + edgeLength
                )

                // Draw straight vertical line
                drawLine(
                    color = Color(0xFF888888),
                    start = start,
                    end = end,
                    strokeWidth = 4f
                )

                // Draw arrow at the end
                val arrowSize = 20f
                val angle = Math.PI / 2 // 90 degrees (pointing down)
                val arrowAngle1 = angle + Math.PI * 5 / 6
                val arrowAngle2 = angle - Math.PI * 5 / 6

                val arrowPoint1 = Offset(
                    x = end.x + (arrowSize * kotlin.math.cos(arrowAngle1)).toFloat(),
                    y = end.y + (arrowSize * kotlin.math.sin(arrowAngle1)).toFloat()
                )

                val arrowPoint2 = Offset(
                    x = end.x + (arrowSize * kotlin.math.cos(arrowAngle2)).toFloat(),
                    y = end.y + (arrowSize * kotlin.math.sin(arrowAngle2)).toFloat()
                )

                drawLine(
                    color = Color(0xFF888888),
                    start = end,
                    end = arrowPoint1,
                    strokeWidth = 4f
                )

                drawLine(
                    color = Color(0xFF888888),
                    start = end,
                    end = arrowPoint2,
                    strokeWidth = 4f
                )
            }
        }

        // Debug overlay for edges
        if (debugMode) {
            edges.forEach { edge ->
                val fromNode = nodes[edge.fromId]
                val toNode = nodes[edge.toId]

                if (fromNode != null && toNode != null) {
                    val horizontalSpacing = with(density) { 180.dp.toPx() }
                    val verticalSpacing = with(density) { 220.dp.toPx() }
                    val configuration = LocalConfiguration.current
                    val canvasWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

                    val fromPos = calculateNodePosition(
                        fromNode,
                        canvasWidth,
                        horizontalSpacing,
                        verticalSpacing,
                        pan,
                        zoom
                    )
                    val toPos = calculateNodePosition(
                        toNode,
                        canvasWidth,
                        horizontalSpacing,
                        verticalSpacing,
                        pan,
                        zoom
                    )

                    val nodeWidth = with(density) { 120.dp.toPx() }
                    val nodeHeight = with(density) { 150.dp.toPx() }

                    val startY = fromPos.y + nodeHeight
                    val endY = toPos.y
                    val midX = (fromPos.x + toPos.x) / 2f + nodeWidth / 2f
                    val midY = (startY + endY) / 2f

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(midX.roundToInt(), midY.roundToInt()) }
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "E: ${edge.fromId}→${edge.toId}",
                            color = Color.Yellow,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEdge(
        fromNode: UiNode,
        toNode: UiNode,
        nodeWidth: Float,
        nodeHeight: Float,
        horizontalSpacing: Float,
        verticalSpacing: Float,
        pan: Offset,
        zoom: Float,
        canvasWidth: Float
    ) {
        val fromPos = calculateNodePosition(
            fromNode,
            canvasWidth,
            horizontalSpacing,
            verticalSpacing,
            pan,
            zoom
        )
        val toPos = calculateNodePosition(
            toNode,
            canvasWidth,
            horizontalSpacing,
            verticalSpacing,
            pan,
            zoom
        )

        // Start from bottom center of source node
        val start = Offset(
            fromPos.x + nodeWidth / 2,
            fromPos.y + nodeHeight
        )

        // End at top center of target node
        val targetEnd = Offset(
            toPos.x + nodeWidth / 2,
            toPos.y
        )

        // End the curve slightly above the target node (20dp above)
        val curveEndOffset = 20f
        val curveEnd = Offset(
            targetEnd.x,
            targetEnd.y - curveEndOffset
        )

        // Calculate control points for cubic Bézier curve
        val verticalDistance = curveEnd.y - start.y
        val controlPointOffset = verticalDistance * 0.5f

        val controlPoint1 = Offset(
            start.x,
            start.y + controlPointOffset
        )

        val controlPoint2 = Offset(
            curveEnd.x,
            curveEnd.y - controlPointOffset
        )

        // Draw curved path using cubic Bézier
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                curveEnd.x, curveEnd.y
            )
        }

        drawPath(
            path = path,
            color = Color(0xFF888888),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )

        // Draw straight vertical line from curve end to target node
        drawLine(
            color = Color(0xFF888888),
            start = curveEnd,
            end = targetEnd,
            strokeWidth = 4f
        )

        // Arrow always points straight down
        val arrowSize = 20f
        val angle = Math.PI / 2 // 90 degrees (pointing down)
        val arrowAngle1 = angle + Math.PI * 5 / 6
        val arrowAngle2 = angle - Math.PI * 5 / 6

        val arrowPoint1 = Offset(
            x = targetEnd.x + (arrowSize * kotlin.math.cos(arrowAngle1)).toFloat(),
            y = targetEnd.y + (arrowSize * kotlin.math.sin(arrowAngle1)).toFloat()
        )

        val arrowPoint2 = Offset(
            x = targetEnd.x + (arrowSize * kotlin.math.cos(arrowAngle2)).toFloat(),
            y = targetEnd.y + (arrowSize * kotlin.math.sin(arrowAngle2)).toFloat()
        )

        drawLine(
            color = Color(0xFF888888),
            start = targetEnd,
            end = arrowPoint1,
            strokeWidth = 4f
        )

        drawLine(
            color = Color(0xFF888888),
            start = targetEnd,
            end = arrowPoint2,
            strokeWidth = 4f
        )
    }

    @Composable
    private fun PlusIconLayer(
        edges: List<UiEdge>,
        nodes: Map<Long, UiNode>,
        pan: Offset,
        zoom: Float,
        onPlusClick: (fromId: Long, toId: Long?) -> Unit,
        onPlusLongPress: (fromId: Long, toId: Long?) -> Unit
    ) {
        val density = LocalDensity.current
        val nodeWidth = with(density) { 120.dp.toPx() }
        val nodeHeight = with(density) { 150.dp.toPx() }
        val horizontalSpacing = with(density) { 180.dp.toPx() }
        val verticalSpacing = with(density) { 220.dp.toPx() }
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Plus icons for regular edges
            edges.forEach { edge ->
                // Use key to ensure proper identity tracking
                androidx.compose.runtime.key(edge.fromId, edge.toId) {
                    val fromNode = nodes[edge.fromId]
                    val toNode = nodes[edge.toId]

                    if (fromNode != null && toNode != null) {
                        val fromPos = calculateNodePosition(
                            fromNode,
                            with(density) { screenWidth.toPx() },
                            horizontalSpacing,
                            verticalSpacing,
                            pan,
                            zoom
                        )
                        val toPos = calculateNodePosition(
                            toNode,
                            with(density) { screenWidth.toPx() },
                            horizontalSpacing,
                            verticalSpacing,
                            pan,
                            zoom
                        )

                        // Calculate middle point of the edge (approximation using simple midpoint)
                        val startY = fromPos.y + nodeHeight
                        val endY = toPos.y
                        val midX = (fromPos.x + toPos.x) / 2f + nodeWidth / 2f
                        val midY = (startY + endY) / 2f

                        PlusIcon(
                            x = midX,
                            y = midY,
                            onClick = { onPlusClick(edge.fromId, edge.toId) },
                            onLongClick = { onPlusLongPress(edge.fromId, edge.toId) }
                        )
                    }
                }
            }

            // Plus icons for leaf nodes
            val nodesWithChildren = edges.map { it.fromId }.toSet()
            val leafNodes = nodes.values.filter { it.id !in nodesWithChildren }

            leafNodes.forEach { leafNode ->
                // Use key to ensure proper identity tracking
                androidx.compose.runtime.key(leafNode.id) {
                    val fromPos = calculateNodePosition(
                        leafNode,
                        with(density) { screenWidth.toPx() },
                        horizontalSpacing,
                        verticalSpacing,
                        pan,
                        zoom
                    )

                    // Calculate middle point of the leaf edge
                    val startY = fromPos.y + nodeHeight
                    val edgeLength = with(density) { 100.dp.toPx() }
                    val midX = fromPos.x + nodeWidth / 2f
                    val midY = startY + edgeLength / 2f

                    PlusIcon(
                        x = midX,
                        y = midY,
                        onClick = { onPlusClick(leafNode.id, null) },
                        onLongClick = { onPlusLongPress(leafNode.id, null) }
                    )
                }
            }
        }
    }

    @Composable
    private fun PlusIcon(
        x: Float,
        y: Float,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ) {
        val density = LocalDensity.current
        val context = LocalContext.current
        val vibrator = context.getSystemService(Vibrator::class.java)
        val iconSizeDp = 32f
        val iconSizePx = with(density) { iconSizeDp.dp.toPx() }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (x - iconSizePx / 2).roundToInt(),
                        (y - iconSizePx / 2).roundToInt()
                    )
                }
                .size(iconSizeDp.dp)
                .clip(CircleShape)
                .background(Color(0xFF2a2a2a))
                .then(
                    if (onLongClick != null) {
                        Modifier.pointerInput(Unit) {
                            var longPressJob: kotlinx.coroutines.Job? = null

                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var longPressTriggered = false

                                // Launch coroutine for long press detection
                                @OptIn(DelicateCoroutinesApi::class)
                                longPressJob = GlobalScope.launch {
                                    delay(500) // 500ms long press threshold
                                    longPressTriggered = true

                                    // Trigger vibration
                                    @Suppress("MissingPermission")
                                    vibrator?.vibrate(
                                        VibrationEffect.createOneShot(
                                            50,
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )
                                }

                                // Wait for release
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })

                                // Clean up
                                longPressJob.cancel()

                                if (longPressTriggered) {
                                    onLongClick()
                                } else {
                                    onClick()
                                }
                            }
                        }
                    } else {
                        Modifier.clickable { onClick() }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Add node",
                modifier = Modifier.size((iconSizeDp * 0.75f).dp),
                tint = Color(0xFF888888)
            )
        }
    }

    @Composable
    private fun NodeLayer(
        nodes: List<UiNode>,
        pan: Offset,
        zoom: Float,
        draggingNodeId: Long?,
        dragOffset: Offset,
        debugMode: Boolean = false,
        onDragStart: (Long) -> Unit,
        onDrag: (Offset) -> Unit,
        onDragEnd: () -> Unit
    ) {
        val density = LocalDensity.current
        val context = LocalContext.current
        val vibrator = context.getSystemService(Vibrator::class.java)

        Box(modifier = Modifier.fillMaxSize()) {
            nodes.forEach { node ->
                val position = with(density) {
                    calculateNodePositionDp(node, pan, zoom)
                }

                val isDragging = draggingNodeId == node.id
                val finalOffset = if (isDragging) {
                    IntOffset(
                        (position.x + dragOffset.x).roundToInt(),
                        (position.y + dragOffset.y).roundToInt()
                    )
                } else {
                    IntOffset(position.x.roundToInt(), position.y.roundToInt())
                }

                Box {
                    SongNode(
                        song = node.song,
                        modifier = Modifier
                            .offset { finalOffset }
                            .pointerInput(node.id) {
                                var longPressJob: kotlinx.coroutines.Job? = null

                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var longPressTriggered = false

                                    // Launch coroutine for long press detection
                                    @OptIn(DelicateCoroutinesApi::class)
                                    longPressJob = GlobalScope.launch {
                                        delay(500) // 500ms long press threshold
                                        longPressTriggered = true

                                        // Trigger vibration
                                        @Suppress("MissingPermission")
                                        vibrator?.vibrate(
                                            VibrationEffect.createOneShot(
                                                50,
                                                VibrationEffect.DEFAULT_AMPLITUDE
                                            )
                                        )

                                        // Trigger visual feedback immediately
                                        onDragStart(node.id)
                                    }

                                    // Handle drag gestures
                                    var dragStarted = false
                                    do {
                                        val event = awaitPointerEvent()

                                        if (longPressTriggered) {
                                            // Once long press is triggered, handle drag
                                            event.changes.forEach { change ->
                                                if (change.pressed) {
                                                    val dragAmount = change.position - down.position
                                                    if (!dragStarted) {
                                                        dragStarted = true
                                                    }
                                                    onDrag(
                                                        Offset(
                                                            dragAmount.x - dragOffset.x,
                                                            dragAmount.y - dragOffset.y
                                                        )
                                                    )
                                                    change.consume()
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    // Clean up
                                    longPressJob.cancel()
                                    if (longPressTriggered) {
                                        onDragEnd()
                                    }
                                }
                            },
                        zoom = zoom,
                        isDragging = isDragging
                    )

                    // Debug overlay for nodes
                    if (debugMode) {
                        Box(
                            modifier = Modifier
                                .offset { finalOffset }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "ID: ${node.id}",
                                        color = Color.Cyan,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Col: ${node.col}",
                                        color = Color.Green,
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Row: ${node.row}",
                                        color = Color.Magenta,
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun calculateNodePositionDp(node: UiNode, pan: Offset, zoom: Float): Offset {
        val density = LocalDensity.current
        val horizontalSpacing = with(density) { 180.dp.toPx() }
        val verticalSpacing = with(density) { 220.dp.toPx() }
        val configuration = LocalConfiguration.current
        val canvasWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

        return calculateNodePosition(
            node,
            canvasWidth,
            horizontalSpacing,
            verticalSpacing,
            pan,
            zoom
        )
    }

    private fun calculateNodePosition(
        node: UiNode,
        canvasWidth: Float,
        horizontalSpacing: Float,
        verticalSpacing: Float,
        pan: Offset = Offset.Zero,
        zoom: Float = 1f
    ): Offset {
        val nodeWidth = 120f

        // Calculate horizontal position with centering
        // Center the node at canvas center, then apply column offset
        // Subtract half node width to center the node on its position
        val baseX = canvasWidth / 2f
        val x = baseX + (node.col * horizontalSpacing) - (nodeWidth / 2f)

        // Calculate vertical position
        val y = 100f + (node.row * verticalSpacing)

        return Offset(x, y)
    }

    private fun computeGraphLayout(
        nodes: List<SetNode>,
        edges: List<SetEdge>,
        songs: List<Song>
    ): EditorState {
        // Create a map of song ID to Song for quick lookup
        val songMap = songs.associateBy { it.id }

        // Build adjacency list
        val adjacencyList = mutableMapOf<Int, MutableList<Int>>()
        val inDegree = mutableMapOf<Int, Int>()

        nodes.forEach { node ->
            adjacencyList[node.id] = mutableListOf()
            inDegree[node.id] = 0
        }

        edges.forEach { edge ->
            adjacencyList[edge.fromNodeId]?.add(edge.toNodeId)
            inDegree[edge.toNodeId] = (inDegree[edge.toNodeId] ?: 0) + 1
        }

        // Find starting node (node with in-degree 0)
        val startNodes = nodes.filter { (inDegree[it.id] ?: 0) == 0 }

        // Compute row (layer) for each node using topological sorting
        val nodeRows = mutableMapOf<Int, Int>()
        val nodeColumns = mutableMapOf<Int, Int>()
        val queue = ArrayDeque<Pair<Int, Int>>() // (nodeId, row)

        // Initialize with start nodes at row 0, center column
        startNodes.forEach { node ->
            queue.add(Pair(node.id, 0))
            nodeRows[node.id] = 0
            nodeColumns[node.id] = 0 // Start in center
        }

        val processed = mutableSetOf<Int>()
        val tempInDegree = inDegree.toMutableMap()

        while (queue.isNotEmpty()) {
            val (nodeId, row) = queue.removeFirst()

            if (nodeId in processed) continue
            processed.add(nodeId)

            val children = adjacencyList[nodeId] ?: emptyList()

            if (children.isNotEmpty()) {
                // Assign columns to children
                val numChildren = children.size
                val parentCol = nodeColumns[nodeId] ?: 0

                children.forEachIndexed { index, childId ->
                    val childRow = row + 1

                    // Calculate column offset for branching
                    val columnOffset = if (numChildren == 1) {
                        parentCol
                    } else {
                        // For even numbers: spread around parent (-0.5, 0.5, -1.5, 1.5, etc.)
                        // For odd numbers: spread around parent (0, -1, 1, -2, 2, etc.)
                        if (numChildren % 2 == 0) {
                            // Even: alternate left and right, starting right
                            val step = (index + 1) / 2
                            if (index % 2 == 0) {
                                parentCol + step
                            } else {
                                parentCol - step
                            }
                        } else {
                            // Odd: center child at parent's column
                            val centerIndex = numChildren / 2
                            parentCol + (index - centerIndex)
                        }
                    }

                    // Only update if not yet assigned or if this gives a later row
                    if (childId !in nodeRows || nodeRows[childId]!! < childRow) {
                        nodeRows[childId] = childRow
                        nodeColumns[childId] = columnOffset
                    }

                    tempInDegree[childId] = (tempInDegree[childId] ?: 0) - 1
                    if (tempInDegree[childId] == 0) {
                        queue.add(Pair(childId, childRow))
                    }
                }
            }
        }

        // Create UiNodes
        val uiNodes = nodes.mapNotNull { node ->
            val song = songMap[node.songId]
            if (song != null) {
                UiNode(
                    id = node.id.toLong(),
                    col = nodeColumns[node.id] ?: 0,
                    row = nodeRows[node.id] ?: 0,
                    song = song
                )
            } else null
        }.associateBy { it.id }

        // Create UiEdges
        val uiEdges = edges.map { edge ->
            UiEdge(
                fromId = edge.fromNodeId.toLong(),
                toId = edge.toNodeId.toLong()
            )
        }

        return EditorState(
            nodes = uiNodes,
            edges = uiEdges,
            pan = Offset.Zero,
            zoom = 1f
        )
    }

    @Composable
    fun SongNode(
        song: Song,
        modifier: Modifier = Modifier,
        zoom: Float = 1f,
        isDragging: Boolean = false
    ) {
        Card(
            modifier = modifier
                .width(120.dp)
                .wrapContentHeight()
                .graphicsLayer(
                    alpha = if (isDragging) 0.5f else 1f,
                    rotationZ = if (isDragging) 5f else 0f
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3a3a3a)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = "Album cover for ${song.title}",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.text_primary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.text_secondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Modern way to handle back
        return true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_debug -> {
                debugMode = !debugMode
                item.isChecked = debugMode
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}