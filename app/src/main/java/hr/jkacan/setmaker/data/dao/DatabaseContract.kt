package hr.jkacan.setmaker.data.dao

object DatabaseContract {
    // Song table
    object SongEntry {
        const val TABLE_NAME = "songs"
        const val COLUMN_ID = "id"
        const val COLUMN_PLATFORM_ID = "platform_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_ARTIST = "artist"
        const val COLUMN_COVER_URL = "cover_url"
        const val COLUMN_PROVIDER = "provider"
        const val COLUMN_PREVIEW_URL = "preview_url"
        const val COLUMN_SONG_URL = "song_url"
        const val COLUMN_DATE_ADDED = "date_added"
    }

    // Set table (playlist)
    object SetEntry {
        const val TABLE_NAME = "sets"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_COVER_URL = "cover_url"
        const val COLUMN_DATE_ADDED = "date_added"
        const val COLUMN_DATE_UPDATED = "date_updated"
    }

    // Set Node table - each occurrence/slot in a set
    object SetNodeEntry {
        const val TABLE_NAME = "set_node"
        const val COLUMN_ID = "id"
        const val COLUMN_SET_ID = "set_id"
        const val COLUMN_SONG_ID = "song_id"
        const val COLUMN_NOTE = "note"
    }

    // Set Edge table - directed transitions between nodes
    object SetEdgeEntry {
        const val TABLE_NAME = "set_edge"
        const val COLUMN_ID = "id"
        const val COLUMN_SET_ID = "set_id"
        const val COLUMN_FROM_NODE_ID = "from_node_id"
        const val COLUMN_TO_NODE_ID = "to_node_id"
        const val COLUMN_ORD = "ord"
        const val COLUMN_KIND = "kind"
    }
}

