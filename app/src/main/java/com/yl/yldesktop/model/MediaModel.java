package com.yl.yldesktop.model;

import android.graphics.Bitmap;

import java.util.Objects;

public class MediaModel {

    private String title;
    private String artist;
    private Bitmap albumArt;

    public MediaModel(String title, String artist, Bitmap albumArt) {
        this.title = title;
        this.artist = artist;
        this.albumArt = albumArt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MediaModel that = (MediaModel) o;
        return Objects.equals(title, that.title) && Objects.equals(artist, that.artist) && Objects.equals(albumArt, that.albumArt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist, albumArt);
    }
}
