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
import coil.compose.AsyncImage
import hr.jkacan.setmaker.R
import hr.jkacan.setmaker.databinding.ActivityEditorBinding
import hr.jkacan.setmaker.models.editor.EditorState
import hr.jkacan.setmaker.models.editor.UiEdge
import hr.jkacan.setmaker.models.editor.UiNode
import hr.jkacan.setmaker.models.set.SetEdge
import hr.jkacan.setmaker.models.set.SetNode
import hr.jkacan.setmaker.models.song.Song
import hr.jkacan.setmaker.utils.DemoItems
import hr.jkacan.setmaker.utils.ThemeHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get set information from intent
        val setId = intent.getIntExtra("SET_ID", -1)
        val setName = intent.getStringExtra("SET_NAME")

        supportActionBar?.title = "Editing: $setName"

        // TODO: Handle error case when setId is -1
        setupEditor()
    }

    private fun setupEditor() {
        binding.composeView.setContent {
            EditorCanvas()
        }
    }

    @Composable
    private fun EditorCanvas() {
        val demoItems = DemoItems()

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        // Get screen/canvas center
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        val density = LocalDensity.current

        val centerX = with(density) { screenWidth.toPx() / 2f }
        val centerY = with(density) { screenHeight.toPx() / 2f }

        // Compute initial layout
        val initialState = remember {
            computeGraphLayout(
                nodes = demoItems.demoSetNodes,
                edges = demoItems.demoSetEdges,
                songs = demoItems.demoSongs
            )
        }

        var editorState by remember { mutableStateOf(initialState) }

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
                    zoom = editorState.zoom
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
                        // TODO: Handle adding node between fromId and toId
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
                        // Redraw graph by recomputing layout
                        editorState = computeGraphLayout(
                            nodes = demoItems.demoSetNodes,
                            edges = demoItems.demoSetEdges,
                            songs = demoItems.demoSongs
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun EdgeLayer(
        edges: List<UiEdge>,
        nodes: Map<Long, UiNode>,
        pan: Offset,
        zoom: Float
    ) {
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
                    fromPos.x + (nodeWidth * zoom) / 2,
                    fromPos.y + (nodeHeight * zoom)
                )

                // End point is straight down, 100dp below
                val edgeLength = 100.dp.toPx() * zoom
                val end = Offset(
                    start.x,
                    start.y + edgeLength
                )

                // Draw straight vertical line
                drawLine(
                    color = Color(0xFF888888),
                    start = start,
                    end = end,
                    strokeWidth = 4f * zoom
                )

                // Draw arrow at the end
                val arrowSize = 20f * zoom
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
                    strokeWidth = 4f * zoom
                )

                drawLine(
                    color = Color(0xFF888888),
                    start = end,
                    end = arrowPoint2,
                    strokeWidth = 4f * zoom
                )
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
            fromPos.x + (nodeWidth * zoom) / 2,
            fromPos.y + (nodeHeight * zoom)
        )

        // End at top center of target node
        val targetEnd = Offset(
            toPos.x + (nodeWidth * zoom) / 2,
            toPos.y
        )

        // End the curve slightly above the target node (20dp above)
        val curveEndOffset = 20f * zoom
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
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f * zoom)
        )

        // Draw straight vertical line from curve end to target node
        drawLine(
            color = Color(0xFF888888),
            start = curveEnd,
            end = targetEnd,
            strokeWidth = 4f * zoom
        )

        // Arrow always points straight down
        val arrowSize = 20f * zoom
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
            strokeWidth = 4f * zoom
        )

        drawLine(
            color = Color(0xFF888888),
            start = targetEnd,
            end = arrowPoint2,
            strokeWidth = 4f * zoom
        )
    }

    @Composable
    private fun PlusIconLayer(
        edges: List<UiEdge>,
        nodes: Map<Long, UiNode>,
        pan: Offset,
        zoom: Float,
        onPlusClick: (fromId: Long, toId: Long?) -> Unit
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
                    val startY = fromPos.y + (nodeHeight * zoom)
                    val endY = toPos.y
                    val midX = (fromPos.x + toPos.x) / 2f + (nodeWidth * zoom) / 2f
                    val midY = (startY + endY) / 2f

                    PlusIcon(
                        x = midX,
                        y = midY,
                        zoom = zoom,
                        onClick = { onPlusClick(edge.fromId, edge.toId) }
                    )
                }
            }

            // Plus icons for leaf nodes
            val nodesWithChildren = edges.map { it.fromId }.toSet()
            val leafNodes = nodes.values.filter { it.id !in nodesWithChildren }

            leafNodes.forEach { leafNode ->
                val fromPos = calculateNodePosition(
                    leafNode,
                    with(density) { screenWidth.toPx() },
                    horizontalSpacing,
                    verticalSpacing,
                    pan,
                    zoom
                )

                // Calculate middle point of the leaf edge
                val startY = fromPos.y + (nodeHeight * zoom)
                val edgeLength = with(density) { 100.dp.toPx() } * zoom
                val midX = fromPos.x + (nodeWidth * zoom) / 2f
                val midY = startY + edgeLength / 2f

                PlusIcon(
                    x = midX,
                    y = midY,
                    zoom = zoom,
                    onClick = { onPlusClick(leafNode.id, null) }
                )
            }
        }
    }

    @Composable
    private fun PlusIcon(
        x: Float,
        y: Float,
        zoom: Float,
        onClick: () -> Unit
    ) {
        val density = LocalDensity.current
        val iconSizeDp = (32 * zoom).coerceIn(24f, 48f)
        val iconSizePx = with(density) { iconSizeDp.dp.toPx() }

        Box(
            modifier = Modifier
                .offset { IntOffset((x - iconSizePx / 2).roundToInt(), (y - iconSizePx / 2).roundToInt()) }
                .size(iconSizeDp.dp)
                .clip(CircleShape)
                .background(Color(0xFF2a2a2a))
                .clickable { onClick() },
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
                                        VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
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
                                                onDrag(Offset(
                                                    dragAmount.x - dragOffset.x,
                                                    dragAmount.y - dragOffset.y
                                                ))
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
            }
        }
    }

    @Composable
    private fun calculateNodePositionDp(node: UiNode, pan: Offset, zoom: Float): Offset {
        val density = LocalDensity.current
        val horizontalSpacing = with(density) { 180.dp.toPx() }
        val verticalSpacing = with(density) { 220.dp.toPx() }
        val canvasWidth = with(density) {
            // Use a reference width for centering
            1080f
        }

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
        pan: Offset,
        zoom: Float = 1f
    ): Offset {
        val nodeWidth = 120f

        // Calculate horizontal position with centering (subtract half node width to center the node)
        val baseX = canvasWidth / 2f - (nodeWidth / 2f)
        val x = (baseX + (node.col * horizontalSpacing)) * zoom + pan.x

        // Calculate vertical position
        val y = (100f + (node.row * verticalSpacing)) * zoom + pan.y

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
                .width((120 * zoom).dp)
                .wrapContentHeight()
                .graphicsLayer(
                    alpha = if (isDragging) 0.5f else 1f,
                    rotationZ = if (isDragging) 5f else 0f
                ),
            shape = RoundedCornerShape((12 * zoom).dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF3a3a3a)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = (4 * zoom).dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding((8 * zoom).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = "Album cover for ${song.title}",
                    modifier = Modifier
                        .size((100 * zoom).dp)
                        .clip(RoundedCornerShape((8 * zoom).dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height((8 * zoom).dp))

                Text(
                    text = song.title,
                    fontSize = (14 * zoom).sp,
                    color = colorResource(id = R.color.text_primary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = song.artist,
                    fontSize = (12 * zoom).sp,
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
}