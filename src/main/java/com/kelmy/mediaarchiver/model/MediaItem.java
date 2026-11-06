package com.kelmy.mediaarchiver.model;

/**
 * Immutable Data Transfer Object (DTO) to standardize media data
 * obtained from any platform (YouTube, Bilibili, etc.).
 * 
 * @param id         Unique identifier for the video/post on the originating
 *                   platform
 * @param title      Content title
 * @param url        Direct URL for accessing or downloading the video/post
 * @param extractor  Extractor identifier name (e.g., 'youtube', 'bilibili')
 * @param uploadDate Publication date (YYYYMMDD or ISO format, optional)
 */
public record MediaItem(String id, String title, String url, String extractor, String uploadDate) {
}
