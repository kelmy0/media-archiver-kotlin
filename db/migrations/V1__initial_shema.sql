CREATE TABLE IF NOT EXISTS channels (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    platform TEXT NOT NULL,
    feed_url TEXT NOT NULL UNIQUE,
    folder_name TEXT NOT NULL,
    options_json TEXT,
    active BOOLEAN DEFAULT 1,
    last_checked TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- A video belongs to exactly one channel (aggregation: the channel holds a list of videos).
-- The pair (platform, video_id) identifies the video and prevents the same media from being
-- downloaded twice. Deleting a channel deletes the records of its videos (never the files).
CREATE TABLE IF NOT EXISTS videos (
    platform TEXT NOT NULL,
    video_id TEXT NOT NULL,
    channel_id INTEGER NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    file_path TEXT NOT NULL,
    source_url TEXT NOT NULL,
    downloaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archive_status TEXT DEFAULT 'none' CHECK (
        archive_status IN (
            'none',
            'pending',
            'archived',
            'failed',
            'skipped'
        )
    ),
    archive_url TEXT,
    PRIMARY KEY (platform, video_id)
);

CREATE INDEX IF NOT EXISTS idx_videos_channel_id ON videos (channel_id);

CREATE TABLE IF NOT EXISTS retries (
    platform TEXT NOT NULL,
    video_id TEXT NOT NULL,
    item_url TEXT NOT NULL,
    folder_name TEXT NOT NULL,
    title TEXT NOT NULL,
    PRIMARY KEY (platform, video_id)
);

-- Channel folders whose archive.txt was already imported into `videos`
-- (so the file is read once per folder, not on every run).
CREATE TABLE IF NOT EXISTS imported_folders (
    folder_name TEXT PRIMARY KEY,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
